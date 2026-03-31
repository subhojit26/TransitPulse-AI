package com.transitpulse.common.util;

public final class CrowdLabelUtil {

    private CrowdLabelUtil() {}

    public static String fromOccupancy(Integer occupancyPercent) {
        if (occupancyPercent == null) return "UNKNOWN";
        if (occupancyPercent <= 25) return "EMPTY";
        if (occupancyPercent <= 50) return "LOW";
        if (occupancyPercent <= 75) return "MODERATE";
        if (occupancyPercent <= 90) return "HIGH";
        return "FULL";
    }
}
