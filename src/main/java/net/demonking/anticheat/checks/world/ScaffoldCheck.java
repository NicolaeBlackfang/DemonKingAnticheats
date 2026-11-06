package net.demonking.anticheat.checks.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;

/** Validates a block-item placement's claimed support face and reach before vanilla handles it. */
public final class ScaffoldCheck {
    public Optional<String> check(ServerPlayer player, BlockHitResult hit, double maximumReach, boolean checkAirPlace) {
        if (!(player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).getItem() instanceof BlockItem)
                && !(player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND).getItem() instanceof BlockItem)) return Optional.empty();
        double reach = player.getEyePosition().distanceTo(hit.getLocation());
        if (reach > maximumReach) return Optional.of(String.format("placement hit %.2f blocks away (limit %.2f)", reach, maximumReach));
        if (checkAirPlace) {
            BlockPos supportPos = hit.getBlockPos();
            BlockState support = player.level().getBlockState(supportPos);
            if (support.isAir() || !support.isFaceSturdy(player.level(), supportPos, hit.getDirection()))
                return Optional.of("block placement packet has no solid supporting face");
        }
        return Optional.empty();
    }
}
