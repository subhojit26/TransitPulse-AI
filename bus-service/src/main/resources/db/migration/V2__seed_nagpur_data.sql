-- V2: Seed Nagpur city bus routes, stops, and buses

-- Route 1: Sitabuldi to Automotive Square (Route 10)
INSERT INTO routes (id, route_number, route_name) VALUES
(1, '10', 'Sitabuldi - Automotive Square');

-- Route 2: Dharampeth to Hingna (Route 47B)
INSERT INTO routes (id, route_number, route_name) VALUES
(2, '47B', 'Dharampeth - Hingna T-Point');

-- Stops for Route 10 (Sitabuldi - Automotive Square)
-- Real Nagpur coordinates
INSERT INTO stops (route_id, stop_name, stop_sequence, latitude, longitude, location) VALUES
(1, 'Sitabuldi Bus Stand',   1, 21.1458, 79.0882, ST_MakePoint(79.0882, 21.1458)::geography),
(1, 'Variety Square',        2, 21.1396, 79.0787, ST_MakePoint(79.0787, 21.1396)::geography),
(1, 'Law College Square',    3, 21.1370, 79.0675, ST_MakePoint(79.0675, 21.1370)::geography),
(1, 'Shankar Nagar Square',  4, 21.1345, 79.0572, ST_MakePoint(79.0572, 21.1345)::geography),
(1, 'Pratap Nagar Square',   5, 21.1289, 79.0480, ST_MakePoint(79.0480, 21.1289)::geography),
(1, 'Trimurti Nagar',        6, 21.1225, 79.0385, ST_MakePoint(79.0385, 21.1225)::geography),
(1, 'Manewada Square',       7, 21.1170, 79.0290, ST_MakePoint(79.0290, 21.1170)::geography),
(1, 'Automotive Square',     8, 21.1102, 79.0195, ST_MakePoint(79.0195, 21.1102)::geography);

-- Stops for Route 47B (Dharampeth - Hingna T-Point)
INSERT INTO stops (route_id, stop_name, stop_sequence, latitude, longitude, location) VALUES
(2, 'Dharampeth Bus Stop',    1, 21.1510, 79.0780, ST_MakePoint(79.0780, 21.1510)::geography),
(2, 'Telephone Exchange Sq.', 2, 21.1475, 79.0710, ST_MakePoint(79.0710, 21.1475)::geography),
(2, 'Laxmi Nagar Square',     3, 21.1430, 79.0630, ST_MakePoint(79.0630, 21.1430)::geography),
(2, 'Panchsheel Square',      4, 21.1380, 79.0545, ST_MakePoint(79.0545, 21.1380)::geography),
(2, 'Nandanvan Colony',       5, 21.1320, 79.0450, ST_MakePoint(79.0450, 21.1320)::geography),
(2, 'Wadi Bus Stop',          6, 21.1250, 79.0350, ST_MakePoint(79.0350, 21.1250)::geography),
(2, 'Hingna T-Point',         7, 21.1180, 79.0255, ST_MakePoint(79.0255, 21.1180)::geography);

-- Buses for Route 10 (3 buses)
INSERT INTO buses (bus_number, route_id, capacity, status) VALUES
('BUS-10-01', 1, 60, 'ACTIVE'),
('BUS-10-02', 1, 55, 'ACTIVE'),
('BUS-10-03', 1, 60, 'INACTIVE');

-- Buses for Route 47B (3 buses)
INSERT INTO buses (bus_number, route_id, capacity, status) VALUES
('BUS-47B-01', 2, 50, 'ACTIVE'),
('BUS-47B-02', 2, 60, 'ACTIVE'),
('BUS-47B-03', 2, 55, 'ACTIVE');

-- Seed some initial location history
INSERT INTO bus_location_history (bus_id, latitude, longitude, occupancy_percent) VALUES
(1, 21.1458, 79.0882, 45),
(2, 21.1370, 79.0675, 72),
(4, 21.1510, 79.0780, 30),
(5, 21.1380, 79.0545, 88);
