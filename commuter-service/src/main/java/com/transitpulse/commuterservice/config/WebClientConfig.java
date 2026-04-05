package com.transitpulse.commuterservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${ai-prediction-service.base-url:http://localhost:8086}")
    private String aiPredictionBaseUrl;

    @Value("${notification-service.base-url:http://localhost:8087}")
    private String notificationBaseUrl;

    @Bean
    public WebClient aiPredictionWebClient() {
        return WebClient.builder()
                .baseUrl(aiPredictionBaseUrl)
                .build();
    }

    @Bean
    public WebClient notificationWebClient() {
        return WebClient.builder()
                .baseUrl(notificationBaseUrl)
                .build();
    }
}
