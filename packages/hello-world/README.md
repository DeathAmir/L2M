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

## Client events

The example also subscribes to `player_join` and `player_leave`. L2M exposes
`client_tick`, `player_join`, `player_leave`, `player_hurt`, and `player_eat`
through `l2m.on(eventName, callback)`.

- `player_hurt(amount, damageType, health)` fires after the local player takes damage.
- `player_eat(itemId, count)` fires after the local player finishes eating food.
- `client_tick()` runs once per client tick, so keep its callback lightweight.

Use `l2m.giveItem("minecraft:totem_of_undying", 1)` to request an item through the vanilla
`/give` command. The player needs permission to use that command.

Manage the package in Minecraft with `/l2m package validate hello-world`,
`/l2m package info hello-world`, `/l2m package disable hello-world`,
`/l2m package enable hello-world`, and `/l2m package remove hello-world`.
Open `/l2m console` to inspect recent script and runtime messages.
