# AI maintainer workflow

Internal instructions for authorized maintainers.

## Architecture rules

Keep the `net.demonking.anticheat` package split: checks under combat/movement/world/player, configuration in `config`, profile/violation data in `model`, lag handling in `lag`, and punishments/webhooks in `punishment`. A check should have an isolated class, tailored config object, bounded state, and a documented server-visible signal. Do not add empty check classes and mark them implemented.

The config exposes only nine implemented checks and enables them by default. Do not add a config toggle until its module is wired to a server event and has a real server-visible signal. Client-only rendering cannot be observed by the server.

## Check workflow

1. Find the authoritative Fabric event or server packet handler for the behavior.
2. Document normal vanilla behavior, exemptions, packet ordering, and false-positive risks.
3. Use `PlayerProfile` buffers to require repeated signals; preserve configured auto-cancel behavior only where the server can safely reject the action.
4. Use lag grace and correction for movement. Never punish a player solely because a movement packet arrived late.
5. Clear transient state on disconnect; preserve decaying violation levels when escalation must survive kicks/restarts.
6. Keep webhook I/O asynchronous and never log webhook secrets.

## Build and version changes

The current target is Minecraft 26.1.2, mod version 1.0.1, Java 25. Run `./gradlew clean build`; inspect the jar metadata. A new Minecraft target requires verifying Loom, loader, API, Java, mappings, source APIs, mixins, and metadata, then testing on a dedicated server before release.
