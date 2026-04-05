package com.transitpulse.gtfsrtadapter.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Maps an MTA GTFS vehicle to our internal bus/route IDs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GtfsBusMapping {
    private String gtfsRouteId;     // e.g., "MTA NYCT_M15"
    private String gtfsTripPattern; // prefix match for trip IDs
    private Long internalBusId;
    private Long internalRouteId;
    private String busNumber;       // display name, e.g., "NYC-M15-01"
}
