l2m.log("Hello World package loaded")

l2m.systemChat("Hello from L2M! This message is local and is not sent as the player.")

l2m.registerBlock("ruby_ore", {
    displayName = "Ruby Ore",
    hardness = 3.0,
    requiresTool = true
})

l2m.registerItem("ruby", {
    displayName = "Ruby",
    stackSize = 64
})

l2m.drawRect(12, 12, 190, 46, 0xCC17152B)
l2m.drawRect(12, 12, 3, 46, 0xFF9B5CFF)
l2m.drawText(24, 20, "Lua2Minecraft HUD", 0xFFFFFFFF)
l2m.drawText(24, 36, "Hello World package is running", 0xFFBFA8FF)
