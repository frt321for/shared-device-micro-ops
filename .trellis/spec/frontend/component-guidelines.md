# Component Guidelines

> IoT 项目通用组件规范

---

## 核心原则

- **UI/UX 优先级高于功能** — 用户看重精美 UI 大于功能完整
- 必须参考 Design Reference（`H:\iot\Design Reference\`）中的设计风格
- 必须调用 `frontend-design`、`ui-ux-pro-max` 等技能包
- 禁止蓝紫渐变等千篇一律的风格
- 组件必须响应式，兼容移动端

---

## 组件结构

```typescript
// 标准组件文件结构
interface DeviceCardProps {
  device: Device;
  onCommand?: (deviceId: string, command: string) => void;
  variant?: 'compact' | 'full';
}

export function DeviceCard({ device, onCommand, variant = 'full' }: DeviceCardProps) {
  const { data, isLoading } = useDeviceData(device.id);

  const handleCommand = (cmd: string) => {
    onCommand?.(device.id, cmd);
  };

  if (isLoading) return <DeviceCardSkeleton />;

  return (
    <Card className="device-card">
      <DeviceStatusDot status={device.status} />
      <h3>{device.name}</h3>
      {variant === 'full' && <DeviceMetrics data={data} />}
      <DeviceCommandPanel onCommand={handleCommand} />
    </Card>
  );
}
```

---

## 通用组件（所有 IoT 项目共享）

| 组件 | 用途 |
|------|------|
| `DeviceCard` | 设备信息卡片 |
| `DeviceStatusDot` | 设备状态指示灯 |
| `WorkOrderCard` | 工单卡片 |
| `TimeSeriesChart` | 时序数据图表 |
| `MapView` | 地图视图 |
| `DataTable` | 数据表格 |
| `StatCard` | 统计卡片 |

业务特有组件放在 `components/{business-domain}/` 目录下。

---

## 样式规范

### 主题配色

- **禁止**蓝紫渐变等 AI 默认风格
- 参考 Design Reference 中的配色方案
- IoT 项目推荐的配色方向：
  - 深色背景 + 高对比度数据展示（适合监控大屏）
  - 浅色背景 + 清晰的信息层次（适合管理后台）
  - 工业风 / 科技感（适合工厂/能源项目）

### 状态颜色

```css
:root {
  --status-online: #10b981;        /* 绿色 - 在线/正常 */
  --status-offline: #6b7280;       /* 灰色 - 离线 */
  --status-warning: #f59e0b;       /* 黄色 - 告警 */
  --status-error: #ef4444;         /* 红色 - 故障 */
  --status-maintenance: #3b82f6;   /* 蓝色 - 维护中 */
}
```

---

## 原型要求

在写前端代码之前，必须先在 `prototypes/` 目录创建 HTML 原型：

- 使用真实数据结构（不要用 lorem ipsum）
- 展示完整的交互状态（加载、空状态、错误状态）
- 体现响应式布局
- 用户确认满意后才能写前端代码

---

## Forbidden Patterns

- **禁止**跳过原型直接写前端代码
- **禁止**不看 Design Reference 就开始设计
- **禁止**蓝紫渐变等千篇一律的 AI 风格
- **禁止**不调用 `frontend-design` / `ui-ux-pro-max` 技能包
- **禁止**在组件里直接写 API 调用（必须通过 services 层）
- **禁止**用 `any` 类型定义 Props
- **禁止**忽略加载状态和错误状态
- **禁止**不考虑移动端响应式
