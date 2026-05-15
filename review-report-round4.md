# 代码审查报告 — 第 4 轮 (Git Diff)

**审查范围**: `git diff HEAD~1..HEAD` (提交 `1ab9773`)  
**提交信息**: `fix: remaining P2 items - AI retry, DeviceGroup, frontend tests`  
**变更文件**: 9 个文件，+916 / -38 行

---

## 变更概览

| 文件 | 变更类型 | 行数 | 说明 |
|------|----------|------|------|
| `AiService.java` | 修改 | +93/-38 | AI 重试机制 + 配置化 |
| `DeviceGroupController.java` | 新增 | +42 | 设备组 API |
| `DeviceGroupService.java` | 新增 | +42 | 设备组业务逻辑 |
| `DeviceGroupRepository.java` | 新增 | +9 | 设备组仓库 |
| `package.json` | 修改 | +9/-1 | 添加测试依赖 |
| `vitest.config.ts` | 新增 | +11 | 测试配置 |
| `setup.ts` | 新增 | +1 | 测试 setup |
| `Placeholder.test.tsx` | 新增 | +7 | 占位测试 |
| `pnpm-lock.yaml` | 修改 | +740 | 依赖锁定 |

---

## 一、AiService.java 变更审查

### 改进点（正面）

| 改进 | 说明 | 评价 |
|------|------|------|
| 配置化 AI 参数 | `ai.base-url` 和 `ai.model` 从配置读取 | ✅ 正确做法 |
| 复用 RestTemplate | `@PostConstruct` 初始化一次，不再每次创建 | ✅ 性能优化 |
| 重试机制 | 最多 3 次重试，间隔 1 秒 | ✅ 提高可用性 |
| 正确处理中断 | `Thread.currentThread().interrupt()` | ✅ 规范处理 |

### 问题

| 严重度 | 问题 | 位置 | 建议 |
|--------|------|------|------|
| **HIGH** | 重试只在异常时触发，HTTP 5xx 响应未重试 | 第 249-260 行 | 检查 `response.getStatusCode().is5xxServerError()` |
| **MEDIUM** | `Thread.sleep(1000)` 在同步线程中阻塞 | 第 265 行 | 考虑指数退避或使用 `CompletableFuture` |
| **MEDIUM** | API key 仍从环境变量读取，未从 Spring 配置注入 | 第 221-226 行 | 统一使用 `@Value("${ai.api-key}")` |
| **LOW** | `callAiApi` 方法过长（54 行） | 第 220-274 行 | 可拆分为 `buildRequest()` + `executeWithRetry()` |

### 建议代码改进

```java
// 当前：只在异常时重试
for (int attempt = 1; attempt <= 3; attempt++) {
    try {
        ResponseEntity<Map> response = restTemplate.exchange(...);
        // TODO: 检查 HTTP 状态码
        return extractContent(response);
    } catch (Exception e) {
        log.warn("...");
        Thread.sleep(1000);  // 阻塞
    }
}

// 建议：增加 HTTP 状态码检查 + 指数退避
for (int attempt = 1; attempt <= 3; attempt++) {
    try {
        ResponseEntity<Map> response = restTemplate.exchange(...);
        if (response.getStatusCode().is5xxServerError()) {
            throw new RuntimeException("Server error: " + response.getStatusCode());
        }
        return extractContent(response);
    } catch (Exception e) {
        long delay = (long) Math.pow(2, attempt) * 500; // 1s, 2s, 4s
        Thread.sleep(Math.min(delay, 5000));
    }
}
```

---

## 二、DeviceGroup 模块审查

### DeviceGroupController.java

| 问题 | 严重度 | 说明 |
|------|--------|------|
| 使用 `Map<String, Object>` 接收参数 | MEDIUM | 缺少类型安全，应使用 DTO 或 `@RequestBody` 注解 |
| 无分页支持 | LOW | `findAll()` 返回全部数据，数据量大时有性能问题 |
| 无 `@Valid` 校验 | MEDIUM | 缺少输入验证 |
| 无 PUT 更新接口 | LOW | 只有 Create 和 Delete，无 Update |
| 无审计日志 | LOW | 删除操作未记录 |

**建议**:
```java
// 当前
@PostMapping
public ApiResponse<DeviceGroup> create(@RequestBody Map<String, Object> body) {
    String name = (String) body.get("name");
    // 手动解析...
}

// 建议：使用 DTO
public record DeviceGroupRequest(
    @NotBlank String name,
    @NotNull Long siteId,
    String description
) {}

@PostMapping
public ApiResponse<DeviceGroup> create(@Valid @RequestBody DeviceGroupRequest request) {
    return ApiResponse.success(deviceGroupService.create(request));
}
```

### DeviceGroupService.java

| 问题 | 严重度 | 说明 |
|------|--------|------|
| `create()` 无重复检查 | MEDIUM | 同一站点可能创建同名分组 |
| `delete()` 未检查关联设备 | HIGH | 删除分组时未处理关联的 DeviceGroupMember |
| 无事务注解 | MEDIUM | 删除操作应加 `@Transactional` |

### DeviceGroupRepository.java

✅ 实现正确，接口简洁。

---

## 三、前端测试基础设施审查

### vitest.config.ts

✅ 配置正确：
- 使用 `jsdom` 环境
- 配置了 `setup.ts`
- 启用 `globals: true`

### package.json 新增依赖

| 依赖 | 版本 | 用途 | 评价 |
|------|------|------|------|
| `vitest` | ^4.1.6 | 测试框架 | ✅ 现代选择 |
| `@testing-library/react` | ^16.3.2 | React 组件测试 | ✅ 标准库 |
| `@testing-library/jest-dom` | ^6.9.1 | DOM 断言 | ✅ 常用 |
| `jsdom` | ^29.1.1 | DOM 环境 | ✅ 必需 |

### Placeholder.test.tsx

```typescript
describe('App', () => {
  it('should pass a placeholder test', () => {
    expect(1 + 1).toBe(2)
  })
})
```

| 问题 | 严重度 | 说明 |
|------|--------|------|
| 占位测试无实际价值 | LOW | 应删除或替换为真实测试 |
| 测试文件放在 `src/test/` | INFO | 符合项目约定 |
| 未测试任何组件 | MEDIUM | 无回归保障 |

---

## 四、整体评估

### 正面评价

| 方面 | 说明 |
|------|------|
| AI 重试机制 | 提高了 AI 服务的可用性 |
| 配置化改进 | AI 参数从配置读取，不再硬编码 |
| RestTemplate 复用 | 性能优化，减少资源消耗 |
| 测试基础设施 | vitest + testing-library 是正确选择 |
| DeviceGroup 补全 | 填补了之前审查报告指出的缺失 |

### 需要改进

| 优先级 | 问题 | 影响 |
|--------|------|------|
| P0 | DeviceGroup 删除未检查关联设备 | 数据完整性风险 |
| P1 | AI 重试未检查 HTTP 5xx | 服务端错误不重试 |
| P1 | DeviceGroupController 缺少输入校验 | 恶意输入风险 |
| P2 | 占位测试应替换为真实测试 | 测试覆盖率无提升 |
| P2 | API key 未统一从配置注入 | 配置管理不一致 |

---

## 五、与上次审查对比

| 上次发现的问题 | 本次状态 | 说明 |
|----------------|----------|------|
| AI Service 每次创建 RestTemplate | ✅ 已修复 | 改为 @PostConstruct 初始化 |
| AI 无重试机制 | ✅ 已修复 | 添加 3 次重试 |
| DeviceGroup 无 Service/Controller | ✅ 已修复 | 新增完整模块 |
| 前端零测试覆盖 | ⚠️ 部分修复 | 添加了测试基础设施，但无真实测试 |
| 种子数据密码无法登录 | ❌ 未修复 | 本次未涉及 |
| JWT 密钥随机生成 | ❌ 未修复 | 本次未涉及 |

---

## 六、建议的下一步

1. **立即**: 为 DeviceGroup 添加 `@Valid` 校验和删除前检查
2. **短期**: 为 AiService 添加 HTTP 状态码检查
3. **短期**: 添加 DeviceGroup 的单元测试
4. **中期**: 添加前端页面的真实测试（至少覆盖登录、站点管理）
5. **中期**: 修复种子数据密码和 JWT 密钥问题

---

*审查完成时间: 2026-05-15*  
*审查方式: git diff HEAD~1..HEAD + 文件内容审查*
