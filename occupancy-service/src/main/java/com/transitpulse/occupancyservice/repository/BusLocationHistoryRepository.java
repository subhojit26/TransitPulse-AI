package com.transitpulse.occupancyservice.repository;

import com.transitpulse.common.entity.BusLocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusLocationHistoryRepository extends JpaRepository<BusLocationHistory, Long> {
    Optional<BusLocationHistory> findTopByBusIdOrderByRecordedAtDesc(Long busId);
}
