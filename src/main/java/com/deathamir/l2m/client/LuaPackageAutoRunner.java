package com.deathamir.l2m.client;

import org.luaj.vm2.LuaValue;

import com.deathamir.l2m.L2M;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = L2M.MODID, value = Dist.CLIENT)
public final class LuaPackageAutoRunner {
    private LuaPackageAutoRunner() {}

    @SubscribeEvent
    public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft.getInstance().execute(() -> {
            LuaEventBus.dispatch("player_join", LuaValue.valueOf(event.getPlayer().getGameProfile().getName()));
            LuaClientCommands.runAutoPackages();
        });
    }

    @SubscribeEvent
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        LuaEventBus.dispatch("player_leave", LuaValue.valueOf(event.getPlayer().getGameProfile().getName()));
        LuaEventBus.clear();
        LuaRenderBridge.clear();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player != null) LuaEventBus.dispatch("client_tick");
    }
}
