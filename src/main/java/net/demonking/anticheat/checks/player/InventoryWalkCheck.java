package net.demonking.anticheat.checks.player;

import net.minecraft.server.level.ServerPlayer;

/** Uses only server-authoritative container state; the client-only inventory screen is unknowable server-side. */
public final class InventoryWalkCheck {
    public boolean shouldBlock(ServerPlayer player, boolean allowRotation) {
        return player.containerMenu != player.inventoryMenu && !allowRotation;
    }
}
