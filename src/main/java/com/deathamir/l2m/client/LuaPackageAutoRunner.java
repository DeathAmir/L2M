package com.deathamir.l2m.client;

import com.deathamir.l2m.L2M;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = L2M.MODID, value = Dist.CLIENT)
public final class LuaPackageAutoRunner {
    private LuaPackageAutoRunner() {}

    @SubscribeEvent
    public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft.getInstance().execute(LuaClientCommands::runAutoPackages);
    }
}
