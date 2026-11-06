package net.demonking.anticheat.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.LinkedHashMap;
import java.util.Map;

/** Versioned, per-check JSON schema. Each check stores only settings meaningful to that module. */
public final class ConfigSchema {
    @SerializedName("config_version") public int configVersion = 3;
    @SerializedName("global_settings") public GlobalSettings globalSettings = new GlobalSettings();
    @SerializedName("punishment_matrix") public PunishmentMatrix punishmentMatrix = new PunishmentMatrix();
    public Map<String, JsonObject> checks = new LinkedHashMap<>();

    public static final class GlobalSettings {
        public boolean enabled = true;
        @SerializedName("debug_mode") public boolean debugMode = false;
        @SerializedName("decay_per_minute") public double decayPerMinute = 0.25;
        @SerializedName("discord_webhook") public DiscordWebhook discordWebhook = new DiscordWebhook();
    }

    public static final class DiscordWebhook {
        public boolean enabled = false;
        public String url = "";
        @SerializedName("embed_color") public int embedColor = 16711680;
        public String username = "Demon King Anti-Cheats";
        @SerializedName("timeout_seconds") public int timeoutSeconds = 5;
    }

    public static final class PunishmentMatrix {
        @SerializedName("escalation_ladder") public JsonArray escalationLadder = defaultLadder();
    }

    public boolean isEnabled(String id) { return bool(id, "enabled", true); }
    public int ruleIndexFor(double violations) {
        int chosen = -1;
        for (int i = 0; i < punishmentMatrix.escalationLadder.size(); i++) {
            JsonObject rule = punishmentMatrix.escalationLadder.get(i).getAsJsonObject();
            if (violations >= rule.get("violations").getAsDouble()) chosen = i;
        }
        return chosen;
    }
    public JsonObject ruleAt(int index) { return punishmentMatrix.escalationLadder.get(index).getAsJsonObject(); }
    public boolean bool(String id, String field, boolean fallback) {
        JsonObject object = checks.get(key(id));
        return object != null && object.has(field) ? object.get(field).getAsBoolean() : fallback;
    }
    public int integer(String id, String field, int fallback) {
        JsonObject object = checks.get(key(id));
        return object != null && object.has(field) ? object.get(field).getAsInt() : fallback;
    }
    public double decimal(String id, String field, double fallback) {
        JsonObject object = checks.get(key(id));
        return object != null && object.has(field) ? object.get(field).getAsDouble() : fallback;
    }
    public String string(String id, String field, String fallback) {
        JsonObject object = checks.get(key(id));
        return object != null && object.has(field) ? object.get(field).getAsString() : fallback;
    }
    private static String key(String id) { return id.toLowerCase(java.util.Locale.ROOT); }

    public static ConfigSchema defaults() {
        ConfigSchema schema = new ConfigSchema();
        String[] ids = {"fastbreak","fastplace","flight","inventorywalk","jesus","killaura","reach","scaffold","speed"};
        for (String id : ids) {
            JsonObject options = new JsonObject();
            options.addProperty("enabled", true);
            options.addProperty("strictness", 5);
            schema.checks.put(id, options);
        }
        tailored(schema);
        return schema;
    }

    private static void tailored(ConfigSchema schema) {
        JsonObject aim = schema.checks.get("killaura");
        aim.addProperty("strictness", 8); aim.addProperty("max_angle_delta", 60.0);
        aim.addProperty("check_reach", true); aim.addProperty("max_reach", 3.2); aim.addProperty("auto_cancel", true);
        aim.addProperty("target_players", true); aim.addProperty("target_mobs", true); aim.addProperty("target_animals", true);
        add(schema, "jesus", "strictness", 6, "vertical_tolerance", 0.05, "require_movement_packet", true, "auto_cancel", true, "buffer_limit", 8);
        add(schema, "fastbreak", "strictness", 7, "buffer_limit", 10, "minimum_completion_ratio", 0.65, "auto_cancel", true);
        add(schema, "fastplace", "strictness", 5, "min_delay_ms", 50, "auto_cancel", true);
        add(schema, "scaffold", "strictness", 9, "check_air_place", true, "max_extension_reach", 4.5, "auto_cancel", true);
        add(schema, "inventorywalk", "strictness", 5, "max_reach", 3.05, "allow_rotation", false, "auto_cancel", true);
        add(schema, "reach", "max_reach", 3.2, "auto_cancel", true);
        add(schema, "speed", "base_horizontal_per_tick", 0.85, "buffer_limit", 8, "auto_cancel", false);
        add(schema, "flight", "hover_ticks", 45, "vertical_tolerance", 0.005, "buffer_limit", 8, "auto_cancel", false);
    }

    private static void add(ConfigSchema schema, String id, Object... pairs) {
        JsonObject object = schema.checks.get(id);
        for (int i = 0; i < pairs.length; i += 2) {
            Object value = pairs[i + 1];
            if (value instanceof Boolean b) object.addProperty((String) pairs[i], b);
            else if (value instanceof Number n) object.addProperty((String) pairs[i], n);
            else object.addProperty((String) pairs[i], String.valueOf(value));
        }
    }

    private static JsonArray defaultLadder() {
        JsonArray rules = new JsonArray();
        rules.add(rule(5, "WARN", 0, true, "&c[AntiCheat] &e%player% &7was warned for &f%check%&7."));
        rules.add(rule(15, "KICK", 0, true, "&c[AntiCheat] &f%player% &ckicked for unfair advantage (%check%)."));
        rules.add(rule(25, "BAN", 7, true, "&c[AntiCheat] &4%player% &cbanned for 7 days by DemonKing AntiCheat."));
        return rules;
    }
    private static JsonObject rule(int violations, String action, int days, boolean broadcast, String message) {
        JsonObject rule = new JsonObject();
        rule.addProperty("violations", violations); rule.addProperty("action", action);
        if (days > 0) rule.addProperty("duration_days", days);
        rule.addProperty("broadcast", broadcast); rule.addProperty("message", message);
        return rule;
    }
}
