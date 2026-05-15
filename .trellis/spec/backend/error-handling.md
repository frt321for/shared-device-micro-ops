# Error Handling

> IoT 项目统一错误处理规范

---

## 核心原则

- 所有 API 返回统一错误格式
- 区分用户错误（4xx）和系统错误（5xx）
- 错误信息不能泄露内部实现细节
- IoT 设备错误必须有明确的错误码体系
- 设备接入层的错误不能导致整个服务崩溃

---

## 统一响应格式

```json
// 成功
{
  "code": 0,
  "message": "success",
  "data": { ... },
  "timestamp": "2026-05-03T12:00:00Z",
  "traceId": "abc-123-def"
}

// 错误
{
  "code": 40001,
  "message": "设备不存在",
  "details": "Device ID 1001 not found",
  "timestamp": "2026-05-03T12:00:00Z",
  "traceId": "abc-123-def"
}
```

---

## 错误码体系

### 通用错误码

| 范围 | 类型 | 示例 |
|------|------|------|
| 0 | 成功 | `0` |
| 10000-19999 | 参数错误 | `10001` 参数缺失, `10002` 格式错误 |
| 20000-29999 | 认证/授权 | `20001` 未登录, `20002` 权限不足 |
| 30000-39999 | 业务逻辑 | `30001` 设备不存在, `30002` 设备离线 |
| 40000-49999 | 设备/协议 | `40001` 设备超时, `40002` 协议错误 |
| 50000-59999 | 系统错误 | `50001` 内部错误, `50002` 数据库错误 |

### IoT 专用错误码

| 错误码 | 含义 | 场景 |
|--------|------|------|
| 40001 | 设备超时 | 设备心跳超时未响应 |
| 40002 | 协议错误 | MQTT/HTTP 消息格式不正确 |
| 40003 | 设备离线 | 向离线设备发送命令 |
| 40004 | 命令超时 | 设备未在规定时间内响应命令 |
| 40005 | 数据越界 | 传感器数据超出合理范围 |
| 40006 | 设备冲突 | 同一设备被多次注册 |

---

## 异常处理模式

### 后端异常分层

```
Controller/API 层 → 捕获所有异常，转换为统一响应格式
Service 层        → 抛出业务异常（BusinessException）
Repository 层     → 抛出数据异常（DataException）
设备接入层         → 抛出设备异常（DeviceException），不能崩溃服务
```

### 全局异常处理器

```typescript
app.use((err, req, res, next) => {
  const traceId = req.headers['x-trace-id'] || generateTraceId();

  if (err instanceof BusinessException) {
    return res.status(400).json({
      code: err.code, message: err.message,
      timestamp: new Date().toISOString(), traceId
    });
  }

  // 系统错误不暴露内部细节
  logger.error(`[${traceId}] Internal error:`, err);
  return res.status(500).json({
    code: 50001, message: '系统内部错误',
    timestamp: new Date().toISOString(), traceId
  });
});
```

---

## 设备接入错误处理

### MQTT 消息处理

```typescript
// 所有 MQTT 消息处理必须有 try-catch
// 设备报错不能导致服务崩溃
mqttClient.on('message', async (topic, payload) => {
  try {
    const data = JSON.parse(payload.toString());
    await deviceService.handleMessage(topic, data);
  } catch (err) {
    if (err instanceof SyntaxError) {
      logger.warn(`Invalid JSON from ${topic}: ${payload.toString()}`);
    } else {
      logger.error(`Error processing ${topic}:`, err);
    }
  }
});
```

### 命令下发超时

```typescript
async function sendDeviceCommand(deviceId: string, command: object, timeoutMs = 5000) {
  return Promise.race([
    deviceGateway.send(deviceId, command),
    new Promise((_, reject) =>
      setTimeout(() => reject(new DeviceException(deviceId, 40004, '命令超时')), timeoutMs)
    )
  ]);
}
```

---

## Forbidden Patterns

- **禁止** try-catch 后只打印 console.log 不处理
- **禁止** 返回 500 错误时暴露堆栈信息给前端
- **禁止** 忽略设备上报的异常数据（必须记录日志）
- **禁止** 设备接入层的错误导致整个服务崩溃
- **禁止** 硬编码错误消息（用错误码 + 配置）
