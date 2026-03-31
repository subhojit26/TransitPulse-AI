package com.transitpulse.busservice.service;

import com.transitpulse.busservice.repository.BusRepository;
import com.transitpulse.common.dto.BusDto;
import com.transitpulse.common.entity.Bus;
import com.transitpulse.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusService {

    private final BusRepository busRepository;

    public List<BusDto> getAllBuses() {
        return busRepository.findAllWithRoute().stream()
                .map(this::toDto)
                .toList();
    }

    public BusDto getBusById(Long busId) {
        Bus bus = busRepository.findById(busId)
                .orElseThrow(() -> new ResourceNotFoundException("Bus", busId));
        return toDto(bus);
    }

    private BusDto toDto(Bus bus) {
        return BusDto.builder()
                .id(bus.getId())
                .busNumber(bus.getBusNumber())
                .routeId(bus.getRoute().getId())
                .routeNumber(bus.getRoute().getRouteNumber())
                .routeName(bus.getRoute().getRouteName())
                .capacity(bus.getCapacity())
                .status(bus.getStatus())
                .build();
    }
}
