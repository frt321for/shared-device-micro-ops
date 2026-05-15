# Directory Structure

> IoT 项目后端目录结构规范

---

## 核心原则

- 按**业务领域**组织模块，不按技术分层
- 每个模块内聚：API / Service / Model / Repository 自包含
- 公共能力提取到 `common/`
- 设备接入层与业务服务层严格分离
- 使用 `iot-project-builder` skill 的标准结构

---

## 项目根目录结构

```
<project-name>/
├── .git/                          # Git 仓库
├── .gitignore                     # Git 忽略规则
├── .env.example                   # 环境变量模板
├── README.md                      # 工程完成后才写
├── docs/                          # 文档目录
│   ├── planning/                  # 规划文档
│   │   ├── requirements.md        # 需求文档
│   │   ├── architecture.md        # 架构设计
│   │   ├── phases.md              # 阶段规划 (A/B/C/D)
│   │   ├── decisions.md           # 技术决策记录
│   │   └── api-design.md          # API 设计文档
│   ├── progress/                  # 进度记录
│   │   ├── phase-a.md
│   │   ├── phase-b.md
│   │   ├── phase-c.md
│   │   └── phase-d.md
│   └── references/
│       ├── official-docs.md       # 官方文档链接
│       ├── design-ref.md          # 设计参考
│       └── tech-stack.md          # 技术栈说明
├── src/
│   ├── backend/
│   │   ├── api/                   # API 层（按业务模块分目录）
│   │   ├── services/              # 业务逻辑层
│   │   ├── models/                # 数据模型层
│   │   ├── repositories/          # 数据访问层
│   │   ├── common/                # 公共工具、中间件、异常
│   │   │   ├── config/
│   │   │   ├── exception/
│   │   │   ├── middleware/
│   │   │   └── util/
│   │   └── gateway/               # 设备接入网关（MQTT/HTTP/WebSocket）
│   ├── frontend/
│   └── shared/                    # 前后端共享类型定义
│       ├── types/
│       └── constants/
├── simulator/                     # 设备模拟器（独立运行）
│   ├── devices/
│   ├── protocols/
│   └── replay/
├── tests/
│   ├── unit/
│   ├── integration/
│   └── e2e/
├── docker/
│   ├── infra-compose.yml          # 云服务器基础设施编排
│   ├── dev-compose.yml            # 本地开发编排
│   └── Dockerfile
├── scripts/
├── prototypes/                    # HTML 原型文件
└── config/
```

---

## 通用后端模块

所有 IoT 项目都包含以下通用模块：

```
src/backend/
├── api/
│   ├── device/                    # 设备管理 API
│   ├── workorder/                 # 工单管理 API
│   └── audit/                     # 审计日志 API
├── services/
│   ├── device-service/
│   ├── workorder-service/
│   └── audit-service/
├── models/
│   ├── device.ts
│   ├── workorder.ts
│   └── audit-log.ts
├── gateway/
│   ├── mqtt-gateway/              # MQTT 接入
│   └── http-gateway/              # HTTP 接入
└── twin/                          # 设备影子 / 状态聚合
    ├── state-machine/
    └── shadow-store/
```

项目特有模块根据业务需求在 `api/`、`services/`、`models/` 下按领域添加。

---

## 模块命名规则

| 类型 | 规则 | 示例 |
|------|------|------|
| 业务模块 | kebab-case，按领域命名 | `device/`, `workorder/`, `charging/` |
| API 文件 | `{resource}-api.ts` 或 `{resource}_controller.py` | `device-api.ts` |
| Service 文件 | `{resource}-service.ts` 或 `{resource}_service.py` | `device-service.ts` |
| Model 文件 | `{resource}.ts` 或 `{resource}.py` | `device.ts` |
| 测试文件 | `{filename}.test.ts` 或 `{filename}_test.py` | `device-api.test.ts` |

---

## 数据分离架构

```
src/backend/
├── gateway/          → 设备数据进入 Redis（实时状态）
├── services/         → 业务逻辑写入 MySQL（业务事实）
├── repositories/     → 时序数据写入 TimescaleDB（事件流）
└── twin/             → 从 Redis 读取实时状态，聚合后返回
```

---

## Forbidden Patterns

- **禁止**把所有代码塞进单个 `main.go` / `app.py` / `index.ts`
- **禁止**在 API 层直接写 SQL / 数据库操作（必须通过 Service 和 Repository）
- **禁止**设备接入层和业务服务层互相依赖（通过事件总线解耦）
- **禁止**硬编码设备 ID、阈值、告警规则（必须可配置，存数据库）
- **禁止**在工程未完成时写 README.md
