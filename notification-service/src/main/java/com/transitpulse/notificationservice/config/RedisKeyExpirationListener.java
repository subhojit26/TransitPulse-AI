package com.transitpulse.notificationservice.config;

import com.transitpulse.common.dto.BusAlertEvent;
import com.transitpulse.notificationservice.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisKeyExpirationListener implements MessageListener {

    private final AlertService alertService;
    private final KafkaTemplate<String, BusAlertEvent> kafkaTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();

        if (expiredKey.startsWith("bus:live:")) {
            String busIdStr = expiredKey.substring("bus:live:".length());
            try {
                Long busId = Long.parseLong(busIdStr);
                log.warn("Bus {} key expired - possible breakdown detected", busId);

                // Create breakdown alert
                BusAlertEvent breakdownAlert = BusAlertEvent.builder()
                        .busId(busId)
                        .busNumber("BUS-" + busId)
                        .occupancyPercent(0)
                        .crowdLabel("N/A")
                        .timestamp(LocalDateTime.now())
                        .build();

                // Publish to Kafka alert topic
                kafkaTemplate.send("bus-alert-events", busIdStr, breakdownAlert);

                // Also save directly as a BREAKDOWN alert
                String alertMessage = String.format(
                        "Bus %s is out of service. Please use alternate route.",
                        "BUS-" + busId);
                alertService.saveAlert(null, busId, "BREAKDOWN_ALERT", alertMessage);

            } catch (NumberFormatException e) {
                log.debug("Ignoring non-numeric bus key: {}", expiredKey);
            }
        }
    }
}
