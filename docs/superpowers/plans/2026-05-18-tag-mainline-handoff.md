# 标签主线交接回迁 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在隔离 worktree 中把标签主线交接作业落地为一个干净、可验证的工作区，只包含交接文档白名单里的 43 个文件内容。

**Architecture:** 先创建一个新的 project-local worktree，避免碰脏的 `master` 工作区；再按交接文档的分组顺序审计并必要时从 `5048cd3` 精准恢复文件；最后跑标签主线后端定向测试和前端定向校验，确认结果可继续交付。

**Tech Stack:** git worktree, Git restore/diff, Maven, Vue 3, pnpm, ESLint

---

## File Map

### SQL
- Verify/Restore: `yudao-module-business/src/main/resources/sql/2026-05-13-product-tag-full-mount.sql`

### Backend Java
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/ProductSpuTagController.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/vo/ProductSpuTagBatchRespVO.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/vo/ProductSpuTagBatchSaveReqVO.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/StoreProductTagController.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductPageReqVO.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductRespVO.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagBatchRespVO.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagBatchSaveReqVO.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagRespVO.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagSaveReqVO.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagSimpleRespVO.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/dataobject/tag/TagObjectRelationDO.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/store/StoreProductMapper.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/tag/TagObjectRelationMapper.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/enums/tag/TagConstants.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagService.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagServiceImpl.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/SpuTableServiceImpl.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductServiceImpl.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagService.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagServiceImpl.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationService.java`
- Verify/Restore: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationServiceImpl.java`

### Frontend API 与页面
- Verify/Restore: `ytsh-ui-vue3/src/api/business/product/index.ts`
- Verify/Restore: `ytsh-ui-vue3/src/api/business/store-product/index.ts`
- Verify/Restore: `ytsh-ui-vue3/src/views/business/product/ProductSpuBatchTagForm.vue`
- Verify/Restore: `ytsh-ui-vue3/src/views/business/product/ProductSpuTagForm.vue`
- Verify/Restore: `ytsh-ui-vue3/src/views/business/product/index.vue`
- Verify/Restore: `ytsh-ui-vue3/src/views/business/product/productTagLogic.ts`
- Verify/Restore: `ytsh-ui-vue3/src/views/business/store-product/StoreProductBatchTagForm.vue`
- Verify/Restore: `ytsh-ui-vue3/src/views/business/store-product/StoreProductTagForm.vue`
- Verify/Restore: `ytsh-ui-vue3/src/views/business/store-product/index.vue`

### Tests 与测试 SQL
- Verify/Restore: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagServiceImplTest.java`
- Verify/Restore: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/product/SpuTableServiceImplTest.java`
- Verify/Restore: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/store/StoreProductServiceImplTest.java`
- Verify/Restore: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagServiceImplTest.java`
- Verify/Restore: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationServiceImplTest.java`
- Verify/Restore: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/tag/TagValueServiceImplTest.java`
- Verify/Restore: `yudao-module-business/src/test/resources/sql/clean.sql`
- Verify/Restore: `yudao-module-business/src/test/resources/sql/create_tables.sql`

### Context Docs
- Verify/Restore: `docs/superpowers/plans/2026-05-13-product-tag-full-mount.md`
- Verify/Restore: `docs/superpowers/specs/2026-05-13-product-tag-full-mount-design.md`

---

### Task 1: 创建隔离 worktree 并建立白名单基线

**Files:**
- Verify: `.worktrees/`
- Create: `.worktrees/tag-mainline-handoff/`
- Verify: `docs/superpowers/specs/2026-05-18-tag-mainline-handoff-design.md`
- Verify: `tag-mainline-handoff-2026-05-18.md`

- [ ] **Step 1: 创建新的 project-local worktree**

Run:
```bash
git worktree list --porcelain
```
Expected: 输出当前注册的 worktree，确认没有名为 `tag-mainline-handoff` 的有效工作区。

- [ ] **Step 2: 使用原生 worktree 工具进入新的隔离工作区**

Action:
```text
EnterWorktree(name="tag-mainline-handoff")
```
Expected: 会话切换到新的 `.worktrees/tag-mainline-handoff` 目录，且当前分支是新建分支，不影响脏的 `master` 工作区。

- [ ] **Step 3: 确认新 worktree 的 HEAD 就是标签主线基线**

Run:
```bash
git rev-parse HEAD && git log --oneline -n 1
```
Expected: HEAD 为 `5048cd3...`，最近一条提交是 `feat: complete product tag mainline flow`。

- [ ] **Step 4: 把交接文档里的 43 个文件写成检查清单，后续所有动作只围绕这份白名单**

Create file content:
```text
yudao-module-business/src/main/resources/sql/2026-05-13-product-tag-full-mount.sql
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/ProductSpuTagController.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/vo/ProductSpuTagBatchRespVO.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/vo/ProductSpuTagBatchSaveReqVO.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/StoreProductTagController.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductPageReqVO.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductRespVO.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagBatchRespVO.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagBatchSaveReqVO.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagRespVO.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagSaveReqVO.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagSimpleRespVO.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/dataobject/tag/TagObjectRelationDO.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/store/StoreProductMapper.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/tag/TagObjectRelationMapper.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/enums/tag/TagConstants.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagService.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagServiceImpl.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/SpuTableServiceImpl.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductServiceImpl.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagService.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagServiceImpl.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationService.java
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationServiceImpl.java
ytsh-ui-vue3/src/api/business/product/index.ts
ytsh-ui-vue3/src/api/business/store-product/index.ts
ytsh-ui-vue3/src/views/business/product/ProductSpuBatchTagForm.vue
ytsh-ui-vue3/src/views/business/product/ProductSpuTagForm.vue
ytsh-ui-vue3/src/views/business/product/index.vue
ytsh-ui-vue3/src/views/business/product/productTagLogic.ts
ytsh-ui-vue3/src/views/business/store-product/StoreProductBatchTagForm.vue
ytsh-ui-vue3/src/views/business/store-product/StoreProductTagForm.vue
ytsh-ui-vue3/src/views/business/store-product/index.vue
yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagServiceImplTest.java
yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/product/SpuTableServiceImplTest.java
yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/store/StoreProductServiceImplTest.java
yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagServiceImplTest.java
yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationServiceImplTest.java
yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/tag/TagValueServiceImplTest.java
yudao-module-business/src/test/resources/sql/clean.sql
yudao-module-business/src/test/resources/sql/create_tables.sql
docs/superpowers/plans/2026-05-13-product-tag-full-mount.md
docs/superpowers/specs/2026-05-13-product-tag-full-mount-design.md
```
Expected: 后续 diff、restore、验证都只允许针对这 43 行路径。

- [ ] **Step 5: 记录一个关键判断，避免无意义回迁**

Run:
```bash
git diff --name-only 5048cd3..HEAD -- $(cat .tag-mainline-whitelist.txt)
```
Expected: 大概率为空，因为当前仓库 HEAD 已经是 `5048cd3`；若为空，说明新 worktree 天然已经带着目标版本，只需要验证文件完整性，不需要逐个 restore。

---

### Task 2: 按 SQL → 后端 → 前端 → 测试/文档顺序审计并必要时恢复

**Files:**
- Verify/Restore: 上述 43 个白名单文件

- [ ] **Step 1: 先审计 SQL 组是否存在缺失或偏离**

Run:
```bash
while read -r f; do [ -e "$f" ] || echo "MISSING $f"; done <<'EOF'
yudao-module-business/src/main/resources/sql/2026-05-13-product-tag-full-mount.sql
EOF
```
Expected: 无 `MISSING` 输出。

- [ ] **Step 2: 如果 SQL 文件缺失或脏改，则仅恢复 SQL 组**

Run:
```bash
git diff -- yudao-module-business/src/main/resources/sql/2026-05-13-product-tag-full-mount.sql && \
git restore --source 5048cd3 -- yudao-module-business/src/main/resources/sql/2026-05-13-product-tag-full-mount.sql
```
Expected: 只有在该文件不等于 `5048cd3` 时才出现 diff；restore 后 `git diff -- <path>` 为空。

- [ ] **Step 3: 审计后端 Java 组，并只在发现缺失或偏离时恢复后端组**

Run:
```bash
git diff -- \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/ProductSpuTagController.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/vo/ProductSpuTagBatchRespVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/vo/ProductSpuTagBatchSaveReqVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/StoreProductTagController.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductPageReqVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductRespVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagBatchRespVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagBatchSaveReqVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagRespVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagSaveReqVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagSimpleRespVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/dataobject/tag/TagObjectRelationDO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/store/StoreProductMapper.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/tag/TagObjectRelationMapper.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/enums/tag/TagConstants.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagService.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagServiceImpl.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/SpuTableServiceImpl.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductServiceImpl.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagService.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagServiceImpl.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationService.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationServiceImpl.java
```
Expected: 如果为空，说明当前 clean worktree 的后端组已经和目标基线一致，无需 restore。

- [ ] **Step 4: 若后端组不一致，整组恢复后端白名单文件**

Run:
```bash
git restore --source 5048cd3 -- \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/ProductSpuTagController.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/vo/ProductSpuTagBatchRespVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/vo/ProductSpuTagBatchSaveReqVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/StoreProductTagController.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductPageReqVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductRespVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagBatchRespVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagBatchSaveReqVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagRespVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagSaveReqVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagSimpleRespVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/dataobject/tag/TagObjectRelationDO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/store/StoreProductMapper.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/tag/TagObjectRelationMapper.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/enums/tag/TagConstants.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagService.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagServiceImpl.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/SpuTableServiceImpl.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductServiceImpl.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagService.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagServiceImpl.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationService.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationServiceImpl.java
```
Expected: 恢复后 `git diff --` 对这一组为空。

- [ ] **Step 5: 以前端组重复同样的“先 diff 再 restore”流程**

Run:
```bash
git diff -- \
  ytsh-ui-vue3/src/api/business/product/index.ts \
  ytsh-ui-vue3/src/api/business/store-product/index.ts \
  ytsh-ui-vue3/src/views/business/product/ProductSpuBatchTagForm.vue \
  ytsh-ui-vue3/src/views/business/product/ProductSpuTagForm.vue \
  ytsh-ui-vue3/src/views/business/product/index.vue \
  ytsh-ui-vue3/src/views/business/product/productTagLogic.ts \
  ytsh-ui-vue3/src/views/business/store-product/StoreProductBatchTagForm.vue \
  ytsh-ui-vue3/src/views/business/store-product/StoreProductTagForm.vue \
  ytsh-ui-vue3/src/views/business/store-product/index.vue
```
Expected: 如果输出为空，说明前端组已与基线一致；否则再执行同组 `git restore --source 5048cd3 -- ...`。

- [ ] **Step 6: 以测试与文档组重复同样流程，并确认没有白名单外文件被碰到**

Run:
```bash
git diff -- \
  yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagServiceImplTest.java \
  yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/product/SpuTableServiceImplTest.java \
  yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/store/StoreProductServiceImplTest.java \
  yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagServiceImplTest.java \
  yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationServiceImplTest.java \
  yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/tag/TagValueServiceImplTest.java \
  yudao-module-business/src/test/resources/sql/clean.sql \
  yudao-module-business/src/test/resources/sql/create_tables.sql \
  docs/superpowers/plans/2026-05-13-product-tag-full-mount.md \
  docs/superpowers/specs/2026-05-13-product-tag-full-mount-design.md && \
  git status --short
```
Expected: 最终 `git status --short` 里只会出现白名单内路径，绝不出现 `.env`、`Login/**`、`script/**`、`yudao-module-ele/**` 等白名单外内容。

---

### Task 3: 运行后端最低验证

**Files:**
- Verify: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagServiceImplTest.java`
- Verify: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/product/SpuTableServiceImplTest.java`
- Verify: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/store/StoreProductServiceImplTest.java`
- Verify: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagServiceImplTest.java`
- Verify: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationServiceImplTest.java`
- Verify: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/tag/TagValueServiceImplTest.java`
- Verify: `yudao-module-business/src/test/resources/sql/clean.sql`
- Verify: `yudao-module-business/src/test/resources/sql/create_tables.sql`

- [ ] **Step 1: 先跑交接文档要求的最低后端验证**

Run:
```bash
mvn -pl yudao-module-business -Dtest=ProductSpuTagServiceImplTest,SpuTableServiceImplTest,StoreProductServiceImplTest,StoreProductTagServiceImplTest,TagValueServiceImplTest test
```
Expected: PASS。如果失败，输出里应该能定位到某个标签主线测试类或测试 SQL 断言。

- [ ] **Step 2: 补跑 `TagObjectRelationServiceImplTest`，确保关系层没有漏掉**

Run:
```bash
mvn -pl yudao-module-business -Dtest=TagObjectRelationServiceImplTest test
```
Expected: PASS。

- [ ] **Step 3: 如果任一测试失败，先保留失败输出，不做白名单外修复**

Capture summary format:
```text
FAILED_TEST=<class>#<method>
CAUSE=<first meaningful assertion or compile error line>
AFFECTED_FILE=<white-listed file path only>
NEXT_ACTION=仅在白名单内定位并修复；若需要白名单外改动则暂停请示用户
```
Expected: 失败时能把问题压缩到白名单范围内，而不是顺手扩散修复。

---

### Task 4: 运行前端最低验证

**Files:**
- Verify: `ytsh-ui-vue3/src/api/business/product/index.ts`
- Verify: `ytsh-ui-vue3/src/api/business/store-product/index.ts`
- Verify: `ytsh-ui-vue3/src/views/business/product/ProductSpuBatchTagForm.vue`
- Verify: `ytsh-ui-vue3/src/views/business/product/ProductSpuTagForm.vue`
- Verify: `ytsh-ui-vue3/src/views/business/product/index.vue`
- Verify: `ytsh-ui-vue3/src/views/business/product/productTagLogic.ts`
- Verify: `ytsh-ui-vue3/src/views/business/store-product/StoreProductBatchTagForm.vue`
- Verify: `ytsh-ui-vue3/src/views/business/store-product/StoreProductTagForm.vue`
- Verify: `ytsh-ui-vue3/src/views/business/store-product/index.vue`

- [ ] **Step 1: 先跑标签主线涉及文件的定向 ESLint**

Run:
```bash
pnpm --dir ytsh-ui-vue3 exec eslint \
  src/api/business/product/index.ts \
  src/api/business/store-product/index.ts \
  src/views/business/product/ProductSpuBatchTagForm.vue \
  src/views/business/product/ProductSpuTagForm.vue \
  src/views/business/product/index.vue \
  src/views/business/product/productTagLogic.ts \
  src/views/business/store-product/StoreProductBatchTagForm.vue \
  src/views/business/store-product/StoreProductTagForm.vue \
  src/views/business/store-product/index.vue
```
Expected: PASS。

- [ ] **Step 2: 如果当前环境允许，启动前端 dev server 做页面核对**

Run:
```bash
pnpm --dir ytsh-ui-vue3 dev
```
Expected: 本地开发服务器启动成功，默认端口为项目配置端口。

- [ ] **Step 3: 人工核对两个页面的四条主链路**

Checklist:
```text
/biz/spu-table
- 标签筛选可用
- 标签列可见
- 单个挂标可打开并保存
- 批量挂标可打开并保存

/biz/store-product
- 标签筛选可用
- 标签列可见
- 单个挂标可打开并保存
- 批量挂标可打开并保存
```
Expected: 至少能确认 UI 路由、组件渲染和主交互未断；若因环境阻塞无法完成，要明确记下阻塞点，不虚报“已验证”。

---

### Task 5: 收尾审计并输出交付结果

**Files:**
- Verify: `docs/superpowers/specs/2026-05-18-tag-mainline-handoff-design.md`
- Verify: `docs/superpowers/plans/2026-05-18-tag-mainline-handoff.md`

- [ ] **Step 1: 最后再核对白名单外路径完全没被碰过**

Run:
```bash
git status --short
```
Expected: 状态里只出现白名单内文件，绝不出现 `ytsh-ui-vue3/.env*`、`src/views/Login/**`、`script/**`、`.flattened-pom.xml`、`/.tag-branch-tscheck.txt` 等排除项。

- [ ] **Step 2: 汇总结果时必须给出证据，不做口头完成**

Summary format:
```text
1. worktree 路径
2. 是否需要实际 restore，还是 clean worktree 已天然等于 5048cd3
3. 实际变更文件列表
4. 后端测试结果
5. 前端 lint / 页面核对结果
6. 剩余风险或阻塞
```
Expected: 用户能直接据此判断“这次交接作业是否真的完成”。

- [ ] **Step 3: 不自动提交，等待用户明确要求后再处理 git commit**

Rule:
```text
即使执行完成，也只汇报结果；除非用户明确要求，否则不创建 commit、不 push、不清理 worktree。
```
Expected: 遵守当前仓库的 git 安全边界。

---

## Self-Review

- Spec coverage: 已覆盖隔离 worktree、白名单 43 文件、按 SQL/后端/前端/测试顺序处理、同名先 diff、后端最低验证、前端最低验证、白名单外边界控制。
- Placeholder scan: 无 `TODO/TBD/稍后再定/类似前一步` 之类占位语句；每一步都给了明确命令或明确输出格式。
- Type consistency: 本计划是操作型回迁计划，不引入新的类型或接口名；所有路径、提交号、验证命令都与交接文档和当前仓库状态一致。
- Key risk fixed in plan: 当前仓库 `HEAD` 就是 `5048cd3`，所以计划显式加入“先判断 clean worktree 是否天然等于目标基线”的步骤，避免无意义地逐文件 restore 一遍。