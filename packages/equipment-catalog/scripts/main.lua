l2m.defineCategory("shields", {
    displayName = "Shields",
    icon = "minecraft:shield",
    description = "Defensive offhand equipment",
    sortOrder = 1
})

l2m.defineCategory("armor", {
    displayName = "Armor",
    icon = "minecraft:diamond_chestplate",
    description = "Wearable armor pieces",
    sortOrder = 2
})

l2m.defineCategory("consumables", {
    displayName = "Consumables",
    icon = "minecraft:golden_apple",
    description = "Food and usable items",
    sortOrder = 3
})

l2m.registerItem("iron_guard_shield", {
    displayName = "Iron Guard Shield",
    description = "A shield equipped in the offhand slot.",
    category = "shields",
    minecraftItem = "minecraft:shield",
    icon = "minecraft:shield",
    slot = "offhand",
    stackSize = 1
})

l2m.registerItem("royal_chestplate", {
    displayName = "Royal Chestplate",
    description = "A diamond chestplate for the chest armor slot.",
    category = "armor",
    minecraftItem = "minecraft:diamond_chestplate",
    icon = "minecraft:diamond_chestplate",
    slot = "chest",
    stackSize = 1,
    armor = 8,
    toughness = 2
})

l2m.registerItem("iron_helmet", {
    displayName = "Iron Helmet",
    description = "A helmet equipped in the head armor slot.",
    category = "armor",
    minecraftItem = "minecraft:iron_helmet",
    icon = "minecraft:iron_helmet",
    slot = "head",
    stackSize = 1,
    armor = 2
})

l2m.registerItem("golden_apple_supply", {
    displayName = "Golden Apple",
    description = "A consumable golden apple.",
    category = "consumables",
    minecraftItem = "minecraft:golden_apple",
    icon = "minecraft:golden_apple",
    slot = "mainhand",
    stackSize = 16
})

l2m.openInventory()
l2m.log("Equipment Catalog loaded. Open it with /l2m inventory.")
