package net.demonking.anticheat.checks.combat;

import net.demonking.anticheat.checks.DetectionRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

public final class ReachCheck {
    public Optional<String> check(ServerPlayer attacker, ServerPlayer target, double maxReach) {
        Vec3 eye = attacker.getEyePosition();
        AABB box = target.getBoundingBox();
        Vec3 nearest = new Vec3(
                Math.max(box.minX, Math.min(box.maxX, eye.x)),
                Math.max(box.minY, Math.min(box.maxY, eye.y)),
                Math.max(box.minZ, Math.min(box.maxZ, eye.z)));
        double reach = eye.distanceTo(nearest);
        if (DetectionRules.excessiveReach(reach, maxReach) && !attacker.isSpectator())
            return Optional.of(String.format("attack distance %.2f exceeds %.2f", reach, maxReach));
        return Optional.empty();
    }
}
