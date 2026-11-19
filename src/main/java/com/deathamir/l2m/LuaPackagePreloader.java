package com.deathamir.l2m;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

public final class LuaPackagePreloader {
    private static final Path PACKAGE_DIR = Path.of("config", "l2m", "packages");
    private static final Path GENERATED_DIR = Path.of("config", "l2m", "generated");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private LuaPackagePreloader() {}

    public static void preload() {
        if (!Files.isDirectory(PACKAGE_DIR)) return;
        try (var packages = Files.list(PACKAGE_DIR)) {
            packages.filter(Files::isDirectory).filter(directory -> !Files.exists(directory.resolve(".disabled"))).sorted().forEach(directory -> {
                Path manifestPath = directory.resolve("package.json");
                if (!Files.isRegularFile(manifestPath)) return;
                try {
                    JsonObject manifest = JsonParser.parseString(Files.readString(manifestPath)).getAsJsonObject();
                    String packageId = manifest.has("id") ? manifest.get("id").getAsString() : "";
                    String entrypoint = manifest.has("entrypoint") ? manifest.get("entrypoint").getAsString() : "";
                    if (!validPackageId(packageId) || !directory.getFileName().toString().equals(packageId))
                        throw new IllegalArgumentException("Package id does not match its directory.");
                    Path root = directory.toAbsolutePath().normalize();
                    Path script = root.resolve(entrypoint).normalize();
                    if (!script.startsWith(root) || !entrypoint.endsWith(".lua") || !Files.isRegularFile(script))
                        throw new IllegalArgumentException("Invalid or missing package entrypoint.");
                    preloadScript(script);
                    L2M.LOGGER.info("Preloaded L2M package definitions from {}", packageId);
                } catch (Exception e) {
                    L2M.LOGGER.error("Could not preload L2M package {}", directory.getFileName(), e);
                }
            });
        } catch (IOException e) {
            L2M.LOGGER.error("Could not scan L2M packages before registry setup", e);
        }
    }

    private static void preloadScript(Path script) throws IOException {
        Globals globals = JsePlatform.standardGlobals();
        LuaTable api = new LuaTable();
        api.set("registerBlock", new TwoArgFunction() {
            @Override public LuaValue call(LuaValue id, LuaValue properties) {
                saveDefinition("blocks", id.checkjstring(), properties.checktable());
                return LuaValue.TRUE;
            }
        });
        api.set("registerItem", new TwoArgFunction() {
            @Override public LuaValue call(LuaValue id, LuaValue properties) {
                saveDefinition("items", id.checkjstring(), properties.checktable());
                return LuaValue.TRUE;
            }
        });
        api.set("defineCategory", new TwoArgFunction() {
            @Override public LuaValue call(LuaValue id, LuaValue properties) {
                saveDefinition("categories", id.checkjstring(), properties.checktable());
                return LuaValue.TRUE;
            }
        });
        VarArgFunction ignored = new VarArgFunction() {
            @Override public org.luaj.vm2.Varargs invoke(org.luaj.vm2.Varargs args) {
                return LuaValue.NIL;
            }
        };
        api.set("on", new TwoArgFunction() {
            @Override public LuaValue call(LuaValue event, LuaValue callback) {
                if (!callback.isfunction()) throw new LuaError("Event callback must be a function.");
                return LuaValue.TRUE;
            }
        });
        api.set("systemChat", ignored);
        api.set("clientChat", ignored);
        api.set("chat", ignored);
        api.set("log", ignored);
        api.set("drawText", ignored);
        api.set("drawRect", ignored);
        api.set("clearDraw", ignored);
        api.set("openInventory", ignored);
        api.set("giveCustomItem", ignored);
        api.set("equipItem", ignored);
        api.set("listCategories", ignored);
        globals.set("l2m", api);
        try {
            globals.load(Files.readString(script), "@" + script.getFileName()).call();
        } catch (LuaError e) {
            throw new IllegalArgumentException("Package registration pass failed: " + e.getMessage(), e);
        }
    }

    private static void saveDefinition(String type, String id, LuaTable properties) {
        if (!validId(id)) throw new LuaError("Content id must use lowercase letters, numbers and underscores.");
        JsonObject definition = new JsonObject();
        definition.addProperty("id", id);
        definition.addProperty("displayName", properties.get("displayName").optjstring(id));
        if (type.equals("blocks")) {
            definition.addProperty("hardness", properties.get("hardness").optdouble(1.5));
            definition.addProperty("requiresTool", properties.get("requiresTool").optboolean(false));
            definition.addProperty("category", properties.get("category").optjstring("building"));
            definition.addProperty("icon", properties.get("icon").optjstring("minecraft:stone"));
            definition.addProperty("texture", properties.get("texture").optjstring(""));
            definition.addProperty("description", properties.get("description").optjstring(""));
        } else if (type.equals("categories")) {
            definition.addProperty("icon", properties.get("icon").optjstring("minecraft:chest"));
            definition.addProperty("description", properties.get("description").optjstring(""));
            definition.addProperty("sortOrder", properties.get("sortOrder").optint(0));
        } else {
            int stackSize = properties.get("stackSize").optint(64);
            if (stackSize < 1 || stackSize > 99) throw new LuaError("Item stackSize must be between 1 and 99.");
            String slot = properties.get("slot").optjstring("mainhand");
            if (!java.util.List.of("mainhand", "offhand", "shield", "head", "chest", "legs", "feet").contains(slot))
                throw new LuaError("Invalid item equipment slot.");
            definition.addProperty("stackSize", stackSize);
            definition.addProperty("category", properties.get("category").optjstring("misc"));
            definition.addProperty("minecraftItem", properties.get("minecraftItem").optjstring("minecraft:stone"));
            definition.addProperty("slot", slot);
            definition.addProperty("description", properties.get("description").optjstring(""));
            definition.addProperty("icon", properties.get("icon").optjstring(properties.get("minecraftItem").optjstring("minecraft:stone")));
            definition.addProperty("texture", properties.get("texture").optjstring(""));
            definition.addProperty("armor", properties.get("armor").optint(0));
            definition.addProperty("toughness", properties.get("toughness").optdouble(0));
        }
        try {
            Path output = GENERATED_DIR.resolve(type).resolve(id + ".json");
            Files.createDirectories(output.getParent());
            Files.writeString(output, GSON.toJson(definition));
        } catch (IOException e) {
            throw new LuaError("Could not save generated content: " + e.getMessage());
        }
    }

    private static boolean validPackageId(String id) {
        return id.matches("[a-z0-9][a-z0-9_-]{0,47}");
    }

    private static boolean validId(String id) {
        return id.matches("[a-z0-9_]{1,48}");
    }
}
