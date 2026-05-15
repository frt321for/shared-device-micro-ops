# Type Safety

> IoT 项目通用类型安全规范

---

## 核心原则

- TypeScript strict 模式开启
- 禁止 `any` 类型
- API 响应必须有类型定义
- 设备数据模型必须有完整类型

---

## 通用 IoT 类型定义

```typescript
// 设备通用类型
interface Device {
  id: string;
  deviceCode: string;
  deviceType: string;
  name: string;
  status: 'online' | 'offline' | 'fault' | 'maintenance';
  siteId?: string;
  metadata?: Record<string, unknown>;
  lastHeartbeat?: string;
  createdAt: string;
  updatedAt: string;
}

// 工单通用类型
interface WorkOrder {
  id: string;
  orderNo: string;
  type: 'maintenance' | 'repair' | 'inspection' | 'review';
  status: 'open' | 'assigned' | 'in_progress' | 'resolved' | 'closed';
  priority: 1 | 2 | 3;
  deviceId?: string;
  title: string;
  description?: string;
  assigneeId?: string;
  createdAt: string;
}

// API 响应通用类型
interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: string;
  traceId: string;
}

// 分页响应类型
interface PaginatedResponse<T> {
  list: T[];
  pagination: {
    page: number;
    pageSize: number;
    total: number;
    totalPages: number;
  };
}

// 设备遥测数据类型
interface TelemetryData {
  time: string;
  deviceId: string;
  metric: string;
  value: number;
  tags?: Record<string, unknown>;
}
```

---

## 类型组织

```
src/frontend/types/
├── device.ts              # 设备相关类型
├── workorder.ts           # 工单相关类型
├── api.ts                 # API 响应类型
├── telemetry.ts           # 遥测数据类型
├── {business}.ts          # 业务特有类型
└── index.ts               # 统一导出
```

---

## 运行时验证

```typescript
import { z } from 'zod';

const DeviceSchema = z.object({
  id: z.string(),
  deviceCode: z.string(),
  status: z.enum(['online', 'offline', 'fault', 'maintenance']),
});

// API 响应必须做运行时验证
const response = await deviceApi.getDevice(id);
const device = DeviceSchema.parse(response.data);
```

---

## Forbidden Patterns

- **禁止** `any` 类型（用 `unknown` + 类型守卫）
- **禁止**类型断言 `as` 滥用（优先用类型守卫）
- **禁止** API 响应不做类型定义直接使用
- **禁止** `@ts-ignore` / `@ts-nocheck`（必须修复类型错误）
