# AI/developer workflow

## Expanding a check

1. Identify a server-observable signal and explain what evidence distinguishes it from legitimate play. If the behavior is client-only, label it undetectable server-side rather than inventing certainty.
2. Add a small check class with no direct file/network I/O. Keep mutable state scoped by player UUID and clear it on disconnect.
3. Add a `CheckManager` dispatch from the narrowest Fabric event or mixin. Keep world/entity access on the server thread and bound work per packet/tick.
4. Add defaults and schema docs. Ensure missing JSON fields migrate to safe defaults; preserve unknown keys where practical.
5. Add exemptions for vanilla abilities, effects, vehicles, teleports, knockback, latency, and common plugin behavior as applicable. Prefer violation accumulation, decay, and configurable thresholds over single-event punishments.
6. Review the injection descriptor against the exact Yarn/Javadoc/source for each target version. Avoid ordinal-only injections when a stable method/argument hook exists. Document why the injection point is safe and what cancels the action.
7. Compile and smoke-test each supported target independently on a dedicated test server. Exercise legitimate edge cases before enabling KICK/BAN.

## Version management

The repository's current Java sources target 1.21.1. Treat each major API/toolchain family as a distinct build target. Do not advertise a version in the download matrix until its profile compiles and receives server smoke testing. Update one target at a time, compare official Fabric Develop versions, update mappings and Gradle/JDK, then port mixins/events and check behavior. The 26.x releases use Java 25 and current Loom 1.19+; 1.21.x uses Java 21-era tooling. Do not force one Loom plugin or compiled jar across both families.

Release artifacts go under `releases/<minecraft-version>/` and must be built from the corresponding version adapter. Keep local `build/`, `.gradle/`, and `run/` outputs out of version control.

## Debugging mixins

Check the runtime log for the selected refmap and injection count. Use required injections for hooks essential to operation; do not suppress injection failures with `defaultRequire: 0`. Verify shadow field visibility/finality against mapped bytecode. Reproduce with the exact game, loader, API, and Java versions and inspect whether another mod transforms the same method. Include a minimal reproduction and the failing descriptor in developer notes.
