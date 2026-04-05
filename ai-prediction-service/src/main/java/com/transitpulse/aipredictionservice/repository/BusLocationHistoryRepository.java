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

    @Query(value = """
            SELECT * FROM bus_location_history h
            WHERE h.bus_id = :busId
            AND h.recorded_at >= :since
            AND EXTRACT(HOUR FROM h.recorded_at) BETWEEN :hourStart AND :hourEnd
            ORDER BY h.recorded_at DESC
            """, nativeQuery = true)
    List<BusLocationHistory> findHistoricalArrivals(
            @Param("busId") Long busId,
            @Param("since") LocalDateTime since,
            @Param("hourStart") int hourStart,
            @Param("hourEnd") int hourEnd);
}
