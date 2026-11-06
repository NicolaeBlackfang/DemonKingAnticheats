# Demon King Anti-Cheats

**Fabric server-side protection for Minecraft Java 26.1.2** · Mod version **1.0.1** · Java **25**

Demon King Anti-Cheats checks selected server-visible combat, movement, and block-interaction behavior. It is designed to keep work in packet and event handlers bounded, with configurable violation buffering, latency grace, and escalating server actions.

> Compatibility is exact-versioned. This binary targets Minecraft 26.1.2 and is not a 1.21.x mod.

## Download

| Minecraft server | Mod version | Required server dependencies | Download |
| --- | --- | --- | --- |
| **26.1.2** | **1.0.1** (latest) | Fabric Loader 0.20.1+ and Fabric API 0.155.3+26.1.2 | [DemonKingAntiCheats-mc26.1.2-1.0.1.jar](releases/26.1.2/DemonKingAntiCheats-mc26.1.2-1.0.1.jar) |
| **26.1.2** | 1.0.0 (previous) | Matching Fabric Loader and Fabric API for 26.1.2 | [DemonKingAntiCheats-mc26.1.2-1.0.0.jar](releases/26.1.2/DemonKingAntiCheats-mc26.1.2-1.0.0.jar) |
| **1.21.1** | 1.0.0 (archived) | Matching Fabric Loader and Fabric API for 1.21.1 | [DemonKingAntiCheats-mc1.21.1-1.0.0.jar](releases/1.21.1/DemonKingAntiCheats-mc1.21.1-1.0.0.jar) |

Choose the row matching your exact server version. The 1.21.1 jar is an archived binary from an older project line; the current source tree and build target are 26.1.2. Do not substitute one Minecraft version's jar for another.

## Installation

1. Install a dedicated Minecraft 26.1.2 server with Java 25, Fabric Loader, and Fabric API 0.155.3+26.1.2.
2. Stop the server and copy `DemonKingAntiCheats-mc26.1.2-1.0.1.jar` into the server's `mods/` directory.
3. Start the server. The mod prints its colored startup banner and creates `config/demonkinganticheats.json` on first run.
4. Review the configuration, restart the server, and monitor alerts before enabling punitive escalation on a production server.

## Active protections

The configuration contains only the nine checks wired into the current event pipeline:

| Check | Server-side signal / action |
| --- | --- |
| **KillAura** | Filters configured target categories, checks attack reach and whether the server view ray intersects the target hitbox; buffered anomalies can cancel attacks. |
| **Reach** | Measures distance from the player's eye position to the nearest point on a target player's hitbox, with a bounded ping margin. |
| **Flight** | Tracks prolonged unsupported hover and corrects repeated movement anomalies before escalation. |
| **Speed** | Compares horizontal movement displacement with a configured allowance adjusted for ping and jitter. |
| **Jesus** | Looks for sustained near-zero vertical movement above water without swimming, vehicle, or flight context. |
| **FastBreak** | Uses vanilla block destroy progress, including block/tool/effect factors, to estimate a conservative minimum completion interval. |
| **FastPlace** | Tracks per-player block placement packet intervals and can cancel repeated packets below the configured delay. |
| **Scaffold** | Checks block-item placement reach and the claimed supporting face before vanilla handles the packet. |
| **InventoryWalk** | Cancels movement/interaction packets while a server-known container menu is open, when configured. The server cannot observe the client-only personal inventory screen. |

These are heuristics and safeguards, not proof of cheating. Server-side code cannot inspect client rendering or reliably detect visual-only hacks such as ESP, Chams, Tracers, and Fullbright. No anti-cheat can guarantee zero false positives under every latency, mod, and server-load condition. Validate settings on a test server.

## Configuration

Stop the server before editing `config/demonkinganticheats.json`. The checked-in file is a safe template; first run writes an equivalent file into the server's config directory. The schema includes only the active checks, each enabled by default, plus global latency options and the punishment ladder.

### Per-check tuning

Each object under `checks` has check-specific options. Common settings include:

- `enabled`: enable or disable that check.
- `strictness`: controls how many repeated signals fill its violation buffer; higher values are more sensitive.
- `buffer_limit`: caps buffered signals where that check uses one.
- `auto_cancel`: reject the corresponding server action when its check confirms an anomaly.

Other parameters are check-specific: `max_reach`, `max_angle_delta`, `vertical_tolerance`, `minimum_completion_ratio`, `min_delay_ms`, and `max_extension_reach`. Prefer conservative changes and adjust one value at a time.

### Latency and corrections

`global_settings.high_ping_grace_ms`, `high_jitter_grace_ms`, and `low_tps_grace_threshold` control movement grace. Early movement anomalies can be rubberbanded to a recently grounded position before they enter the violation ladder. These protections reduce risk but trade detection coverage for fewer false positives.

### Warning, kick, and ban ladder

`punishment_matrix.escalation_ladder` is ordered by `violations`. The defaults warn at 5, kick at 15, and apply a seven-day ban at 25. Each step supports `action`, `broadcast`, `message`, and optional `duration_days`. Messages accept `%player%`, `%check%`, and `%violations%`; ampersand color codes are translated to Minecraft formatting codes. Bans are written to the server's ban list.

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

The jar is generated at `build/libs/DemonKingAntiCheats-mc26.1.2-1.0.1.jar`. See [the developer build guide](docs/maintainer_builds.md) before changing Minecraft versions; a version-property change alone does not port Minecraft APIs.

## License and source

This project is proprietary. Compiled binaries are offered for server installation; source redistribution, modification, sublicensing, and commercial relicensing are restricted. See [LICENSE](LICENSE) for the project notice. A public GitHub repository makes source visible to visitors; the notice sets reuse and redistribution terms but cannot technically hide source in a public repository.
