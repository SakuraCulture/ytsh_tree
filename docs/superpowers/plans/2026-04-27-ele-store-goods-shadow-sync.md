# 饿了么门店商品影子同步 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让饿了么门店商品在本地 SKU 主档缺失时仍可落库、展示、治理，并在主档补齐后归并到正式门店商品。

**Architecture:** 新增 `ele_store_goods_shadow` 作为三方门店品事实快照，饿了么同步链路先 upsert 影子记录，再尝试匹配本地 SKU；匹配成功写入 `store_product_table` 并把影子记录标记为 `MERGED`，未匹配则保留 `UNMATCHED` 并进入正式列表聚合展示。正式门店商品列表通过业务层聚合正式表与未归并影子表，正式记录优先，影子记录带 `rowSource=SHADOW` 和 `MASTER_MISSING` 标识。

**Tech Stack:** Java 17, Spring Boot 3, MyBatis Plus, Yudao BaseDbUnitTest/BaseMockitoUnitTest, Vue 3, TypeScript, Element Plus, pnpm, Maven.

---

## File Structure

### Backend schema and shadow persistence

- Modify: `ele_store_goods_full_sync.sql` — 追加 MySQL 版 `ele_store_goods_shadow` DDL。
- Modify: `yudao-module-business/src/test/resources/sql/create_tables.sql` — 为 H2 单测环境追加 `ele_store_goods_shadow`。
- Modify: `yudao-module-business/src/test/resources/sql/clean.sql` — 清理 `ele_store_goods_shadow`。
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/enums/EleStoreGoodsShadowStatus.java` — 影子记录状态常量。
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/dataobject/EleStoreGoodsShadowDO.java` — 影子表 DO。
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/mysql/EleStoreGoodsShadowMapper.java` — 影子表 Mapper。
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/bo/EleStoreGoodsShadowUpsertReqBO.java` — 同步链路写影子表的入参。
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowService.java` — 影子表服务接口。
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowServiceImpl.java` — upsert、标记归并、状态查询。
- Create: `yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowServiceImplTest.java` — Mockito 单测覆盖 upsert 与标记归并。

### Backend sync integration

- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/bo/EleStoreGoodsPageSyncResult.java` — 增加 shadowCount。
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsSyncService.java` — `queryAndSyncStoreGoods` 返回分页同步结果。
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleStoreGoodsSyncController.java` — `/query-sync` 返回同步结果对象，给前端展示 shadowCount。
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsSyncServiceImpl.java` — 同步时先落影子表，SKU 缺失不再计为失败。
- Modify: `yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsSyncServiceImplTest.java` — 更新 SKU 缺失测试，新增匹配成功与影子写入测试。

### Backend merge and business list aggregation

- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductPageReqVO.java` — 增加 `rowSource`、`completenessStatus`、`matchStatus` 筛选。
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductRespVO.java` — 增加聚合展示字段。
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductShadowQueryService.java` — business 模块读取影子记录的接口边界。
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/StoreProductShadowQueryServiceImpl.java` — 实现 business 侧查询接口。
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/bo/StoreProductShadowRowBO.java` — business 聚合列表使用的影子行 BO。
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductServiceImpl.java` — 聚合正式表与影子表。
- Create: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/store/StoreProductServiceImplTest.java` — H2 单测覆盖列表聚合。

### Backend shadow governance and merge entry

- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreGoodsShadowPageReqVO.java` — 影子列表查询入参。
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreGoodsShadowRespVO.java` — 影子详情响应。
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowService.java` — 增加分页、手动归并、忽略接口。
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowServiceImpl.java` — 实现分页、手动归并、忽略。
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsSyncServiceImpl.java` — 复用正式写入逻辑处理手动归并。
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleStoreGoodsShadowController.java` — 影子记录治理接口。
- Modify: `yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowServiceImplTest.java` — 增加归并和忽略测试。

### Frontend APIs and view

- Modify: `ytsh-ui-vue3/src/api/ele/storeGoods/index.ts` — 增加影子列表、详情、归并、忽略 API 类型。
- Modify: `ytsh-ui-vue3/src/views/ele/store-goods/index.vue` — 同步结果区展示 shadowCount，补充入口文案。
- Modify: `ytsh-ui-vue3/src/views/ele/store-goods/components/GovernancePoolPanel.vue` — 增加影子状态筛选、详情展示、手动归并和忽略入口。

---

## Task 1: Add shadow table schema and persistence model

**Files:**
- Modify: `ele_store_goods_full_sync.sql`
- Modify: `yudao-module-business/src/test/resources/sql/create_tables.sql`
- Modify: `yudao-module-business/src/test/resources/sql/clean.sql`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/enums/EleStoreGoodsShadowStatus.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/dataobject/EleStoreGoodsShadowDO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/mysql/EleStoreGoodsShadowMapper.java`

- [ ] **Step 1: Append MySQL DDL**

Append this to `ele_store_goods_full_sync.sql` after `ele_store_goods_full_sync_task_store`:

```sql
CREATE TABLE IF NOT EXISTS `ele_store_goods_shadow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '影子门店品ID',
  `platform_id` bigint NOT NULL COMMENT '平台ID，饿了么=1',
  `merchant_code` varchar(64) NOT NULL COMMENT '商家编码',
  `erp_store_code` varchar(64) NOT NULL COMMENT 'ERP门店编码',
  `platform_store_id` varchar(64) DEFAULT NULL COMMENT '平台门店编码',
  `store_id` varchar(64) DEFAULT NULL COMMENT '本地门店ID',
  `spu_code` varchar(100) DEFAULT NULL COMMENT '三方SPU编码',
  `sku_code` varchar(100) NOT NULL COMMENT '三方SKU编码',
  `sub_sku_code` varchar(100) DEFAULT NULL COMMENT '店内SKU编码',
  `title` varchar(255) DEFAULT NULL COMMENT '商品名称快照',
  `main_pic` varchar(500) DEFAULT NULL COMMENT '主图快照',
  `sub_pics` longtext DEFAULT NULL COMMENT '副图快照JSON',
  `front_category` longtext DEFAULT NULL COMMENT '前台类目JSON',
  `brand_name` varchar(128) DEFAULT NULL COMMENT '品牌名称快照',
  `specification` varchar(255) DEFAULT NULL COMMENT '规格名称快照',
  `sale_price` decimal(24, 6) DEFAULT NULL COMMENT '门店销售价',
  `pos_status` varchar(32) DEFAULT NULL COMMENT '平台上下架状态映射值',
  `is_active` tinyint DEFAULT NULL COMMENT '是否启用',
  `raw_payload` longtext DEFAULT NULL COMMENT '原始报文',
  `match_status` varchar(32) NOT NULL COMMENT 'UNMATCHED/MATCHED/MERGED/CONFLICT/IGNORED',
  `matched_product_sku_id` varchar(64) DEFAULT NULL COMMENT '匹配到的本地SKU ID',
  `merged_store_product_id` varchar(64) DEFAULT NULL COMMENT '归并后的正式门店商品ID',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最近同步时间',
  `matched_time` datetime DEFAULT NULL COMMENT '匹配时间',
  `merged_time` datetime DEFAULT NULL COMMENT '归并时间',
  `conflict_reason` varchar(500) DEFAULT NULL COMMENT '冲突原因',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ele_store_goods_shadow_biz` (`platform_id`, `merchant_code`, `erp_store_code`, `sku_code`, `deleted`),
  KEY `idx_ele_store_goods_shadow_store_status` (`store_id`, `match_status`, `deleted`),
  KEY `idx_ele_store_goods_shadow_platform_status` (`platform_id`, `erp_store_code`, `match_status`, `deleted`),
  KEY `idx_ele_store_goods_shadow_sku_status` (`sku_code`, `match_status`, `deleted`),
  KEY `idx_ele_store_goods_shadow_status_time` (`match_status`, `update_time`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='饿了么门店商品影子快照';
```

- [ ] **Step 2: Append H2 DDL**

Append this to `yudao-module-business/src/test/resources/sql/create_tables.sql` after existing ele/business test tables:

```sql
CREATE TABLE IF NOT EXISTS `ele_store_goods_shadow` (
    `id` bigint NOT NULL GENERATED BY DEFAULT AS IDENTITY,
    `platform_id` bigint NOT NULL,
    `merchant_code` varchar(64) NOT NULL,
    `erp_store_code` varchar(64) NOT NULL,
    `platform_store_id` varchar(64) DEFAULT NULL,
    `store_id` varchar(64) DEFAULT NULL,
    `spu_code` varchar(100) DEFAULT NULL,
    `sku_code` varchar(100) NOT NULL,
    `sub_sku_code` varchar(100) DEFAULT NULL,
    `title` varchar(255) DEFAULT NULL,
    `main_pic` varchar(500) DEFAULT NULL,
    `sub_pics` clob DEFAULT NULL,
    `front_category` clob DEFAULT NULL,
    `brand_name` varchar(128) DEFAULT NULL,
    `specification` varchar(255) DEFAULT NULL,
    `sale_price` decimal(24, 6) DEFAULT NULL,
    `pos_status` varchar(32) DEFAULT NULL,
    `is_active` tinyint DEFAULT NULL,
    `raw_payload` clob DEFAULT NULL,
    `match_status` varchar(32) NOT NULL,
    `matched_product_sku_id` varchar(64) DEFAULT NULL,
    `merged_store_product_id` varchar(64) DEFAULT NULL,
    `last_sync_time` timestamp DEFAULT NULL,
    `matched_time` timestamp DEFAULT NULL,
    `merged_time` timestamp DEFAULT NULL,
    `conflict_reason` varchar(500) DEFAULT NULL,
    `creator` varchar(64) DEFAULT '',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted` bit NOT NULL DEFAULT FALSE,
    `tenant_id` bigint NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`)
);
CREATE UNIQUE INDEX IF NOT EXISTS `uk_ele_store_goods_shadow_biz` ON `ele_store_goods_shadow` (`platform_id`, `merchant_code`, `erp_store_code`, `sku_code`, `deleted`);
CREATE INDEX IF NOT EXISTS `idx_ele_store_goods_shadow_store_status` ON `ele_store_goods_shadow` (`store_id`, `match_status`, `deleted`);
CREATE INDEX IF NOT EXISTS `idx_ele_store_goods_shadow_sku_status` ON `ele_store_goods_shadow` (`sku_code`, `match_status`, `deleted`);
```

- [ ] **Step 3: Add clean SQL**

Append this to `yudao-module-business/src/test/resources/sql/clean.sql`:

```sql
DELETE FROM `ele_store_goods_shadow`;
```

- [ ] **Step 4: Create shadow status constants**

Create `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/enums/EleStoreGoodsShadowStatus.java`:

```java
package cn.iocoder.yudao.module.ele.enums;

public interface EleStoreGoodsShadowStatus {

    String UNMATCHED = "UNMATCHED";
    String MATCHED = "MATCHED";
    String MERGED = "MERGED";
    String CONFLICT = "CONFLICT";
    String IGNORED = "IGNORED";
}
```

- [ ] **Step 5: Create shadow DO**

Create `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/dataobject/EleStoreGoodsShadowDO.java`:

```java
package cn.iocoder.yudao.module.ele.dal.dataobject;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("ele_store_goods_shadow")
@KeySequence("ele_store_goods_shadow_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EleStoreGoodsShadowDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long platformId;
    private String merchantCode;
    private String erpStoreCode;
    private String platformStoreId;
    private String storeId;
    private String spuCode;
    private String skuCode;
    private String subSkuCode;
    private String title;
    private String mainPic;
    private String subPics;
    private String frontCategory;
    private String brandName;
    private String specification;
    private BigDecimal salePrice;
    private String posStatus;
    private Integer isActive;
    private String rawPayload;
    private String matchStatus;
    private String matchedProductSkuId;
    private String mergedStoreProductId;
    private LocalDateTime lastSyncTime;
    private LocalDateTime matchedTime;
    private LocalDateTime mergedTime;
    private String conflictReason;
}
```

- [ ] **Step 6: Create shadow mapper**

Create `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/mysql/EleStoreGoodsShadowMapper.java`:

```java
package cn.iocoder.yudao.module.ele.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsShadowDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface EleStoreGoodsShadowMapper extends BaseMapperX<EleStoreGoodsShadowDO> {

    default EleStoreGoodsShadowDO selectByBizKey(Long platformId, String merchantCode, String erpStoreCode, String skuCode) {
        return selectOne(new LambdaQueryWrapperX<EleStoreGoodsShadowDO>()
                .eq(EleStoreGoodsShadowDO::getPlatformId, platformId)
                .eq(EleStoreGoodsShadowDO::getMerchantCode, merchantCode)
                .eq(EleStoreGoodsShadowDO::getErpStoreCode, erpStoreCode)
                .eq(EleStoreGoodsShadowDO::getSkuCode, skuCode));
    }

    default List<EleStoreGoodsShadowDO> selectActiveList(Collection<String> matchStatuses, String storeId,
                                                         String erpStoreCode, String skuCode, String title) {
        return selectList(new LambdaQueryWrapperX<EleStoreGoodsShadowDO>()
                .inIfPresent(EleStoreGoodsShadowDO::getMatchStatus, matchStatuses)
                .eqIfPresent(EleStoreGoodsShadowDO::getStoreId, storeId)
                .eqIfPresent(EleStoreGoodsShadowDO::getErpStoreCode, erpStoreCode)
                .likeIfPresent(EleStoreGoodsShadowDO::getSkuCode, skuCode)
                .likeIfPresent(EleStoreGoodsShadowDO::getTitle, title)
                .orderByDesc(EleStoreGoodsShadowDO::getUpdateTime));
    }
}
```

- [ ] **Step 7: Run compile check**

Run:

```bash
mvn -pl yudao-module-ele -DskipTests compile
```

Expected: compilation fails only if imports/packages need adjustment; fix package/import issues before proceeding.

- [ ] **Step 8: Commit schema/model task**

```bash
git add ele_store_goods_full_sync.sql yudao-module-business/src/test/resources/sql/create_tables.sql yudao-module-business/src/test/resources/sql/clean.sql yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/enums/EleStoreGoodsShadowStatus.java yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/dataobject/EleStoreGoodsShadowDO.java yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/dal/mysql/EleStoreGoodsShadowMapper.java
git commit -m "feat: add ele store goods shadow model"
```

## Task 2: Implement shadow service upsert and merge markers

**Files:**
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/bo/EleStoreGoodsShadowUpsertReqBO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowService.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowServiceImpl.java`
- Create: `yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowServiceImplTest.java`

- [ ] **Step 1: Write failing upsert test**

Create `yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowServiceImplTest.java`:

```java
package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsShadowDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsShadowMapper;
import cn.iocoder.yudao.module.ele.enums.EleStoreGoodsShadowStatus;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsShadowUpsertReqBO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EleStoreGoodsShadowServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private EleStoreGoodsShadowServiceImpl shadowService;

    @Mock
    private EleStoreGoodsShadowMapper shadowMapper;

    @Test
    void upsertFromSync_shouldInsertUnmatchedShadowWhenMissing() {
        EleStoreGoodsShadowUpsertReqBO reqBO = buildReq();
        when(shadowMapper.selectByBizKey(1L, "MERCHANT001", "STORE001", "SKU001")).thenReturn(null);

        EleStoreGoodsShadowDO result = shadowService.upsertFromSync(reqBO, EleStoreGoodsShadowStatus.UNMATCHED, null, null);

        assertEquals("SKU001", result.getSkuCode());
        assertEquals("UNMATCHED", result.getMatchStatus());
        verify(shadowMapper).insert(any(EleStoreGoodsShadowDO.class));
    }

    @Test
    void markMerged_shouldSetMergedStatusAndFormalIds() {
        EleStoreGoodsShadowDO shadow = new EleStoreGoodsShadowDO();
        shadow.setId(10L);
        shadow.setMatchStatus(EleStoreGoodsShadowStatus.UNMATCHED);
        when(shadowMapper.selectById(10L)).thenReturn(shadow);

        shadowService.markMerged(10L, "1001", "SP0001");

        verify(shadowMapper).updateById(any(EleStoreGoodsShadowDO.class));
    }

    private EleStoreGoodsShadowUpsertReqBO buildReq() {
        EleStoreGoodsShadowUpsertReqBO reqBO = new EleStoreGoodsShadowUpsertReqBO();
        reqBO.setPlatformId(1L);
        reqBO.setMerchantCode("MERCHANT001");
        reqBO.setErpStoreCode("STORE001");
        reqBO.setPlatformStoreId("STORE001");
        reqBO.setStoreId("LOCAL_STORE001");
        reqBO.setSpuCode("SPU001");
        reqBO.setSkuCode("SKU001");
        reqBO.setSubSkuCode("SUB001");
        reqBO.setTitle("测试商品");
        reqBO.setSpecification("默认规格");
        reqBO.setSalePrice(new BigDecimal("12.30"));
        reqBO.setPosStatus("上架");
        reqBO.setIsActive(1);
        reqBO.setRawPayload("{skuCode=SKU001}");
        return reqBO;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -pl yudao-module-ele -Dtest=EleStoreGoodsShadowServiceImplTest test
```

Expected: FAIL because `EleStoreGoodsShadowServiceImpl` and BO/interface do not exist.

- [ ] **Step 3: Create upsert BO**

Create `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/bo/EleStoreGoodsShadowUpsertReqBO.java`:

```java
package cn.iocoder.yudao.module.ele.service.bo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EleStoreGoodsShadowUpsertReqBO {

    private Long platformId;
    private String merchantCode;
    private String erpStoreCode;
    private String platformStoreId;
    private String storeId;
    private String spuCode;
    private String skuCode;
    private String subSkuCode;
    private String title;
    private String mainPic;
    private String subPics;
    private String frontCategory;
    private String brandName;
    private String specification;
    private BigDecimal salePrice;
    private String posStatus;
    private Integer isActive;
    private String rawPayload;
}
```

- [ ] **Step 4: Create shadow service interface**

Create `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowService.java`:

```java
package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsShadowDO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsShadowUpsertReqBO;

public interface EleStoreGoodsShadowService {

    EleStoreGoodsShadowDO upsertFromSync(EleStoreGoodsShadowUpsertReqBO reqBO, String matchStatus,
                                         String matchedProductSkuId, String mergedStoreProductId);

    void markMerged(Long shadowId, String matchedProductSkuId, String mergedStoreProductId);
}
```

- [ ] **Step 5: Implement shadow service**

Create `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowServiceImpl.java`:

```java
package cn.iocoder.yudao.module.ele.service;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsShadowDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsShadowMapper;
import cn.iocoder.yudao.module.ele.enums.EleStoreGoodsShadowStatus;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsShadowUpsertReqBO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EleStoreGoodsShadowServiceImpl implements EleStoreGoodsShadowService {

    @Resource
    private EleStoreGoodsShadowMapper shadowMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EleStoreGoodsShadowDO upsertFromSync(EleStoreGoodsShadowUpsertReqBO reqBO, String matchStatus,
                                                String matchedProductSkuId, String mergedStoreProductId) {
        EleStoreGoodsShadowDO exist = shadowMapper.selectByBizKey(reqBO.getPlatformId(), reqBO.getMerchantCode(),
                reqBO.getErpStoreCode(), reqBO.getSkuCode());
        EleStoreGoodsShadowDO row = exist == null ? new EleStoreGoodsShadowDO() : new EleStoreGoodsShadowDO();
        if (exist != null) {
            row.setId(exist.getId());
        }
        fillFromReq(row, reqBO);
        row.setMatchStatus(matchStatus);
        row.setMatchedProductSkuId(matchedProductSkuId);
        row.setMergedStoreProductId(mergedStoreProductId);
        row.setLastSyncTime(LocalDateTime.now());
        if (EleStoreGoodsShadowStatus.MERGED.equals(matchStatus)) {
            row.setMatchedTime(LocalDateTime.now());
            row.setMergedTime(LocalDateTime.now());
        }
        if (exist == null) {
            shadowMapper.insert(row);
            return row;
        }
        shadowMapper.updateById(row);
        return shadowMapper.selectById(exist.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markMerged(Long shadowId, String matchedProductSkuId, String mergedStoreProductId) {
        EleStoreGoodsShadowDO exist = shadowMapper.selectById(shadowId);
        if (exist == null) {
            throw new RuntimeException("影子门店品不存在");
        }
        EleStoreGoodsShadowDO updateObj = new EleStoreGoodsShadowDO();
        updateObj.setId(shadowId);
        updateObj.setMatchStatus(EleStoreGoodsShadowStatus.MERGED);
        updateObj.setMatchedProductSkuId(StrUtil.trim(matchedProductSkuId));
        updateObj.setMergedStoreProductId(StrUtil.trim(mergedStoreProductId));
        updateObj.setMatchedTime(LocalDateTime.now());
        updateObj.setMergedTime(LocalDateTime.now());
        shadowMapper.updateById(updateObj);
    }

    private void fillFromReq(EleStoreGoodsShadowDO row, EleStoreGoodsShadowUpsertReqBO reqBO) {
        row.setPlatformId(reqBO.getPlatformId());
        row.setMerchantCode(StrUtil.trim(reqBO.getMerchantCode()));
        row.setErpStoreCode(StrUtil.trim(reqBO.getErpStoreCode()));
        row.setPlatformStoreId(StrUtil.trim(reqBO.getPlatformStoreId()));
        row.setStoreId(StrUtil.trim(reqBO.getStoreId()));
        row.setSpuCode(StrUtil.trim(reqBO.getSpuCode()));
        row.setSkuCode(StrUtil.trim(reqBO.getSkuCode()));
        row.setSubSkuCode(StrUtil.trim(reqBO.getSubSkuCode()));
        row.setTitle(StrUtil.trim(reqBO.getTitle()));
        row.setMainPic(StrUtil.trim(reqBO.getMainPic()));
        row.setSubPics(reqBO.getSubPics());
        row.setFrontCategory(reqBO.getFrontCategory());
        row.setBrandName(StrUtil.trim(reqBO.getBrandName()));
        row.setSpecification(StrUtil.trim(reqBO.getSpecification()));
        row.setSalePrice(reqBO.getSalePrice());
        row.setPosStatus(StrUtil.trim(reqBO.getPosStatus()));
        row.setIsActive(reqBO.getIsActive());
        row.setRawPayload(reqBO.getRawPayload());
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run:

```bash
mvn -pl yudao-module-ele -Dtest=EleStoreGoodsShadowServiceImplTest test
```

Expected: PASS.

- [ ] **Step 7: Commit shadow service task**

```bash
git add yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/bo/EleStoreGoodsShadowUpsertReqBO.java yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowService.java yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowServiceImpl.java yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowServiceImplTest.java
git commit -m "feat: add store goods shadow service"
```

## Task 3: Adjust store goods sync to write shadows before SKU matching

**Files:**
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/bo/EleStoreGoodsPageSyncResult.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsSyncService.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleStoreGoodsSyncController.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsSyncServiceImpl.java`
- Modify: `yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsSyncServiceImplTest.java`

- [ ] **Step 1: Update failing SKU-missing test expectation**

In `EleStoreGoodsSyncServiceImplTest`, replace `syncStoreGoodsPage_shouldCountGovernanceWhenSkuMissing` assertions with:

```java
assertEquals(1, result.getSyncCount());
assertEquals(0, result.getSuccessCount());
assertEquals(0, result.getFailCount());
assertEquals(1, result.getGovernanceCount());
assertEquals(1, result.getShadowCount());
verify(governanceService).create(org.mockito.ArgumentMatchers.any());
verify(syncLogService).create(org.mockito.ArgumentMatchers.any());
verify(storeProductSyncWriteService, never()).upsertStoreProduct(org.mockito.ArgumentMatchers.any());
verify(shadowService).upsertFromSync(org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.eq("UNMATCHED"),
        org.mockito.ArgumentMatchers.isNull(),
        org.mockito.ArgumentMatchers.isNull());
```

Add a mock field near other mocks:

```java
@Mock
private EleStoreGoodsShadowService shadowService;
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -pl yudao-module-ele -Dtest=EleStoreGoodsSyncServiceImplTest#syncStoreGoodsPage_shouldCountGovernanceWhenSkuMissing test
```

Expected: FAIL because `shadowCount` and `shadowService` integration are not implemented.

- [ ] **Step 3: Add shadow count field**

Modify `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/bo/EleStoreGoodsPageSyncResult.java` by adding:

```java
private Integer shadowCount;
```

Ensure Lombok `@Data` already exposes getter/setter.

- [ ] **Step 4: Inject shadow service and initialize shadow count**

In `EleStoreGoodsSyncServiceImpl`, add resource:

```java
@Resource
private EleStoreGoodsShadowService shadowService;
```

In `syncStoreGoodsPage`, after governance count initialization, add:

```java
result.setShadowCount(0);
```

- [ ] **Step 5: Build shadow upsert request helper**

Add this private helper to `EleStoreGoodsSyncServiceImpl`:

```java
private EleStoreGoodsShadowUpsertReqBO buildShadowReq(EleStoreGoodsSyncReqBO reqBO, StorePlatformRespVO store) {
    EleStoreGoodsShadowUpsertReqBO shadowReqBO = new EleStoreGoodsShadowUpsertReqBO();
    shadowReqBO.setPlatformId(ELE_PLATFORM_ID);
    shadowReqBO.setMerchantCode(StrUtil.trim(reqBO.getMerchantCode()));
    shadowReqBO.setErpStoreCode(resolveErpStoreCode(reqBO));
    shadowReqBO.setPlatformStoreId(store == null ? StrUtil.trim(reqBO.getPlatformStoreId()) : store.getPlatformStoreId());
    shadowReqBO.setStoreId(store == null ? null : store.getStoreId());
    shadowReqBO.setSpuCode(StrUtil.trim(reqBO.getSpuCode()));
    shadowReqBO.setSkuCode(StrUtil.trim(reqBO.getSkuCode()));
    shadowReqBO.setSubSkuCode(StrUtil.trim(reqBO.getSubSkuCode()));
    shadowReqBO.setSalePrice(reqBO.getStoreProductPrice());
    shadowReqBO.setPosStatus(reqBO.getStoreProductPosStatus());
    shadowReqBO.setIsActive(reqBO.getStoreProductIsActive());
    shadowReqBO.setRawPayload(StrUtil.blankToDefault(reqBO.getRawPayload(), reqBO.getResponseBody()));
    return shadowReqBO;
}
```

Add import:

```java
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsShadowUpsertReqBO;
import cn.iocoder.yudao.module.ele.enums.EleStoreGoodsShadowStatus;
```

- [ ] **Step 6: Set title/specification/mainPic in sync request**

Extend `EleStoreGoodsSyncReqBO` with:

```java
private String title;
private String mainPic;
private String specification;
```

In `buildSyncReq`, add:

```java
syncReqBO.setTitle(goodsItem.getTitle());
syncReqBO.setMainPic(goodsItem.getMainPic());
syncReqBO.setSpecification(skuItem.getSpecification());
```

Then update `buildShadowReq`:

```java
shadowReqBO.setTitle(StrUtil.trim(reqBO.getTitle()));
shadowReqBO.setMainPic(StrUtil.trim(reqBO.getMainPic()));
shadowReqBO.setSpecification(StrUtil.trim(reqBO.getSpecification()));
```

- [ ] **Step 7: Write shadow before SKU-missing governance**

In `doSyncStoreGoods`, replace the SKU-missing branch with:

```java
if (sku == null) {
    for (StorePlatformRespVO store : stores) {
        shadowService.upsertFromSync(buildShadowReq(reqBO, store), EleStoreGoodsShadowStatus.UNMATCHED, null, null);
        createGovernanceRecord(reqBO, store, testMode);
        writeFailureLog(reqBO, store, GOVERNANCE_REASON_SKU_NOT_FOUND,
                appendTestMode(testMode, "skuCode未匹配本地SKU，已写入影子门店品: " + skuCode));
    }
    return false;
}
```

In `syncStoreGoodsPage`, after `boolean success = doSyncStoreGoods(syncReqBO);`, change counters to:

```java
result.setSyncCount(result.getSyncCount() + 1);
if (success) {
    result.setSuccessCount(result.getSuccessCount() + 1);
} else {
    result.setShadowCount(result.getShadowCount() + 1);
    result.setGovernanceCount(result.getGovernanceCount() + 1);
}
```

Do not increment `failCount` for SKU-missing shadow success.

- [ ] **Step 8: Mark matching SKU as merged**

In `upsertStoreProduct`, capture returned store product id and call shadow service:

```java
String storeProductId = storeProductSyncWriteService.upsertStoreProduct(upsertReqBO);
shadowService.upsertFromSync(buildShadowReq(reqBO, store), EleStoreGoodsShadowStatus.MERGED,
        String.valueOf(sku.getProductSkuId()), storeProductId);
```

Keep existing success log.

- [ ] **Step 9: Return page sync result from query-sync API**

Change `EleStoreGoodsSyncService` signature:

```java
EleStoreGoodsPageSyncResult queryAndSyncStoreGoods(EleStoreGoodsQueryReqBO reqBO, Boolean testMode);
```

Change `EleStoreGoodsSyncServiceImpl.queryAndSyncStoreGoods`:

```java
@Override
public EleStoreGoodsPageSyncResult queryAndSyncStoreGoods(EleStoreGoodsQueryReqBO reqBO, Boolean testMode) {
    return syncStoreGoodsPage(reqBO, testMode);
}
```

Change `EleStoreGoodsSyncController.queryAndSyncStoreGoods` return type and body:

```java
public CommonResult<EleStoreGoodsPageSyncResult> queryAndSyncStoreGoods(@Valid @RequestBody EleStoreGoodsQueryReqVO reqVO,
                                                                        @RequestParam(defaultValue = "false") Boolean testMode) {
    EleStoreGoodsQueryReqBO reqBO = BeanUtils.toBean(reqVO, EleStoreGoodsQueryReqBO.class);
    return CommonResult.success(eleStoreGoodsSyncService.queryAndSyncStoreGoods(reqBO, testMode));
}
```

Add import in controller:

```java
import cn.iocoder.yudao.module.ele.service.bo.EleStoreGoodsPageSyncResult;
```

- [ ] **Step 10: Run adjusted sync tests**

Run:

```bash
mvn -pl yudao-module-ele -Dtest=EleStoreGoodsSyncServiceImplTest test
```

Expected: PASS.

- [ ] **Step 11: Commit sync integration task**

```bash
git add yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/bo/EleStoreGoodsPageSyncResult.java yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/bo/EleStoreGoodsSyncReqBO.java yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsSyncService.java yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleStoreGoodsSyncController.java yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsSyncServiceImpl.java yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsSyncServiceImplTest.java
git commit -m "feat: write shadow records during store goods sync"
```

## Task 4: Add shadow query boundary for business list aggregation

**Files:**
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/bo/StoreProductShadowRowBO.java`
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductShadowQueryService.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/StoreProductShadowQueryServiceImpl.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductPageReqVO.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductRespVO.java`

- [ ] **Step 1: Create shadow row BO**

Create `StoreProductShadowRowBO.java`:

```java
package cn.iocoder.yudao.module.business.service.store.bo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StoreProductShadowRowBO {

    private Long shadowId;
    private String storeId;
    private String erpStoreCode;
    private String platformStoreId;
    private String skuCode;
    private String spuCode;
    private String productName;
    private String specification;
    private BigDecimal price;
    private String posStatus;
    private Integer isActive;
    private String matchStatus;
    private LocalDateTime createTime;
}
```

- [ ] **Step 2: Create business query interface**

Create `StoreProductShadowQueryService.java`:

```java
package cn.iocoder.yudao.module.business.service.store;

import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductPageReqVO;
import cn.iocoder.yudao.module.business.service.store.bo.StoreProductShadowRowBO;

import java.util.List;
import java.util.Set;

public interface StoreProductShadowQueryService {

    List<StoreProductShadowRowBO> listActiveShadowRows(StoreProductPageReqVO pageReqVO, Set<String> formalSkuCodes);
}
```

- [ ] **Step 3: Implement query interface in ele module**

Create `StoreProductShadowQueryServiceImpl.java`:

```java
package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductPageReqVO;
import cn.iocoder.yudao.module.business.service.store.StoreProductShadowQueryService;
import cn.iocoder.yudao.module.business.service.store.bo.StoreProductShadowRowBO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsShadowDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsShadowMapper;
import cn.iocoder.yudao.module.ele.enums.EleStoreGoodsShadowStatus;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class StoreProductShadowQueryServiceImpl implements StoreProductShadowQueryService {

    @Resource
    private EleStoreGoodsShadowMapper shadowMapper;

    @Override
    public List<StoreProductShadowRowBO> listActiveShadowRows(StoreProductPageReqVO pageReqVO, Set<String> formalSkuCodes) {
        List<EleStoreGoodsShadowDO> shadows = shadowMapper.selectActiveList(
                List.of(EleStoreGoodsShadowStatus.UNMATCHED, EleStoreGoodsShadowStatus.CONFLICT),
                pageReqVO.getStoreId(), null, pageReqVO.getSkuCode(), pageReqVO.getSkuName());
        return shadows.stream()
                .filter(item -> formalSkuCodes == null || !formalSkuCodes.contains(item.getSkuCode()))
                .map(this::toRow)
                .toList();
    }

    private StoreProductShadowRowBO toRow(EleStoreGoodsShadowDO shadow) {
        StoreProductShadowRowBO row = new StoreProductShadowRowBO();
        row.setShadowId(shadow.getId());
        row.setStoreId(shadow.getStoreId());
        row.setErpStoreCode(shadow.getErpStoreCode());
        row.setPlatformStoreId(shadow.getPlatformStoreId());
        row.setSkuCode(shadow.getSkuCode());
        row.setSpuCode(shadow.getSpuCode());
        row.setProductName(shadow.getTitle());
        row.setSpecification(shadow.getSpecification());
        row.setPrice(shadow.getSalePrice());
        row.setPosStatus(shadow.getPosStatus());
        row.setIsActive(shadow.getIsActive());
        row.setMatchStatus(shadow.getMatchStatus());
        row.setCreateTime(shadow.getCreateTime());
        return row;
    }
}
```

- [ ] **Step 4: Extend page request VO**

Add fields to `StoreProductPageReqVO`:

```java
@Schema(description = "数据来源 FORMAL/SHADOW")
private String rowSource;

@Schema(description = "完整性状态 COMPLETE/MASTER_MISSING")
private String completenessStatus;

@Schema(description = "影子匹配状态 UNMATCHED/CONFLICT/MERGED/IGNORED")
private String matchStatus;
```

- [ ] **Step 5: Extend response VO**

Add fields to `StoreProductRespVO`:

```java
@Schema(description = "数据来源 FORMAL/SHADOW")
private String rowSource;

@Schema(description = "影子记录ID")
private Long shadowId;

@Schema(description = "完整性状态 COMPLETE/MASTER_MISSING")
private String completenessStatus;

@Schema(description = "影子匹配状态")
private String matchStatus;

@Schema(description = "平台门店编码")
private String platformStoreId;

@Schema(description = "SPU编码")
private String spuCode;

@Schema(description = "规格")
private String specification;
```

- [ ] **Step 6: Run compile check**

Run:

```bash
mvn -pl yudao-server -am -DskipTests compile
```

Expected: PASS.

- [ ] **Step 7: Commit query boundary task**

```bash
git add yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/bo/StoreProductShadowRowBO.java yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductShadowQueryService.java yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/StoreProductShadowQueryServiceImpl.java yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductPageReqVO.java yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductRespVO.java
git commit -m "feat: expose store goods shadow rows to business list"
```

## Task 5: Aggregate formal and shadow rows in store product page

**Files:**
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductServiceImpl.java`
- Create: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/store/StoreProductServiceImplTest.java`

- [ ] **Step 1: Write failing aggregation unit test**

Create `StoreProductServiceImplTest.java` with Mockito-based test:

```java
package cn.iocoder.yudao.module.business.service.store;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreProductDO;
import cn.iocoder.yudao.module.business.dal.mysql.product.SkuTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreProductMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreStockMapper;
import cn.iocoder.yudao.module.business.service.store.bo.StoreProductShadowRowBO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class StoreProductServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private StoreProductServiceImpl storeProductService;

    @Mock
    private StoreProductMapper storeProductMapper;
    @Mock
    private StoreStockMapper storeStockMapper;
    @Mock
    private StoreMapper storeMapper;
    @Mock
    private SkuTableMapper skuTableMapper;
    @Mock
    private StoreProductShadowQueryService shadowQueryService;

    @Test
    void getStoreProductPage_shouldAppendShadowRows() {
        StoreProductPageReqVO reqVO = new StoreProductPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        StoreProductDO formal = new StoreProductDO();
        formal.setStoreProductId("SP001");
        formal.setStoreId("STORE001");
        formal.setProductSkuId("1001");
        formal.setStoreProductPrice(new BigDecimal("10.00"));
        formal.setStoreProductIsActive(1);
        when(storeProductMapper.selectPage(any(), any())).thenReturn(new PageResult<>(List.of(formal), 1L));
        StoreProductShadowRowBO shadow = new StoreProductShadowRowBO();
        shadow.setShadowId(20L);
        shadow.setStoreId("STORE001");
        shadow.setSkuCode("SKU_MISSING");
        shadow.setProductName("缺主档商品");
        shadow.setPrice(new BigDecimal("12.30"));
        shadow.setMatchStatus("UNMATCHED");
        when(shadowQueryService.listActiveShadowRows(any(), any())).thenReturn(List.of(shadow));

        PageResult<StoreProductRespVO> result = storeProductService.getStoreProductPage(reqVO);

        assertEquals(2, result.getList().size());
        assertEquals("FORMAL", result.getList().get(0).getRowSource());
        assertEquals("SHADOW", result.getList().get(1).getRowSource());
        assertEquals("MASTER_MISSING", result.getList().get(1).getCompletenessStatus());
        assertEquals(2L, result.getTotal());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -pl yudao-module-business -Dtest=StoreProductServiceImplTest#getStoreProductPage_shouldAppendShadowRows test
```

Expected: FAIL because `StoreProductServiceImpl` does not yet inject or append shadow rows.

- [ ] **Step 3: Inject shadow query service**

In `StoreProductServiceImpl`, add:

```java
@Resource
private StoreProductShadowQueryService shadowQueryService;
```

- [ ] **Step 4: Set formal row metadata**

In `buildRespVO`, before returning, add:

```java
respVO.setRowSource("FORMAL");
respVO.setCompletenessStatus("COMPLETE");
respVO.setMatchStatus("MERGED");
```

- [ ] **Step 5: Add shadow response builder**

Add private method to `StoreProductServiceImpl`:

```java
private StoreProductRespVO buildShadowRespVO(StoreProductShadowRowBO shadow) {
    StoreProductRespVO respVO = new StoreProductRespVO();
    respVO.setRowSource("SHADOW");
    respVO.setShadowId(shadow.getShadowId());
    respVO.setStoreId(shadow.getStoreId());
    respVO.setPlatformStoreId(shadow.getPlatformStoreId());
    respVO.setSkuCode(shadow.getSkuCode());
    respVO.setSkuName(shadow.getProductName());
    respVO.setSpuCode(shadow.getSpuCode());
    respVO.setSpecification(shadow.getSpecification());
    respVO.setStoreRetailPrice(shadow.getPrice());
    respVO.setPosStatus(parseInteger(shadow.getPosStatus()));
    respVO.setEnterShopStatus(shadow.getIsActive());
    respVO.setCompletenessStatus("MASTER_MISSING");
    respVO.setMatchStatus(shadow.getMatchStatus());
    respVO.setCreateTime(shadow.getCreateTime());
    return respVO;
}
```

- [ ] **Step 6: Aggregate rows in `getStoreProductPage`**

After formal `respList` is built, add:

```java
Set<String> formalSkuCodes = respList.stream()
        .map(StoreProductRespVO::getSkuCode)
        .filter(StrUtil::isNotBlank)
        .collect(Collectors.toSet());
List<StoreProductRespVO> shadowRows = shadowQueryService.listActiveShadowRows(pageReqVO, formalSkuCodes)
        .stream()
        .map(this::buildShadowRespVO)
        .collect(Collectors.toList());
if ("FORMAL".equalsIgnoreCase(pageReqVO.getRowSource())) {
    return new PageResult<>(respList, pageResult.getTotal());
}
if ("SHADOW".equalsIgnoreCase(pageReqVO.getRowSource()) || "MASTER_MISSING".equalsIgnoreCase(pageReqVO.getCompletenessStatus())) {
    return new PageResult<>(shadowRows, (long) shadowRows.size());
}
List<StoreProductRespVO> mergedRows = new ArrayList<>(respList);
mergedRows.addAll(shadowRows);
return new PageResult<>(mergedRows, pageResult.getTotal() + shadowRows.size());
```

Replace the original `return new PageResult<>(respList, pageResult.getTotal());`.

- [ ] **Step 7: Run aggregation test**

Run:

```bash
mvn -pl yudao-module-business -Dtest=StoreProductServiceImplTest test
```

Expected: PASS.

- [ ] **Step 8: Commit aggregation task**

```bash
git add yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductServiceImpl.java yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/store/StoreProductServiceImplTest.java
git commit -m "feat: aggregate shadow store goods in product list"
```

## Task 6: Add shadow governance API

**Files:**
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreGoodsShadowPageReqVO.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreGoodsShadowRespVO.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowService.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowServiceImpl.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsSyncServiceImpl.java`
- Create: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleStoreGoodsShadowController.java`
- Modify: `yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowServiceImplTest.java`

- [ ] **Step 1: Extend shadow service test for ignore**

Add test to `EleStoreGoodsShadowServiceImplTest`:

```java
@Test
void ignore_shouldMarkShadowIgnored() {
    EleStoreGoodsShadowDO shadow = new EleStoreGoodsShadowDO();
    shadow.setId(11L);
    shadow.setMatchStatus(EleStoreGoodsShadowStatus.UNMATCHED);
    when(shadowMapper.selectById(11L)).thenReturn(shadow);

    shadowService.ignore(11L);

    verify(shadowMapper).updateById(any(EleStoreGoodsShadowDO.class));
}

@Test
void mergeManually_shouldMarkShadowMerged() {
    EleStoreGoodsShadowDO shadow = new EleStoreGoodsShadowDO();
    shadow.setId(12L);
    shadow.setStoreId("STORE001");
    shadow.setSkuCode("SKU001");
    shadow.setMatchStatus(EleStoreGoodsShadowStatus.UNMATCHED);
    when(shadowMapper.selectById(12L)).thenReturn(shadow);

    shadowService.mergeManually(12L, "1001", "SP0001");

    verify(shadowMapper).updateById(any(EleStoreGoodsShadowDO.class));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn -pl yudao-module-ele -Dtest=EleStoreGoodsShadowServiceImplTest#ignore_shouldMarkShadowIgnored test
```

Expected: FAIL because `ignore` and `mergeManually` do not exist.

- [ ] **Step 3: Add request and response VO**

Create `EleStoreGoodsShadowPageReqVO.java`:

```java
package cn.iocoder.yudao.module.ele.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 饿了么门店商品影子分页 Request VO")
@Data
public class EleStoreGoodsShadowPageReqVO extends PageParam {

    private String erpStoreCode;
    private String platformStoreId;
    private String storeId;
    private String skuCode;
    private String title;
    private String matchStatus;
}
```

Create `EleStoreGoodsShadowRespVO.java`:

```java
package cn.iocoder.yudao.module.ele.controller.admin.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EleStoreGoodsShadowRespVO {

    private Long id;
    private Long platformId;
    private String merchantCode;
    private String erpStoreCode;
    private String platformStoreId;
    private String storeId;
    private String spuCode;
    private String skuCode;
    private String subSkuCode;
    private String title;
    private String mainPic;
    private String specification;
    private BigDecimal salePrice;
    private String posStatus;
    private Integer isActive;
    private String matchStatus;
    private String matchedProductSkuId;
    private String mergedStoreProductId;
    private String conflictReason;
    private String rawPayload;
    private LocalDateTime lastSyncTime;
    private LocalDateTime createTime;
}
```

- [ ] **Step 4: Extend service interface**

Add to `EleStoreGoodsShadowService`:

```java
PageResult<EleStoreGoodsShadowRespVO> getShadowPage(EleStoreGoodsShadowPageReqVO reqVO);

EleStoreGoodsShadowRespVO getShadow(Long id);

void ignore(Long id);

void mergeManually(Long id, String matchedProductSkuId, String mergedStoreProductId);
```

Add imports:

```java
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsShadowPageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsShadowRespVO;
```

- [ ] **Step 5: Implement ignore and query methods**

Add to `EleStoreGoodsShadowServiceImpl`:

```java
@Override
public PageResult<EleStoreGoodsShadowRespVO> getShadowPage(EleStoreGoodsShadowPageReqVO reqVO) {
    PageResult<EleStoreGoodsShadowDO> page = shadowMapper.selectPage(reqVO, new LambdaQueryWrapperX<EleStoreGoodsShadowDO>()
            .eqIfPresent(EleStoreGoodsShadowDO::getErpStoreCode, reqVO.getErpStoreCode())
            .eqIfPresent(EleStoreGoodsShadowDO::getPlatformStoreId, reqVO.getPlatformStoreId())
            .eqIfPresent(EleStoreGoodsShadowDO::getStoreId, reqVO.getStoreId())
            .likeIfPresent(EleStoreGoodsShadowDO::getSkuCode, reqVO.getSkuCode())
            .likeIfPresent(EleStoreGoodsShadowDO::getTitle, reqVO.getTitle())
            .eqIfPresent(EleStoreGoodsShadowDO::getMatchStatus, reqVO.getMatchStatus())
            .orderByDesc(EleStoreGoodsShadowDO::getUpdateTime));
    return BeanUtils.toBean(page, EleStoreGoodsShadowRespVO.class);
}

@Override
public EleStoreGoodsShadowRespVO getShadow(Long id) {
    EleStoreGoodsShadowDO shadow = shadowMapper.selectById(id);
    return shadow == null ? null : BeanUtils.toBean(shadow, EleStoreGoodsShadowRespVO.class);
}

@Override
@Transactional(rollbackFor = Exception.class)
public void ignore(Long id) {
    EleStoreGoodsShadowDO exist = shadowMapper.selectById(id);
    if (exist == null) {
        throw new RuntimeException("影子门店品不存在");
    }
    EleStoreGoodsShadowDO updateObj = new EleStoreGoodsShadowDO();
    updateObj.setId(id);
    updateObj.setMatchStatus(EleStoreGoodsShadowStatus.IGNORED);
    shadowMapper.updateById(updateObj);
}

@Override
@Transactional(rollbackFor = Exception.class)
public void mergeManually(Long id, String matchedProductSkuId, String mergedStoreProductId) {
    EleStoreGoodsShadowDO exist = shadowMapper.selectById(id);
    if (exist == null) {
        throw new RuntimeException("影子门店品不存在");
    }
    markMerged(id, matchedProductSkuId, mergedStoreProductId);
}
```

Add imports:

```java
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsShadowPageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsShadowRespVO;
```

- [ ] **Step 6: Create controller**

Create `EleStoreGoodsShadowController.java`:

```java
package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsShadowPageReqVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreGoodsShadowRespVO;
import cn.iocoder.yudao.module.ele.service.EleStoreGoodsShadowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 饿了么门店商品影子治理")
@RestController
@RequestMapping("/ele/store-goods/shadow")
@Validated
@TenantIgnore
public class EleStoreGoodsShadowController {

    @Resource
    private EleStoreGoodsShadowService shadowService;

    @GetMapping("/page")
    @Operation(summary = "分页查询影子门店品")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<PageResult<EleStoreGoodsShadowRespVO>> getShadowPage(@Valid EleStoreGoodsShadowPageReqVO reqVO) {
        return success(shadowService.getShadowPage(reqVO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取影子门店品详情")
    @PreAuthorize("@ss.hasPermission('ele:order:query')")
    public CommonResult<EleStoreGoodsShadowRespVO> getShadow(@PathVariable Long id) {
        return success(shadowService.getShadow(id));
    }

    @PutMapping("/{id}/ignored")
    @Operation(summary = "忽略影子门店品")
    @PreAuthorize("@ss.hasPermission('ele:order:sync')")
    public CommonResult<Boolean> ignore(@PathVariable Long id) {
        shadowService.ignore(id);
        return success(true);
    }

    @PutMapping("/{id}/merge")
    @Operation(summary = "手动归并影子门店品")
    @PreAuthorize("@ss.hasPermission('ele:order:sync')")
    public CommonResult<Boolean> merge(@PathVariable Long id,
                                       @RequestParam String matchedProductSkuId,
                                       @RequestParam String mergedStoreProductId) {
        shadowService.mergeManually(id, matchedProductSkuId, mergedStoreProductId);
        return success(true);
    }
}
```

- [ ] **Step 7: Run shadow tests and compile**

Run:

```bash
mvn -pl yudao-module-ele -Dtest=EleStoreGoodsShadowServiceImplTest test
mvn -pl yudao-server -am -DskipTests compile
```

Expected: both PASS.

- [ ] **Step 8: Commit governance API task**

```bash
git add yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreGoodsShadowPageReqVO.java yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreGoodsShadowRespVO.java yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowService.java yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowServiceImpl.java yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsSyncServiceImpl.java yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/EleStoreGoodsShadowController.java yudao-module-ele/src/test/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowServiceImplTest.java
git commit -m "feat: add store goods shadow governance api"
```

## Task 7: Add frontend API and governance display

**Files:**
- Modify: `ytsh-ui-vue3/src/api/ele/storeGoods/index.ts`
- Modify: `ytsh-ui-vue3/src/views/ele/store-goods/index.vue`
- Modify: `ytsh-ui-vue3/src/views/ele/store-goods/components/GovernancePoolPanel.vue`

- [ ] **Step 1: Extend frontend API types**

Append to `ytsh-ui-vue3/src/api/ele/storeGoods/index.ts` before full-sync APIs:

```ts
export interface StoreGoodsShadowReqVO {
  erpStoreCode?: string
  platformStoreId?: string
  storeId?: string
  skuCode?: string
  title?: string
  matchStatus?: string
  pageNo?: number
  pageSize?: number
}

export interface StoreGoodsShadowRespVO {
  id?: number
  platformId?: number
  merchantCode?: string
  erpStoreCode?: string
  platformStoreId?: string
  storeId?: string
  spuCode?: string
  skuCode?: string
  subSkuCode?: string
  title?: string
  mainPic?: string
  specification?: string
  salePrice?: number
  posStatus?: string
  isActive?: number
  matchStatus?: string
  matchedProductSkuId?: string
  mergedStoreProductId?: string
  conflictReason?: string
  rawPayload?: string
  lastSyncTime?: string
  createTime?: string
}

export interface StoreGoodsShadowPageRespVO {
  list?: StoreGoodsShadowRespVO[]
  total?: number
}

export const getShadowPage = async (params: StoreGoodsShadowReqVO) => {
  return await request.get<StoreGoodsShadowPageRespVO>({ url: '/ele/store-goods/shadow/page', params })
}

export const getShadow = async (id: number) => {
  return await request.get<StoreGoodsShadowRespVO>({ url: `/ele/store-goods/shadow/${id}` })
}

export const ignoreShadow = async (id: number) => {
  return await request.put<boolean>({ url: `/ele/store-goods/shadow/${id}/ignored` })
}

export const mergeShadow = async (id: number, matchedProductSkuId: string, mergedStoreProductId: string) => {
  return await request.put<boolean>({
    url: `/ele/store-goods/shadow/${id}/merge`,
    params: { matchedProductSkuId, mergedStoreProductId }
  })
}
```

- [ ] **Step 2: Show shadow count after query sync**

In `index.vue`, after `handleQueryAndSync` receives result, change the success message from a single count to include shadow count when backend returns object. If current API still returns number, keep compatibility:

```ts
const syncCount = typeof res === 'number' ? res : res?.syncCount || 0
const shadowCount = typeof res === 'number' ? 0 : res?.shadowCount || 0
ElMessage.success(`同步完成：处理 ${syncCount} 条，影子门店品 ${shadowCount} 条`)
```

Update `queryAndSyncStoreGoods` return type to `StoreGoodsPageSyncResultVO`:

```ts
export interface StoreGoodsPageSyncResultVO {
  pageNo?: number
  pageSize?: number
  total?: number
  syncCount?: number
  successCount?: number
  failCount?: number
  governanceCount?: number
  shadowCount?: number
}
```

Then change `queryAndSyncStoreGoods` implementation to:

```ts
export const queryAndSyncStoreGoods = async (data: StoreGoodsQueryReqVO, testMode = false) => {
  return await request.post<StoreGoodsPageSyncResultVO>({
    url: '/ele/store-goods/query-sync',
    data,
    params: { testMode }
  })
}
```

- [ ] **Step 3: Add shadow tab to governance panel**

In `GovernancePoolPanel.vue`, import APIs:

```ts
import { getShadowPage, getShadow, ignoreShadow, type StoreGoodsShadowRespVO } from '@/api/ele/storeGoods'
```

Add reactive state:

```ts
const shadowLoading = ref(false)
const shadowList = ref<StoreGoodsShadowRespVO[]>([])
const shadowTotal = ref(0)
const shadowFilters = reactive({ pageNo: 1, pageSize: 10, skuCode: '', title: '', matchStatus: '' })
```

Add loader:

```ts
const loadShadows = async () => {
  shadowLoading.value = true
  try {
    const res = await getShadowPage(shadowFilters)
    shadowList.value = res.list || []
    shadowTotal.value = res.total || 0
  } finally {
    shadowLoading.value = false
  }
}
```

Add ignore handler:

```ts
const handleIgnoreShadow = async (row: StoreGoodsShadowRespVO) => {
  if (!row.id) return
  await ignoreShadow(row.id)
  ElMessage.success('已忽略影子门店品')
  await loadShadows()
}
```

Add mounted call:

```ts
onMounted(() => {
  loadShadows()
})
```

- [ ] **Step 4: Add shadow table markup**

In `GovernancePoolPanel.vue` template, add a second table section below existing governance pool table:

```vue
<div class="section-header mt-16">
  <span class="section-title">影子门店品</span>
</div>
<div class="log-filter">
  <el-input v-model="shadowFilters.skuCode" placeholder="SKU编码" clearable class="text-input" />
  <el-input v-model="shadowFilters.title" placeholder="商品名称" clearable class="text-input" />
  <el-select v-model="shadowFilters.matchStatus" placeholder="匹配状态" clearable class="number-input">
    <el-option label="未匹配" value="UNMATCHED" />
    <el-option label="冲突" value="CONFLICT" />
    <el-option label="已归并" value="MERGED" />
    <el-option label="已忽略" value="IGNORED" />
  </el-select>
  <el-button type="primary" @click="loadShadows">搜索</el-button>
</div>
<el-table :data="shadowList" v-loading="shadowLoading" border stripe style="width: 100%">
  <el-table-column prop="title" label="商品名称" min-width="180" show-overflow-tooltip />
  <el-table-column prop="skuCode" label="SKU编码" min-width="130" show-overflow-tooltip />
  <el-table-column prop="spuCode" label="SPU编码" min-width="130" show-overflow-tooltip />
  <el-table-column prop="erpStoreCode" label="ERP门店" min-width="130" show-overflow-tooltip />
  <el-table-column prop="salePrice" label="售价" width="100" align="right" />
  <el-table-column label="状态" width="110" align="center">
    <template #default="{ row }">
      <el-tag :type="row.matchStatus === 'CONFLICT' ? 'danger' : 'warning'">{{ row.matchStatus }}</el-tag>
    </template>
  </el-table-column>
  <el-table-column label="操作" width="120" align="center">
    <template #default="{ row }">
      <el-button link type="warning" @click="handleIgnoreShadow(row)">忽略</el-button>
    </template>
  </el-table-column>
</el-table>
<pagination
  v-show="shadowTotal > 0"
  v-model:total="shadowTotal"
  v-model:page="shadowFilters.pageNo"
  v-model:limit="shadowFilters.pageSize"
  :page-sizes="[10, 20, 50, 100]"
  @pagination="loadShadows"
/>
```

- [ ] **Step 5: Run frontend type check**

Run:

```bash
cd ytsh-ui-vue3 && pnpm ts:check
```

Expected: PASS. If existing unrelated TypeScript errors appear, record them and run a targeted `pnpm lint:eslint` only if the changed files are implicated.

- [ ] **Step 6: Commit frontend task**

```bash
git add ytsh-ui-vue3/src/api/ele/storeGoods/index.ts ytsh-ui-vue3/src/views/ele/store-goods/index.vue ytsh-ui-vue3/src/views/ele/store-goods/components/GovernancePoolPanel.vue
git commit -m "feat: show store goods shadow governance"
```

## Task 8: Full verification

**Files:**
- No new files.

- [ ] **Step 1: Run focused backend tests**

Run:

```bash
mvn -pl yudao-module-ele -Dtest=EleStoreGoodsShadowServiceImplTest,EleStoreGoodsSyncServiceImplTest test
mvn -pl yudao-module-business -Dtest=StoreProductServiceImplTest test
```

Expected: PASS.

- [ ] **Step 2: Compile runnable server**

Run:

```bash
mvn -pl yudao-server -am -DskipTests compile
```

Expected: PASS.

- [ ] **Step 3: Run frontend checks**

Run:

```bash
cd ytsh-ui-vue3 && pnpm ts:check
```

Expected: PASS.

- [ ] **Step 4: Manual UI verification**

Start frontend:

```bash
cd ytsh-ui-vue3 && pnpm dev
```

Verify in browser:

1. Open 饿了么门店商品同步 page.
2. Query and sync a page containing at least one SKU missing from local SKU master.
3. Confirm success message shows shadow count.
4. Open 待治理池 / 影子门店品 section.
5. Confirm the missing SKU appears as `UNMATCHED`.
6. Click 忽略 and confirm status changes or row disappears from active view.

- [ ] **Step 5: Review diff for unrelated changes**

Run:

```bash
git status --short
git diff --stat
```

Expected: only files listed in this plan are modified.

- [ ] **Step 6: Final commit if verification fixes were needed**

If verification required fixes, commit only those files:

```bash
git add docs/superpowers/plans/2026-04-27-ele-store-goods-shadow-sync.md
git commit -m "docs: refine store goods shadow sync plan"
```

## Self-Review Notes

- Spec coverage: schema, shadow statuses, sync write path, SKU-missing behavior, formal-list aggregation, governance API, frontend visibility, and verification are covered.
- Scope intentionally excludes automatic placeholder SKU creation and full master-data sync.
- The plan keeps `store_product_table.product_sku_id` required and uses shadow rows to protect formal business semantics.
