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
        if (gap > 250_000_000L) ignoreTimingUntilNanos = now + 750_000_000L;
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
