package com.transitpulse.common.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OccupancyDto {
    private Long busId;
    private String busNumber;
    private Integer occupancyPercent;
    private String crowdLabel;
    private String conductorId;
    private LocalDateTime recordedAt;
}
