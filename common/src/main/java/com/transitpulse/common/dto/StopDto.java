package com.transitpulse.common.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StopDto {
    private Long id;
    private Long routeId;
    private String stopName;
    private Integer stopSequence;
    private Double latitude;
    private Double longitude;
}
