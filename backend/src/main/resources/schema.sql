CREATE EXTENSION IF NOT EXISTS postgis;

-- OSINT Sources
CREATE TABLE IF NOT EXISTS osint_sources (
    id                      BIGSERIAL PRIMARY KEY,
    name                    VARCHAR(255) UNIQUE NOT NULL,
    type                    VARCHAR(100),
    base_url                VARCHAR(512),
    api_key_ref             VARCHAR(255),
    active                  BOOLEAN DEFAULT TRUE,
    update_interval_minutes INTEGER DEFAULT 60,
    created_at              TIMESTAMP DEFAULT NOW(),
    updated_at              TIMESTAMP DEFAULT NOW()
);

-- IoT Devices
CREATE TABLE IF NOT EXISTS iot_devices (
    id                BIGSERIAL PRIMARY KEY,
    source_id         BIGINT REFERENCES osint_sources(id) ON DELETE SET NULL,
    ip_address        VARCHAR(45),
    port              INTEGER,
    protocol          VARCHAR(50),
    device_type       VARCHAR(100),
    manufacturer      VARCHAR(255),
    firmware_version  VARCHAR(255),
    city              VARCHAR(255) DEFAULT 'Almaty',
    latitude          DOUBLE PRECISION,
    longitude         DOUBLE PRECISION,
    raw_data          JSONB,
    discovered_at     TIMESTAMP DEFAULT NOW()
);

-- Vulnerabilities
CREATE TABLE IF NOT EXISTS vulnerabilities (
    id          BIGSERIAL PRIMARY KEY,
    device_id   BIGINT REFERENCES iot_devices(id) ON DELETE CASCADE,
    cve_id      VARCHAR(30),
    severity    VARCHAR(20),
    cvss_score  DOUBLE PRECISION,
    description TEXT,
    source_id   BIGINT REFERENCES osint_sources(id) ON DELETE SET NULL,
    detected_at TIMESTAMP DEFAULT NOW()
);

-- Scoring Results
CREATE TABLE IF NOT EXISTS scoring_results (
    id                  BIGSERIAL PRIMARY KEY,
    source_id           BIGINT REFERENCES osint_sources(id) ON DELETE CASCADE,
    reliability_score   DOUBLE PRECISION,
    timeliness_score    DOUBLE PRECISION,
    completeness_score  DOUBLE PRECISION,
    accessibility_score DOUBLE PRECISION,
    total_score         DOUBLE PRECISION,
    parameters          JSONB,
    calculated_at       TIMESTAMP DEFAULT NOW()
);

-- Security Events
CREATE TABLE IF NOT EXISTS security_events (
    id          BIGSERIAL PRIMARY KEY,
    device_id   BIGINT REFERENCES iot_devices(id) ON DELETE CASCADE,
    event_type  VARCHAR(50),
    severity    VARCHAR(20),
    description TEXT,
    source_id   BIGINT REFERENCES osint_sources(id) ON DELETE SET NULL,
    event_time  TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_devices_source     ON iot_devices(source_id);
CREATE INDEX IF NOT EXISTS idx_devices_city        ON iot_devices(city);
CREATE INDEX IF NOT EXISTS idx_vulns_device        ON vulnerabilities(device_id);
CREATE INDEX IF NOT EXISTS idx_vulns_severity      ON vulnerabilities(severity);
CREATE INDEX IF NOT EXISTS idx_scoring_source      ON scoring_results(source_id);
CREATE INDEX IF NOT EXISTS idx_events_device       ON security_events(device_id);
CREATE INDEX IF NOT EXISTS idx_events_type         ON security_events(event_type);
