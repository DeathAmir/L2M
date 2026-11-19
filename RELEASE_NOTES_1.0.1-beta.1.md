# Lua2Minecraft 1.0.1-beta.1

## Release overview

This preview expands L2M from basic Lua script execution into a more useful client-side development environment. It introduces an in-game Developer Console, package lifecycle commands, and the first client event hooks.

**Target platform:** Minecraft 1.21.1 · NeoForge 21.1.252 · Java 21 · LuaJ 3.0.1 (Lua 5.1-compatible)

## Highlights

### Developer Console

- Added the in-game console, opened with `/l2m console`.
- Displays recent Lua script execution messages and runtime/package errors.
- Shows the number of active Lua event listeners.
- Includes Clear log and Close controls.
- Keeps a bounded in-memory history to avoid unbounded log growth.

### Package Manager

New commands:

- `/l2m package list` — list installed package folders.
- `/l2m package info <id>` — inspect package name, version, entrypoint and enabled state.
- `/l2m package validate <id>` — validate manifest identity, entrypoint path and declared dependencies.
- `/l2m package disable <id>` — skip the package during automatic loading and startup registration preload.
- `/l2m package enable <id>` — re-enable a disabled package.
- `/l2m package remove <id>` — remove an installed package directory after checking its manifest and normalized path.

The existing `/l2m packages` command remains available as a list alias. Dependencies are currently validated against already-installed package IDs; automatic dependency downloading and version solving are not included in this preview.

### Initial Client Event API

Lua scripts can now subscribe with `l2m.on(eventName, callback)`.

| Event | Callback arguments | Description |
|---|---|---|
| `client_tick` | none | Fires once per client tick while in a world. |
| `player_join` | `playerName` | Fires when the local client joins a world. |
| `player_leave` | `playerName` | Fires when the local client leaves a world. |

Listeners are cleared when leaving the world. Callback failures are logged and do not stop dispatching other listeners. Keep tick callbacks lightweight.

## Example package

The Hello World package now demonstrates join/leave callbacks and can be managed with the new package commands.

## Compatibility

- Minecraft: 1.21.1
- NeoForge: 21.1.252
- Java: 21
- Lua runtime: LuaJ 3.0.1

## Known limitations

- Event hooks are client-side only; server-authoritative gameplay events are not exposed.
- The event API currently supports only `client_tick`, `player_join` and `player_leave`.
- Package dependencies must already be installed. There is no remote package registry or automatic installer yet.
- Disabling a package prevents future automatic loads and startup registration passes; it does not unload code already running in the current world. Rejoin or restart to apply changes.
- Generated blocks/items still follow the existing startup registration flow and require a game restart after changing definitions.
- The Developer Console is a runtime log viewer, not a full interactive Lua REPL.

## Upgrade notes

Back up `config/l2m` before upgrading. Existing script and package folders are retained. The new commands are additive; existing `/l2m run`, `/l2m package run` and `/l2m packages` commands remain supported.

## Contributors

Developed by DeathAmir · IRAutoX
