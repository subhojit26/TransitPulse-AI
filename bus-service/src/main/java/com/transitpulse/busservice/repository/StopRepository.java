package com.transitpulse.busservice.repository;

import com.transitpulse.common.entity.Stop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StopRepository extends JpaRepository<Stop, Long> {
    List<Stop> findByRouteIdOrderByStopSequence(Long routeId);
}
