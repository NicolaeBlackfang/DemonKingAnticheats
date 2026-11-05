package com.anticheat.config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("demonkinganticheats.json");
    private static ModConfig config = new ModConfig();
    private static boolean firstRun;
    private ConfigManager() {}
    public static synchronized void load() {
        try {
            Files.createDirectories(FILE.getParent());
            firstRun = !Files.exists(FILE);
            if (Files.exists(FILE)) {
                ModConfig loaded = GSON.fromJson(Files.readString(FILE), ModConfig.class);
                if (loaded != null) config = loaded;
                if (config.general == null) config.general = new ModConfig.General();
                if (config.checks == null) config.checks = new ModConfig().checks;
                new ModConfig().checks.forEach(config.checks::putIfAbsent);
            }
            save();
        } catch (Exception e) { throw new IllegalStateException("Could not load " + FILE, e); }
    }
    public static synchronized void save() {
        try { Files.createDirectories(FILE.getParent()); Files.writeString(FILE, GSON.toJson(config)); }
        catch (Exception e) { throw new IllegalStateException("Could not save " + FILE, e); }
    }
    public static ModConfig get() { return config; }
    public static boolean isFirstRun() { return firstRun; }
}
