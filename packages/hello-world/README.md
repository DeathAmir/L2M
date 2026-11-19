# L2M Hello World

This is a starter package for Lua2Minecraft.

## Layout

- package.json: package metadata and entry point
- scripts/main.lua: Lua entry point
- lang/en_us.json: example English translations
- assets/: place textures, models, sounds and other package assets here

## Run

Install the package directory under `config/l2m/packages/hello-world`, then run:

```text
/l2m package run hello-world main
```

The example writes a declarative block definition to `config/l2m/generated/blocks/ruby_ore.json`.
This preview does not register a live Minecraft block. Live blocks, items and recipes require
Java-side NeoForge registry integration before registries freeze.

Lua scripts execute locally and are trusted code. `l2m.systemChat` and `l2m.clientChat`
write a local L2M message in the chat UI; they do not send a message as the player.
