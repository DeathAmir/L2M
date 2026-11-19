package com.deathamir.l2m.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import com.deathamir.l2m.L2M;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = L2M.MODID, value = Dist.CLIENT)
public final class LuaClientCommands {
    private static final Path SCRIPT_DIR = Path.of("config", "l2m", "scripts");

    private LuaClientCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("l2m")
            .then(Commands.literal("run")
                .then(Commands.argument("file", StringArgumentType.word())
                    .executes(context -> runScript(context.getSource(), StringArgumentType.getString(context, "file")))))
            .then(Commands.literal("chat")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(context -> sendChat(context.getSource(), StringArgumentType.getString(context, "message"))))));
    }

    private static int runScript(net.minecraft.commands.CommandSourceStack source, String fileName) {
        if (!fileName.matches("[A-Za-z0-9_-]+")) {
            source.sendFailure(Component.literal("Invalid script name."));
            return 0;
        }

        try {\n            Files.createDirectories(SCRIPT_DIR);\n        } catch (IOException exception) {\n            source.sendFailure(Component.literal("L2M: could not create scripts directory."));\n            return 0;\n        }\n\n        Path script = SCRIPT_DIR.resolve(fileName + ".lua").normalize();
        if (!script.startsWith(SCRIPT_DIR)) {
            source.sendFailure(Component.literal("Invalid script path."));
            return 0;
        }

        try {
            if (!Files.isRegularFile(script)) {
                source.sendFailure(Component.literal("Script not found: " + fileName + ".lua"));
                return 0;
            }

            String code = Files.readString(script);
            Globals globals = JsePlatform.standardGlobals();
            LuaValue api = LuaValue.tableOf();
            api.set("chat", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue message) {
                    String text = message.checkjstring();
                    if (text.length() > 256) {
                        throw new LuaError("Chat messages cannot exceed 256 characters.");
                    }
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft.player == null || minecraft.player.connection == null) {
                        throw new LuaError("You must be in a world to send chat messages.");
                    }
                    minecraft.player.connection.sendChat(text);
                    return LuaValue.NIL;
                }
            });
            globals.set("l2m", api);
            globals.load(code, "@" + script.getFileName()).call();
            source.sendSuccess(() -> Component.literal("L2M: executed " + script.getFileName()), false);
            return 1;
        } catch (IOException | LuaError | IllegalArgumentException exception) {
            L2M.LOGGER.error("Failed to execute Lua script {}", script, exception);
            source.sendFailure(Component.literal("L2M: " + exception.getMessage()));
            return 0;
        }
    }

    private static int sendChat(net.minecraft.commands.CommandSourceStack source, String message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null) {
            source.sendFailure(Component.literal("You must be in a world to send chat."));
            return 0;
        }
        if (message.length() > 256) {
            source.sendFailure(Component.literal("Chat messages cannot exceed 256 characters."));
            return 0;
        }
        minecraft.player.connection.sendChat(message);
        return 1;
    }
}
