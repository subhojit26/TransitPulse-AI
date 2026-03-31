package com.transitpulse.occupancyservice.service;

import com.transitpulse.common.dto.BusLocationEvent;
import com.transitpulse.common.dto.OccupancyDto;
import com.transitpulse.common.dto.OccupancyUpdateRequest;
import com.transitpulse.common.entity.Bus;
import com.transitpulse.common.entity.BusLocationHistory;
import com.transitpulse.common.exception.ResourceNotFoundException;
import com.transitpulse.common.util.CrowdLabelUtil;
import com.transitpulse.occupancyservice.repository.BusLocationHistoryRepository;
import com.transitpulse.occupancyservice.repository.BusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OccupancyService {

    private final BusRepository busRepository;
    private final BusLocationHistoryRepository historyRepository;
    private final LiveBusCache liveBusCache;

    @Transactional
    public OccupancyDto updateOccupancy(OccupancyUpdateRequest request) {
        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus", request.getBusId()));

        // Save to location history
        BusLocationHistory history = BusLocationHistory.builder()
                .bus(bus)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .occupancyPercent(request.getOccupancyPercent())
                .build();
        historyRepository.save(history);

        // Update Redis cache
        BusLocationEvent event = BusLocationEvent.builder()
                .busId(bus.getId())
                .busNumber(bus.getBusNumber())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .occupancyPercent(request.getOccupancyPercent())
                .crowdLabel(CrowdLabelUtil.fromOccupancy(request.getOccupancyPercent()))
                .conductorId(request.getConductorId())
                .recordedAt(LocalDateTime.now())
                .build();
        liveBusCache.updateLiveLocation(event);

        log.info("Occupancy updated for bus {} by conductor {}: {}%",
                bus.getBusNumber(), request.getConductorId(), request.getOccupancyPercent());

        return OccupancyDto.builder()
                .busId(bus.getId())
                .busNumber(bus.getBusNumber())
                .occupancyPercent(request.getOccupancyPercent())
                .crowdLabel(CrowdLabelUtil.fromOccupancy(request.getOccupancyPercent()))
                .conductorId(request.getConductorId())
                .recordedAt(history.getRecordedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public OccupancyDto getCurrentOccupancy(Long busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", busId));

        // Try Redis first
        return liveBusCache.getLiveLocation(busId)
                .map(event -> OccupancyDto.builder()
                        .busId(bus.getId())
                        .busNumber(bus.getBusNumber())
                        .occupancyPercent(event.getOccupancyPercent())
                        .crowdLabel(CrowdLabelUtil.fromOccupancy(event.getOccupancyPercent()))
                        .conductorId(event.getConductorId())
                        .recordedAt(event.getRecordedAt())
                        .build())
                // Fallback to DB
                .orElseGet(() -> historyRepository.findTopByBusIdOrderByRecordedAtDesc(busId)
                        .map(h -> OccupancyDto.builder()
                                .busId(bus.getId())
                                .busNumber(bus.getBusNumber())
                                .occupancyPercent(h.getOccupancyPercent())
                                .crowdLabel(CrowdLabelUtil.fromOccupancy(h.getOccupancyPercent()))
                                .recordedAt(h.getRecordedAt())
                                .build())
                        .orElse(OccupancyDto.builder()
                                .busId(bus.getId())
                                .busNumber(bus.getBusNumber())
                                .occupancyPercent(null)
                                .crowdLabel("UNKNOWN")
                                .build()));
    }
}
