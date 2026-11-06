package net.demonking.anticheat.checks.world;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;

/** Per-player block placement packet-rate gate. */
public final class FastPlaceCheck {
    private long previousPlacementNanos;

    public boolean tooFast(ServerPlayer player, InteractionHand hand, long minimumDelayMillis) {
        if (!(player.getItemInHand(hand).getItem() instanceof BlockItem)) return false;
        long now = System.nanoTime();
        long minimum = Math.max(0, minimumDelayMillis) * 1_000_000L;
        boolean fast = previousPlacementNanos != 0 && now - previousPlacementNanos < minimum;
        previousPlacementNanos = now;
        return fast;
    }
}
