package com.transitpulse.busservice.service;

import com.transitpulse.busservice.repository.RouteRepository;
import com.transitpulse.busservice.repository.StopRepository;
import com.transitpulse.common.dto.RouteDto;
import com.transitpulse.common.dto.StopDto;
import com.transitpulse.common.entity.Route;
import com.transitpulse.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteService {

    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;

    public List<RouteDto> getAllRoutes() {
        return routeRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public List<StopDto> getStopsByRouteId(Long routeId) {
        routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));

        return stopRepository.findByRouteIdOrderByStopSequence(routeId).stream()
                .map(this::toStopDto)
                .toList();
    }

    private RouteDto toDto(Route route) {
        return RouteDto.builder()
                .id(route.getId())
                .routeNumber(route.getRouteNumber())
                .routeName(route.getRouteName())
                .createdAt(route.getCreatedAt())
                .stopCount(route.getStops() != null ? route.getStops().size() : 0)
                .busCount(route.getBuses() != null ? route.getBuses().size() : 0)
                .build();
    }

    private StopDto toStopDto(com.transitpulse.common.entity.Stop stop) {
        return StopDto.builder()
                .id(stop.getId())
                .routeId(stop.getRoute().getId())
                .stopName(stop.getStopName())
                .stopSequence(stop.getStopSequence())
                .latitude(stop.getLatitude())
                .longitude(stop.getLongitude())
                .build();
    }
}
