package com.transitpulse.commuterservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transitpulse.common.dto.AlertDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationClient {

    private final WebClient notificationWebClient;
    private final ObjectMapper redisObjectMapper;

    public List<AlertDto> getRecentAlerts(Long stopId) {
        try {
            String responseBody = notificationWebClient.get()
                    .uri("/api/notifications/stops/{stopId}/alerts?last=10", stopId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseBody != null) {
                JsonNode root = redisObjectMapper.readTree(responseBody);
                JsonNode data = root.path("data");
                if (data.isArray()) {
                    return redisObjectMapper.readerForListOf(AlertDto.class).readValue(data);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch alerts for stop {}: {}", stopId, e.getMessage());
        }
        return Collections.emptyList();
    }
}
