package com.transitpulse.busservice.config;

import com.transitpulse.common.dto.BusEtaEvent;
import com.transitpulse.common.dto.BusLocationEvent;
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
    public ConsumerFactory<String, BusLocationEvent> locationConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
        JsonDeserializer<BusLocationEvent> deserializer = new JsonDeserializer<>(BusLocationEvent.class, false);
        deserializer.addTrustedPackages("com.transitpulse.common.dto");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BusLocationEvent> locationListenerFactory(
            ConsumerFactory<String, BusLocationEvent> locationConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, BusLocationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(locationConsumerFactory);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, BusEtaEvent> etaConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
        JsonDeserializer<BusEtaEvent> deserializer = new JsonDeserializer<>(BusEtaEvent.class, false);
        deserializer.addTrustedPackages("com.transitpulse.common.dto");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BusEtaEvent> etaListenerFactory(
            ConsumerFactory<String, BusEtaEvent> etaConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, BusEtaEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(etaConsumerFactory);
        return factory;
    }
}
