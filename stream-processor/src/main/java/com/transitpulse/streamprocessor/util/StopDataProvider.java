package com.transitpulse.streamprocessor.util;

import java.util.List;
import java.util.Map;

/**
 * Hardcoded Nagpur stop data matching the V2 seed migration.
 * In production this would be loaded from the database or a shared config topic.
 */
public final class StopDataProvider {

    private StopDataProvider() {}

    public record StopInfo(Long stopId, String stopName, double latitude, double longitude, int sequence) {}

    // Route 10: Sitabuldi - Automotive Square
    private static final List<StopInfo> ROUTE_10_STOPS = List.of(
            new StopInfo(1L,  "Sitabuldi Bus Stand",   21.1458, 79.0882, 1),
            new StopInfo(2L,  "Variety Square",        21.1396, 79.0787, 2),
            new StopInfo(3L,  "Law College Square",    21.1370, 79.0675, 3),
            new StopInfo(4L,  "Shankar Nagar Square",  21.1345, 79.0572, 4),
            new StopInfo(5L,  "Pratap Nagar Square",   21.1289, 79.0480, 5),
            new StopInfo(6L,  "Trimurti Nagar",        21.1225, 79.0385, 6),
            new StopInfo(7L,  "Manewada Square",       21.1170, 79.0290, 7),
            new StopInfo(8L,  "Automotive Square",     21.1102, 79.0195, 8)
    );

    // Route 47B: Dharampeth - Hingna T-Point
    private static final List<StopInfo> ROUTE_47B_STOPS = List.of(
            new StopInfo(9L,  "Dharampeth Bus Stop",    21.1510, 79.0780, 1),
            new StopInfo(10L, "Telephone Exchange Sq.", 21.1475, 79.0710, 2),
            new StopInfo(11L, "Laxmi Nagar Square",     21.1430, 79.0630, 3),
            new StopInfo(12L, "Panchsheel Square",      21.1380, 79.0545, 4),
            new StopInfo(13L, "Nandanvan Colony",       21.1320, 79.0450, 5),
            new StopInfo(14L, "Wadi Bus Stop",          21.1250, 79.0350, 6),
            new StopInfo(15L, "Hingna T-Point",         21.1180, 79.0255, 7)
    );

    private static final Map<Long, List<StopInfo>> ROUTE_STOPS = Map.of(
            1L, ROUTE_10_STOPS,
            2L, ROUTE_47B_STOPS
    );

    public static List<StopInfo> getStopsForRoute(Long routeId) {
        return ROUTE_STOPS.getOrDefault(routeId, List.of());
    }

    public static StopInfo getStop(Long routeId, int stopIndex) {
        List<StopInfo> stops = getStopsForRoute(routeId);
        if (stopIndex >= 0 && stopIndex < stops.size()) {
            return stops.get(stopIndex);
        }
        return null;
    }

    /**
     * Returns all stops from currentStopIndex onward for a given route.
     */
    public static List<StopInfo> getUpcomingStops(Long routeId, int currentStopIndex) {
        List<StopInfo> stops = getStopsForRoute(routeId);
        if (currentStopIndex >= 0 && currentStopIndex < stops.size()) {
            return stops.subList(currentStopIndex, stops.size());
        }
        return List.of();
    }
}
