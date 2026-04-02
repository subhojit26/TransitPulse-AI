package com.transitpulse.busservice.service;

import com.transitpulse.common.dto.BusEtaEvent;
import com.transitpulse.common.dto.BusLocationEvent;
import com.transitpulse.common.entity.Bus;
import com.transitpulse.common.entity.BusLocationHistory;
import com.transitpulse.busservice.repository.BusLocationHistoryRepository;
import com.transitpulse.busservice.repository.BusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationEventConsumer {

    private final BusLocationHistoryRepository locationHistoryRepo;
    private final BusRepository busRepository;
    private final LiveBusCache liveBusCache;
    private final RedisTemplate<String, BusEtaEvent> etaRedisTemplate;

    private static final String ETA_KEY_PREFIX = "bus:eta:";

    @KafkaListener(
            topics = "bus-location-events",
            containerFactory = "locationListenerFactory",
            groupId = "bus-service-location-group"
    )
    @Transactional
    public void consumeLocation(BusLocationEvent event) {
        log.debug("Received location event for bus {}: ({}, {})",
                event.getBusId(), event.getLatitude(), event.getLongitude());

        // 1. Save to PostgreSQL history table
        Bus bus = busRepository.findById(event.getBusId()).orElse(null);
        if (bus != null) {
            BusLocationHistory history = BusLocationHistory.builder()
                    .bus(bus)
                    .latitude(event.getLatitude())
                    .longitude(event.getLongitude())
                    .occupancyPercent(event.getOccupancyPercent())
                    .build();
            locationHistoryRepo.save(history);
        }

        // 2. Update Redis live cache (TTL 30s)
        liveBusCache.updateLiveLocation(event);
    }

    @KafkaListener(
            topics = "bus-eta-events",
            containerFactory = "etaListenerFactory",
            groupId = "bus-service-eta-group"
    )
    public void consumeEta(BusEtaEvent event) {
        log.debug("Received ETA event for bus {}: {} min to {}",
                event.getBusId(), event.getEtaMinutes(), event.getNextStopName());

        // Update Redis ETA cache (TTL 60s)
        etaRedisTemplate.opsForValue().set(
                ETA_KEY_PREFIX + event.getBusId(),
                event,
                Duration.ofSeconds(60)
        );
    }
}
