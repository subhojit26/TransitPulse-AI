package com.transitpulse.common.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OccupancyUpdateRequest {

    @NotNull(message = "busId is required")
    private Long busId;

    @NotNull(message = "occupancyPercent is required")
    @Min(value = 0, message = "occupancyPercent must be >= 0")
    @Max(value = 100, message = "occupancyPercent must be <= 100")
    private Integer occupancyPercent;

    @NotBlank(message = "conductorId is required")
    private String conductorId;

    private Double latitude;
    private Double longitude;
}
