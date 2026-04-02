package com.transitpulse.common.dto;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusEtaEvent implements Serializable {
    private Long busId;
    private String busNumber;
    private Long routeId;
    private Long nextStopId;
    private String nextStopName;
    private Double etaMinutes;
    private Double averageSpeedKmh;
    private Integer occupancyPercent;
    private String crowdLabel;
    private LocalDateTime timestamp;
}
