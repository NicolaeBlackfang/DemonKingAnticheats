package com.anticheat.config;
import java.util.LinkedHashMap;
import java.util.Map;
public final class ModConfig {
    public General general = new General();
    public Map<String, CheckOptions> checks = defaults();
    public static final class General {
        public boolean enabled = true, debugMode = false;
        public String defaultPunishment = "KICK";
        public int maxViolations = 20;
        public double decayRatePerMinute = 2.0;
    }
    public static final class CheckOptions {
        public boolean enabled;
        public String punishment = "WARN";
        public double threshold = 1.0, maxReach = 3.05;
        public int strictness = 5;
    }
    private static Map<String, CheckOptions> defaults() {
        Map<String, CheckOptions> m = new LinkedHashMap<>();
        String[] ids = {"aimAssist","aimbot","airJump","antiAfk","antiBlind","antiCactus","antiFire","antiKnockback","autoArmor","autoClicker","autoEat","autoFish","autoLog","autoMine","autoSoup","autoTotem","baritone","blink","boatFly","bowAimbot","breadcrumbs","bunnyHop","chams","criticals","derp","dolphin","dupeExploits","esp","fastBow","fastBreak","fastPlace","flight","freecam","fullbright","highJump","inventoryWalk","jesus","killAura","longJump","noClip","noFall","noSlowDown","nuker","phase","reach","regen","safeWalk","scaffold","sneak","speed","spider","spinBot","step","timer","tracers","triggerbot","xRay"};
        for (String id : ids) m.put(id, new CheckOptions());
        for (String id : new String[]{"fastBreak","flight","speed","reach","killAura"}) m.get(id).enabled = true;
        m.get("killAura").punishment="KICK"; m.get("flight").punishment="KICK"; m.get("speed").punishment="KICK"; m.get("fastBreak").threshold=0.65;
        return m;
    }
}
