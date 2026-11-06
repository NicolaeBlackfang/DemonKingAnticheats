package com.anticheat.checks.combat;
import com.anticheat.checks.DetectionRules;
import net.minecraft.server.level.ServerPlayer;
import java.util.Optional;
public final class ReachCheck {
    public Optional<String> check(ServerPlayer attacker, ServerPlayer target, double maxReach) {
        double reach=Math.sqrt(attacker.distanceToSqr(target));
        if (DetectionRules.excessiveReach(reach,maxReach) && !attacker.isSpectator()) return Optional.of(String.format("attack distance %.2f exceeds %.2f",reach,maxReach));
        return Optional.empty();
    }
}
