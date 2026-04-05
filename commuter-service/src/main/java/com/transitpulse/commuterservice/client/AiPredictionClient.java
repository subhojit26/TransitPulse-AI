package com.transitpulse.commuterservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transitpulse.common.dto.AiPrediction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiPredictionClient {

    private final WebClient aiPredictionWebClient;
    private final ObjectMapper redisObjectMapper;

    public Optional<AiPrediction> predictEta(Long busId, Long stopId, Double streamEta) {
        try {
            String responseBody = aiPredictionWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/ai/predict-eta")
                            .queryParam("busId", busId)
                            .queryParam("stopId", stopId)
                            .queryParam("streamEta", streamEta)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseBody != null) {
                JsonNode root = redisObjectMapper.readTree(responseBody);
                JsonNode data = root.path("data");
                if (!data.isMissingNode()) {
                    AiPrediction prediction = redisObjectMapper.treeToValue(data, AiPrediction.class);
                    return Optional.of(prediction);
                }
            }
        } catch (Exception e) {
            log.warn("AI prediction call failed for bus {} stop {}: {}", busId, stopId, e.getMessage());
        }
        return Optional.empty();
    }
}
