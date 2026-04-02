package com.transitpulse.streamprocessor.topology;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.transitpulse.common.dto.BusAlertEvent;
import com.transitpulse.common.dto.BusEtaEvent;
import com.transitpulse.common.dto.BusLocationEvent;
import com.transitpulse.common.util.CrowdLabelUtil;
import com.transitpulse.streamprocessor.model.LocationAggregate;
import com.transitpulse.streamprocessor.util.HaversineUtil;
import com.transitpulse.streamprocessor.util.StopDataProvider;
import com.transitpulse.streamprocessor.util.StopDataProvider.StopInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EtaTopologyBuilder {

    private static final String LOCATION_TOPIC = "bus-location-events";
    private static final String ETA_TOPIC = "bus-eta-events";
    private static final String ALERT_TOPIC = "bus-alert-events";

    private final ObjectMapper streamsObjectMapper;

    @Autowired
    public void buildPipeline(StreamsBuilder builder) {
        var locationSerde = jsonSerde(BusLocationEvent.class);
        var aggregateSerde = jsonSerde(LocationAggregate.class);
        var etaSerde = jsonSerde(BusEtaEvent.class);
        var alertSerde = jsonSerde(BusAlertEvent.class);

        // ── Stream 1: bus-location-events → compute ETA → bus-eta-events ──

        KStream<String, BusLocationEvent> locationStream = builder.stream(
                LOCATION_TOPIC,
                Consumed.with(Serdes.String(), locationSerde)
        );

        locationStream
                .groupByKey(Grouped.with(Serdes.String(), locationSerde))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(30)))
                .aggregate(
                        LocationAggregate::new,
                        (busIdKey, event, aggregate) -> aggregate.update(
                                busIdKey,
                                event.getBusNumber(),
                                event.getRouteId(),
                                event.getCurrentStopIndex(),
                                event.getOccupancyPercent(),
                                event.getLatitude(),
                                event.getLongitude(),
                                event.getRecordedAt() != null
                                        ? event.getRecordedAt().toInstant(ZoneOffset.UTC).toEpochMilli()
                                        : System.currentTimeMillis()
                        ),
                        Materialized.<String, LocationAggregate, WindowStore<Bytes, byte[]>>as("location-aggregates")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(aggregateSerde)
                )
                .toStream()
                .filter((windowedKey, agg) -> agg != null && agg.hasEnoughData())
                .map((windowedKey, agg) -> {
                    BusEtaEvent eta = computeEta(agg);
                    return KeyValue.pair(windowedKey.key(), eta);
                })
                .filter((key, eta) -> eta != null)
                .to(ETA_TOPIC, Produced.with(Serdes.String(), etaSerde));

        // ── Stream 2: bus-eta-events → generate alerts → bus-alert-events ──

        KStream<String, BusEtaEvent> etaStream = builder.stream(
                ETA_TOPIC,
                Consumed.with(Serdes.String(), etaSerde)
        );

        etaStream
                .flatMap((busId, eta) -> {
                    List<KeyValue<String, BusAlertEvent>> alerts = new ArrayList<>();
                    if (eta == null || eta.getRouteId() == null) return alerts;

                    // Get upcoming stops for this bus's route
                    int nextStopIdx = eta.getNextStopId() != null
                            ? findStopIndex(eta.getRouteId(), eta.getNextStopId())
                            : 0;

                    List<StopInfo> upcoming = StopDataProvider.getUpcomingStops(
                            eta.getRouteId(), nextStopIdx);

                    double cumulativeDistance = 0.0;
                    double speed = eta.getAverageSpeedKmh() != null && eta.getAverageSpeedKmh() > 0
                            ? eta.getAverageSpeedKmh() : 20.0; // default 20 km/h

                    StopInfo prevStop = null;
                    for (StopInfo stop : upcoming) {
                        if (prevStop != null) {
                            cumulativeDistance += HaversineUtil.distanceKm(
                                    prevStop.latitude(), prevStop.longitude(),
                                    stop.latitude(), stop.longitude());
                        }

                        double stopEta = (cumulativeDistance == 0.0)
                                ? (eta.getEtaMinutes() != null ? eta.getEtaMinutes() : 0.0)
                                : (cumulativeDistance / speed) * 60.0;

                        BusAlertEvent alert = BusAlertEvent.builder()
                                .stopId(stop.stopId())
                                .stopName(stop.stopName())
                                .busId(eta.getBusId())
                                .busNumber(eta.getBusNumber())
                                .etaMinutes(Math.round(stopEta * 10.0) / 10.0)
                                .occupancyPercent(eta.getOccupancyPercent())
                                .crowdLabel(CrowdLabelUtil.fromOccupancyEmoji(eta.getOccupancyPercent()))
                                .timestamp(LocalDateTime.now())
                                .build();

                        // Key by stopId for the alert topic
                        alerts.add(KeyValue.pair(String.valueOf(stop.stopId()), alert));
                        prevStop = stop;
                    }

                    return alerts;
                })
                .to(ALERT_TOPIC, Produced.with(Serdes.String(), alertSerde));

        log.info("Kafka Streams ETA + Alert topology configured");
    }

    private BusEtaEvent computeEta(LocationAggregate agg) {
        double distKm = HaversineUtil.distanceKm(
                agg.getPrevLat(), agg.getPrevLng(),
                agg.getCurrLat(), agg.getCurrLng()
        );

        double timeSec = (agg.getCurrTimestamp() - agg.getPrevTimestamp()) / 1000.0;
        if (timeSec <= 0) return null;

        double speedKmh = (distKm / timeSec) * 3600.0;
        if (speedKmh < 0.5) speedKmh = 15.0; // default for stationary or near-zero

        // Find next stop
        Long routeId = agg.getRouteId();
        Integer stopIndex = agg.getCurrentStopIndex();
        StopInfo nextStop = (routeId != null && stopIndex != null)
                ? StopDataProvider.getStop(routeId, stopIndex)
                : null;

        double remainingKm = 0.0;
        Long nextStopId = null;
        String nextStopName = "Unknown";
        if (nextStop != null) {
            remainingKm = HaversineUtil.distanceKm(
                    agg.getCurrLat(), agg.getCurrLng(),
                    nextStop.latitude(), nextStop.longitude()
            );
            nextStopId = nextStop.stopId();
            nextStopName = nextStop.stopName();
        }

        double etaMinutes = (remainingKm / speedKmh) * 60.0;
        etaMinutes = Math.round(etaMinutes * 10.0) / 10.0;

        return BusEtaEvent.builder()
                .busId(Long.parseLong(agg.getBusId()))
                .busNumber(agg.getBusNumber())
                .routeId(routeId)
                .nextStopId(nextStopId)
                .nextStopName(nextStopName)
                .etaMinutes(etaMinutes)
                .averageSpeedKmh(Math.round(speedKmh * 100.0) / 100.0)
                .occupancyPercent(agg.getOccupancyPercent())
                .crowdLabel(CrowdLabelUtil.fromOccupancyEmoji(agg.getOccupancyPercent()))
                .timestamp(LocalDateTime.now())
                .build();
    }

    private int findStopIndex(Long routeId, Long stopId) {
        List<StopInfo> stops = StopDataProvider.getStopsForRoute(routeId);
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).stopId().equals(stopId)) return i;
        }
        return 0;
    }

    private <T> org.apache.kafka.common.serialization.Serde<T> jsonSerde(Class<T> clazz) {
        var serializer = new org.apache.kafka.common.serialization.Serializer<T>() {
            @Override
            public byte[] serialize(String topic, T data) {
                if (data == null) return null;
                try {
                    return streamsObjectMapper.writeValueAsBytes(data);
                } catch (Exception e) {
                    throw new RuntimeException("Error serializing " + clazz.getSimpleName(), e);
                }
            }
        };
        var deserializer = new org.apache.kafka.common.serialization.Deserializer<T>() {
            @Override
            public T deserialize(String topic, byte[] data) {
                if (data == null) return null;
                try {
                    return streamsObjectMapper.readValue(data, clazz);
                } catch (Exception e) {
                    log.warn("Error deserializing {} from topic {}: {}", clazz.getSimpleName(), topic, e.getMessage());
                    return null;
                }
            }
        };
        return Serdes.serdeFrom(serializer, deserializer);
    }
}
