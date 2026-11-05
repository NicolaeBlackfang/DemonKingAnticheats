package com.anticheat.data;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public final class ViolationTracker {
    private final Map<UUID, Map<String, Double>> violations = new ConcurrentHashMap<>();
    private final Map<UUID, Long> updated = new ConcurrentHashMap<>();
    public double add(UUID player, String check, double amount, double decayPerMinute) {
        long now = System.nanoTime();
        Map<String, Double> checks = violations.computeIfAbsent(player, k -> new ConcurrentHashMap<>());
        long previous = updated.getOrDefault(player, now);
        double elapsed = Math.max(0, (now - previous) / 60_000_000_000.0);
        updated.put(player, now);
        return checks.compute(check, (k, old) -> Math.max(0, old == null ? 0 : old - decayPerMinute * elapsed) + amount);
    }
    public void clear(UUID player) { violations.remove(player); updated.remove(player); }
}
