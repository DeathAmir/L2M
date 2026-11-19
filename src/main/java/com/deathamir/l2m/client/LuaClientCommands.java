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
import net.minecraft.commands.CommandSourceStack;
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

    private static int runScript(CommandSourceStack source, String fileName) {
        if (!fileName.matches("[A-Za-z0-9_-]+")) {
            source.sendFailure(Component.literal("L2M: invalid script name."));
            return 0;
        }

        try {
            Files.createDirectories(SCRIPT_DIR);
        } catch (IOException exception) {
            source.sendFailure(Component.literal("L2M: could not create config/l2m/scripts."));
            return 0;
        }

        Path script = SCRIPT_DIR.resolve(fileName + ".lua").normalize();
        if (!script.startsWith(SCRIPT_DIR)) {
            source.sendFailure(Component.literal("L2M: invalid script path."));
            return 0;
        }

        try {
            if (!Files.isRegularFile(script)) {
                source.sendFailure(Component.literal("L2M: script not found: " + fileName + ".lua"));
                return 0;
            }

            Globals globals = JsePlatform.standardGlobals();
            LuaValue api = LuaValue.tableOf();
            api.set("chat", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue message) {
                    sendLuaChat(message.checkjstring());
                    return LuaValue.NIL;
                }
            });
            globals.set("l2m", api);
            globals.load(Files.readString(script), "@" + script.getFileName()).call();
            source.sendSuccess(() -> Component.literal("L2M: executed " + script.getFileName()), false);
            return 1;
        } catch (IOException | LuaError | IllegalArgumentException exception) {
            L2M.LOGGER.error("Failed to execute Lua script {}", script, exception);
            source.sendFailure(Component.literal("L2M: " + exception.getMessage()));
            return 0;
        }
    }

    private static int sendChat(CommandSourceStack source, String message) {
        try {
            sendLuaChat(message);
            return 1;
        } catch (LuaError exception) {
            source.sendFailure(Component.literal("L2M: " + exception.getMessage()));
            return 0;
        }
    }

    private static void sendLuaChat(String message) {
        if (message.isBlank()) {
            throw new LuaError("Chat message cannot be empty.");
        }
        if (message.length() > 256) {
            throw new LuaError("Chat messages cannot exceed 256 characters.");
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.connection == null) {
            throw new LuaError("You must be in a world to send chat messages.");
        }
        minecraft.player.connection.sendChat(message);
    }
}
