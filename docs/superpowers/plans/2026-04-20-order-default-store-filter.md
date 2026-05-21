# 订单页默认门店筛选 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让订单页首次进入与点击重置时，默认只查询第一家开业门店的当日订单，避免自动触发全门店当日大查询。

**Architecture:** 保持现有订单页主体结构不变，只把“默认门店选择”和“是否允许自动查询”抽成一个小型纯函数模块，供页面初始化和重置逻辑复用。页面仍然沿用现有 `today` 日期快捷筛选与 `getOrderPage` 请求，只在“自动场景”下阻止没有开业门店时的全量查询。

**Tech Stack:** Vue 3 Options API、Vite、Element Plus、Node.js 内置 `node:test`、pnpm

---

## File Structure

- Create: `ytsh-ui-vue3/src/views/order/defaultStoreState.mjs`
  - 负责从门店列表中计算默认开业门店 `platformStoreId`
  - 负责告诉页面“当前是否允许自动触发首屏/重置查询”
- Create: `ytsh-ui-vue3/src/views/order/defaultStoreState.test.mjs`
  - 用 Node 内置测试验证默认门店选择规则，避免把判断逻辑埋在 `.vue` 文件里无法单测
- Modify: `ytsh-ui-vue3/src/views/order/index.vue:477-580`
  - 引入纯函数模块，新增默认查询参数构造方法
- Modify: `ytsh-ui-vue3/src/views/order/index.vue:590-609`
  - 在门店列表加载完成后应用默认门店，并仅在允许时触发自动查询
- Modify: `ytsh-ui-vue3/src/views/order/index.vue:869-942`
  - 保持 `getList()` 只负责“按当前筛选条件查询”，不在这里偷偷补默认门店
- Modify: `ytsh-ui-vue3/src/views/order/index.vue:1258-1282`
  - 重置时恢复“今日 + 第一家开业门店”，并在无开业门店时停止自动查询

---

### Task 1: 提取默认门店状态纯函数并先写失败测试

**Files:**
- Create: `ytsh-ui-vue3/src/views/order/defaultStoreState.mjs`
- Test: `ytsh-ui-vue3/src/views/order/defaultStoreState.test.mjs`

- [ ] **Step 1: 写失败测试，固定默认门店规则**

```js
import test from 'node:test'
import assert from 'node:assert/strict'
import { resolveDefaultStoreState } from './defaultStoreState.mjs'

test('picks the first open store and enables auto query', () => {
  const result = resolveDefaultStoreState([
    { platformStoreId: 'closed-1', storeStatus: 0 },
    { platformStoreId: 'open-1', storeStatus: 1 },
    { platformStoreId: 'open-2', storeStatus: 1 }
  ])

  assert.deepEqual(result, {
    defaultStoreId: 'open-1',
    shouldAutoQuery: true
  })
})

test('skips stores without platformStoreId', () => {
  const result = resolveDefaultStoreState([
    { platformStoreId: '', storeStatus: 1 },
    { platformStoreId: 'open-2', storeStatus: 1 }
  ])

  assert.deepEqual(result, {
    defaultStoreId: 'open-2',
    shouldAutoQuery: true
  })
})

test('returns null and disables auto query when there is no open store', () => {
  const result = resolveDefaultStoreState([
    { platformStoreId: 'closed-1', storeStatus: 0 },
    { platformStoreId: 'closed-2', storeStatus: 0 }
  ])

  assert.deepEqual(result, {
    defaultStoreId: null,
    shouldAutoQuery: false
  })
})
```

- [ ] **Step 2: 运行测试，确认当前确实失败**

Run:
```bash
node --test "ytsh-ui-vue3/src/views/order/defaultStoreState.test.mjs"
```

Expected:
```text
not ok 1 - picks the first open store and enables auto query
...
Error [ERR_MODULE_NOT_FOUND]: Cannot find module '.../defaultStoreState.mjs'
```

- [ ] **Step 3: 写最小实现，让测试描述的规则成立**

```js
export const OPEN_STORE_STATUS = 1

export function resolveDefaultStoreState(storeList = []) {
  const defaultStore = storeList.find(
    (store) => Number(store?.storeStatus) === OPEN_STORE_STATUS && store?.platformStoreId
  )

  return {
    defaultStoreId: defaultStore?.platformStoreId ?? null,
    shouldAutoQuery: Boolean(defaultStore?.platformStoreId)
  }
}
```

- [ ] **Step 4: 再次运行测试，确认纯函数通过**

Run:
```bash
node --test "ytsh-ui-vue3/src/views/order/defaultStoreState.test.mjs"
```

Expected:
```text
# tests 3
# pass 3
# fail 0
```

---

### Task 2: 把默认门店逻辑接回订单页初始化与重置流程

**Files:**
- Modify: `ytsh-ui-vue3/src/views/order/index.vue:477-580,590-609,1258-1282`
- Create: `ytsh-ui-vue3/src/views/order/defaultStoreState.mjs`

- [ ] **Step 1: 在页面中引入纯函数，并补一个默认查询参数构造方法**

在 `ytsh-ui-vue3/src/views/order/index.vue` 的顶部 import 与 methods 中加入下面代码。

```js
import { resolveDefaultStoreState } from './defaultStoreState.mjs'
```

```js
buildDefaultQueryParams(defaultStoreId = null) {
  return {
    pageNum: 1,
    pageSize: 20,
    orderNo: null,
    storeId: defaultStoreId,
    isException: null,
    receiverName: null,
    phoneSuffix: null,
    channelOrderNo: null,
    orderSort: 'createTime_desc',
    goodsId: null,
    goodsName: null,
    deliveryType: null,
    channelType: null,
    orderType: null,
    address: null,
    status: null,
    beginTime: null,
    endTime: null
  }
}
```

- [ ] **Step 2: 修改门店加载完成逻辑，只在存在开业门店时自动查询**

把 `loadStoreList()` 中成功分支替换为下面逻辑。

```js
loadStoreList() {
  this.storeLoading = true
  TableApi.getTableAllSimpleList(1)
    .then((res) => {
      const list = Array.isArray(res) ? res : []
      this.storeList = list.sort((a, b) => {
        const aStatus = a.storeStatus ?? 0
        const bStatus = b.storeStatus ?? 0
        return aStatus - bStatus
      })

      const { defaultStoreId, shouldAutoQuery } = resolveDefaultStoreState(this.storeList)
      this.queryParams.storeId = defaultStoreId

      if (shouldAutoQuery) {
        this.getList()
        return
      }

      this.clearListState()
      this.loading = false
    })
    .catch(() => {
      this.storeList = []
      this.loading = false
    })
    .finally(() => {
      this.storeLoading = false
    })
}
```

- [ ] **Step 3: 修改重置逻辑，恢复“今日 + 第一家开业门店”**

把 `resetQuery()` 改成下面实现。

```js
resetQuery() {
  const { defaultStoreId, shouldAutoQuery } = resolveDefaultStoreState(this.storeList)

  this.dateType = 'today'
  this.queryParams = this.buildDefaultQueryParams(defaultStoreId)
  this.resetForm('queryForm')

  if (shouldAutoQuery) {
    this.handleQuery()
    return
  }

  this.clearListState()
}
```

- [ ] **Step 4: 做一次类型与构建侧校验，确认页面代码可编译**

Run:
```bash
pnpm --dir "ytsh-ui-vue3" ts:check
```

Expected:
```text
Exit code 0
```

---

### Task 3: 验证自动场景不触发大查询，手动场景保持原语义

**Files:**
- Modify: `ytsh-ui-vue3/src/views/order/index.vue:876-942`
- Test: `ytsh-ui-vue3/src/views/order/defaultStoreState.test.mjs`

- [ ] **Step 1: 保持 `getList()` 的请求语义不变，只让自动入口决定是否调用它**

此任务不重写 `getList()` 主体，只确认下面两点仍然成立：

```js
const requestParams = {
  pageNo: 1,
  pageSize: 1000,
  startTime,
  endTime
}
if (this.queryParams.storeId) {
  requestParams.platformStoreId = this.queryParams.storeId
}
```

```js
handleQuery() {
  this.queryParams.pageNum = 1
  this.getList()
}
```

这保证：
- 首屏/重置时是否自动查询，由 `loadStoreList()` / `resetQuery()` 决定
- 用户手动清空门店后，如果点击“搜索”，仍然保留当前“可查全部门店”的原语义

- [ ] **Step 2: 再跑一次纯函数测试，确认门店选择规则没有被回归修改**

Run:
```bash
node --test "ytsh-ui-vue3/src/views/order/defaultStoreState.test.mjs"
```

Expected:
```text
# tests 3
# pass 3
# fail 0
```

- [ ] **Step 3: 启动前端并手工验证 4 个关键场景**

Run:
```bash
pnpm --dir "ytsh-ui-vue3" dev
```

在浏览器中验证：

1. 首次打开订单页时，门店下拉已选中第一家开业门店，且请求参数包含该 `platformStoreId`
2. 点击“重置”后，门店恢复为第一家开业门店，日期恢复为“今日”
3. 手动切换到其他门店后点击“搜索”，请求参数跟随用户选择变化
4. 手动清空门店后点击“搜索”，页面不偷偷回填默认门店

Expected:
```text
首屏与重置只查默认开业门店；手动清空门店后仍按用户当前选择发请求
```

- [ ] **Step 4: 运行一次最终静态校验，确认改动没有引入前端类型错误**

Run:
```bash
pnpm --dir "ytsh-ui-vue3" ts:check
```

Expected:
```text
Exit code 0
```

## Self-Review

- **Spec coverage:**
  - “首次进入默认查第一家开业门店 + 今日” → Task 1、Task 2
  - “重置后恢复同样默认条件” → Task 2
  - “不影响用户手动切换/清空后的主动查询” → Task 3
  - “无开业门店时不触发自动全门店大查询” → Task 1、Task 2
- **Placeholder scan:** 已检查，没有 `TODO`、`TBD`、`适当处理` 这类占位描述
- **Type consistency:** 计划中统一使用 `resolveDefaultStoreState`、`defaultStoreId`、`shouldAutoQuery` 三个命名，和页面修改步骤保持一致
