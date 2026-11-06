package com.anticheat.checks;
import com.anticheat.config.ConfigManager;
import com.anticheat.data.ViolationTracker;
import com.anticheat.handler.*;
import com.anticheat.checks.combat.*;
import com.anticheat.checks.movement.*;
import com.anticheat.checks.world.FastBreakCheck;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import java.util.*;
public final class CheckManager {
    private static final List<String> IMPLEMENTED_CHECKS = List.of("flight", "speed", "reach", "killAura", "fastBreak");
    private final ViolationTracker tracker=new ViolationTracker();
    private final NotificationHandler notifications=new NotificationHandler();
    private final PunishmentHandler punishments;
    private final Map<UUID,FlightCheck> flight=new HashMap<>();
    private final Map<UUID,SpeedCheck> speed=new HashMap<>();
    private final Map<UUID,FastBreakCheck> breaks=new HashMap<>();
    private final ReachCheck reach=new ReachCheck();
    private final KillAuraCheck aura=new KillAuraCheck();
    public CheckManager(PunishmentHandler punishments) { this.punishments=punishments; }
    public int activeCheckCount() {
        return (int) IMPLEMENTED_CHECKS.stream().filter(id -> {
            var options = option(id);
            return options != null && options.enabled;
        }).count();
    }
    public void onMovement(ServerPlayer p) {
        if (!active()) return;
        flight.computeIfAbsent(p.getUUID(),k->new FlightCheck()).check(p).ifPresent(d->flag(p,"flight",d));
        speed.computeIfAbsent(p.getUUID(),k->new SpeedCheck()).check(p).ifPresent(d->flag(p,"speed",d));
    }
    public void onAttack(ServerPlayer a, ServerPlayer v) {
        if (!active()) return;
        var r=option("reach"); if(r!=null && r.enabled) reach.check(a,v,r.maxReach).ifPresent(d->flag(a,"reach",d));
        aura.check(a,v).ifPresent(d->flag(a,"killAura",d));
    }
    public boolean onBlockBreak(ServerPlayer p, BlockPos pos, BlockState state) {
        if(!active()) return true;
        return breaks.computeIfAbsent(p.getUUID(),k->new FastBreakCheck()).check(p,pos,state).map(d->{flag(p,"fastBreak",d); return false;}).orElse(true);
    }
    public void onDisconnect(UUID player) {
        tracker.clear(player); flight.remove(player); speed.remove(player); breaks.remove(player); aura.clear(player);
    }
    private boolean active(){return ConfigManager.get().general.enabled;}
    private com.anticheat.config.ModConfig.CheckOptions option(String id){return ConfigManager.get().checks.get(id);}
    private void flag(ServerPlayer p,String id,String detail){
        var cfg=option(id); if(cfg==null || !cfg.enabled) return;
        double vl=tracker.add(p.getUUID(),id,1,ConfigManager.get().general.decayRatePerMinute);
        notifications.alert(p.level().getServer(),p,id,vl,detail);
        if(vl>=Math.max(1,cfg.threshold)) punishments.apply(p,id,cfg.punishment,vl);
    }
}
