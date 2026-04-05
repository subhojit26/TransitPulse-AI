package com.transitpulse.notificationservice.repository;

import com.transitpulse.common.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findTop10ByStopIdOrderByCreatedAtDesc(Long stopId);
}
