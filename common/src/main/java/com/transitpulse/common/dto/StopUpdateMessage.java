package com.transitpulse.common.dto;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StopUpdateMessage implements Serializable {
    private Long stopId;
    private String stopName;
    private List<IncomingBusLive> buses;
    private List<AlertDto> alerts;
    private LocalDateTime timestamp;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IncomingBusLive implements Serializable {
        private String busNumber;
        private String routeNumber;
        private Double streamEtaMinutes;
        private Double aiAdjustedEtaMinutes;
        private Double aiConfidence;
        private String aiReason;
        private Integer occupancyPercent;
        private String crowdLabel;
        private String status;
    }
}
