package net.demonking.anticheat.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Persistent, decaying per-player violation levels so escalation survives kicks and restarts. */
public final class ViolationTracker {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("demonkinganticheats-violations.json");
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "demonkinganticheats-data-writer");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<UUID, Map<String, Double>> violations = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> updatedEpochMillis = new ConcurrentHashMap<>();

    public ViolationTracker() { load(); }

    public double get(UUID player, String check, double decayPerMinute) {
        Map<String, Double> levels = violations.get(player);
        if (levels == null) return 0;
        long now = System.currentTimeMillis();
        long previous = updatedEpochMillis.getOrDefault(player, Map.of()).getOrDefault(check, now);
        double elapsedMinutes = Math.max(0, (System.currentTimeMillis() - previous) / 60_000.0);
        return Math.max(0, levels.getOrDefault(check, 0.0) - decayPerMinute * elapsedMinutes);
    }

    public double add(UUID player, String check, double amount, double decayPerMinute) {
        long now = System.currentTimeMillis();
        Map<String, Double> levels = violations.computeIfAbsent(player, ignored -> new ConcurrentHashMap<>());
        Map<String, Long> timestamps = updatedEpochMillis.computeIfAbsent(player, ignored -> new ConcurrentHashMap<>());
        long previous = timestamps.getOrDefault(check, now);
        double elapsedMinutes = Math.max(0, (now - previous) / 60_000.0);
        timestamps.put(check, now);
        double level = levels.compute(check, (ignored, old) -> Math.max(0, old == null ? 0 : old - decayPerMinute * elapsedMinutes) + amount);
        return level;
    }

    public void save() {
        Map<String, SavedPlayer> snapshot = snapshot();
        WRITER.execute(() -> write(snapshot));
    }

    public void saveNow() {
        try {
            Future<?> queuedWrites = WRITER.submit(() -> {});
            queuedWrites.get();
            write(snapshot());
        } catch (Exception e) {
            net.demonking.anticheat.DemonKingAntiCheat.LOGGER.error("Could not flush persistent violation data", e);
        }
    }

    private Map<String, SavedPlayer> snapshot() {
        Map<String, SavedPlayer> snapshot = new HashMap<>();
        violations.forEach((uuid, levels) -> snapshot.put(uuid.toString(), new SavedPlayer(
                new HashMap<>(levels), new HashMap<>(updatedEpochMillis.getOrDefault(uuid, Map.of())))));
        return snapshot;
    }

    private void write(Map<String, SavedPlayer> snapshot) {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(snapshot));
        } catch (Exception e) {
            net.demonking.anticheat.DemonKingAntiCheat.LOGGER.error("Could not save persistent violation data", e);
        }
    }

    private void load() {
        if (!Files.exists(FILE)) return;
        try {
            Map<String, SavedPlayer> snapshot = GSON.fromJson(Files.readString(FILE),
                    new com.google.gson.reflect.TypeToken<Map<String, SavedPlayer>>() {}.getType());
            if (snapshot == null) return;
            snapshot.forEach((key, saved) -> {
                try {
                    UUID uuid = UUID.fromString(key);
                    if (saved != null && saved.levels != null) {
                        Map<String, Double> safeLevels = new ConcurrentHashMap<>();
                        Map<String, Long> timestamps = new ConcurrentHashMap<>();
                        saved.levels.forEach((check, level) -> {
                            if (check == null || level == null || !Double.isFinite(level) || level <= 0) return;
                            // Old speed scores came from tick-aggregated displacement and are not trustworthy.
                            if ("speed".equalsIgnoreCase(check)) return;
                            safeLevels.put(check, Math.min(level, 1000.0));
                            long time = saved.updatedAtByCheck != null
                                    ? saved.updatedAtByCheck.getOrDefault(check, saved.updatedAtMillis)
                                    : saved.updatedAtMillis;
                            timestamps.put(check, time > 0 ? time : System.currentTimeMillis());
                        });
                        if (!safeLevels.isEmpty()) violations.put(uuid, safeLevels);
                        if (!timestamps.isEmpty()) updatedEpochMillis.put(uuid, timestamps);
                    }
                } catch (IllegalArgumentException ignored) { }
            });
        } catch (Exception e) {
            net.demonking.anticheat.DemonKingAntiCheat.LOGGER.error("Could not load persistent violation data", e);
        }
    }

    private static final class SavedPlayer {
        Map<String, Double> levels;
        long updatedAtMillis;
        Map<String, Long> updatedAtByCheck;
        SavedPlayer(Map<String, Double> levels, Map<String, Long> timestamps) {
            this.levels = levels;
            this.updatedAtByCheck = timestamps;
            this.updatedAtMillis = System.currentTimeMillis();
        }
    }
}
