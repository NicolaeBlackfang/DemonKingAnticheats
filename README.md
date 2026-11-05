# Demon King Anti-Cheats

A server-side Fabric anti-cheat project. Install the jar built for the **exact Minecraft server version**; a Fabric mod jar is not interchangeable across Minecraft releases unless its release entry explicitly says so.

## Download by Minecraft version

Choose the exact server version in the release's **Version** options (or the matching directory under `releases/` for a private binary distribution). Use this filename pattern: `DemonKingAntiCheats-mc<exact-version>-<mod-version>.jar`.

| Server version | Select this artifact | Status in this workspace |
|---|---|---|
| 1.21 | `DemonKingAntiCheats-mc1.21-<mod-version>.jar` | Not published |
| 1.21.1 | `DemonKingAntiCheats-mc1.21.1-<mod-version>.jar` | Current source target; binary not published |
| 1.21.2 | `DemonKingAntiCheats-mc1.21.2-<mod-version>.jar` | Not published |
| 1.21.3 | `DemonKingAntiCheats-mc1.21.3-<mod-version>.jar` | Not published |
| 1.21.4 | `DemonKingAntiCheats-mc1.21.4-<mod-version>.jar` | Not published |
| 1.21.5 | `DemonKingAntiCheats-mc1.21.5-<mod-version>.jar` | Not published |
| 1.21.6 | `DemonKingAntiCheats-mc1.21.6-<mod-version>.jar` | Not published |
| 1.21.7 | `DemonKingAntiCheats-mc1.21.7-<mod-version>.jar` | Not published |
| 1.21.8 | `DemonKingAntiCheats-mc1.21.8-<mod-version>.jar` | Not published |
| 1.21.9 | `DemonKingAntiCheats-mc1.21.9-<mod-version>.jar` | Not published |
| 1.21.10 | `DemonKingAntiCheats-mc1.21.10-<mod-version>.jar` | Not published |
| 1.21.11 | `DemonKingAntiCheats-mc1.21.11-<mod-version>.jar` | Not published |
| 26.1 | `DemonKingAntiCheats-mc26.1-<mod-version>.jar` | Not published |
| 26.2 | `DemonKingAntiCheats-mc26.2-<mod-version>.jar` | Not published |
| 26.3 | `DemonKingAntiCheats-mc26.3-<mod-version>.jar` | Not published |

Do not install a different version as a substitute. Version-specific binaries must be compiled and smoke-tested against that exact game/API/toolchain. Fabric's [version selector](https://fabricmc.net/develop/) gives current recommended dependencies; 26.3 uses a newer toolchain than 1.21.x. Fabric documents Loom 1.17 / Gradle 9.6 at the 26.3 launch, while the live selector may list newer recommendations; use the live selector when preparing a build. [Fabric 26.3 release notes](https://fabricmc.net/2026/09/15/263.html)

## Install and configure

1. Install Fabric Loader and Fabric API matching the server's exact Minecraft version.
2. Put the matching Demon King Anti-Cheats jar in the server `mods/` folder and restart.
3. On first start, the mod creates `config/demonkinganticheats.json` and prints a colored activation banner to the console.
4. Stop the server before editing the JSON. Set `general.enabled`, then configure each `checks.<id>.enabled`, `threshold`, and `punishment` (`WARN`, `KICK`, or `BAN`). Restart to apply.

A complete template is in [`config/demonkinganticheats.json`](config/demonkinganticheats.json). Only five checks currently have executable logic: Flight, Speed, Reach, rapid attack cadence (Kill Aura pattern), and Fast Break. The other entries are reserved configuration switches and are inert. `BAN` currently disconnects the player; it does not write to a persistent ban list. Review thresholds in a private test server before enabling punitive actions.

## Detection limits

This is server-side software. It cannot inspect client rendering, so ESP, Chams, Tracers, Fullbright, and Breadcrumbs do not expose a reliable server-side detection signal. Several automation labels also overlap with legitimate player actions. The current five checks are representative heuristics, not a reliable detector for all 57 names in the supplied list. The source list contains 57 numbered entries despite the specification heading saying 56.

## Proprietary distribution notice

This project is **not open-source** and grants no public license to reuse, modify, redistribute, or contribute code. Source files are present in the restricted maintainer workspace for authorized compilation and maintenance; public source viewing and contributions are restricted. Server owners should obtain precompiled binaries only from the authorized release's exact Minecraft **Version** option. Do not distribute source archives.
