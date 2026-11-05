# Maintainer build and version guide

This document is for authorized maintainers compiling the proprietary source and preparing version-specific binary releases. Do not publish source archives or `*-sources.jar` files.

## Current build target

The checked-in Gradle configuration currently builds **Minecraft 1.21.1 only**:

| Component | Current setting |
|---|---|
| Minecraft | `1.21.1` |
| Java compile toolchain | Java 21 (`options.release = 21`) |
| Fabric Loom | `1.8.13` |
| Yarn mappings | `1.21.1+build.3` |
| Fabric Loader | `0.16.5` |
| Fabric API | `0.110.0+1.21.1` |
| Mod version | `1.0.0` |

The values live in `gradle.properties` except Loom, which is in `build.gradle`. This project has no checked-in Gradle Wrapper yet. Use an installed Gradle compatible with the pinned Loom release; Gradle 8.10.2 is the maintainer baseline for this 1.21.1 profile. Java 21 can run Gradle starting with Gradle 8.5. See the [Gradle Java compatibility table](https://docs.gradle.org/current/userguide/compatibility.html).

## Compile the current target

1. Install a Java 21 JDK and Gradle 8.10.2. Check `java -version` and `gradle --version`.
2. From the repository root, run:

   ```sh
   gradle clean build
   ```

3. The distributable remapped mod jar is `build/libs/demonkinganticheats-1.0.0.jar`. The `*-sources.jar` is for authorized development only and must not be included in a server-owner release.
4. Install the built jar with Fabric Loader and Fabric API for Minecraft 1.21.1 on a private test server. Check startup, config generation, mixin application, all enabled check paths, and clean shutdown.
5. Once validated, copy the binary to `releases/1.21.1/DemonKingAntiCheats-mc1.21.1-1.0.0.jar`. Keep the versioned release folder as the only committed location for release jars.
6. Update the README's artifact status only after that binary is built and smoke-tested.

Gradle output under `build/`, downloaded dependencies and transformed game files under `.gradle/`, and run-server data under `run/` are generated local files. They are ignored and can be recreated. Never copy the entire `build/libs/` directory into a public release because it can include a source jar.

## Updating or adding a Minecraft target

Changing only `minecraft_version` is **not** a port. Minecraft/Fabric APIs, mappings, Java requirements, mixin descriptors, and metadata can change independently. Use a dedicated version branch/profile for each target family and follow these steps:

1. Select the exact server release to support. Get its current recommended Loader, Loom, mappings (when available), Fabric API, and Java versions from [Fabric Develop](https://fabricmc.net/develop/).
2. Update the target's build profile and Java toolchain. Keep separate builds for API/toolchain families; do not try to reuse 1.21.1 compiled classes or a 1.21.1 jar on a later release.
3. Port every Fabric event and Minecraft reference. Check each Mixin target, shadow field, method descriptor, and injection point against that target's mapped Javadoc/source. A successful compile does not prove a Mixin applies at runtime.
4. Update `fabric.mod.json` Minecraft/Loader/Java constraints and the Mixin config for the target artifact. Keep the actual dependency constraint narrow enough to avoid loading the jar on an unported version.
5. Compile the target and run a dedicated server with the exact target Minecraft, Loader, API, and JDK. Confirm startup banner, generated config, packet hook application, callbacks, punishments, and disconnect cleanup. Exercise legitimate movement/combat/block-break cases as well as violations.
6. Copy only the validated binary to `releases/<minecraft-version>/DemonKingAntiCheats-mc<exact-version>-<mod-version>.jar`. Keep other versions in their own folders. Bump `mod_version` only when cutting a new product release; the Minecraft version belongs in the artifact filename and release option.
7. Mark that exact version as available in `README.md`. Leave all unbuilt targets marked unavailable.

### 26.3 toolchain note

Minecraft 26.3 is not a property-only update from the current project. The current Fabric version selector reports a newer Loom/Loader/API line and Java 25; its recommendations can change. The [26.3 Fabric announcement](https://fabricmc.net/2026/09/15/263.html) recorded Loom 1.17 and Gradle 9.6 at launch, while the live [Fabric version selector](https://fabricmc.net/develop/) now lists newer values. Use the live selector at build time. Keep a dedicated 26.3 adapter/profile, compile with its recommended toolchain, and port/test all hooks before claiming support. The current source in this repository is not a 26.3 implementation.

## Repository layout and cleanup

* `src/main/java/` — mod entrypoint, checks, config, violation tracking, punishment/notification handlers, and mixins.
* `src/main/resources/` — Fabric metadata and mixin configuration.
* `config/demonkinganticheats.json` — checked-in operator template; runtime config is created under the server's `config/` directory.
* `docs/` — maintainer architecture, AI workflow, and this build/version guide.
* `releases/<version>/` — validated binary artifacts only.

Do not keep duplicate copies of jars in the repository root, `build/libs/`, or a generic downloads folder. Remove stale generated build output with `gradle clean`; `.gradle/` is a local cache and is ignored. Preserve the config template and developer docs even though runtime config is generated separately: the template is a reviewable schema example, not a packaged release artifact.
