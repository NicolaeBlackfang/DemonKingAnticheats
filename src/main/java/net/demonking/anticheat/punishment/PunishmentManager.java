package net.demonking.anticheat.punishment;

import com.google.gson.JsonObject;
import net.demonking.anticheat.config.ConfigManager;
import net.demonking.anticheat.config.ConfigSchema;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanListEntry;

import java.util.Date;
import java.util.Locale;

/** Executes configured warning, kick, and timed/permanent-ban escalation rules. */
public final class PunishmentManager {
    private final WebhookDispatcher webhooks = new WebhookDispatcher();

    public String apply(ServerPlayer player, String check, double violations, String detail, JsonObject rule) {
        String action = rule.get("action").getAsString().toUpperCase(Locale.ROOT);
        ConfigSchema.PunishmentMatrix matrix = ConfigManager.get().punishmentMatrix;
        if (matrix == null || matrix.actions == null || !matrix.actions.enabled(action)) return "DISABLED";
        String message = format(rule.has("message") ? rule.get("message").getAsString() : "Anti-cheat action: %check%", player, check, violations);
        Component component = Component.literal(message);
        switch (action) {
            case "BAN" -> {
                Date expires = null;
                int days = rule.has("duration_days") ? rule.get("duration_days").getAsInt() : 0;
                if (days > 0) expires = new Date(System.currentTimeMillis() + days * 86_400_000L);
                player.level().getServer().getPlayerList().getBans().add(new UserBanListEntry(
                        player.nameAndId(), new Date(), "DemonKingAntiCheats", expires, "DemonKingAntiCheats: " + check));
                player.connection.disconnect(component);
            }
            case "KICK" -> player.connection.disconnect(component);
            default -> player.sendSystemMessage(component);
        }
        if (rule.has("broadcast") && rule.get("broadcast").getAsBoolean())
            player.level().getServer().getPlayerList().broadcastSystemMessage(component, false);
        webhooks.send(player, check, violations, player.connection.latency(), action, detail);
        return action;
    }

    private static String format(String template, ServerPlayer player, String check, double violations) {
        return template.replace("%player%", player.getGameProfile().name())
                .replace("%check%", CheckNames.display(check))
                .replace("%violations%", String.format(Locale.ROOT, "%.1f", violations))
                .replace('&', '\u00a7');
    }
}
