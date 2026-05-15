CREATE TABLE skus (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code            VARCHAR(64) NOT NULL UNIQUE,
    name            VARCHAR(128) NOT NULL,
    category        VARCHAR(32),
    unit            VARCHAR(16) NOT NULL DEFAULT '个',
    cost_price      DECIMAL(10,2) DEFAULT 0,
    selling_price   DECIMAL(10,2) DEFAULT 0,
    shelf_life_days INTEGER,
    reorder_point   INTEGER DEFAULT 10,
    description     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP
);

CREATE TABLE warehouse_stock (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku_id          BIGINT NOT NULL REFERENCES skus(id),
    quantity        INTEGER NOT NULL DEFAULT 0,
    batch_no        VARCHAR(64),
    expiry_date     DATE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_warehouse_sku ON warehouse_stock(sku_id);

CREATE TABLE warehouse_transactions (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku_id          BIGINT NOT NULL REFERENCES skus(id),
    type            VARCHAR(16) NOT NULL,
    quantity        INTEGER NOT NULL,
    reference_type  VARCHAR(32),
    reference_id    BIGINT,
    operator        VARCHAR(64),
    remark          TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_warehouse_tx_sku ON warehouse_transactions(sku_id);
CREATE INDEX idx_warehouse_tx_ref ON warehouse_transactions(reference_type, reference_id);

CREATE TABLE device_stock (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    device_id       BIGINT NOT NULL REFERENCES devices(id),
    sku_id          BIGINT NOT NULL REFERENCES skus(id),
    quantity        INTEGER NOT NULL DEFAULT 0,
    min_threshold   INTEGER NOT NULL DEFAULT 10,
    max_capacity    INTEGER NOT NULL DEFAULT 100,
    predicted_sold_out TIMESTAMP,
    status          VARCHAR(16) NOT NULL DEFAULT 'adequate',
    corrected_at    TIMESTAMP,
    corrected_by    VARCHAR(64),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_device_stock_uk ON device_stock(device_id, sku_id);

CREATE TABLE stock_loss_records (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    device_id       BIGINT REFERENCES devices(id),
    sku_id          BIGINT NOT NULL REFERENCES skus(id),
    quantity        INTEGER NOT NULL,
    reason          VARCHAR(32) NOT NULL,
    description     TEXT,
    recorded_by     VARCHAR(64),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
