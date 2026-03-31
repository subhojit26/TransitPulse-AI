package com.transitpulse.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusLocationEvent implements Serializable {
    private Long busId;
    private String busNumber;
    private Double latitude;
    private Double longitude;
    private Integer occupancyPercent;
    private String crowdLabel;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String conductorId;

    private LocalDateTime recordedAt;
}
