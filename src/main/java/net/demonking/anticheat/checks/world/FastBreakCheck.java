package net.demonking.anticheat.checks.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/** Uses vanilla's destroy-progress calculation (block, tool and effects) as a conservative timing floor. */
public final class FastBreakCheck {
    private long lastCompletionNanos;

    public Optional<String> check(ServerPlayer player, BlockPos pos, BlockState state, double minimumCompletionRatio) {
        long now = System.nanoTime();
        float progressPerTick = state.getDestroyProgress(player, player.level(), pos);
        if (lastCompletionNanos != 0 && progressPerTick > 0.0F && progressPerTick < 1.0F) {
            double vanillaExpectedMs = (1.0 / progressPerTick) * 50.0;
            double elapsedMs = (now - lastCompletionNanos) / 1_000_000.0;
            double requiredMs = vanillaExpectedMs * Math.max(0.1, Math.min(0.95, minimumCompletionRatio));
            if (elapsedMs < requiredMs) {
                lastCompletionNanos = now;
                return Optional.of(String.format("break interval %.0fms below tool-adjusted floor %.0fms", elapsedMs, requiredMs));
            }
        }
        lastCompletionNanos = now;
        return Optional.empty();
    }
}
