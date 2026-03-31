package com.transitpulse.busservice.controller;

import com.transitpulse.busservice.service.BusService;
import com.transitpulse.common.dto.ApiResponse;
import com.transitpulse.common.dto.BusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buses")
@RequiredArgsConstructor
public class BusController {

    private final BusService busService;

    @GetMapping
    public ApiResponse<List<BusDto>> getAllBuses() {
        return ApiResponse.ok(busService.getAllBuses());
    }

    @GetMapping("/{busId}")
    public ApiResponse<BusDto> getBusById(@PathVariable Long busId) {
        return ApiResponse.ok(busService.getBusById(busId));
    }
}
