package com.transitpulse.commuterservice.repository;

import com.transitpulse.common.entity.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusRepository extends JpaRepository<Bus, Long> {

    @Query("SELECT b FROM Bus b JOIN FETCH b.route WHERE b.route.id = :routeId AND b.status = 'ACTIVE'")
    List<Bus> findActiveByRouteId(@Param("routeId") Long routeId);
}
