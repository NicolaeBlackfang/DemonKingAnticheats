package net.demonking.anticheat.mixin;

import net.demonking.anticheat.DemonKingAntiCheat;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
    private static final String ENSURE_SERVER_THREAD =
            "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(" +
                    "Lnet/minecraft/network/protocol/Packet;" +
                    "Lnet/minecraft/network/PacketListener;" +
                    "Lnet/minecraft/server/level/ServerLevel;)V";

    @Shadow @Final private ServerPlayer player;

    /** Run after PacketUtils has moved the packet onto the authoritative server thread. */
    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = ENSURE_SERVER_THREAD,
            shift = At.Shift.AFTER), cancellable = true)
    private void demonking$validateMovement(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (DemonKingAntiCheat.CHECKS == null || player == null) return;
        if (DemonKingAntiCheat.CHECKS.shouldBlockMovement(player)
                || DemonKingAntiCheat.CHECKS.onMovementPacket(player, packet)) {
            ci.cancel();
        }
    }

    /** Check actual serverbound attacks; AttackEntityCallback is a client-side hook. */
    @Inject(method = "handleAttack", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/server/level/ServerPlayer;isWithinAttackRange(" +
                    "Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/AABB;D)Z", shift = At.Shift.BEFORE),
            cancellable = true)
    private void demonking$validateAttack(ServerboundAttackPacket packet, CallbackInfo ci) {
        if (DemonKingAntiCheat.CHECKS == null || player == null) return;
        Entity target = player.level().getEntityOrPart(packet.entityId());
        if (target != null && DemonKingAntiCheat.CHECKS.onAttack(player, target)) ci.cancel();
    }

    @Inject(method = "handleUseItemOn", at = @At(value = "INVOKE", target = ENSURE_SERVER_THREAD,
            shift = At.Shift.AFTER), cancellable = true)
    private void demonking$validatePlacement(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        if (DemonKingAntiCheat.CHECKS != null && player != null
                && DemonKingAntiCheat.CHECKS.onBlockUse(player, packet)) {
            ci.cancel();
        }
    }
}
