CREATE TABLE sites (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    address         VARCHAR(256),
    building        VARCHAR(128),
    floor           VARCHAR(32),
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    business_hours  VARCHAR(64) DEFAULT '08:00-22:00',
    service_level   VARCHAR(16) NOT NULL DEFAULT 'standard',
    status          VARCHAR(16) NOT NULL DEFAULT 'active',
    contact_name    VARCHAR(64),
    contact_phone   VARCHAR(32),
    description     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP
);

CREATE INDEX idx_sites_status ON sites(status);
CREATE INDEX idx_sites_deleted ON sites(deleted_at);

CREATE TABLE device_types (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code            VARCHAR(32) NOT NULL UNIQUE,
    name            VARCHAR(128) NOT NULL,
    category        VARCHAR(32) NOT NULL,
    description     TEXT,
    icon            VARCHAR(64),
    config_template JSONB,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO device_types (code, name, category, description) VALUES
    ('coffee_machine', '咖啡机', 'vending', '自动咖啡售卖机'),
    ('snack_cabinet', '零食柜', 'vending', '智能零食自动售货柜'),
    ('washer', '洗衣机', 'service', '共享洗衣机'),
    ('locker', '自提柜', 'storage', '智能自提储物柜'),
    ('smart_fridge', '共享冰箱', 'storage', '共享智能冰箱');

CREATE TABLE devices (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    device_code     VARCHAR(64) NOT NULL UNIQUE,
    device_type_id  BIGINT NOT NULL REFERENCES device_types(id),
    name            VARCHAR(128) NOT NULL,
    site_id         BIGINT REFERENCES sites(id),
    status          VARCHAR(16) NOT NULL DEFAULT 'offline',
    model           VARCHAR(64),
    capacity        INTEGER DEFAULT 0,
    location_desc   VARCHAR(128),
    install_date    DATE,
    metadata        JSONB,
    last_heartbeat  TIMESTAMP,
    inventory_warn_threshold INTEGER DEFAULT 20,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP
);

CREATE INDEX idx_devices_code ON devices(device_code);
CREATE INDEX idx_devices_site ON devices(site_id);
CREATE INDEX idx_devices_status ON devices(status);
CREATE INDEX idx_devices_type ON devices(device_type_id);
CREATE INDEX idx_devices_deleted ON devices(deleted_at);

CREATE TABLE device_groups (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    site_id         BIGINT NOT NULL REFERENCES sites(id),
    name            VARCHAR(128) NOT NULL,
    description     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE device_group_members (
    group_id        BIGINT NOT NULL REFERENCES device_groups(id),
    device_id       BIGINT NOT NULL REFERENCES devices(id),
    PRIMARY KEY (group_id, device_id)
);
