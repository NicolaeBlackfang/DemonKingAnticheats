package com.anticheat.checks.movement;
import com.anticheat.checks.DetectionRules;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;
import java.util.Optional;
public final class SpeedCheck {
    private double x, z; private boolean initialized;
    public Optional<String> check(ServerPlayer p) {
        double nx=p.getX(), nz=p.getZ();
        if (!initialized) { x=nx; z=nz; initialized=true; return Optional.empty(); }
        double dx=nx-x, dz=nz-z; x=nx; z=nz;
        boolean exempt=p.isPassenger() || p.isFallFlying() || p.onClimbable() || p.isInWater() || p.isInLava() || p.isCreative() || p.isSpectator() || p.getAbilities().flying;
        if (exempt || p.isSleeping() || p.isDeadOrDying()) return Optional.empty();
        var speed=p.getEffect(MobEffects.SPEED);
        double effect=speed == null ? 0.0 : (speed.getAmplifier()+1)*0.12;
        double limit=0.72 + effect;
        double distance=Math.sqrt(dx*dx+dz*dz);
        if (DetectionRules.excessiveHorizontalMove(distance,limit)) return Optional.of(String.format("horizontal displacement %.3f blocks per accepted move", distance));
        return Optional.empty();
    }
}
