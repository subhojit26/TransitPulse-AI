package com.transitpulse.commuterservice.repository;

import com.transitpulse.common.entity.Stop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StopRepository extends JpaRepository<Stop, Long> {

    @Query(value = """
            SELECT s.id, s.stop_name, s.latitude, s.longitude,
                   s.route_id, s.stop_sequence,
                   r.route_number, r.route_name,
                   ST_Distance(
                       s.location,
                       CAST(ST_MakePoint(:lng, :lat) AS geography)
                   ) as distance_meters
            FROM stops s
            JOIN routes r ON r.id = s.route_id
            WHERE ST_DWithin(
                s.location,
                CAST(ST_MakePoint(:lng, :lat) AS geography),
                :radiusMeters
            )
            ORDER BY distance_meters
            """, nativeQuery = true)
    List<Object[]> findNearbyStops(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusMeters") double radiusMeters);

    @Query(value = """
            SELECT s.id, s.stop_name, s.latitude, s.longitude,
                   s.route_id, s.stop_sequence,
                   r.route_number, r.route_name,
                   ST_Distance(
                       s.location,
                       CAST(ST_MakePoint(:lng, :lat) AS geography)
                   ) as distance_meters
            FROM stops s
            JOIN routes r ON r.id = s.route_id
            ORDER BY distance_meters
            """, nativeQuery = true)
    List<Object[]> findAllStopsSortedByDistance(
            @Param("lat") double lat,
            @Param("lng") double lng);
}
