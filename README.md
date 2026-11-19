<!-- L2M README -->

<div align="center">

<a href="https://github.com/DeathAmir/L2M"><img src="https://capsule-render.vercel.app/api?type=waving&color=0:171A35,50:273B70,100:36D6D0&height=220&section=header&text=L2M%20%E2%80%94%20Lua2Minecraft&fontSize=42&fontColor=FFFFFF&fontAlignY=38&desc=BRING%20LUA%20INTO%20MINECRAFT&descAlignY=58&descSize=17" alt="L2M banner" /></a>

<br/>

**Bring the flexibility of Lua to Minecraft mod development.**

[Documentation](https://DeathAmir.github.io/L2M/) · [Website](https://irautox.ir) · [Issues](https://github.com/DeathAmir/L2M/issues) · [Source](https://github.com/DeathAmir/L2M)

![Stars](https://img.shields.io/github/stars/DeathAmir/L2M?style=flat-square&logo=github&label=Stars)
![Forks](https://img.shields.io/github/forks/DeathAmir/L2M?style=flat-square&logo=github&label=Forks)
![Commits](https://img.shields.io/github/commit-activity/t/DeathAmir/L2M?style=flat-square&logo=git&label=Activity)
![Last Commit](https://img.shields.io/github/last-commit/DeathAmir/L2M?style=flat-square&logo=github&label=Updated)
![License](https://img.shields.io/github/license/DeathAmir/L2M?style=flat-square&label=License)

</div>

---

## What is L2M?

**L2M (Lua2Minecraft)** is a project focused on bringing Lua scripting into Minecraft mod development. The goal is to let developers create and extend gameplay features with Lua while keeping Minecraft's Java-based modding environment underneath.

Instead of writing every small gameplay change in Java, mod authors will be able to use Lua scripts for supported features and iterate faster.

> L2M is under active development. Features described as goals below are planned and should not be considered available until they are implemented and documented.

## Project goals

- **Lua scripting:** Embed a Lua runtime for executing mod scripts.
- **Minecraft integration:** Provide a clear Lua API for interacting with supported game systems.
- **Developer workflow:** Make it easier to organize, load, and manage Lua-based mod projects.
- **Compatibility:** Keep Minecraft-version-specific code isolated through adapters where practical.
- **Packages:** Develop an installable package format and tooling for sharing Lua extensions.
- **Documentation:** Provide examples and API references as the project becomes usable.

## Technology

| Component | Target |
|---|---|
| Minecraft | 1.21.1 |
| Mod loader | NeoForge |
| Java | 21 |
| Scripting language | Lua 5.1 |

These are the project's current target technologies, not a claim that every integration is already complete.

## Current preview capabilities

The current development preview includes:

- Client-side Lua script execution and package entrypoints.
- A bounded in-game Developer Console available with `/l2m console`.
- Package list, info, validation, enable, disable and remove commands.
- Initial client events: `client_tick`, `player_join` and `player_leave`.
- Declarative generated block and item definitions loaded during startup.

This is still a preview. Server-authoritative events, remote package installation, a full Lua REPL, and live registry mutation are not part of this version.

## Project status

L2M targets Minecraft 1.21.1, NeoForge 21.1.252 and Java 21. The current development version is **1.0.1-beta.1**. APIs and package behavior may change during preview development.

- **Release notes:** [1.0.1-beta.1](https://github.com/DeathAmir/L2M/blob/main/RELEASE_NOTES_1.0.1-beta.1.md)
- **Build status:** [GitHub Actions](https://github.com/DeathAmir/L2M/actions)
- **Issues and suggestions:** Use the [GitHub Issues](https://github.com/DeathAmir/L2M/issues) page.

## Repository activity

<div align="center">

![Stars](https://img.shields.io/github/stars/DeathAmir/L2M?style=for-the-badge&logo=github&label=STARS)
![Forks](https://img.shields.io/github/forks/DeathAmir/L2M?style=for-the-badge&logo=git&label=FORKS)
![Open Issues](https://img.shields.io/github/issues/DeathAmir/L2M?style=for-the-badge&logo=github&label=OPEN%20ISSUES)
![Release Count](https://img.shields.io/github/release-count/DeathAmir/L2M?style=for-the-badge&label=RELEASES)

</div>

## Getting started

Build the mod with Java 21:

```bash
./gradlew clean build
```

The mod JAR is written to `build/libs/`, and the example package archive is written to `build/packages/`. Install the JAR in a Minecraft 1.21.1 NeoForge 21.1.252 client. Lua scripts and packages live under `config/l2m/`. See the [installation guide](https://deathamir.github.io/L2M/docs/installation.html) and [commands reference](https://deathamir.github.io/L2M/docs/commands.html).

## Links

- [GitHub repository](https://github.com/DeathAmir/L2M)
- [Releases](https://github.com/DeathAmir/L2M/releases)
- [Issue tracker](https://github.com/DeathAmir/L2M/issues)
- [L2M website](https://irautox.ir/L2M)

---

<div align="center">

**Developed by DeathAmir**  
**IRAutoX**

</div>

## Documentation

- [Getting started](https://deathamir.github.io/L2M/docs/)
- [Lua examples](https://deathamir.github.io/L2M/docs/examples.html)
- [Lua API reference](https://deathamir.github.io/L2M/docs/api.html)

## First client script

Create `config/l2m/scripts/hello.lua`:

```lua
l2m.chat("Hello from Lua and L2M!")
```

Run it in a world with `/l2m run hello`. You can also use `/l2m chat <message>` directly. The current preview only exposes `l2m.chat(message)`; scripts execute on the client. Only run scripts you trust.
