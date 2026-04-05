package com.transitpulse.aipredictionservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OllamaClient {

    private final WebClient ollamaWebClient;
    private final ObjectMapper redisObjectMapper;

    @Value("${ollama.model}")
    private String model;

    public Optional<String> generate(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", false
            );

            String responseBody = ollamaWebClient.post()
                    .uri("/api/generate")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseBody != null) {
                JsonNode node = redisObjectMapper.readTree(responseBody);
                String response = node.path("response").asText("");
                if (!response.isBlank()) {
                    return Optional.of(response);
                }
            }
        } catch (Exception e) {
            log.warn("Ollama call failed: {}", e.getMessage());
        }
        return Optional.empty();
    }
}
