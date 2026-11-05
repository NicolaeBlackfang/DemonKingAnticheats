package com.anticheat.checks.movement;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.Optional;
public final class SpeedCheck {
    private double x, z; private boolean initialized;
    public Optional<String> check(ServerPlayerEntity p) {
        double nx=p.getX(), nz=p.getZ();
        if (!initialized) { x=nx; z=nz; initialized=true; return Optional.empty(); }
        double dx=nx-x, dz=nz-z; x=nx; z=nz;
        boolean exempt=p.hasVehicle() || p.isFallFlying() || p.isClimbing() || p.isTouchingWater() || p.isInLava() || p.isCreative() || p.isSpectator() || p.getAbilities().flying;
        if (exempt || p.isSleeping() || p.isDead()) return Optional.empty();
        var speed=p.getStatusEffect(StatusEffects.SPEED);
        double effect=speed == null ? 0.0 : (speed.getAmplifier()+1)*0.12;
        double limit=0.72 + effect;
        double distance=Math.sqrt(dx*dx+dz*dz);
        if (distance>limit) return Optional.of(String.format("horizontal displacement %.3f blocks per accepted move", distance));
        return Optional.empty();
    }
}
