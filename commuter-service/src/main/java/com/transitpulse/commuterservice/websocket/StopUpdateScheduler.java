package com.transitpulse.commuterservice.websocket;

import com.transitpulse.common.dto.*;
import com.transitpulse.common.entity.Bus;
import com.transitpulse.common.entity.Stop;
import com.transitpulse.common.util.CrowdLabelUtil;
import com.transitpulse.commuterservice.client.AiPredictionClient;
import com.transitpulse.commuterservice.client.NotificationClient;
import com.transitpulse.commuterservice.repository.BusRepository;
import com.transitpulse.commuterservice.repository.StopRepository;
import com.transitpulse.commuterservice.service.LiveBusCacheReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class StopUpdateScheduler {

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;
    private final StopRepository stopRepository;
    private final BusRepository busRepository;
    private final LiveBusCacheReader liveBusCacheReader;
    private final AiPredictionClient aiPredictionClient;
    private final NotificationClient notificationClient;

    // Track subscribed stops (populated by subscription interceptor or manually)
    private final Set<Long> activeStopSubscriptions = ConcurrentHashMap.newKeySet();

    public void addSubscription(Long stopId) {
        activeStopSubscriptions.add(stopId);
    }

    public void removeSubscription(Long stopId) {
        activeStopSubscriptions.remove(stopId);
    }

    @Scheduled(fixedRate = 10000)
    public void pushStopUpdates() {
        if (activeStopSubscriptions.isEmpty()) {
            // If no explicit subscriptions, push for all stops (demo mode)
            List<Stop> allStops = stopRepository.findAll();
            for (Stop stop : allStops) {
                pushUpdateForStop(stop);
            }
        } else {
            for (Long stopId : activeStopSubscriptions) {
                stopRepository.findById(stopId).ifPresent(this::pushUpdateForStop);
            }
        }
    }

    @Scheduled(fixedRate = 3000)
    public void pushLiveBusPositions() {
        List<Bus> allBuses = busRepository.findAll();
        Set<Long> dbBusIds = new HashSet<>();
        List<Map<String, Object>> liveBuses = new ArrayList<>();

        for (Bus bus : allBuses) {
            dbBusIds.add(bus.getId());
            Optional<BusLocationEvent> loc = liveBusCacheReader.getLiveLocation(bus.getId());
            if (loc.isEmpty()) continue;

            BusLocationEvent e = loc.get();
            Map<String, Object> entry = new HashMap<>();
            entry.put("busId", bus.getId());
            entry.put("busNumber", bus.getBusNumber());
            entry.put("latitude", e.getLatitude());
            entry.put("longitude", e.getLongitude());
            entry.put("occupancyPercent", e.getOccupancyPercent());
            entry.put("crowdLabel", CrowdLabelUtil.fromOccupancyEmoji(
                    e.getOccupancyPercent() != null ? e.getOccupancyPercent() : 0));
            entry.put("status", bus.getStatus());
            entry.put("speed", e.getSpeed());

            Optional<BusEtaEvent> eta = liveBusCacheReader.getEta(bus.getId());
            eta.ifPresent(etaEvt -> entry.put("etaMinutes", etaEvt.getEtaMinutes()));

            liveBuses.add(entry);
        }

        // Include GTFS-RT buses that exist only in Redis (e.g., NYC MTA live buses)
        List<BusLocationEvent> allRedis = liveBusCacheReader.getAllLiveBuses();
        for (BusLocationEvent e : allRedis) {
            if (e.getBusId() == null || dbBusIds.contains(e.getBusId())) continue;

            Map<String, Object> entry = new HashMap<>();
            entry.put("busId", e.getBusId());
            entry.put("busNumber", e.getBusNumber());
            entry.put("latitude", e.getLatitude());
            entry.put("longitude", e.getLongitude());
            entry.put("occupancyPercent", e.getOccupancyPercent());
            entry.put("crowdLabel", CrowdLabelUtil.fromOccupancyEmoji(
                    e.getOccupancyPercent() != null ? e.getOccupancyPercent() : 0));
            entry.put("status", "ACTIVE");
            entry.put("speed", e.getSpeed());

            Optional<BusEtaEvent> eta = liveBusCacheReader.getEta(e.getBusId());
            eta.ifPresent(etaEvt -> entry.put("etaMinutes", etaEvt.getEtaMinutes()));

            liveBuses.add(entry);
        }

        if (!liveBuses.isEmpty()) {
            messagingTemplate.convertAndSend("/topic/buses/live", liveBuses);
            log.debug("Pushed {} live bus positions", liveBuses.size());
        }
    }

    private void pushUpdateForStop(Stop stop) {
        Long routeId = stop.getRoute().getId();
        List<Bus> activeBuses = busRepository.findActiveByRouteId(routeId);

        List<StopUpdateMessage.IncomingBusLive> busUpdates = new ArrayList<>();

        for (Bus bus : activeBuses) {
            Double streamEta = null;
            Integer occupancy = null;
            String crowdLabel = "UNKNOWN";
            String status = bus.getStatus();

            // Get stream ETA from Redis
            Optional<BusEtaEvent> etaEvent = liveBusCacheReader.getEta(bus.getId());
            if (etaEvent.isPresent()) {
                BusEtaEvent eta = etaEvent.get();
                streamEta = eta.getEtaMinutes();
                if (eta.getOccupancyPercent() != null) {
                    occupancy = eta.getOccupancyPercent();
                }
                if (eta.getCrowdLabel() != null) {
                    crowdLabel = eta.getCrowdLabel();
                }
            }

            // Get live location for occupancy fallback
            Optional<BusLocationEvent> liveData = liveBusCacheReader.getLiveLocation(bus.getId());
            if (liveData.isPresent()) {
                if (occupancy == null) {
                    occupancy = liveData.get().getOccupancyPercent();
                    crowdLabel = CrowdLabelUtil.fromOccupancyEmoji(occupancy);
                }
            } else if (streamEta == null) {
                // No live data and no ETA → might be inactive
                status = "INACTIVE";
            }

            // Get AI prediction
            Double aiAdjustedEta = null;
            Double aiConfidence = null;
            String aiReason = null;

            if (streamEta != null) {
                Optional<AiPrediction> aiPrediction = aiPredictionClient
                        .predictEta(bus.getId(), stop.getId(), streamEta);
                if (aiPrediction.isPresent()) {
                    AiPrediction pred = aiPrediction.get();
                    aiAdjustedEta = pred.getAdjustedEtaMinutes();
                    aiConfidence = pred.getConfidence();
                    aiReason = pred.getReason();
                }
            }

            busUpdates.add(StopUpdateMessage.IncomingBusLive.builder()
                    .busNumber(bus.getBusNumber())
                    .routeNumber(bus.getRoute().getRouteNumber())
                    .streamEtaMinutes(streamEta)
                    .aiAdjustedEtaMinutes(aiAdjustedEta)
                    .aiConfidence(aiConfidence)
                    .aiReason(aiReason)
                    .occupancyPercent(occupancy)
                    .crowdLabel(crowdLabel)
                    .status(status)
                    .build());
        }

        // Fetch recent alerts
        List<AlertDto> alerts = notificationClient.getRecentAlerts(stop.getId());

        StopUpdateMessage message = StopUpdateMessage.builder()
                .stopId(stop.getId())
                .stopName(stop.getStopName())
                .buses(busUpdates)
                .alerts(alerts)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/stops/" + stop.getId(), message);
        log.debug("Pushed update for stop {} with {} buses", stop.getId(), busUpdates.size());
    }
}
