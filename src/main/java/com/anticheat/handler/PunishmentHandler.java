package com.anticheat.handler;
import com.anticheat.config.ConfigManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
public final class PunishmentHandler {
    public void apply(ServerPlayer player, String check, String punishment, double vl) {
        String action = punishment == null ? ConfigManager.get().general.defaultPunishment : punishment;
        if ("BAN".equalsIgnoreCase(action) && vl < ConfigManager.get().general.maxViolations) return;
        switch (action.toUpperCase()) {
            case "BAN" -> player.connection.disconnect(Component.literal("Banned by Demon King Anti-Cheats: " + check));
            case "KICK" -> player.connection.disconnect(Component.literal("Kicked by Demon King Anti-Cheats: " + check));
            default -> player.sendSystemMessage(Component.literal("Demon King Anti-Cheats warning: " + check));
        }
    }
}
