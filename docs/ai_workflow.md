# AI maintainer workflow

This internal guide is for authorized AI assistants and developers working in this proprietary repository.

## Repository/build model

This is one Gradle project. Do not recreate `versions/`, `common/`, a matrix build, or custom artifact copy tasks. The default values in `gradle.properties` target Minecraft 26.1.2. A standard `./gradlew clean build` writes the final jar to `build/libs/`; release copies are manually curated under `releases/<minecraft-version>/`.

## Working on a check

1. Read the relevant check and `CheckManager` before changing behavior.
2. Keep pure thresholds in `DetectionRules`; return concise violation details from check methods.
3. Check the config is honored, exemptions are explicit, and per-player state is removed on disconnect.
4. Keep packet/event callbacks quick and server-thread safe. Avoid network calls, disk writes, and broad entity/block scans in hot paths.
5. State honestly which behavior is implemented. Config catalog entries alone are not detection algorithms; visual-only client hacks generally have no server-side signal.

## Updating Minecraft / Fabric versions

A target change is a port. Confirm exact Loom, Loader, Fabric API, Java, and mappings from the official Fabric developer resources. Then update `gradle.properties`, the Loom plugin version if required, Fabric metadata, mixin compatibility, source API names, callback signatures, and mixin targets. The default 26.1.2 code uses official named classes with Java 25; a 1.21.x build would require a Yarn/remapping setup and appropriate source/API adaptation. Never claim that changing Gradle properties alone makes the source cross-version compatible.

Build with `./gradlew --no-daemon clean build`. Inspect the resulting jar metadata and validate against a dedicated server at the exact target version. Update docs/download guidance only after validation. Do not publish source archives or automatically create public releases.
