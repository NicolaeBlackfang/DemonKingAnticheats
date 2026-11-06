package net.demonking.anticheat.checks;

import com.google.gson.JsonObject;
import net.demonking.anticheat.checks.combat.KillAuraCheck;
import net.demonking.anticheat.checks.combat.ReachCheck;
import net.demonking.anticheat.checks.movement.FlightCheck;
import net.demonking.anticheat.checks.movement.JesusCheck;
import net.demonking.anticheat.checks.movement.SpeedCheck;
import net.demonking.anticheat.checks.player.InventoryWalkCheck;
import net.demonking.anticheat.checks.world.FastBreakCheck;
import net.demonking.anticheat.checks.world.FastPlaceCheck;
import net.demonking.anticheat.checks.world.ScaffoldCheck;
import net.demonking.anticheat.config.ConfigManager;
import net.demonking.anticheat.config.ConfigSchema;
import net.demonking.anticheat.lag.LagCompensationManager;
import net.demonking.anticheat.model.PlayerProfile;
import net.demonking.anticheat.model.ViolationTracker;
import net.demonking.anticheat.punishment.NotificationHandler;
import net.demonking.anticheat.punishment.PunishmentManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Routes server events to isolated check modules and shared buffers/escalation. */
public final class CheckManager {
    private static final String[] IMPLEMENTED = {"fastbreak", "fastplace", "flight", "speed", "reach", "killaura", "jesus", "scaffold", "inventorywalk"};
    private final ViolationTracker violations = new ViolationTracker();
    private final NotificationHandler notifications = new NotificationHandler();
    private final PunishmentManager punishments = new PunishmentManager();
    private final Map<UUID, PlayerProfile> profiles = new HashMap<>();
    private final Map<UUID, FlightCheck> flights = new HashMap<>();
    private final Map<UUID, SpeedCheck> speeds = new HashMap<>();
    private final Map<UUID, JesusCheck> jesus = new HashMap<>();
    private final Map<UUID, FastBreakCheck> breaks = new HashMap<>();
    private final Map<UUID, FastPlaceCheck> places = new HashMap<>();
    private final Map<UUID, LagCompensationManager> lag = new HashMap<>();
    private final KillAuraCheck killaura = new KillAuraCheck();
    private final ReachCheck reach = new ReachCheck();
    private final ScaffoldCheck scaffold = new ScaffoldCheck();
    private final InventoryWalkCheck inventoryWalk = new InventoryWalkCheck();

    public int activeCheckCount() {
        int active = 0;
        for (String id : IMPLEMENTED) if (ConfigManager.get().isEnabled(id)) active++;
        return active;
    }

    public boolean onAttack(ServerPlayer attacker, Entity target) {
        if (!active()) return false;
        if (ConfigManager.get().isEnabled("inventorywalk") && inventoryWalk.shouldBlock(attacker, ConfigManager.get().bool("inventorywalk", "allow_rotation", false))) {
            flag(attacker, "inventorywalk", "attack while server container is open");
            return ConfigManager.get().bool("inventorywalk", "auto_cancel", true);
        }
        int ping = Math.max(0, attacker.connection.latency());
        int gracePing = ConfigManager.get().globalSettings.highPingGraceMs;
        if (ConfigManager.get().isEnabled("killaura") && ping < gracePing * 2) {
            double reachSetting = ConfigManager.get().decimal("killaura", "max_reach", 3.05);
            double reachMargin = Math.min(0.75, ping * ConfigManager.get().decimal("reach", "ping_margin_per_ms", 0.0015));
            Optional<String> aura = killaura.check(attacker, target,
                    ConfigManager.get().decimal("killaura", "max_angle_delta", 45.0), reachSetting + reachMargin,
                    ConfigManager.get().bool("killaura", "check_reach", true),
                    ConfigManager.get().bool("killaura", "target_players", true),
                    ConfigManager.get().bool("killaura", "target_mobs", true),
                    ConfigManager.get().bool("killaura", "target_animals", true));
            if (aura.isPresent()) {
                boolean registered = flag(attacker, "killaura", aura.get());
                if (registered && ConfigManager.get().bool("killaura", "auto_cancel", true)) return true;
            }
        }
        if (target instanceof ServerPlayer victim && ConfigManager.get().isEnabled("reach")) {
            double maxReach = ConfigManager.get().decimal("reach", "max_reach", 3.05) + Math.min(0.75, ping * 0.0015);
            Optional<String> reachViolation = reach.check(attacker, victim, maxReach);
            if (reachViolation.isPresent()) {
                boolean registered = flag(attacker, "reach", reachViolation.get());
                if (registered && ConfigManager.get().bool("reach", "auto_cancel", true)) return true;
            }
        }
        return false;
    }

    public void onMovement(ServerPlayer player) {
        if (!active()) return;
        ConfigSchema.GlobalSettings global = ConfigManager.get().globalSettings;
        LagCompensationManager compensation = lag.computeIfAbsent(player.getUUID(), ignored -> new LagCompensationManager());
        if (compensation.inGraceWindow(player, global.highPingGraceMs, global.highJitterGraceMs, global.lowTpsGraceThreshold)) {
            if (player.onGround()) compensation.rememberSafePosition(player);
            return;
        }
        Optional<String> violation = Optional.empty();
        String checkId = "";
        if (ConfigManager.get().isEnabled("flight")) {
            violation = flights.computeIfAbsent(player.getUUID(), ignored -> new FlightCheck()).check(player,
                    ConfigManager.get().integer("flight", "hover_ticks", 45),
                    ConfigManager.get().decimal("flight", "vertical_tolerance", 0.005));
            if (violation.isPresent()) checkId = "flight";
        }
        if (violation.isEmpty() && ConfigManager.get().isEnabled("speed")) {
            double baseDistance = ConfigManager.get().decimal("speed", "base_horizontal_per_packet", 0.72);
            double pingScale = ConfigManager.get().decimal("speed", "ping_tolerance_scale", 0.0025);
            double tolerance = baseDistance + Math.max(0, player.connection.latency()) * pingScale
                    + Math.min(0.25, compensation.currentJitterMillis() / 1000.0);
            violation = speeds.computeIfAbsent(player.getUUID(), ignored -> new SpeedCheck()).check(player,
                    tolerance);
            if (violation.isPresent()) checkId = "speed";
        }
        if (violation.isEmpty() && ConfigManager.get().isEnabled("jesus")) {
            violation = jesus.computeIfAbsent(player.getUUID(), ignored -> new JesusCheck()).check(player,
                    ConfigManager.get().decimal("jesus", "vertical_tolerance", 0.05),
                    ConfigManager.get().bool("jesus", "require_movement_packet", true));
            if (violation.isPresent()) checkId = "jesus";
        }
        if (violation.isPresent()) {
            if (!compensation.correctBeforePunish(player, global.rubberbandBeforePunish)) flag(player, checkId, violation.get());
            return;
        }
        compensation.accepted();
        if (player.onGround()) compensation.rememberSafePosition(player);
    }

    public boolean onBlockBreak(ServerPlayer player, BlockPos pos, BlockState state) {
        if (!active() || !ConfigManager.get().isEnabled("fastbreak")) return true;
        if (Math.max(0, player.connection.latency()) >= ConfigManager.get().globalSettings.highPingGraceMs) return true;
        Optional<String> result = breaks.computeIfAbsent(player.getUUID(), ignored -> new FastBreakCheck())
                .check(player, pos, state, ConfigManager.get().decimal("fastbreak", "minimum_completion_ratio", 0.65));
        if (result.isEmpty()) return true;
        flag(player, "fastbreak", result.get());
        return !ConfigManager.get().bool("fastbreak", "auto_cancel", true);
    }

    /** Called at packet HEAD, before vanilla handles a block interaction or placement. */
    public boolean onBlockUse(ServerPlayer player, ServerboundUseItemOnPacket packet) {
        if (!active()) return false;
        if (ConfigManager.get().isEnabled("inventorywalk") && inventoryWalk.shouldBlock(player, ConfigManager.get().bool("inventorywalk", "allow_rotation", false))) {
            flag(player, "inventorywalk", "block interaction while server container is open");
            if (ConfigManager.get().bool("inventorywalk", "auto_cancel", true)) return true;
        }
        int ping = Math.max(0, player.connection.latency());
        int grace = ConfigManager.get().globalSettings.highPingGraceMs;
        if (ConfigManager.get().isEnabled("fastplace") && ping < grace) {
            boolean tooFast = places.computeIfAbsent(player.getUUID(), ignored -> new FastPlaceCheck())
                    .tooFast(player, packet.getHand(), ConfigManager.get().integer("fastplace", "min_delay_ms", 50));
            if (tooFast) {
                boolean registered = flag(player, "fastplace", "block placement packets arrived faster than the configured delay");
                if (registered && ConfigManager.get().bool("fastplace", "auto_cancel", true)) return true;
            }
        }
        if (ConfigManager.get().isEnabled("scaffold")) {
            double baseReach = ConfigManager.get().decimal("scaffold", "max_extension_reach", 4.5);
            double pingMargin = Math.min(0.5, ping * 0.0025);
            Optional<String> invalid = scaffold.check(player, packet.getHitResult(), baseReach + pingMargin,
                    ConfigManager.get().bool("scaffold", "check_air_place", true));
            if (invalid.isPresent()) {
                flag(player, "scaffold", invalid.get());
                return ConfigManager.get().bool("scaffold", "auto_cancel", true);
            }
        }
        return false;
    }

    public boolean shouldBlockMovement(ServerPlayer player) {
        if (!active() || !ConfigManager.get().isEnabled("inventorywalk")) return false;
        boolean block = inventoryWalk.shouldBlock(player, ConfigManager.get().bool("inventorywalk", "allow_rotation", false));
        if (block) flag(player, "inventorywalk", "movement packet while server container is open");
        return block && ConfigManager.get().bool("inventorywalk", "auto_cancel", true);
    }

    public void onDisconnect(UUID player) {
        violations.save();
        profiles.remove(player); flights.remove(player); speeds.remove(player); jesus.computeIfPresent(player, (id, check) -> { check.clear(id); return null; });
        breaks.remove(player); places.remove(player); lag.remove(player);
    }
    public void savePersistentState() { violations.save(); }
    public void savePersistentStateOnShutdown() { violations.saveNow(); }

    private boolean flag(ServerPlayer player, String id, String detail) {
        ConfigSchema schema = ConfigManager.get();
        if (!schema.isEnabled(id)) return false;
        PlayerProfile profile = profiles.computeIfAbsent(player.getUUID(), ignored -> new PlayerProfile());
        if (!profile.addSignal(id, schema.integer(id, "strictness", 5), schema.integer(id, "buffer_limit", 10), schema.globalSettings.decayPerMinute)) return false;
        double previous = violations.get(player.getUUID(), id, schema.globalSettings.decayPerMinute);
        double level = violations.add(player.getUUID(), id, 1, schema.globalSettings.decayPerMinute);
        notifications.alert(player.level().getServer(), player, id, level, detail);
        int currentRule = schema.ruleIndexFor(level), previousRule = schema.ruleIndexFor(previous);
        if (currentRule >= 0 && (currentRule != previousRule || "WARN".equals(schema.ruleAt(currentRule).get("action").getAsString())))
            punishments.apply(player, id, level, detail, schema.ruleAt(currentRule));
        return true;
    }
    private boolean active() { return ConfigManager.get().globalSettings.enabled; }
}
