# Equipment Catalog Demo

Demonstrates L2M data-driven categories, icon metadata, item definitions and the categorized inventory screen.

## Install

Copy this folder to `config/l2m/packages/equipment-catalog` and join/rejoin a world.

## Commands

- `/l2m inventory` opens the custom catalog.
- Choose a category to filter items.
- **Give** requests the mapped vanilla Minecraft item.
- **Equip** requests the mapped vanilla item in the configured equipment slot.

## Lua API

```lua
l2m.defineCategory("shields", {
    displayName = "Shields",
    icon = "minecraft:shield",
    description = "Defensive equipment"
})

l2m.registerItem("iron_guard_shield", {
    displayName = "Iron Guard Shield",
    description = "A shield for the offhand slot",
    category = "shields",
    minecraftItem = "minecraft:shield",
    icon = "minecraft:shield",
    slot = "offhand",
    stackSize = 1
})

l2m.giveCustomItem("iron_guard_shield", 1)
l2m.equipItem("iron_guard_shield")
l2m.openInventory("shields")
```

## Item properties

- `displayName`: shown name
- `description`: item description in the catalog
- `category`: category ID
- `minecraftItem`: existing Minecraft item used as the functional backing item
- `icon`: registered Minecraft item ID used as the icon
- `texture`: optional resource location for a 16x16 texture in the mod resource pack, such as `l2m:textures/items/royal_shield.png`
- `slot`: `mainhand`, `offhand`, `shield`, `head`, `chest`, `legs`, or `feet`
- `stackSize`: stack size metadata
- `armor` and `toughness`: descriptive metadata; actual combat stats come from the mapped Minecraft item

## Important

This release provides a real category/catalog UI and uses actual vanilla items for inventory, shield behavior and armor equipment. It does not yet register brand-new Minecraft item IDs or custom armor attributes. Commands such as `/give` and `/item replace` require permission on the current world/server.
