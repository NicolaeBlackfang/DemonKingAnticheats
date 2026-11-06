package net.demonking.anticheat.checks;

import com.google.gson.JsonObject;
import net.demonking.anticheat.checks.combat.CrystalAuraCheck;
import net.demonking.anticheat.checks.combat.KillAuraCheck;
import net.demonking.anticheat.checks.combat.ReachCheck;
import net.demonking.anticheat.checks.movement.FlightCheck;
import net.demonking.anticheat.checks.movement.JesusCheck;
import net.demonking.anticheat.checks.movement.NoFallCheck;
import net.demonking.anticheat.checks.movement.NoSlowCheck;
import net.demonking.anticheat.checks.movement.SpeedCheck;
import net.demonking.anticheat.checks.player.InventoryWalkCheck;
import net.demonking.anticheat.checks.world.FastBreakCheck;
import net.demonking.anticheat.checks.world.FastPlaceCheck;
import net.demonking.anticheat.checks.world.ScaffoldCheck;
import net.demonking.anticheat.config.ConfigManager;
import net.demonking.anticheat.config.ConfigSchema;
import net.demonking.anticheat.model.PlayerProfile;
import net.demonking.anticheat.model.ViolationTracker;
import net.demonking.anticheat.punishment.NotificationHandler;
import net.demonking.anticheat.punishment.PunishmentManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Routes server events to isolated check modules and shared buffers/escalation. */
public final class CheckManager {
    private static final int UNRELIABLE_RTT_MS = 250;
    private static final String[] IMPLEMENTED = {"fastbreak", "fastplace", "flight", "speed", "reach", "killaura", "jesus", "scaffold", "inventorywalk", "nofall", "noslow", "crystalaura"};
    private final ViolationTracker violations = new ViolationTracker();
    private final NotificationHandler notifications = new NotificationHandler();
    private final PunishmentManager punishments = new PunishmentManager();
    private final Map<UUID, PlayerProfile> profiles = new HashMap<>();
    private final Map<UUID, FlightCheck> flights = new HashMap<>();
    private final Map<UUID, SpeedCheck> speeds = new HashMap<>();
    private final Map<UUID, JesusCheck> jesus = new HashMap<>();
    private final Map<UUID, NoSlowCheck> noSlow = new HashMap<>();
    private final Map<UUID, FastBreakCheck> breaks = new HashMap<>();
    private final Map<UUID, FastPlaceCheck> places = new HashMap<>();
    private final KillAuraCheck killaura = new KillAuraCheck();
    private final ReachCheck reach = new ReachCheck();
    private final CrystalAuraCheck crystalAura = new CrystalAuraCheck();
    private final NoFallCheck noFall = new NoFallCheck();
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
        if (ConfigManager.get().isEnabled("crystalaura")
                && Math.max(0, attacker.connection.latency()) < UNRELIABLE_RTT_MS) {
            Optional<String> crystalViolation = crystalAura.check(attacker, target,
                    ConfigManager.get().integer("crystalaura", "max_crystal_attacks_per_tick", 2));
            if (crystalViolation.isPresent()) {
                boolean registered = flag(attacker, "crystalaura", crystalViolation.get());
                if (registered && ConfigManager.get().bool("crystalaura", "auto_cancel", false)) return true;
            }
        }
        // Combat geometry compares the current server snapshot to an older client action.
        // Suppress geometry flags while either side has a delayed view of that snapshot.
        int attackerPing = Math.max(0, attacker.connection.latency());
        int targetPing = target instanceof ServerPlayer victim ? Math.max(0, victim.connection.latency()) : 0;
        if (Math.max(attackerPing, targetPing) < UNRELIABLE_RTT_MS && ConfigManager.get().isEnabled("killaura")) {
            double reachSetting = ConfigManager.get().decimal("killaura", "max_reach", 3.05);
            Optional<String> aura = killaura.check(attacker, target,
                    ConfigManager.get().decimal("killaura", "max_angle_delta", 60.0), reachSetting,
                    ConfigManager.get().bool("killaura", "check_reach", true),
                    ConfigManager.get().bool("killaura", "target_players", true),
                    ConfigManager.get().bool("killaura", "target_mobs", true),
                    ConfigManager.get().bool("killaura", "target_animals", true));
            if (aura.isPresent()) {
                boolean registered = flag(attacker, "killaura", aura.get());
                if (registered && ConfigManager.get().bool("killaura", "auto_cancel", true)) return true;
            }
        }
        if (target instanceof ServerPlayer victim && Math.max(attackerPing, targetPing) < UNRELIABLE_RTT_MS && ConfigManager.get().isEnabled("reach")) {
            double maxReach = ConfigManager.get().decimal("reach", "max_reach", 3.2);
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
        // Called once from END_SERVER_TICK, after vanilla movement has been applied.
        Optional<String> flight = ConfigManager.get().isEnabled("flight")
                ? flights.computeIfAbsent(player.getUUID(), ignored -> new FlightCheck()).check(player,
                    ConfigManager.get().integer("flight", "hover_ticks", 45),
                    ConfigManager.get().decimal("flight", "vertical_tolerance", 0.005))
                : Optional.empty();
        Optional<String> speed = ConfigManager.get().isEnabled("speed")
                ? speeds.computeIfAbsent(player.getUUID(), ignored -> new SpeedCheck()).check(player,
                ConfigManager.get().decimal("speed", "base_horizontal_per_tick", 0.42))
                : Optional.empty();
        Optional<String> water = ConfigManager.get().isEnabled("jesus")
                ? jesus.computeIfAbsent(player.getUUID(), ignored -> new JesusCheck()).check(player,
                    ConfigManager.get().decimal("jesus", "vertical_tolerance", 0.05),
                    ConfigManager.get().bool("jesus", "require_movement_packet", true))
                : Optional.empty();
        Optional<String> noSlowViolation = ConfigManager.get().isEnabled("noslow")
                ? noSlow.computeIfAbsent(player.getUUID(), ignored -> new NoSlowCheck()).check(player,
                    ConfigManager.get().decimal("noslow", "max_horizontal_per_tick", 0.22))
                : Optional.empty();

        Optional<String> violation = flight.isPresent() ? flight : speed.isPresent() ? speed
                : water.isPresent() ? water : noSlowViolation;
        String checkId = flight.isPresent() ? "flight" : speed.isPresent() ? "speed"
                : water.isPresent() ? "jesus" : "noslow";
        if (violation.isPresent()) {
            flag(player, checkId, violation.get());
        }
    }

    /** Runs after vanilla has moved the player packet onto the server thread. */
    public boolean onMovementPacket(ServerPlayer player, ServerboundMovePlayerPacket packet) {
        if (!active() || !ConfigManager.get().isEnabled("nofall")) return false;
        Optional<String> spoof = noFall.check(player, packet,
                ConfigManager.get().decimal("nofall", "minimum_fall_distance", 4.0));
        if (spoof.isEmpty()) return false;
        flag(player, "nofall", spoof.get());
        return ConfigManager.get().bool("nofall", "cancel_spoofed_ground_packet", true);
    }

    public boolean onBlockBreak(ServerPlayer player, BlockPos pos, BlockState state) {
        if (!active() || !ConfigManager.get().isEnabled("fastbreak")) return true;
        if (Math.max(0, player.connection.latency()) >= UNRELIABLE_RTT_MS) return true;
        Optional<String> result = breaks.computeIfAbsent(player.getUUID(), ignored -> new FastBreakCheck())
                .check(player, pos, state, ConfigManager.get().decimal("fastbreak", "minimum_completion_ratio", 0.65));
        if (result.isEmpty()) return true;
        flag(player, "fastbreak", result.get());
        return !ConfigManager.get().bool("fastbreak", "auto_cancel", true);
    }

    /** Called on the server thread before vanilla applies a block interaction or placement. */
    public boolean onBlockUse(ServerPlayer player, ServerboundUseItemOnPacket packet) {
        if (!active()) return false;
        if (ConfigManager.get().isEnabled("inventorywalk") && inventoryWalk.shouldBlock(player, ConfigManager.get().bool("inventorywalk", "allow_rotation", false))) {
            flag(player, "inventorywalk", "block interaction while server container is open");
            if (ConfigManager.get().bool("inventorywalk", "auto_cancel", true)) return true;
        }
        if (ConfigManager.get().isEnabled("fastplace") && Math.max(0, player.connection.latency()) < UNRELIABLE_RTT_MS) {
            boolean tooFast = places.computeIfAbsent(player.getUUID(), ignored -> new FastPlaceCheck())
                    .tooFast(player, packet.getHand(), ConfigManager.get().integer("fastplace", "min_delay_ms", 50));
            if (tooFast) {
                boolean registered = flag(player, "fastplace", "block placement packets arrived faster than the configured delay");
                if (registered && ConfigManager.get().bool("fastplace", "auto_cancel", true)) return true;
            }
        }
        if (ConfigManager.get().isEnabled("scaffold") && Math.max(0, player.connection.latency()) < UNRELIABLE_RTT_MS) {
            double baseReach = ConfigManager.get().decimal("scaffold", "max_extension_reach", 4.5);
            Optional<String> invalid = scaffold.check(player, packet.getHitResult(), baseReach,
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
        noSlow.remove(player); crystalAura.clear(player); breaks.remove(player); places.remove(player);
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
