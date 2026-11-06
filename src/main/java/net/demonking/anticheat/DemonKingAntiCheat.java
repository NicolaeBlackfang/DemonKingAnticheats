package net.demonking.anticheat;

import net.demonking.anticheat.checks.CheckManager;
import net.demonking.anticheat.config.ConfigManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DemonKingAntiCheat implements ModInitializer {
    public static final String MOD_ID = "demonkinganticheats";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static CheckManager CHECKS;

    @Override
    public void onInitialize() {
        ConfigManager.load();
        CHECKS = new CheckManager();
        ServerTickEvents.END_SERVER_TICK.register(server ->
                server.getPlayerList().getPlayers().forEach(CHECKS::onMovement));
        System.out.println("\u001B[38;5;196m╔══════════════════════════════════════════════════════════╗\u001B[0m");
        System.out.println("\u001B[38;5;208m║          👑 DEMON KING ANTI-CHEATS • ONLINE 👑           ║\u001B[0m");
        System.out.printf("\u001B[38;5;196m ║       %d active server-side checks                       ║\u001B[0m%n",
                CHECKS.activeCheckCount());
        System.out.println("\u001B[38;5;196m╚══════════════════════════════════════════════════════════╝\u001B[0m");
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state,
                blockEntity) -> player instanceof ServerPlayer sp ? CHECKS.onBlockBreak(sp, pos, state) : true);
        ServerPlayConnectionEvents.DISCONNECT
                .register((handler, server) -> CHECKS.onDisconnect(handler.getPlayer().getUUID()));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ConfigManager.save();
            CHECKS.savePersistentStateOnShutdown();
        });
        LOGGER.info("Demon King Anti-Cheats initialized");
    }
}
