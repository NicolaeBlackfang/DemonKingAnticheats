# Developer build and release guide

This guide is for authorized developers maintaining and compiling the proprietary mod. The project is a **single Gradle project**: there are no version subprojects, shared build modules, or version matrix folders.

## Requirements and normal build

- JDK 25 (used to run Gradle and compile the default Minecraft 26.1.2 target)
- Network access on the first build so Gradle can download Loom, Minecraft, Loader, and Fabric API

At the repository root:

```sh
./gradlew clean build
```

The production jar is generated under `build/libs/`, named `DemonKingAntiCheats-mc26.1.2-1.0.0.jar`. The ordinary `jar` task is final for the current official named-class Loom setup. Sources, classes, and other build intermediates stay in the ignored `build/` folder.

## Building a different Minecraft release

A version change is a source port and dependency update, not just a filename change. Before treating a jar as compatible:

1. Check the target Minecraft version's official Fabric developer page for its required Java runtime, Fabric Loader, Fabric API, and Loom plugin version. Confirm the release exists in the Fabric Maven repository.
2. Update the default values in `gradle.properties`: `minecraft_version`, `loader_version`, `fabric_version`, and `java_version`. Keep `mod_version` for the mod's own release number.
3. Update the Loom plugin version in the root `build.gradle` if the selected Fabric instructions require another version. Legacy versions may use Yarn mappings and `net.fabricmc.fabric-loom-remap`; Minecraft 26.1+ uses official named game classes and `net.fabricmc.fabric-loom`. Do not assume the existing named-class code builds against Yarn targets.
4. Port all Minecraft imports, event callback types, mixin targets and method descriptors, and metadata requirements for that exact release. Update `src/main/resources/fabric.mod.json` if the target Java version or metadata constraint changes. Keep the source mixin compatibility level aligned in `processResources`.
5. Build and inspect the artifact:

   ```sh
   ./gradlew --no-daemon clean build
   ```

   Confirm that `build/libs/` contains one final jar named with the target Minecraft and mod versions. Inspect its `fabric.mod.json`, then launch a dedicated server with that exact Minecraft version, Loader, and API. Verify startup, config generation, movement, attack and block-break handlers, disconnect cleanup, and punishments before release.
6. Copy only the validated production jar into `releases/<minecraft-version>/`. For example:

   ```sh
   mkdir -p releases/26.1.2
   cp build/libs/DemonKingAntiCheats-mc26.1.2-1.0.0.jar releases/26.1.2/
   ```

   Rename the copied file only if required by the release channel. Keep generated outputs in `build/libs/`; do not commit them as source or create duplicate version-specific build trees.
7. Update the README's supported-version statement only after the target has compiled and passed dedicated-server validation. Do not advertise unbuilt targets.

For local iteration, `./gradlew build` is enough. `./gradlew clean` clears generated output. GitHub Actions runs the same clean build on pushes, pull requests, and manual dispatch, then stores the jar as a workflow artifact; it does not publish a public release automatically.

## Repository layout

```text
src/main/java/com/anticheat/     mod entrypoint, checks, handlers, config, mixin
src/main/resources/              Fabric metadata and mixin config
config/                          example administrator configuration
 docs/                           maintainer-only technical guides
.github/workflows/build.yml      CI build and artifact upload
build/libs/                      generated final jar (ignored by Git)
releases/<mc-version>/           manually curated release copies
```

## Current implementation limits

Five checks have executable logic: Flight, Speed, Reach, rapid attack cadence, and Fast Break. The remaining check names are only config catalog entries. Client-rendered-only hacks cannot be reliably inferred by a server. Review the heuristics and false-positive behavior in a test server when adding checks; do not describe placeholder entries as implemented detection.
