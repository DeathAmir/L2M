package com.deathamir.l2m.client;

import java.util.List;

import com.google.gson.JsonObject;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class LuaInventoryScreen extends Screen {
    private final String initialCategory;
    private String category = "";
    private List<JsonObject> categories = List.of();
    private List<JsonObject> items = List.of();
    private int page;

    public LuaInventoryScreen(String category) {
        super(Component.literal("L2M Inventory"));
        this.initialCategory = category == null ? "" : category;
    }

    @Override protected void init() {
        categories = LuaContentApi.readAll(LuaContentApi.CATEGORIES);
        items = LuaContentApi.readAll(LuaContentApi.ITEMS);
        category = initialCategory;
        if (!category.isBlank() && categories.stream().noneMatch(c -> c.get("id").getAsString().equals(category))) category = "";
        int x = 12;
        addRenderableWidget(Button.builder(Component.literal("All"), b -> { category = ""; page = 0; refresh(); }).bounds(x, 32, 48, 20).build());
        x += 52;
        for (JsonObject value : categories) {
            String id = value.get("id").getAsString();
            String name = value.get("displayName").getAsString();
            int width = Math.min(100, Math.max(54, name.length() * 6 + 16));
            if (x + width > this.width - 12) break;
            addRenderableWidget(Button.builder(Component.literal(name), b -> { category = id; page = 0; refresh(); }).bounds(x, 32, width, 20).build());
            x += width + 4;
        }
        List<JsonObject> visible = visibleItems();
        int start = page * 6;
        for (int i = start; i < Math.min(visible.size(), start + 6); i++) {
            JsonObject item = visible.get(i);
            String id = item.get("id").getAsString();
            int y = 78 + (i - start) * 34;
            addRenderableWidget(Button.builder(Component.literal("Give"), b -> give(id)).bounds(width - 112, y, 44, 20).build());
            addRenderableWidget(Button.builder(Component.literal("Equip"), b -> equip(id)).bounds(width - 64, y, 52, 20).build());
        }
        if (page > 0) addRenderableWidget(Button.builder(Component.literal("Previous"), b -> { page--; refresh(); }).bounds(12, height - 30, 70, 20).build());
        if ((page + 1) * 6 < visible.size()) addRenderableWidget(Button.builder(Component.literal("Next"), b -> { page++; refresh(); }).bounds(88, height - 30, 70, 20).build());
    }

    private List<JsonObject> visibleItems() {
        return items.stream().filter(item -> category.isBlank() || item.has("category") && item.get("category").getAsString().equals(category)).toList();
    }

    private void refresh() { clearWidgets(); init(); }

    private void give(String id) {
        JsonObject item = LuaContentApi.readItem(id);
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) connection.sendCommand("give @s " + item.get("minecraftItem").getAsString() + " 1");
    }

    private void equip(String id) {
        JsonObject item = LuaContentApi.readItem(id);
        String slot = item.has("slot") ? item.get("slot").getAsString() : "mainhand";
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) connection.sendCommand("item replace entity @s " + LuaContentApi.targetSlot(slot) + " with " + item.get("minecraftItem").getAsString() + " 1");
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, "L2M CUSTOM INVENTORY", width / 2, 12, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
        List<JsonObject> visible = visibleItems();
        int start = page * 6;
        for (int i = start; i < Math.min(visible.size(), start + 6); i++) {
            JsonObject item = visible.get(i);
            int y = 78 + (i - start) * 34;
            ResourceLocation resource = ResourceLocation.tryParse(item.get("icon").getAsString());
            if (resource == null || !BuiltInRegistries.ITEM.containsKey(resource))
                resource = ResourceLocation.tryParse(item.get("minecraftItem").getAsString());
            if (resource != null && BuiltInRegistries.ITEM.containsKey(resource)) {
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(resource));
                graphics.renderItem(stack, 16, y);
                graphics.renderItemDecorations(font, stack, 16, y);
            }
            graphics.drawString(font, item.get("displayName").getAsString(), 42, y + 2, 0xFFFFFF);
            String desc = item.has("description") ? item.get("description").getAsString() : "";
            graphics.drawString(font, desc, 42, y + 15, 0xA0A0A0);
        }
        if (visible.isEmpty()) graphics.drawCenteredString(font, "No items in this category.", width / 2, 100, 0xAAAAAA);
    }

    @Override public boolean isPauseScreen() { return false; }
}
