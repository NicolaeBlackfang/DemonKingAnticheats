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
    private final Map<UUID, Long> updatedEpochMillis = new ConcurrentHashMap<>();

    public ViolationTracker() { load(); }

    public double get(UUID player, String check, double decayPerMinute) {
        Map<String, Double> levels = violations.get(player);
        if (levels == null) return 0;
        long previous = updatedEpochMillis.getOrDefault(player, System.currentTimeMillis());
        double elapsedMinutes = Math.max(0, (System.currentTimeMillis() - previous) / 60_000.0);
        return Math.max(0, levels.getOrDefault(check, 0.0) - decayPerMinute * elapsedMinutes);
    }

    public double add(UUID player, String check, double amount, double decayPerMinute) {
        long now = System.currentTimeMillis();
        Map<String, Double> levels = violations.computeIfAbsent(player, ignored -> new ConcurrentHashMap<>());
        long previous = updatedEpochMillis.getOrDefault(player, now);
        double elapsedMinutes = Math.max(0, (now - previous) / 60_000.0);
        updatedEpochMillis.put(player, now);
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
        violations.forEach((uuid, levels) -> snapshot.put(uuid.toString(),
                new SavedPlayer(new HashMap<>(levels), updatedEpochMillis.getOrDefault(uuid, System.currentTimeMillis()))));
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
                        violations.put(uuid, new ConcurrentHashMap<>(saved.levels));
                        updatedEpochMillis.put(uuid, saved.updatedAtMillis);
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
        SavedPlayer(Map<String, Double> levels, long updatedAtMillis) {
            this.levels = levels;
            this.updatedAtMillis = updatedAtMillis;
        }
    }
}
