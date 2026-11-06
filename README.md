# Demon King Anti-Cheats

**Fabric server-side protection for Minecraft Java 26.1.2** · Mod version **1.0.3** · Java **25**

Demon King Anti-Cheats checks selected server-visible combat, movement, and block-interaction behavior. Detection hooks run on the authoritative server thread, with configurable violation buffering and escalating server actions.

> Compatibility is exact-versioned. This binary targets Minecraft 26.1.2 and is not a 1.21.x mod.

## Download

| Minecraft server | Mod version | Required server dependencies | Download |
| --- | --- | --- | --- |
| **26.1.2** | **1.0.3** (latest) | Fabric Loader 0.20.1+ and Fabric API 0.155.3+26.1.2 | [DemonKingAntiCheats-mc26.1.2-1.0.3.jar](releases/26.1.2/DemonKingAntiCheats-mc26.1.2-1.0.3.jar) |
| **26.1.2** | 1.0.2 (previous) | Fabric Loader 0.20.1+ and Fabric API 0.155.3+26.1.2 | [DemonKingAntiCheats-mc26.1.2-1.0.2.jar](releases/26.1.2/DemonKingAntiCheats-mc26.1.2-1.0.2.jar) |
| **26.1.2** | 1.0.1 (previous) | Fabric Loader 0.20.1+ and Fabric API 0.155.3+26.1.2 | [DemonKingAntiCheats-mc26.1.2-1.0.1.jar](releases/26.1.2/DemonKingAntiCheats-mc26.1.2-1.0.1.jar) |
| **26.1.2** | 1.0.0 (previous) | Matching Fabric Loader and Fabric API for 26.1.2 | [DemonKingAntiCheats-mc26.1.2-1.0.0.jar](releases/26.1.2/DemonKingAntiCheats-mc26.1.2-1.0.0.jar) |
| **1.21.1** | 1.0.0 (archived) | Matching Fabric Loader and Fabric API for 1.21.1 | [DemonKingAntiCheats-mc1.21.1-1.0.0.jar](releases/1.21.1/DemonKingAntiCheats-mc1.21.1-1.0.0.jar) |

Choose the row matching your exact server version. The 1.21.1 jar is an archived binary from an older project line; the current source tree and build target are 26.1.2. Do not substitute one Minecraft version's jar for another.

## Installation

1. Install a dedicated Minecraft 26.1.2 server with Java 25, Fabric Loader, and Fabric API 0.155.3+26.1.2.
2. Stop the server and copy `DemonKingAntiCheats-mc26.1.2-1.0.3.jar` into the server's `mods/` directory.
3. Start the server. The mod prints its colored startup banner and creates `config/demonkinganticheats.json` on first run.
4. Review the configuration, restart the server, and monitor alerts before enabling punitive escalation on a production server.

## Active protections

The configuration contains twelve active checks wired into server packet, tick, and block-break paths:

| Check | Server-side signal / action |
| --- | --- |
| **Kill Aura** | Checks real serverbound attacks, selected entity categories, reach, and whether the server view ray intersects the target hitbox. Repeated anomalies can cancel the attack. |
| **Reach** | Measures distance to target hitboxes before vanilla's attack-range gate; vanilla validation remains active too. |
| **Flight** | Tracks prolonged unsupported hovering once per server tick. |
| **Speed** | Samples displacement after each server-accepted movement packet, ignores packet gaps and high RTT, and uses a conservative per-packet threshold with movement-context exemptions. |
| **Jesus / Water Walking** | Looks for sustained near-zero vertical movement above water without swimming, vehicle, or flight context. |
| **No Fall** | Rejects a client on-ground claim after a substantial fall when the reported destination has no supporting collision block. |
| **No Slow** | Tracks sustained ground movement while the server knows the player is using an item, with exemptions for sprinting, knockback, and fluids. |
| **Crystal Aura** | Reports unusually many end-crystal attacks in one server tick. It is alert-only by default to avoid cancelling legitimate burst traffic. |
| **FastBreak** | Uses vanilla block destroy progress, including block/tool/effect factors, to estimate a conservative minimum completion interval. |
| **FastPlace** | Tracks per-player block placement packet intervals and can cancel repeated packets below the configured delay. |
| **Scaffold / Air Place** | Checks block-item placement reach and the claimed supporting face before vanilla handles the packet. |
| **InventoryWalk** | Cancels movement/interaction packets while a server-known container menu is open, when configured. The server cannot observe the client-only personal inventory screen. |

These are heuristics and safeguards, not proof of cheating. Client-only aim assistance cannot be reliably distinguished from skilled play using server packets alone. Critical-hit damage is calculated from server movement state; the server does not receive a trustworthy “critical” claim to flag. Visual-only hacks such as ESP, Chams, Tracers, and Fullbright are not observable by a server-side mod. No anti-cheat can guarantee zero false positives under every latency, mod, and server-load condition. Validate settings on a test server.

## Configuration

Stop the server before editing `config/demonkinganticheats.json`. The checked-in file is bundled into the jar and is the source of truth for generated defaults. First run writes it to the server's config directory; upgrades add newly supported options while keeping valid administrator values. The schema includes only active checks, each enabled by default, plus the punishment ladder.

Config schema version 7 validates option types and clamps numeric settings to supported ranges. Upgrades preserve custom ladders and add missing action controls. If the JSON is malformed, the mod saves a `.bak` copy and regenerates safe defaults instead of aborting startup.

### Per-check tuning

Each object under `checks` has check-specific options. Common settings include:

- `enabled`: enable or disable that check.
- `strictness`: controls how many repeated signals fill its violation buffer; higher values are more sensitive.
- `buffer_limit`: caps buffered signals where that check uses one.
- `auto_cancel`: reject the corresponding server action when its check confirms an anomaly.

Other parameters are check-specific: `max_reach`, `max_angle_delta`, `vertical_tolerance`, `minimum_completion_ratio`, `min_delay_ms`, and `max_extension_reach`. Prefer conservative changes and adjust one value at a time.

### Latency and corrections

Flight, Jesus, and No Slow sample on the server tick. Speed samples each movement packet only after vanilla handles it on the server thread; it resets its baseline after a 200 ms packet gap and does not flag at RTT of 250 ms or higher. Reach, combat geometry, break, place, and scaffold checks are also skipped at RTT of 250 ms or higher because delayed packet arrival makes their timing or geometry ambiguous. These safeguards reduce ambiguity; they cannot guarantee zero false positives on every modded or heavily overloaded server.

### Warning, kick, and ban ladder

`punishment_matrix.escalation_ladder` is ordered by `violations` and includes warning at 5, kick at 15, and a seven-day ban at 25. `punishment_matrix.actions.warn`, `.kick`, and `.ban` independently enable each action. Warning is enabled by default; kick and ban are disabled until an administrator explicitly enables them. Each ladder step supports `action`, `broadcast`, `message`, and optional `duration_days`. Messages accept `%player%`, `%check%`, and `%violations%`; ampersand color codes are translated to Minecraft formatting codes. Bans are written to the server's ban list. Review check alerts before enabling kick or ban on a live server.

### Discord webhook

To enable Discord alerts:

1. Create a Discord webhook for a private moderation channel.
2. Set `global_settings.discord_webhook.enabled` to `true` and set `url` to the webhook URL in the server's local config.
3. Keep the URL secret. Never commit a real webhook URL or token to GitHub; the repository template leaves it blank.
4. Restart the server. Webhook posts are asynchronous and use the configured embed color and timeout.

If a webhook URL has ever been committed, revoke it in Discord and create a new one. Deleting it from the latest file does not remove it from Git history.

## Build from source

Authorized maintainers with JDK 25 can run:

```sh
./gradlew clean build
```

The jar is generated at `build/libs/DemonKingAntiCheats-mc26.1.2-1.0.3.jar`. See [the developer build guide](docs/maintainer_builds.md) before changing Minecraft versions; a version-property change alone does not port Minecraft APIs.

## License and source

This project is proprietary. Compiled binaries are offered for server installation; source redistribution, modification, sublicensing, and commercial relicensing are restricted. See [LICENSE](LICENSE) for the project notice. A public GitHub repository makes source visible to visitors; the notice sets reuse and redistribution terms but cannot technically hide source in a public repository.
