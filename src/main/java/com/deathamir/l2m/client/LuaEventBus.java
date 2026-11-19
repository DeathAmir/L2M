package com.deathamir.l2m.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import com.deathamir.l2m.L2M;

public final class LuaEventBus {
    private static final Map<String, List<LuaValue>> LISTENERS = new ConcurrentHashMap<>();
    private static final List<String> EVENT_NAMES = List.of("client_tick", "player_join", "player_leave");

    private LuaEventBus() {}

    public static boolean isSupported(String name) {
        return EVENT_NAMES.contains(name);
    }

    public static void subscribe(String name, LuaValue callback) {
        if (!isSupported(name)) throw new LuaError("Unsupported event '" + name + "'. Supported: client_tick, player_join, player_leave.");
        if (!callback.isfunction()) throw new LuaError("Event callback must be a function.");
        List<LuaValue> listeners = LISTENERS.computeIfAbsent(name, key -> new CopyOnWriteArrayList<>());
        if (listeners.size() >= 128) throw new LuaError("Listener limit reached for event " + name + ".");
        listeners.add(callback);
    }

    public static void dispatch(String name, LuaValue... args) {
        List<LuaValue> listeners = LISTENERS.get(name);
        if (listeners == null || listeners.isEmpty()) return;
        for (LuaValue listener : listeners) {
            try {
                listener.invoke(args);
            } catch (LuaError | RuntimeException error) {
                L2M.LOGGER.error("L2M event '{}' callback failed", name, error);
                LuaDeveloperConsole.log("EVENT ERROR [" + name + "]: " + error.getMessage());
            }
        }
    }

    public static int listenerCount() {
        return LISTENERS.values().stream().mapToInt(List::size).sum();
    }

    public static void clear() {
        LISTENERS.clear();
    }
}
