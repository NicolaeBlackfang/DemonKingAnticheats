package net.demonking.anticheat.checks.movement;

import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** Detects sustained ground movement above a conservative item-use speed ceiling. */
public final class NoSlowCheck {
    private double previousX;
    private double previousZ;
    private boolean initialized;

    public Optional<String> check(ServerPlayer player, double maximumHorizontalPerTick) {
        double x = player.getX();
        double z = player.getZ();
        if (!initialized) {
            reset(x, z);
            return Optional.empty();
        }

        double dx = x - previousX;
        double dz = z - previousZ;
        reset(x, z);

        if (!player.isUsingItem() || !player.onGround() || player.isSprinting() || player.isPassenger()
                || player.isInWater() || player.isInLava() || player.onClimbable() || player.hurtTime > 0
                || player.isCreative() || player.isSpectator()
                || player.level().getBlockState(player.blockPosition().below()).getBlock().getFriction() > 0.6F) {
            return Optional.empty();
        }

        var speedEffect = player.getEffect(net.minecraft.world.effect.MobEffects.SPEED);
        int speedAmplifier = speedEffect == null ? 0 : speedEffect.getAmplifier() + 1;
        double allowance = Math.max(0.05, maximumHorizontalPerTick) + speedAmplifier * 0.035;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance > allowance) {
            return Optional.of(String.format("moved %.2f blocks while using an item (limit %.2f)", distance, allowance));
        }
        return Optional.empty();
    }

    private void reset(double x, double z) {
        previousX = x;
        previousZ = z;
        initialized = true;
    }
}
