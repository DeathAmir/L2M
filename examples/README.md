# L2M Lua examples

Copy `hello.lua` into:

```text
.minecraft/config/l2m/scripts/hello.lua
```

Run it in a Minecraft world:

```text
/l2m run hello
```

The first client API is intentionally small:

```lua
l2m.chat("Hello from Lua!")
```

This sends a normal chat message through the local Minecraft client connection. The script runs locally; it does not grant server permissions or bypass server chat rules. Lua APIs are experimental and will expand in later versions.
