package net.demonking.anticheat.punishment;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;
public final class NotificationHandler {
    public void alert(MinecraftServer server, ServerPlayer player, String check, double vl, String detail) {
        Component msg = Component.literal("[Demon King Anti-Cheats] " + player.getGameProfile().name() + " failed " + check + " (VL " + String.format("%.1f", vl) + "): " + detail);
        server.getPlayerList().getPlayers().stream()
                .filter(p -> p.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                .forEach(p -> p.sendSystemMessage(msg));
    }
}
