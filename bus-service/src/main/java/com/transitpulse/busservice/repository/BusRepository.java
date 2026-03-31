package com.transitpulse.busservice.repository;

import com.transitpulse.common.entity.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusRepository extends JpaRepository<Bus, Long> {
    List<Bus> findByRouteId(Long routeId);

    @Query("SELECT b FROM Bus b JOIN FETCH b.route")
    List<Bus> findAllWithRoute();
}
