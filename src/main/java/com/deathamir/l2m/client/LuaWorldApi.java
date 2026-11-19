package com.deathamir.l2m.client;

import java.util.UUID;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;

public final class LuaWorldApi {
    private LuaWorldApi() {}

    public static void install(LuaTable api) {
        api.set("explodeTnt", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                double x = args.arg(1).checkdouble();
                double y = args.arg(2).checkdouble();
                double z = args.arg(3).checkdouble();
                int fuse = args.narg() >= 4 ? args.arg(4).checkint() : 40;
                if (fuse < 1 || fuse > 32767) throw new LuaError("TNT fuse must be between 1 and 32767 ticks.");
                sendCommand("summon minecraft:tnt " + x + " " + y + " " + z + " {Fuse:" + fuse + "s}");
                return LuaValue.TRUE;
            }
        });
        api.set("launchRocket", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                var player = Minecraft.getInstance().player;
                if (player == null) throw new LuaError("You must be in a world to launch a rocket.");
                double speed = args.narg() >= 1 ? args.arg(1).checkdouble() : 1.8;
                if (speed < 0.1 || speed > 8.0) throw new LuaError("Rocket speed must be between 0.1 and 8.");
                var eye = player.getEyePosition();
                var look = player.getLookAngle().normalize().scale(speed);
                double x = eye.x + look.x * 1.2;
                double y = eye.y + look.y * 1.2;
                double z = eye.z + look.z * 1.2;
                sendCommand("summon minecraft:fireball " + x + " " + y + " " + z + " {Motion:[" + look.x + "d," + look.y + "d," + look.z + "d],ExplosionPower:2b}");
                return LuaValue.TRUE;
            }
        });
        api.set("setBlock", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                int x = args.arg(1).checkint();
                int y = args.arg(2).checkint();
                int z = args.arg(3).checkint();
                String block = args.arg(4).checkjstring();
                ResourceLocation id = ResourceLocation.tryParse(block);
                if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) throw new LuaError("Unknown block: " + block);
                sendCommand("setblock " + x + " " + y + " " + z + " " + id + " replace");
                return LuaValue.TRUE;
            }
        });
        api.set("spawnParticle", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                var level = Minecraft.getInstance().level;
                if (level == null) throw new LuaError("You must be in a world to spawn particles.");
                ResourceLocation id = ResourceLocation.tryParse(args.arg(1).checkjstring());
                if (id == null || !BuiltInRegistries.PARTICLE_TYPE.containsKey(id)) throw new LuaError("Unknown particle type.");
                var type = BuiltInRegistries.PARTICLE_TYPE.get(id);
                if (!(type instanceof SimpleParticleType particle)) throw new LuaError("This particle requires parameters and is not supported by spawnParticle.");
                double x = args.arg(2).checkdouble();
                double y = args.arg(3).checkdouble();
                double z = args.arg(4).checkdouble();
                double vx = args.narg() >= 5 ? args.arg(5).checkdouble() : 0;
                double vy = args.narg() >= 6 ? args.arg(6).checkdouble() : 0;
                double vz = args.narg() >= 7 ? args.arg(7).checkdouble() : 0;
                level.addParticle(particle, x, y, z, vx, vy, vz);
                return LuaValue.TRUE;
            }
        });
        api.set("moveEntity", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                var level = Minecraft.getInstance().level;
                if (level == null) throw new LuaError("You must be in a world to control entities.");
                UUID uuid;
                try {
                    uuid = UUID.fromString(args.arg(1).checkjstring());
                } catch (IllegalArgumentException error) {
                    throw new LuaError("Invalid entity UUID.");
                }
                var entity = findEntity(level, uuid);
                if (entity == null) throw new LuaError("Entity is not currently loaded on this client.");
                entity.setPos(args.arg(2).checkdouble(), args.arg(3).checkdouble(), args.arg(4).checkdouble());
                if (args.narg() >= 7) entity.setDeltaMovement(args.arg(5).checkdouble(), args.arg(6).checkdouble(), args.arg(7).checkdouble());
                return LuaValue.TRUE;
            }
        });
        api.set("setEntityVelocity", new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                var level = Minecraft.getInstance().level;
                if (level == null) throw new LuaError("You must be in a world to control entities.");
                UUID uuid;
                try {
                    uuid = UUID.fromString(args.arg(1).checkjstring());
                } catch (IllegalArgumentException error) {
                    throw new LuaError("Invalid entity UUID.");
                }
                var entity = level.entitiesForRendering().stream().filter(candidate -> candidate.getUUID().equals(uuid)).findFirst().orElse(null);
                if (entity == null) throw new LuaError("Entity is not currently loaded on this client.");
                entity.setDeltaMovement(args.arg(2).checkdouble(), args.arg(3).checkdouble(), args.arg(4).checkdouble());
                return LuaValue.TRUE;
            }
        });
    }

    private static net.minecraft.world.entity.Entity findEntity(net.minecraft.client.multiplayer.ClientLevel level, UUID uuid) {
        for (var entity : level.entitiesForRendering()) {
            if (entity.getUUID().equals(uuid)) return entity;
        }
        return null;
    }

    private static void sendCommand(String command) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) throw new LuaError("You must be in a world to run this action.");
        connection.sendCommand(command);
    }
}
