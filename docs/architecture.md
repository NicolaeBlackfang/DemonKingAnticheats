# Internal architecture

This document is for authorized developers and AI assistants maintaining the workspace.

## Single-project layout

The root Gradle project builds the configured Minecraft release. Java sources under `src/main/java` contain the mod entrypoint, check orchestration, individual checks, configuration, violation tracking, punishment and notification handlers, and the packet-handler mixin. `src/main/resources` carries Fabric metadata and the mixin configuration. The default target is 26.1.2 with Fabric Loom's official named classes and Java 25. The standard `jar` task is the production artifact; no matrix collector or version subprojects are involved.

## Runtime data flow

- `AntiCheatMod` loads `config/demonkinganticheats.json`, initializes `CheckManager`, emits the first-run ANSI banner, and registers Fabric callbacks for attacks, block breaks, disconnects, and server shutdown.
- `ServerGamePacketListenerMixin` injects at the tail of the server movement packet handler. It sends the accepted player state to `CheckManager.onMovement`.
- `CheckManager` gates processing on the global and per-check configuration, keeps per-player state for movement and break checks, and invokes the combat checks.
- `DetectionRules` contains small pure threshold decisions. Check implementations return optional violation descriptions rather than mutating the server directly.
- `ViolationTracker` applies time-based decay. `NotificationHandler` alerts operators and `PunishmentHandler` applies configured warning or disconnect behavior.

## Injection and cancellation

The movement mixin observes packet handling after vanilla's handler. It does not cancel vanilla movement; current movement checks report violations only. `AttackEntityCallback` observes server-side player attacks and returns PASS. `PlayerBlockBreakEvents.BEFORE` can cancel a break when Fast Break flags it. Disconnect removes per-player state, and server stopping saves config. If a check is added, prefer Fabric events; introduce a mixin only when no suitable event exists and document the target descriptor and ordering.

## Performance notes

Per-player check state is keyed by UUID and cleared on disconnect. Keep hot callbacks allocation-light; avoid disk I/O, blocking work, or expensive world scans in packet and interaction callbacks. Use bounded per-player state and deterministic short-circuit conditions. Any async work must return to the server thread before touching game state.

## Current limits

Five checks have active logic: Flight, Speed, Reach, rapid attack cadence, and Fast Break. Other config IDs are placeholders. Server-only code cannot inspect client rendering directly. The movement callback sees server-processed state and is not a raw client telemetry feed. Treat checks as heuristics and tune conservative thresholds.
