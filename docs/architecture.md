# Internal architecture

For authorized developers and AI assistants maintaining this proprietary mod.

## Project and packages

This single Gradle project builds Minecraft 26.1.2 / Java 25 using official named classes. Sources use `net.demonking.anticheat`:

- `DemonKingAntiCheat` registers Fabric events, the tick monitor, and the ANSI startup banner.
- `config` loads and normalizes the versioned JSON schema. Every check has its own JSON object, so parameters can be check-specific.
- `model` contains persistent decaying violation levels and per-session token buffers.
- `lag` tracks movement packet timing, RTT/jitter grace, measured server tick delays, and recent grounded coordinates for correction.
- `punishment` applies the configured ladder, timed/permanent server bans, messages, staff notifications, and asynchronous Discord embeds.
- `checks` contains isolated checks and the event router. The network mixin observes movement packets at the handler head/tail and use-on-block packets before vanilla processes them.

## Implemented event paths

- KillAura and Reach run through `AttackEntityCallback`; KillAura filters target categories from config, checks the server view ray against the target hitbox, and checks configured reach. Invalid attacks can be cancelled after the configured signal buffer confirms them.
- Flight, Speed, and Jesus run after vanilla movement handling. RTT, jitter, packet gaps, and measured tick stalls grant grace. Early repeated movement violations are rubberbanded to the last grounded location before entering escalation.
- FastBreak runs from Fabric's pre-break callback, uses vanilla destroy-progress calculation (block, tool and effects) for a timing floor, and may cancel the break after buffer confirmation.
- FastPlace and Scaffold inspect `ServerboundUseItemOnPacket` before vanilla handles it. They check rate, reach, and a supporting face; configured auto-cancel can reject invalid placements.
- InventoryWalk checks server-known container menus. It can cancel movement, attacks, and block interactions while such a menu is open. The server cannot know when a player opens the client-only personal inventory screen.

## Coverage and safety boundaries

The configuration intentionally contains only nine active checks: KillAura, Reach, Flight, Speed, Jesus, FastBreak, FastPlace, Scaffold, and InventoryWalk. Several named hacks are client-render-only, and many others cannot be inferred reliably from server packets alone. Do not add a check option without a real server-side signal and exact-version validation.

The buffers and RTT/tick compensation are heuristics. No server plugin can promise zero false positives under every mod, latency pattern, or server load. Validate thresholds and cancellation behavior on the exact target server before enabling high-impact punishment rules.
