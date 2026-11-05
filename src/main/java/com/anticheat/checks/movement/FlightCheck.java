package com.anticheat.checks.movement;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.Optional;
public final class FlightCheck {
    private int airTicks;
    public Optional<String> check(ServerPlayerEntity p) {
        boolean exempt = p.getAbilities().allowFlying || p.isFallFlying() || p.hasVehicle() || p.isTouchingWater() || p.isInLava() || p.isClimbing() || p.isSleeping() || p.isCreative() || p.isSpectator();
        if (exempt || p.isOnGround()) { airTicks = 0; return Optional.empty(); }
        airTicks++;
        if (airTicks > 45 && Math.abs(p.getVelocity().y) < 0.005) return Optional.of("prolonged unsupported hover");
        return Optional.empty();
    }
}
