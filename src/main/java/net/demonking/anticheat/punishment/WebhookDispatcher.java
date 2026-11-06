package net.demonking.anticheat.punishment;

import net.demonking.anticheat.config.ConfigManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Sends bounded, asynchronous Discord notifications without blocking the server thread. */
public final class WebhookDispatcher {
    private static final Gson GSON = new Gson();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "demonkinganticheats-webhook");
        thread.setDaemon(true);
        return thread;
    });
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).executor(EXECUTOR).build();

    public void send(ServerPlayer player, String check, double violations, int ping, String action, String detail) {
        var config = ConfigManager.get().globalSettings.discordWebhook;
        if (config == null || !config.enabled || config.url == null || config.url.isBlank()) return;
        try {
            URI uri = URI.create(config.url);
            if (!"https".equalsIgnoreCase(uri.getScheme())) return;
            JsonObject embed = new JsonObject();
            embed.addProperty("title", "Anti-cheat alert");
            embed.addProperty("color", config.embedColor);
            JsonArray fields = new JsonArray();
            addField(fields, "Player", player.getGameProfile().name());
            addField(fields, "Check", CheckNames.display(check));
            addField(fields, "Violations", String.format("%.1f", violations));
            addField(fields, "Ping", ping + " ms");
            addField(fields, "Action", action);
            addField(fields, "Detail", detail == null || detail.isBlank() ? "No extra detail" : detail);
            embed.add("fields", fields);
            JsonObject body = new JsonObject();
            body.addProperty("username", config.username == null || config.username.isBlank() ? "Demon King Anti-Cheats" : config.username);
            JsonArray embeds = new JsonArray();
            embeds.add(embed);
            body.add("embeds", embeds);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(Math.max(1, Math.min(15, config.timeoutSeconds))))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body))).build();
            CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(response -> {
                        if (response.statusCode() < 200 || response.statusCode() >= 300)
                            net.demonking.anticheat.DemonKingAntiCheat.LOGGER.warn("Webhook returned HTTP {}", response.statusCode());
                    }).exceptionally(error -> {
                        net.demonking.anticheat.DemonKingAntiCheat.LOGGER.warn("Webhook delivery failed: {}", error.toString());
                        return null;
                    });
        } catch (IllegalArgumentException ex) {
            net.demonking.anticheat.DemonKingAntiCheat.LOGGER.warn("Webhook URL is invalid; notification skipped");
        }
    }

    private static void addField(JsonArray fields, String name, String value) {
        JsonObject field = new JsonObject();
        field.addProperty("name", name);
        field.addProperty("value", value);
        field.addProperty("inline", true);
        fields.add(field);
    }
}
