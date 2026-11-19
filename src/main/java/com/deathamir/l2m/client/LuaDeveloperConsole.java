package com.deathamir.l2m.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class LuaDeveloperConsole extends Screen {
    private static final int MAX_LINES = 120;
    private static final List<String> LOGS = new CopyOnWriteArrayList<>();

    public LuaDeveloperConsole() {
        super(Component.literal("Lua2Minecraft Developer Console"));
    }

    public static void log(String message) {
        LOGS.add(message == null ? "Unknown error" : message);
        while (LOGS.size() > MAX_LINES) LOGS.remove(0);
    }

    public static void clear() {
        LOGS.clear();
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        addRenderableWidget(Button.builder(Component.literal("Clear log"), button -> clear())
            .bounds(center - 104, this.height - 34, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
            .bounds(center + 4, this.height - 34, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(24, 20, this.width - 24, this.height - 62, 0xEE101820);
        graphics.fill(24, 20, this.width - 24, 48, 0xFF075B9A);
        graphics.drawString(this.font, "Lua2Minecraft / Developer Console", 36, 30, 0xFFFFFFFF, false);
        graphics.drawString(this.font, "Events: " + LuaEventBus.listenerCount() + " listeners", 36, 57, 0xFF8CCBEE, false);
        List<String> snapshot = List.copyOf(LOGS);
        int visible = Math.max(0, (this.height - 112) / 12);
        int start = Math.max(0, snapshot.size() - visible);
        int y = 76;
        for (int i = start; i < snapshot.size(); i++) {
            String line = snapshot.get(i);
            graphics.drawString(this.font, line.length() > 110 ? line.substring(0, 107) + "..." : line,
                36, y, line.contains("ERROR") ? 0xFFFF7777 : 0xFFE1EAF0, false);
            y += 12;
        }
        if (snapshot.isEmpty()) graphics.drawString(this.font, "No runtime messages yet. Run a Lua script to see activity here.",
            36, 80, 0xFF9AAAB5, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
