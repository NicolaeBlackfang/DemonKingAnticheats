# Developer build and release guide

This proprietary project is maintained by authorized developers. It is a single Gradle project, not a version matrix.

## Default build

Use JDK 25 and run from the repository root:

```sh
./gradlew clean build
```

The production artifact is `build/libs/DemonKingAntiCheats-mc26.1.2-1.0.1.jar`. The standard jar task is final for the current named-class Loom setup. Source archives are disabled.

## Porting to another Minecraft version

1. Verify the exact release's Java, Fabric Loader, Fabric API, Loom and mappings from Fabric's current developer resources.
2. Update `minecraft_version`, `loader_version`, `fabric_version`, and `java_version` in `gradle.properties`. Change the Loom plugin version in `build.gradle` if required.
3. Port Minecraft types, Fabric callback signatures, packet handler descriptors, mixin targets, and metadata constraints. The current implementation uses official named classes for 26.1.2; a 1.21.x target needs a separate Yarn/remapping source port.
4. Run `./gradlew --no-daemon clean build`, inspect the generated jar metadata, then run an exact-version dedicated server. Verify movement, combat, block-break and use-on-block cancellation, menu handling, config migration, webhook errors, and the punishment ladder.
5. Only after validation, copy the binary to `releases/<minecraft-version>/`. Do not claim compatibility for targets that were not compiled and server-validated.

The GitHub Actions workflow builds and stores the jar as a workflow artifact. It does not publish public releases. Never distribute a source archive.
