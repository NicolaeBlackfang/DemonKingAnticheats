package net.demonking.anticheat.lag;

import net.minecraft.server.level.ServerPlayer;

/** Tracks movement packet cadence and applies a grace window during latency, jitter or TPS spikes. */
public final class LagCompensationManager {
    private long lastPacketNanos;
    private double jitterMs;
    private double lastIntervalMs = 50.0;
    private double lastX, lastY, lastZ;
    private float lastYaw, lastPitch;
    private boolean initialized;
    private int consecutiveCorrections;

    public boolean inGraceWindow(ServerPlayer player, int highPingMs, int highJitterMs, double lowTpsThreshold) {
        long now = System.nanoTime();
        if (lastPacketNanos != 0) {
            lastIntervalMs = (now - lastPacketNanos) / 1_000_000.0;
            double deviation = Math.abs(lastIntervalMs - 50.0);
            jitterMs = jitterMs == 0 ? deviation : jitterMs * 0.8 + deviation * 0.2;
        }
        lastPacketNanos = now;
        int ping = Math.max(0, player.connection.latency());
        return ping >= highPingMs || jitterMs >= highJitterMs || lastIntervalMs >= 250.0
                || ServerLagMonitor.isTickLagging(lowTpsThreshold);
    }

    public double currentJitterMillis() { return jitterMs; }

    public void rememberSafePosition(ServerPlayer player) {
        lastX = player.getX(); lastY = player.getY(); lastZ = player.getZ();
        lastYaw = player.getYRot(); lastPitch = player.getXRot();
        initialized = true;
    }

    /** Corrects early suspicious movement before it can reach an escalating punishment. */
    public boolean correctBeforePunish(ServerPlayer player, int correctionLimit) {
        consecutiveCorrections++;
        if (initialized && consecutiveCorrections <= Math.max(0, correctionLimit)) {
            player.connection.teleport(lastX, lastY, lastZ, lastYaw, lastPitch);
            return true;
        }
        return false;
    }

    public void accepted() { consecutiveCorrections = 0; }
}
