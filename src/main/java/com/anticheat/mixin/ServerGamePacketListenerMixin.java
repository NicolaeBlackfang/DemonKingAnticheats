package com.anticheat.mixin;
import com.anticheat.AntiCheatMod;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
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
    @Inject(method="handleMovePlayer", at=@At("TAIL"))
    private void guardian$checkMovement(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (AntiCheatMod.CHECKS != null && player != null && !player.isRemoved()) AntiCheatMod.CHECKS.onMovement(player);
    }
}
