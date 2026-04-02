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

    public static String fromOccupancyEmoji(Integer occupancyPercent) {
        if (occupancyPercent == null) return "UNKNOWN";
        if (occupancyPercent <= 30) return "Comfortable \uD83D\uDFE2";
        if (occupancyPercent <= 60) return "Moderate \uD83D\uDFE1";
        if (occupancyPercent <= 80) return "Crowded \uD83D\uDFE0";
        return "Very Crowded \uD83D\uDD34";
    }
}
