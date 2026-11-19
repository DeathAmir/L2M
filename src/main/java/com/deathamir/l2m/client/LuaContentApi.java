package com.deathamir.l2m.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class LuaContentApi {
    public static final Path ROOT = Path.of("config", "l2m", "generated");
    public static final Path CATEGORIES = ROOT.resolve("categories");
    public static final Path ITEMS = ROOT.resolve("items");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private LuaContentApi() {}

    public static void install(LuaTable api) {
        api.set("defineCategory", new TwoArgFunction() {
            @Override public LuaValue call(LuaValue id, LuaValue props) {
                save("categories", id.checkjstring(), props.checktable(), null);
                return LuaValue.TRUE;
            }
        });
        api.set("registerItem", new TwoArgFunction() {
            @Override public LuaValue call(LuaValue id, LuaValue props) {
                save("items", id.checkjstring(), props.checktable(), null);
                return LuaValue.TRUE;
            }
        });
        api.set("giveCustomItem", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                JsonObject item = readItem(args.arg(1).checkjstring());
                int count = args.narg() > 1 ? args.arg(2).checkint() : 1;
                if (count < 1 || count > 64) throw new LuaError("Count must be between 1 and 64.");
                send("give @s " + required(item, "minecraftItem") + " " + count);
                return LuaValue.TRUE;
            }
        });
        api.set("equipItem", new OneArgFunction() {
            @Override public LuaValue call(LuaValue id) {
                JsonObject item = readItem(id.checkjstring());
                String slot = item.has("slot") ? item.get("slot").getAsString() : "mainhand";
                send("item replace entity @s " + targetSlot(slot) + " with " + required(item, "minecraftItem") + " 1");
                return LuaValue.TRUE;
            }
        });
        api.set("openInventory", new OneArgFunction() {
            @Override public LuaValue call(LuaValue category) {
                String selected = category.isnil() ? "" : category.checkjstring();
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new LuaInventoryScreen(selected)));
                return LuaValue.TRUE;
            }
        });
        api.set("listCategories", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                LuaTable result = new LuaTable();
                List<JsonObject> values = readAll(CATEGORIES);
                for (int i = 0; i < values.size(); i++) result.set(i + 1, LuaValue.valueOf(values.get(i).get("id").getAsString()));
                return result;
            }
        });
    }

    public static void save(String type, String id, LuaTable props, String source) {
        if (!id.matches("[a-z0-9_]{1,48}")) throw new LuaError("Content id must use lowercase letters, numbers and underscores.");
        JsonObject data = new JsonObject();
        data.addProperty("id", id);
        data.addProperty("displayName", props.get("displayName").optjstring(id));
        if (source != null) data.addProperty("source", source);
        if (type.equals("categories")) {
            data.addProperty("icon", props.get("icon").optjstring("minecraft:chest"));
            data.addProperty("description", props.get("description").optjstring(""));
            data.addProperty("sortOrder", props.get("sortOrder").optint(0));
        } else {
            String base = props.get("minecraftItem").optjstring("minecraft:stone");
            ResourceLocation resource = ResourceLocation.tryParse(base);
            if (resource == null || !BuiltInRegistries.ITEM.containsKey(resource))
                throw new LuaError("minecraftItem must be a registered Minecraft item id.");
            String category = props.get("category").optjstring("misc");
            if (!category.matches("[a-z0-9_]{1,48}")) throw new LuaError("Invalid item category.");
            String slot = props.get("slot").optjstring("mainhand");
            if (!List.of("mainhand", "offhand", "shield", "head", "chest", "legs", "feet").contains(slot))
                throw new LuaError("Invalid equipment slot.");
            int stack = props.get("stackSize").optint(64);
            if (stack < 1 || stack > 99) throw new LuaError("stackSize must be between 1 and 99.");
            data.addProperty("category", category);
            data.addProperty("minecraftItem", base);
            data.addProperty("slot", slot);
            data.addProperty("stackSize", stack);
            data.addProperty("description", props.get("description").optjstring(""));
            data.addProperty("icon", props.get("icon").optjstring(base));
            data.addProperty("texture", props.get("texture").optjstring(""));
            data.addProperty("armor", props.get("armor").optint(0));
            data.addProperty("toughness", props.get("toughness").optdouble(0));
        }
        Path output = ROOT.resolve(type).resolve(id + ".json");
        try {
            Files.createDirectories(output.getParent());
            Files.writeString(output, GSON.toJson(data));
        } catch (IOException e) {
            throw new LuaError("Could not save content definition: " + e.getMessage());
        }
    }

    public static List<JsonObject> readAll(Path folder) {
        List<JsonObject> values = new ArrayList<>();
        if (!Files.isDirectory(folder)) return values;
        try (var files = Files.list(folder)) {
            files.filter(p -> p.toString().endsWith(".json")).sorted(Comparator.comparing(Path::toString)).forEach(path -> {
                try { values.add(JsonParser.parseString(Files.readString(path)).getAsJsonObject()); } catch (Exception ignored) {}
            });
        } catch (IOException ignored) {}
        return values;
    }

    public static JsonObject readItem(String id) {
        if (!id.matches("[a-z0-9_]{1,48}")) throw new LuaError("Invalid item id.");
        Path path = ITEMS.resolve(id + ".json");
        if (!Files.isRegularFile(path)) throw new LuaError("Unknown custom item: " + id);
        try { return JsonParser.parseString(Files.readString(path)).getAsJsonObject(); }
        catch (Exception e) { throw new LuaError("Could not read custom item: " + id); }
    }

    public static String targetSlot(String slot) {
        return switch (slot) {
            case "head" -> "armor.head";
            case "chest" -> "armor.chest";
            case "legs" -> "armor.legs";
            case "feet" -> "armor.feet";
            case "offhand", "shield" -> "weapon.offhand";
            case "mainhand" -> "weapon.mainhand";
            default -> throw new LuaError("Unsupported equipment slot: " + slot);
        };
    }

    private static String required(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).getAsString().isBlank()) throw new LuaError("Missing item property: " + key);
        return json.get(key).getAsString();
    }

    private static void send(String command) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) throw new LuaError("You must be in a world to use custom items.");
        connection.sendCommand(command);
    }
}
