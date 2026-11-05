package com.anticheat.handler;
import com.anticheat.config.ConfigManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
public final class PunishmentHandler {
    public void apply(ServerPlayerEntity player, String check, String punishment, double vl) {
        String action = punishment == null ? ConfigManager.get().general.defaultPunishment : punishment;
        if ("BAN".equalsIgnoreCase(action) && vl < ConfigManager.get().general.maxViolations) return;
        switch (action.toUpperCase()) {
            case "BAN" -> player.networkHandler.disconnect(Text.literal("Banned by Demon King Anti-Cheats: " + check));
            case "KICK" -> player.networkHandler.disconnect(Text.literal("Kicked by Demon King Anti-Cheats: " + check));
            default -> player.sendMessage(Text.literal("Demon King Anti-Cheats warning: " + check), false);
        }
    }
}
