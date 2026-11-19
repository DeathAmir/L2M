local rockets = 0

l2m.on("item_use_start", function(itemId, count)
    if itemId ~= "minecraft:firework_rocket" then
        return
    end

    local ok = l2m.launchRocket(2.4)
    if ok then
        rockets = rockets + 1
        l2m.log("Rocket launched: " .. tostring(rockets))
    end
end)

l2m.log("Rocket Launcher loaded. Use a firework rocket to launch a guided fireball.")
