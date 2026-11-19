package com.deathamir.l2m.client;

import org.luaj.vm2.LuaValue;

import com.deathamir.l2m.L2M;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

@EventBusSubscriber(modid = L2M.MODID, value = Dist.CLIENT)
public final class LuaPackageAutoRunner {
    private LuaPackageAutoRunner() {}

    @SubscribeEvent
    public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft.getInstance().execute(() -> {
            var player = Minecraft.getInstance().player;
            if (player != null) LuaEventBus.dispatch("player_join", LuaValue.valueOf(player.getGameProfile().getName()));
            LuaClientCommands.runAutoPackages();
        });
    }

    @SubscribeEvent
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        var player = event.getPlayer();
        if (player != null) {
            LuaEventBus.dispatch("player_leave", LuaValue.valueOf(player.getGameProfile().getName()));
        }
        LuaEventBus.clear();
        LuaRenderBridge.clear();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player != null) LuaEventBus.dispatch("client_tick");
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingDamageEvent.Post event) {
        var player = Minecraft.getInstance().player;
        if (player == null || event.getEntity() != player) return;
        String damageType = event.getSource().getMsgId();
        LuaEventBus.dispatch("player_hurt",
            LuaValue.valueOf(event.getNewDamage()),
            LuaValue.valueOf(damageType),
            LuaValue.valueOf(player.getHealth()));
    }

    @SubscribeEvent
    public static void onPlayerEat(LivingEntityUseItemEvent.Finish event) {
        var player = Minecraft.getInstance().player;
        if (player == null || event.getEntity() != player) return;
        if (event.getItem().getFoodProperties(player) == null) return;
        String itemId = BuiltInRegistries.ITEM.getKey(event.getItem().getItem()).toString();
        LuaEventBus.dispatch("player_eat",
            LuaValue.valueOf(itemId),
            LuaValue.valueOf(event.getItem().getCount()));
    }
}
