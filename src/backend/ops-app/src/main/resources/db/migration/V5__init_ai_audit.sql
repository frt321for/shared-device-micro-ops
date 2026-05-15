CREATE TABLE weekly_reports (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    site_id         BIGINT NOT NULL REFERENCES sites(id),
    title           VARCHAR(256) NOT NULL,
    content         TEXT,
    period_start    DATE NOT NULL,
    period_end      DATE NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'draft',
    created_by      BIGINT REFERENCES users(id),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_weekly_reports_site ON weekly_reports(site_id, period_start DESC);

CREATE TABLE audit_logs (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entity_type     VARCHAR(32) NOT NULL,
    entity_id       BIGINT NOT NULL,
    action          VARCHAR(32) NOT NULL,
    operator_id     BIGINT REFERENCES users(id),
    operator_name   VARCHAR(64),
    changes         JSONB,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);
