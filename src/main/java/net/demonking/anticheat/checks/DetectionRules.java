package net.demonking.anticheat.checks;

/** Pure threshold comparisons used by active checks. */
public final class DetectionRules {
    private DetectionRules() {}

    public static boolean excessiveHorizontalMove(double distance, double allowedDistance) {
        return distance > allowedDistance;
    }

    public static boolean excessiveReach(double distance, double maximumReach) {
        return distance > maximumReach;
    }

}
