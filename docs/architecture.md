# Developer architecture

## Runtime flow

`AntiCheatMod` initializes the server config, one `CheckManager`, event callbacks, and the version-specific startup banner. The movement mixin observes `ServerPlayNetworkHandler.onPlayerMove` at `TAIL`, after vanilla has validated/applied the movement packet. The manager dispatches bounded checks on the server thread. Fabric's `AttackEntityCallback` observes attacks before the target interaction is processed. `PlayerBlockBreakEvents.BEFORE` can cancel a suspicious break by returning `false`.

Current event paths:

1. Movement packet -> mixin -> `CheckManager.onMovement` -> flight and speed checks.
2. Entity attack callback -> manager -> reach and attack cadence checks.
3. Block break BEFORE callback -> manager -> fast-break heuristic -> cancel or allow.
4. Check result -> per-player/check violation level with time-based decay -> staff notification -> configured punishment router.

Live Minecraft world/entity state is thread-confined. These checks remain on the server thread and must be bounded; do not run packet handlers asynchronously. If expensive analysis is added, capture immutable primitives on-thread and bound work in a background queue, then schedule any player/world action back onto the server thread.

## Mixins and version adapters

The current implementation is pinned to Yarn 1.21.1 mappings. The packet mixin shadows `ServerPlayNetworkHandler.player` and injects into `onPlayerMove(PlayerMoveC2SPacket)` at `TAIL`. Injection signatures, shadow names, descriptors, and event APIs must be reviewed per Minecraft line. Keep version-specific hooks behind source adapters/profile directories; do not assume a binary compiled against 1.21.1 runs on later releases.

Minecraft 26.3 has a distinct toolchain and API surface. Fabric's current version selector provides the release-specific dependencies; at the time this document was updated it reports Loader 0.20.1, Loom 1.19-SNAPSHOT, Fabric API 0.162.0+26.3. Java 25 is required by the current upstream example metadata. Always re-check official version data before a release. (Fabric 26.3 release notes and version selector are linked in the admin README.)

## Cancellation and limits

Only the fast-break path currently cancels gameplay. Reach/attack cadence are detected from server attack events but do not cancel the attack directly. Movement is sampled after vanilla's packet handling and is observational; correction/cancellation needs a narrowly tested injection before state application. Never cancel a vanilla packet based only on one noisy heuristic.

The config declares all 57 supplied names, but only five have concrete check implementations. Server-only software cannot reliably detect purely client-rendered visuals such as ESP, Chams, Tracers, or Fullbright without client cooperation or gameplay changes. Behavior labels such as AimAssist, Baritone, Auto-* and Scaffold often have legitimate equivalents; reliable flags require multiple corroborating signals and false-positive testing. Unsupported config entries are inert.
