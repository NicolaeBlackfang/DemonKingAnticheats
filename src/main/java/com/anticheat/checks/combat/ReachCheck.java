package com.anticheat.checks.combat;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.Optional;
public final class ReachCheck {
    public Optional<String> check(ServerPlayerEntity attacker, ServerPlayerEntity target, double maxReach) {
        double reach=Math.sqrt(attacker.squaredDistanceTo(target));
        if (reach>maxReach && !attacker.isSpectator()) return Optional.of(String.format("attack distance %.2f exceeds %.2f",reach,maxReach));
        return Optional.empty();
    }
}
