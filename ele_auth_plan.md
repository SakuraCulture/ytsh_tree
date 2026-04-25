# ele 模块认证改造暂缓执行清单

## 当前现状

### 1. 全局白名单直接放开了 ele 模块
配置位置：`yudao-server/src/main/resources/application.yaml:299`

当前存在：

```yaml
yudao:
  security:
    permit-all_urls:
      - /admin-api/ele/**
      - /ele/**
```

这意味着 ele 模块当前大部分接口：
- 不要求 Bearer token
- 不经过正式登录认证

### 2. ele 控制器几乎没有细粒度权限注解
`yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/**`

当前仅发现显式 `@PermitAll`：
- `EleOrderController.java:267-271`
  - `GET /ele/order/list/all`

但更大的问题不是这一个接口，而是全局白名单已将整个 ele 放开。

---

## 本次暂不执行的原因

用户要求：
- 当前同事仍需测试 ele 新增功能
- 避免在测试期间引入认证策略变化
- 因此本次只输出方案与明细，不落代码

---

## 后续建议改造顺序

### 第一阶段：先收口 token 认证
修改：`yudao-server/src/main/resources/application.yaml`

删除：

```yaml
- /admin-api/ele/**
- /ele/**
```

效果：
- ele 所有接口恢复到 Spring Security 默认规则
- 默认要求 Bearer token
- 未登录请求直接被拦截

### 第二阶段：给 ele 控制器补 `@PreAuthorize`
建议按功能拆分权限点：

#### 查询类
- `ele:order:query`

建议覆盖：
- `/ele/order/list`
- `/ele/order/list/remote`
- `/ele/order/detail`
- `/ele/order/status-log/list`
- `/ele/order/fail-record/list`
- `/ele/order/fail-record/page`
- `/ele/order/fail-record/unhandled-count`
- `/ele/order/fail-record/all-failed-ids`
- `/ele/order/sync/progress`
- `/ele/order/sync/config`
- `/ele/status-log/page`
- `/ele/status-log/{id}`
- `/ele/sync-log/page`
- `/ele/sync-log/{id}`

#### 同步类
- `ele:order:sync`

建议覆盖：
- `/ele/order/sync`
- `/ele/order/sync/all`
- `/ele/order/sync/submit`
- `/ele/order-sync/sync-all`
- `/ele/order-sync/sync-range`
- `/ele/order-sync/sync/submit`

#### 重试/补偿类
- `ele:order:retry`

建议覆盖：
- `/ele/order/fail-record/retry`
- `/ele/order/fail-record/retry-with-overwrite`
- `/ele/order/fail-record/batch-retry`
- `/ele/order/fail-record/retry-by-specified-time`

#### 监控查询类
- `ele:monitor:query`

建议覆盖：
- `/ele/traffic/today-stats`
- `/ele/traffic/stats`
- `/ele/traffic/hourly-stats`
- `/ele/traffic/hourly-stats-by-date`
- `/ele/traffic/available-dates`
- `/ele/traffic/record/{traceId}`
- `/ele/thread-pool/status`
- `/ele/thread-pool/status/{poolName}`
- `/ele/thread-pool/alarm-threshold`
- `/ele/thread-pool/health`
- `/ele/order/pool/status`
- `/ele/compensate/pool/stats`
- `/admin-api/ele/datasource/pool-status`
- `/admin-api/ele/datasource/connection-details`
- `/admin-api/ele/datasource/long-running-connections`

#### 监控/运维修改类
- `ele:monitor:update`

建议覆盖：
- `/ele/thread-pool/alarm-threshold` (PUT)
- `/ele/compensate/pool/resize`
- `/ele/traffic/reset`

### 第三阶段：处理是否保留匿名接口
如果确实有少量接口需要给：
- 外部系统
- 第三方平台
- 联调回调

建议：
- 单独路径隔离
- 单独签名机制
- 不与后台管理接口共用 `/ele/**`

---

## 涉及的主要文件

### 配置
- `yudao-server/src/main/resources/application.yaml`

### 控制器
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleOrderController.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleOrderSyncController.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleOrderStatusLogController.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleOrderSyncLogController.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleTrafficMonitorController.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/ThreadPoolController.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleCompensatePoolController.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleOrderPoolStatusController.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleDataSourceMonitorController.java`

---

## 建议的实施时机

在同事完成 ele 新增功能测试后，再统一做：
1. 删除 ele 白名单
2. 一次性补全 `@PreAuthorize`
3. 补菜单/权限资源 SQL
4. 前端联调 401/403 场景
5. 给测试账号授权
