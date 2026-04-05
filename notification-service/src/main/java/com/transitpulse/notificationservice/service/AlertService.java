package com.transitpulse.notificationservice.service;

import com.transitpulse.common.dto.AlertDto;
import com.transitpulse.common.entity.Alert;
import com.transitpulse.notificationservice.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;

    @Transactional
    public Alert saveAlert(Long stopId, Long busId, String alertType, String message) {
        Alert alert = Alert.builder()
                .stopId(stopId)
                .busId(busId)
                .alertType(alertType)
                .message(message)
                .isRead(false)
                .build();
        Alert saved = alertRepository.save(alert);
        log.info("Alert saved: [{}] {}", alertType, message);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AlertDto> getRecentAlerts(Long stopId) {
        return alertRepository.findTop10ByStopIdOrderByCreatedAtDesc(stopId)
                .stream()
                .map(a -> AlertDto.builder()
                        .id(a.getId())
                        .stopId(a.getStopId())
                        .busId(a.getBusId())
                        .alertType(a.getAlertType())
                        .message(a.getMessage())
                        .createdAt(a.getCreatedAt())
                        .isRead(a.getIsRead())
                        .build())
                .toList();
    }
}
