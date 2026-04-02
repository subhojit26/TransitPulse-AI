package com.transitpulse.streamprocessor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationAggregate implements Serializable {
    private String busId;
    private String busNumber;
    private Long routeId;
    private Integer currentStopIndex;
    private Integer occupancyPercent;

    // Previous position
    private Double prevLat;
    private Double prevLng;
    private Long prevTimestamp; // epoch millis

    // Current position
    private Double currLat;
    private Double currLng;
    private Long currTimestamp; // epoch millis

    private int eventCount;

    public LocationAggregate update(String busId, String busNumber, Long routeId,
                                    Integer currentStopIndex, Integer occupancyPercent,
                                    double lat, double lng, long timestamp) {
        this.busId = busId;
        this.busNumber = busNumber;
        this.routeId = routeId;
        this.currentStopIndex = currentStopIndex;
        this.occupancyPercent = occupancyPercent;

        // Shift current → previous
        this.prevLat = this.currLat;
        this.prevLng = this.currLng;
        this.prevTimestamp = this.currTimestamp;

        // Set new current
        this.currLat = lat;
        this.currLng = lng;
        this.currTimestamp = timestamp;

        this.eventCount++;
        return this;
    }

    public boolean hasEnoughData() {
        return prevLat != null && currLat != null
                && prevTimestamp != null && currTimestamp != null
                && eventCount >= 2;
    }
}
