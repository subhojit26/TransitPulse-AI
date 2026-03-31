package com.transitpulse.occupancyservice.controller;

import com.transitpulse.common.dto.ApiResponse;
import com.transitpulse.common.dto.OccupancyDto;
import com.transitpulse.common.dto.OccupancyUpdateRequest;
import com.transitpulse.occupancyservice.service.OccupancyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/occupancy")
@RequiredArgsConstructor
public class OccupancyController {

    private final OccupancyService occupancyService;

    @PostMapping("/update")
    public ApiResponse<OccupancyDto> updateOccupancy(
            @Valid @RequestBody OccupancyUpdateRequest request) {
        return ApiResponse.ok(
                occupancyService.updateOccupancy(request),
                "Occupancy updated successfully"
        );
    }

    @GetMapping("/{busId}/current")
    public ApiResponse<OccupancyDto> getCurrentOccupancy(@PathVariable Long busId) {
        return ApiResponse.ok(occupancyService.getCurrentOccupancy(busId));
    }
}
