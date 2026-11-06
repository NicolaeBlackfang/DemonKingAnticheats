package net.demonking.anticheat.punishment;

import java.util.Locale;

/** Stable, operator-friendly names shared by alerts, messages and webhooks. */
public final class CheckNames {
    private CheckNames() {}

    public static String display(String id) {
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "killaura" -> "Kill Aura";
            case "crystalaura" -> "Crystal Aura";
            case "fastbreak" -> "Fast Break";
            case "fastplace" -> "Fast Place";
            case "jesus" -> "Water Walking";
            case "nofall" -> "No Fall";
            case "noslow" -> "No Slow";
            case "inventorywalk" -> "Inventory Movement";
            case "scaffold" -> "Air Placement / Scaffold";
            case "reach" -> "Reach";
            case "flight" -> "Flight";
            case "speed" -> "Speed";
            default -> id.replace('_', ' ');
        };
    }
}
