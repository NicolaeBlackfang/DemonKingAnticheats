package net.demonking.anticheat.lag;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

/** Measures actual server tick duration so movement checks can grant grace during TPS stalls. */
public final class ServerLagMonitor {
    private static volatile long tickStartedNanos;
    private static volatile double averageTickMillis = 50.0;
    private static int tickCount;

    private ServerLagMonitor() {}

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> tickStartedNanos = System.nanoTime());
        ServerTickEvents.END_SERVER_TICK.register(ServerLagMonitor::recordTick);
    }

    private static void recordTick(MinecraftServer server) {
        long start = tickStartedNanos;
        if (start == 0) return;
        double duration = (System.nanoTime() - start) / 1_000_000.0;
        averageTickMillis = averageTickMillis * 0.9 + Math.min(1000.0, duration) * 0.1;
        if (++tickCount >= 600) {
            tickCount = 0;
            if (net.demonking.anticheat.DemonKingAntiCheat.CHECKS != null) net.demonking.anticheat.DemonKingAntiCheat.CHECKS.savePersistentState();
        }
    }

    public static boolean isTickLagging(double minimumTps) {
        double allowedMillis = 1000.0 / Math.max(1.0, minimumTps);
        return averageTickMillis > allowedMillis;
    }
}
