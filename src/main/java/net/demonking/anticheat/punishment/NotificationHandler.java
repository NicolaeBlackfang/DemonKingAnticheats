package net.demonking.anticheat.punishment;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

import java.util.Locale;
public final class NotificationHandler {
    public void alert(MinecraftServer server, ServerPlayer player, String check, double vl, String detail) {
        String message = "§8[§6✦ §4DEMON KING§8] §e" + player.getGameProfile().name()
                + " §7triggered §c" + CheckNames.display(check)
                + " §8• §f" + detail + " §8[§cVL " + String.format(Locale.ROOT, "%.1f", vl) + "§8]";
        Component msg = Component.literal(message);
        server.getPlayerList().getPlayers().stream()
                .filter(p -> p.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                .forEach(p -> p.sendSystemMessage(msg));
    }
}
