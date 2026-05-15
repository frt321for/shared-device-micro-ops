# 架构设计 — 共享设备微运营平台

## 整体架构

```
┌──────────────────────────────────────────────────────────┐
│                    用户浏览器 (React)                       │
│          运营总览 / 站点详情 / 工单 / 地图 / AI 周报          │
└───────────────────────┬──────────────────────────────────┘
                        │ HTTP REST API + WebSocket
┌───────────────────────▼──────────────────────────────────┐
│                Spring Boot 3 (Java 21)                      │
│  ┌────────┐ ┌──────────┐ ┌─────────┐ ┌────────┐ ┌─────┐ │
│  │  Site  │ │  Device  │ │Inventory│ │Workorder│ │ AI  │ │
│  │ Module │ │  Module  │ │ Module  │ │ Module  │ │Module│ │
│  └───┬────┘ └────┬─────┘ └────┬────┘ └────┬────┘ └──┬──┘ │
│      │           │            │           │          │     │
│  ┌───▼───────────▼────────────▼───────────▼──────────▼─┐  │
│  │                Service Layer                        │   │
│  └───────────┬───────────────────────────┬─────────────┘  │
│              │                           │                 │
│  ┌───────────▼─────────┐    ┌───────────▼──────────────┐  │
│  │  Repository Layer   │    │  Gateway / MQTT          │  │
│  │  (PostgreSQL +      │    │  (Device Ingress)        │  │
│  │   TimescaleDB +     │    │  Mosquitto MQTT Broker   │  │
│  │   Redis)            │    │                          │  │
│  └───────────┬─────────┘    └───────────┬──────────────┘  │
└──────────────┼──────────────────────────┼─────────────────┘
               │                          │
               │               ┌──────────▼──────────┐
               │               │  Device Simulator   │
               │               │  (Spring Boot)      │
               │               │  咖啡机 / 零食柜 /    │
               │               │  洗衣机 / 自提柜 /    │
               │               │  共享冰箱            │
               │               └─────────────────────┘
               │
     ┌─────────┴──────────────────────────────────┐
     │            Aliyun Docker                    │
     │  ┌──────────┐ ┌──────────┐ ┌────────────┐  │
     │  │PostgreSQL│ │TimescaleDB│ │   Redis    │  │
     │  │:5432→15432│ │:5432→15432│ │:6379→16379│  │
     │  └──────────┘ └──────────┘ └────────────┘  │
     │  ┌──────────┐                               │
     │  │ Mosquitto│                               │
     │  │:1883→11883│                               │
     │  └──────────┘                               │
     └─────────────────────────────────────────────┘
```

## 三库分离

| 数据类型 | 存储引擎 | 用途 | 示例 |
|---------|---------|------|------|
| 实时状态 | Redis | 设备在线/离线、最新心跳、当前工单 | 设备最后上报时间、当前库存余量 |
| 业务事实 | PostgreSQL | 站点/设备/工单/用户/SKU/收益 | 站点档案、工单流转记录 |
| 时序事件 | TimescaleDB | 心跳、库存曲线、故障码、交易事件 | 设备遥测时序、库存变化曲线 |

## 模块结构

```
src/
├── main/java/com/iot/ops/
│   ├── application/              # 应用入口 + 配置
│   │   ├── OpsApplication.java
│   │   └── config/
│   │       ├── WebConfig.java
│   │       ├── SecurityConfig.java
│   │       ├── RedisConfig.java
│   │       ├── MqttConfig.java
│   │       └── DataSourceConfig.java
│   ├── modules/
│   │   ├── site/                 # 站点管理
│   │   │   ├── api/SiteController.java
│   │   │   ├── service/SiteService.java
│   │   │   ├── domain/Site.java
│   │   │   ├── domain/DeviceGroup.java
│   │   │   └── repository/SiteRepository.java
│   │   ├── device/               # 设备管理
│   │   │   ├── api/DeviceController.java
│   │   │   ├── service/DeviceService.java
│   │   │   ├── domain/Device.java
│   │   │   ├── domain/DeviceType.java
│   │   │   └── repository/DeviceRepository.java
│   │   ├── inventory/            # 库存管理
│   │   │   ├── api/SkuController.java
│   │   │   ├── api/WarehouseController.java
│   │   │   ├── api/DeviceStockController.java
│   │   │   ├── service/InventoryService.java
│   │   │   ├── domain/Sku.java
│   │   │   ├── domain/WarehouseStock.java
│   │   │   ├── domain/DeviceStock.java
│   │   │   └── repository/
│   │   ├── workorder/            # 工单管理
│   │   │   ├── api/WorkOrderController.java
│   │   │   ├── service/WorkOrderService.java
│   │   │   ├── domain/WorkOrder.java
│   │   │   ├── domain/WorkOrderStatus.java
│   │   │   └── repository/WorkOrderRepository.java
│   │   ├── dispatch/             # 派单 + 路线
│   │   │   ├── api/DispatchController.java
│   │   │   ├── service/DispatchService.java
│   │   │   ├── service/RouteService.java
│   │   │   ├── domain/Route.java
│   │   │   └── domain/DispatchPriority.java
│   │   ├── revenue/              # 收益分析
│   │   │   ├── api/RevenueController.java
│   │   │   ├── service/RevenueService.java
│   │   │   ├── domain/OrderEvent.java
│   │   │   └── domain/SiteRevenue.java
│   │   ├── ai/                   # AI 助手
│   │   │   ├── api/AiController.java
│   │   │   ├── service/AiService.java
│   │   │   ├── service/WeeklyReportService.java
│   │   │   └── client/ModelScopeClient.java
│   │   └── dashboard/            # 看板聚合
│   │       ├── api/DashboardController.java
│   │       └── service/DashboardService.java
│   ├── infra/
│   │   ├── mqtt/                 # MQTT 接入网关
│   │   │   ├── MqttGateway.java
│   │   │   ├── MqttMessageHandler.java
│   │   │   └── DeviceEventPublisher.java
│   │   ├── cache/                # Redis 封装
│   │   │   ├── DeviceCache.java
│   │   │   └── SessionCache.java
│   │   ├── db/                   # 多数据源配置
│   │   │   ├── TimescaleConfig.java
│   │   │   └── FlywayConfig.java
│   │   └── security/
│   │       ├── JwtFilter.java
│   │       ├── UserContext.java
│   │       └── Role.java
│   └── common/
│       ├── exception/
│       │   ├── BusinessException.java
│       │   └── GlobalExceptionHandler.java
│       ├── util/
│       │   ├── PageUtils.java
│       │   └── DistanceCalc.java
│       └── config/
│           └── JacksonConfig.java
├── simulator/                    # 设备模拟器
│   ├── SimulatorApplication.java
│   ├── device/
│   │   ├── AbstractDeviceSimulator.java
│   │   ├── CoffeeMachineSimulator.java
│   │   ├── SnackCabinetSimulator.java
│   │   └── WasherSimulator.java
│   ├── protocol/
│   │   └── MqttPublisher.java
│   └── config/
│       └── SimulatorConfig.java
└── resources/
    ├── application.yml
    ├── application-dev.yml
    ├── application-prod.yml
    └── db/migration/
        ├── V1__init_site_device.sql
        ├── V2__init_inventory.sql
        ├── V3__init_workorder.sql
        ├── V4__init_revenue.sql
        └── V5__init_ai_audit.sql
```

## 设备状态机

```
未启用 → 在线 → 缺货预警 → 缺货 → 故障 → 维护中 → 恢复在线 → 停用
```

## 工单状态机

```
待生成 → 待派单 → 已派单 → 已到场 → 处理中 → 待复核 → 已关闭 / 已取消
```

## 库存状态机

```
充足 → 低库存 → 即将售罄 → 已售罄 → 待补货 → 已补货
```

## 状态约束

- 已停用设备不能生成新的补货任务
- 已关闭工单必须包含处理结果、到场时间和关闭人
- 库存人工校正必须记录原因，不能覆盖历史销量
- 路线确认后修改执行顺序必须留痕（审计日志）
