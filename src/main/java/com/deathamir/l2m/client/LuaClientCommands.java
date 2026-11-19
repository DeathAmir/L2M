package com.deathamir.l2m.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import com.deathamir.l2m.L2M;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = L2M.MODID, value = Dist.CLIENT)
public final class LuaClientCommands {
    private static final Path ROOT = Path.of("config", "l2m");
    private static final Path SCRIPT_DIR = ROOT.resolve("scripts");
    private static final Path PACKAGE_DIR = ROOT.resolve("packages");
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]{1,48}");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private LuaClientCommands() {}

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("l2m")
            .then(Commands.literal("run").then(Commands.argument("file", StringArgumentType.word())
                .executes(c -> runScript(c.getSource(), SCRIPT_DIR.resolve(StringArgumentType.getString(c, "file") + ".lua")))))
            .then(Commands.literal("package").then(Commands.literal("run")
                .then(Commands.argument("package", StringArgumentType.word())
                    .then(Commands.argument("script", StringArgumentType.word())
                        .executes(c -> runPackageScript(c.getSource(), StringArgumentType.getString(c, "package"), StringArgumentType.getString(c, "script")))))))
            .then(Commands.literal("packages").executes(c -> listPackages(c.getSource())))
            .then(Commands.literal("inventory").executes(c -> { Minecraft.getInstance().setScreen(new LuaInventoryScreen("")); return 1; }))
            .then(Commands.literal("console").executes(c -> {
                Minecraft.getInstance().setScreen(new LuaDeveloperConsole());
                return 1;
            }))
            .then(Commands.literal("package")
                .then(Commands.literal("list").executes(c -> listPackages(c.getSource())))
                .then(Commands.literal("validate").then(Commands.argument("package", StringArgumentType.word())
                    .executes(c -> validatePackage(c.getSource(), StringArgumentType.getString(c, "package")))))
                .then(Commands.literal("info").then(Commands.argument("package", StringArgumentType.word())
                    .executes(c -> packageInfo(c.getSource(), StringArgumentType.getString(c, "package")))))
                .then(Commands.literal("enable").then(Commands.argument("package", StringArgumentType.word())
                    .executes(c -> setPackageEnabled(c.getSource(), StringArgumentType.getString(c, "package"), true))))
                .then(Commands.literal("disable").then(Commands.argument("package", StringArgumentType.word())
                    .executes(c -> setPackageEnabled(c.getSource(), StringArgumentType.getString(c, "package"), false))))
                .then(Commands.literal("remove").then(Commands.argument("package", StringArgumentType.word())
                    .executes(c -> removePackage(c.getSource(), StringArgumentType.getString(c, "package")))))));
    }

    private static int validatePackage(CommandSourceStack source, String packageId) {
        if (!validId(packageId)) return fail(source, "Invalid package id.");
        Path root = PACKAGE_DIR.resolve(packageId).toAbsolutePath().normalize();
        try {
            Path base = PACKAGE_DIR.toAbsolutePath().normalize();
            if (!root.startsWith(base)) return fail(source, "Invalid package path.");
            Path manifestPath = root.resolve("package.json");
            if (!Files.isRegularFile(manifestPath)) return fail(source, "package.json is missing.");
            JsonObject manifest = JsonParser.parseString(Files.readString(manifestPath)).getAsJsonObject();
            String id = manifest.has("id") ? manifest.get("id").getAsString() : "";
            String entrypoint = manifest.has("entrypoint") ? manifest.get("entrypoint").getAsString() : "";
            if (!id.equals(packageId)) return fail(source, "Manifest id does not match the package folder.");
            Path entry = root.resolve(entrypoint).normalize();
            if (!entry.startsWith(root) || !entrypoint.endsWith(".lua") || !Files.isRegularFile(entry))
                return fail(source, "Entrypoint is missing or outside the package directory.");
            if (manifest.has("dependencies") && !manifest.get("dependencies").isJsonArray())
                return fail(source, "dependencies must be a JSON array.");
            if (manifest.has("dependencies")) {
                for (var dependency : manifest.getAsJsonArray("dependencies")) {
                    String dependencyId = dependency.getAsString();
                    if (!validId(dependencyId) || !Files.isDirectory(PACKAGE_DIR.resolve(dependencyId)))
                        return fail(source, "Missing dependency: " + dependencyId);
                }
            }
            send(source, "Package '" + packageId + "' is valid. Entrypoint: " + entrypoint);
            LuaDeveloperConsole.log("PACKAGE OK: " + packageId);
            return 1;
        } catch (Exception e) {
            return fail(source, "Package validation failed: " + safeMessage(e));
        }
    }

    private static int packageInfo(CommandSourceStack source, String packageId) {
        if (!validId(packageId)) return fail(source, "Invalid package id.");
        Path root = PACKAGE_DIR.resolve(packageId).normalize();
        Path manifestPath = root.resolve("package.json");
        try {
            if (!Files.isRegularFile(manifestPath)) return fail(source, "Package manifest not found.");
            JsonObject manifest = JsonParser.parseString(Files.readString(manifestPath)).getAsJsonObject();
            String name = manifest.has("name") ? manifest.get("name").getAsString() : packageId;
            String version = manifest.has("version") ? manifest.get("version").getAsString() : "unknown";
            String entry = manifest.has("entrypoint") ? manifest.get("entrypoint").getAsString() : "not set";
            boolean enabled = !Files.exists(root.resolve(".disabled"));
            send(source, name + " [" + packageId + "] v" + version + " | " + (enabled ? "enabled" : "disabled") + " | entry: " + entry);
            return 1;
        } catch (Exception e) {
            return fail(source, "Could not read package: " + safeMessage(e));
        }
    }

    private static int setPackageEnabled(CommandSourceStack source, String packageId, boolean enabled) {
        if (!validId(packageId)) return fail(source, "Invalid package id.");
        Path root = PACKAGE_DIR.resolve(packageId).normalize();
        if (!Files.isDirectory(root) || !Files.isRegularFile(root.resolve("package.json")))
            return fail(source, "Installed package not found.");
        try {
            Path marker = root.resolve(".disabled");
            if (enabled) Files.deleteIfExists(marker);
            else Files.writeString(marker, "disabled");
            send(source, "Package '" + packageId + "' " + (enabled ? "enabled" : "disabled") + ". Rejoin the world to apply.");
            return 1;
        } catch (IOException e) {
            return fail(source, "Could not update package state: " + safeMessage(e));
        }
    }

    private static int removePackage(CommandSourceStack source, String packageId) {
        if (!validId(packageId)) return fail(source, "Invalid package id.");
        Path root = PACKAGE_DIR.resolve(packageId).toAbsolutePath().normalize();
        try {
            if (!root.startsWith(PACKAGE_DIR.toAbsolutePath().normalize()) || !Files.isRegularFile(root.resolve("package.json")))
                return fail(source, "Installed package not found or path rejected.");
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
            send(source, "Package '" + packageId + "' removed.");
            LuaDeveloperConsole.log("PACKAGE REMOVED: " + packageId);
            return 1;
        } catch (IOException e) {
            return fail(source, "Could not remove package: " + safeMessage(e));
        }
    }

    private static void send(CommandSourceStack source, String message) {
        if (source == null) localMessage(message);
        else source.sendSuccess(() -> Component.literal("L2M: " + message), false);
        LuaDeveloperConsole.log(message);
    }

    private static String safeMessage(Exception error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private static int runPackageScript(CommandSourceStack source, String packageId, String scriptName) {
        if (!validId(packageId) || !validId(scriptName)) return fail(source, "Invalid package or script id.");
        return runScript(source, PACKAGE_DIR.resolve(packageId).resolve("scripts").resolve(scriptName + ".lua"));
    }

    private static int listPackages(CommandSourceStack source) {
        try {
            Files.createDirectories(PACKAGE_DIR);
            try (var entries = Files.list(PACKAGE_DIR)) {
                var names = entries.filter(Files::isDirectory).map(p -> p.getFileName().toString()).sorted().toList();
                source.sendSuccess(() -> Component.literal(names.isEmpty() ? "L2M: no packages installed." : "L2M packages: " + String.join(", ", names)), false);
            }
            return 1;
        } catch (IOException e) {
            return fail(source, "Could not read package directory.");
        }
    }

    public static void runAutoPackages() {
        try {
            Files.createDirectories(PACKAGE_DIR);
            try (var packages = Files.list(PACKAGE_DIR)) {
                packages.filter(Files::isDirectory).sorted().forEach(directory -> {
                    Path metadata = directory.resolve("package.json");
                    if (!Files.isRegularFile(metadata) || Files.exists(directory.resolve(".disabled"))) return;
                    try {
                        JsonObject manifest = JsonParser.parseString(Files.readString(metadata)).getAsJsonObject();
                        String packageId = manifest.has("id") ? manifest.get("id").getAsString() : "";
                        String entrypoint = manifest.has("entrypoint") ? manifest.get("entrypoint").getAsString() : "";
                        if (!validId(packageId) || !directory.getFileName().toString().equals(packageId))
                            throw new IllegalArgumentException("Package id does not match its folder.");
                        Path packageRoot = directory.toAbsolutePath().normalize();
                        Path script = packageRoot.resolve(entrypoint).normalize();
                        if (!script.startsWith(packageRoot) || !entrypoint.endsWith(".lua"))
                            throw new IllegalArgumentException("Invalid package entrypoint.");
                        if (Files.isRegularFile(script)) runScript(null, script);
                        else L2M.LOGGER.warn("L2M package {} has no entrypoint at {}", packageId, entrypoint);
                    } catch (Exception e) {
                        L2M.LOGGER.error("Could not auto-run L2M package {}", directory.getFileName(), e);
                    }
                });
            }
        } catch (IOException e) {
            L2M.LOGGER.error("Could not scan L2M packages", e);
        }
    }

    private static int runScript(CommandSourceStack source, Path script) {
        try {
            Files.createDirectories(SCRIPT_DIR);
            Files.createDirectories(PACKAGE_DIR);
            Path normalized = script.toAbsolutePath().normalize();
            if (!normalized.startsWith(SCRIPT_DIR.toAbsolutePath().normalize()) && !normalized.startsWith(PACKAGE_DIR.toAbsolutePath().normalize()))
                return fail(source, "Invalid script path.");
            if (!Files.isRegularFile(normalized)) return fail(source, "Script not found: " + normalized.getFileName());

            LuaRenderBridge.clear();
            Globals globals = JsePlatform.standardGlobals();
            LuaTable api = new LuaTable();
            api.set("on", new TwoArgFunction() {
                @Override public LuaValue call(LuaValue event, LuaValue callback) {
                    LuaEventBus.subscribe(event.checkjstring(), callback);
                    return LuaValue.TRUE;
                }
            });
            api.set("systemChat", new OneArgFunction() {
                @Override public LuaValue call(LuaValue value) { localMessage(value.checkjstring()); return LuaValue.NIL; }
            });
            api.set("clientChat", api.get("systemChat"));
            api.set("chat", api.get("systemChat"));
            api.set("log", new OneArgFunction() {
                @Override public LuaValue call(LuaValue value) { L2M.LOGGER.info("[Lua] {}", value.tojstring()); return LuaValue.NIL; }
            });
            api.set("registerBlock", new TwoArgFunction() {
                @Override public LuaValue call(LuaValue name, LuaValue properties) {
                    writeDefinition("blocks", normalized, name.checkjstring(), properties.checktable());
                    return LuaValue.TRUE;
                }
            });
            api.set("registerItem", new TwoArgFunction() {
                @Override public LuaValue call(LuaValue name, LuaValue properties) {
                    LuaContentApi.save("items", name.checkjstring(), properties.checktable(), normalized.getFileName().toString());
                    return LuaValue.TRUE;
                }
            });
            api.set("defineCategory", new TwoArgFunction() {
                @Override public LuaValue call(LuaValue name, LuaValue properties) {
                    LuaContentApi.save("categories", name.checkjstring(), properties.checktable(), normalized.getFileName().toString());
                    return LuaValue.TRUE;
                }
            });
            api.set("drawText", new VarArgFunction() {
                @Override public Varargs invoke(Varargs args) {
                    LuaRenderBridge.addText(args.arg(1).checkint(), args.arg(2).checkint(), args.arg(3).checkjstring(),
                        args.narg() >= 4 ? args.arg(4).checkint() : 0xFFFFFFFF);
                    return LuaValue.NIL;
                }
            });
            api.set("drawRect", new VarArgFunction() {
                @Override public Varargs invoke(Varargs args) {
                    LuaRenderBridge.addRect(args.arg(1).checkint(), args.arg(2).checkint(), args.arg(3).checkint(),
                        args.arg(4).checkint(), args.arg(5).checkint());
                    return LuaValue.NIL;
                }
            });
            api.set("clearDraw", new ZeroArgFunction() {
                @Override public LuaValue call() { LuaRenderBridge.clear(); return LuaValue.NIL; }
            });
            api.set("giveItem", new TwoArgFunction() {
                @Override public LuaValue call(LuaValue itemValue, LuaValue countValue) {
                    String itemId = itemValue.checkjstring();
                    int count = countValue.checkint();
                    if (count < 1 || count > 64) throw new LuaError("Item count must be between 1 and 64.");
                    net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(itemId);
                    if (id == null || !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(id))
                        throw new LuaError("Unknown item: " + itemId);
                    var connection = Minecraft.getInstance().getConnection();
                    if (connection == null) throw new LuaError("You must be in a world to give items.");
                    connection.sendCommand("give @s " + id + " " + count);
                    return LuaValue.TRUE;
                }
            });
            LuaWorldApi.install(api);
            LuaContentApi.install(api);
            globals.set("l2m", api);
            globals.load(Files.readString(normalized), "@" + normalized.getFileName()).call();
            String success = "Executed " + normalized.getFileName();
            LuaDeveloperConsole.log("SCRIPT OK: " + normalized.getFileName());
            if (source == null) localMessage("Automatically loaded " + normalized.getFileName());
            else source.sendSuccess(() -> Component.literal("L2M: " + success), false);
            return 1;
        } catch (IOException | LuaError | IllegalArgumentException e) {
            L2M.LOGGER.error("Lua script failed: {}", script, e);
            LuaDeveloperConsole.log("SCRIPT ERROR: " + script.getFileName() + " - " + safeMessage(e));
            return fail(source, safeMessage(e));
        }
    }

    private static void writeDefinition(String type, Path script, String name, LuaTable properties) {
        if (!name.matches("[a-z0-9_]{1,48}")) throw new LuaError("Content id must use lowercase letters, numbers and underscores.");
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("id", name);
        definition.put("source", script.getFileName().toString());
        if (type.equals("blocks")) {
            definition.put("displayName", properties.get("displayName").optjstring(name));
            definition.put("hardness", properties.get("hardness").optdouble(1.5));
            definition.put("requiresTool", properties.get("requiresTool").optboolean(false));
        } else {
            definition.put("displayName", properties.get("displayName").optjstring(name));
            definition.put("stackSize", properties.get("stackSize").optint(64));
        }
        try {
            Path output = ROOT.resolve("generated").resolve(type).resolve(name + ".json");
            Files.createDirectories(output.getParent());
            Files.writeString(output, GSON.toJson(definition));
            localMessage("L2M: " + type.substring(0, type.length() - 1) + " definition saved. Restart Minecraft to register it.");
        } catch (IOException e) {
            throw new LuaError("Could not save definition: " + e.getMessage());
        }
    }

    private static void localMessage(String message) {
        if (message.isBlank() || message.length() > 256) throw new LuaError("Message must contain 1-256 characters.");
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui == null) throw new LuaError("Chat UI is not available.");
        minecraft.gui.getChat().addMessage(Component.literal("[L2M] " + message));
    }

    private static int fail(CommandSourceStack source, String message) {
        if (source == null) localMessage(message);
        else source.sendFailure(Component.literal("L2M: " + message));
        return 0;
    }

    private static boolean validId(String value) {
        return ID.matcher(value).matches() && !value.equals(".") && !value.equals("..");
    }
}
