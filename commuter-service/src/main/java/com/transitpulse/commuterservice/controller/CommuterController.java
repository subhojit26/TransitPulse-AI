package com.transitpulse.commuterservice.controller;

import com.transitpulse.common.dto.AlertDto;
import com.transitpulse.common.dto.ApiResponse;
import com.transitpulse.common.dto.IncomingBusDto;
import com.transitpulse.common.dto.NearbyStopDto;
import com.transitpulse.commuterservice.client.NotificationClient;
import com.transitpulse.commuterservice.service.CommuterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commuter")
@RequiredArgsConstructor
public class CommuterController {

    private final CommuterService commuterService;
    private final NotificationClient notificationClient;

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

    @GetMapping("/stops/{stopId}/alerts")
    public ApiResponse<List<AlertDto>> getStopAlerts(
            @PathVariable Long stopId,
            @RequestParam(defaultValue = "10") int last) {
        return ApiResponse.ok(notificationClient.getRecentAlerts(stopId));
    }
}
