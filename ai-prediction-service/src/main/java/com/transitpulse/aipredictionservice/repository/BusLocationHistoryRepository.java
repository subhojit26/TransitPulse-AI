package com.transitpulse.aipredictionservice.repository;

import com.transitpulse.common.entity.BusLocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BusLocationHistoryRepository extends JpaRepository<BusLocationHistory, Long> {

    @Query("""
            SELECT h FROM BusLocationHistory h
            WHERE h.bus.id = :busId
            AND h.recordedAt >= :since
            AND FUNCTION('EXTRACT', HOUR FROM h.recordedAt) BETWEEN :hourStart AND :hourEnd
            ORDER BY h.recordedAt DESC
            """)
    List<BusLocationHistory> findHistoricalArrivals(
            @Param("busId") Long busId,
            @Param("since") LocalDateTime since,
            @Param("hourStart") int hourStart,
            @Param("hourEnd") int hourEnd);
}
