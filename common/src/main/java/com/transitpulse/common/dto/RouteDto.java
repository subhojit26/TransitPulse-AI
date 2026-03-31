package com.transitpulse.common.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteDto {
    private Long id;
    private String routeNumber;
    private String routeName;
    private LocalDateTime createdAt;
    private int stopCount;
    private int busCount;
}
