# Directory Structure

> IoT 项目前端目录结构规范

---

## 核心原则

- **UI/UX 优先级高于功能** — 先做原型，满意后再写前端代码
- 必须使用 Design Reference（`H:\iot\Design Reference\`）中的设计参考
- 必须调用 `frontend-design`、`ui-ux-pro-max` 等技能包
- 禁止蓝紫渐变等千篇一律的风格
- 响应式设计，确保移动端兼容

---

## 前端开发流程（强制）

```
1. 查看 Design Reference（H:\iot\Design Reference\）选择设计风格
2. 调用 frontend-design / ui-ux-pro-max 技能包
3. 创建 HTML 原型文件（prototypes/ 目录）
4. 用户确认原型满意
5. 才能开始写前端代码
```

---

## 前端目录结构

```
src/frontend/
├── components/                    # 组件目录
│   ├── common/                    # 通用组件
│   │   ├── layout/                # 布局组件
│   │   │   ├── AppLayout.tsx
│   │   │   ├── Sidebar.tsx
│   │   │   └── Header.tsx
│   │   ├── data-display/          # 数据展示组件
│   │   │   ├── DataTable.tsx
│   │   │   ├── StatCard.tsx
│   │   │   └── StatusBadge.tsx
│   │   ├── feedback/              # 反馈组件
│   │   │   ├── Toast.tsx
│   │   │   └── ConfirmDialog.tsx
│   │   └── navigation/            # 导航组件
│   │       ├── Breadcrumb.tsx
│   │       └── TabNav.tsx
│   ├── charts/                    # 图表组件（IoT 通用）
│   │   ├── TimeSeriesChart.tsx    # 时序数据图表
│   │   ├── GaugeChart.tsx         # 仪表盘图表
│   │   ├── MapView.tsx            # 地图视图
│   │   └── FlowDiagram.tsx        # 流程图/能流图
│   ├── device/                    # 设备相关组件（通用）
│   │   ├── DeviceCard.tsx
│   │   ├── DeviceStatusDot.tsx
│   │   └── DeviceCommandPanel.tsx
│   ├── workorder/                 # 工单相关组件（通用）
│   │   ├── WorkOrderCard.tsx
│   │   └── WorkOrderTimeline.tsx
│   └── {business-specific}/       # 项目业务特有组件
├── pages/                         # 页面目录
│   ├── dashboard/                 # 仪表盘页面
│   ├── devices/                   # 设备管理页面
│   ├── workorders/                # 工单管理页面
│   ├── audit/                     # 审计日志页面
│   ├── settings/                  # 设置页面
│   └── {business-specific}/       # 项目业务特有页面
├── hooks/                         # 自定义 Hooks
│   ├── useDeviceData.ts
│   ├── useRealtimeStatus.ts
│   ├── useWorkOrder.ts
│   └── useMqtt.ts
├── services/                      # API 服务层
│   ├── api-client.ts
│   ├── device-api.ts
│   ├── workorder-api.ts
│   └── {business}-api.ts
├── store/                         # 状态管理
│   ├── index.ts
│   ├── device-store.ts
│   ├── workorder-store.ts
│   └── ui-store.ts
├── styles/                        # 样式文件
│   ├── global.css
│   ├── theme.ts
│   └── variables.css
├── types/                         # 类型定义
│   ├── device.ts
│   ├── workorder.ts
│   └── api.ts
├── utils/                         # 工具函数
│   ├── format.ts
│   ├── validation.ts
│   └── constants.ts
├── App.tsx
└── main.tsx
```

---

## Prototypes 目录（HTML 原型）

```
prototypes/
├── dashboard.html                 # 仪表盘原型
├── device-list.html               # 设备列表原型
├── device-detail.html             # 设备详情原型
├── workorder.html                 # 工单页面原型
└── {business-specific}.html       # 业务特有页面原型
```

**重要**：原型文件必须在写前端代码之前完成，用户确认满意后才能开始开发。

---

## Forbidden Patterns

- **禁止**跳过原型直接写前端代码
- **禁止**不看 Design Reference 就开始设计
- **禁止**蓝紫渐变等千篇一律的风格
- **禁止**不调用 `frontend-design` / `ui-ux-pro-max` 技能包
- **禁止**把所有组件塞进一个文件
- **禁止**在组件里直接写 API 调用（必须通过 services 层）
