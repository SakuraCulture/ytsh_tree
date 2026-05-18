# 门店库存批量导入与批量/定时拉取 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为饿了么门店库存补齐独立的批量导入、后台批量任务和定时拉取能力，并让 Excel 导入与 API 拉取复用同一条正式/影子/治理写入链路。

**Architecture:** 先把当前已完成但未提交的库存查询基线同步到隔离 worktree，再从 `EleSkuInventoryQueryServiceImpl` 中抽出统一 ingest 服务。Excel 导入、手工批量任务和定时任务都先转换成标准库存行，交给 ingest 服务完成正式库存更新、影子库存写入和治理池刷新；批量任务沿用现有门店商品全量同步的任务表、门店级并发、任务聚合和锁防重模式。

**Tech Stack:** Java 17, Spring Boot 3, MyBatis Plus, Redisson, ThreadPoolTaskExecutor, Quartz/Spring Scheduler, Vue 3, TypeScript, Element Plus, pnpm, Maven.

---

## File Structure

### Current committed vs working-tree baseline

当前 `master` 最近提交的 worktree **不包含** 你主工作区里已经完成的库存查询基线文件，但实施必须建立在这组文件之上：

- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleStoreInventoryController.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleSkuInventoryBatchQueryReqVO.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/dataobject/EleStoreInventoryShadowDO.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/dataobject/EleStoreInventoryGovernancePoolDO.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/mysql/EleStoreInventoryShadowMapper.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/mysql/EleStoreInventoryGovernancePoolMapper.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryQueryService.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryQueryServiceImpl.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryShadowService.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryShadowServiceImpl.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryGovernanceService.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryGovernanceServiceImpl.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/bo/EleSkuInventoryBatchQueryReqBO.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/bo/EleSkuInventoryShadowUpsertReqBO.java`
- `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/dto/EleSkuInventoryBatchQueryRespDTO.java`
- `yudao-module-ele/src/main/resources/sql/add_ele_inventory_shadow_tables.sql`
- `ytsh-ui-vue3/src/api/ele/storeInventory/index.ts`
- `ytsh-ui-vue3/src/views/ele/store-inventory/index.vue`
- `ytsh-ui-vue3/src/views/ele/storeInventoryPageLogic.ts`
- `ytsh-ui-vue3/src/views/business/store-product/components/ShadowInventoryExpandCard.vue`
- `ytsh-ui-vue3/src/views/ele/components/InventoryMetricsCard.vue`

### New backend inventory ingest and task files

- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/bo/EleStoreInventoryIngestRowBO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryIngestService.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryIngestServiceImpl.java`
- Create: `yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryIngestServiceImplTest.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/dataobject/EleStoreInventoryBatchTaskDO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/dataobject/EleStoreInventoryBatchTaskStoreDO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/mysql/EleStoreInventoryBatchTaskMapper.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/mysql/EleStoreInventoryBatchTaskStoreMapper.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventorySkuScopeService.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventorySkuScopeServiceImpl.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryBatchTaskService.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryBatchTaskServiceImpl.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/executor/EleStoreInventoryBatchExecutor.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/executor/EleStoreInventoryBatchExecutorImpl.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/config/EleStoreInventorySchedulerProperties.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/job/EleStoreInventoryAutoSyncJob.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/config/EleOrderExecutorConfiguration.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/redis/EleLockKeyConstants.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/redis/EleOrderLockService.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/redis/EleOrderLockServiceImpl.java`
- Modify: `yudao-module-ele/src/main/resources/sql/add_ele_inventory_shadow_tables.sql`
- Modify: `yudao-module-business/src/test/resources/sql/create_tables.sql`
- Modify: `yudao-module-business/src/test/resources/sql/clean.sql`

### New backend import and controller files

- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryImportExcelVO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryImportRespVO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryBatchTaskRespVO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryBatchTaskStoreRespVO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryBatchTaskPageReqVO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryBatchTaskStorePageReqVO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryBatchExecuteReqVO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryImportService.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryImportServiceImpl.java`
- Create: `yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryImportServiceImplTest.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleStoreInventoryController.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleStoreInventoryBatchTaskController.java`
- Create: `yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryBatchTaskServiceImplTest.java`
- Create: `yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/executor/EleStoreInventoryBatchExecutorImplTest.java`

### Frontend files

- Modify: `ytsh-ui-vue3/src/api/ele/storeInventory/index.ts`
- Modify: `ytsh-ui-vue3/src/views/ele/store-inventory/index.vue`
- Create: `ytsh-ui-vue3/src/views/ele/store-inventory/components/InventoryImportDialog.vue`
- Create: `ytsh-ui-vue3/src/views/ele/store-inventory/components/InventoryBatchTaskPanel.vue`
- Create: `ytsh-ui-vue3/src/views/ele/storeInventoryTaskPageLogic.ts`
- Possibly Modify: `ytsh-ui-vue3/src/views/ele/storeInventoryPageLogic.ts`

---

### Task 1: Reconcile the current inventory baseline into the isolated worktree

**Files:**
- Copy into worktree: all baseline files listed in “Current committed vs working-tree baseline”
- Verify against worktree branch: `.worktrees/worktree-store-inventory-batch-spec/**`

- [ ] **Step 1: Show the baseline delta between the main checkout and the worktree**

Run:

```bash
git diff --no-index --stat \
  "C:/Users/ytsh01/Desktop/ant_dev/yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleStoreInventoryController.java" \
  "C:/Users/ytsh01/Desktop/ant_dev/.worktrees/worktree-store-inventory-batch-spec/yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleStoreInventoryController.java"
```

Expected: the worktree side is missing or differs, proving the isolated branch does not yet contain the completed inventory-query baseline.

- [ ] **Step 2: Copy the backend baseline files into the worktree**

Run:

```bash
cp "C:/Users/ytsh01/Desktop/ant_dev/yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleStoreInventoryController.java" \
   "C:/Users/ytsh01/Desktop/ant_dev/.worktrees/worktree-store-inventory-batch-spec/yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleStoreInventoryController.java"
```

Repeat the same copy pattern for the remaining backend baseline files listed above.

Expected: all current inventory query / shadow / governance Java files now exist inside the worktree.

- [ ] **Step 3: Copy the frontend baseline files into the worktree**

Run:

```bash
cp "C:/Users/ytsh01/Desktop/ant_dev/ytsh-ui-vue3/src/api/ele/storeInventory/index.ts" \
   "C:/Users/ytsh01/Desktop/ant_dev/.worktrees/worktree-store-inventory-batch-spec/ytsh-ui-vue3/src/api/ele/storeInventory/index.ts"
```

Repeat for:

```text
ytsh-ui-vue3/src/views/ele/store-inventory/index.vue
ytsh-ui-vue3/src/views/ele/storeInventoryPageLogic.ts
ytsh-ui-vue3/src/views/business/store-product/components/ShadowInventoryExpandCard.vue
ytsh-ui-vue3/src/views/ele/components/InventoryMetricsCard.vue
```

Expected: the worktree contains the same inventory UI baseline as the current main checkout.

- [ ] **Step 4: Run compile and type-check on the reconciled baseline**

Run:

```bash
mvn -pl yudao-module-ele -DskipTests compile
pnpm --dir ytsh-ui-vue3 ts:check
```

Expected: PASS, or only existing unrelated errors already present in the baseline.

- [ ] **Step 5: Commit the baseline reconciliation before feature work**

```bash
git -C "C:/Users/ytsh01/Desktop/ant_dev/.worktrees/worktree-store-inventory-batch-spec" add \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleStoreInventoryController.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleSkuInventoryBatchQueryReqVO.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/dataobject/EleStoreInventoryShadowDO.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/dataobject/EleStoreInventoryGovernancePoolDO.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/mysql/EleStoreInventoryShadowMapper.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/mysql/EleStoreInventoryGovernancePoolMapper.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryQueryService.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryQueryServiceImpl.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryShadowService.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryShadowServiceImpl.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryGovernanceService.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryGovernanceServiceImpl.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/bo/EleSkuInventoryBatchQueryReqBO.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/bo/EleSkuInventoryShadowUpsertReqBO.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/dto/EleSkuInventoryBatchQueryRespDTO.java \
  yudao-module-ele/src/main/resources/sql/add_ele_inventory_shadow_tables.sql \
  ytsh-ui-vue3/src/api/ele/storeInventory/index.ts \
  ytsh-ui-vue3/src/views/ele/store-inventory/index.vue \
  ytsh-ui-vue3/src/views/ele/storeInventoryPageLogic.ts \
  ytsh-ui-vue3/src/views/business/store-product/components/ShadowInventoryExpandCard.vue \
  ytsh-ui-vue3/src/views/ele/components/InventoryMetricsCard.vue

git -C "C:/Users/ytsh01/Desktop/ant_dev/.worktrees/worktree-store-inventory-batch-spec" commit -m "feat: add inventory query baseline"
```

### Task 2: Extract a shared ingest service from the current query flow

**Files:**
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/bo/EleStoreInventoryIngestRowBO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryIngestService.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryIngestServiceImpl.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryQueryServiceImpl.java`
- Test: `yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryIngestServiceImplTest.java`

- [ ] **Step 1: Write the failing ingest test for formal vs shadow split**

Create `EleStoreInventoryIngestServiceImplTest.java`:

```java
package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.business.dal.dataobject.product.SkuTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreProductDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreStockDO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SkuTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreProductMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreStockMapper;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryGovernancePoolDO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreInventoryShadowDO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreInventoryIngestRowBO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EleStoreInventoryIngestServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private EleStoreInventoryIngestServiceImpl ingestService;

    @Mock
    private SkuTableMapper skuTableMapper;
    @Mock
    private StoreProductMapper storeProductMapper;
    @Mock
    private StoreStockMapper storeStockMapper;
    @Mock
    private EleSkuInventoryShadowService shadowService;
    @Mock
    private EleSkuInventoryGovernanceService governanceService;

    @Test
    void ingest_shouldWriteFormalStockWhenStoreProductExists() {
        EleStoreInventoryIngestRowBO row = buildRow();
        SkuTableDO sku = new SkuTableDO();
        sku.setProductSkuId("1001");
        StoreProductDO storeProduct = new StoreProductDO();
        storeProduct.setStoreProductId("SP001");
        when(skuTableMapper.selectByProductSkuCode("SKU001")).thenReturn(sku);
        when(storeProductMapper.selectByStoreIdAndProductSkuId("STORE001", "1001")).thenReturn(storeProduct);
        when(storeStockMapper.selectByStoreProductId("SP001")).thenReturn(null);

        EleStoreInventoryIngestService.IngestResult result = ingestService.ingest(row);

        assertEquals("FORMAL", result.getPersistStatus());
        verify(storeStockMapper).insert(any(StoreStockDO.class));
    }

    @Test
    void ingest_shouldWriteShadowAndGovernanceWhenStoreProductMissing() {
        EleStoreInventoryIngestRowBO row = buildRow();
        when(skuTableMapper.selectByProductSkuCode("SKU001")).thenReturn(null);
        when(shadowService.upsert(any(), any(), any())).thenReturn(new EleStoreInventoryShadowDO());

        EleStoreInventoryIngestService.IngestResult result = ingestService.ingest(row);

        assertEquals("SHADOW", result.getPersistStatus());
        verify(shadowService).upsert(any(), any(), any());
        verify(governanceService).createOrRefresh(any(EleStoreInventoryGovernancePoolDO.class));
    }

    private EleStoreInventoryIngestRowBO buildRow() {
        EleStoreInventoryIngestRowBO row = new EleStoreInventoryIngestRowBO();
        row.setPlatformId(1L);
        row.setMerchantCode("M001");
        row.setErpStoreCode("ERP001");
        row.setPlatformStoreId("ERP001");
        row.setStoreId("STORE001");
        row.setSkuCode("SKU001");
        row.setSubSkuCode("SUB001");
        row.setAvailableForSale(10);
        row.setReservedAmount(1);
        row.setPhysicalStockTotalAmount(12);
        row.setPhysicalStockAvailableAmount(10);
        row.setPhysicalStockOccupiedAmount(1);
        row.setPhysicalStockIntransitAmount(1);
        row.setRawPayload("{\"skuCode\":\"SKU001\"}");
        row.setSourceType("QUERY");
        return row;
    }
}
```

- [ ] **Step 2: Run the test to confirm the shared service does not exist yet**

Run:

```bash
mvn -pl yudao-module-ele -Dtest=EleStoreInventoryIngestServiceImplTest test
```

Expected: FAIL because `EleStoreInventoryIngestServiceImpl` and `EleStoreInventoryIngestRowBO` do not exist.

- [ ] **Step 3: Create the standard ingest row BO and service contract**

Create `EleStoreInventoryIngestRowBO.java`:

```java
package cn.iocoder.yudao.module.ele.service.bo;

import lombok.Data;

@Data
public class EleStoreInventoryIngestRowBO {
    private Long platformId;
    private String merchantCode;
    private String erpStoreCode;
    private String platformStoreId;
    private String storeId;
    private String skuCode;
    private String subSkuCode;
    private Integer availableForSale;
    private Integer reservedAmount;
    private Integer physicalStockTotalAmount;
    private Integer physicalStockAvailableAmount;
    private Integer physicalStockOccupiedAmount;
    private Integer physicalStockIntransitAmount;
    private String ownerCode;
    private String ownerName;
    private String rawPayload;
    private String sourceType;
    private Integer sourceRowNo;
}
```

Create `EleStoreInventoryIngestService.java`:

```java
package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.module.ele.service.bo.EleStoreInventoryIngestRowBO;
import lombok.Data;

public interface EleStoreInventoryIngestService {

    IngestResult ingest(EleStoreInventoryIngestRowBO row);

    @Data
    class IngestResult {
        private String persistStatus;
        private String reasonCode;
        private Long shadowId;
    }
}
```

- [ ] **Step 4: Implement the minimal ingest service and rewire the query path to call it**

Create `EleStoreInventoryIngestServiceImpl.java` with the current query logic extracted from `processInventoryRow(...)`:

```java
@Service
public class EleStoreInventoryIngestServiceImpl implements EleStoreInventoryIngestService {

    @Resource private SkuTableMapper skuTableMapper;
    @Resource private StoreProductMapper storeProductMapper;
    @Resource private StoreStockMapper storeStockMapper;
    @Resource private EleSkuInventoryShadowService shadowService;
    @Resource private EleSkuInventoryGovernanceService governanceService;

    @Override
    public IngestResult ingest(EleStoreInventoryIngestRowBO row) {
        IngestResult result = new IngestResult();
        String skuCode = StrUtil.trimToNull(row.getSkuCode());
        if (StrUtil.isNotBlank(skuCode)) {
            SkuTableDO sku = skuTableMapper.selectByProductSkuCode(skuCode);
            if (sku != null) {
                StoreProductDO storeProduct = storeProductMapper.selectByStoreIdAndProductSkuId(
                        row.getStoreId(), String.valueOf(sku.getProductSkuId()));
                if (storeProduct != null) {
                    upsertFormalStock(storeProduct.getStoreProductId(), row);
                    result.setPersistStatus("FORMAL");
                    return result;
                }
            }
        }
        EleSkuInventoryShadowUpsertReqBO shadowReq = toShadowReq(row);
        EleStoreInventoryShadowDO shadow = shadowService.upsert(shadowReq, "SKU_NOT_MATCHED", "SKU_NOT_FOUND");
        governanceService.createOrRefresh(buildGovernancePool(row, shadow));
        result.setPersistStatus("SHADOW");
        result.setReasonCode("SKU_NOT_FOUND");
        result.setShadowId(shadow.getId());
        return result;
    }
}
```

Then modify `EleSkuInventoryQueryServiceImpl.processInventoryRow(...)` so it becomes a thin adapter:

```java
EleStoreInventoryIngestRowBO ingestRow = buildIngestRow(normalizedReq, inventory, row);
EleStoreInventoryIngestService.IngestResult ingestResult = ingestService.ingest(ingestRow);
if ("FORMAL".equals(ingestResult.getPersistStatus())) {
    respDTO.setFormalSuccessCount(respDTO.getFormalSuccessCount() + 1);
    row.setPersistStatus("FORMAL");
    return;
}
respDTO.setShadowSuccessCount(respDTO.getShadowSuccessCount() + 1);
respDTO.setGovernanceCount(respDTO.getGovernanceCount() + 1);
row.setPersistStatus("SHADOW");
row.setReasonCode(ingestResult.getReasonCode());
```

- [ ] **Step 5: Run the shared ingest test and the current inventory query compile**

Run:

```bash
mvn -pl yudao-module-ele -Dtest=EleStoreInventoryIngestServiceImplTest test
mvn -pl yudao-module-ele -DskipTests compile
```

Expected: PASS.

- [ ] **Step 6: Commit the ingest extraction**

```bash
git add \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/bo/EleStoreInventoryIngestRowBO.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryIngestService.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryIngestServiceImpl.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryQueryServiceImpl.java \
  yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryIngestServiceImplTest.java

git commit -m "refactor: extract shared inventory ingest flow"
```

### Task 3: Add inventory batch task schema, lock keys, and executor skeleton

**Files:**
- Modify: `yudao-module-ele/src/main/resources/sql/add_ele_inventory_shadow_tables.sql`
- Modify: `yudao-module-business/src/test/resources/sql/create_tables.sql`
- Modify: `yudao-module-business/src/test/resources/sql/clean.sql`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/config/EleOrderExecutorConfiguration.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/redis/EleLockKeyConstants.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/redis/EleOrderLockService.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/redis/EleOrderLockServiceImpl.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/dataobject/EleStoreInventoryBatchTaskDO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/dataobject/EleStoreInventoryBatchTaskStoreDO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/mysql/EleStoreInventoryBatchTaskMapper.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/mysql/EleStoreInventoryBatchTaskStoreMapper.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/executor/EleStoreInventoryBatchExecutor.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/executor/EleStoreInventoryBatchExecutorImpl.java`
- Test: `yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/executor/EleStoreInventoryBatchExecutorImplTest.java`

- [ ] **Step 1: Write the failing executor test for task aggregation**

Create `EleStoreInventoryBatchExecutorImplTest.java`:

```java
class EleStoreInventoryBatchExecutorImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private EleStoreInventoryBatchExecutorImpl executor;

    @Mock private EleStoreInventoryBatchTaskMapper taskMapper;
    @Mock private EleStoreInventoryBatchTaskStoreMapper taskStoreMapper;
    @Mock private EleStoreInventorySkuScopeService skuScopeService;
    @Mock private EleStoreInventoryIngestService ingestService;

    @Test
    void execute_shouldAggregateStoreResults() {
        EleStoreInventoryBatchTaskDO task = new EleStoreInventoryBatchTaskDO();
        task.setId(1L);
        task.setStatus("PENDING");
        EleStoreInventoryBatchTaskStoreDO store = new EleStoreInventoryBatchTaskStoreDO();
        store.setId(11L);
        store.setTaskId(1L);
        store.setStoreId("STORE001");
        store.setErpStoreCode("ERP001");
        store.setPlatformStoreId("ERP001");
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(taskStoreMapper.selectListByTaskId(1L)).thenReturn(List.of(store));
        when(skuScopeService.listStoreSkuScope("STORE001", "ERP001")).thenReturn(List.of("SKU001"));

        executor.execute(1L);

        verify(taskStoreMapper, atLeastOnce()).updateById(any(EleStoreInventoryBatchTaskStoreDO.class));
        verify(taskMapper, atLeastOnce()).updateById(any(EleStoreInventoryBatchTaskDO.class));
    }
}
```

- [ ] **Step 2: Run the executor test to verify the new task classes do not exist**

Run:

```bash
mvn -pl yudao-module-ele -Dtest=EleStoreInventoryBatchExecutorImplTest test
```

Expected: FAIL because task DO/mapper/executor classes do not exist.

- [ ] **Step 3: Add SQL tables and H2 support for inventory batch tasks**

Append to `add_ele_inventory_shadow_tables.sql`:

```sql
CREATE TABLE IF NOT EXISTS `ele_store_inventory_batch_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_no` varchar(64) NOT NULL,
  `source_type` varchar(32) NOT NULL COMMENT 'IMPORT/MANUAL/SCHEDULED',
  `scope` varchar(32) NOT NULL COMMENT 'ALL_OPEN_STORES',
  `status` varchar(32) NOT NULL,
  `total_store_count` int NOT NULL DEFAULT 0,
  `finished_store_count` int NOT NULL DEFAULT 0,
  `total_batch_count` int NOT NULL DEFAULT 0,
  `finished_batch_count` int NOT NULL DEFAULT 0,
  `total_sku_count` int NOT NULL DEFAULT 0,
  `formal_success_count` int NOT NULL DEFAULT 0,
  `shadow_success_count` int NOT NULL DEFAULT 0,
  `governance_count` int NOT NULL DEFAULT 0,
  `failure_count` int NOT NULL DEFAULT 0,
  `error_msg` varchar(500) DEFAULT NULL,
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ele_store_inventory_batch_task_no` (`task_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店库存批量任务';

CREATE TABLE IF NOT EXISTS `ele_store_inventory_batch_task_store` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `task_no` varchar(64) NOT NULL,
  `store_id` varchar(64) DEFAULT NULL,
  `store_name` varchar(255) DEFAULT NULL,
  `erp_store_code` varchar(64) NOT NULL,
  `platform_store_id` varchar(64) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `current_batch` int NOT NULL DEFAULT 0,
  `total_batch` int NOT NULL DEFAULT 0,
  `total_sku_count` int NOT NULL DEFAULT 0,
  `formal_success_count` int NOT NULL DEFAULT 0,
  `shadow_success_count` int NOT NULL DEFAULT 0,
  `governance_count` int NOT NULL DEFAULT 0,
  `failure_count` int NOT NULL DEFAULT 0,
  `error_msg` varchar(500) DEFAULT NULL,
  `started_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店库存批量任务-门店子任务';
```

Also append matching H2 DDL to `create_tables.sql` and matching cleanup lines to `clean.sql`.

- [ ] **Step 4: Add lock key and thread-pool entry for inventory batch tasks**

In `EleLockKeyConstants.java`, add:

```java
public static final String STORE_INVENTORY_BATCH_TASK_LOCK = "ele:store:inventory:batch:task:lock:%s";
```

In `EleOrderLockService.java`, add:

```java
void lockStoreInventoryBatchTask(String lockKey, int waitSeconds, int leaseMinutes);
void unlockStoreInventoryBatchTask(String lockKey);
```

In `EleOrderLockServiceImpl.java`, implement the two methods exactly like `lockStoreGoodsFullSyncTask(...)` / `unlockStoreGoodsFullSyncTask(...)`, but using the inventory batch lock key.

In `EleOrderExecutorConfiguration.java`, add:

```java
@Bean(name = "eleStoreInventoryBatchExecutor", destroyMethod = "shutdown")
public ThreadPoolTaskExecutor eleStoreInventoryBatchExecutor(
        @Value("${ele.store.inventory.batch.pool.core-size:8}") int coreSize,
        @Value("${ele.store.inventory.batch.pool.max-size:16}") int maxSize,
        @Value("${ele.store.inventory.batch.pool.queue-capacity:0}") int queueCapacity,
        @Value("${ele.store.inventory.batch.pool.keep-alive-seconds:60}") int keepAliveSeconds,
        @Value("${ele.store.inventory.batch.shutdown.await-termination:true}") boolean awaitTermination,
        @Value("${ele.store.inventory.batch.shutdown.await-termination-seconds:120}") int awaitTerminationSeconds) {
    return buildExecutor(coreSize, maxSize, queueCapacity, keepAliveSeconds, awaitTermination,
            awaitTerminationSeconds, "ele-store-inventory-batch-");
}
```

- [ ] **Step 5: Create minimal task DOs, mappers, and executor shell**

Create `EleStoreInventoryBatchTaskDO.java` and `EleStoreInventoryBatchTaskStoreDO.java` following the same pattern as `EleStoreGoodsFullSyncTaskDO` and `EleStoreGoodsFullSyncTaskStoreDO`, but with the inventory-batch fields from the SQL above.

Create mapper methods:

```java
default List<EleStoreInventoryBatchTaskStoreDO> selectListByTaskId(Long taskId) {
    return selectList(EleStoreInventoryBatchTaskStoreDO::getTaskId, taskId);
}
```

Create `EleStoreInventoryBatchExecutorImpl.java` by cloning the task lifecycle from `EleStoreGoodsFullSyncExecutorImpl`, but replacing page loops with SKU-batch loops.

Core loop:

```java
List<String> skuCodes = skuScopeService.listStoreSkuScope(taskStore.getStoreId(), taskStore.getErpStoreCode());
List<List<String>> batches = ListUtil.partition(skuCodes, 50);
for (int i = 0; i < batches.size(); i++) {
    taskStore.setCurrentBatch(i + 1);
    taskStore.setTotalBatch(batches.size());
    taskStoreMapper.updateById(taskStore);
    // Task 5 will fill in the real pull + ingest logic.
}
```

- [ ] **Step 6: Run executor test and compile**

Run:

```bash
mvn -pl yudao-module-ele -Dtest=EleStoreInventoryBatchExecutorImplTest test
mvn -pl yudao-module-ele -DskipTests compile
```

Expected: PASS.

- [ ] **Step 7: Commit the task schema and executor skeleton**

```bash
git add \
  yudao-module-ele/src/main/resources/sql/add_ele_inventory_shadow_tables.sql \
  yudao-module-business/src/test/resources/sql/create_tables.sql \
  yudao-module-business/src/test/resources/sql/clean.sql \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/config/EleOrderExecutorConfiguration.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/redis/EleLockKeyConstants.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/redis/EleOrderLockService.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/redis/EleOrderLockServiceImpl.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/dataobject/EleStoreInventoryBatchTaskDO.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/dataobject/EleStoreInventoryBatchTaskStoreDO.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/mysql/EleStoreInventoryBatchTaskMapper.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/mysql/EleStoreInventoryBatchTaskStoreMapper.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/executor/EleStoreInventoryBatchExecutor.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/executor/EleStoreInventoryBatchExecutorImpl.java \
  yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/executor/EleStoreInventoryBatchExecutorImplTest.java

git commit -m "feat: add store inventory batch task skeleton"
```

### Task 4: Add import template parsing and route Excel rows into the shared ingest flow

**Files:**
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryImportExcelVO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryImportRespVO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryImportService.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryImportServiceImpl.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleStoreInventoryController.java`
- Test: `yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryImportServiceImplTest.java`

- [ ] **Step 1: Write the failing import service test**

Create `EleStoreInventoryImportServiceImplTest.java`:

```java
class EleStoreInventoryImportServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private EleStoreInventoryImportServiceImpl importService;

    @Mock private StoreService storeService;
    @Mock private EleStoreInventoryIngestService ingestService;

    @Test
    void importRows_shouldCountFormalShadowAndValidationFailures() {
        EleStoreInventoryImportExcelVO ok = new EleStoreInventoryImportExcelVO();
        ok.setErpStoreCode("ERP001");
        ok.setSkuCode("SKU001");
        ok.setPhysicalStockTotalAmount(10);
        ok.setPhysicalStockAvailableAmount(8);
        ok.setPhysicalStockOccupiedAmount(1);
        ok.setPhysicalStockIntransitAmount(1);
        ok.setAvailableForSale(8);
        ok.setReservedAmount(1);

        EleStoreInventoryImportExcelVO bad = new EleStoreInventoryImportExcelVO();
        bad.setErpStoreCode("ERP001");
        bad.setSkuCode("");

        when(storeService.getOpenPlatformStores(1L)).thenReturn(List.of());
        EleStoreInventoryIngestService.IngestResult shadow = new EleStoreInventoryIngestService.IngestResult();
        shadow.setPersistStatus("SHADOW");
        when(ingestService.ingest(any())).thenReturn(shadow);

        EleStoreInventoryImportRespVO result = importService.importRows(List.of(ok, bad));

        assertEquals(1, result.getShadowSuccessCount());
        assertEquals(1, result.getFailureCount());
    }
}
```

- [ ] **Step 2: Run the import test to verify the service does not exist yet**

Run:

```bash
mvn -pl yudao-module-ele -Dtest=EleStoreInventoryImportServiceImplTest test
```

Expected: FAIL because the import VO/service classes do not exist.

- [ ] **Step 3: Create the import Excel VO and response VO**

Create `EleStoreInventoryImportExcelVO.java`:

```java
@Data
public class EleStoreInventoryImportExcelVO {
    @ExcelProperty("ERP门店编码")
    private String erpStoreCode;
    @ExcelProperty("SKU编码")
    private String skuCode;
    @ExcelProperty("子SKU编码")
    private String subSkuCode;
    @ExcelProperty("总库存")
    private Integer physicalStockTotalAmount;
    @ExcelProperty("可售库存")
    private Integer availableForSale;
    @ExcelProperty("预留库存")
    private Integer reservedAmount;
    @ExcelProperty("物理可用库存")
    private Integer physicalStockAvailableAmount;
    @ExcelProperty("物理占用库存")
    private Integer physicalStockOccupiedAmount;
    @ExcelProperty("物理在途库存")
    private Integer physicalStockIntransitAmount;
    @ExcelProperty("备注")
    private String remark;
}
```

Create `EleStoreInventoryImportRespVO.java`:

```java
@Data
public class EleStoreInventoryImportRespVO {
    private Integer formalSuccessCount = 0;
    private Integer shadowSuccessCount = 0;
    private Integer governanceCount = 0;
    private Integer failureCount = 0;
    private List<FailureItem> failureList = new ArrayList<>();

    @Data
    public static class FailureItem {
        private Integer rowNo;
        private String skuCode;
        private String message;
    }
}
```

- [ ] **Step 4: Implement the import service with row validation and ingest routing**

Create `EleStoreInventoryImportServiceImpl.java`:

```java
@Service
public class EleStoreInventoryImportServiceImpl implements EleStoreInventoryImportService {

    @Resource private StoreService storeService;
    @Resource private EleStoreInventoryIngestService ingestService;

    @Override
    public EleStoreInventoryImportRespVO importRows(List<EleStoreInventoryImportExcelVO> rows) {
        EleStoreInventoryImportRespVO resp = new EleStoreInventoryImportRespVO();
        Map<String, StorePlatformRespVO> storeMap = storeService.getOpenPlatformStores(1L).stream()
                .collect(Collectors.toMap(StorePlatformRespVO::getPlatformStoreId, Function.identity(), (a, b) -> a));
        for (int i = 0; i < rows.size(); i++) {
            EleStoreInventoryImportExcelVO row = rows.get(i);
            String validationError = validateRow(row);
            if (validationError != null) {
                addFailure(resp, i + 1, row.getSkuCode(), validationError);
                continue;
            }
            StorePlatformRespVO store = storeMap.get(StrUtil.trim(row.getErpStoreCode()));
            if (store == null) {
                addFailure(resp, i + 1, row.getSkuCode(), "ERP门店编码未匹配到平台门店");
                continue;
            }
            EleStoreInventoryIngestRowBO ingestRow = toIngestRow(row, store, i + 1);
            EleStoreInventoryIngestService.IngestResult result = ingestService.ingest(ingestRow);
            if ("FORMAL".equals(result.getPersistStatus())) {
                resp.setFormalSuccessCount(resp.getFormalSuccessCount() + 1);
            } else {
                resp.setShadowSuccessCount(resp.getShadowSuccessCount() + 1);
                resp.setGovernanceCount(resp.getGovernanceCount() + 1);
            }
        }
        return resp;
    }
}
```

- [ ] **Step 5: Add import endpoints to the inventory controller**

Modify `EleStoreInventoryController.java` by adding:

```java
@Resource
private EleStoreInventoryImportService eleStoreInventoryImportService;

@GetMapping("/import-template")
@Operation(summary = "下载门店库存导入模板")
@PreAuthorize("@ss.hasPermission('ele:order:sync')")
public void downloadImportTemplate(HttpServletResponse response) throws IOException {
    ExcelUtils.write(response, "门店库存导入模板.xls", "库存导入", EleStoreInventoryImportExcelVO.class, List.of());
}

@PostMapping("/import")
@Operation(summary = "导入门店库存")
@PreAuthorize("@ss.hasPermission('ele:order:sync')")
public CommonResult<EleStoreInventoryImportRespVO> importExcel(@RequestParam("file") MultipartFile file) throws Exception {
    List<EleStoreInventoryImportExcelVO> list = ExcelUtils.read(file, EleStoreInventoryImportExcelVO.class);
    return CommonResult.success(eleStoreInventoryImportService.importRows(list));
}
```

- [ ] **Step 6: Run the import test and compile**

Run:

```bash
mvn -pl yudao-module-ele -Dtest=EleStoreInventoryImportServiceImplTest test
mvn -pl yudao-module-ele -DskipTests compile
```

Expected: PASS.

- [ ] **Step 7: Commit the import flow**

```bash
git add \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryImportExcelVO.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryImportRespVO.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryImportService.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryImportServiceImpl.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleStoreInventoryController.java \
  yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryImportServiceImplTest.java

git commit -m "feat: add store inventory import flow"
```

### Task 5: Implement dynamic SKU scope and the real batch pull pipeline

**Files:**
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventorySkuScopeService.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventorySkuScopeServiceImpl.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/executor/EleStoreInventoryBatchExecutorImpl.java`
- Test: `yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/executor/EleStoreInventoryBatchExecutorImplTest.java`

- [ ] **Step 1: Extend the executor test to assert SKU-scope and ingest are called**

Append to `EleStoreInventoryBatchExecutorImplTest.java`:

```java
@Test
void execute_shouldPullSkuBatchesAndIngestEachRow() {
    EleStoreInventoryBatchTaskDO task = new EleStoreInventoryBatchTaskDO();
    task.setId(2L);
    task.setStatus("PENDING");
    EleStoreInventoryBatchTaskStoreDO store = new EleStoreInventoryBatchTaskStoreDO();
    store.setId(21L);
    store.setTaskId(2L);
    store.setStoreId("STORE001");
    store.setMerchantCode("M001");
    store.setErpStoreCode("ERP001");
    store.setPlatformStoreId("ERP001");
    when(taskMapper.selectById(2L)).thenReturn(task);
    when(taskStoreMapper.selectListByTaskId(2L)).thenReturn(List.of(store));
    when(skuScopeService.listStoreSkuScope("STORE001", "ERP001")).thenReturn(List.of("SKU001", "SKU002"));

    executor.execute(2L);

    verify(skuScopeService).listStoreSkuScope("STORE001", "ERP001");
    verify(taskMapper, atLeastOnce()).updateById(any(EleStoreInventoryBatchTaskDO.class));
}
```

- [ ] **Step 2: Run the test to verify the SKU-scope service does not exist yet**

Run:

```bash
mvn -pl yudao-module-ele -Dtest=EleStoreInventoryBatchExecutorImplTest#execute_shouldPullSkuBatchesAndIngestEachRow test
```

Expected: FAIL because `EleStoreInventorySkuScopeService` is not implemented.

- [ ] **Step 3: Implement the dynamic SKU-scope service**

Create `EleStoreInventorySkuScopeServiceImpl.java` with three-source union logic:

```java
@Service
public class EleStoreInventorySkuScopeServiceImpl implements EleStoreInventorySkuScopeService {

    @Resource private StoreProductMapper storeProductMapper;
    @Resource private EleStoreGoodsShadowMapper storeGoodsShadowMapper;
    @Resource private EleStoreInventoryShadowMapper inventoryShadowMapper;

    @Override
    public List<String> listStoreSkuScope(String storeId, String erpStoreCode) {
        LinkedHashSet<String> skuCodes = new LinkedHashSet<>();
        skuCodes.addAll(storeProductMapper.selectSkuCodesByStoreId(storeId));
        skuCodes.addAll(storeGoodsShadowMapper.selectActiveSkuCodesByErpStoreCode(erpStoreCode));
        skuCodes.addAll(inventoryShadowMapper.selectActiveSkuCodes(storeId, erpStoreCode));
        return skuCodes.stream().filter(StrUtil::isNotBlank).toList();
    }
}
```

Add the needed mapper methods, for example in `EleStoreInventoryShadowMapper`:

```java
default List<String> selectActiveSkuCodes(String storeId, String erpStoreCode) {
    return selectList(new LambdaQueryWrapperX<EleStoreInventoryShadowDO>()
            .eqIfPresent(EleStoreInventoryShadowDO::getStoreId, storeId)
            .eqIfPresent(EleStoreInventoryShadowDO::getErpStoreCode, erpStoreCode)
            .orderByDesc(EleStoreInventoryShadowDO::getUpdateTime))
            .stream()
            .map(EleStoreInventoryShadowDO::getSkuCode)
            .filter(StrUtil::isNotBlank)
            .toList();
}
```

- [ ] **Step 4: Replace the executor skeleton with real pull + ingest logic**

In `EleStoreInventoryBatchExecutorImpl`, replace the placeholder batch loop with:

```java
List<String> skuCodes = skuScopeService.listStoreSkuScope(taskStore.getStoreId(), taskStore.getErpStoreCode());
List<List<String>> batches = CollUtil.split(skuCodes, 50);
for (int i = 0; i < batches.size(); i++) {
    List<String> batch = batches.get(i);
    taskStore.setCurrentBatch(i + 1);
    taskStore.setTotalBatch(batches.size());
    taskStoreMapper.updateById(taskStore);

    EleSkuInventoryBatchQueryReqBO reqBO = new EleSkuInventoryBatchQueryReqBO();
    reqBO.setPlatformStoreId(taskStore.getPlatformStoreId());
    reqBO.setMerchantCode(taskStore.getMerchantCode());
    reqBO.setErpStoreCode(taskStore.getErpStoreCode());
    reqBO.setSkuCodes(batch);

    EleSkuInventoryBatchQueryRespDTO respDTO = queryService.queryBatch(reqBO);
    summary.totalSkuCount += batch.size();
    summary.formalSuccessCount += value(respDTO.getFormalSuccessCount());
    summary.shadowSuccessCount += value(respDTO.getShadowSuccessCount());
    summary.governanceCount += value(respDTO.getGovernanceCount());
    summary.failureCount += value(respDTO.getFailureCount());
}
```

Inject `EleSkuInventoryQueryService queryService` into the executor.

- [ ] **Step 5: Run the executor test suite and compile**

Run:

```bash
mvn -pl yudao-module-ele -Dtest=EleStoreInventoryBatchExecutorImplTest test
mvn -pl yudao-module-ele -DskipTests compile
```

Expected: PASS.

- [ ] **Step 6: Commit the real batch pull flow**

```bash
git add \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventorySkuScopeService.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventorySkuScopeServiceImpl.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/mysql/EleStoreInventoryShadowMapper.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/mysql/EleStoreGoodsShadowMapper.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/store/StoreProductMapper.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/executor/EleStoreInventoryBatchExecutorImpl.java \
  yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/executor/EleStoreInventoryBatchExecutorImplTest.java

git commit -m "feat: add inventory batch sku scope and executor"
```

### Task 6: Add task creation, task query, cancel, and scheduler entrypoints

**Files:**
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryBatchTaskRespVO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryBatchTaskStoreRespVO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryBatchTaskPageReqVO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryBatchTaskStorePageReqVO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryBatchExecuteReqVO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryBatchTaskService.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryBatchTaskServiceImpl.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleStoreInventoryBatchTaskController.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/config/EleStoreInventorySchedulerProperties.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/job/EleStoreInventoryAutoSyncJob.java`
- Test: `yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryBatchTaskServiceImplTest.java`

- [ ] **Step 1: Write the failing task-service test for create + cancel**

Create `EleStoreInventoryBatchTaskServiceImplTest.java`:

```java
class EleStoreInventoryBatchTaskServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private EleStoreInventoryBatchTaskServiceImpl taskService;

    @Mock private EleStoreInventoryBatchTaskMapper taskMapper;
    @Mock private EleStoreInventoryBatchTaskStoreMapper taskStoreMapper;
    @Mock private StoreService storeService;
    @Mock private EleStoreInventoryBatchExecutor batchExecutor;
    @Mock private EleOrderLockService eleOrderLockService;

    @Test
    void createAllOpenTask_shouldInsertTaskAndStoreRows() {
        StorePlatformRespVO store = new StorePlatformRespVO();
        store.setStoreId("STORE001");
        store.setPlatformStoreId("ERP001");
        store.setSettlementAccount("M001");
        when(storeService.getOpenPlatformStores(1L)).thenReturn(List.of(store));

        Long taskId = taskService.createAllOpenTask("MANUAL");

        assertNotNull(taskId);
        verify(taskMapper).insert(any(EleStoreInventoryBatchTaskDO.class));
        verify(taskStoreMapper).insert(any(EleStoreInventoryBatchTaskStoreDO.class));
    }
}
```

- [ ] **Step 2: Run the test to confirm the task service does not exist yet**

Run:

```bash
mvn -pl yudao-module-ele -Dtest=EleStoreInventoryBatchTaskServiceImplTest test
```

Expected: FAIL.

- [ ] **Step 3: Implement the task service by following the store-goods full-sync pattern**

Create `EleStoreInventoryBatchTaskServiceImpl.java` with the same transaction + lock + afterCommit submit structure as `EleStoreGoodsFullSyncServiceImpl`.

Core create method:

```java
@Transactional(rollbackFor = Exception.class)
public Long createAllOpenTask(String sourceType) {
    String lockKey = "ALL_OPEN_STORES";
    eleOrderLockService.lockStoreInventoryBatchTask(lockKey, 5, 1);
    try {
        List<StorePlatformRespVO> stores = storeService.getOpenPlatformStores(1L);
        if (CollUtil.isEmpty(stores)) {
            throw new RuntimeException("没有可执行库存任务的开业门店");
        }
        EleStoreInventoryBatchTaskDO task = createTask(sourceType, stores.size());
        taskMapper.insert(task);
        for (StorePlatformRespVO store : stores) {
            taskStoreMapper.insert(createTaskStore(task, store));
        }
        submitAfterCommit(task.getId(), lockKey);
        return task.getId();
    } finally {
        unlockIfNoTransactionSynchronization(lockKey);
    }
}
```

Also implement page query, detail query, store-detail query, and cancel.

- [ ] **Step 4: Add controller and scheduler entrypoints**

Create `EleStoreInventoryBatchTaskController.java`:

```java
@RestController
@RequestMapping("/ele/store-inventory/task")
@Validated
@TenantIgnore
public class EleStoreInventoryBatchTaskController {

    @Resource private EleStoreInventoryBatchTaskService taskService;

    @PostMapping("/all-open")
    @PreAuthorize("@ss.hasPermission('ele:order:sync')")
    public CommonResult<Long> createAllOpenTask(@Valid @RequestBody EleStoreInventoryBatchExecuteReqVO reqVO) {
        return CommonResult.success(taskService.createAllOpenTask(reqVO.getSourceType()));
    }

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<PageResult<EleStoreInventoryBatchTaskRespVO>> getTaskPage(EleStoreInventoryBatchTaskPageReqVO reqVO) {
        return CommonResult.success(taskService.getTaskPage(reqVO));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@ss.hasPermission('ele:order:sync')")
    public CommonResult<Boolean> cancel(@PathVariable Long id) {
        taskService.cancelTask(id);
        return CommonResult.success(true);
    }
}
```

Create `EleStoreInventoryAutoSyncJob.java` by following `EleOrderAutoSyncScheduler`, but call:

```java
taskService.createAllOpenTask("SCHEDULED");
```

and guard with a boolean property in `EleStoreInventorySchedulerProperties`.

- [ ] **Step 5: Run task-service tests and compile**

Run:

```bash
mvn -pl yudao-module-ele -Dtest=EleStoreInventoryBatchTaskServiceImplTest test
mvn -pl yudao-module-ele -DskipTests compile
```

Expected: PASS.

- [ ] **Step 6: Commit the task service and scheduler**

```bash
git add \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryBatchTaskRespVO.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryBatchTaskStoreRespVO.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryBatchTaskPageReqVO.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryBatchTaskStorePageReqVO.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreInventoryBatchExecuteReqVO.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryBatchTaskService.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryBatchTaskServiceImpl.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleStoreInventoryBatchTaskController.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/config/EleStoreInventorySchedulerProperties.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/job/EleStoreInventoryAutoSyncJob.java \
  yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/EleStoreInventoryBatchTaskServiceImplTest.java

git commit -m "feat: add inventory batch task service and scheduler"
```

### Task 7: Add frontend import dialog and batch-task panel

**Files:**
- Modify: `ytsh-ui-vue3/src/api/ele/storeInventory/index.ts`
- Modify: `ytsh-ui-vue3/src/views/ele/store-inventory/index.vue`
- Create: `ytsh-ui-vue3/src/views/ele/store-inventory/components/InventoryImportDialog.vue`
- Create: `ytsh-ui-vue3/src/views/ele/store-inventory/components/InventoryBatchTaskPanel.vue`
- Create: `ytsh-ui-vue3/src/views/ele/storeInventoryTaskPageLogic.ts`

- [ ] **Step 1: Extend the frontend API module with import and task endpoints**

Append to `src/api/ele/storeInventory/index.ts`:

```ts
export interface EleStoreInventoryImportFailureVO {
  rowNo?: number
  skuCode?: string
  message?: string
}

export interface EleStoreInventoryImportRespVO {
  formalSuccessCount?: number
  shadowSuccessCount?: number
  governanceCount?: number
  failureCount?: number
  failureList?: EleStoreInventoryImportFailureVO[]
}

export interface EleStoreInventoryBatchTaskRespVO {
  id?: number
  taskNo?: string
  sourceType?: string
  scope?: string
  status?: string
  totalStoreCount?: number
  finishedStoreCount?: number
  totalBatchCount?: number
  finishedBatchCount?: number
  totalSkuCount?: number
  formalSuccessCount?: number
  shadowSuccessCount?: number
  governanceCount?: number
  failureCount?: number
  errorMsg?: string
  createTime?: string
}

export const downloadInventoryImportTemplate = async () => {
  return await request.download({ url: '/ele/store-inventory/import-template' })
}

export const importStoreInventory = async (data: FormData) => {
  return await request.upload<EleStoreInventoryImportRespVO>({ url: '/ele/store-inventory/import', data })
}

export const createAllOpenInventoryTask = async () => {
  return await request.post<number>({ url: '/ele/store-inventory/task/all-open', data: { sourceType: 'MANUAL' } })
}

export const getInventoryTaskPage = async (params) => {
  return await request.get({ url: '/ele/store-inventory/task/page', params })
}

export const cancelInventoryTask = async (id: number) => {
  return await request.post<boolean>({ url: `/ele/store-inventory/task/${id}/cancel` })
}
```

- [ ] **Step 2: Create the import dialog component**

Create `InventoryImportDialog.vue` with upload + result display:

```vue
<template>
  <Dialog v-model="dialogVisible" title="门店库存导入" width="520px">
    <el-upload :auto-upload="false" :limit="1" :on-change="handleFileChange">
      <template #trigger>
        <el-button type="primary">选择文件</el-button>
      </template>
    </el-upload>
    <template #footer>
      <el-button @click="handleDownloadTemplate">下载模板</el-button>
      <el-button type="primary" :loading="uploading" @click="handleSubmit">开始导入</el-button>
    </template>
  </Dialog>
</template>
```

Inside `handleSubmit`, build `FormData`, call `importStoreInventory`, and show:

```ts
ElMessage.success(
  `导入完成：正式 ${res.formalSuccessCount || 0}，影子 ${res.shadowSuccessCount || 0}，治理 ${res.governanceCount || 0}，失败 ${res.failureCount || 0}`
)
```

- [ ] **Step 3: Create the batch-task panel component**

Create `InventoryBatchTaskPanel.vue` by following `store-goods/components/FullSyncTaskPanel.vue`, but replace page-progress text with batch-progress text and counters with formal/shadow/governance/failure.

Use this action bar:

```vue
<div class="action-buttons">
  <el-button type="success" :loading="creatingAllOpen" @click="createAllOpenTask">所有开业门店库存拉取</el-button>
  <el-button @click="loadTasks()">刷新</el-button>
</div>
```

Use polling logic identical to the full-sync panel:

```ts
const canCancel = (status?: string) => ['PENDING', 'RUNNING'].includes(status || '')
const hasRunningTask = () => taskList.value.some((task) => canCancel(task.status))
```

- [ ] **Step 4: Mount both components into the inventory page**

Modify `store-inventory/index.vue` near the page header buttons:

```vue
<div class="form-row btn-row">
  <el-button type="primary" :loading="queryLoading" @click="handleQuery">查询</el-button>
  <el-button @click="handleReset">重置</el-button>
  <el-button type="success" @click="importVisible = true">批量导入库存</el-button>
</div>

<InventoryImportDialog v-model="importVisible" />
<InventoryBatchTaskPanel />
```

And add:

```ts
import InventoryImportDialog from './components/InventoryImportDialog.vue'
import InventoryBatchTaskPanel from './components/InventoryBatchTaskPanel.vue'

const importVisible = ref(false)
```

- [ ] **Step 5: Run frontend type-check**

Run:

```bash
pnpm --dir ytsh-ui-vue3 ts:check
```

Expected: PASS.

- [ ] **Step 6: Commit the frontend inventory task UI**

```bash
git add \
  ytsh-ui-vue3/src/api/ele/storeInventory/index.ts \
  ytsh-ui-vue3/src/views/ele/store-inventory/index.vue \
  ytsh-ui-vue3/src/views/ele/store-inventory/components/InventoryImportDialog.vue \
  ytsh-ui-vue3/src/views/ele/store-inventory/components/InventoryBatchTaskPanel.vue \
  ytsh-ui-vue3/src/views/ele/storeInventoryTaskPageLogic.ts

git commit -m "feat: add inventory import and batch task UI"
```

### Task 8: Full verification

**Files:**
- No new files.

- [ ] **Step 1: Run focused backend tests**

Run:

```bash
mvn -pl yudao-module-ele -Dtest=EleStoreInventoryIngestServiceImplTest,EleStoreInventoryImportServiceImplTest,EleStoreInventoryBatchTaskServiceImplTest,EleStoreInventoryBatchExecutorImplTest test
```

Expected: PASS.

- [ ] **Step 2: Compile the runnable backend**

Run:

```bash
mvn -pl yudao-server -am -DskipTests compile
```

Expected: PASS.

- [ ] **Step 3: Run frontend type-check**

Run:

```bash
pnpm --dir ytsh-ui-vue3 ts:check
```

Expected: PASS.

- [ ] **Step 4: Manual UI verification**

Run the frontend:

```bash
pnpm --dir ytsh-ui-vue3 dev
```

Verify:

1. 打开库存查询页面。
2. 下载库存导入模板。
3. 导入一份同时包含“可匹配正式门店商品”和“找不到正式门店商品”的测试文件。
4. 确认成功提示区分正式、影子、治理和失败数。
5. 打开库存批量任务面板，手工触发“所有开业门店库存拉取”。
6. 确认任务列表出现 RUNNING 任务并持续刷新。
7. 打开任务详情，确认门店级批次进度和错误信息可见。
8. 查看影子库存或治理记录，确认导入缺失行和批量拉取缺失行都进入同一治理口径。

- [ ] **Step 5: Review the diff for unrelated changes**

Run:

```bash
git status --short
git diff --stat
```

Expected: only the files from Tasks 1-7 are changed.

- [ ] **Step 6: Final commit if verification produced fixes**

```bash
git add <verification-fix-files>
git commit -m "fix: address inventory batch verification issues"
```

## Self-Review Notes

- Spec coverage: baseline sync, unified ingest flow, independent import, inventory batch task tables, door-level concurrency, dynamic SKU scope, manual task entrypoint, scheduler entrypoint, frontend import/task UI, and verification are all mapped to tasks.
- Placeholder scan: no TBD/TODO markers remain; each task includes exact files, code direction, and commands.
- Type consistency: the plan uses one unified name family around `EleStoreInventory*` and keeps the shared write boundary on `EleStoreInventoryIngestService`.
- Important execution note: Task 1 is mandatory because the current isolated worktree does not include the already-completed inventory query baseline from the dirty main checkout.
