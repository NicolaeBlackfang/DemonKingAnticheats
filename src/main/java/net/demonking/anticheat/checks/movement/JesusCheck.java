package net.demonking.anticheat.checks.movement;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Detects repeated unsupported, near-zero vertical movement just above water. */
public final class JesusCheck {
    private final Map<UUID, Integer> surfaceTicks = new HashMap<>();
    private final Map<UUID, Double> previousVerticalVelocity = new HashMap<>();

    public Optional<String> check(ServerPlayer player, double verticalTolerance, boolean requireMovementPacket) {
        UUID id = player.getUUID();
        boolean waterBelow = player.level().getFluidState(player.blockPosition().below()).is(FluidTags.WATER);
        boolean exempt = player.isSwimming() || player.isPassenger() || player.isFallFlying()
                || player.getAbilities().flying || player.isInWater() || player.isInLava();
        if (!waterBelow || exempt || player.onGround()) {
            surfaceTicks.remove(id);
            previousVerticalVelocity.remove(id);
            return Optional.empty();
        }
        double verticalVelocity = player.getDeltaMovement().y;
        double prior = previousVerticalVelocity.getOrDefault(id, verticalVelocity);
        previousVerticalVelocity.put(id, verticalVelocity);
        double velocityDelta = Math.abs(verticalVelocity - prior);
        if (Math.abs(verticalVelocity) <= Math.max(0.0, verticalTolerance)
                && velocityDelta <= Math.max(0.0, verticalTolerance)) {
            int ticks = surfaceTicks.merge(id, 1, Integer::sum);
            if (ticks >= (requireMovementPacket ? 6 : 10)) return Optional.of("unsupported stable movement above water surface");
        } else surfaceTicks.remove(id);
        return Optional.empty();
    }

    public void clear(UUID player) { surfaceTicks.remove(player); previousVerticalVelocity.remove(player); }
}
