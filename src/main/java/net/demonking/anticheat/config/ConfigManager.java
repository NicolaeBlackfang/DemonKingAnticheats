package net.demonking.anticheat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.io.InputStreamReader;
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
            JsonObject defaults = loadBundledDefaults();
            JsonObject current = firstRun ? new JsonObject() : readCurrentConfig(defaults);
            migrateLegacyPunishmentDefaults(current, defaults);
            if (!current.has("global_settings")) current = migrateLegacy(current, defaults);
            removeDeprecatedLatencyOptions(current);
            retainSupportedChecks(defaults, current);
            mergeDefaults(defaults, current);
            normalizeGlobalSettings(defaults, current);
            normalizePunishmentActions(defaults, current);
            current.add("config_version", defaults.get("config_version").deepCopy());
            normalizePunishmentLadder(current, defaults);
            ConfigSchema defaultSchema = GSON.fromJson(defaults, ConfigSchema.class);
            config = GSON.fromJson(current, ConfigSchema.class);
            if (config.globalSettings == null) config.globalSettings = defaultSchema.globalSettings;
            if (config.globalSettings.discordWebhook == null) config.globalSettings.discordWebhook = defaultSchema.globalSettings.discordWebhook;
            if (config.punishmentMatrix == null) config.punishmentMatrix = defaultSchema.punishmentMatrix;
            if (config.checks == null) config.checks = defaultSchema.checks;
            if (config.punishmentMatrix.escalationLadder == null || config.punishmentMatrix.escalationLadder.isEmpty())
                config.punishmentMatrix.escalationLadder = defaultSchema.punishmentMatrix.escalationLadder;
            Files.writeString(FILE, GSON.toJson(config));
        } catch (Exception e) {
            throw new IllegalStateException("Could not load " + FILE, e);
        }
    }

    private static JsonObject loadBundledDefaults() throws java.io.IOException {
        try (var stream = ConfigManager.class.getResourceAsStream("/demonkinganticheats.json")) {
            if (stream == null) return GSON.toJsonTree(ConfigSchema.defaults()).getAsJsonObject();
            JsonElement parsed = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new IllegalStateException("Bundled config template must be a JSON object");
            return parsed.getAsJsonObject();
        }
    }

    private static JsonObject readCurrentConfig(JsonObject defaults) throws java.io.IOException {
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(FILE));
            if (!parsed.isJsonObject()) throw new IllegalStateException("Config root must be a JSON object");
            return parsed.getAsJsonObject();
        } catch (RuntimeException malformed) {
            Path backup = FILE.resolveSibling(FILE.getFileName() + ".invalid-" + System.currentTimeMillis() + ".bak");
            Files.copy(FILE, backup, StandardCopyOption.REPLACE_EXISTING);
            net.demonking.anticheat.DemonKingAntiCheat.LOGGER.error(
                    "Invalid config was backed up to {}; writing safe defaults", backup.getFileName(), malformed);
            return defaults.deepCopy();
        }
    }

    private static JsonObject migrateLegacy(JsonObject old, JsonObject defaults) {
        JsonObject migrated = defaults.deepCopy();
        if (old.has("general") && old.get("general").isJsonObject()) {
            JsonObject g = old.getAsJsonObject("general"), n = migrated.getAsJsonObject("global_settings");
            copy(g, n, "enabled");
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
            JsonObject template = entry.getValue().getAsJsonObject();
            JsonObject supplied = existing.has(entry.getKey()) && existing.get(entry.getKey()).isJsonObject()
                    ? existing.getAsJsonObject(entry.getKey()) : new JsonObject();
            JsonObject options = new JsonObject();
            for (Map.Entry<String, JsonElement> option : template.entrySet()) {
                JsonElement value = supplied.get(option.getKey());
                if (!sameJsonType(option.getValue(), value)) value = option.getValue();
                options.add(option.getKey(), value.deepCopy());
            }
            clampCheckOptions(entry.getKey(), options);
            cleaned.add(entry.getKey(), options);
        }
        user.add("checks", cleaned);
    }

    private static boolean sameJsonType(JsonElement template, JsonElement supplied) {
        if (supplied == null || supplied.isJsonNull() || !supplied.isJsonPrimitive()) return false;
        var expected = template.getAsJsonPrimitive();
        var actual = supplied.getAsJsonPrimitive();
        return expected.isBoolean() == actual.isBoolean()
                && expected.isNumber() == actual.isNumber()
                && expected.isString() == actual.isString();
    }

    private static void normalizeGlobalSettings(JsonObject defaults, JsonObject config) {
        JsonObject schema = defaults.getAsJsonObject("global_settings");
        JsonObject supplied = config.getAsJsonObject("global_settings");
        config.add("global_settings", normalizeObject(schema, supplied));
        JsonObject global = config.getAsJsonObject("global_settings");
        clamp(global, "decay_per_minute", 0, 10);
        JsonObject webhook = global.getAsJsonObject("discord_webhook");
        clamp(webhook, "embed_color", 0, 16777215);
        clamp(webhook, "timeout_seconds", 1, 15);
    }

    private static JsonObject normalizeObject(JsonObject schema, JsonObject supplied) {
        JsonObject normalized = new JsonObject();
        for (Map.Entry<String, JsonElement> field : schema.entrySet()) {
            JsonElement value = supplied.get(field.getKey());
            if (field.getValue().isJsonObject()) {
                JsonObject nested = value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
                normalized.add(field.getKey(), normalizeObject(field.getValue().getAsJsonObject(), nested));
            } else {
                normalized.add(field.getKey(), sameJsonType(field.getValue(), value)
                        ? value.deepCopy() : field.getValue().deepCopy());
            }
        }
        return normalized;
    }

    private static void clampCheckOptions(String id, JsonObject options) {
        clamp(options, "strictness", 1, 10);
        clamp(options, "buffer_limit", 1, 100);
        switch (id) {
            case "killaura" -> { clamp(options, "max_reach", 2.5, 6.0); clamp(options, "max_angle_delta", 5, 180); }
            case "reach" -> clamp(options, "max_reach", 2.5, 6.0);
            case "speed" -> clamp(options, "max_horizontal_per_packet", 0.5, 2.0);
            case "flight" -> { clamp(options, "hover_ticks", 10, 1200); clamp(options, "vertical_tolerance", 0.001, 0.5); }
            case "jesus" -> clamp(options, "vertical_tolerance", 0.001, 0.5);
            case "fastbreak" -> clamp(options, "minimum_completion_ratio", 0.1, 0.95);
            case "fastplace" -> clamp(options, "min_delay_ms", 0, 1000);
            case "scaffold" -> clamp(options, "max_extension_reach", 2.5, 6.0);
            case "nofall" -> clamp(options, "minimum_fall_distance", 2, 100);
            case "noslow" -> clamp(options, "max_horizontal_per_tick", 0.05, 1.0);
            case "crystalaura" -> clamp(options, "max_crystal_attacks_per_tick", 1, 20);
            default -> { }
        }
    }

    private static void clamp(JsonObject object, String key, double min, double max) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive() || !object.get(key).getAsJsonPrimitive().isNumber()) return;
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value)) value = min;
        object.addProperty(key, Math.max(min, Math.min(max, value)));
    }

    private static void normalizePunishmentLadder(JsonObject config, JsonObject defaults) {
        JsonObject configured = config.getAsJsonObject("punishment_matrix");
        JsonElement ladderElement = configured.get("escalation_ladder");
        if (ladderElement == null || !ladderElement.isJsonArray()) {
            configured.add("escalation_ladder", defaults.getAsJsonObject("punishment_matrix")
                    .getAsJsonArray("escalation_ladder").deepCopy());
            return;
        }

        var rules = new ArrayList<JsonObject>();
        for (JsonElement element : ladderElement.getAsJsonArray()) {
            if (!element.isJsonObject()) continue;
            JsonObject supplied = element.getAsJsonObject();
            if (!supplied.has("violations") || !supplied.get("violations").isJsonPrimitive()
                    || !supplied.getAsJsonPrimitive("violations").isNumber()
                    || !supplied.has("action") || !supplied.get("action").isJsonPrimitive()
                    || !supplied.getAsJsonPrimitive("action").isString()) continue;
            double threshold = supplied.get("violations").getAsDouble();
            if (!Double.isFinite(threshold) || threshold < 0) continue;
            String action = supplied.get("action").getAsString().toUpperCase(java.util.Locale.ROOT);
            if (!action.equals("WARN") && !action.equals("KICK") && !action.equals("BAN")) continue;

            JsonObject rule = new JsonObject();
            rule.addProperty("violations", threshold);
            rule.addProperty("action", action);
            rule.addProperty("broadcast", supplied.has("broadcast")
                    && supplied.get("broadcast").isJsonPrimitive()
                    && supplied.getAsJsonPrimitive("broadcast").isBoolean()
                    ? supplied.get("broadcast").getAsBoolean() : true);
            rule.addProperty("message", supplied.has("message") && supplied.get("message").isJsonPrimitive()
                    && supplied.getAsJsonPrimitive("message").isString()
                    ? supplied.get("message").getAsString() : "&8[&6✦ &4DEMON KING&8] &e%player% &7action for &c%check%&7.");
            if (supplied.has("duration_days") && supplied.get("duration_days").isJsonPrimitive()
                    && supplied.getAsJsonPrimitive("duration_days").isNumber()) {
                rule.addProperty("duration_days", Math.max(0, Math.min(36500, supplied.get("duration_days").getAsInt())));
            }
            rules.add(rule);
        }
        if (rules.isEmpty()) {
            config.getAsJsonObject("punishment_matrix").add("escalation_ladder",
                    defaults.getAsJsonObject("punishment_matrix").getAsJsonArray("escalation_ladder").deepCopy());
            return;
        }
        rules.sort(Comparator.comparingDouble(rule -> rule.get("violations").getAsDouble()));
        com.google.gson.JsonArray normalized = new com.google.gson.JsonArray();
        rules.forEach(normalized::add);
        configured.add("escalation_ladder", normalized);
    }

    private static void normalizePunishmentActions(JsonObject defaults, JsonObject config) {
        JsonObject matrix = config.getAsJsonObject("punishment_matrix");
        JsonObject defaultMatrix = defaults.getAsJsonObject("punishment_matrix");
        JsonElement supplied = matrix.get("actions");
        JsonObject actions = supplied != null && supplied.isJsonObject()
                ? supplied.getAsJsonObject() : new JsonObject();
        matrix.add("actions", normalizeObject(defaultMatrix.getAsJsonObject("actions"), actions));
    }

    /** Replace shipped defaults from v5/v6; retain administrator-customized ladders. */
    private static void migrateLegacyPunishmentDefaults(JsonObject current, JsonObject defaults) {
        if (!current.has("config_version") || !current.get("config_version").isJsonPrimitive()
                || !current.getAsJsonPrimitive("config_version").isNumber()
                || current.get("config_version").getAsInt() >= 7) return;
        if (!current.has("punishment_matrix") || !current.get("punishment_matrix").isJsonObject()) return;
        JsonObject matrix = current.getAsJsonObject("punishment_matrix");
        if (!matrix.has("escalation_ladder") || !matrix.get("escalation_ladder").isJsonArray()) return;
        var ladder = matrix.getAsJsonArray("escalation_ladder");
        if (ladder.size() == 1 && ladder.get(0).isJsonObject()) {
            JsonObject rule = ladder.get(0).getAsJsonObject();
            if (rule.has("action") && rule.get("action").isJsonPrimitive()
                    && rule.getAsJsonPrimitive("action").isString()
                    && "WARN".equalsIgnoreCase(rule.get("action").getAsString())
                    && rule.has("violations") && rule.get("violations").isJsonPrimitive()
                    && rule.getAsJsonPrimitive("violations").isNumber()
                    && rule.get("violations").getAsInt() == 5
                    && rule.has("message") && rule.get("message").isJsonPrimitive()
                    && "&8[&6✦ &4DEMON KING&8] &e%player% &7received a warning for &c%check%&7."
                    .equals(rule.get("message").getAsString())
                    && rule.has("broadcast") && rule.get("broadcast").isJsonPrimitive()
                    && !rule.get("broadcast").getAsBoolean()) {
                matrix.add("escalation_ladder", defaults.getAsJsonObject("punishment_matrix")
                        .getAsJsonArray("escalation_ladder").deepCopy());
                net.demonking.anticheat.DemonKingAntiCheat.LOGGER.info(
                        "Added configurable kick/ban ladder entries; kick and ban remain disabled by default.");
            }
            return;
        }
        if (ladder.size() != 3) return;
        String[] actions = {"WARN", "KICK", "BAN"};
        int[] thresholds = {5, 15, 25};
        String[] messages = {
                "&8[&6✦ &4DEMON KING&8] &e%player% &7received a warning for &c%check%&7.",
                "&8[&6✦ &4DEMON KING&8] &e%player% &7was removed for &c%check%&7.",
                "&8[&6✦ &4DEMON KING&8] &e%player% &7received a 7-day ban for &c%check%&7."
        };
        for (int i = 0; i < ladder.size(); i++) {
            if (!ladder.get(i).isJsonObject()) return;
            JsonObject rule = ladder.get(i).getAsJsonObject();
            if (!rule.has("action") || !rule.has("violations")
                    || !rule.get("action").isJsonPrimitive()
                    || !rule.getAsJsonPrimitive("action").isString()
                    || !rule.get("violations").isJsonPrimitive()
                    || !rule.getAsJsonPrimitive("violations").isNumber()
                    || !actions[i].equalsIgnoreCase(rule.get("action").getAsString())
                    || rule.get("violations").getAsInt() != thresholds[i]) return;
            if (!rule.has("message") || !rule.get("message").isJsonPrimitive()
                    || !rule.getAsJsonPrimitive("message").isString()
                    || !messages[i].equals(rule.get("message").getAsString())
                    || !rule.has("broadcast") || !rule.get("broadcast").isJsonPrimitive()
                    || !rule.getAsJsonPrimitive("broadcast").isBoolean()
                    || !rule.get("broadcast").getAsBoolean()) return;
            if (i == 2 && (!rule.has("duration_days") || !rule.get("duration_days").isJsonPrimitive()
                    || rule.get("duration_days").getAsInt() != 7)) return;
        }
        matrix.add("escalation_ladder", defaults.getAsJsonObject("punishment_matrix")
                .getAsJsonArray("escalation_ladder").deepCopy());
        net.demonking.anticheat.DemonKingAntiCheat.LOGGER.warn(
                "Migrated the shipped punishment ladder. Kick and ban are available but disabled by default; " +
                        "enable them only after reviewing check alerts.");
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
            options.remove("base_horizontal_per_tick");
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
