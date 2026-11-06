package net.demonking.anticheat.checks.movement;

import net.demonking.anticheat.checks.DetectionRules;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;
import java.util.Optional;

public final class SpeedCheck {
    private double x, z;
    private boolean initialized;
    private long lastSampleNanos;
    private long ignoreTimingUntilNanos;

    /** Establish a fresh baseline after a network stall or server correction. */
    public void reset(ServerPlayer p) {
        x = p.getX();
        z = p.getZ();
        lastSampleNanos = System.nanoTime();
        ignoreTimingUntilNanos = lastSampleNanos + 500_000_000L;
        initialized = true;
    }

    public Optional<String> check(ServerPlayer p, double allowedDistance) {
        double nx = p.getX(), nz = p.getZ();
        long now = System.nanoTime();
        if (!initialized) {
            x = nx;
            z = nz;
            lastSampleNanos = now;
            initialized = true;
            return Optional.empty();
        }
        double dx = nx - x, dz = nz - z;
        x = nx;
        z = nz;
        long gap = now - lastSampleNanos;
        lastSampleNanos = now;
        // Never compare positions separated by a packet stall: the accumulated
        // legitimate movement is not a per-packet speed sample.
        if (gap > 200_000_000L) {
            ignoreTimingUntilNanos = now + 500_000_000L;
            return Optional.empty();
        }
        if (now < ignoreTimingUntilNanos) return Optional.empty();
        boolean exempt = p.isPassenger() || p.isFallFlying() || p.onClimbable() || p.isInWater() || p.isInLava()
                || p.isCreative() || p.isSpectator() || p.getAbilities().flying || p.isUsingItem() || p.hurtTime > 0;
        exempt |= p.level().getBlockState(p.blockPosition().below()).getBlock().getFriction() > 0.6F;
        if (exempt || p.isSleeping() || p.isDeadOrDying())
            return Optional.empty();
        var speed = p.getEffect(MobEffects.SPEED);
        double effect = speed == null ? 0.0 : (speed.getAmplifier() + 1) * 0.08;
        double limit = Math.max(0.1, allowedDistance) + effect;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (DetectionRules.excessiveHorizontalMove(distance, limit))
            return Optional.of(String.format("horizontal displacement %.3f blocks per accepted move", distance));
        return Optional.empty();
    }
}
