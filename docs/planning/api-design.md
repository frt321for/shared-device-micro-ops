# API 设计文档

基础路径：`/api/v1`

通用响应格式：
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

分页请求：`?page=0&size=20&sort=createdAt,desc`
分页响应：
```json
{
  "content": [],
  "totalElements": 100,
  "totalPages": 5,
  "number": 0,
  "size": 20
}
```

## 站点管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/sites | 站点列表（分页+搜索+排序） |
| POST | /api/v1/sites | 创建站点 |
| GET | /api/v1/sites/{id} | 站点详情（含设备列表） |
| PUT | /api/v1/sites/{id} | 更新站点 |
| DELETE | /api/v1/sites/{id} | 删除站点 |
| GET | /api/v1/sites/{id}/statistics | 站点统计（设备/工单/收益汇总） |

## 设备管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/devices | 设备列表（按站点/类型/状态过滤） |
| POST | /api/v1/devices | 创建设备（含模拟参数） |
| GET | /api/v1/devices/{id} | 设备详情（含实时状态） |
| PUT | /api/v1/devices/{id} | 更新设备 |
| DELETE | /api/v1/devices/{id} | 删除设备 |
| GET | /api/v1/devices/{id}/events | 设备事件时间线（分页） |
| GET | /api/v1/devices/{id}/telemetry | 设备遥测曲线（时序） |
| POST | /api/v1/devices/{id}/command | 设备命令下发 |
| GET | /api/v1/device-types | 设备类型列表 |

## 库存管理

### SKU

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/skus | SKU 列表 |
| POST | /api/v1/skus | 创建 SKU |
| PUT | /api/v1/skus/{id} | 更新 SKU |
| DELETE | /api/v1/skus/{id} | 删除 SKU |

### 仓库库存

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/warehouse/stock | 仓库库存列表 |
| POST | /api/v1/warehouse/inbound | 仓库入库 |
| POST | /api/v1/warehouse/outbound | 仓库出库（关联工单） |
| GET | /api/v1/warehouse/stock/{skuId} | 仓库 SKU 库存详情 |
| POST | /api/v1/warehouse/check | 仓库盘点 |

### 设备库存

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/device-stock | 设备库存列表 |
| GET | /api/v1/device-stock/{deviceId} | 设备库存详情 |
| PUT | /api/v1/device-stock/{id}/correct | 库存校正 |
| GET | /api/v1/stock/predictions | 缺货预测列表 |
| POST | /api/v1/stock/loss | 记录损耗 |

## 工单管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/work-orders | 工单列表（按状态/类型/人员/站点过滤） |
| POST | /api/v1/work-orders | 创建工单（自动或手动） |
| GET | /api/v1/work-orders/{id} | 工单详情 |
| PUT | /api/v1/work-orders/{id}/assign | 派单 |
| PUT | /api/v1/work-orders/{id}/arrive | 到场确认 |
| PUT | /api/v1/work-orders/{id}/process | 处理中 |
| PUT | /api/v1/work-orders/{id}/complete | 完成处理 |
| PUT | /api/v1/work-orders/{id}/review | 复核关闭 |
| PUT | /api/v1/work-orders/{id}/cancel | 取消工单 |

## 派单与路线

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/dispatch/priorities | 计算待处理工单优先级 |
| POST | /api/v1/dispatch/routes | 生成路线建议 |
| GET | /api/v1/dispatch/routes/{id} | 路线详情（含站点顺序+地图坐标） |
| PUT | /api/v1/dispatch/routes/{id} | 人工调整路线/顺序 |
| GET | /api/v1/dispatch/routes/active | 当前执行中的路线 |

## 收益分析

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/revenue/sites | 站点收益排行 |
| GET | /api/v1/revenue/sites/{id} | 站点收益详情（含趋势） |
| GET | /api/v1/revenue/devices | 设备效率统计 |
| GET | /api/v1/revenue/skus | SKU 分析 |
| GET | /api/v1/revenue/overview | 收益总览 |

## AI 助手

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/ai/replenishment-note | 生成补货说明 |
| POST | /api/v1/ai/fault-analysis | 故障归因分析 |
| POST | /api/v1/ai/weekly-report | 生成站点周报 |
| GET | /api/v1/ai/weekly-reports | 周报列表 |

## 看板

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/dashboard/overview | 运营总览数据 |
| GET | /api/v1/dashboard/alerts | 当前告警列表 |
| GET | /api/v1/dashboard/device-stats | 设备状态统计 |
| GET | /api/v1/dashboard/today-tasks | 今日任务概览 |

## 设备模拟器

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/simulator/start | 启动模拟 |
| POST | /api/v1/simulator/stop | 停止模拟 |
| GET | /api/v1/simulator/status | 模拟状态 |

## 订单事件（设备上报专用）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/events/heartbeat | 设备心跳上报 |
| POST | /api/v1/events/inventory | 库存变化上报 |
| POST | /api/v1/events/fault | 故障码上报 |
| POST | /api/v1/events/transaction | 交易事件上报 |
| POST | /api/v1/events/door | 开门事件上报 |
