-- V2: Seed Pune, Mumbai bus routes, stops, and buses
-- Also includes placeholder NYC MTA routes for GTFS-RT integration

-- ===== PUNE ROUTES =====

-- Route 1: Swargate → Shivajinagar (via JM Road)
INSERT INTO routes (id, route_number, route_name) VALUES
(1, 'P1', 'Swargate - Shivajinagar');

-- Route 2: Katraj → Nigdi (via NH48)
INSERT INTO routes (id, route_number, route_name) VALUES
(2, 'P2', 'Katraj - Nigdi');

-- ===== MUMBAI ROUTES =====

-- Route 3: CST → Andheri (via Western Express)
INSERT INTO routes (id, route_number, route_name) VALUES
(3, 'M1', 'CST - Andheri');

-- Route 4: Dadar → Powai
INSERT INTO routes (id, route_number, route_name) VALUES
(4, 'M2', 'Dadar - Powai');

-- ===== NYC MTA ROUTES (populated live by gtfs-rt-adapter) =====
INSERT INTO routes (id, route_number, route_name) VALUES
(5, 'NYC-M15', 'NYC MTA - 1st/2nd Ave (M15)'),
(6, 'NYC-M34', 'NYC MTA - 34th St Crosstown (M34)');

-- ===== PUNE STOPS =====

-- Stops for Route P1 (Swargate → Shivajinagar) — real coordinates
INSERT INTO stops (route_id, stop_name, stop_sequence, latitude, longitude, location) VALUES
(1, 'Swargate Bus Stand',    1, 18.5018, 73.8636, ST_MakePoint(73.8636, 18.5018)::geography),
(1, 'Parvati Paytha',        2, 18.5065, 73.8610, ST_MakePoint(73.8610, 18.5065)::geography),
(1, 'Sarasbaug',             3, 18.5120, 73.8571, ST_MakePoint(73.8571, 18.5120)::geography),
(1, 'Shaniwar Wada',         4, 18.5162, 73.8530, ST_MakePoint(73.8530, 18.5162)::geography),
(1, 'Deccan Gymkhana',       5, 18.5200, 73.8490, ST_MakePoint(73.8490, 18.5200)::geography),
(1, 'Garware Bridge',        6, 18.5248, 73.8445, ST_MakePoint(73.8445, 18.5248)::geography),
(1, 'Agriculture College',   7, 18.5290, 73.8395, ST_MakePoint(73.8395, 18.5290)::geography),
(1, 'Shivajinagar Bus Stand',8, 18.5308, 73.8363, ST_MakePoint(73.8363, 18.5308)::geography);

-- Stops for Route P2 (Katraj → Nigdi)
INSERT INTO stops (route_id, stop_name, stop_sequence, latitude, longitude, location) VALUES
(2, 'Katraj Bus Stop',       1, 18.4578, 73.8660, ST_MakePoint(73.8660, 18.4578)::geography),
(2, 'Bharati Vidyapeeth',    2, 18.4730, 73.8665, ST_MakePoint(73.8665, 18.4730)::geography),
(2, 'Balaji Nagar',          3, 18.4885, 73.8650, ST_MakePoint(73.8650, 18.4885)::geography),
(2, 'Swargate (P2)',         4, 18.5018, 73.8636, ST_MakePoint(73.8636, 18.5018)::geography),
(2, 'Khadki Station',        5, 18.5355, 73.8410, ST_MakePoint(73.8410, 18.5355)::geography),
(2, 'Dapodi',                6, 18.5625, 73.8140, ST_MakePoint(73.8140, 18.5625)::geography),
(2, 'Pimpri Chinchwad',      7, 18.5930, 73.7950, ST_MakePoint(73.7950, 18.5930)::geography),
(2, 'Nigdi Bus Stop',        8, 18.6510, 73.7672, ST_MakePoint(73.7672, 18.6510)::geography);

-- ===== MUMBAI STOPS =====

-- Stops for Route M1 (CST → Andheri)
INSERT INTO stops (route_id, stop_name, stop_sequence, latitude, longitude, location) VALUES
(3, 'CST (Chhatrapati Shivaji Terminus)', 1, 18.9398, 72.8355, ST_MakePoint(72.8355, 18.9398)::geography),
(3, 'Marine Lines',          2, 18.9440, 72.8240, ST_MakePoint(72.8240, 18.9440)::geography),
(3, 'Mumbai Central',        3, 18.9540, 72.8160, ST_MakePoint(72.8160, 18.9540)::geography),
(3, 'Dadar TT',              4, 18.9710, 72.8200, ST_MakePoint(72.8200, 18.9710)::geography),
(3, 'Mahim Junction',        5, 18.9932, 72.8226, ST_MakePoint(72.8226, 18.9932)::geography),
(3, 'Bandra Station',        6, 19.0176, 72.8420, ST_MakePoint(72.8420, 19.0176)::geography),
(3, 'Vile Parle',            7, 19.0535, 72.8409, ST_MakePoint(72.8409, 19.0535)::geography),
(3, 'Andheri Station',       8, 19.1190, 72.8465, ST_MakePoint(72.8465, 19.1190)::geography);

-- Stops for Route M2 (Dadar → Powai)
INSERT INTO stops (route_id, stop_name, stop_sequence, latitude, longitude, location) VALUES
(4, 'Dadar TT (M2)',         1, 18.9710, 72.8200, ST_MakePoint(72.8200, 18.9710)::geography),
(4, 'Sion',                  2, 18.9760, 72.8340, ST_MakePoint(72.8340, 18.9760)::geography),
(4, 'Chunabhatti',           3, 18.9832, 72.8420, ST_MakePoint(72.8420, 18.9832)::geography),
(4, 'Kurla Station',         4, 19.0022, 72.8527, ST_MakePoint(72.8527, 19.0022)::geography),
(4, 'Ghatkopar',             5, 19.0544, 72.8688, ST_MakePoint(72.8688, 19.0544)::geography),
(4, 'Vikhroli',              6, 19.0640, 72.8770, ST_MakePoint(72.8770, 19.0640)::geography),
(4, 'Kanjurmarg',            7, 19.0750, 72.8880, ST_MakePoint(72.8880, 19.0750)::geography),
(4, 'Powai (Hiranandani)',   8, 19.0760, 72.9050, ST_MakePoint(72.9050, 19.0760)::geography);

-- ===== NYC STOPS (key stops for visualization) =====

INSERT INTO stops (route_id, stop_name, stop_sequence, latitude, longitude, location) VALUES
(5, 'South Ferry',           1, 40.7013, -74.0131, ST_MakePoint(-74.0131, 40.7013)::geography),
(5, 'City Hall',             2, 40.7128, -74.0060, ST_MakePoint(-74.0060, 40.7128)::geography),
(5, '23rd St & 1st Ave',     3, 40.7359, -73.9781, ST_MakePoint(-73.9781, 40.7359)::geography),
(5, '42nd St & 1st Ave',     4, 40.7489, -73.9680, ST_MakePoint(-73.9680, 40.7489)::geography),
(5, '57th St & 1st Ave',     5, 40.7592, -73.9608, ST_MakePoint(-73.9608, 40.7592)::geography),
(5, '72nd St & 1st Ave',     6, 40.7684, -73.9554, ST_MakePoint(-73.9554, 40.7684)::geography),
(5, '96th St & 1st Ave',     7, 40.7847, -73.9470, ST_MakePoint(-73.9470, 40.7847)::geography),
(5, '125th St & 1st Ave',    8, 40.8048, -73.9349, ST_MakePoint(-73.9349, 40.8048)::geography);

INSERT INTO stops (route_id, stop_name, stop_sequence, latitude, longitude, location) VALUES
(6, '34th St & 1st Ave',     1, 40.7448, -73.9729, ST_MakePoint(-73.9729, 40.7448)::geography),
(6, '34th St & 3rd Ave',     2, 40.7467, -73.9790, ST_MakePoint(-73.9790, 40.7467)::geography),
(6, '34th St & 5th Ave',     3, 40.7488, -73.9857, ST_MakePoint(-73.9857, 40.7488)::geography),
(6, 'Penn Station (34th)',   4, 40.7500, -73.9920, ST_MakePoint(-73.9920, 40.7500)::geography),
(6, '34th St & 8th Ave',     5, 40.7520, -73.9947, ST_MakePoint(-73.9947, 40.7520)::geography),
(6, '34th St & 10th Ave',    6, 40.7539, -73.9995, ST_MakePoint(-73.9995, 40.7539)::geography);

-- ===== BUSES =====

-- Pune buses
INSERT INTO buses (id, bus_number, route_id, capacity, status) VALUES
(1, 'PNQ-P1-01', 1, 60, 'ACTIVE'),
(2, 'PNQ-P1-02', 1, 55, 'ACTIVE'),
(3, 'PNQ-P2-01', 2, 60, 'ACTIVE'),
(4, 'PNQ-P2-02', 2, 55, 'ACTIVE');

-- Mumbai buses
INSERT INTO buses (id, bus_number, route_id, capacity, status) VALUES
(5, 'MUM-M1-01', 3, 60, 'ACTIVE'),
(6, 'MUM-M1-02', 3, 55, 'ACTIVE'),
(7, 'MUM-M2-01', 4, 60, 'ACTIVE'),
(8, 'MUM-M2-02', 4, 55, 'ACTIVE');

-- NYC placeholder buses (real positions come from GTFS-RT adapter)
INSERT INTO buses (id, bus_number, route_id, capacity, status) VALUES
(100, 'NYC-M15-LIVE', 5, 80, 'ACTIVE'),
(101, 'NYC-M34-LIVE', 6, 80, 'ACTIVE');

-- Reset sequences
SELECT setval('routes_id_seq', (SELECT MAX(id) FROM routes));
SELECT setval('buses_id_seq', (SELECT MAX(id) FROM buses));
SELECT setval('stops_id_seq', (SELECT MAX(id) FROM stops));

-- Seed initial location history
INSERT INTO bus_location_history (bus_id, latitude, longitude, occupancy_percent) VALUES
(1, 18.5018, 73.8636, 45),
(2, 18.5162, 73.8530, 72),
(3, 18.4578, 73.8660, 30),
(5, 18.9398, 72.8355, 88),
(7, 18.9710, 72.8200, 55);
