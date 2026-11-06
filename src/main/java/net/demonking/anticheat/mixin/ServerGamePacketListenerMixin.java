package net.demonking.anticheat.mixin;
import net.demonking.anticheat.DemonKingAntiCheat;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
    @Shadow @Final private ServerPlayer player;
    @Inject(method="handleMovePlayer", at=@At("HEAD"), cancellable=true)
    private void demonking$inventoryMovement(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (DemonKingAntiCheat.CHECKS != null && player != null && DemonKingAntiCheat.CHECKS.shouldBlockMovement(player)) ci.cancel();
    }
    @Inject(method="handleMovePlayer", at=@At("TAIL"))
    private void demonking$checkMovement(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (DemonKingAntiCheat.CHECKS != null && player != null && !player.isRemoved()) DemonKingAntiCheat.CHECKS.onMovement(player);
    }

    @Inject(method="handleUseItemOn", at=@At("HEAD"), cancellable=true)
    private void demonking$validatePlacement(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        if (DemonKingAntiCheat.CHECKS != null && player != null && DemonKingAntiCheat.CHECKS.onBlockUse(player, packet)) ci.cancel();
    }
}
