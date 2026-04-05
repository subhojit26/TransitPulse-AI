package com.transitpulse.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisKeyspaceConfig {

    @Bean
    public RedisMessageListenerContainer keyspaceEventsContainer(
            RedisConnectionFactory connectionFactory,
            RedisKeyExpirationListener keyExpirationListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(keyExpirationListener,
                new PatternTopic("__keyevent@*__:expired"));
        return container;
    }
}
