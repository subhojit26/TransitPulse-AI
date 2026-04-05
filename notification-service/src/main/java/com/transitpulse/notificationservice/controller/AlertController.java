package com.transitpulse.notificationservice.controller;

import com.transitpulse.common.dto.AlertDto;
import com.transitpulse.common.dto.ApiResponse;
import com.transitpulse.notificationservice.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping("/stops/{stopId}/alerts")
    public ApiResponse<List<AlertDto>> getStopAlerts(
            @PathVariable Long stopId,
            @RequestParam(defaultValue = "10") int last) {
        return ApiResponse.ok(alertService.getRecentAlerts(stopId));
    }
}
