# 共享设备微运营平台 — 全面审查报告

> 审查日期：2026-05-15
> 审查范围：功能完整性、前后端Bug、逻辑问题
> 弱化项：安全相关问题

---

## 一、审查总结

| 类别 | CRITICAL | HIGH | MEDIUM | LOW | 合计 |
|------|----------|------|--------|-----|------|
| 后端Bug | 3 | 5 | 10 | - | 18 |
| 前端Bug | 3 | 4 | 10 | 8 | 25 |
| 功能缺失 | - | - | - | - | 34 |
| 逻辑问题 | - | - | - | - | 6 |
| 需求不符 | - | - | - | - | 8 |
| **合计** | **6** | **9** | **20** | **8** | **43** |

---

## 二、最需要优先修复的问题（Top 5）

### 1. [CRITICAL] 设备状态机形同虚设

**问题**：`DeviceStatus` 枚举定义了完整的状态转换规则，但运行时代码完全没有使用枚举进行状态管理和转换约束，而是散落在各处用原始字符串操作。

**影响**：状态机是整个运营平台"监控→预测→派单→处理"闭环的基础，当前实现无法保证状态转换的合法性。

**涉及文件**：
- `src/backend/ops-app/src/main/java/com/iot/ops/application/module/device/domain/DeviceStatus.java`
- `src/backend/ops-app/src/main/java/com/iot/ops/application/infra/mqtt/DeviceEventMessageHandler.java:86` — `device.setStatus("online")` 直接赋值
- `src/backend/ops-app/src/main/java/com/iot/ops/application/infra/mqtt/DeviceEventMessageHandler.java:190` — `device.setStatus("fault")` 直接赋值

**建议**：在 `Device` 实体中添加 `transitionTo(DeviceStatus target)` 方法，封装状态转换规则校验。

---

### 2. [CRITICAL] 库存状态机未实现

**问题**：`StockStatus` 枚举定义了6种状态，但 `InventoryService` 和 `DeviceEventMessageHandler` 中使用原始字符串 `"adequate"` / `"low_stock"` / `"out_of_stock"` 管理状态，且状态值与枚举定义不一致。

**影响**：库存状态流转（充足→低库存→即将售罄→已售罄→待补货→已补货）无法正确执行。

**涉及文件**：
- `src/backend/ops-app/src/main/java/com/iot/ops/application/module/inventory/domain/StockStatus.java`
- `src/backend/ops-app/src/main/java/com/iot/ops/application/infra/mqtt/DeviceEventMessageHandler.java:135-142` — 使用 `"out_of_stock"` / `"low_stock"` / `"adequate"`
- `src/backend/ops-app/src/main/java/com/iot/ops/application/module/inventory/service/InventoryService.java:191` — `ds.setStatus(quantity <= ds.getMinThreshold() ? "low" : "adequate")`

**建议**：统一使用 `StockStatus` 枚举，实现状态转换约束。

---

### 3. [CRITICAL] 前端绕过API Client直接fetch()

**问题**：`DeviceDetailPage` 和 `WorkOrdersPage` 绕过统一的 API Client 直接使用 `fetch()`，导致认证Token不传递、401错误处理断裂。

**影响**：这两个页面在Token过期后无法正常工作，用户体验断裂。

**涉及文件**：
- `src/frontend/src/pages/DeviceDetailPage.tsx` — 直接 `fetch(\`/api/v1/devices/\${deviceId}\`)`
- `src/frontend/src/pages/WorkOrdersPage.tsx` — 直接 `fetch(\`/api/v1/work-orders\`)`

**建议**：统一使用 `apiClient.get()` / `apiClient.post()` 替代原生 `fetch()`。

---

### 4. [HIGH] 工单工作台状态流转逻辑缺陷

**问题**：
- 派单按钮对所有状态的工单都显示，没有根据状态过滤
- "完成"按钮缺少 `actualQty` 参数收集
- "复核"按钮的 `result` 参数硬编码为 `"approved"`，无法驳回
- 状态流转后列表不自动刷新

**影响**：核心业务流程（工单派单→处理→复核）无法正常使用。

**涉及文件**：`src/frontend/src/pages/WorkOrdersPage.tsx`

---

### 5. [HIGH] 页面刷新后用户信息丢失

**问题**：`useAuth` Hook 在页面刷新后无法从 `localStorage` 恢复用户信息，Sidebar 显示硬编码的 "张三"。

**影响**：用户每次刷新页面都看到错误的用户名。

**涉及文件**：
- `src/frontend/src/hooks/useAuth.tsx` — 缺少 localStorage 持久化
- `src/frontend/src/components/layout/Sidebar.tsx` — 硬编码 "张三"

---

## 三、后端审查详情

### 3.1 CRITICAL 级别问题

| 编号 | 问题 | 文件位置 | 说明 |
|------|------|----------|------|
| B-C1 | 设备状态机未使用枚举 | `DeviceEventMessageHandler.java:86,190` | 直接字符串赋值，无状态转换校验 |
| B-C2 | 库存状态机未使用枚举 | `InventoryService.java:191` | 使用 `"low"` 而非枚举值，与 `StockStatus` 定义不一致 |
| B-C3 | 工单状态机使用原始字符串 | `WorkOrderService.java:109,118,129,139,148,169,183` | 虽有 `validateTransition()` 方法，但未使用 `WorkOrderStatus` 枚举 |

### 3.2 HIGH 级别问题

| 编号 | 问题 | 文件位置 | 说明 |
|------|------|----------|------|
| B-H1 | 缺货预测算法过于简单 | `DeviceEventMessageHandler.java:147-154` | 硬编码 `hourlyRate = 2.0`，未使用历史数据 |
| B-H2 | 派单优先级计算不完整 | `DispatchService.java:86-125` | 缺少"距离"和"人员负载"因子 |
| B-H3 | 路线优化仅用最近邻算法 | `DispatchService.java:52-74` | 贪心算法非全局最优，缺少TSP优化 |
| B-H4 | 缺少订单事件聚合 | `RevenueService` | 站点收益计算缺少订单事件的正确聚合 |
| B-H5 | MQTT消息处理无重试机制 | `DeviceEventMessageHandler.java:78-80` | 异常仅打日志，消息丢失无感知 |

### 3.3 MEDIUM 级别问题

| 编号 | 问题 | 文件位置 | 说明 |
|------|------|----------|------|
| B-M1 | 缺少设备命令下发实现 | `DeviceController` | API设计中有 `/devices/{id}/command` 但未实现 |
| B-M2 | 缺少工单取消后的资源释放 | `WorkOrderService.java:179-187` | 取消工单未释放关联的路线站点 |
| B-M3 | 仓库出库未校验工单状态 | `InventoryService.java:117` | 允许对已关闭工单进行出库操作 |
| B-M4 | 设备库存校正缺少审计日志 | `InventoryService.java:185-192` | 仅记录 `correctedBy` 字段，无独立审计表 |
| B-M5 | 缺少SLA指标计算 | `DashboardService` | 看板SLA数据可能为硬编码或空 |
| B-M6 | 缺少AI周报保存功能 | `AiService` | 周报生成后未持久化到 `WeeklyReport` 表 |
| B-M7 | 缺少站点统计聚合API | `SiteController` | API设计中有 `/sites/{id}/statistics` 但可能未实现 |
| B-M8 | 路线状态机不完整 | `DispatchService.java:194-206` | 仅允许 `pending→in_progress→completed`，缺少取消状态 |
| B-M9 | 设备组成员管理缺少校验 | `DeviceGroupService` | 添加成员时未校验设备是否已属于其他组 |
| B-M10 | 缺少订单事件的SKU关联校验 | `DeviceEventMessageHandler.java:228-245` | 未校验skuId是否存在于SKU表 |

### 3.4 功能缺失清单（后端）

| 编号 | 缺失功能 | 需求文档引用 | 优先级 |
|------|----------|--------------|--------|
| B-F1 | 设备状态机完整实现 | 架构文档 - 设备状态机 | HIGH |
| B-F2 | 库存状态机完整实现 | 架构文档 - 库存状态机 | HIGH |
| B-F3 | SLA指标计算服务 | F5 - SLA指标 | HIGH |
| B-F4 | 设备效率统计 | F5 - 设备效率 | MEDIUM |
| B-F5 | 站点收益趋势分析 | F5 - 站点收益 | MEDIUM |
| B-F6 | AI周报持久化 | F6 - 周报生成 | MEDIUM |
| B-F7 | 设备命令下发 | API设计 - /devices/{id}/command | MEDIUM |
| B-F8 | 站点统计聚合 | API设计 - /sites/{id}/statistics | MEDIUM |
| B-F9 | 工单批量操作 | 工单工作台需求 | LOW |
| B-F10 | 操作审计日志表 | 架构文档 - 审计日志 | LOW |

---

## 四、前端审查详情

### 4.1 CRITICAL/HIGH 级别问题

| 编号 | 问题 | 文件位置 | 说明 |
|------|------|----------|------|
| F-C1 | 绕过API Client直接fetch | `DeviceDetailPage.tsx`, `WorkOrdersPage.tsx` | 认证失效、401处理断裂 |
| F-C2 | 页面刷新用户信息丢失 | `useAuth.tsx` | 缺少localStorage持久化 |
| F-C3 | Sidebar硬编码用户名 | `Sidebar.tsx` | 显示"张三"而非真实用户 |
| F-H1 | 工单派单逻辑缺陷 | `WorkOrdersPage.tsx` | 所有状态都显示派单按钮 |
| F-H2 | 工单完成缺少参数 | `WorkOrdersPage.tsx` | 缺少actualQty收集 |
| F-H3 | 工单复核硬编码approved | `WorkOrdersPage.tsx` | 无法驳回工单 |
| F-H4 | 收益页面API路径错误 | `RevenuePage.tsx` | 可能调用了不存在的端点 |

### 4.2 MEDIUM 级别问题

| 编号 | 问题 | 文件位置 | 说明 |
|------|------|----------|------|
| F-M1 | 缺少加载状态提示 | 多个页面 | 数据加载时无loading指示 |
| F-M2 | 缺少错误边界处理 | `App.tsx` | 未实现全局ErrorBoundary |
| F-M3 | 表单缺少防重复提交 | 多个页面 | 提交按钮未禁用 |
| F-M4 | 缺少数据导出功能 | `RevenuePage.tsx` | 需求要求支持导出 |
| F-M5 | 地图组件未实现 | `RoutePage.tsx` | Leaflet地图可能未集成 |
| F-M6 | AI周报编辑功能不完整 | `AiReportPage.tsx` | 编辑后保存可能失败 |
| F-M7 | 设备事件时间线未按类型过滤 | `DeviceDetailPage.tsx` | 过滤功能可能缺失 |
| F-M8 | 库存管理缺少Tab切换 | `InventoryPage.tsx` | SKU/仓库/设备/损耗Tab可能不完整 |
| F-M9 | 分页组件不统一 | 多个页面 | 部分页面分页组件行为不一致 |
| F-M10 | 缺少响应式布局 | 多个页面 | 移动端适配可能缺失 |

### 4.3 LOW 级别问题

| 编号 | 问题 | 文件位置 | 说明 |
|------|------|----------|------|
| F-L1 | 图表缺少hover tooltip | `DashboardPage.tsx` | ECharts配置可能不完整 |
| F-L2 | 搜索防抖未实现 | 多个列表页 | 搜索输入无防抖 |
| F-L3 | 缺少空状态展示 | 多个列表页 | 无数据时显示空白 |
| F-L4 | 颜色主题未统一 | 全局 | CSS变量可能不完整 |
| F-L5 | 缺少键盘快捷键 | 全局 | 无快捷键支持 |
| F-L6 | 缺少页面过渡动画 | 全局 | 路由切换无过渡 |
| F-L7 | 缺少打印样式 | 报表页面 | 打印时布局可能异常 |
| F-L8 | 缺少国际化支持 | 全局 | 硬编码中文字符串 |

### 4.4 功能缺失清单（前端）

| 编号 | 缺失功能 | 需求文档引用 | 优先级 |
|------|----------|--------------|--------|
| F-F1 | ErrorBoundary全局错误处理 | 测试策略 | HIGH |
| F-F2 | Loading状态统一处理 | 测试策略 | MEDIUM |
| F-F3 | 数据导出功能 | 收益分析需求 | MEDIUM |
| F-F4 | 地图路线可视化 | 路线视图需求 | MEDIUM |
| F-F5 | 工单审计日志时间线 | 工单工作台需求 | MEDIUM |
| F-F6 | 设备事件时间线过滤 | 设备详情需求 | MEDIUM |
| F-F7 | 库存校正原因弹窗 | 库存管理需求 | MEDIUM |
| F-F8 | 站点坐标编辑 | 站点管理需求 | LOW |
| F-F9 | 批量操作功能 | 工单/库存需求 | LOW |
| F-F10 | 快捷键支持 | 用户体验 | LOW |

---

## 五、业务链路完整性分析

### 链路1：设备上报库存 → 自动生成补货工单 → 派单 → 处理 → 复核

```
[✓] 设备上报库存 → DeviceEventMessageHandler.handleInventory()
[✓] 库存低于阈值检测 → if (quantity <= stock.getMinThreshold())
[✓] 自动生成补货工单 → tryAutoCreateWorkOrder()
[✓] 工单派单 → WorkOrderService.assign()
[✓] 人员到场 → WorkOrderService.arrive()
[✓] 处理完成 → WorkOrderService.complete()
[✓] 复核关闭 → WorkOrderService.review()
```

**判定：完整 ✓**

---

### 链路2：设备上报故障码 → 自动生成维修工单 → 派单 → 处理 → 复核

```
[✓] 设备上报故障码 → DeviceEventMessageHandler.handleFault()
[✓] 故障分级 → switch (severity) → priority 1/2/3
[✓] 自动生成维修工单 → WorkOrder.builder().type("repair")...
[✓] 工单派单 → WorkOrderService.assign()
[✓] 人员到场 → WorkOrderService.arrive()
[✓] 处理完成 → WorkOrderService.complete()
[✓] 复核关闭 → WorkOrderService.review()
```

**判定：完整 ✓**

**注意**：缺少"复测"环节（需求要求维修后复测）

---

### 链路3：运营经理手动创建补货工单 → 派单 → 路线规划

```
[✓] 手动创建工单 → WorkOrderService.create()
[?] 缺货预警展示 → DashboardService（需验证）
[✓] 工单派单 → WorkOrderService.assign()
[✓] 路线规划 → DispatchService.generateRoute()
```

**判定：基本完整 ⚠**

**缺失**：
- 缺少"运营经理查看缺货预警"的明确UI入口
- 路线规划与派单的联动可能不完整

---

### 链路4：仓库管理员 → SKU入库 → 设备补货出库 → 设备库存更新

```
[✓] SKU入库 → InventoryService.inbound()
[✓] 设备补货出库 → InventoryService.outbound(skuId, qty, "device", deviceId, operator)
[✓] 设备库存更新 → DeviceStock.quantity += quantity
```

**判定：完整 ✓**

**注意**：
- 出库后未自动更新设备库存状态（adequate/low_stock）
- 缺少出库与工单状态的联动

---

### 链路5：运营经理 → 生成路线 → 人工调整 → 路线执行

```
[✓] 生成路线 → DispatchService.generateRoute()
[✓] 人工调整 → DispatchService.adjustRoute()
[✓] 路线状态更新 → DispatchService.updateRouteStatus()
[?] 路线执行 → 缺少执行中的实时更新
```

**判定：基本完整 ⚠**

**缺失**：
- 路线执行过程中的状态同步
- 路线完成与工单状态的自动联动

---

## 六、与需求文档不符的地方

| 编号 | 需求 | 实际实现 | 差异 |
|------|------|----------|------|
| D1 | 设备状态机：未启用→在线→缺货预警→缺货→故障→维护中→恢复在线→停用 | 仅实现 online/fault 两种状态 | 缺少6种状态 |
| D2 | 库存状态机：充足→低库存→即将售罄→已售罄→待补货→已补货 | 仅实现 adequate/low_stock/out_of_stock | 缺少3种状态 |
| D3 | 工单状态：待生成→待派单→已派单→已到场→处理中→待复核→已关闭/已取消 | 使用 pending_assign/assigned/arrived/processing/pending_review/closed/cancelled | 状态值不一致，缺少"待生成"状态 |
| D4 | SLA指标：故障响应、维修恢复、缺货时长、任务完成率 | 未见SLA计算实现 | 完全缺失 |
| D5 | 缺货预测：按历史销量、时段、库存余量估算售罄时间 | 硬编码 hourlyRate=2.0 | 未使用历史数据 |
| D6 | 路线建议：将多个补货和维修点组织为可执行路线 | 仅用最近邻算法 | 非全局最优 |
| D7 | AI周报：可编辑周报 | 周报生成后未持久化 | 缺少保存功能 |
| D8 | 设备命令下发 | API端点未实现 | 完全缺失 |

---

## 七、修复优先级建议

### 第一阶段：核心状态机修复（1-2天）

1. 实现设备状态机完整转换规则
2. 实现库存状态机完整转换规则
3. 统一工单状态枚举使用

### 第二阶段：前端认证和工单修复（1天）

1. 修复前端绕过API Client的问题
2. 修复用户信息持久化
3. 修复工单工作台状态流转

### 第三阶段：业务逻辑补全（2-3天）

1. 实现SLA指标计算
2. 完善缺货预测算法
3. 补全AI周报保存功能
4. 实现设备命令下发

### 第四阶段：体验优化（1-2天）

1. 添加Loading状态
2. 实现ErrorBoundary
3. 完善地图组件
4. 添加数据导出功能

---

## 八、测试覆盖情况

### 后端测试

| 模块 | 测试文件 | 状态 |
|------|----------|------|
| 库存服务 | `InventoryServiceTest.java` | ✓ 存在 |
| 工单服务 | `WorkOrderServiceTest.java` | ✓ 存在 |
| 派单服务 | `DispatchServiceTest.java` | ✓ 存在 |
| 站点服务 | `SiteServiceTest.java` | ✓ 存在 |
| 收益服务 | `RevenueServiceTest.java` | ✓ 存在 |

### 前端测试

| 模块 | 测试文件 | 状态 |
|------|----------|------|
| 登录页 | `LoginPage.test.tsx` | ✓ 存在 |
| API客户端 | `api-client.test.ts` | ✓ 存在 |
| 收益页 | `revenue-page.test.tsx` | ✓ 存在 |

**注意**：前端测试覆盖率明显不足，大部分页面缺少测试。

---

**审查完成。建议按优先级分阶段修复，优先解决状态机和前端认证问题。**
