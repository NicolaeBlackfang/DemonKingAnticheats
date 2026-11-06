package net.demonking.anticheat.checks.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/** Rejects an airborne on-ground claim after a fall that should still be accumulating damage. */
public final class NoFallCheck {
    public Optional<String> check(ServerPlayer player, ServerboundMovePlayerPacket packet, double minimumFall) {
        if (!packet.isOnGround() || player.onGround() || player.fallDistance < minimumFall
                || player.isPassenger() || player.isFallFlying() || player.isInWater() || player.isInLava()
                || player.getAbilities().mayfly || player.isCreative() || player.isSpectator()) {
            return Optional.empty();
        }

        double x = packet.getX(player.getX());
        double y = packet.getY(player.getY());
        double z = packet.getZ(player.getZ());
        BlockPos support = BlockPos.containing(x, y - 0.05, z);
        BlockState state = player.level().getBlockState(support);
        if (!state.getCollisionShape(player.level(), support).isEmpty()) return Optional.empty();

        return Optional.of(String.format("claimed ground contact while airborne after falling %.1f blocks", player.fallDistance));
    }
}
