package net.demonking.anticheat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("demonkinganticheats.json");
    private static ConfigSchema config = ConfigSchema.defaults();
    private static boolean firstRun;
    private ConfigManager() {}

    public static synchronized void load() {
        try {
            Files.createDirectories(FILE.getParent());
            firstRun = !Files.exists(FILE);
            JsonObject defaults = GSON.toJsonTree(ConfigSchema.defaults()).getAsJsonObject();
            JsonObject current = firstRun ? new JsonObject() : JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            if (!current.has("global_settings")) current = migrateLegacy(current, defaults);
            removeDeprecatedLatencyOptions(current);
            retainSupportedChecks(defaults, current);
            mergeDefaults(defaults, current);
            config = GSON.fromJson(current, ConfigSchema.class);
            if (config.globalSettings == null) config.globalSettings = new ConfigSchema.GlobalSettings();
            if (config.globalSettings.discordWebhook == null) config.globalSettings.discordWebhook = new ConfigSchema.DiscordWebhook();
            if (config.punishmentMatrix == null) config.punishmentMatrix = new ConfigSchema.PunishmentMatrix();
            if (config.checks == null) config.checks = ConfigSchema.defaults().checks;
            if (config.punishmentMatrix.escalationLadder == null || config.punishmentMatrix.escalationLadder.isEmpty())
                config.punishmentMatrix.escalationLadder = ConfigSchema.defaults().punishmentMatrix.escalationLadder;
            var rules = new ArrayList<JsonElement>();
            config.punishmentMatrix.escalationLadder.forEach(rule -> {
                if (rule.isJsonObject() && rule.getAsJsonObject().has("violations") && rule.getAsJsonObject().has("action"))
                    rules.add(rule);
            });
            if (rules.isEmpty()) ConfigSchema.defaults().punishmentMatrix.escalationLadder.forEach(rules::add);
            rules.sort(Comparator.comparingInt(rule -> rule.getAsJsonObject().get("violations").getAsInt()));
            config.punishmentMatrix.escalationLadder = new com.google.gson.JsonArray();
            rules.forEach(config.punishmentMatrix.escalationLadder::add);
            Files.writeString(FILE, GSON.toJson(config));
        } catch (Exception e) {
            throw new IllegalStateException("Could not load " + FILE, e);
        }
    }

    private static JsonObject migrateLegacy(JsonObject old, JsonObject defaults) {
        JsonObject migrated = defaults.deepCopy();
        if (old.has("general") && old.get("general").isJsonObject()) {
            JsonObject g = old.getAsJsonObject("general"), n = migrated.getAsJsonObject("global_settings");
            copy(g, n, "enabled", "debugMode");
            if (g.has("decayRatePerMinute")) n.add("decay_per_minute", g.get("decayRatePerMinute"));
        }
        if (old.has("checks") && old.get("checks").isJsonObject()) {
            JsonObject dest = migrated.getAsJsonObject("checks");
            for (Map.Entry<String, JsonElement> entry : old.getAsJsonObject("checks").entrySet()) {
                String key = entry.getKey().replaceAll("([a-z])([A-Z])", "$1$2").toLowerCase(java.util.Locale.ROOT);
                if ("dupeexploits".equals(key)) key = "dupe";
                if (!dest.has(key) || !entry.getValue().isJsonObject()) continue;
                JsonObject from = entry.getValue().getAsJsonObject(), to = dest.getAsJsonObject(key);
                copy(from, to, "enabled", "strictness", "maxReach");
                if (from.has("maxReach")) to.add("max_reach", from.get("maxReach"));
            }
        }
        return migrated;
    }

    private static void copy(JsonObject from, JsonObject to, String... keys) {
        for (String key : keys) if (from.has(key)) to.add(key.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT), from.get(key));
    }

    private static void retainSupportedChecks(JsonObject defaults, JsonObject user) {
        JsonObject supported = defaults.getAsJsonObject("checks");
        JsonObject existing = user.has("checks") && user.get("checks").isJsonObject()
                ? user.getAsJsonObject("checks") : new JsonObject();
        JsonObject cleaned = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : supported.entrySet()) {
            JsonElement value = existing.has(entry.getKey()) ? existing.get(entry.getKey()) : entry.getValue();
            cleaned.add(entry.getKey(), value == null ? entry.getValue().deepCopy() : value.deepCopy());
        }
        user.add("checks", cleaned);
    }

    private static void removeDeprecatedLatencyOptions(JsonObject config) {
        if (!config.has("checks") || !config.get("checks").isJsonObject()) return;
        for (var check : config.getAsJsonObject("checks").entrySet()) {
            if (!check.getValue().isJsonObject()) continue;
            JsonObject options = check.getValue().getAsJsonObject();
            options.remove("ping_tolerance_scale");
            options.remove("ping_margin_per_ms");
            options.remove("max_ping_margin");
            options.remove("base_horizontal_per_packet");
        }
    }

    private static void mergeDefaults(JsonObject defaults, JsonObject user) {
        for (Map.Entry<String, JsonElement> entry : defaults.entrySet()) {
            if (!user.has(entry.getKey()) || user.get(entry.getKey()).isJsonNull()
                    || (entry.getValue().isJsonObject() && !user.get(entry.getKey()).isJsonObject()))
                user.add(entry.getKey(), entry.getValue().deepCopy());
            else if (entry.getValue().isJsonObject() && user.get(entry.getKey()).isJsonObject()) mergeDefaults(entry.getValue().getAsJsonObject(), user.getAsJsonObject(entry.getKey()));
        }
    }

    public static synchronized void save() {
        try { Files.createDirectories(FILE.getParent()); Files.writeString(FILE, GSON.toJson(config)); }
        catch (Exception e) { throw new IllegalStateException("Could not save " + FILE, e); }
    }
    public static ConfigSchema get() { return config; }
    public static boolean isFirstRun() { return firstRun; }
}
