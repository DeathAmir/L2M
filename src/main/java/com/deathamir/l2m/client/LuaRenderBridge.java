package com.deathamir.l2m.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = "l2m", value = Dist.CLIENT)
public final class LuaRenderBridge {
    private sealed interface DrawCall permits TextCall, RectCall {}
    private record TextCall(int x, int y, String text, int color) implements DrawCall {}
    private record RectCall(int x, int y, int width, int height, int color) implements DrawCall {}
    private static final List<DrawCall> CALLS = new CopyOnWriteArrayList<>();

    private LuaRenderBridge() {}

    public static void clear() {
        CALLS.clear();
    }

    public static void addText(int x, int y, String text, int color) {
        if (text.length() > 512) throw new IllegalArgumentException("Draw text is limited to 512 characters.");
        CALLS.add(new TextCall(x, y, text, color));
    }

    public static void addRect(int x, int y, int width, int height, int color) {
        if (width < 0 || height < 0 || width > 4096 || height > 4096)
            throw new IllegalArgumentException("Draw rectangle dimensions are out of range.");
        CALLS.add(new RectCall(x, y, width, height, color));
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font == null) return;
        var graphics = event.getGuiGraphics();
        for (DrawCall call : CALLS) {
            if (call instanceof TextCall text) {
                graphics.drawString(minecraft.font, Component.literal(text.text()), text.x(), text.y(), text.color(), true);
            } else if (call instanceof RectCall rect) {
                graphics.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), rect.color());
            }
        }
    }
}
