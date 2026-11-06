package net.demonking.anticheat.checks.movement;

import net.demonking.anticheat.checks.DetectionRules;
import net.minecraft.server.level.ServerPlayer;
import java.util.Optional;

public final class FlightCheck {
    private int airTicks;

    public Optional<String> check(ServerPlayer p, int hoverThreshold, double verticalTolerance) {
        boolean exempt = p.getAbilities().mayfly || p.isFallFlying() || p.isPassenger() || p.isInWater() || p.isInLava()
                || p.onClimbable() || p.isSleeping() || p.isCreative() || p.isSpectator();
        if (exempt || p.onGround()) {
            airTicks = 0;
            return Optional.empty();
        }
        airTicks++;
        if (airTicks > hoverThreshold && Math.abs(p.getDeltaMovement().y) < verticalTolerance)
            return Optional.of("prolonged unsupported hover");
        return Optional.empty();
    }
}
