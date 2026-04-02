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

    @Value("${simulator.breakdown-probability:0.001}")
    private double breakdownProbability;

    @Value("${simulator.rush-hour-speed-factor:0.5}")
    private double rushHourSpeedFactor;

    // Route 10: Sitabuldi - Automotive Square (8 stops)
    private static final List<double[]> ROUTE_10_COORDS = List.of(
            new double[]{21.1458, 79.0882},  // Sitabuldi Bus Stand
            new double[]{21.1396, 79.0787},  // Variety Square
            new double[]{21.1370, 79.0675},  // Law College Square
            new double[]{21.1345, 79.0572},  // Shankar Nagar Square
            new double[]{21.1289, 79.0480},  // Pratap Nagar Square
            new double[]{21.1225, 79.0385},  // Trimurti Nagar
            new double[]{21.1170, 79.0290},  // Manewada Square
            new double[]{21.1102, 79.0195}   // Automotive Square
    );

    // Route 47B: Dharampeth - Hingna T-Point (7 stops)
    private static final List<double[]> ROUTE_47B_COORDS = List.of(
            new double[]{21.1510, 79.0780},  // Dharampeth Bus Stop
            new double[]{21.1475, 79.0710},  // Telephone Exchange Sq.
            new double[]{21.1430, 79.0630},  // Laxmi Nagar Square
            new double[]{21.1380, 79.0545},  // Panchsheel Square
            new double[]{21.1320, 79.0450},  // Nandanvan Colony
            new double[]{21.1250, 79.0350},  // Wadi Bus Stop
            new double[]{21.1180, 79.0255}   // Hingna T-Point
    );

    @PostConstruct
    public void initBuses() {
        // 3 buses on Route 10
        activeBuses.add(new SimulatedBus(1L, "BUS-10-01", 1L, ROUTE_10_COORDS));
        activeBuses.add(new SimulatedBus(2L, "BUS-10-02", 1L, ROUTE_10_COORDS));
        // Start bus 2 midway
        SimulatedBus bus2 = activeBuses.get(1);
        bus2.setCurrentLat(21.1345);
        bus2.setCurrentLng(79.0572);
        bus2.setCurrentStopIndex(4);

        // 2 buses on Route 47B
        activeBuses.add(new SimulatedBus(4L, "BUS-47B-01", 2L, ROUTE_47B_COORDS));
        activeBuses.add(new SimulatedBus(5L, "BUS-47B-02", 2L, ROUTE_47B_COORDS));
        // Start bus 5 midway
        SimulatedBus bus5 = activeBuses.get(3);
        bus5.setCurrentLat(21.1380);
        bus5.setCurrentLng(79.0545);
        bus5.setCurrentStopIndex(4);

        // 5th bus on Route 10
        SimulatedBus bus3 = new SimulatedBus(3L, "BUS-10-03", 1L, ROUTE_10_COORDS);
        bus3.setCurrentLat(21.1225);
        bus3.setCurrentLng(79.0385);
        bus3.setCurrentStopIndex(6);
        activeBuses.add(bus3);

        log.info("Initialized {} simulated buses", activeBuses.size());
    }

    @Scheduled(fixedRateString = "${simulator.tick-rate-ms:3000}")
    public void tick() {
        boolean rushHour = isRushHour();

        for (SimulatedBus bus : activeBuses) {
            if ("BREAKDOWN".equals(bus.getStatus())) {
                // Don't publish — Redis TTL will expire naturally
                continue;
            }

            // Random breakdown (0.1% chance per tick)
            if (ThreadLocalRandom.current().nextDouble() < breakdownProbability) {
                bus.setStatus("BREAKDOWN");
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
