package com.anticheat.checks.movement;
import com.anticheat.checks.DetectionRules;
import net.minecraft.server.level.ServerPlayer;
import java.util.Optional;
public final class FlightCheck {
    private int airTicks;
    public Optional<String> check(ServerPlayer p) {
        boolean exempt = p.getAbilities().mayfly || p.isFallFlying() || p.isPassenger() || p.isInWater() || p.isInLava() || p.onClimbable() || p.isSleeping() || p.isCreative() || p.isSpectator();
        if (exempt || p.onGround()) { airTicks = 0; return Optional.empty(); }
        airTicks++;
        if (DetectionRules.unsupportedHover(airTicks, p.getDeltaMovement().y)) return Optional.of("prolonged unsupported hover");
        return Optional.empty();
    }
}
