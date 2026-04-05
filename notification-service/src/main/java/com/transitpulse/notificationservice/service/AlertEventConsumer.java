package com.transitpulse.notificationservice.service;

import com.transitpulse.common.dto.BusAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertEventConsumer {

    private final AlertService alertService;

    @KafkaListener(
            topics = "bus-alert-events",
            containerFactory = "alertListenerFactory",
            groupId = "notification-service-alert-group"
    )
    public void consumeAlertEvent(BusAlertEvent event) {
        log.debug("Received alert event for bus {} at stop {}", event.getBusId(), event.getStopId());

        // Rule 1: CROWD_ALERT — occupancy > 80%
        if (event.getOccupancyPercent() != null && event.getOccupancyPercent() > 80) {
            String message = String.format(
                    "Bus %s is very crowded (%d%%). Next bus in %.0f min may have more space.",
                    event.getBusNumber(), event.getOccupancyPercent(),
                    event.getEtaMinutes() != null ? event.getEtaMinutes() + 5 : 10.0);
            alertService.saveAlert(event.getStopId(), event.getBusId(),
                    "CROWD_ALERT", message);
        }

        // Rule 2: DELAY_ALERT — ETA > 5 min beyond expected
        // If ETA exceeds 15 min (proxy for significant delay), flag it
        if (event.getEtaMinutes() != null && event.getEtaMinutes() > 15) {
            String message = String.format(
                    "Bus %s is running %.0f min late.",
                    event.getBusNumber(), event.getEtaMinutes() - 10);
            alertService.saveAlert(event.getStopId(), event.getBusId(),
                    "DELAY_ALERT", message);
        }

        // Rule 4: FIRST_BUS_ALERT — bus is close (ETA < 5 minutes)
        if (event.getEtaMinutes() != null && event.getEtaMinutes() <= 5 && event.getEtaMinutes() > 0) {
            String message = String.format(
                    "Bus %s arriving in ~%.0f min. Occupancy: %d%%",
                    event.getBusNumber(),
                    event.getEtaMinutes(),
                    event.getOccupancyPercent() != null ? event.getOccupancyPercent() : 0);
            alertService.saveAlert(event.getStopId(), event.getBusId(),
                    "FIRST_BUS_ALERT", message);
        }
    }
}
