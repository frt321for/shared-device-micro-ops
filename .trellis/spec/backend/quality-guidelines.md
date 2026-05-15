# Quality Guidelines

> IoT 项目代码质量标准

---

## 代码规范

### 函数规范

- 单个函数不超过 **50 行**
- 单个文件不超过 **400 行**（超过必须拆分）
- 函数参数不超过 **4 个**（超过用对象封装）
- 嵌套层级不超过 **4 层**

### 命名规范

```typescript
// ✅ 正确：语义清晰，IoT 领域术语
const getDeviceById = (id: string) => { ... };
const isDeviceOnline = (device: Device) => { ... };
const calculateBatteryHealth = (voltage: number, cycles: number) => { ... };
const createWorkOrder = (device: Device, type: string) => { ... };

// ❌ 错误：含义不明
const getData = (id: string) => { ... };
const check = (d: any) => { ... };
```

---

## API 设计规范

### RESTful 命名

```
GET    /api/v1/devices                    # 设备列表
GET    /api/v1/devices/:id                # 设备详情
POST   /api/v1/devices                    # 创建设备
PUT    /api/v1/devices/:id                # 更新设备
DELETE /api/v1/devices/:id                # 删除设备
GET    /api/v1/devices/:id/telemetry      # 设备遥测数据
POST   /api/v1/devices/:id/commands       # 下发命令

GET    /api/v1/work-orders                # 工单列表
POST   /api/v1/work-orders                # 创建工单
PUT    /api/v1/work-orders/:id/assign     # 指派
PUT    /api/v1/work-orders/:id/resolve    # 解决

GET    /api/v1/audit-logs                 # 审计查询
GET    /api/v1/audit-logs/export          # 导出审计包
```

### 分页规范

```json
GET /api/v1/devices?page=1&pageSize=20&status=online

{
  "code": 0,
  "data": {
    "list": [...],
    "pagination": { "page": 1, "pageSize": 20, "total": 156, "totalPages": 8 }
  }
}
```

---

## Git 提交规范（强制 Conventional Commits）

```
<type>(<scope>): <description>
```

| Type | 用途 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat(device): 添加设备批量注册接口` |
| `fix` | 修复 | `fix(mqtt): 修复心跳超时误判问题` |
| `refactor` | 重构 | `refactor(twin): 重构状态机实现` |
| `docs` | 文档 | `docs(api): 更新设备接口文档` |
| `test` | 测试 | `test(workorder): 添加工单流转单元测试` |
| `chore` | 构建/工具 | `chore(docker): 更新 MySQL 镜像版本` |
| `perf` | 性能 | `perf(query): 优化设备列表查询性能` |
| `style` | UI | `style(dashboard): 优化监控大屏布局` |

---

## 项目初始化规范

```
1. 新建文件夹作为项目根目录
2. git init
3. 创建 .gitignore（排除 node_modules, .env, __pycache__, dist 等）
4. 创建 .env.example（环境变量模板）
5. 创建 docker-compose.yml（云服务器基础设施编排）
6. 开始写代码
7. 关键阶段做约定式提交
8. 工程完成后写 README.md
```

---

## 环境与部署规范

### 云服务器基础设施（通过 SSH aliyun2738 连接）

```yaml
# infra-compose.yml — 部署在云服务器上
# 先检查 docker images 复用已有 tag，不要重新拉取
services:
  mysql:
    image: mysql:8.0
    ports: ["3306:3306"]
    volumes: ["mysql-data:/var/lib/mysql"]

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  emqx:
    image: emqx/emqx:5.8.x    # 注意 5.9+ 改为 BSL 1.1 许可证
    ports: ["1883:1883", "8083:8083", "8883:8883"]

  timescaledb:
    image: timescale/timescaledb:latest-pg16
    ports: ["5432:5432"]
```

### 本地开发连接方式

```bash
# SSH 隧道连接云服务器基础设施
ssh aliyun2738 -L 3306:localhost:3306      # MySQL
ssh aliyun2738 -L 6379:localhost:6379      # Redis
ssh aliyun2738 -L 1883:localhost:1883      # MQTT
ssh aliyun2738 -L 5432:localhost:5432      # TimescaleDB
```

### 包管理优先级

| 语言 | 包管理器 |
|------|---------|
| Python | **uv** > pip |
| JavaScript/TypeScript | **pnpm** > npm |
| Go | go mod |
| Rust | cargo |
| Java | maven (mvn)，JDK21 |

### Windows 开发环境

- 使用 `bash -lc` 执行 Linux 命令（WSL2）
- `rg` 命令可用，优先用于检索
- PowerShell 遇到问题时切换 bash

---

## Forbidden Patterns

- **禁止** `console.log` / `print()` 作为日志输出（用结构化日志库）
- **禁止**硬编码 IP、端口、密码（用环境变量）
- **禁止** `any` 类型（TypeScript 项目）
- **禁止**裸 SQL 字符串拼接（用参数化查询或 ORM）
- **禁止**在工程未完成时写 README.md
- **禁止**重新拉取已有 Docker 镜像（先检查 docker images 复用 tag）
- **禁止**在本地直接连接云服务器数据库（必须通过 SSH 隧道）
