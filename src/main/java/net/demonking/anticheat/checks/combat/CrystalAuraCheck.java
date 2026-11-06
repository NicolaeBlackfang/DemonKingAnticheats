package net.demonking.anticheat.checks.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Identifies implausibly many crystal attacks in a single server tick without affecting normal swings. */
public final class CrystalAuraCheck {
    private final Map<UUID, TickCount> attacks = new HashMap<>();

    public Optional<String> check(ServerPlayer player, Entity target, int maximumPerTick) {
        if (!(target instanceof EndCrystal) || player.isSpectator() || player.isCreative()) return Optional.empty();
        UUID id = player.getUUID();
        int tick = player.tickCount;
        TickCount count = attacks.compute(id, (ignored, prior) -> prior == null || prior.tick != tick
                ? new TickCount(tick, 1) : new TickCount(tick, prior.count + 1));
        if (count.count > Math.max(1, maximumPerTick)) {
            return Optional.of(String.format("attacked %d end crystals in one server tick", count.count));
        }
        return Optional.empty();
    }

    public void clear(UUID player) { attacks.remove(player); }

    private record TickCount(int tick, int count) {}
}
