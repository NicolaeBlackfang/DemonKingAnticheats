package com.anticheat.checks.combat;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;
public final class KillAuraCheck {
    private final Map<UUID, Long> lastAttack = new HashMap<>();
    private final Map<UUID, Integer> rapid = new HashMap<>();
    public void clear(UUID player) { lastAttack.remove(player); rapid.remove(player); }
    public Optional<String> check(ServerPlayerEntity attacker, ServerPlayerEntity target) {
        long now=System.nanoTime(), previous=lastAttack.getOrDefault(attacker.getUuid(),now);
        lastAttack.put(attacker.getUuid(),now);
        int streak= now-previous < 85_000_000L ? rapid.merge(attacker.getUuid(),1,Integer::sum) : 0;
        if (streak>=12) { rapid.put(attacker.getUuid(),0); return Optional.of("sustained attacks faster than server combat timing"); }
        return Optional.empty();
    }
}
