package com.deathamir.l2m;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(L2M.MODID)
public final class L2M {
    public static final String MODID = "l2m";
    public static final Logger LOGGER = LogUtils.getLogger();

    public L2M(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Lua2Minecraft initialized with LuaJ 5.1.");
    }
}
