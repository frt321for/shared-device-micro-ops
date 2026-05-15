# 项目审查报告 — 共享设备微运营平台

**审查日期**: 2026-05-15  
**审查范围**: 后端全部 Java 代码（62 文件）、前端全部 TSX/TS 代码（17 文件）、数据库迁移（5 份 SQL）、基础设施脚本（PS1）  
**审查基准**: requirements.md + api-design.md + architecture.md + phases.md

---

## 一、总览评分

| 维度 | 评分 | 说明 |
|------|------|------|
| **后端完成度** | **45%** | CRUD + 工单状态机已实现，但 16 个架构组件缺失、9 个逻辑错误、6 处性能 CRITICAL |
| **前端完成度** | **45%** | 8 个页面全部存在且调真实 API（无假数据），但无图表/地图/分页/搜索，存在路由路径不匹配等关键 Bug |
| **基础设施** | **40%** | Maven 骨架+PS1 脚本+SSH 隧道到位，但缺 Docker Compose、多数据源配置、Redis 配置 |
| **测试** | **0%** | 零测试文件，零测试覆盖 |
| **安全** | **20%** | JWT 密钥硬编码、登录无密码验证、events 端点无认证，4 个 CRITICAL 安全漏洞 |
| **综合** | **~35%** | 可跑通基础演示流程，但存在严重安全漏洞、性能隐患和架构偏离，远未达到验收标准 |

---

## 二、后端逐模块审查

### 2.1 站点模块 (Site) — 完成度 70%

**已有**:
- `SiteController`: 6 个 API 端点齐全（列表/创建/详情/更新/删除/统计）
- `SiteService`: 完整 CRUD + 软删除 + 搜索/筛选分页
- `SiteRepository`: JPA Repository + 自定义查询方法
- `Site` 域模型: 字段完整匹配 DB schema

**问题**:
- 统计接口 (`getStatistics`) 用 `findAll().size()` 做计数，大数据量下性能堪忧
- `findById` 抛 `RuntimeException` 而非自定义业务异常
- 缺少输入参数校验（无 `@Valid`）

### 2.2 设备模块 (Device) — 完成度 65%

**已有**:
- `DeviceController`: CRUD + 设备类型列表
- `DeviceService`: 动态 Criteria 查询 + 软删除
- `DeviceTypeController`: 设备类型查询
- `Device` / `DeviceType` / `DeviceGroup` 域模型

**缺失**（对比 API 设计）:
- `/devices/{id}/events` — 设备事件时间线 **未实现**
- `/devices/{id}/telemetry` — 设备遥测曲线 **未实现**
- `/devices/{id}/command` — 设备命令下发 **未实现**

**问题**:
- 设备状态机（未启用→在线→缺货预警→缺货→故障→维护中→恢复在线→停用）**未在代码中实现**，状态直接由前端/模拟器设置
- 无状态转换校验逻辑

### 2.3 库存模块 (Inventory) — 完成度 60%

**已有**:
- `InventoryController`: SKU CRUD + 仓库库存 + 设备库存 + 损耗记录
- `InventoryService`: 完整 SKU/仓库/设备库存/损耗操作
- 仓库入库 (`inbound`) / 出库 (`outbound`) 有库存校验逻辑

**缺失**:
- `/warehouse/check` — 仓库盘点 **未实现**
- `/stock/predictions` — 缺货预测列表返回的是过滤后的全量数据，无真正预测算法
- `/device-stock/{id}/correct` — 库存校正前端入口缺失
- 库存状态机（充足→低库存→即将售罄→已售罄→待补货→已补货）**未在 Service 层实现**，仅在 `correctDeviceStock` 中做了简单判断

**问题**:
- `findAllSkus` 先全量查 DB 再内存过滤分页，大数据量下 O(n) 内存过滤
- `predictSoldOut` 字段存在于 DB 但无人写入，永远为 null

### 2.4 工单模块 (WorkOrder) — 完成度 75%

**已有**:
- `WorkOrderController`: 完整 9 个 API（CRUD + 派单/到场/处理/完成/复核/取消）
- `WorkOrderService`: **状态机流转完整实现** — `pending_assign → assigned → arrived → processing → pending_review → closed`
- 审计日志 (`WorkOrderAudit`) 每次状态变更自动记录
- 订单号自动生成（`WO` + 日期 + 序号）
- 动态 Criteria 分页查询

**问题**:
- `create()` 方法不校验 `type` 是否合法（补货/维修/故障）
- 工单状态机虽然实现了，但没有防重复转换校验（可重复 `assign`）
- `review` 时如果 `result != "approved"` 不会设置状态，工单卡死在 `pending_review`
- 缺少工单自动创建逻辑（库存低于阈值时自动生成补货工单）

### 2.5 派单模块 (Dispatch) — 完成度 50%

**已有**:
- `DispatchController`: 优先级计算 + 路线生成/查询/调整
- `DispatchService`: 优先级算法（权重+超时+紧急类型）
- 路线生成 + 站点停靠列表

**缺失/问题**:
- **距离计算完全缺失** — `totalDistance` 始终为 0.0
- **路线建议算法缺失** — 没有基于经纬度的最优路线排序
- 路线状态仅支持 `pending`，无法推进到 `in_progress` / `completed`
- `adjustRoute` 不保存调整原因到 `adjustmentReason`（虽然字段存在）
- `Route` / `RouteStop` 域模型存在但 `RouteController` 无独立端点（通过 DispatchController 处理）

### 2.6 收益模块 (Revenue) — 完成度 55%

**已有**:
- `RevenueController`: 站点排行/详情/设备效率/总览
- `RevenueService`: 真实数据聚合，按时间范围过滤订单事件

**问题**:
- `totalCost` / `grossProfit` / `lossAmount` **全部硬编码为 0** — 成本核算逻辑未实现
- `getDeviceEfficiency` 无效率计算，仅按订单数排序
- `getOverview` 无趋势对比（环比/同比）
- 所有查询都用 `findAll()` 再内存过滤，无数据库级聚合

### 2.7 AI 模块 — 完成度 60%

**已有**:
- `AiController`: 补货说明 + 故障分析 + 周报生成/列表
- `AiService`: 真实调用 ModelScope API（Qwen2.5-7B-Instruct）
- 降级策略：API 不可用时返回模板生成的文本
- 周报数据真实聚合（订单/工单/库存）

**问题**:
- `generateReplenishmentNote` 和 `generateFaultAnalysis` **不调用 AI API**，仅生成本地文本拼接
- 仅 `generateWeeklyReport` 调用了 AI API
- API 调用无重试/超时/熔断机制
- 无周报编辑/保存端点（仅有生成和列表）

### 2.8 看板模块 (Dashboard) — 完成度 65%

**已有**:
- `DashboardController`: 总览/告警/设备统计/今日任务
- `DashboardService`: 真实数据库查询聚合

**问题**:
- `countActiveRoutes()` 永远返回 0
- `getAlerts` 不区分告警级别规则，直接遍历故障设备和低库存
- `getTodayTasks` 用 `findAll()` 再内存过滤日期

### 2.9 设备模拟器 — 完成度 80%

**已有**:
- `DeviceSimulator`: 启动时自动创建设备 + 定时模拟心跳/库存/交易
- `MqttEventPublisher`: MQTT 消息发布
- 支持 5 种设备类型
- 自动故障模拟（15% 概率）

**问题**:
- 模拟器 `@PostConstruct` 自动启动，无手动控制开关
- 缺少 `SimulatorController`（API 设计中要求的 start/stop/status 端点未实现）
- MQTT 消息发布到本地 broker，但后端 `DeviceEventMessageHandler` 处理逻辑需验证

### 2.10 基础设施

**已有**:
- 5 份 Flyway 迁移文件，表结构完整
- `SecurityConfig`: JWT + 无状态会话
- `WebConfig`: CORS 配置
- `MqttConfig`: MQTT 连接配置
- `JpaConfig`: JPA 配置
- `OpenApiConfig`: Swagger UI
- SSH 隧道 + PS1 进程管理脚本

**缺失**:
- `RedisConfig` — 未实现
- `DataSourceConfig` — 未实现（多数据源分离）
- `FlywayConfig` — 未实现
- `TimescaleDB` 时序查询支持 — 未实现
- **Docker Compose 文件缺失** — 无法一键部署基础设施

---

## 三、前端逐页面审查

### 3.1 运营总览 (DashboardPage) — 完成度 35%

**真实 API 调用**: `fetchDashboardOverview` + `fetchDashboardAlerts`

**Bug**:
- **路由路径不匹配** — Sidebar 导航链接指向 `/dashboard`，但 App.tsx 定义为 `/`，点击侧边栏无法访问此页面

**问题**:
- **无 ECharts 图表** — "设备在线率"区域仅显示"暂无数据"占位
- **无快速操作按钮**（需求要求跳转到对应页面）
- "最近工单" 表格仅显示"暂无工单"占位，**未调用工单 API**
- 告警列表无分页/排序/点击跳转
- 无图表时间范围切换

### 3.2 站点管理 (SitesPage) — 完成度 65%

**真实 API 调用**: `fetchSites` + `createSite` + `updateSite` + `deleteSite`

**功能完整**: 列表 + 新建 + 编辑 + 删除 + 确认弹窗

**问题**:
- **无分页组件** — 虽然后端支持分页，前端未实现分页 UI
- 无搜索/筛选功能
- 无站点详情页面（需求要求点击跳转到设备/库存/收益/工单的聚合视图）

### 3.3 设备管理 (DevicesPage) — 完成度 50%

**真实 API 调用**: `fetchDevices` + `fetchDeviceTypes` + `fetchSites` + CRUD

**功能完整**: 列表 + 新建 + 编辑 + 删除

**问题**:
- **无分页/搜索/筛选**
- 无设备详情页面（心跳曲线/库存曲线/故障历史/事件时间线）
- 无命令下发功能
- 设备类型下拉正确联动

### 3.4 库存管理 (InventoryPage) — 完成度 55%

**真实 API 调用**: `fetchSkus` + `fetchDeviceStock` + `fetchWarehouseStock` + SKU CRUD

**功能完整**: 三个 Tab（SKU 列表/设备库存/仓库库存）

**问题**:
- **缺失 Tab**: 无"损耗记录" Tab
- SKU 无删除功能
- 无仓库入库/出库/盘点操作
- 无设备库存校正功能
- 无缺货预测展示

### 3.5 工单工作台 (WorkOrdersPage) — 完成度 50%

**真实 API 调用**: `fetchWorkOrders` + 状态流转 API (assign/arrive/process/complete/review)

**功能完整**: 列表 + 按类型筛选 + 状态流转按钮

**Bug**:
- **乐观更新 Bug** — API 调用失败时静默更新本地状态，用户看到"操作成功"实际未生效（WorkOrdersPage.tsx:77-79）
- **硬编码指派人** — 派单时 `assigneeId` 固定为 0（WorkOrdersPage.tsx:47）

**问题**:
- **无创建工单功能**（手动创建）
- 无分页
- 无搜索/排序
- 无工单详情弹窗
- 无审计日志展示
- `site` 字段显示 `-`（后端未 join 站点名称）

### 3.6 路线视图 (RoutePage) — 完成度 20%

**真实 API 调用**: `fetchRoutes` + `createRoute`

**问题**:
- **无 Leaflet 地图** — 仅显示路线卡片列表
- 无站点标记/连线
- 无拖拽排序
- 无路线详情（站点顺序 + 预计耗时）
- 创建路线表单过于简陋（输入工单 ID 逗号分隔）

### 3.7 收益分析 (RevenuePage) — 完成度 40%

**真实 API 调用**: `fetchRevenueOverview` + `fetchRevenueSites` + `fetchRevenueDevices`

**功能完整**: 总览卡片 + 站点排行表 + 设备效率列表

**问题**:
- **无 ECharts 图表** — 无趋势曲线
- 无时间筛选
- 无 SKU 分析
- 无导出功能
- 无点击跳转站点详情

### 3.8 AI 周报 (AiReportPage) — 完成度 60%

**真实 API 调用**: `fetchSites` + `fetchAiReportsBySite` + `generateAiReport` + `fetchAiReportDetail`

**功能完整**: 站点选择 + 生成报告 + 历史列表 + 报告详情

**Bug**:
- **路由路径不匹配** — Sidebar 导航链接指向 `/ai-report`，但 App.tsx 定义为 `/ai-reports`，点击侧边栏无法访问此页面

**问题**:
- 无时间范围选择（固定为近 7 天）
- 无编辑功能
- 无导出 Markdown 功能
- 报告内容纯文本展示，无 Markdown 渲染

---

## 四、关键问题汇总

### 4.1 严重缺失（影响验收）

| # | 问题 | 影响 |
|---|------|------|
| 1 | **无测试** — 零单元测试/集成测试/E2E 测试 | 违反项目最高优先级约束 |
| 2 | **无 ECharts 图表** — Dashboard/Revenue 无任何可视化 | 核心功能缺失 |
| 3 | **无 Leaflet 地图** — RoutePage 无地图 | 核心功能缺失 |
| 4 | **无 Docker Compose** — 无法部署基础设施 | 无法启动项目 |
| 5 | **工单自动创建缺失** — 库存低于阈值不触发工单 | 运营闭环断裂 |
| 6 | **路由算法缺失** — 无基于坐标的路线排序 | 派单功能不完整 |
| 7 | **成本核算缺失** — 利润/成本全部为 0 | 收益分析无意义 |

### 4.2 功能缺失（影响体验）

| # | 问题 | 影响 |
|---|------|------|
| 8 | 前端无分页组件 | 大数据量不可用 |
| 9 | 前端无搜索/筛选 | 查找效率低 |
| 10 | 无站点详情页/设备详情页 | 需求未满足 |
| 11 | 无仓库出入库/盘点操作 UI | 库存管理不完整 |
| 12 | 无工单创建/详情/审计日志 UI | 工单功能不完整 |
| 13 | 无 ECharts 心跳/库存曲线 | 设备监控缺失 |

### 4.3 代码质量问题

| # | 问题 | 位置 |
|---|------|------|
| 14 | 大量 `findAll()` + 内存过滤 | RevenueService / DashboardService / InventoryService |
| 15 | 抛 `RuntimeException` 而非业务异常 | 全局 |
| 16 | `WorkOrder.review` 非 approved 时状态不更新 | WorkOrderService:137 |
| 17 | AI 模块仅周报调 API，其余两功能为本地拼接 | AiService:41,80 |
| 18 | 前端 `api/client.ts` 的 `api.get` 不支持 query params | client.ts:78 |

### 4.4 前端关键 Bug

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| 19 | **路由路径不匹配** — Sidebar 导航链接与 App.tsx 路由定义不一致：Dashboard (`/dashboard` vs `/`)、AI周报 (`/ai-report` vs `/ai-reports`) | Sidebar.tsx:72,83 vs App.tsx:19,26 | 点击侧边栏导航会显示空白页，两个页面无法通过正常导航访问 |
| 20 | **编译错误** — `useAuth.tsx` 导入 `IUserInfo` 接口，但 `endpoints.ts` 未导出该接口 | useAuth.tsx:3 | TypeScript 编译失败 |
| 21 | **乐观更新 Bug** — 工单状态流转 API 调用失败时，前端静默更新本地状态，用户看到"操作成功"实际未生效 | WorkOrdersPage.tsx:77-79 | 数据不一致，用户操作误导 |
| 22 | **硬编码用户信息** — 侧边栏显示"张三 / 运维工程师"而非真实用户数据 | Sidebar.tsx:161-163 | 用户身份不准确 |
| 23 | **Recharts 已安装但未使用** — `package.json` 包含 `recharts` 依赖，但全项目未导入 | package.json:16 | 浪费依赖空间，Dashboard/Revenue 无图表 |

---

## 五、改进建议（按优先级排序）

### P0 — 必须完成（验收门槛）

1. **JWT 密钥外部化** — 移至环境变量 `JWT_SECRET`，使用 256 位随机密钥，移除源码中的硬编码密钥
2. **实现登录密码验证** — `AuthController.login()` 校验密码哈希（BCrypt），拒绝未注册用户；移除前端自动登录逻辑
3. **保护 events 端点** — 移除 `permitAll()` 或改为 API Key/mTLS 认证，防止伪造设备事件
4. **修复路由路径不匹配** — Sidebar 导航链接与 App.tsx 路由定义对齐（Dashboard `/dashboard` → `/`，AI周报 `/ai-report` → `/ai-reports`）
5. **修复编译错误** — `endpoints.ts` 补导出 `IUserInfo` 接口，或 `useAuth.tsx` 移除未使用导入
6. **Docker Compose 编排** — 编写 `docker/infra-compose.yml`（PostgreSQL + TimescaleDB + Redis + Mosquitto），端口映射按 ADR-009 规范
7. **补充测试** — 至少覆盖工单状态机、库存阈值、优先级计算的核心路径
8. **ECharts 图表** — Dashboard 设备在线率饼图 + 收益趋势折线图
9. **Leaflet 地图** — RoutePage 地图标记 + 路线连线
10. **工单自动创建** — 库存低于阈值时自动触发补货工单生成

### P1 — 重要功能补齐

6. **前端分页组件** — 所有列表页实现分页 UI 对接后端分页 API
7. **前端搜索/筛选** — 站点/设备/工单列表支持关键字搜索和状态筛选
8. **站点详情页** — 聚合设备列表 + 库存概览 + 工单历史 + 收益趋势
9. **设备详情页** — 心跳曲线 + 库存曲线 + 故障历史 + 事件时间线
10. **仓库出入库/盘点操作 UI** — 库存管理补全

### P2 — 代码质量提升

11. **替换 `findAll()` + 内存过滤** 为数据库级分页查询
12. **统一异常处理** — 定义 `BusinessException` + `GlobalExceptionHandler`
13. **工单状态机加锁** — 防止重复状态转换
14. **补全 AI 模块** — 补货说明和故障分析也走 AI API
15. **`api/client.ts` 支持 query params** — `api.get` 方法增加 params 参数

---

## 六、前端假数据排查结论

**结论：前端未使用硬编码假数据。**

所有 8 个页面均通过 `src/api/endpoints.ts` → `src/api/client.ts` 调用真实后端 API（`http://localhost:8080/api/v1/...`）。页面数据通过 `useEffect` + `fetch` 获取，响应后渲染到 UI。

但存在以下"看起来完整但实际功能空缺"的情况：
- Dashboard 的"设备在线率"和"最近工单"仅显示占位文字
- 工单状态流转按钮的 catch 分支会静默更新本地状态（即使 API 调用失败），可能给用户造成"操作成功"的错觉

---

## 七、文件清单

| 类别 | 数量 | 说明 |
|------|------|------|
| Java 源文件 | 62 | 含 Controller/Service/Domain/Repository/Config |
| TypeScript/TSX | 17 | 含 Pages/API/Components/Hooks |
| SQL 迁移 | 5 | V1-V5 覆盖全部表 |
| PS1 脚本 | 3 | manage.ps1 / tunnel.ps1 / freeport.ps1 |
| Maven POM | 4 | 父 + 3 子模块 |
| 前端依赖 | 已安装 | pnpm + Vite + React 19 + TypeScript 6 |

---

## 八、第2轮深度审查（安全/性能/架构合规/业务逻辑）

**审查日期**: 2026-05-15  
**审查方法**: 4个专项代理并行审查，覆盖安全审计、性能审计、架构合规性、业务逻辑正确性

---

### 8.1 安全审计

#### CRITICAL（必须修复）

| # | 问题 | 文件位置 | 风险 | 修复建议 |
|---|------|----------|------|----------|
| S-1 | **JWT 签名密钥硬编码** | `JwtFilter.java:24-26` | 密钥 `"IotOpsSecretKey2026MustBe32CharsLong!"` 硬编码在源码中，任何能访问代码仓库的人都可以伪造 JWT token | 移至环境变量 `JWT_SECRET`，使用 256 位随机密钥 |
| S-2 | **登录接口无密码验证** | `AuthController.java:14-17` | `login()` 接受任意 `username` 直接颁发 admin token，完全跳过密码校验 | 实现基于数据库的用户认证 + BCrypt 密码哈希 |
| S-3 | **前端自动以 admin 登录** | `useAuth.tsx:32-36` | 无 token 时自动调用 `/auth/login` 传入 `username: 'admin'`，无需凭证获取管理员权限 | 移除自动登录逻辑，强制用户通过登录表单 |
| S-4 | **数据库密码硬编码** | `application.yml:11` | `password: iotops123` 明文写在配置文件中 | 使用环境变量 `${DB_PASSWORD}` 注入 |

#### HIGH（强烈建议修复）

| # | 问题 | 文件位置 | 风险 | 修复建议 |
|---|------|----------|------|----------|
| S-5 | **events 端点未认证** | `SecurityConfig.java:29` | `/api/v1/events/**` 被 `permitAll()` 放行，可向 MQTT 通道注入任意事件数据 | 移除 permitAll 或改为 API Key/mTLS 认证 |
| S-6 | **JWT 角色未用于权限控制** | `JwtFilter.java:49-52` | token 中 role claim 被丢弃，传入空权限列表 `List.of()`，无 RBAC 实现 | 转换为 `SimpleGrantedAuthority("ROLE_" + role)` |
| S-7 | **MQTT 连接无认证** | `MqttConfig.java:36-44` | `MqttConnectOptions` 未设置用户名/密码 | 添加 MQTT 认证或 TLS 客户端证书 |
| S-8 | **Swagger UI 公开访问** | `SecurityConfig.java:27` | `/api-docs/**`、`/swagger-ui/**` 无认证放行 | 生产环境禁用或添加角色限制 |

#### MEDIUM（建议修复）

| # | 问题 | 文件位置 | 说明 |
|---|------|----------|------|
| S-9 | 大量端点缺少输入验证 | `AiController.java`、`EventController.java` | 使用 `Map<String, Object>` 接收请求体，无 `@Valid` 注解 |
| S-10 | JWT 无刷新机制 | `JwtFilter.java:27` | Token 有效期 24h，无 refresh token |
| S-11 | AI API 异常被静默吞没 | `AiService.java:239-241` | `catch (Exception e)` 无日志记录 |
| S-12 | 无请求频率限制 | 全局 | 所有端点无 rate limiting |
| S-13 | Token 存储在 localStorage | `useAuth.tsx:21,34,49` | XSS 漏洞可窃取 token |

---

### 8.2 性能审计

#### CRITICAL（严重性能问题）

| # | 问题 | 文件位置 | 影响 | 优化建议 |
|---|------|----------|------|----------|
| P-1 | `findAll()` 全表加载 + 内存聚合 | `RevenueService.java:169` | 10万级订单消耗数百MB堆内存，百万级OOM | 改用 `SELECT SUM(amount) FROM order_events` 聚合查询 |
| P-2 | `findAll()` 双表全量加载 + 内存关联 | `RevenueService.java:141,146` | 两个全表扫描同时发生，内存峰值为两表之和 | 改为 SQL JOIN + GROUP BY |
| P-3 | `findAll()` 全量站点 + 内存聚合 | `DashboardService.java:36-37` | 两个全表查询仅用于 `.size()` | 改用 `siteRepository.count()` + `countByStatus()` |
| P-4 | `findAll()` 全量设备 + 内存分组 | `DashboardService.java:93` | 每次访问 Dashboard 全表扫描设备表 | 改为 `SELECT status, COUNT(*) FROM devices GROUP BY status` |
| P-5 | `findAll()` + 内存过滤分页 | `InventoryService.java:43-54` | 无上限消耗内存，分页完全在内存中 | 改为 Spring Data JPA Specification |
| P-6 | AI API 同步阻塞主线程 | `AiService.java:209,227` | 每次 `new RestTemplate()` 同步调用 5-30s AI API，Tomcat 线程被阻塞 | 改用 `@Async` + `CompletableFuture`，配置超时 |

#### HIGH（显著性能问题）

| # | 问题 | 文件位置 | 影响 | 优化建议 |
|---|------|----------|------|----------|
| P-7 | N+1 查询：循环内逐个 `findById` | `DispatchService.java:138-139` | N 个工单 = N 次额外 SQL 查询 | 收集 siteId 批量 `findAllById()` |
| P-8 | `findAll().size()` 替代 `count()` | `SiteService.java:79,81` | 加载全部实体只为计数 | 改用 `countBySiteId()` |
| P-9 | `findAll()` 无分页返回全部数据 | `RevenueService.java:36` | 30天窗口可能返回数十万条记录 | 改为 SQL 聚合 `GROUP BY site_id` |
| P-10 | RestTemplate 无超时配置 | `AiService.java:209` | AI API 不可达时线程无限阻塞 | 配置 connectTimeout=5s, readTimeout=30s |
| P-11 | Redis 缓存未覆盖高频查询 | `DeviceCache.java` + 各 Service | Dashboard 概览等高频读取无缓存 | 对 `getOverview()` 等添加 `@Cacheable` |
| P-12 | 缺失复合索引 | V4 migration `order_events` | `(site_id, event_time)` 复合索引缺失 | 添加 `CREATE INDEX idx_order_events_site_time` |
| P-13 | 前端搜索无防抖 | `SitesPage.tsx:173`、`DevicesPage.tsx:185` | 每次按键立即触发 API 请求 | 使用 `useDebounce` hook 延迟 300ms |

---

### 8.3 架构合规性审计

#### 严重偏离（16项架构组件缺失）

| # | 架构要求 | 当前状态 |
|---|---------|---------|
| A-1 | `infra/db/TimescaleConfig.java` | **完全缺失** |
| A-2 | `infra/db/FlywayConfig.java` | **完全缺失** |
| A-3 | `config/DataSourceConfig.java` | **完全缺失** |
| A-4 | `config/RedisConfig.java` | **完全缺失** |
| A-5 | `infra/mqtt/MqttGateway.java` | **完全缺失** |
| A-6 | `infra/mqtt/DeviceEventPublisher.java` | **完全缺失** |
| A-7 | `infra/cache/SessionCache.java` | **完全缺失** |
| A-8 | `infra/security/UserContext.java` | **完全缺失** |
| A-9 | `infra/security/Role.java` | **完全缺失** |
| A-10 | `common/util/PageUtils.java` | **完全缺失** |
| A-11 | `common/util/DistanceCalc.java` | **完全缺失** |
| A-12 | `common/config/JacksonConfig.java` | **完全缺失** |
| A-13 | 设备状态机枚举 | **完全缺失** |
| A-14 | 库存状态机枚举 | **完全缺失** |
| A-15 | 工单状态机枚举 | **完全缺失** |
| A-16 | `DeviceSimulatorController` | **完全缺失** |

#### API 端点未实现（11项）

| # | API 端点 | 状态 |
|---|---------|------|
| A-17 | `POST /api/v1/simulator/start` | 未实现 |
| A-18 | `POST /api/v1/simulator/stop` | 未实现 |
| A-19 | `GET /api/v1/simulator/status` | 未实现 |
| A-20 | `GET /api/v1/device-stock/{deviceId}` | 未实现 |
| A-21 | `POST /api/v1/warehouse/check` | 未实现 |
| A-22 | `GET /api/v1/revenue/skus` | 未实现 |
| A-23~27 | `/api/v1/events/{heartbeat,inventory,fault,transaction,door}` | 路径不符（实现为通配） |

#### 状态机实现差距

| 状态机 | 架构要求 | 代码实现 | 差距 |
|--------|---------|---------|------|
| **设备** | 8个状态，有转换验证 | 裸字符串，无枚举，无验证 | 严重缺失 |
| **工单** | 7个状态 | 有 `validateTransition()` 但无枚举 | 有校验但无枚举 |
| **库存** | 6个状态 | 仅用 `adequate`/`low`/`out_of_stock` | 严重缺失 |

---

### 8.4 业务逻辑正确性审计

#### 逻辑错误（会导致错误行为）

| # | 模块 | 问题 | 文件位置 | 影响 |
|---|------|------|----------|------|
| B-1 | **工单** | 工单号生成并发冲突风险 | `WorkOrderService.java:87-89` | 同一毫秒生成重复工单号 |
| B-2 | **工单** | `review()` 对无效 result 值不做校验 | `WorkOrderService.java:140` | 无效审查结果被持久化 |
| B-3 | **派单** | `updateRouteStatus()` else 分支允许任意状态覆盖 | `DispatchService.java:204` | 路线状态被非法回退 |
| B-4 | **派单** | `adjustRoute()` 调整后未重新计算距离 | `DispatchService.java:229-230` | totalDistance 与实际不一致 |
| B-5 | **AI** | `generateFaultAnalysis()` 使用不存在的状态值 `"in_progress"` | `AiService.java:113` | pendingCount 计算错误 |
| B-6 | **看板** | `getTodayTasks()` 只筛选今天创建的工单 | `DashboardService.java:107-111` | 活跃工单不会出现在任务列表 |
| B-7 | **站点** | `getStatistics()` 使用无效设备状态 `"active"` | `SiteService.java:80` | activeDeviceCount 始终为 0 |
| B-8 | **收益** | `getOverview()` 加载全部 OrderEvent 到内存 | `RevenueService.java:169` | 内存溢出风险 |
| B-9 | **收益** | `getDeviceEfficiency()` 效率计算公式不合理 | `RevenueService.java:162` | orderCount<=10 效率永远为 0 |

#### 边界条件遗漏（12项）

| # | 模块 | 问题 | 文件位置 |
|---|------|------|----------|
| B-10 | 工单 | `create()` 未校验必填字段 | WorkOrderService.java:86-92 |
| B-11 | 库存 | `inbound()` 对多条记录只取第一条 | InventoryService.java:98-109 |
| B-12 | 库存 | `outbound()` 未同步更新设备库存 | InventoryService.java:123-144 |
| B-13 | 库存 | `correctDeviceStock()` reason 未保存 | InventoryService.java:153-161 |
| B-14 | 库存 | `recordLoss()` 不扣减设备库存 | InventoryService.java:172-175 |
| B-15 | 看板 | `getOverview()` 低库存阈值硬编码为 10 | DashboardService.java:39 |
| B-16 | 站点 | `getStatistics()` 工单计数包含已关闭/已取消 | SiteService.java:81 |
| B-17 | 设备 | `update()` 直接覆盖保存未做字段合并 | DeviceService.java:83-85 |
| B-18 | 设备 | `delete()` 设备不存在时静默返回 | DeviceService.java:87-91 |
| B-19 | AI | `callAiApi()` 异常被静默吞掉无日志 | AiService.java:239-241 |
| B-20 | 派单 | `nearestNeighborOptimize` 对2个工单不优化 | DispatchService.java:53 |
| B-21 | 工单 | `complete()` 未设置 `processedAt` 字段 | WorkOrderService.java:126-136 |

---

### 8.5 第2轮审查汇总

**新增发现统计**:

| 维度 | CRITICAL | HIGH | MEDIUM | LOW | 合计 |
|------|----------|------|--------|-----|------|
| 安全 | 4 | 4 | 5 | 0 | 13 |
| 性能 | 6 | 7 | 0 | 0 | 13 |
| 架构合规 | 16 | 11 | 0 | 0 | 27 |
| 业务逻辑 | 9 | 12 | 0 | 0 | 21 |
| **合计** | **35** | **34** | **5** | **0** | **74** |

**第2轮关键发现**:
1. **安全**: JWT 密钥硬编码 + 登录无密码验证 = 任何人都可获取管理员权限
2. **性能**: 6处 `findAll()` 全表加载 + 内存聚合，AI API 同步阻塞主线程
3. **架构**: 16个架构组件完全缺失，3个状态机无枚举定义，11个API端点未实现
4. **业务逻辑**: 9个逻辑错误（工单号并发冲突、无效状态值、状态非法回退等），12个边界条件遗漏

---

**第2轮审查完成。本报告为只读审查，未修改任何文件。**
