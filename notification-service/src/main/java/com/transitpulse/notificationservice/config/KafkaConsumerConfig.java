package com.transitpulse.notificationservice.config;

import com.transitpulse.common.dto.BusAlertEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, BusAlertEvent> alertConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
        JsonDeserializer<BusAlertEvent> deserializer = new JsonDeserializer<>(BusAlertEvent.class, false);
        deserializer.addTrustedPackages("com.transitpulse.common.dto");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BusAlertEvent> alertListenerFactory(
            ConsumerFactory<String, BusAlertEvent> alertConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, BusAlertEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(alertConsumerFactory);
        return factory;
    }
}
