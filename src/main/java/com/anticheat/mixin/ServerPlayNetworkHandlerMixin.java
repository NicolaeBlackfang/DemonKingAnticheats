package com.anticheat.mixin;
import com.anticheat.AntiCheatMod;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow @Final private ServerPlayerEntity player;
    @Inject(method="onPlayerMove", at=@At("TAIL"))
    private void guardian$checkMovement(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        if (AntiCheatMod.CHECKS != null && player != null && !player.isRemoved()) AntiCheatMod.CHECKS.onMovement(player);
    }
}
