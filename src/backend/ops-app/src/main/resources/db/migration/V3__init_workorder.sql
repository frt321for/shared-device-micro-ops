CREATE TABLE users (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username        VARCHAR(64) NOT NULL UNIQUE,
    password_hash   VARCHAR(256) NOT NULL,
    display_name    VARCHAR(128) NOT NULL,
    phone           VARCHAR(32),
    role            VARCHAR(32) NOT NULL DEFAULT 'operator',
    status          VARCHAR(16) NOT NULL DEFAULT 'active',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP
);

INSERT INTO users (username, password_hash, display_name, role) VALUES
    ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 'admin'),
    ('manager', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '运营经理', 'manager'),
    ('replenisher', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '补货员', 'replenisher'),
    ('maintainer', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '维修员', 'maintainer'),
    ('warehouse', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '仓管员', 'warehouse_keeper');

CREATE TABLE work_orders (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_no        VARCHAR(64) NOT NULL UNIQUE,
    type            VARCHAR(16) NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'pending_assign',
    priority        INTEGER NOT NULL DEFAULT 2,
    device_id       BIGINT REFERENCES devices(id),
    site_id         BIGINT REFERENCES sites(id),
    sku_id          BIGINT REFERENCES skus(id),
    title           VARCHAR(256) NOT NULL,
    description     TEXT,
    expected_qty    INTEGER,
    actual_qty      INTEGER,
    assignee_id     BIGINT REFERENCES users(id),
    priority_reason TEXT,
    arrived_at      TIMESTAMP,
    processed_at    TIMESTAMP,
    completed_at    TIMESTAMP,
    review_result   VARCHAR(32),
    review_remark   TEXT,
    closed_at       TIMESTAMP,
    closed_by       BIGINT REFERENCES users(id),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_work_orders_no ON work_orders(order_no);
CREATE INDEX idx_work_orders_status ON work_orders(status);
CREATE INDEX idx_work_orders_device ON work_orders(device_id);
CREATE INDEX idx_work_orders_site ON work_orders(site_id);
CREATE INDEX idx_work_orders_assignee ON work_orders(assignee_id);
CREATE INDEX idx_work_orders_created ON work_orders(created_at);

CREATE TABLE work_order_audit (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    work_order_id   BIGINT NOT NULL REFERENCES work_orders(id),
    from_status     VARCHAR(16),
    to_status       VARCHAR(16) NOT NULL,
    operator_id     BIGINT REFERENCES users(id),
    operator_name   VARCHAR(64),
    remark          TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wo_audit_order ON work_order_audit(work_order_id);
