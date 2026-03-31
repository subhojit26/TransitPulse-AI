package com.transitpulse.commuterservice.controller;

import com.transitpulse.common.dto.ApiResponse;
import com.transitpulse.common.dto.IncomingBusDto;
import com.transitpulse.common.dto.NearbyStopDto;
import com.transitpulse.commuterservice.service.CommuterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commuter")
@RequiredArgsConstructor
public class CommuterController {

    private final CommuterService commuterService;

    @GetMapping("/stops/nearby")
    public ApiResponse<List<NearbyStopDto>> getNearbyStops(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "500") double radiusMeters) {
        return ApiResponse.ok(commuterService.findNearbyStops(lat, lng, radiusMeters));
    }

    @GetMapping("/stops/{stopId}/incoming-buses")
    public ApiResponse<List<IncomingBusDto>> getIncomingBuses(@PathVariable Long stopId) {
        return ApiResponse.ok(commuterService.getIncomingBuses(stopId));
    }
}
