package com.anticheat.checks.combat;
import com.anticheat.checks.DetectionRules;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
public final class KillAuraCheck {
    private final Map<UUID, Long> lastAttack = new HashMap<>();
    private final Map<UUID, Integer> rapid = new HashMap<>();
    public void clear(UUID player) { lastAttack.remove(player); rapid.remove(player); }
    public Optional<String> check(ServerPlayer attacker, ServerPlayer target) {
        long now=System.nanoTime(), previous=lastAttack.getOrDefault(attacker.getUUID(),now);
        lastAttack.put(attacker.getUUID(),now);
        int streak= now-previous < 85_000_000L ? rapid.merge(attacker.getUUID(),1,Integer::sum) : 0;
        if (DetectionRules.sustainedRapidAttacks(streak)) { rapid.put(attacker.getUUID(),0); return Optional.of("sustained attacks faster than server combat timing"); }
        return Optional.empty();
    }
}
