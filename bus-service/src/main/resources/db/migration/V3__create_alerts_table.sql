-- V3: Create alerts table
CREATE TABLE IF NOT EXISTS alerts (
    id BIGSERIAL PRIMARY KEY,
    stop_id BIGINT,
    bus_id BIGINT,
    alert_type VARCHAR(30) NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    is_read BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_alerts_stop_id ON alerts(stop_id);
CREATE INDEX idx_alerts_bus_id ON alerts(bus_id);
CREATE INDEX idx_alerts_created_at ON alerts(created_at DESC);
CREATE INDEX idx_alerts_type ON alerts(alert_type);
