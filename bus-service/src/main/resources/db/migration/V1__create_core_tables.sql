-- V1: Enable PostGIS and create core tables
CREATE EXTENSION IF NOT EXISTS postgis;

-- Bus routes
CREATE TABLE routes (
    id SERIAL PRIMARY KEY,
    route_number VARCHAR(10) NOT NULL,
    route_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Bus stops with PostGIS geography
CREATE TABLE stops (
    id SERIAL PRIMARY KEY,
    route_id INT NOT NULL REFERENCES routes(id),
    stop_name VARCHAR(100) NOT NULL,
    stop_sequence INT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    location GEOGRAPHY(POINT, 4326)
);

-- Buses
CREATE TABLE buses (
    id SERIAL PRIMARY KEY,
    bus_number VARCHAR(20) NOT NULL UNIQUE,
    route_id INT NOT NULL REFERENCES routes(id),
    capacity INT DEFAULT 60,
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

-- Location history (append-only)
CREATE TABLE bus_location_history (
    id BIGSERIAL PRIMARY KEY,
    bus_id INT NOT NULL REFERENCES buses(id),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    occupancy_percent INT,
    recorded_at TIMESTAMP DEFAULT NOW()
);

-- PostGIS GIST index for fast geo queries
CREATE INDEX idx_stops_location ON stops USING GIST(location);

-- Additional indexes for common queries
CREATE INDEX idx_stops_route_id ON stops(route_id);
CREATE INDEX idx_buses_route_id ON buses(route_id);
CREATE INDEX idx_bus_location_history_bus_id ON bus_location_history(bus_id);
CREATE INDEX idx_bus_location_history_recorded_at ON bus_location_history(recorded_at DESC);
