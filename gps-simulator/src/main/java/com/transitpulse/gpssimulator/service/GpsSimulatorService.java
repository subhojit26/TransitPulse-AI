package com.transitpulse.gpssimulator.service;

import com.transitpulse.common.dto.BusLocationEvent;
import com.transitpulse.common.util.CrowdLabelUtil;
import com.transitpulse.gpssimulator.model.SimulatedBus;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class GpsSimulatorService {

    private static final String TOPIC = "bus-location-events";
    private static final double STOP_PROXIMITY_THRESHOLD = 0.0003; // ~30 meters

    private final KafkaTemplate<String, BusLocationEvent> kafkaTemplate;
    private final CopyOnWriteArrayList<SimulatedBus> activeBuses = new CopyOnWriteArrayList<>();

    @Value("${simulator.interpolation-factor:0.10}")
    private double interpolationFactor;

    @Value("${simulator.breakdown-probability:0.0005}")
    private double breakdownProbability;

    @Value("${simulator.rush-hour-speed-factor:0.5}")
    private double rushHourSpeedFactor;

    // ===== PUNE ROUTES (real road coordinates) =====

    // Route P1: Swargate → Shivajinagar (via JM Road) — 8 stops
    private static final List<double[]> PUNE_ROUTE_1 = List.of(
            new double[]{18.5018, 73.8636},  // Swargate Bus Stand
            new double[]{18.5065, 73.8610},  // Parvati Paytha
            new double[]{18.5120, 73.8571},  // Sarasbaug
            new double[]{18.5162, 73.8530},  // Shaniwar Wada
            new double[]{18.5200, 73.8490},  // PMC Building (Deccan Gymkhana)
            new double[]{18.5248, 73.8445},  // Garware Bridge
            new double[]{18.5290, 73.8395},  // Agriculture College
            new double[]{18.5308, 73.8363}   // Shivajinagar Bus Stand
    );

    // Route P2: Katraj → Nigdi (via NH48) — 8 stops
    private static final List<double[]> PUNE_ROUTE_2 = List.of(
            new double[]{18.4578, 73.8660},  // Katraj Bus Stop
            new double[]{18.4730, 73.8665},  // Bharati Vidyapeeth
            new double[]{18.4885, 73.8650},  // Balaji Nagar
            new double[]{18.5018, 73.8636},  // Swargate
            new double[]{18.5355, 73.8410},  // Khadki
            new double[]{18.5625, 73.8140},  // Dapodi
            new double[]{18.5930, 73.7950},  // Pimpri Chinchwad
            new double[]{18.6510, 73.7672}   // Nigdi Bus Stop
    );

    // ===== MUMBAI ROUTES (real road coordinates) =====

    // Route M1: CST → Andheri (via Western Express Highway) — 8 stops
    private static final List<double[]> MUMBAI_ROUTE_1 = List.of(
            new double[]{18.9398, 72.8355},  // CST (Chhatrapati Shivaji Terminus)
            new double[]{18.9440, 72.8240},  // Marine Lines
            new double[]{18.9540, 72.8160},  // Mumbai Central
            new double[]{18.9710, 72.8200},  // Dadar TT
            new double[]{18.9932, 72.8226},  // Mahim Junction
            new double[]{19.0176, 72.8420},  // Bandra Station
            new double[]{19.0535, 72.8409},  // Vile Parle
            new double[]{19.1190, 72.8465}   // Andheri Station
    );

    // Route M2: Dadar → BKC → Powai — 8 stops
    private static final List<double[]> MUMBAI_ROUTE_2 = List.of(
            new double[]{18.9710, 72.8200},  // Dadar TT
            new double[]{18.9760, 72.8340},  // Sion
            new double[]{18.9832, 72.8420},  // Chunabhatti
            new double[]{19.0022, 72.8527},  // Kurla Station
            new double[]{19.0544, 72.8688},  // Ghatkopar
            new double[]{19.0640, 72.8770},  // Vikhroli
            new double[]{19.0750, 72.8880},  // Kanjurmarg
            new double[]{19.0760, 72.9050}   // Powai (Hiranandani)
    );

    @PostConstruct
    public void initBuses() {
        // --- Pune buses ---
        // 2 buses on Pune Route P1 (Swargate → Shivajinagar)
        activeBuses.add(new SimulatedBus(1L, "PNQ-P1-01", 1L, PUNE_ROUTE_1));
        activeBuses.add(new SimulatedBus(2L, "PNQ-P1-02", 1L, PUNE_ROUTE_1));
        // Start bus 2 from midway
        activeBuses.get(1).setCurrentLat(18.5162);
        activeBuses.get(1).setCurrentLng(73.8530);
        activeBuses.get(1).setCurrentStopIndex(4);

        // 2 buses on Pune Route P2 (Katraj → Nigdi)
        activeBuses.add(new SimulatedBus(3L, "PNQ-P2-01", 2L, PUNE_ROUTE_2));
        activeBuses.add(new SimulatedBus(4L, "PNQ-P2-02", 2L, PUNE_ROUTE_2));
        activeBuses.get(3).setCurrentLat(18.5355);
        activeBuses.get(3).setCurrentLng(73.8410);
        activeBuses.get(3).setCurrentStopIndex(5);

        // --- Mumbai buses ---
        // 2 buses on Mumbai Route M1 (CST → Andheri)
        activeBuses.add(new SimulatedBus(5L, "MUM-M1-01", 3L, MUMBAI_ROUTE_1));
        activeBuses.add(new SimulatedBus(6L, "MUM-M1-02", 3L, MUMBAI_ROUTE_1));
        activeBuses.get(5).setCurrentLat(18.9932);
        activeBuses.get(5).setCurrentLng(72.8226);
        activeBuses.get(5).setCurrentStopIndex(5);

        // 2 buses on Mumbai Route M2 (Dadar → Powai)
        activeBuses.add(new SimulatedBus(7L, "MUM-M2-01", 4L, MUMBAI_ROUTE_2));
        activeBuses.add(new SimulatedBus(8L, "MUM-M2-02", 4L, MUMBAI_ROUTE_2));
        activeBuses.get(7).setCurrentLat(19.0544);
        activeBuses.get(7).setCurrentLng(72.8688);
        activeBuses.get(7).setCurrentStopIndex(5);

        log.info("Initialized {} simulated buses (Pune: 4, Mumbai: 4)", activeBuses.size());
    }

    @Scheduled(fixedRateString = "${simulator.tick-rate-ms:3000}")
    public void tick() {
        boolean rushHour = isRushHour();

        for (SimulatedBus bus : activeBuses) {
            if ("BREAKDOWN".equals(bus.getStatus())) {
                // Auto-recover after 60 seconds
                if (bus.getBreakdownTime() != null &&
                        java.time.Instant.now().isAfter(bus.getBreakdownTime().plusSeconds(60))) {
                    bus.setStatus("ACTIVE");
                    bus.setBreakdownTime(null);
                    // Reset to nearest stop
                    double[] start = bus.getRouteCoordinates().get(0);
                    bus.setCurrentLat(start[0]);
                    bus.setCurrentLng(start[1]);
                    bus.setCurrentStopIndex(1);
                    log.info("Bus {} has recovered from breakdown, restarting route",
                            bus.getBusNumber());
                }
                continue;
            }

            // Random breakdown (0.05% chance per tick — ~1 breakdown every 10 min per bus)
            if (ThreadLocalRandom.current().nextDouble() < breakdownProbability) {
                bus.setStatus("BREAKDOWN");
                bus.setBreakdownTime(java.time.Instant.now());
                log.warn("Bus {} has broken down at ({}, {})",
                        bus.getBusNumber(), bus.getCurrentLat(), bus.getCurrentLng());
                continue;
            }

            // Interpolate position toward next stop
            double[] nextStop = bus.getNextStopCoords();
            double factor = rushHour
                    ? interpolationFactor * rushHourSpeedFactor
                    : interpolationFactor;

            double prevLat = bus.getCurrentLat();
            double prevLng = bus.getCurrentLng();

            double newLat = prevLat + (nextStop[0] - prevLat) * factor;
            double newLng = prevLng + (nextStop[1] - prevLng) * factor;
            bus.setCurrentLat(newLat);
            bus.setCurrentLng(newLng);

            // Check if reached next stop
            if (bus.hasReachedNextStop(STOP_PROXIMITY_THRESHOLD)) {
                log.info("Bus {} reached stop index {}", bus.getBusNumber(), bus.getCurrentStopIndex());
                bus.advanceToNextStop();
            }

            // Simulate occupancy
            if (rushHour) {
                bus.setOccupancyPercent(70 + ThreadLocalRandom.current().nextInt(26)); // 70-95
            } else {
                bus.setOccupancyPercent(20 + ThreadLocalRandom.current().nextInt(21)); // 20-40
            }

            // Compute approximate speed (km/h) from movement
            double distKm = haversineKm(prevLat, prevLng, newLat, newLng);
            double timeHours = 3.0 / 3600.0; // 3-second tick
            double speedKmh = distKm / timeHours;
            if (rushHour) {
                speedKmh = Math.min(speedKmh, 25.0); // cap during rush hour
            }

            publishToKafka(bus, speedKmh);
        }
    }

    private void publishToKafka(SimulatedBus bus, double speedKmh) {
        BusLocationEvent event = BusLocationEvent.builder()
                .busId(bus.getBusId())
                .busNumber(bus.getBusNumber())
                .routeId(bus.getRouteId())
                .latitude(bus.getCurrentLat())
                .longitude(bus.getCurrentLng())
                .occupancyPercent(bus.getOccupancyPercent())
                .crowdLabel(CrowdLabelUtil.fromOccupancyEmoji(bus.getOccupancyPercent()))
                .speed(Math.round(speedKmh * 100.0) / 100.0)
                .currentStopIndex(bus.getCurrentStopIndex())
                .recordedAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send(TOPIC, String.valueOf(bus.getBusId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish location for bus {}: {}",
                                bus.getBusNumber(), ex.getMessage());
                    } else {
                        log.debug("Published location for bus {} → ({}, {}), speed={} km/h",
                                bus.getBusNumber(), bus.getCurrentLat(),
                                bus.getCurrentLng(), speedKmh);
                    }
                });
    }

    private boolean isRushHour() {
        LocalTime now = LocalTime.now();
        return (now.isAfter(LocalTime.of(8, 0)) && now.isBefore(LocalTime.of(10, 0)))
                || (now.isAfter(LocalTime.of(17, 0)) && now.isBefore(LocalTime.of(19, 0)));
    }

    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
