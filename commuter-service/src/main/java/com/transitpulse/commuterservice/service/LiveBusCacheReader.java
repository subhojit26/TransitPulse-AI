package com.transitpulse.commuterservice.service;

import com.transitpulse.common.dto.BusLocationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LiveBusCacheReader {

    private static final String KEY_PREFIX = "bus:live:";
    private final RedisTemplate<String, BusLocationEvent> redisTemplate;

    public Optional<BusLocationEvent> getLiveLocation(Long busId) {
        BusLocationEvent event = redisTemplate.opsForValue().get(KEY_PREFIX + busId);
        return Optional.ofNullable(event);
    }
}
