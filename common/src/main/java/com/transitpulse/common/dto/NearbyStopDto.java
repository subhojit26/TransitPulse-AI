package com.transitpulse.common.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyStopDto {
    private Long stopId;
    private String stopName;
    private Double latitude;
    private Double longitude;
    private Double distanceMeters;
    private Long routeId;
    private String routeNumber;
    private String routeName;
}
