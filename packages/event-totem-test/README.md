# L2M Event Totem Test

A small test package for the L2M client event API.

## Install

Copy this folder into:

`config/l2m/packages/event-totem-test`

Then join a world. L2M automatically loads enabled packages when you enter a world.

## Test damage events

Take damage. When `player_hurt` fires, the package requests one Totem of Undying with the vanilla `/give @s minecraft:totem_of_undying 1` command. It has a 40-tick cooldown to avoid flooding the chat with commands.

The world must allow the player to use the `/give` command. In singleplayer, enable cheats or open the world to LAN with cheats enabled. On a server, you need permission to use `/give`.

## Test food events

Eat any food item. The package logs the item ID and prints a local message when `player_eat` fires.

Check `latest.log` or open the L2M developer console with `/l2m console` to inspect messages.
