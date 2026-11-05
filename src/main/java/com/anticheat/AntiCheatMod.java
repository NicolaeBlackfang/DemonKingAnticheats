package com.anticheat;
import com.anticheat.checks.CheckManager;
import com.anticheat.config.ConfigManager;
import com.anticheat.handler.PunishmentHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;   
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public final class AntiCheatMod implements ModInitializer {
    public static final String MOD_ID="demonkinganticheats";
    public static final Logger LOGGER=LoggerFactory.getLogger(MOD_ID);
    public static CheckManager CHECKS;
    @Override public void onInitialize() {
        ConfigManager.load(); CHECKS=new CheckManager(new PunishmentHandler());
        if (ConfigManager.isFirstRun()) {
            System.out.println("\u001B[38;5;196m╔══════════════════════════════════════════════════════╗\u001B[0m");
            System.out.println("\u001B[38;5;208m║  👑  DEMON KING ANTI-CHEATS  •  SERVER GUARD ACTIVE  👑 ║\u001B[0m");
            System.out.println("\u001B[38;5;196m║       Packet checks initialized successfully        ║\u001B[0m");
            System.out.println("\u001B[38;5;196m╚══════════════════════════════════════════════════════╝\u001B[0m");
        }
        AttackEntityCallback.EVENT.register((player,world,hand,target,hit)->{
            if(!world.isClient && player instanceof ServerPlayerEntity attacker && target instanceof ServerPlayerEntity victim) CHECKS.onAttack(attacker,victim);
            return ActionResult.PASS;
        });
        PlayerBlockBreakEvents.BEFORE.register((world,player,pos,state,blockEntity)->player instanceof ServerPlayerEntity sp ? CHECKS.onBlockBreak(sp,pos,state) : true);
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->CHECKS.onDisconnect(handler.getPlayer().getUuid()));
        ServerLifecycleEvents.SERVER_STOPPING.register(server->ConfigManager.save());
        LOGGER.info("Demon King Anti-Cheats initialized");
    }
}
