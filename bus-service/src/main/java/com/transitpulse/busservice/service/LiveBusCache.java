package com.transitpulse.busservice.service;

import com.transitpulse.common.dto.BusLocationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveBusCache {

    private static final String KEY_PREFIX = "bus:live:";
    private static final Duration TTL = Duration.ofSeconds(30);

    private final RedisTemplate<String, BusLocationEvent> liveBusRedisTemplate;

    public void updateLiveLocation(BusLocationEvent event) {
        String key = KEY_PREFIX + event.getBusId();
        liveBusRedisTemplate.opsForValue().set(key, event, TTL);
        log.debug("Updated live location cache for bus {}", event.getBusId());
    }

    public Optional<BusLocationEvent> getLiveLocation(Long busId) {
        BusLocationEvent event = liveBusRedisTemplate.opsForValue().get(KEY_PREFIX + busId);
        return Optional.ofNullable(event);
    }

    public List<BusLocationEvent> getAllLiveBuses() {
        Set<String> keys = liveBusRedisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        List<BusLocationEvent> events = liveBusRedisTemplate.opsForValue().multiGet(keys);
        if (events == null) {
            return Collections.emptyList();
        }
        return events.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
