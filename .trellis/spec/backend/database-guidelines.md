# Database Guidelines

> IoT 项目数据库规范 — 三库分离架构

---

## 核心架构：三库分离（强制）

IoT 项目的数据必须分成三类存储，**禁止混用**：

| 数据类型 | 存储引擎 | 用途 | 示例 |
|---------|---------|------|------|
| **实时状态** | Redis（云服务器 Docker） | 设备在线/离线、最新值、心跳、当前工单状态 | 设备最后上报时间、当前温度 |
| **业务事实** | MySQL 8.x（云服务器 Docker） | 合同、订单、工单、策略、审计记录、配置、用户 | 租赁合同、维修工单、告警规则 |
| **时序事件** | TimescaleDB / InfluxDB（云服务器 Docker） | 传感器数据、设备日志、功率曲线、GPS轨迹 | 温度时序、电压曲线、车辆轨迹 |

### 为什么必须分离

```
# ❌ 错误：把时序传感器数据塞进 MySQL 普通表
# 设备每秒上报，几百万行数据会让查询变慢、备份巨大

# ❌ 错误：把业务数据塞进 Redis
# 重启丢失，没有事务保证，无法做复杂查询

# ✅ 正确：各司其职
Redis       → 设备在线状态、最新值缓存、会话缓存
MySQL       → 业务实体、关系、配置、审计、用户
TimescaleDB → 时序数据、事件流、历史记录
```

---

## MySQL 规范（云服务器 Docker 部署）

### 连接配置

```yaml
# docker-compose.yml（部署在云服务器）
services:
  mysql:
    image: mysql:8.0    # 复用已有镜像 tag，不要重新拉取
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${DB_NAME}
    volumes:
      - mysql-data:/var/lib/mysql

# 本地开发通过 SSH 隧道连接
# ssh aliyun2738 -L 3306:localhost:3306
# 然后本地代码连接 localhost:3306
```

### 表命名

- 小写下划线，复数形式：`devices`, `work_orders`, `audit_logs`
- 关联表：`device_contracts`, `order_items`

### 必备字段

每张业务表必须包含：

```sql
id          BIGINT AUTO_INCREMENT PRIMARY KEY,
created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
deleted_at  DATETIME NULL     -- 软删除
```

### 通用表结构

```sql
-- 设备表（所有 IoT 项目通用）
CREATE TABLE devices (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_code     VARCHAR(64) NOT NULL UNIQUE,
    device_type     VARCHAR(32) NOT NULL,
    name            VARCHAR(128) NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'offline',
    site_id         BIGINT,
    metadata        JSON,
    last_heartbeat  DATETIME,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME NULL,
    INDEX idx_device_code (device_code),
    INDEX idx_status (status),
    INDEX idx_site_id (site_id)
);

-- 工单表（所有 IoT 项目通用）
CREATE TABLE work_orders (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no        VARCHAR(64) NOT NULL UNIQUE,
    type            VARCHAR(32) NOT NULL,
    status          VARCHAR(16) NOT NULL DEFAULT 'open',
    priority        TINYINT NOT NULL DEFAULT 2,
    device_id       BIGINT,
    title           VARCHAR(256) NOT NULL,
    description     TEXT,
    assignee_id     BIGINT,
    resolved_at     DATETIME,
    closed_at       DATETIME,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME NULL,
    INDEX idx_order_no (order_no),
    INDEX idx_status (status),
    INDEX idx_device_id (device_id)
);

-- 审计日志表（所有 IoT 项目通用）
CREATE TABLE audit_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type     VARCHAR(32) NOT NULL,
    entity_id       BIGINT NOT NULL,
    action          VARCHAR(32) NOT NULL,
    operator_id     BIGINT,
    operator_name   VARCHAR(64),
    changes         JSON,
    ip_address      VARCHAR(45),
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_created_at (created_at)
);
```

---

## Redis 规范（云服务器 Docker 部署）

### Key 命名

```
{project}:{module}:{entity}:{id}:{field}

# 示例
my-project:device:1001:status
my-project:device:1001:latest
my-project:session:abc123:state
```

### 数据结构选择

| 场景 | 结构 | 示例 |
|------|------|------|
| 设备最新状态 | Hash | `device:1001 -> {temp: 25.3, voltage: 3.7, online: true}` |
| 设备在线集合 | Set | `devices:online -> {1001, 1002, 1003}` |
| 会话数据 | Hash | `session:abc123 -> {deviceId, startTime, status}` |
| 事件队列 | Stream | `events:device -> [{id, ts, payload}...]` |

### TTL 策略

- 设备心跳：60s（设备每 30s 上报一次）
- 缓存数据：最长不超过 24h
- 会话数据：会话结束后 24h

---

## 时序库规范（云服务器 Docker 部署）

### TimescaleDB 表设计

```sql
-- 设备遥测数据（通用结构）
CREATE TABLE device_telemetry (
    time        TIMESTAMPTZ NOT NULL,
    device_id   BIGINT NOT NULL,
    metric      TEXT NOT NULL,
    value       DOUBLE PRECISION,
    tags        JSONB
);

SELECT create_hypertable('device_telemetry', 'time');
SELECT add_compression_policy('device_telemetry', INTERVAL '7 days');
SELECT add_retention_policy('device_telemetry', INTERVAL '90 days');
```

---

## 迁移规范

- 使用项目框架的迁移工具（Flyway / Alembic / Prisma Migrate / golang-migrate）
- 每次迁移必须可回滚
- 禁止直接修改生产数据库结构

---

## Forbidden Patterns

- **禁止**在 MySQL 里存时序传感器数据
- **禁止**在 Redis 里存需要持久化的业务数据
- **禁止**硬编码数据库连接字符串（用环境变量 + SSH 隧道）
- **禁止** N+1 查询（必须用 JOIN 或批量查询）
- **禁止**在循环中逐行 INSERT（用批量插入）
- **禁止** SELECT * 查询（明确指定字段）
