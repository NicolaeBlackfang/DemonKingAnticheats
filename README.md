# Demon King Anti-Cheats

A server-side Fabric mod built from one Gradle project. The default build target is **Minecraft 26.1.2**. The output is an ordinary production jar in `build/libs/`.

## Build and releases

For authorized maintainers, install JDK 25 and run from the repository root:

```sh
./gradlew clean build
```

The jar is written to `build/libs/DemonKingAntiCheats-mc26.1.2-1.0.0.jar`. For release packaging, copy that jar to `releases/26.1.2/` and rename it if your release naming convention requires it. Do not copy source jars. See [the developer build guide](docs/maintainer_builds.md) for changing the target to another Minecraft release.

## Server installation and configuration

1. Use Fabric Loader and Fabric API that match the jar's exact Minecraft version.
2. Put the jar in the server's `mods/` directory and start the server.
3. On first run, the mod creates `config/demonkinganticheats.json` and prints its colored activation banner.
4. Stop the server before editing the config. Tune `general.enabled`, then each check's `enabled`, `threshold`, and `punishment` (`WARN`, `KICK`, or `BAN`). Restart to apply.

The current implementation has executable checks for Flight, Speed, Reach, rapid attack cadence (Kill Aura pattern), and Fast Break. Other catalog entries are configuration placeholders. BAN currently disconnects the player rather than maintaining a persistent ban list. Server-side checks cannot directly observe client-only render features such as ESP, Chams, Tracers, or Fullbright. Validate thresholds on a private server before enabling punitive actions.

## Exact-version rule

Use a jar compiled and validated for the exact Minecraft version shown in its filename. The checked-in default metadata targets 26.1.2 only; it does not claim compatibility with 1.21.x or other 26.x releases. Update dependencies, source/API usage, and metadata together as described in the maintainer guide before producing a different release.

## Source and distribution

This project is proprietary and is not licensed as open-source software. Source is kept in the restricted maintainer workspace for authorized compilation and maintenance; public source viewing, reuse, redistribution, and contributions are restricted. Server operators should obtain precompiled binaries from the authorized release download for their exact Minecraft version.
