local damageCooldown = 0

l2m.log("Event Totem Test loaded")
l2m.systemChat("Event Totem Test active. Take damage to receive a Totem of Undying.")

l2m.on("player_hurt", function(amount, damageType, health)
    if damageCooldown > 0 then
        l2m.log("Damage event received during cooldown")
        return
    end

    damageCooldown = 40
    l2m.giveItem("minecraft:totem_of_undying", 1)
    l2m.systemChat("player_hurt fired: +" .. tostring(amount) .. " damage event. Totem requested.")
    l2m.log("player_hurt | type=" .. tostring(damageType) .. " | health=" .. tostring(health))
end)

l2m.on("player_eat", function(itemId, count)
    l2m.systemChat("player_eat fired: " .. itemId)
    l2m.log("player_eat | item=" .. itemId .. " | count=" .. tostring(count))
end)

l2m.on("client_tick", function()
    if damageCooldown > 0 then
        damageCooldown = damageCooldown - 1
    end
end)
