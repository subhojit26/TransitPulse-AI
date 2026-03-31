package com.transitpulse.common.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusDto {
    private Long id;
    private String busNumber;
    private Long routeId;
    private String routeNumber;
    private String routeName;
    private Integer capacity;
    private String status;
}
