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
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = L2M.MODID, value = Dist.CLIENT)
public final class LuaPackageAutoRunner {
    private LuaPackageAutoRunner() {}

    private static boolean isLocal(net.minecraft.world.entity.Entity entity) {
        var player = Minecraft.getInstance().player;
        return player != null && entity != null && entity.getUUID().equals(player.getUUID());
    }

    private static void dispatch(String name, LuaValue... args) {
        Minecraft.getInstance().execute(() -> LuaEventBus.dispatch(name, args));
    }

    private static String itemId(net.minecraft.world.item.ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static String blockId(net.minecraft.world.level.block.state.BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

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
        if (player != null) LuaEventBus.dispatch("player_leave", LuaValue.valueOf(player.getGameProfile().getName()));
        LuaEventBus.clear();
        LuaRenderBridge.clear();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player != null) LuaEventBus.dispatch("client_tick");
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingDamageEvent.Post event) {
        if (!isLocal(event.getEntity())) return;
        dispatch("player_hurt",
            LuaValue.valueOf(event.getNewDamage()),
            LuaValue.valueOf(event.getSource().getMsgId()),
            LuaValue.valueOf(event.getEntity().getHealth()));
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!isLocal(event.getEntity())) return;
        dispatch("player_death", LuaValue.valueOf(event.getSource().getMsgId()));
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!isLocal(event.getEntity())) return;
        dispatch("player_respawn", LuaValue.valueOf(event.getEntity().getHealth()));
    }

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (!isLocal(event.getEntity())) return;
        var target = event.getTarget();
        dispatch("player_attack",
            LuaValue.valueOf(target.getType().toString()),
            LuaValue.valueOf(target.getUUID().toString()));
    }

    @SubscribeEvent
    public static void onPlayerEat(LivingEntityUseItemEvent.Finish event) {
        if (!isLocal(event.getEntity()) || event.getItem().getFoodProperties(event.getEntity()) == null) return;
        dispatch("player_eat", LuaValue.valueOf(itemId(event.getItem())), LuaValue.valueOf(event.getItem().getCount()));
    }

    @SubscribeEvent
    public static void onPlayerUseItem(LivingEntityUseItemEvent.Start event) {
        if (!isLocal(event.getEntity())) return;
        dispatch("item_use_start", LuaValue.valueOf(itemId(event.getItem())), LuaValue.valueOf(event.getItem().getCount()));
    }

    @SubscribeEvent
    public static void onPlayerBreakBlock(BlockEvent.BreakEvent event) {
        if (!isLocal(event.getPlayer())) return;
        dispatch("block_break",
            LuaValue.valueOf(blockId(event.getState())),
            LuaValue.valueOf(event.getPos().getX()),
            LuaValue.valueOf(event.getPos().getY()),
            LuaValue.valueOf(event.getPos().getZ()));
    }

    @SubscribeEvent
    public static void onPlayerPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (!isLocal(event.getEntity())) return;
        dispatch("block_place",
            LuaValue.valueOf(blockId(event.getPlacedBlock())),
            LuaValue.valueOf(event.getPos().getX()),
            LuaValue.valueOf(event.getPos().getY()),
            LuaValue.valueOf(event.getPos().getZ()));
    }

    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        var entity = event.getEntity();
        if (entity instanceof net.minecraft.world.entity.player.Player) return;
        dispatch("entity_spawn",
            LuaValue.valueOf(entity.getType().toString()),
            LuaValue.valueOf(entity.getUUID().toString()),
            LuaValue.valueOf(entity.getX()),
            LuaValue.valueOf(entity.getY()),
            LuaValue.valueOf(entity.getZ()));
    }
}
