# 项目审查报告 — 共享设备微运营平台

**审查日期**: 2026-05-15  
**审查范围**: 后端全部 Java 代码（62 文件）、前端全部 TSX/TS 代码（17 文件）、数据库迁移（5 份 SQL）、基础设施脚本（PS1）  
**审查基准**: requirements.md + api-design.md + architecture.md + phases.md

---

## 一、总览评分

| 维度 | 评分 | 说明 |
|------|------|------|
| **后端完成度** | **55%** | 骨架完整，核心 CRUD + 状态机已实现，但关键业务算法缺失 |
| **前端完成度** | **45%** | 8 个页面全部存在且调真实 API（无假数据），但无图表/地图/分页/搜索，存在路由路径不匹配等关键 Bug |
| **基础设施** | **65%** | Maven 骨架+PS1 脚本+SSH 隧道到位，但缺 Docker Compose |
| **测试** | **0%** | 零测试文件，零测试覆盖 |
| **综合** | **~45%** | 可跑通基础流程，但远未达到验收标准 |

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

1. **修复路由路径不匹配** — Sidebar 导航链接与 App.tsx 路由定义对齐（Dashboard `/dashboard` → `/`，AI周报 `/ai-report` → `/ai-reports`）
2. **修复编译错误** — `endpoints.ts` 补导出 `IUserInfo` 接口，或 `useAuth.tsx` 移除未使用导入
3. **Docker Compose 编排** — 编写 `docker/infra-compose.yml`（PostgreSQL + TimescaleDB + Redis + Mosquitto），端口映射按 ADR-009 规范
4. **补充测试** — 至少覆盖工单状态机、库存阈值、优先级计算的核心路径
5. **ECharts 图表** — Dashboard 设备在线率饼图 + 收益趋势折线图
6. **Leaflet 地图** — RoutePage 地图标记 + 路线连线
7. **工单自动创建** — 库存低于阈值时自动触发补货工单生成

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

**审查完成。本报告为只读审查，未修改任何文件。**
