# L2M Rocket Launcher

A playable prototype package for Lua2Minecraft 1.21.1.

## Install

Copy this folder into `config/l2m/packages/rocket-launcher`, then join or rejoin a world. Enable cheats or use a world/server where you have permission to run `/summon`.

## Use

Use a vanilla Firework Rocket. The package listens for `item_use_start` and launches a fireball in the direction the player is looking. The projectile uses Minecraft's normal collision and explosion behavior.

## Included model sources

- `models/rocket_launcher.obj` and `models/rocket_launcher.mtl`
- `models/rocket.obj` and `models/rocket.mtl`

These are source meshes for a future custom renderer. The current Lua package does not register a custom held item or render OBJ files in-game.

## Requirements and limits

- Requires the L2M world API additions.
- The launch command is sent through vanilla `/summon`; server permissions apply.
- This prototype uses a vanilla Firework Rocket as the input trigger. It is not yet a registered custom launcher item.
