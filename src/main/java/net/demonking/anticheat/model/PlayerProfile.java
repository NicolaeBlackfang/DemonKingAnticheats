package net.demonking.anticheat.model;

import java.util.HashMap;
import java.util.Map;

/** Per-session token buckets used to absorb isolated, ambiguous check signals. */
public final class PlayerProfile {
    private final Map<String, Buffer> buffers = new HashMap<>();

    public boolean addSignal(String check, int strictness, int bufferLimit, double decayPerMinute) {
        Buffer buffer = buffers.computeIfAbsent(check, ignored -> new Buffer());
        long now = System.nanoTime();
        if (buffer.updatedAt != 0) {
            double elapsedMinutes = Math.max(0, (now - buffer.updatedAt) / 60_000_000_000.0);
            buffer.points = Math.max(0, buffer.points - decayPerMinute * elapsedMinutes);
        }
        buffer.updatedAt = now;
        int requiredSignals = Math.max(1, 11 - Math.max(1, Math.min(10, strictness)));
        buffer.points = Math.min(Math.max(requiredSignals, bufferLimit), buffer.points + 1.0);
        if (buffer.points < requiredSignals) return false;
        buffer.points -= requiredSignals;
        return true;
    }

    private static final class Buffer {
        double points;
        long updatedAt;
    }
}
