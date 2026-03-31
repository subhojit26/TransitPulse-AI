package com.transitpulse.busservice.controller;

import com.transitpulse.busservice.service.RouteService;
import com.transitpulse.common.dto.ApiResponse;
import com.transitpulse.common.dto.RouteDto;
import com.transitpulse.common.dto.StopDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    public ApiResponse<List<RouteDto>> getAllRoutes() {
        return ApiResponse.ok(routeService.getAllRoutes());
    }

    @GetMapping("/{id}/stops")
    public ApiResponse<List<StopDto>> getStopsByRoute(@PathVariable Long id) {
        return ApiResponse.ok(routeService.getStopsByRouteId(id));
    }
}
