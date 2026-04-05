package com.transitpulse.commuterservice.service;

import com.transitpulse.common.dto.BusEtaEvent;
import com.transitpulse.common.dto.BusLocationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LiveBusCacheReader {

    private static final String LIVE_KEY_PREFIX = "bus:live:";
    private static final String ETA_KEY_PREFIX = "bus:eta:";

    private final RedisTemplate<String, BusLocationEvent> liveBusRedisTemplate;
    private final RedisTemplate<String, BusEtaEvent> etaRedisTemplate;

    public Optional<BusLocationEvent> getLiveLocation(Long busId) {
        BusLocationEvent event = liveBusRedisTemplate.opsForValue().get(LIVE_KEY_PREFIX + busId);
        return Optional.ofNullable(event);
    }

    public Optional<BusEtaEvent> getEta(Long busId) {
        BusEtaEvent eta = etaRedisTemplate.opsForValue().get(ETA_KEY_PREFIX + busId);
        return Optional.ofNullable(eta);
    }

    public List<BusLocationEvent> getAllLiveBuses() {
        Set<String> keys = liveBusRedisTemplate.keys(LIVE_KEY_PREFIX + "*");
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
