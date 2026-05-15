# Hook Guidelines

> IoT 项目通用自定义 Hook 规范

---

## 核心原则

- Hook 封装可复用的状态逻辑
- 命名必须以 `use` 开头
- 数据获取通过 services 层，不直接在 Hook 里写 fetch
- IoT 实时数据 Hook 必须处理连接断开和重连

---

## 通用 IoT Hooks

### useDeviceData — 设备数据获取

```typescript
function useDeviceData(deviceId: string) {
  // 获取设备详情 + 实时状态
  // 返回：{ device, realtimeData, isLoading, error }
}
```

### useRealtimeStatus — 实时状态订阅

```typescript
function useRealtimeStatus(deviceIds: string[]) {
  // 通过 WebSocket / MQTT 订阅设备状态变化
  // 返回：{ statuses, isConnected, lastUpdate }
  // 必须处理：连接断开自动重连、设备上下线通知
}
```

### useMqtt — MQTT 连接管理

```typescript
function useMqtt(topics: string[]) {
  // 管理 MQTT 连接和订阅
  // 返回：{ messages, isConnected, subscribe, unsubscribe }
  // 必须处理：自动重连、消息队列、离线缓存
}
```

### useWorkOrder — 工单操作

```typescript
function useWorkOrder() {
  // 工单 CRUD + 状态流转
  // 返回：{ create, assign, resolve, close, list }
}
```

---

## Forbidden Patterns

- **禁止**在 Hook 里直接写 fetch / axios 调用（用 services 层）
- **禁止**在 Hook 里做复杂计算（用 useMemo 或提取到 utils）
- **禁止**忽略 WebSocket / MQTT 的断连重连逻辑
