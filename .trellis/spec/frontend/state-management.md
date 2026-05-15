# State Management

> IoT 项目通用状态管理规范

---

## 核心原则

- 按数据类型分层管理状态
- 实时状态用 WebSocket/MQTT 直接更新，不走全局 Store
- 业务数据用服务端状态管理（React Query / SWR）
- UI 状态用轻量 Store（Zustand / Pinia）

---

## 状态分类

| 类型 | 管理方式 | 示例 |
|------|---------|------|
| **实时设备状态** | WebSocket/MQTT 直接更新 | 设备在线/离线、当前温度、充电状态 |
| **服务端状态** | React Query / SWR | 设备列表、工单列表、审计日志 |
| **全局 UI 状态** | Zustand / Pinia | 当前选中的站点、侧边栏展开/折叠、主题 |
| **页面局部状态** | useState | 表单输入、弹窗开关、排序筛选 |

---

## 实时状态管理（IoT 特有）

```typescript
// 设备实时状态不走全局 Store，直接通过 Hook 订阅
const { statuses } = useRealtimeStatus(deviceIds);

// 状态变化时直接更新 UI，不经过 dispatch/action
// 因为实时数据更新频率高（每秒），走 Store 会增加不必要的开销
```

---

## Forbidden Patterns

- **禁止**把实时传感器数据存进全局 Store（频率太高，用 Hook 直接订阅）
- **禁止**把所有状态都塞进一个 Store（按职责分离）
- **禁止**在 Store 里写 API 调用逻辑（用 services 层）
