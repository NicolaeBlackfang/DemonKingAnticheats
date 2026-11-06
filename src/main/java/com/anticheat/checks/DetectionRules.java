package com.anticheat.checks;

/** Pure decision rules shared by the Minecraft-version adapters. */
public final class DetectionRules {
    private DetectionRules() {}

    public static boolean unsupportedHover(int consecutiveAirMoves, double verticalVelocity) {
        return consecutiveAirMoves > 45 && Math.abs(verticalVelocity) < 0.005;
    }

    public static boolean excessiveHorizontalMove(double distance, double allowedDistance) {
        return distance > allowedDistance;
    }

    public static boolean excessiveReach(double distance, double maximumReach) {
        return distance > maximumReach;
    }

    public static boolean sustainedRapidAttacks(int rapidAttackStreak) {
        return rapidAttackStreak >= 12;
    }

    public static boolean implausibleBreakInterval(double hardness, long elapsedNanos) {
        return hardness >= 0.5 && elapsedNanos < 120_000_000L;
    }
}
