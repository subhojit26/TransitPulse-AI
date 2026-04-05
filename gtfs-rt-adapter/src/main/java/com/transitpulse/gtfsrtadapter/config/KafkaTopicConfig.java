package com.transitpulse.gtfsrtadapter.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic busLocationEventsTopic() {
        return TopicBuilder.name("bus-location-events")
                .partitions(6).replicas(1).build();
    }
}
