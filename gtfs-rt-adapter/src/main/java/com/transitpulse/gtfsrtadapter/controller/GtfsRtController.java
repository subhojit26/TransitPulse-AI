package com.transitpulse.gtfsrtadapter.controller;

import com.transitpulse.gtfsrtadapter.service.GtfsRtFetcherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/gtfs")
@RequiredArgsConstructor
public class GtfsRtController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "gtfs-rt-adapter");
    }
}
