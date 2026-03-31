package com.transitpulse.common.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomingBusDto {
    private Long busId;
    private String busNumber;
    private String routeNumber;
    private String eta;
    private Integer occupancyPercent;
    private String crowdLabel;
    private String status;
}
