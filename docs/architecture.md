# Internal architecture

For authorized developers and AI assistants maintaining this proprietary mod.

## Project and packages

This single Gradle project builds Minecraft 26.1.2 / Java 25 using official named classes. Sources use `net.demonking.anticheat`:

- `DemonKingAntiCheat` registers Fabric events, the server tick check loop, and the ANSI startup banner.
- `config` loads and normalizes the versioned JSON schema. Every check has its own JSON object, so parameters can be check-specific.
- `model` contains persistent decaying violation levels and per-session token buffers.
- `punishment` applies the configured ladder, timed/permanent server bans, messages, staff notifications, and asynchronous Discord embeds.
- `checks` contains isolated checks and the event router. The network mixin hooks movement, attacks, and use-on-block packets after Minecraft's thread handoff.

## Implemented event paths

- KillAura and Reach inspect `ServerboundAttackPacket` immediately before vanilla's attack-range check. This is the serverbound path; the Fabric `AttackEntityCallback` is a client interaction hook and is not used for enforcement.
- Flight, Speed, Jesus, and No Slow run from `ServerTickEvents.END_SERVER_TICK` after vanilla movement. This avoids packet-thread races and gives counters a real server-tick cadence.
- No Fall inspects on-ground claims after `PacketUtils.ensureRunningOnSameThread`, compares them with server fall distance and destination support, and cancels spoofed ground packets.
- Crystal Aura counts end-crystal attacks by server tick. It alerts on excessive bursts but does not cancel by default.
- FastBreak runs from Fabric's pre-break callback, uses vanilla destroy-progress calculation (block, tool and effects) for a timing floor, and may cancel the break after buffer confirmation.
- FastPlace and Scaffold inspect `ServerboundUseItemOnPacket` before vanilla handles it. They check rate, reach, and a supporting face; configured auto-cancel can reject invalid placements.
- InventoryWalk checks server-known container menus. It can cancel movement, attacks, and block interactions while such a menu is open. The server cannot know when a player opens the client-only personal inventory screen.

## Coverage and safety boundaries

The config contains twelve active checks: KillAura, Reach, Flight, Speed, Jesus, No Fall, No Slow, Crystal Aura, FastBreak, FastPlace, Scaffold, and InventoryWalk. AimAssist/Aimbot cannot be reliably distinguished from skilled aim using server packets alone. Critical damage is computed server-side from movement state, so the server receives no trustworthy critical-hit flag. Render-only hacks are also not observable. Do not add a toggle without a meaningful server-side signal.

The checks skip latency-sensitive timing and geometry at RTT of at least 250 ms, and all movement checks run on the server tick loop. No server plugin can promise zero false positives under every mod, latency pattern, or server load. Validate thresholds and cancellation behavior on the exact target server before enabling high-impact punishment rules.
