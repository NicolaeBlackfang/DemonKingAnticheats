package com.anticheat.handler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
public final class NotificationHandler {
    public void alert(MinecraftServer server, ServerPlayerEntity player, String check, double vl, String detail) {
        Text msg = Text.literal("[Guardian] " + player.getName().getString() + " failed " + check + " (VL " + String.format("%.1f", vl) + "): " + detail);
        server.getPlayerManager().getPlayerList().stream().filter(p -> p.hasPermissionLevel(2)).forEach(p -> p.sendMessage(msg, false));
    }
}
