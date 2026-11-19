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
import com.google.gson.GsonBuilder;\nimport com.google.gson.JsonObject;\nimport com.google.gson.JsonParser;
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
            .then(Commands.literal("packages").executes(c -> listPackages(c.getSource()))));
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
                    writeDefinition("items", normalized, name.checkjstring(), properties.checktable());
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
            globals.set("l2m", api);
            globals.load(Files.readString(normalized), "@" + normalized.getFileName()).call();
            if (source == null) localMessage("Automatically loaded " + normalized.getFileName());\n            else source.sendSuccess(() -> Component.literal("L2M: executed " + normalized.getFileName()), false);
            return 1;
        } catch (IOException | LuaError | IllegalArgumentException e) {
            L2M.LOGGER.error("Lua script failed: {}", script, e);
            return fail(source, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
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
        source.sendFailure(Component.literal("L2M: " + message));
        return 0;
    }

    private static boolean validId(String value) {
        return ID.matcher(value).matches() && !value.equals(".") && !value.equals("..");
    }
}
