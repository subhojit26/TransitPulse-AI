package com.transitpulse.gpssimulator.model;

import lombok.Data;

import java.util.List;

@Data
public class SimulatedBus {

    private final Long busId;
    private final String busNumber;
    private final Long routeId;
    private final List<double[]> routeCoordinates; // [lat, lng] per stop

    private double currentLat;
    private double currentLng;
    private int currentStopIndex; // index of the NEXT stop the bus is heading toward
    private int occupancyPercent;
    private String status; // ACTIVE, BREAKDOWN

    public SimulatedBus(Long busId, String busNumber, Long routeId,
                        List<double[]> routeCoordinates) {
        this.busId = busId;
        this.busNumber = busNumber;
        this.routeId = routeId;
        this.routeCoordinates = routeCoordinates;
        this.currentLat = routeCoordinates.get(0)[0];
        this.currentLng = routeCoordinates.get(0)[1];
        this.currentStopIndex = 1; // heading toward second stop
        this.occupancyPercent = 30;
        this.status = "ACTIVE";
    }

    public double[] getNextStopCoords() {
        return routeCoordinates.get(currentStopIndex);
    }

    public boolean hasReachedNextStop(double threshold) {
        double[] target = getNextStopCoords();
        double dist = Math.sqrt(
                Math.pow(currentLat - target[0], 2) +
                Math.pow(currentLng - target[1], 2)
        );
        return dist < threshold;
    }

    public void advanceToNextStop() {
        currentStopIndex++;
        if (currentStopIndex >= routeCoordinates.size()) {
            // Loop back to start
            currentStopIndex = 1;
            currentLat = routeCoordinates.get(0)[0];
            currentLng = routeCoordinates.get(0)[1];
        }
    }
}
