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
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import com.deathamir.l2m.L2M;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    private LuaClientCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("l2m")
            .then(Commands.literal("run")
                .then(Commands.argument("file", StringArgumentType.word())
                    .executes(c -> runScript(c.getSource(), SCRIPT_DIR.resolve(StringArgumentType.getString(c, "file") + ".lua")))))
            .then(Commands.literal("package")
                .then(Commands.literal("run")
                    .then(Commands.argument("package", StringArgumentType.word())
                        .then(Commands.argument("script", StringArgumentType.word())
                            .executes(c -> runPackageScript(c.getSource(),
                                StringArgumentType.getString(c, "package"),
                                StringArgumentType.getString(c, "script")))))))
            .then(Commands.literal("packages")
                .executes(c -> listPackages(c.getSource()))));
    }

    private static int runPackageScript(CommandSourceStack source, String packageId, String scriptName) {
        if (!validId(packageId) || !validId(scriptName)) {
            source.sendFailure(Component.literal("L2M: invalid package or script id."));
            return 0;
        }
        return runScript(source, PACKAGE_DIR.resolve(packageId).resolve("scripts").resolve(scriptName + ".lua"));
    }

    private static int listPackages(CommandSourceStack source) {
        try {
            Files.createDirectories(PACKAGE_DIR);
            try (var entries = Files.list(PACKAGE_DIR)) {
                var names = entries.filter(Files::isDirectory).map(p -> p.getFileName().toString()).sorted().toList();
                source.sendSuccess(() -> Component.literal(names.isEmpty()
                    ? "L2M: no packages installed."
                    : "L2M packages: " + String.join(", ", names)), false);
            }
            return 1;
        } catch (IOException e) {
            source.sendFailure(Component.literal("L2M: could not read package directory."));
            return 0;
        }
    }

    private static int runScript(CommandSourceStack source, Path script) {
        try {
            Files.createDirectories(SCRIPT_DIR);
            Files.createDirectories(PACKAGE_DIR);
            Path normalized = script.toAbsolutePath().normalize();
            if (!normalized.startsWith(SCRIPT_DIR.toAbsolutePath().normalize())
                    && !normalized.startsWith(PACKAGE_DIR.toAbsolutePath().normalize())) {
                source.sendFailure(Component.literal("L2M: invalid script path."));
                return 0;
            }
            if (!Files.isRegularFile(normalized)) {
                source.sendFailure(Component.literal("L2M: script not found: " + normalized.getFileName()));
                return 0;
            }

            Globals globals = JsePlatform.standardGlobals();
            LuaTable api = new LuaTable();
            api.set("systemChat", new OneArgFunction() {
                @Override public LuaValue call(LuaValue value) {
                    localMessage(value.checkjstring());
                    return LuaValue.NIL;
                }
            });
            api.set("clientChat", api.get("systemChat"));
            api.set("log", new OneArgFunction() {
                @Override public LuaValue call(LuaValue value) {
                    L2M.LOGGER.info("[Lua] {}", value.tojstring());
                    return LuaValue.NIL;
                }
            });
            api.set("registerBlock", new TwoArgFunction() {
                @Override public LuaValue call(LuaValue name, LuaValue properties) {
                    writeBlockDefinition(normalized, name.checkjstring(), properties.checktable());
                    return LuaValue.TRUE;
                }
            });
            api.set("chat", api.get("systemChat"));
            globals.set("l2m", api);
            globals.load(Files.readString(normalized), "@" + normalized.getFileName()).call();
            source.sendSuccess(() -> Component.literal("L2M: executed " + normalized.getFileName()), false);
            return 1;
        } catch (IOException | LuaError | IllegalArgumentException e) {
            L2M.LOGGER.error("Lua script failed: {}", script, e);
            source.sendFailure(Component.literal("L2M: " + safeMessage(e)));
            return 0;
        }
    }

    private static void writeBlockDefinition(Path script, String name, LuaTable properties) {
        if (!name.matches("[a-z0-9_]{1,48}")) throw new LuaError("Block id must use lowercase letters, numbers and underscores.");
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("id", name);
        definition.put("source", script.getFileName().toString());
        definition.put("displayName", properties.get("displayName").optjstring(name));
        definition.put("hardness", properties.get("hardness").optdouble(1.5));
        definition.put("requiresTool", properties.get("requiresTool").optboolean(false));
        definition.put("note", "Declarative definition. Java registry integration is required to create a live Minecraft block.");
        try {
            Path output = ROOT.resolve("generated").resolve("blocks").resolve(name + ".json");
            Files.createDirectories(output.getParent());
            Files.writeString(output, GSON.toJson(definition));
            localMessage("L2M: block definition saved: " + name + " (restart/rebuild integration required)");
        } catch (IOException e) {
            throw new LuaError("Could not save block definition: " + e.getMessage());
        }
    }

    private static void localMessage(String message) {
        if (message.isBlank()) throw new LuaError("Message cannot be empty.");
        if (message.length() > 256) throw new LuaError("Messages cannot exceed 256 characters.");
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui == null) throw new LuaError("Chat UI is not available.");
        minecraft.gui.getChat().addMessage(Component.literal("[L2M] " + message));
    }

    private static boolean validId(String value) {
        return ID.matcher(value).matches() && !value.equals(".") && !value.equals("..");
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
