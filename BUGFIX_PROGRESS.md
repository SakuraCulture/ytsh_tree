# Business 前端首批 P1 Bugfix 进度存档

> 存档时间：2026-05-20

## 作业目标

修复 business 前端首批两项 P1 bug：
1. 类目新增时父类目联动错误
2. 门店商品归属值域不一致

---

## 修复内容

### Bug 1: 类目父类目联动

**问题描述：**
- `CategoryTableForm.vue` 在新增场景选择父类目时，`categoryLevel/categoryPath/categoryPathNames` 未正确联动
- 根因：`el-tree-select` 回传字符串值，而 `handleParentChange` 只处理 `number` 类型

**修复方案：**
- 新增 `categoryFormLogic.ts`，提供 `normalizeCategoryParentId` 归一化函数
- `deriveCategoryFieldsByParent` 兼容 `string | number | undefined | null`
- `CategoryTableForm.vue` 新增 `watch` 监听 `formData.parentId`，不再只依赖 `@change`

**修改文件：**
- `ytsh-ui-vue3/src/views/business/category/categoryFormLogic.ts` (新增)
- `ytsh-ui-vue3/src/views/business/category/categoryFormLogic.test.ts` (新增)
- `ytsh-ui-vue3/src/views/business/category/CategoryTableForm.vue` (修改)

---

### Bug 2: 门店商品归属值域

**问题描述：**
- `index.vue` 与 `StoreProductForm.vue` 把 ownership 硬编码为 `HQ/STORE`
- 后端契约使用原始值域（如"入店"、"联营"等），导致筛选/展示/保存不一致

**修复方案：**
- 新增 `storeProductOwnershipLogic.ts`，统一处理 ownership 的展示、回填、保存
- 组件改用共享逻辑，不再硬编码协议值

**修改文件：**
- `ytsh-ui-vue3/src/views/business/store-product/storeProductOwnershipLogic.ts` (新增)
- `ytsh-ui-vue3/src/views/business/store-product/storeProductOwnershipLogic.test.ts` (新增)
- `ytsh-ui-vue3/src/views/business/store-product/index.vue` (修改)
- `ytsh-ui-vue3/src/views/business/store-product/StoreProductForm.vue` (修改)

---

## 完成进度

| 阶段 | 状态 | 说明 |
|------|------|------|
| 代码修复 | ✅ 完成 | 两处 bug 的最小修复已落地 |
| 单元测试 | ✅ 通过 | `node:test` 两组测试全部通过 |
| 类型检查 | ✅ 通过 | 本次相关文件无新增 TS 错误 |
| 真实 UI 验证 | ⏳ 阻塞 | 后端认证链路异常，待恢复后继续 |

---

## 验证步骤（待执行）

### 类目父类目联动
1. 进入「业务管理 → 类目管理」
2. 点击「新增」打开弹窗
3. 选择一个非顶级的父类目
4. 确认「层级」和「类目路径」自动派生正确

### 门店商品归属值域
1. 进入「业务管理 → 门店商品管理」
2. 列表「商品归属」列显示原始值
3. 点击「编辑」，确认归属正确回填
4. 修改后保存，确认值不变

---

## 阻塞原因

真实浏览器验证时，后端接口返回 500/404：
- `/system/auth/get-permission-info` 返回 500
- `/business/category-table/list` 返回 500
- `/store-product/page` 返回 500

待后端认证/业务接口恢复后，继续执行真实 UI 回归。

---

## 后续事项

1. 后端恢复后，执行真实 UI 验证
2. 验证通过后，提交代码并创建 PR
3. 其他 P2/P3 问题按优先级后续处理
