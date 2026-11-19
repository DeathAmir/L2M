package com.deathamir.l2m;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(L2M.MODID)
public final class L2M {
    public static final String MODID = "l2m";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public L2M(IEventBus modEventBus, ModContainer modContainer) {
        LuaPackagePreloader.preload();
        loadGeneratedContent();
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.l2m"))
            .icon(() -> new ItemStack(Items.AMETHYST_SHARD))
            .displayItems((parameters, output) -> ITEMS.getEntries().forEach(entry -> output.accept(entry.get())))
            .build());
        TABS.register(modEventBus);
        LOGGER.info("Lua2Minecraft initialized. Generated content discovered before registry freeze.");
    }

    private static void loadGeneratedContent() {
        Path root = Path.of("config", "l2m", "generated");
        loadBlocks(root.resolve("blocks"));
        loadItems(root.resolve("items"));
    }

    private static void loadBlocks(Path directory) {
        if (!Files.isDirectory(directory)) return;
        try (var files = Files.list(directory)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().forEach(path -> {
                try {
                    JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                    String id = json.has("id") ? json.get("id").getAsString() : "";
                    if (!validId(id)) throw new IllegalArgumentException("Invalid block id: " + id);
                    float hardness = json.has("hardness") ? json.get("hardness").getAsFloat() : 1.5f;
                    boolean requiresTool = json.has("requiresTool") && json.get("requiresTool").getAsBoolean();
                    var block = BLOCKS.register(id, () -> {
                        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().strength(Math.max(0, hardness));
                        if (requiresTool) properties.requiresCorrectToolForDrops();
                        return new Block(properties);
                    });
                    ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
                    LOGGER.info("Loaded generated block {}", id);
                } catch (Exception e) {
                    LOGGER.error("Could not load generated block definition {}", path, e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Could not scan generated blocks", e);
        }
    }

    private static void loadItems(Path directory) {
        if (!Files.isDirectory(directory)) return;
        try (var files = Files.list(directory)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().forEach(path -> {
                try {
                    JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                    String id = json.has("id") ? json.get("id").getAsString() : "";
                    if (!validId(id)) throw new IllegalArgumentException("Invalid item id: " + id);
                    int stackSize = json.has("stackSize") ? json.get("stackSize").getAsInt() : 64;
                    if (stackSize < 1 || stackSize > 99) throw new IllegalArgumentException("Invalid stack size for " + id);
                    ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(stackSize)));
                    LOGGER.info("Loaded generated item {}", id);
                } catch (Exception e) {
                    LOGGER.error("Could not load generated item definition {}", path, e);
                }
            });
        } catch (Exception e) {
            LOGGER.error("Could not scan generated items", e);
        }
    }

    private static boolean validId(String id) {
        return id.matches("[a-z0-9_]{1,48}");
    }
}
