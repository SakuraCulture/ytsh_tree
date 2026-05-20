# Ele 门店商品与库存同步首期健壮性加固设计

## 1. 背景与目标

本次设计覆盖两条链路：

- `yudao-module-ele` 门店商品全量同步链路
- `yudao-module-ele` 门店库存批量同步链路

目标不是一次性把两条链路重构成订单同步那种完整的异步补偿体系，而是在**第一阶段**优先补齐最危险的防线，避免以下问题继续污染正式数据：

- 同一门店被不同任务并发执行，产生重复写或互相覆盖
- `platformStoreId / merchantCode / erpStoreCode / storeId` 混用不一致，导致串店写入
- 任务在 `PENDING` / `RUNNING` 状态半挂，后续任务继续叠加执行
- 上游返回缺行、局部失败或执行中断后，只留日志不进入恢复路径

第一阶段额外要求：

- 标识冲突时必须**硬拦截并留痕**
- 本阶段**不做 schema 变更**
- 交付时必须附带**未处理问题清单**，明确后续阶段承接项

## 2. 非目标

第一阶段明确不做：

- 正式表唯一键、索引或其他 schema 级兜底
- Kafka / DLQ / 异步重试总线式闭环
- 差异率重拉、严重异常自动停机、订单链路级别的完整补偿体系
- 影子/治理平台化重构

## 3. 方案选择

本次采用“**方案 B：先堵高风险，再补轻量失败闭环**”。

含义如下：

1. 先补执行期同店互斥、门店标识一致性校验、正式写保护、任务恢复。
2. 在此基础上，为商品单 SKU 失败、库存缺行、任务半挂等场景补一层**轻量恢复机制**。
3. 恢复机制先基于库表与定时扫描实现，不引入新的消息基础设施。

选择原因：

- 比方案 A 更接近“可恢复”的业务期望，避免当前只留痕不补回。
- 比方案 C 更可控，能把首期范围压在本地同步体系内，不额外拉起新的异步平台复杂度。

## 4. 总体设计边界

### 4.1 商品链路新增责任点

1. **任务执行互斥层**
   - 在 `EleStoreGoodsFullSyncExecutorImpl` 单店执行入口增加 `platformStoreId` 级执行锁。
2. **标识校验层**
   - 在商品请求构造与落地前，统一校验 `merchantCode / erpStoreCode / platformStoreId / storeId` 是否指向同一门店。
3. **正式写保护层**
   - 在 `StoreProductSyncWriteServiceImpl` 外围增加更强的代码侧防重与冲突处理。
4. **轻量补偿层**
   - 将商品分页中的单 SKU 失败从“只记日志”升级为“失败留痕 + 后续恢复入口”。

### 4.2 库存链路新增责任点

1. **任务执行互斥层**
   - 在 `EleStoreInventoryBatchExecutorImpl` 单店执行入口增加 `platformStoreId` 级执行锁。
2. **标识校验层**
   - 收口到 `EleSkuInventoryQueryServiceImpl.normalize(...)` 前后，禁止混用互相对不上的门店标识。
3. **正式写保护层**
   - 在正式库存写入前再次校验本地门店商品归属。
4. **缺行补偿层**
   - 对“请求过但上游没返回”的 SKU 从纯 `errorDetails` 提升为失败留痕与后续恢复。
5. **任务恢复层**
   - 对超时 `PENDING/RUNNING` 任务增加恢复扫描。

### 4.3 共用抽象

本阶段不抽大框架，只抽 3 个薄组件：

- `StoreExecutionGuard`
- `StoreIdentityValidator`
- `SyncTaskRecoveryService`

目标是让商品与库存共用关键防护能力，但不把第一阶段做成大规模基础设施重构。

## 5. 商品同步设计

### 5.1 任务创建

保留现有的任务创建锁与任务表模式：

- `CURRENT_STORE`
- `ALL_OPEN_STORES`

本阶段不推翻创建路径，只在执行期补防线。

### 5.2 单店执行互斥

在 `EleStoreGoodsFullSyncExecutorImpl.executeStoreTask(...)` 进入单店分页前，按 `platformStoreId` 获取执行锁。

行为要求：

- 同一门店同一时刻只允许一个商品全量执行流进入。
- `CURRENT_STORE` 与 `ALL_OPEN_STORES` 之间也必须共享这把执行锁。
- 获取锁失败时，不允许继续写入，需记录明确的互斥失败原因。

### 5.3 门店标识一致性校验

在 `EleStoreGoodsSyncServiceImpl.buildSyncReq(...)` 与 `doSyncStoreGoods(...)` 周围统一校验：

- 任务侧：`merchantCode / erpStoreCode / platformStoreId`
- 上游返回侧：`merchantCode / storeCode`
- 本地解析侧：`storeId / platformStoreId / settlementAccount`

校验结论分 3 类：

1. **一致**：继续处理
2. **缺字段但可唯一补齐**：补齐后继续
3. **存在冲突**：硬拦截，不允许进入正式写

### 5.4 标识冲突处理

当出现 `STORE_IDENTITY_MISMATCH` 时：

- 不写正式门店商品
- 写同步日志，错误码为 `STORE_IDENTITY_MISMATCH`
- 写失败记录，保留任务号、页码、SKU、请求/响应快照
- 只有在归属仍然明确时才允许写影子/治理；如果归属本身不可信，只写失败记录，不扩散到影子池

### 5.5 正式写保护

`StoreProductSyncWriteServiceImpl.upsertStoreProduct(...)` 当前是“先查再插/更”。

本阶段在不改 schema 的前提下，外围补以下保护：

- 单店执行锁先降低并发重复插概率
- 正式写前再次确认该 `storeId + productSkuId` 的现有关系
- 发现已有正式关系时优先走更新，不重复插入
- 发生冲突时明确留痕，避免静默失败

本阶段承认代码侧防重不能替代数据库唯一约束，因此该项会被列入后续遗留清单。

### 5.6 商品轻量失败闭环

当前商品分页中的单 SKU 失败只做 `warn`。

本阶段改为：

- 单 SKU 失败写失败记录
- 门店任务聚合状态可进入 `PARTIAL_FAIL`
- 由轻量恢复任务对失败 SKU 做有限重放
- 超过重试阈值后停留在人工可见状态，不无限循环

## 6. 库存同步设计

### 6.1 单店执行互斥

在 `EleStoreInventoryBatchExecutorImpl.executeStoreTask(...)` 进入单店库存批次前，按 `platformStoreId` 获取执行锁。

行为要求与商品链路一致：

- 同店同刻仅允许一个库存批次执行流
- `CURRENT_STORE` 与 `ALL_OPEN_STORES` 共享同一执行锁
- 获取锁失败后必须留痕并退出，不允许带着并发继续写库存

### 6.2 `normalize(...)` 收口为强校验入口

`EleSkuInventoryQueryServiceImpl.normalize(...)` 将成为库存链路的门店标识校验总入口。

规则如下：

1. **只给 `platformStoreId`**
   - 允许从本地补齐 `merchantCode / erpStoreCode / storeId`
2. **只给 `merchantCode + erpStoreCode`**
   - 允许按唯一映射反查本地门店
3. **两套标识都给了**
   - 必须校验最终映射到同一条门店平台关系
4. **无法证明一致**
   - 直接失败，不允许进入正式写

### 6.3 正式库存写保护

在 `EleStoreInventoryIngestServiceImpl.ingest(...)` / `upsertFormalStock(...)` 前后增加二次归属保护：

- `storeId` 必须存在且可信
- 本地 `storeProduct` 必须属于该 `storeId`
- 待写库存行的 SKU 必须与该门店商品关系一致

只要任一层不能证明归属一致，就不写正式库存。

### 6.4 标识冲突处理

当库存链路发现标识冲突时：

- 任务门店维度进入 `FAILED` 或 `PARTIAL_FAIL`
- 记录统一错误码 `STORE_IDENTITY_MISMATCH`
- 保留原始请求标识组合与上游响应摘要
- 不写正式表
- 只有在归属明确时才允许影子/治理落地；归属不可信时只写失败记录

### 6.5 缺行补偿

当前对“请求了但上游没返回”的 SKU 只追加 `ERROR_INVENTORY_ROW_MISSING:*`。

本阶段改为：

- 缺行进入失败记录
- 缺行计入门店任务失败统计
- 缺行进入后续轻量恢复队列
- 重试仍缺行时进入人工可见状态

### 6.6 解决“正式库存静默不更新”放大风险

现有缓存命中路径可能导致 `storeId` 不完整，进一步造成正式库存写入直接退化为影子/治理。

本阶段的原则是：

- 不允许在 `storeId` 不可信的情况下尝试正式写
- 必须把“为什么没有进入正式写”记录成显式结果，而不是仅靠行为侧推断
- 对“缓存命中但归属字段不全”的情况输出明确错误原因，进入失败恢复路径

## 7. 共用组件设计

### 7.1 `StoreExecutionGuard`

职责：

- 提供按 `platformStoreId` 的单店执行锁
- 封装获取失败、释放、超时日志
- 统一商品/库存两条链路的互斥失败码

要求：

- 只负责执行期互斥
- 不替代现有任务创建锁
- 不混入业务数据处理逻辑

### 7.2 `StoreIdentityValidator`

职责：

- 接收不同来源的门店标识集合
- 输出 3 种结论：可继续、可补齐后继续、必须硬失败
- 统一生成失败原因与日志字段

要求：

- 可独立单测
- 不直接发起上游请求
- 不直接落库

### 7.3 `SyncTaskRecoveryService`

职责：

- 扫描商品/库存的超时 `PENDING/RUNNING` 任务
- 判断是“重新提交”还是“转失败”
- 写恢复日志，避免静默恢复

要求：

- 仅做轻量恢复
- 不负责完整重放平台化
- 超过阈值后停止自动恢复并转人工可见状态

## 8. 轻量失败闭环设计

本阶段统一的失败闭环原则：

1. 有失败必须有结构化留痕
2. 可恢复的失败进入有限重试
3. 不可证明安全的数据不得为了“成功率”强行落正式表
4. 自动恢复有上限，超过阈值后转人工可见

首期纳入闭环的失败类型：

- 商品单 SKU 执行失败
- 商品门店标识冲突
- 库存门店标识冲突
- 库存缺行
- 任务提交后丢执行
- 任务执行中断导致长时间 `RUNNING`

首期不纳入的失败类型：

- 全量平台级差异对账
- 大规模历史回灌补偿
- 跨天长链路自动追平

## 9. 任务恢复设计

### 9.1 恢复对象

- 商品全量任务：`PENDING` / `RUNNING` 超时
- 库存批量任务：`PENDING` / `RUNNING` 超时
- 门店级子任务：长时间停在运行中但无进度变化

### 9.2 恢复动作

按类型处理：

- **提交丢失类**：重新 submit
- **执行中断类**：转失败并保留恢复日志
- **可安全重放类**：进入轻量恢复队列

### 9.3 恢复保护

恢复前仍需通过：

- 执行锁检查
- 标识一致性检查
- 当前任务状态再确认

防止恢复器自己变成并发污染源。

## 10. 测试策略

### 10.1 标识一致性测试

覆盖以下情况：

- 仅 `platformStoreId`，可正确补齐
- 仅 `merchantCode + erpStoreCode`，可正确反查
- 两套标识一致，可继续
- 两套标识冲突，必须硬拦截
- 缺字段但可唯一补齐
- 缺字段且无法证明一致，必须失败

### 10.2 执行互斥测试

覆盖以下情况：

- 同店 current-store 与 all-open 并发，仅一个执行流进入
- 同店恢复任务与正常任务并发，仅一个执行流进入
- 不同门店允许并行

### 10.3 正式写保护测试

覆盖以下情况：

- 标识冲突绝不写正式表
- 合法输入可更新正式表
- 正式表已存在时优先更新
- 归属不明确时转失败留痕或影子/治理

### 10.4 恢复与补偿测试

覆盖以下情况：

- `PENDING` 超时任务恢复
- `RUNNING` 半挂任务恢复
- 商品单 SKU 失败进入恢复路径
- 库存缺行进入恢复路径
- 超过重试阈值后停在人工可见状态

## 11. 第一阶段验收标准

第一阶段完成后，必须满足：

1. 同一门店不能被两条商品/库存任务同时执行
2. 混用且不一致的门店标识必须被硬拦截并留痕
3. 冲突场景下正式表不发生写入
4. 商品单 SKU 失败、库存缺行、任务半挂都能进入恢复路径
5. 任务最终状态能区分 `SUCCESS / PARTIAL_FAIL / FAILED`
6. 输出一份未处理问题清单，明确第二阶段承接项

## 12. 第一阶段遗留清单

本阶段结束后，需要明确保留以下后续项：

1. 正式表 schema 级唯一性兜底
2. 商品治理池去重/刷新语义加强
3. Kafka / DLQ 异步失败闭环
4. 差异率重拉与严重异常停机保护
5. 更完整的影子/治理处理平台能力

## 13. 受影响的主要文件

商品链路：

- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsSyncServiceImpl.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsFullSyncServiceImpl.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/executor/EleStoreGoodsFullSyncExecutorImpl.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsGovernanceServiceImpl.java`
- `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductSyncWriteServiceImpl.java`

库存链路：

- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryQueryServiceImpl.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryIngestServiceImpl.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryBatchServiceImpl.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/executor/EleStoreInventoryBatchExecutorImpl.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryShadowServiceImpl.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryGovernanceServiceImpl.java`

共用能力建议新增位置：

- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/guard/StoreExecutionGuard.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/validator/StoreIdentityValidator.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/recovery/SyncTaskRecoveryService.java`

## 14. 实施原则

- 先堵高风险，再补轻量闭环
- 无法证明安全归属时，宁可失败，不写正式表
- 共用能力只抽薄层，不借首期加固推动无关重构
- 不以”成功率”换数据污染风险
- 所有未覆盖的剩余风险必须写入交接清单

## 15. 第一阶段实施后补充（2026-05-20）

### 15.1 实施完成状态

本次第一阶段已完成以下改动：

1. **标识校验器** (`StoreIdentityValidator`)
   - 实现多源门店标识归一化校验
   - 支持 `platformStoreId` 单源补齐、`merchantCode+erpStoreCode` 反查、双套标识一致性校验
   - 冲突���硬拦截并返回 `REJECT` 决策

2. **执行锁守卫** (`StoreExecutionGuard`)
   - 基于 Redisson 实现 `platformStoreId` 级执行锁
   - 商品/库存两条链路共用同一锁命名空间
   - 获取失败时抛出明确异常并留痕

3. **商品链路加固**
   - 商品同步请求增加标识一致性校验
   - 正式写前二次确认门店归属
   - 执行器入口增加执行锁

4. **库存链路加固**
   - `EleSkuInventoryQueryServiceImpl.normalize()` 收口为强校验入口
   - 缺行/完整性异常统一计入 `failureCount`
   - 正式库存写前二次确认 `storeProduct` 归属

5. **任务恢复服务** (`SyncTaskRecoveryService` + `EleSyncTaskRecoveryJob`)
   - 扫描超时 `PENDING` / `RUNNING` 任务
   - `PENDING` 超时重新 submit，`RUNNING` 超时转失败
   - 未完成门店子任务批量失败

6. **并发收口增强**
   - 主任务 mapper 新增原子 claim (`markRunningIfPending`)
   - recovery 仅条件失败 (`markFailedIfRunning`)
   - executor progress/finish 仅在 `RUNNING` 时写回
   - 防止 duplicate submit 与 recovery/executor 终态互踩

### 15.2 测试证据

总回归结果（2026-05-20）：

```
mvn -pl yudao-module-ele -Dtest=StoreIdentityValidatorTest,StoreExecutionGuardTest,EleStoreGoodsSyncServiceImplTest,EleSkuInventoryQueryServiceImplTest,EleStoreInventoryIngestServiceImplTest,EleStoreGoodsFullSyncExecutorImplTest,EleStoreInventoryBatchExecutorImplTest,SyncTaskRecoveryServiceTest test

Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

mvn -pl yudao-module-business -Dtest=StoreProductSyncWriteServiceImplTest test

Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 15.3 实施后新增遗留项

以下为实施过程中发现、但第一阶段范围内未收口的残余注意点：

1. **库存侧缺少并发 submit 单测**
   - 商品 executor 已覆盖 duplicate submit claim 收口
   - 库存 executor 对称实现未补同类并发单测
   - 影响：排障时缺少对 inventory claim 路径的直接回归证据

2. **条件更新返回 0 时可观测性一般**
   - recovery/executor 条件更新失败多为静默跳过
   - 主要依赖日志推断排障
   - 建议：后续可考虑增加结构化留痕或指标暴露

3. **恢复服务超时阈值配置未验证真实生效**
   - 单测已校验 cutoff 计算，但未跑真实 job 场景
   - 建议：上线前在 staging 环境验证 `pending-timeout-minutes` / `running-timeout-minutes` 实际生效

### 15.4 第二阶段承接项（含原设计遗留）

综合原设计清单与实施后新增：

1. 正式表 schema 级唯一性兜底
2. 商品治理池去重/刷新语义加强
3. Kafka / DLQ 异步失败闭环
4. 差异率重拉与严重异常停机保护
5. 更完整的影子/治理处理平台能力
6. 库存 executor 并发 submit 单测补齐
7. 条件更新失败可观测性增强
8. 恢复服务超时阈值真实生效验证
