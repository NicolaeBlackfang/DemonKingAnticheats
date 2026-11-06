package net.demonking.anticheat.checks.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

/** Server-side attack cone, distance and target-category validation. */
public final class KillAuraCheck {
    public Optional<String> check(ServerPlayer attacker, Entity target, double maxAngleDegrees, double maxReach,
                                  boolean checkReach, boolean players, boolean mobs, boolean animals) {
        if (!(target instanceof LivingEntity living) || !living.isAlive()) return Optional.empty();
        if (target instanceof Player && !players) return Optional.empty();
        if (target instanceof Animal && !animals) return Optional.empty();
        if (!(target instanceof Player) && !(target instanceof Animal) && !mobs) return Optional.empty();
        Vec3 targetPoint = target.getBoundingBox().getCenter();
        Vec3 toTarget = targetPoint.subtract(attacker.getEyePosition());
        var box = target.getBoundingBox();
        Vec3 eyePosition = attacker.getEyePosition();
        Vec3 nearestPoint = new Vec3(Math.max(box.minX, Math.min(box.maxX, eyePosition.x)),
                Math.max(box.minY, Math.min(box.maxY, eyePosition.y)), Math.max(box.minZ, Math.min(box.maxZ, eyePosition.z)));
        double distance = eyePosition.distanceTo(nearestPoint);
        if (checkReach && distance > maxReach) return Optional.of(String.format("attack ray reached target at %.2f blocks (limit %.2f)", distance, maxReach));
        if (distance < 1.0e-5) return Optional.empty();
        Vec3 eye = eyePosition;
        Vec3 rayEnd = eye.add(attacker.getViewVector(1.0F).normalize().scale(checkReach ? Math.min(maxReach, distance + 0.3) : distance + 0.3));
        AABB hitbox = target.getBoundingBox().inflate(0.08);
        if (hitbox.clip(eye, rayEnd).isPresent()) return Optional.empty();
        double dot = Math.max(-1.0, Math.min(1.0, attacker.getViewVector(1.0F).normalize().dot(toTarget.normalize())));
        double angle = Math.toDegrees(Math.acos(dot));
        if (angle > maxAngleDegrees) return Optional.of(String.format("attack target was %.1f degrees outside the server view direction", angle));
        return Optional.empty();
    }
}
