package com.transitpulse.common.dto;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusAlertEvent implements Serializable {
    private Long stopId;
    private String stopName;
    private Long busId;
    private String busNumber;
    private Double etaMinutes;
    private Integer occupancyPercent;
    private String crowdLabel;
    private LocalDateTime timestamp;
}
