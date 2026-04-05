package com.transitpulse.common.dto;

import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertDto implements Serializable {
    private Long id;
    private Long stopId;
    private Long busId;
    private String alertType;
    private String message;
    private LocalDateTime createdAt;
    private Boolean isRead;
}
