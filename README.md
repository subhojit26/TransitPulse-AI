# TransitPulse AI — Public Bus Tracking System (Part 1)

A multi-module Spring Boot application for real-time public bus tracking in Nagpur city, featuring PostGIS geospatial queries and Redis-backed live bus caching.

## Architecture

```
bus-tracking-system/
├── common/               ← Shared entities, DTOs, exception handling
├── bus-service/           ← Routes, stops, buses CRUD (port 8081)
├── occupancy-service/     ← Conductor occupancy updates + Redis cache (port 8082)
├── commuter-service/      ← Commuter-facing read APIs + PostGIS (port 8083)
├── docker-compose.yml     ← PostgreSQL 15 + PostGIS, Redis 7
└── pom.xml                ← Parent Maven POM
```

## Tech Stack

| Component       | Technology                     |
|-----------------|--------------------------------|
| Language        | Java 17                        |
| Framework       | Spring Boot 3.2                |
| ORM             | Spring Data JPA + Hibernate    |
| Database        | PostgreSQL 15 + PostGIS        |
| Cache           | Redis 7 (Spring Data Redis)    |
| Build           | Maven multi-module             |
| Migrations      | Flyway                         |
| Utilities       | Lombok, MapStruct              |

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose

## Quick Start

### 1. Start Infrastructure

```bash
docker-compose up -d
```

This starts:
- **PostgreSQL 15 + PostGIS** on port `5432`
- **Redis 7** on port `6379`

### 2. Build the Project

```bash
mvn clean install
```

### 3. Run Services

Open three separate terminals:

```bash
# Terminal 1 — Bus Service (runs Flyway migrations)
cd bus-service
mvn spring-boot:run

# Terminal 2 — Occupancy Service
cd occupancy-service
mvn spring-boot:run

# Terminal 3 — Commuter Service
cd commuter-service
mvn spring-boot:run
```

## API Reference & Curl Examples

### Bus Service (port 8081)

#### List all routes
```bash
curl -s http://localhost:8081/api/routes | jq
```

#### Get stops for route 1
```bash
curl -s http://localhost:8081/api/routes/1/stops | jq
```

#### List all buses
```bash
curl -s http://localhost:8081/api/buses | jq
```

#### Get single bus details
```bash
curl -s http://localhost:8081/api/buses/1 | jq
```

---

### Occupancy Service (port 8082)

#### Update occupancy (conductor action)
```bash
curl -s -X POST http://localhost:8082/api/occupancy/update \
  -H "Content-Type: application/json" \
  -d '{
    "busId": 1,
    "occupancyPercent": 72,
    "conductorId": "COND-001",
    "latitude": 21.1396,
    "longitude": 79.0787
  }' | jq
```

#### Get current occupancy for bus 1
```bash
curl -s http://localhost:8082/api/occupancy/1/current | jq
```

---

### Commuter Service (port 8083)

#### Find nearby stops (within 500m of Sitabuldi)
```bash
curl -s "http://localhost:8083/api/commuter/stops/nearby?lat=21.1458&lng=79.0882&radiusMeters=500" | jq
```

#### Find nearby stops (wider radius — 2km)
```bash
curl -s "http://localhost:8083/api/commuter/stops/nearby?lat=21.1400&lng=79.0700&radiusMeters=2000" | jq
```

#### Get incoming buses for stop 1 (Sitabuldi Bus Stand)
```bash
curl -s http://localhost:8083/api/commuter/stops/1/incoming-buses | jq
```

#### Get incoming buses for stop 9 (Dharampeth Bus Stop)
```bash
curl -s http://localhost:8083/api/commuter/stops/9/incoming-buses | jq
```

## Seed Data

### Routes
| ID | Route Number | Route Name                      |
|----|-------------|----------------------------------|
| 1  | 10          | Sitabuldi - Automotive Square    |
| 2  | 47B         | Dharampeth - Hingna T-Point      |

### Route 10 Stops (Sitabuldi - Automotive Square)
| Seq | Stop Name              | Latitude  | Longitude |
|-----|------------------------|-----------|-----------|
| 1   | Sitabuldi Bus Stand    | 21.1458   | 79.0882   |
| 2   | Variety Square         | 21.1396   | 79.0787   |
| 3   | Law College Square     | 21.1370   | 79.0675   |
| 4   | Shankar Nagar Square   | 21.1345   | 79.0572   |
| 5   | Pratap Nagar Square    | 21.1289   | 79.0480   |
| 6   | Trimurti Nagar         | 21.1225   | 79.0385   |
| 7   | Manewada Square        | 21.1170   | 79.0290   |
| 8   | Automotive Square      | 21.1102   | 79.0195   |

### Route 47B Stops (Dharampeth - Hingna T-Point)
| Seq | Stop Name               | Latitude  | Longitude |
|-----|-------------------------|-----------|-----------|
| 1   | Dharampeth Bus Stop     | 21.1510   | 79.0780   |
| 2   | Telephone Exchange Sq.  | 21.1475   | 79.0710   |
| 3   | Laxmi Nagar Square      | 21.1430   | 79.0630   |
| 4   | Panchsheel Square       | 21.1380   | 79.0545   |
| 5   | Nandanvan Colony        | 21.1320   | 79.0450   |
| 6   | Wadi Bus Stop           | 21.1250   | 79.0350   |
| 7   | Hingna T-Point          | 21.1180   | 79.0255   |

### Buses
| Bus Number  | Route | Capacity | Status   |
|-------------|-------|----------|----------|
| BUS-10-01   | 10    | 60       | ACTIVE   |
| BUS-10-02   | 10    | 55       | ACTIVE   |
| BUS-10-03   | 10    | 60       | INACTIVE |
| BUS-47B-01  | 47B   | 50       | ACTIVE   |
| BUS-47B-02  | 47B   | 60       | ACTIVE   |
| BUS-47B-03  | 47B   | 55       | ACTIVE   |

## Redis Cache Design

```
Key Pattern:   bus:live:{busId}
Value:         BusLocationEvent JSON
TTL:           30 seconds (auto-expire if bus goes offline)
```

### Crowd Labels
| Occupancy %  | Label    |
|-------------|----------|
| 0-25        | EMPTY    |
| 26-50       | LOW      |
| 51-75       | MODERATE |
| 76-90       | HIGH     |
| 91-100      | FULL     |

## Environment Variables

| Variable              | Default       | Description               |
|-----------------------|---------------|---------------------------|
| DB_HOST               | localhost     | PostgreSQL host            |
| DB_PORT               | 5432          | PostgreSQL port            |
| DB_NAME               | transitpulse  | Database name              |
| DB_USERNAME           | postgres      | Database username          |
| DB_PASSWORD           | postgres      | Database password          |
| REDIS_HOST            | localhost     | Redis host                 |
| REDIS_PORT            | 6379          | Redis port                 |
| BUS_SERVICE_PORT      | 8081          | Bus service port           |
| OCCUPANCY_SERVICE_PORT| 8082          | Occupancy service port     |
| COMMUTER_SERVICE_PORT | 8083          | Commuter service port      |

## API Response Format

All APIs return a consistent wrapper:

```json
{
  "success": true,
  "message": "Success",
  "data": { ... },
  "timestamp": "2026-03-31T10:30:00"
}
```

Error responses:

```json
{
  "success": false,
  "message": "Bus not found with id: 99",
  "timestamp": "2026-03-31T10:30:00"
}
```

---

## Roadmap

- Part 2 → Kafka + GPS Simulator + Kafka Streams
- Part 3 → AI Prediction + WebSocket Alerts
- Part 4 → Frontend + Docker + Kubernetes

