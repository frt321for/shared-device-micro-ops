# 共享设备微运营平台 (Shared Device Micro-Ops Platform)

物联网共享设备综合运营管理平台，提供设备监控、站点管理、工单调度、库存管理、收益分析及 AI 辅助运营等功能。

## 技术栈

| 层级 | 技术 |
|------|------|
| **前端** | React 19 + TypeScript + Vite 8 + react-router-dom 7 |
| **图表** | ECharts 6 + echarts-for-react |
| **地图** | Leaflet 1.9 |
| **后端** | Spring Boot 3 + JPA + Spring Security (JWT) |
| **数据库** | PostgreSQL 16 (TimescaleDB) + Flyway 迁移 |
| **缓存** | Redis 7 |
| **消息** | Mosquitto (MQTT) |
| **AI** | ModelScope (DeepSeek API) |
| **测试** | JUnit 5 + Mockito + Vitest + Playwright MCP (E2E) |

## 项目结构

```
├── src/
│   ├── backend/          # Spring Boot 后端 (Maven)
│   │   └── ops-app/
│   │       ├── src/main/java/com/iot/ops/application/
│   │       │   ├── config/        # 安全、JWT、MQTT 等配置
│   │       │   ├── infra/         # 基础设施 (MQTT、Redis、Security)
│   │       │   └── module/        # 业务模块
│   │       │       ├── auth/      # 认证
│   │       │       ├── dashboard/ # 仪表盘
│   │       │       ├── device/    # 设备管理
│   │       │       ├── site/      # 站点管理
│   │       │       ├── workorder/ # 工单管理
│   │       │       ├── dispatch/  # 调度路线
│   │       │       ├── inventory/ # 库存管理
│   │       │       ├── revenue/   # 收益分析
│   │       │       ├── ai/        # AI 报告
│   │       │       └── simulator/ # 设备模拟器
│   │       └── src/main/resources/
│   │           ├── application.yml       # 主配置
│   │           └── db/migration/         # Flyway SQL 迁移
│   └── frontend/         # React SPA 前端 (pnpm)
│       └── src/
│           ├── api/          # HTTP 客户端 + 端点定义
│           ├── components/   # 布局组件
│           ├── hooks/        # Auth Provider
│           └── pages/        # 13 个页面组件
├── docker/
│   └── infra-compose.yml    # PostgreSQL + Redis + Mosquitto
├── scripts/                 # 运维脚本
├── config/                  # 环境配置
├── docs/
│   └── testing/             # 测试文档体系
└── prototypes/              # UI 原型
```

## 快速开始

### 前置条件

- Java 21+
- Node.js 20+
- pnpm
- Docker & Docker Compose

### 1. 启动基础设施

```bash
cd docker
docker compose -f infra-compose.yml up -d
```

### 2. 启动后端

```bash
cd src/backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

后端运行在 `http://localhost:8080`，API 前缀 `/api/v1`。

Swagger 文档：`http://localhost:8080/swagger-ui.html`

### 3. 启动前端

```bash
cd src/frontend
pnpm install
pnpm dev
```

前端运行在 `http://localhost:5173`。

### 4. 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| **admin** | password | 系统管理员（全权限） |
| manager | password | 运维经理 |
| tech | password | 运维工程师 |
| replenisher | password | 补货员 |
| maintainer | password | 维修员 |
| warehouse | password | 仓管员 |

> 种子数据由 `DevDataInitializer` 在 `dev` profile 下自动生成。

## 功能模块

| 页面 | 路由 | 说明 |
|------|------|------|
| 仪表盘 | `/dashboard` | 统计概览、在线率、实时告警 |
| 站点管理 | `/sites`, `/sites/:id` | CRUD + 详情 Tab（设备/工单/营收）|
| 设备管理 | `/devices`, `/devices/:id` | CRUD + 遥测/库存/温度曲线/事件 |
| 工单工作台 | `/work-orders` | 完整状态机（派单→到达→处理→完成→审核）|
| 库存管理 | `/inventory` | SKU / 设备库存 / 仓库 / 损耗 |
| 路线视图 | `/routes` | Leaflet 地图路线规划 |
| 收益分析 | `/revenue` | 概览/站点排名/设备效率 |
| AI 周报 | `/ai-report` | 站点 AI 运营分析报告自动生成 |
| 全局搜索 | `/search` | 跨站点/设备/工单搜索 |

## API 概览

共 **14 个控制器 / 52 个 REST 端点**，全部位于 `/api/v1/` 下：

| 模块 | 安全 | 主要端点 |
|------|------|---------|
| Auth | 公开 | `POST /auth/login`, `GET /auth/users` |
| Dashboard | 认证 | `GET /dashboard/overview`, `/alerts`, `/device-stats`, `/today-tasks` |
| 站点 | 认证 | CRUD + `/sites/{id}/statistics` |
| 设备 | 认证 | CRUD + `/devices/{id}/telemetry`, `/events`, `/command` |
| 工单 | 认证 | CRUD + 状态机（assign/arrive/process/complete/review/cancel）|
| 调度 | ADMIN/MANAGER/REPLENISHER | 路线生成/调整 |
| 库存 | 认证 | SKU / 仓库出入库 / 设备库存 / 损耗 |
| 收益 | ADMIN/MANAGER | 概览/站点/设备/SKU 分析 |
| AI | ADMIN/MANAGER | 补货建议/故障分析/周报 |
| 模拟器 | ADMIN | 设备数据模拟 |

## 测试

```bash
# 后端测试
cd src/backend && mvn test

# 后端覆盖率报告
cd src/backend && mvn verify

# 前端测试
cd src/frontend && pnpm test

# E2E 浏览器全量回归（MCP Playwright）
# 参见 docs/testing/e2e-test-spec.md
```

完整测试文档见 [docs/testing/README.md](docs/testing/README.md)。

## License

MIT
