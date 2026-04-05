package com.transitpulse.gtfsrtadapter.service;

import com.google.transit.realtime.GtfsRealtime;
import com.transitpulse.common.dto.BusLocationEvent;
import com.transitpulse.common.util.CrowdLabelUtil;
import com.transitpulse.gtfsrtadapter.model.GtfsBusMapping;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class GtfsRtFetcherService {

    private static final String TOPIC = "bus-location-events";

    private final KafkaTemplate<String, BusLocationEvent> kafkaTemplate;

    @Value("${gtfs.rt.vehicle-positions-url}")
    private String vehiclePositionsUrl;

    @Value("${gtfs.rt.api-key:}")
    private String apiKey;

    @Value("${gtfs.rt.enabled:true}")
    private boolean enabled;

    @Value("${gtfs.rt.city-label:NYC}")
    private String cityLabel;

    // Internal bus ID offset — NYC buses start at ID 100+ to avoid conflict with simulator
    @Value("${gtfs.rt.bus-id-offset:100}")
    private long busIdOffset;

    // Route ID offset — NYC routes start at ID 100+
    @Value("${gtfs.rt.route-id-offset:100}")
    private long routeIdOffset;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Track previously seen vehicles for speed computation
    private final ConcurrentHashMap<String, PreviousPosition> previousPositions = new ConcurrentHashMap<>();

    // Map GTFS vehicle IDs to stable internal IDs
    private final ConcurrentHashMap<String, Long> vehicleIdMap = new ConcurrentHashMap<>();
    private long nextBusId;

    // Route ID mapping (GTFS route string → internal long ID)
    private final ConcurrentHashMap<String, Long> routeIdMap = new ConcurrentHashMap<>();
    private long nextRouteId;

    // List of allowed route patterns (empty = allow all)
    @Value("${gtfs.rt.route-filter:}")
    private String routeFilter;

    private Set<String> allowedRoutes = new HashSet<>();

    @PostConstruct
    public void init() {
        nextBusId = busIdOffset;
        nextRouteId = routeIdOffset;
        if (routeFilter != null && !routeFilter.isBlank()) {
            allowedRoutes.addAll(Arrays.asList(routeFilter.split(",")));
            log.info("GTFS-RT adapter filtering to routes: {}", allowedRoutes);
        }
        log.info("GTFS-RT adapter initialized — URL: {}, city: {}, enabled: {}",
                vehiclePositionsUrl, cityLabel, enabled);
    }

    @Scheduled(fixedRateString = "${gtfs.rt.poll-interval-ms:15000}")
    public void fetchAndPublish() {
        if (!enabled) return;

        try {
            GtfsRealtime.FeedMessage feed = fetchFeed();
            if (feed == null) return;

            int published = 0;
            for (GtfsRealtime.FeedEntity entity : feed.getEntityList()) {
                if (!entity.hasVehicle()) continue;

                GtfsRealtime.VehiclePosition vp = entity.getVehicle();
                if (!vp.hasPosition()) continue;

                String vehicleId = extractVehicleId(vp);
                String routeId = extractRouteId(vp);

                // Apply route filter
                if (!allowedRoutes.isEmpty() && !matchesRouteFilter(routeId)) continue;

                double lat = vp.getPosition().getLatitude();
                double lng = vp.getPosition().getLongitude();

                // Skip invalid coordinates
                if (lat == 0 && lng == 0) continue;

                // For NYC, filter to Manhattan/Brooklyn bounding box to limit volume
                if (!isInServiceArea(lat, lng)) continue;

                // Map to stable internal IDs
                long internalBusId = vehicleIdMap.computeIfAbsent(vehicleId, k -> nextBusId++);
                long internalRouteId = routeIdMap.computeIfAbsent(routeId, k -> nextRouteId++);

                // Compute speed from previous position
                double speedKmh = computeSpeed(vehicleId, lat, lng);

                // Simulate occupancy based on time of day (MTA feed doesn't include it)
                int occupancy = estimateOccupancy(vp);
                String crowdLabel = CrowdLabelUtil.fromOccupancyEmoji(occupancy);

                // Determine timestamp
                long epochSec = vp.getTimestamp() > 0 ? vp.getTimestamp()
                        : feed.getHeader().getTimestamp();
                LocalDateTime recordedAt = epochSec > 0
                        ? LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSec), ZoneId.systemDefault())
                        : LocalDateTime.now();

                String busNumber = cityLabel + "-" + shortenRoute(routeId) + "-" + vehicleId;

                BusLocationEvent event = BusLocationEvent.builder()
                        .busId(internalBusId)
                        .busNumber(busNumber)
                        .routeId(internalRouteId)
                        .latitude(lat)
                        .longitude(lng)
                        .occupancyPercent(occupancy)
                        .crowdLabel(crowdLabel)
                        .speed(Math.round(speedKmh * 100.0) / 100.0)
                        .currentStopIndex(0)
                        .recordedAt(recordedAt)
                        .build();

                kafkaTemplate.send(TOPIC, String.valueOf(internalBusId), event);
                published++;
            }

            if (published > 0) {
                log.info("GTFS-RT: Published {} vehicle positions from {} feed",
                        published, cityLabel);
            }

        } catch (Exception e) {
            log.warn("GTFS-RT fetch failed ({}): {} — will retry next cycle",
                    cityLabel, e.getMessage());
        }
    }

    private GtfsRealtime.FeedMessage fetchFeed() {
        try {
            String url = vehiclePositionsUrl;
            if (apiKey != null && !apiKey.isBlank()) {
                url += (url.contains("?") ? "&" : "?") + "key=" + apiKey;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                log.warn("GTFS-RT HTTP {}: {}", response.statusCode(), url);
                return null;
            }

            try (InputStream is = response.body()) {
                return GtfsRealtime.FeedMessage.parseFrom(is);
            }
        } catch (Exception e) {
            log.warn("GTFS-RT fetch error: {}", e.getMessage());
            return null;
        }
    }

    private String extractVehicleId(GtfsRealtime.VehiclePosition vp) {
        if (vp.hasVehicle() && vp.getVehicle().getId() != null
                && !vp.getVehicle().getId().isBlank()) {
            return vp.getVehicle().getId();
        }
        // Fallback to trip ID
        if (vp.hasTrip() && vp.getTrip().getTripId() != null) {
            return vp.getTrip().getTripId();
        }
        return "unknown-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String extractRouteId(GtfsRealtime.VehiclePosition vp) {
        if (vp.hasTrip() && vp.getTrip().getRouteId() != null
                && !vp.getTrip().getRouteId().isBlank()) {
            return vp.getTrip().getRouteId();
        }
        return "UNKNOWN";
    }

    private boolean matchesRouteFilter(String routeId) {
        for (String pattern : allowedRoutes) {
            if (routeId.contains(pattern.trim())) return true;
        }
        return false;
    }

    private boolean isInServiceArea(double lat, double lng) {
        // NYC metro area bounding box (Manhattan, Brooklyn, Queens)
        return lat >= 40.49 && lat <= 40.92 && lng >= -74.26 && lng <= -73.70;
    }

    private double computeSpeed(String vehicleId, double lat, double lng) {
        PreviousPosition prev = previousPositions.get(vehicleId);
        long now = System.currentTimeMillis();

        previousPositions.put(vehicleId, new PreviousPosition(lat, lng, now));

        if (prev == null) return 0;

        double timeSec = (now - prev.timestamp) / 1000.0;
        if (timeSec < 1) return 0;

        double distKm = haversineKm(prev.lat, prev.lng, lat, lng);
        double speedKmh = distKm / (timeSec / 3600.0);

        // Cap at reasonable bus speed
        return Math.min(speedKmh, 80.0);
    }

    private int estimateOccupancy(GtfsRealtime.VehiclePosition vp) {
        // Use GTFS occupancy status if available
        if (vp.hasOccupancyStatus()) {
            return switch (vp.getOccupancyStatus()) {
                case EMPTY -> 5;
                case MANY_SEATS_AVAILABLE -> 25;
                case FEW_SEATS_AVAILABLE -> 55;
                case STANDING_ROOM_ONLY -> 75;
                case CRUSHED_STANDING_ROOM_ONLY -> 95;
                case FULL -> 100;
                default -> 40;
            };
        }
        // Estimate based on time of day
        int hour = LocalDateTime.now().getHour();
        if ((hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 19)) {
            return 60 + new Random().nextInt(30); // Rush hour: 60-90
        }
        return 20 + new Random().nextInt(30); // Off-peak: 20-50
    }

    private String shortenRoute(String routeId) {
        // "MTA NYCT_M15" → "M15"
        if (routeId.contains("_")) {
            return routeId.substring(routeId.lastIndexOf('_') + 1);
        }
        return routeId.length() > 10 ? routeId.substring(0, 10) : routeId;
    }

    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private record PreviousPosition(double lat, double lng, long timestamp) {}
}
