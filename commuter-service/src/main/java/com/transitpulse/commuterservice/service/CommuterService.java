package com.transitpulse.commuterservice.service;

import com.transitpulse.common.dto.BusEtaEvent;
import com.transitpulse.common.dto.BusLocationEvent;
import com.transitpulse.common.dto.IncomingBusDto;
import com.transitpulse.common.dto.NearbyStopDto;
import com.transitpulse.common.entity.Bus;
import com.transitpulse.common.entity.BusLocationHistory;
import com.transitpulse.common.entity.Stop;
import com.transitpulse.common.exception.ResourceNotFoundException;
import com.transitpulse.common.util.CrowdLabelUtil;
import com.transitpulse.commuterservice.repository.BusLocationHistoryRepository;
import com.transitpulse.commuterservice.repository.BusRepository;
import com.transitpulse.commuterservice.repository.StopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommuterService {

    private final StopRepository stopRepository;
    private final BusRepository busRepository;
    private final BusLocationHistoryRepository historyRepository;
    private final LiveBusCacheReader liveBusCacheReader;

    public List<NearbyStopDto> findNearbyStops(double lat, double lng, double radiusMeters) {
        List<Object[]> results = stopRepository.findNearbyStops(lat, lng, radiusMeters);

        return results.stream()
                .map(row -> NearbyStopDto.builder()
                        .stopId(((Number) row[0]).longValue())
                        .stopName((String) row[1])
                        .latitude(((Number) row[2]).doubleValue())
                        .longitude(((Number) row[3]).doubleValue())
                        .routeId(((Number) row[4]).longValue())
                        .routeNumber((String) row[6])
                        .routeName((String) row[7])
                        .distanceMeters(((Number) row[8]).doubleValue())
                        .build())
                .toList();
    }

    public List<IncomingBusDto> getIncomingBuses(Long stopId) {
        Stop stop = stopRepository.findById(stopId)
                .orElseThrow(() -> new ResourceNotFoundException("Stop", stopId));

        Long routeId = stop.getRoute().getId();
        List<Bus> activeBuses = busRepository.findActiveByRouteId(routeId);

        List<IncomingBusDto> result = new ArrayList<>();
        for (Bus bus : activeBuses) {
            Integer occupancy = null;
            String crowdLabel = "UNKNOWN";
            String eta = "calculating...";

            // Try Redis ETA cache first for real-time ETA
            Optional<BusEtaEvent> etaEvent = liveBusCacheReader.getEta(bus.getId());
            if (etaEvent.isPresent()) {
                BusEtaEvent etaData = etaEvent.get();
                double etaMinutes = etaData.getEtaMinutes() != null ? etaData.getEtaMinutes() : 0.0;
                eta = String.format("%.1f min", etaMinutes);
                if (etaData.getOccupancyPercent() != null) {
                    occupancy = etaData.getOccupancyPercent();
                }
                if (etaData.getCrowdLabel() != null) {
                    crowdLabel = etaData.getCrowdLabel();
                }
            }

            // Try Redis live data for occupancy (fallback / more recent)
            Optional<BusLocationEvent> liveData = liveBusCacheReader.getLiveLocation(bus.getId());
            if (liveData.isPresent()) {
                if (occupancy == null) {
                    occupancy = liveData.get().getOccupancyPercent();
                    crowdLabel = CrowdLabelUtil.fromOccupancyEmoji(occupancy);
                }
            } else if (occupancy == null) {
                // Fallback to DB
                Optional<BusLocationHistory> history =
                        historyRepository.findTopByBusIdOrderByRecordedAtDesc(bus.getId());
                if (history.isPresent()) {
                    occupancy = history.get().getOccupancyPercent();
                    crowdLabel = CrowdLabelUtil.fromOccupancyEmoji(occupancy);
                }
            }

            result.add(IncomingBusDto.builder()
                    .busId(bus.getId())
                    .busNumber(bus.getBusNumber())
                    .routeNumber(bus.getRoute().getRouteNumber())
                    .eta(eta)
                    .occupancyPercent(occupancy)
                    .crowdLabel(crowdLabel)
                    .status(bus.getStatus())
                    .build());
        }

        return result;
    }
}
