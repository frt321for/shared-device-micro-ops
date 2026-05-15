CREATE TABLE order_events (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    device_id       BIGINT NOT NULL REFERENCES devices(id),
    site_id         BIGINT REFERENCES sites(id),
    sku_id          BIGINT REFERENCES skus(id),
    quantity        INTEGER NOT NULL DEFAULT 1,
    amount          DECIMAL(10,2) NOT NULL DEFAULT 0,
    pay_method      VARCHAR(16),
    status          VARCHAR(16) NOT NULL DEFAULT 'completed',
    event_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_events_device ON order_events(device_id);
CREATE INDEX idx_order_events_site ON order_events(site_id);
CREATE INDEX idx_order_events_time ON order_events(event_time);
CREATE INDEX idx_order_events_sku ON order_events(sku_id);

CREATE TABLE device_telemetry (
    time            TIMESTAMPTZ NOT NULL,
    device_id       BIGINT NOT NULL,
    metric          TEXT NOT NULL,
    value           DOUBLE PRECISION,
    tags            JSONB
);

SELECT create_hypertable('device_telemetry', 'time', if_not_exists => TRUE);
SELECT add_compression_policy('device_telemetry', INTERVAL '7 days', if_not_exists => TRUE);

CREATE INDEX idx_telemetry_device ON device_telemetry(device_id, time DESC);

CREATE TABLE device_events (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    device_id       BIGINT NOT NULL REFERENCES devices(id),
    event_type      VARCHAR(32) NOT NULL,
    event_data      JSONB,
    severity        VARCHAR(16) DEFAULT 'info',
    occurred_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_device_events_device ON device_events(device_id, occurred_at DESC);
CREATE INDEX idx_device_events_type ON device_events(event_type);

CREATE TABLE routes (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending',
    assignee_id     BIGINT REFERENCES users(id),
    total_distance  DOUBLE PRECISION,
    estimated_minutes INTEGER,
    adjustment_reason TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE route_stops (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    route_id        BIGINT NOT NULL REFERENCES routes(id),
    work_order_id   BIGINT NOT NULL REFERENCES work_orders(id),
    site_id         BIGINT NOT NULL REFERENCES sites(id),
    stop_order      INTEGER NOT NULL,
    estimated_minutes INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_route_stops_route ON route_stops(route_id, stop_order);
