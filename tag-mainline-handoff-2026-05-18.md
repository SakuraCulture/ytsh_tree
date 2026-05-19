# 标签主线迁移交接文档

## 目的

这份文档只服务一件事：在你**完全删除当前目录并重新拉取仓库**之后，把“只属于标签开发主线”的内容手工迁回去。

本次标签主线的权威基线是：

- Git commit: `5048cd3`
- Commit message: `feat: complete product tag mainline flow`

当前 worktree 已基本干净，唯一未跟踪文件是：

- `/.tag-branch-tscheck.txt`

它只是临时产物，**不要迁移**。

---

## 迁移原则

1. **只迁本文列出的 43 个文件。**
2. **先迁 SQL，再迁后端，再迁前端，最后迁测试和文档。**
3. **不要顺手带上登录联调、环境配置、订单链路、脚本和临时文件。**
4. 如果新拉下来的仓库里已有同名文件新变更，先做 diff，再合并，不要盲覆盖。

---

## 这次标签主线实际包含什么

### 1. 数据库与标签关系能力

- 新增标签主线 SQL
- 扩展 `tag_object_relation`
- 让 SPU / STORE_PRODUCT 都能挂标签、查标签、按标签筛选

### 2. 后端接口与契约

- SPU 标签接口补齐批量挂标入口 `/save-manual-batch`
- STORE_PRODUCT 批量挂标契约统一为：
  - `successCount`
  - `failureCount`
  - `failureDetails`
- 两条批量链路都按“逐条处理 + 返回失败明细”收口

### 3. 前端页面能力

- SPU 列表页标签筛选、标签列、单个挂标、批量挂标
- 门店商品列表页标签筛选、标签列、单个挂标、批量挂标
- 前端 API 模块补齐对应调用

### 4. 回归测试

- 补齐 SPU / STORE_PRODUCT / Tag relation 相关定向单测
- 更新 H2 测试建表与清理 SQL

---

## 必迁文件清单

### A. 先迁 SQL

1. `yudao-module-business/src/main/resources/sql/2026-05-13-product-tag-full-mount.sql`

### B. 再迁后端 Java

2. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/ProductSpuTagController.java`
3. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/vo/ProductSpuTagBatchRespVO.java`
4. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/vo/ProductSpuTagBatchSaveReqVO.java`
5. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/StoreProductTagController.java`
6. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductPageReqVO.java`
7. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductRespVO.java`
8. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagBatchRespVO.java`
9. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagBatchSaveReqVO.java`
10. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagRespVO.java`
11. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagSaveReqVO.java`
12. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagSimpleRespVO.java`
13. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/dataobject/tag/TagObjectRelationDO.java`
14. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/store/StoreProductMapper.java`
15. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/tag/TagObjectRelationMapper.java`
16. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/enums/tag/TagConstants.java`
17. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagService.java`
18. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagServiceImpl.java`
19. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/SpuTableServiceImpl.java`
20. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductServiceImpl.java`
21. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagService.java`
22. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagServiceImpl.java`
23. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationService.java`
24. `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationServiceImpl.java`

### C. 再迁前端 API 与页面

25. `ytsh-ui-vue3/src/api/business/product/index.ts`
26. `ytsh-ui-vue3/src/api/business/store-product/index.ts`
27. `ytsh-ui-vue3/src/views/business/product/ProductSpuBatchTagForm.vue`
28. `ytsh-ui-vue3/src/views/business/product/ProductSpuTagForm.vue`
29. `ytsh-ui-vue3/src/views/business/product/index.vue`
30. `ytsh-ui-vue3/src/views/business/product/productTagLogic.ts`
31. `ytsh-ui-vue3/src/views/business/store-product/StoreProductBatchTagForm.vue`
32. `ytsh-ui-vue3/src/views/business/store-product/StoreProductTagForm.vue`
33. `ytsh-ui-vue3/src/views/business/store-product/index.vue`

### D. 最后迁测试与测试 SQL

34. `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagServiceImplTest.java`
35. `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/product/SpuTableServiceImplTest.java`
36. `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/store/StoreProductServiceImplTest.java`
37. `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagServiceImplTest.java`
38. `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationServiceImplTest.java`
39. `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/tag/TagValueServiceImplTest.java`
40. `yudao-module-business/src/test/resources/sql/clean.sql`
41. `yudao-module-business/src/test/resources/sql/create_tables.sql`

### E. 可一并保留的上下文文档

42. `docs/superpowers/plans/2026-05-13-product-tag-full-mount.md`
43. `docs/superpowers/specs/2026-05-13-product-tag-full-mount-design.md`

---

## 明确不要迁的内容

下面这些即使你在当前目录里看见改动，也**不属于这次标签主线交接范围**：

### 1. 登录联调相关

- `ytsh-ui-vue3/src/views/Login/**`
- `tenantWebsiteProbe.ts`
- 登录验证码弹窗关闭相关文件

### 2. 环境与敏感配置

- `yudao-server/src/main/resources/application*.yaml`
- `ytsh-ui-vue3/.env*`
- 任意本地账号、密钥、数据库连接、API key

### 3. 订单 / 饿了么 / 非标签业务线

- `yudao-module-ele/**` 中与标签主线无关的内容
- 订单页面、订单接口、库存查询、供货链路等

### 4. 脚本、部署与临时文件

- `script/**`
- `.flattened-pom.xml`
- `/.tag-branch-tscheck.txt`
- 任意本地日志、临时导出、测试缓存

### 5. 本次纯标签提交之外的说明文档

例如下面这类，不在 `5048cd3` 这次纯标签基线里，不要因为标题像标签就顺手带走：

- `docs/标签体系说明文档.md`

---

## 建议迁移顺序

### 方案 A：手工复制内容

1. 重新拉取仓库
2. 先落 SQL：`2026-05-13-product-tag-full-mount.sql`
3. 落后端 23 个 Java 文件
4. 落前端 9 个文件
5. 落 8 个测试相关文件
6. 最后把 plan / spec 两个文档带过去留档
7. 每一步都先 diff，再覆盖

### 方案 B：如果新仓库仍能访问旧提交

如果新仓库历史里还保留 `5048cd3`，最省事的不是手抄，而是直接按文件从这条提交取内容，再逐个核对落地。

---

## 迁移后最低验证

### 后端

```bash
mvn -pl yudao-module-business -Dtest=ProductSpuTagServiceImplTest,SpuTableServiceImplTest,StoreProductServiceImplTest,StoreProductTagServiceImplTest,TagValueServiceImplTest test
```

### 前端

至少重新检查：

- `/biz/spu-table` 的标签筛选、标签列、单个挂标、批量挂标
- `/biz/store-product` 的标签筛选、标签列、单个挂标、批量挂标

如果只做静态校验，优先跑标签主线涉及文件的定向 eslint。

---

## 一句话边界

**删库重拉之后，只迁这 43 个文件；凡是登录、环境、订单、脚本、临时文件，一律不要混进来。**
