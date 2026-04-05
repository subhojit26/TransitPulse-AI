package com.transitpulse.common.dto;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiPrediction implements Serializable {
    private Double adjustedEtaMinutes;
    private Double confidence;
    private String reason;
}
