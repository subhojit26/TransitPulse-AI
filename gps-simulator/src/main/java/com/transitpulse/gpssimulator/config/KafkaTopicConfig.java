package com.transitpulse.gpssimulator.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic busLocationEventsTopic() {
        return TopicBuilder.name("bus-location-events")
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic busOccupancyEventsTopic() {
        return TopicBuilder.name("bus-occupancy-events")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic busEtaEventsTopic() {
        return TopicBuilder.name("bus-eta-events")
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic busAlertEventsTopic() {
        return TopicBuilder.name("bus-alert-events")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
