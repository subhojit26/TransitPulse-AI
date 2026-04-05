package com.transitpulse.aipredictionservice.controller;

import com.transitpulse.aipredictionservice.service.AiEtaPredictionService;
import com.transitpulse.common.dto.AiPrediction;
import com.transitpulse.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiPredictionController {

    private final AiEtaPredictionService predictionService;

    @GetMapping("/predict-eta")
    public ApiResponse<AiPrediction> predictEta(
            @RequestParam Long busId,
            @RequestParam Long stopId,
            @RequestParam Double streamEta) {

        LocalDateTime now = LocalDateTime.now();
        DayOfWeek dow = now.getDayOfWeek();
        int hourOfDay = now.getHour();

        AiPrediction prediction = predictionService.predict(
                busId, stopId, streamEta, dow.getValue(), hourOfDay);
        return ApiResponse.ok(prediction);
    }
}
