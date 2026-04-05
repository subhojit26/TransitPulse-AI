package com.transitpulse.aipredictionservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transitpulse.aipredictionservice.client.OllamaClient;
import com.transitpulse.aipredictionservice.repository.BusLocationHistoryRepository;
import com.transitpulse.common.dto.AiPrediction;
import com.transitpulse.common.entity.BusLocationHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiEtaPredictionService {

    private static final String CACHE_PREFIX = "ai:prediction:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    private final BusLocationHistoryRepository historyRepository;
    private final OllamaClient ollamaClient;
    private final RedisTemplate<String, AiPrediction> aiPredictionRedisTemplate;
    private final ObjectMapper redisObjectMapper;

    public AiPrediction predict(Long busId, Long stopId, Double streamEta,
                                int dayOfWeek, int hourOfDay) {
        // Check Redis cache first
        String cacheKey = CACHE_PREFIX + busId + ":" + stopId;
        AiPrediction cached = aiPredictionRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Returning cached AI prediction for bus {} stop {}", busId, stopId);
            return cached;
        }

        // 1. Query historical data (last 30 days, same hour ± 1)
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        int hourStart = Math.max(0, hourOfDay - 1);
        int hourEnd = Math.min(23, hourOfDay + 1);

        List<BusLocationHistory> historical = historyRepository
                .findHistoricalArrivals(busId, since, hourStart, hourEnd);

        // 2. Compute average historical delay
        double avgDelay = 0.0;
        if (!historical.isEmpty()) {
            // Use count of records as a rough proxy for frequency/delay pattern
            avgDelay = historical.size() > 10 ? 2.0 : 0.5;
        }

        // 3. Compute average occupancy trend from history
        double avgOccupancy = historical.stream()
                .filter(h -> h.getOccupancyPercent() != null)
                .mapToInt(BusLocationHistory::getOccupancyPercent)
                .average()
                .orElse(50.0);

        // 4. Build Ollama prompt
        String prompt = buildPrompt(streamEta, avgDelay, dayOfWeek, hourOfDay, avgOccupancy);

        // 5. Call Ollama and parse
        AiPrediction prediction = callOllamaAndParse(prompt, streamEta, avgDelay);

        // 6. Cache in Redis
        aiPredictionRedisTemplate.opsForValue().set(cacheKey, prediction, CACHE_TTL);
        log.debug("AI prediction for bus {} stop {}: {} min (confidence: {})",
                busId, stopId, prediction.getAdjustedEtaMinutes(), prediction.getConfidence());

        return prediction;
    }

    private String buildPrompt(Double streamEta, double avgDelay,
                               int dayOfWeek, int hourOfDay, double avgOccupancy) {
        String dayName = switch (dayOfWeek) {
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            case 6 -> "Saturday";
            case 7 -> "Sunday";
            default -> "Weekday";
        };

        return String.format("""
                You are a transit ETA prediction AI. Analyze the following data and predict the adjusted ETA.
                
                Current stream-computed ETA: %.1f minutes
                Historical average delay at this time: %.1f minutes
                Day: %s
                Hour: %d:00
                Average occupancy trend: %.0f%%
                
                Consider:
                - Higher occupancy means more stop dwell time
                - Rush hours (8-10am, 5-7pm) typically add 20-40%% delay
                - Weekends have lighter traffic
                
                Respond ONLY with valid JSON (no markdown, no explanation):
                {"adjustedEtaMinutes": <number>, "confidence": <0.0-1.0>, "reason": "<brief reason>"}
                """, streamEta, avgDelay, dayName, hourOfDay, avgOccupancy);
    }

    private AiPrediction callOllamaAndParse(String prompt, Double streamEta, double avgDelay) {
        Optional<String> response = ollamaClient.generate(prompt);

        if (response.isPresent()) {
            try {
                String raw = response.get().trim();
                // Extract JSON from response (handle potential surrounding text)
                int jsonStart = raw.indexOf('{');
                int jsonEnd = raw.lastIndexOf('}');
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    String json = raw.substring(jsonStart, jsonEnd + 1);
                    JsonNode node = redisObjectMapper.readTree(json);

                    double adjusted = node.path("adjustedEtaMinutes").asDouble(streamEta);
                    double confidence = node.path("confidence").asDouble(0.5);
                    String reason = node.path("reason").asText("AI-adjusted estimate");

                    // Sanity check: adjusted ETA should not be negative or unreasonably large
                    if (adjusted > 0 && adjusted < streamEta * 3) {
                        return AiPrediction.builder()
                                .adjustedEtaMinutes(Math.round(adjusted * 10.0) / 10.0)
                                .confidence(Math.round(confidence * 100.0) / 100.0)
                                .reason(reason)
                                .build();
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse Ollama response: {}", e.getMessage());
            }
        }

        // Fallback: use stream ETA + historical average delay
        return AiPrediction.builder()
                .adjustedEtaMinutes(Math.round((streamEta + avgDelay) * 10.0) / 10.0)
                .confidence(0.4)
                .reason("Fallback: stream ETA + historical avg delay")
                .build();
    }
}
