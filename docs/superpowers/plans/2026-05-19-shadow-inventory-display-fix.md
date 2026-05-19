# 影子商品库存展示修复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复影子商品展开时看不到库存，以及刷新库存时上游已返回目标 SKU 却被前端误判为“未查询到结果”的问题。

**Architecture:** 后端先把影子商品详情接口补齐为“商品影子主记录 + 影子库存快照”的组合响应，这样展开卡片默认就有快照可显示。前端保持正式商品逻辑不变，只把影子库存刷新匹配从“必须精确命中 skuCode + subSkuCode”调整为“先精确命中，失败后仅在同 SKU 唯一时降级命中”，避免把已有返回误判为无结果。

**Tech Stack:** Vue 3, TypeScript, Node test runner, Java 17, Spring Boot 3, MyBatis Plus.

---

## File Structure

- Modify: `ytsh-ui-vue3/src/views/ele/storeInventoryPageLogic.ts`
- Modify: `ytsh-ui-vue3/src/views/ele/storeInventoryPageLogic.test.ts`
- Modify: `ytsh-ui-vue3/src/views/business/store-product/storeProductShadowInventoryRuntimePolicy.test.ts`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreGoodsShadowRespVO.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryShadowService.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryShadowServiceImpl.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowServiceImpl.java`

## Task 1: 先写失败测试锁定两个根因

**Files:**
- Modify: `ytsh-ui-vue3/src/views/ele/storeInventoryPageLogic.test.ts`
- Modify: `ytsh-ui-vue3/src/views/business/store-product/storeProductShadowInventoryRuntimePolicy.test.ts`

- [ ] **Step 1: 为刷新误判补失败测试**

```ts
test('findMatchingInventoryRow falls back to unique sku match when exact subSku is missing', () => {
  const row = findMatchingInventoryRow(
    [{ skuCode: 'SKU-1', subSkuCode: '', physicalStockTotalAmount: 9 }],
    { skuCode: 'SKU-1', subSkuCode: 'SUB-1' }
  )

  assert.equal(row?.physicalStockTotalAmount, 9)
})
```

- [ ] **Step 2: 运行单测确认红灯**

Run: `node --test "C:/Users/ytsh01/Desktop/ant_dev/.claude/worktrees/shadow-stock-fix/ytsh-ui-vue3/src/views/ele/storeInventoryPageLogic.test.ts"`
Expected: FAIL at the new fallback-match case.

- [ ] **Step 3: 为默认快照缺失补失败测试**

```ts
test('影子商品详情响应包含库存快照字段', () => {
  const backendVo = readFile(
    '../yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreGoodsShadowRespVO.java'
  )

  assert.match(backendVo, /private Integer physicalStockTotalAmount;/)
  assert.match(backendVo, /private Integer availableForSale;/)
  assert.match(backendVo, /private LocalDateTime lastQueryTime;/)
})
```

- [ ] **Step 4: 运行策略测试确认红灯**

Run: `node --test "C:/Users/ytsh01/Desktop/ant_dev/.claude/worktrees/shadow-stock-fix/ytsh-ui-vue3/src/views/business/store-product/storeProductShadowInventoryRuntimePolicy.test.ts"`
Expected: FAIL because the backend response VO does not expose snapshot stock fields yet.

## Task 2: 补齐影子商品详情的库存快照

**Files:**
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreGoodsShadowRespVO.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryShadowService.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryShadowServiceImpl.java`
- Modify: `yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowServiceImpl.java`

- [ ] **Step 1: 给影子详情 VO 增加库存快照字段**

```java
private Integer physicalStockTotalAmount;
private Integer availableForSale;
private Integer physicalStockAvailableAmount;
private Integer physicalStockIntransitAmount;
private Integer reservedAmount;
private Integer physicalStockOccupiedAmount;
private LocalDateTime lastQueryTime;
private String reasonMsg;
```

- [ ] **Step 2: 给库存影子服务暴露按业务键查询能力**

```java
EleStoreInventoryShadowDO getByBizKey(Long platformId, String merchantCode,
        String erpStoreCode, String skuCode, String subSkuCode);
```

```java
@Override
public EleStoreInventoryShadowDO getByBizKey(Long platformId, String merchantCode,
        String erpStoreCode, String skuCode, String subSkuCode) {
    return selectByAnyBizKey(platformId, merchantCode, erpStoreCode, skuCode, subSkuCode);
}
```

- [ ] **Step 3: 在影子商品详情查询里合并库存快照**

```java
public EleStoreGoodsShadowRespVO getShadow(Long id) {
    EleStoreGoodsShadowDO shadow = getRequiredShadow(id);
    EleStoreGoodsShadowRespVO respVO = BeanUtils.toBean(shadow, EleStoreGoodsShadowRespVO.class);
    EleStoreInventoryShadowDO inventoryShadow = eleSkuInventoryShadowService.getByBizKey(
            shadow.getPlatformId(), shadow.getMerchantCode(), shadow.getErpStoreCode(),
            shadow.getSkuCode(), shadow.getSubSkuCode());
    if (inventoryShadow != null) {
        respVO.setPhysicalStockTotalAmount(inventoryShadow.getPhysicalStockTotalAmount());
        respVO.setAvailableForSale(inventoryShadow.getAvailableForSale());
        respVO.setPhysicalStockAvailableAmount(inventoryShadow.getPhysicalStockAvailableAmount());
        respVO.setPhysicalStockIntransitAmount(inventoryShadow.getPhysicalStockIntransitAmount());
        respVO.setReservedAmount(inventoryShadow.getReservedAmount());
        respVO.setPhysicalStockOccupiedAmount(inventoryShadow.getPhysicalStockOccupiedAmount());
        respVO.setLastQueryTime(inventoryShadow.getLastQueryTime());
        respVO.setReasonMsg(inventoryShadow.getReasonMsg());
    }
    return respVO;
}
```

- [ ] **Step 4: 运行策略测试确认默认快照链路转绿**

Run: `node --test "C:/Users/ytsh01/Desktop/ant_dev/.claude/worktrees/shadow-stock-fix/ytsh-ui-vue3/src/views/business/store-product/storeProductShadowInventoryRuntimePolicy.test.ts"`
Expected: PASS.

## Task 3: 放宽刷新库存的前端匹配，但只在唯一 SKU 时降级

**Files:**
- Modify: `ytsh-ui-vue3/src/views/ele/storeInventoryPageLogic.ts`
- Modify: `ytsh-ui-vue3/src/views/ele/storeInventoryPageLogic.test.ts`

- [ ] **Step 1: 先保持“精确命中优先”不变**

```ts
const exactMatch = rows.find((row) => {
  return (
    normalizeInventoryKey(row.skuCode) === targetSkuCode &&
    normalizeInventoryKey(row.subSkuCode) === targetSubSkuCode
  )
})
if (exactMatch) {
  return exactMatch
}
```

- [ ] **Step 2: 只在同 SKU 返回唯一一行时降级命中**

```ts
if (!targetSkuCode) {
  return undefined
}

const sameSkuRows = rows.filter((row) => normalizeInventoryKey(row.skuCode) === targetSkuCode)
if (sameSkuRows.length === 1) {
  return sameSkuRows[0]
}

return undefined
```

- [ ] **Step 3: 运行逻辑单测确认转绿**

Run: `node --test "C:/Users/ytsh01/Desktop/ant_dev/.claude/worktrees/shadow-stock-fix/ytsh-ui-vue3/src/views/ele/storeInventoryPageLogic.test.ts"`
Expected: PASS with the new fallback-match case.

## Task 4: 回归验证正式商品与影子商品两条分支

**Files:**
- Modify: none expected

- [ ] **Step 1: 运行影子库存相关两组 Node 测试**

Run:
```bash
node --test "C:/Users/ytsh01/Desktop/ant_dev/.claude/worktrees/shadow-stock-fix/ytsh-ui-vue3/src/views/business/store-product/storeProductShadowInventoryRuntimePolicy.test.ts" && \
node --test "C:/Users/ytsh01/Desktop/ant_dev/.claude/worktrees/shadow-stock-fix/ytsh-ui-vue3/src/views/ele/storeInventoryPageLogic.test.ts"
```
Expected: both PASS.

- [ ] **Step 2: 运行变更面 lint 校验**

Run: `pnpm --dir "C:/Users/ytsh01/Desktop/ant_dev/.claude/worktrees/shadow-stock-fix/ytsh-ui-vue3" exec eslint "src/views/business/store-product/components/ShadowInventoryExpandCard.vue" "src/views/business/store-product/index.vue" "src/views/ele/storeInventoryPageLogic.ts"`
Expected: no output.

- [ ] **Step 3: 浏览器确认两条用户路径**

Check:
- 影子商品首次展开时直接显示库存快照，不再空白
- 点击“刷新库存”时，只要上游返回唯一同 SKU 结果，就更新卡片而不是弹“本次未查询到库存结果”
- 正式商品展开仍显示 `StockExpandCard`，不受影子逻辑影响

- [ ] **Step 4: Commit**

```bash
git add \
  ytsh-ui-vue3/src/views/ele/storeInventoryPageLogic.ts \
  ytsh-ui-vue3/src/views/ele/storeInventoryPageLogic.test.ts \
  ytsh-ui-vue3/src/views/business/store-product/storeProductShadowInventoryRuntimePolicy.test.ts \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/controller/admin/vo/EleStoreGoodsShadowRespVO.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryShadowService.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleSkuInventoryShadowServiceImpl.java \
  yudao-module-ele/src/main/java/cn/iocoder/yudao/module/ele/service/EleStoreGoodsShadowServiceImpl.java \
  docs/superpowers/plans/2026-05-19-shadow-inventory-display-fix.md

git commit -m "fix: restore shadow inventory display"
```

---

Plan complete and saved to `docs/superpowers/plans/2026-05-19-shadow-inventory-display-fix.md`. You already明确说了“继续”，我将按 Inline Execution 直接执行；如果你想改成 Subagent-Driven，我可以切换。