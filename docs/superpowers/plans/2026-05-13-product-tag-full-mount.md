# 商品标签体系全量挂载 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让商品主档（SPU）和门店商品（STORE_PRODUCT）共用现有 `PRODUCT` 域标签池，并都具备查询、保存、批量挂标、列表回显与按标签筛选能力。

**Architecture:** 先把 `tag_object_relation` 的 `objectId` 语义从 `Long` 升级为 `String`，彻底打通 `STORE_PRODUCT` 的字符串主键，再在统一关系层之上补齐 SPU 批量挂标入口和门店商品标签入口。前端继续沿用现有 SPU 标签交互模式，抽共享选择逻辑，分别接到 SPU 列表和门店商品列表。

**Tech Stack:** Spring Boot, MyBatis-Plus, H2/BaseDbUnitTest, Vue 3, Element Plus, TypeScript

---

## File Map

### Backend
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/enums/tag/TagConstants.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/dataobject/tag/TagObjectRelationDO.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/tag/TagObjectRelationMapper.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationService.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationServiceImpl.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/tag/TagValueServiceImpl.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagService.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagServiceImpl.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/ProductSpuTagController.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/vo/SpuTablePageReqVO.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/SpuTableServiceImpl.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductPageReqVO.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/store/StoreProductMapper.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductServiceImpl.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/StoreProductController.java`
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/StoreProductTagController.java`
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagSaveReqVO.java`
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagBatchSaveReqVO.java`
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagRespVO.java`
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagSimpleRespVO.java`
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagBatchRespVO.java`
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagService.java`
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagServiceImpl.java`
- Create: `yudao-module-business/src/main/resources/sql/2026-05-13-product-tag-full-mount.sql`

### Backend tests
- Create: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationServiceImplTest.java`
- Create: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagServiceImplTest.java`
- Create: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagServiceImplTest.java`
- Create: `yudao-module-business/src/test/resources/sql/create_tables.sql`

### Frontend
- Modify: `ytsh-ui-vue3/src/api/business/product/index.ts`
- Modify: `ytsh-ui-vue3/src/api/business/store-product/index.ts`
- Modify: `ytsh-ui-vue3/src/views/business/product/index.vue`
- Modify: `ytsh-ui-vue3/src/views/business/product/ProductSpuTagForm.vue`
- Modify: `ytsh-ui-vue3/src/views/business/product/productTagLogic.ts`
- Modify: `ytsh-ui-vue3/src/views/business/store-product/index.vue`
- Create: `ytsh-ui-vue3/src/views/business/product/ProductSpuBatchTagForm.vue`
- Create: `ytsh-ui-vue3/src/views/business/store-product/StoreProductTagForm.vue`
- Create: `ytsh-ui-vue3/src/views/business/store-product/StoreProductBatchTagForm.vue`

---

### Task 1: 先把统一关系层改成可承载字符串对象 ID

**Files:**
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/enums/tag/TagConstants.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/dataobject/tag/TagObjectRelationDO.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/tag/TagObjectRelationMapper.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationService.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationServiceImpl.java`
- Create: `yudao-module-business/src/main/resources/sql/2026-05-13-product-tag-full-mount.sql`
- Test: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationServiceImplTest.java`

- [ ] **Step 1: 先写失败用例，卡住字符串对象 ID 与 STORE_PRODUCT 扩展点**

```java
@Import(TagObjectRelationServiceImpl.class)
class TagObjectRelationServiceImplTest extends BaseDbUnitTest {

    @Resource
    private TagObjectRelationServiceImpl service;
    @Resource
    private TagObjectRelationMapper mapper;

    @Test
    void saveManualRelations_supportsStoreProductStringObjectId() {
        service.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_STORE_PRODUCT, "SP-001", List.of(11L, 12L));

        List<TagObjectRelationDO> rows = mapper.selectActiveList(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_STORE_PRODUCT, "SP-001");
        assertEquals(2, rows.size());
    }
}
```

- [ ] **Step 2: 运行定向测试，确认当前实现会因 Long 签名失败**

Run: `mvn -pl yudao-module-business -Dtest=TagObjectRelationServiceImplTest test`
Expected: 编译失败，报 `String cannot be converted to Long` 或 mapper/service 签名不匹配。

- [ ] **Step 3: 把关系层对象 ID 全部切到 String，并放开 STORE_PRODUCT 常量**

```java
public interface TagConstants {
    String DOMAIN_TYPE_PRODUCT = "PRODUCT";
    String OBJECT_TYPE_SPU = "SPU";
    String OBJECT_TYPE_STORE_PRODUCT = "STORE_PRODUCT";

    Set<String> OBJECT_TYPES = Set.of(OBJECT_TYPE_SPU, OBJECT_TYPE_STORE_PRODUCT);
}
```

```java
@TableName("tag_object_relation")
public class TagObjectRelationDO extends BaseDO {
    private String objectId;
}
```

```java
public interface TagObjectRelationService {
    void saveManualRelations(String domainType, String objectType, String objectId, List<Long> tagValueIds);
    void saveRuleRelations(String domainType, String objectType, String objectId, String sourceRef, List<Long> tagValueIds);
    List<TagObjectRelationDO> getActiveRelations(String domainType, String objectType, String objectId);
    List<TagObjectRelationDO> getActiveRelationsByObjectIds(String domainType, String objectType, Collection<String> objectIds);
}
```

```java
private void validateScope(String domainType, String objectType, String objectId) {
    validateDomainType(domainType);
    validateObjectType(objectType);
    if (StrUtil.isBlank(objectId)) {
        throw exception(TAG_OBJECT_TYPE_INVALID);
    }
}
```

```sql
ALTER TABLE tag_object_relation
    MODIFY COLUMN object_id varchar(64) NOT NULL COMMENT '打标对象 ID';
```

- [ ] **Step 4: 更新 mapper 查询签名，保证按字符串对象 ID 查询、批量查、反查都可用**

```java
default List<TagObjectRelationDO> selectActiveList(String domainType, String objectType, String objectId) {
    return selectList(new LambdaQueryWrapperX<TagObjectRelationDO>()
            .eq(TagObjectRelationDO::getDomainType, domainType)
            .eq(TagObjectRelationDO::getObjectType, objectType)
            .eq(TagObjectRelationDO::getObjectId, objectId)
            .eq(TagObjectRelationDO::getStatus, RELATION_STATUS_ENABLED)
            .orderByAsc(TagObjectRelationDO::getId));
}

default List<TagObjectRelationDO> selectActiveListByObjectIds(String domainType, String objectType, Collection<String> objectIds) {
    return selectList(new LambdaQueryWrapperX<TagObjectRelationDO>()
            .eq(TagObjectRelationDO::getDomainType, domainType)
            .eq(TagObjectRelationDO::getObjectType, objectType)
            .inIfPresent(TagObjectRelationDO::getObjectId, objectIds)
            .eq(TagObjectRelationDO::getStatus, RELATION_STATUS_ENABLED));
}
```

- [ ] **Step 5: 让测试通过，并补覆盖式保存回归断言**

```java
@Test
void saveManualRelations_disablesRemovedManualRows() {
    service.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_STORE_PRODUCT, "SP-001", List.of(11L, 12L));
    service.saveManualRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_STORE_PRODUCT, "SP-001", List.of(12L));

    List<TagObjectRelationDO> rows = mapper.selectByObjectIdIgnoreDeleted(OBJECT_TYPE_STORE_PRODUCT, "SP-001");
    assertEquals(2, rows.size());
    assertEquals(RELATION_STATUS_DISABLED, rows.get(0).getStatus());
    assertEquals(RELATION_STATUS_ENABLED, rows.get(1).getStatus());
}
```

Run: `mvn -pl yudao-module-business -Dtest=TagObjectRelationServiceImplTest test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/enums/tag/TagConstants.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/dataobject/tag/TagObjectRelationDO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/tag/TagObjectRelationMapper.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationService.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationServiceImpl.java \
  yudao-module-business/src/main/resources/sql/2026-05-13-product-tag-full-mount.sql \
  yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationServiceImplTest.java \
  yudao-module-business/src/test/resources/sql/create_tables.sql

git commit -m "feat: allow product tags on string-backed objects"
```

---

### Task 2: 补齐 SPU 标签批量挂标，并把所有 SPU 路径切到字符串关系 ID

**Files:**
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagService.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagServiceImpl.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/ProductSpuTagController.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/SpuTableServiceImpl.java`
- Test: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagServiceImplTest.java`

- [ ] **Step 1: 先写失败测试，覆盖 SPU 批量挂标与 simple-list 仍可回显**

```java
@Test
void saveManualTagsBatch_success() {
    ProductSpuTagBatchSaveReqVO reqVO = new ProductSpuTagBatchSaveReqVO();
    reqVO.setProductSpuIds(List.of(101L, 102L));
    reqVO.setTagValueIds(List.of(11L, 12L));

    ProductTagBatchRespVO respVO = service.saveManualTagsBatch(reqVO);

    assertEquals(2, respVO.getSuccessCount());
    assertTrue(respVO.getFailureDetails().isEmpty());
}
```

- [ ] **Step 2: 运行测试，确认批量入口尚未实现**

Run: `mvn -pl yudao-module-business -Dtest=ProductSpuTagServiceImplTest test`
Expected: FAIL，缺少 `saveManualTagsBatch` 或返回类型。

- [ ] **Step 3: 新增批量请求/响应，并让 SPU 服务统一走字符串 objectId**

```java
public interface ProductSpuTagService {
    void saveManualTags(ProductSpuTagSaveReqVO reqVO);
    ProductTagBatchRespVO saveManualTagsBatch(ProductSpuTagBatchSaveReqVO reqVO);
    List<ProductSpuTagRespVO> getTagList(Long productSpuId);
    List<ProductSpuTagSimpleRespVO> getSimpleTagList(Collection<Long> productSpuIds);
}
```

```java
tagObjectRelationService.saveManualRelations(
        DOMAIN_TYPE_PRODUCT,
        OBJECT_TYPE_SPU,
        String.valueOf(reqVO.getProductSpuId()),
        tagValueIds);
```

```java
List<TagObjectRelationDO> relations = sortRelations(
        tagObjectRelationService.getActiveRelations(DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_SPU, String.valueOf(productSpuId)));
```

```java
Map<String, List<TagObjectRelationDO>> relationMap = relations.stream()
        .collect(Collectors.groupingBy(TagObjectRelationDO::getObjectId, LinkedHashMap::new, Collectors.toList()));
respVO.setTags(buildTagRespList(relationMap.getOrDefault(String.valueOf(productSpuId), List.of()), lookupContext));
```

- [ ] **Step 4: 新增控制器批量接口，返回成功数/失败数/失败明细**

```java
@PostMapping("/save-manual-batch")
@PreAuthorize("@ss.hasPermission('business:spu-table:update')")
public CommonResult<ProductTagBatchRespVO> saveManualTagsBatch(@Valid @RequestBody ProductSpuTagBatchSaveReqVO reqVO) {
    return success(productSpuTagService.saveManualTagsBatch(reqVO));
}
```

- [ ] **Step 5: 让 SpuTableServiceImpl 的标签筛选继续可用，并从字符串 objectId 反解回 Long**

```java
List<Long> taggedSpuIds = relations.stream()
        .map(TagObjectRelationDO::getObjectId)
        .filter(StrUtil::isNotBlank)
        .map(Long::valueOf)
        .distinct()
        .toList();
```

- [ ] **Step 6: 跑定向测试确认 SPU 单对象、批量、筛选都通过**

Run: `mvn -pl yudao-module-business -Dtest=TagObjectRelationServiceImplTest,ProductSpuTagServiceImplTest test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagService.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagServiceImpl.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/ProductSpuTagController.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/SpuTableServiceImpl.java \
  yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagServiceImplTest.java

git commit -m "feat: add spu batch tag operations"
```

---

### Task 3: 新增门店商品标签后端入口

**Files:**
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/StoreProductTagController.java`
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagSaveReqVO.java`
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagBatchSaveReqVO.java`
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagRespVO.java`
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagSimpleRespVO.java`
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagBatchRespVO.java`
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagService.java`
- Create: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagServiceImpl.java`
- Test: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagServiceImplTest.java`

- [ ] **Step 1: 先写失败测试，覆盖正式门店商品保存、批量保存、影子行禁止挂标**

```java
@Test
void saveManualTags_success() {
    StoreProductTagSaveReqVO reqVO = new StoreProductTagSaveReqVO();
    reqVO.setStoreProductId("SP-001");
    reqVO.setTagValueIds(List.of(11L, 12L));

    service.saveManualTags(reqVO);

    assertThat(service.getTagList("SP-001")).hasSize(2);
}

@Test
void saveManualTags_rejectsShadowRowId() {
    StoreProductTagSaveReqVO reqVO = new StoreProductTagSaveReqVO();
    reqVO.setStoreProductId("shadow-only");
    reqVO.setTagValueIds(List.of(11L));

    assertServiceException(() -> service.saveManualTags(reqVO), STORE_PRODUCT_NOT_EXISTS);
}
```

- [ ] **Step 2: 运行测试，确认门店商品标签服务还不存在**

Run: `mvn -pl yudao-module-business -Dtest=StoreProductTagServiceImplTest test`
Expected: FAIL，缺少 controller/service/VO。

- [ ] **Step 3: 新增 VO 和 service 接口，保持与 SPU 标签结构对称**

```java
@Data
public class StoreProductTagSaveReqVO {
    @NotBlank(message = "门店商品 ID 不能为空")
    private String storeProductId;
    private List<Long> tagValueIds;
}
```

```java
@Data
public class StoreProductTagBatchSaveReqVO {
    @NotEmpty(message = "门店商品 ID 列表不能为空")
    private List<String> storeProductIds;
    private List<Long> tagValueIds;
}
```

```java
public interface StoreProductTagService {
    void saveManualTags(StoreProductTagSaveReqVO reqVO);
    StoreProductTagBatchRespVO saveManualTagsBatch(StoreProductTagBatchSaveReqVO reqVO);
    List<StoreProductTagRespVO> getTagList(String storeProductId);
    List<StoreProductTagSimpleRespVO> getSimpleTagList(Collection<String> storeProductIds);
}
```

- [ ] **Step 4: 实现服务，先校验正式门店商品存在，再复用统一关系层**

```java
private StoreProductDO validateStoreProductExists(String storeProductId) {
    StoreProductDO storeProduct = storeProductMapper.selectById(storeProductId);
    if (storeProduct == null) {
        throw exception(STORE_PRODUCT_NOT_EXISTS);
    }
    return storeProduct;
}
```

```java
tagObjectRelationService.saveManualRelations(
        DOMAIN_TYPE_PRODUCT,
        OBJECT_TYPE_STORE_PRODUCT,
        reqVO.getStoreProductId(),
        normalizedTagValueIds);
```

```java
List<TagObjectRelationDO> relations = tagObjectRelationService.getActiveRelationsByObjectIds(
        DOMAIN_TYPE_PRODUCT,
        OBJECT_TYPE_STORE_PRODUCT,
        distinctStoreProductIds);
```

- [ ] **Step 5: 暴露对称的查询/保存/批量保存接口**

```java
@RestController
@RequestMapping("/business/store-product-tag")
public class StoreProductTagController {

    @GetMapping("/list")
    public CommonResult<List<StoreProductTagRespVO>> getTagList(@RequestParam("storeProductId") String storeProductId) {
        return success(storeProductTagService.getTagList(storeProductId));
    }

    @GetMapping("/simple-list")
    public CommonResult<List<StoreProductTagSimpleRespVO>> getSimpleTagList(@RequestParam("storeProductIds") List<String> storeProductIds) {
        return success(storeProductTagService.getSimpleTagList(storeProductIds));
    }

    @PostMapping("/save-manual")
    public CommonResult<Boolean> saveManualTags(@Valid @RequestBody StoreProductTagSaveReqVO reqVO) {
        storeProductTagService.saveManualTags(reqVO);
        return success(true);
    }

    @PostMapping("/save-manual-batch")
    public CommonResult<StoreProductTagBatchRespVO> saveManualTagsBatch(@Valid @RequestBody StoreProductTagBatchSaveReqVO reqVO) {
        return success(storeProductTagService.saveManualTagsBatch(reqVO));
    }
}
```

- [ ] **Step 6: 跑定向测试，确认门店商品标签闭环打通**

Run: `mvn -pl yudao-module-business -Dtest=StoreProductTagServiceImplTest test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/StoreProductTagController.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagSaveReqVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagBatchSaveReqVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagRespVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagSimpleRespVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductTagBatchRespVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagService.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagServiceImpl.java \
  yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagServiceImplTest.java

git commit -m "feat: add store product tag management"
```

---

### Task 4: 把 SPU 和门店商品列表的标签筛选、标签回显补成正式后端能力

**Files:**
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/tag/TagValueServiceImpl.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/vo/SpuTablePageReqVO.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductPageReqVO.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/SpuTableServiceImpl.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/store/StoreProductMapper.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductServiceImpl.java`
- Modify: `yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/StoreProductController.java`
- Test: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagServiceImplTest.java`
- Test: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagServiceImplTest.java`

- [ ] **Step 1: 先加失败用例，锁住两个筛选路径互不串扰**

```java
@Test
void getSpuTablePage_filtersBySpuTagOnly() {
    SpuTablePageReqVO reqVO = new SpuTablePageReqVO();
    reqVO.setTagValueId(11L);

    PageResult<SpuTableDO> page = spuTableService.getSpuTablePage(reqVO);
    assertEquals(List.of(101L), page.getList().stream().map(SpuTableDO::getProductSpuId).toList());
}

@Test
void getStoreProductPage_filtersByStoreProductTagOnly() {
    StoreProductPageReqVO reqVO = new StoreProductPageReqVO();
    reqVO.setTagValueId(11L);

    PageResult<StoreProductRespVO> page = storeProductService.getStoreProductPage(reqVO);
    assertEquals(List.of("SP-001"), page.getList().stream().map(StoreProductRespVO::getStoreProductId).toList());
}
```

- [ ] **Step 2: 给门店商品分页请求补标签筛选参数**

```java
@Schema(description = "标签值 ID", example = "1001")
private Long tagValueId;

@Schema(description = "门店商品 ID 列表（内部使用）")
private List<String> storeProductIds;
```

- [ ] **Step 3: 让 `TagValueServiceImpl` 明确接受 `STORE_PRODUCT` 候选池查询**

```java
@Override
public List<TagSelectableValueRespVO> getSelectableTagValuesForObject(String objectType) {
    validateObjectType(objectType);
    return buildSelectableValuesForProductDomain();
}
```

- [ ] **Step 4: 在门店商品分页里先按标签关系反查 objectId，再带回主查询**

```java
if (pageReqVO.getTagValueId() != null) {
    List<TagObjectRelationDO> relations = tagObjectRelationMapper.selectActiveListByTagValue(
            DOMAIN_TYPE_PRODUCT, OBJECT_TYPE_STORE_PRODUCT, pageReqVO.getTagValueId());
    if (CollUtil.isEmpty(relations)) {
        return PageResult.empty();
    }
    pageReqVO.setStoreProductIds(relations.stream()
            .map(TagObjectRelationDO::getObjectId)
            .filter(StrUtil::isNotBlank)
            .distinct()
            .toList());
}
```

```java
private LambdaQueryWrapperX<StoreProductDO> buildPageQuery(StoreProductPageReqVO reqVO, List<String> productSkuIds) {
    return new LambdaQueryWrapperX<StoreProductDO>()
            .inIfPresent(StoreProductDO::getStoreProductId, reqVO.getStoreProductIds())
            .eqIfPresent(StoreProductDO::getStoreId, reqVO.getStoreId())
            .inIfPresent(StoreProductDO::getProductSkuId, productSkuIds)
            .orderByDesc(StoreProductDO::getStoreProductId);
}
```

- [ ] **Step 5: 给门店商品列表聚合简要标签，和 SPU 一样在分页结果里挂回 tags 字段**

```java
Map<String, List<StoreProductTagRespVO>> tagMap = storeProductTagService.getSimpleTagList(formalIds).stream()
        .collect(Collectors.toMap(StoreProductTagSimpleRespVO::getStoreProductId,
                item -> item.getTags() == null ? Collections.emptyList() : item.getTags(),
                (a, b) -> a,
                LinkedHashMap::new));

respVO.setTags(tagMap.getOrDefault(storeProduct.getStoreProductId(), Collections.emptyList()));
```

- [ ] **Step 6: 跑后端定向测试，确认候选池、筛选、回显都通过**

Run: `mvn -pl yudao-module-business -Dtest=TagObjectRelationServiceImplTest,ProductSpuTagServiceImplTest,StoreProductTagServiceImplTest test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/tag/TagValueServiceImpl.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/product/vo/SpuTablePageReqVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/StoreProductPageReqVO.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/product/SpuTableServiceImpl.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/store/StoreProductMapper.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/StoreProductServiceImpl.java \
  yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/StoreProductController.java

git commit -m "feat: wire tag filters into product pages"
```

---

### Task 5: 补齐 SPU 前端批量挂标入口

**Files:**
- Modify: `ytsh-ui-vue3/src/api/business/product/index.ts`
- Modify: `ytsh-ui-vue3/src/views/business/product/index.vue`
- Modify: `ytsh-ui-vue3/src/views/business/product/ProductSpuTagForm.vue`
- Modify: `ytsh-ui-vue3/src/views/business/product/productTagLogic.ts`
- Create: `ytsh-ui-vue3/src/views/business/product/ProductSpuBatchTagForm.vue`

- [ ] **Step 1: 先补 API，暴露 SPU 批量保存接口**

```ts
export interface ProductTagBatchRespVO {
  successCount: number
  failureCount: number
  failureDetails: { objectId: string; reason: string }[]
}

saveProductSpuManualTagsBatch: async (data: { productSpuIds: number[]; tagValueIds: number[] }) => {
  return await request.post<ProductTagBatchRespVO>({
    url: '/business/product-spu-tag/save-manual-batch',
    data
  })
}
```

- [ ] **Step 2: 抽一个可复用的标签选择辅助，单对象与批量都走同一去重逻辑**

```ts
export const collectSelectedTagIds = (ids: number[]) => [...new Set(ids)].sort((a, b) => a - b)

export const formatBatchTagResult = (resp: { successCount: number; failureCount: number }) => {
  return `成功 ${resp.successCount} 条，失败 ${resp.failureCount} 条`
}
```

- [ ] **Step 3: 新增 SPU 批量挂标弹窗**

```vue
<script setup lang="ts">
import { SpuTableApi } from '@/api/business/product'
import { TagValueApi } from '@/api/business/tag/value'
import { collectSelectedTagIds, formatBatchTagResult } from './productTagLogic'

const open = async (ids: number[]) => {
  productSpuIds.value = ids
  selectableTagList.value = await TagValueApi.getTagValueListForObject('SPU')
  selectedTagIds.value = []
  dialogVisible.value = true
}

const submitForm = async () => {
  const resp = await SpuTableApi.saveProductSpuManualTagsBatch({
    productSpuIds: productSpuIds.value,
    tagValueIds: collectSelectedTagIds(selectedTagIds.value)
  })
  message.success(formatBatchTagResult(resp))
  emit('success')
}
</script>
```

- [ ] **Step 4: 在 SPU 列表页加按钮并接入弹窗**

```vue
<el-button
  type="warning"
  plain
  :disabled="isEmpty(checkedIds)"
  @click="openBatchTagForm"
  v-hasPermi="['business:spu-table:update']"
>
  <Icon icon="ep:collection-tag" class="mr-5px" /> 批量挂标
</el-button>
```

```ts
const batchTagFormRef = ref()
const openBatchTagForm = () => {
  batchTagFormRef.value.open(checkedIds.value)
}
```

- [ ] **Step 5: 运行前端 lint，确保 SPU 标签页没有类型错误**

Run: `pnpm --dir ytsh-ui-vue3 exec eslint src/api/business/product/index.ts src/views/business/product/index.vue src/views/business/product/ProductSpuTagForm.vue src/views/business/product/ProductSpuBatchTagForm.vue src/views/business/product/productTagLogic.ts`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add ytsh-ui-vue3/src/api/business/product/index.ts \
  ytsh-ui-vue3/src/views/business/product/index.vue \
  ytsh-ui-vue3/src/views/business/product/ProductSpuTagForm.vue \
  ytsh-ui-vue3/src/views/business/product/ProductSpuBatchTagForm.vue \
  ytsh-ui-vue3/src/views/business/product/productTagLogic.ts

git commit -m "feat: add spu batch tag ui"
```

---

### Task 6: 补齐门店商品前端标签入口、批量挂标、列表回显与筛选

**Files:**
- Modify: `ytsh-ui-vue3/src/api/business/store-product/index.ts`
- Modify: `ytsh-ui-vue3/src/views/business/store-product/index.vue`
- Create: `ytsh-ui-vue3/src/views/business/store-product/StoreProductTagForm.vue`
- Create: `ytsh-ui-vue3/src/views/business/store-product/StoreProductBatchTagForm.vue`

- [ ] **Step 1: 先扩门店商品 API 类型，给列表行补 tags 和四个标签接口**

```ts
export interface StoreProductTagRespVO {
  tagValueId: number
  tagValueCode: string
  tagValueName: string
  dimensionPath: string
  sources: string[]
}

export interface StoreProductTable {
  storeProductId?: number | string
  tags?: StoreProductTagRespVO[]
}

getStoreProductTagList: async (storeProductId: string) => {
  return await request.get<StoreProductTagRespVO[]>({
    url: '/business/store-product-tag/list',
    params: { storeProductId }
  })
}
```

- [ ] **Step 2: 新增单对象标签弹窗，完全复用 SPU 的交互方式，但对象类型切到 STORE_PRODUCT**

```vue
const open = async (storeProductId: string) => {
  const [tagPool, currentTags] = await Promise.all([
    TagValueApi.getTagValueListForObject('STORE_PRODUCT'),
    StoreProductApi.getStoreProductTagList(storeProductId)
  ])
  selectableTagList.value = tagPool
  selectedTagIds.value = currentTags.filter(item => item.sources.includes('MANUAL')).map(item => item.tagValueId)
}
```

- [ ] **Step 3: 新增门店商品批量挂标弹窗**

```vue
const submitForm = async () => {
  const resp = await StoreProductApi.saveStoreProductManualTagsBatch({
    storeProductIds: storeProductIds.value,
    tagValueIds: collectSelectedTagIds(selectedTagIds.value)
  })
  message.success(formatBatchTagResult(resp))
  emit('success')
}
```

- [ ] **Step 4: 在门店商品列表加筛选、标签列、正式行管理按钮和批量挂标按钮**

```vue
<el-form-item label="商品标签" prop="tagValueId">
  <el-select v-model="queryParams.tagValueId" clearable filterable class="!w-100%">
    <el-option v-for="item in selectableTagList" :key="item.tagValueId" :label="`${item.tagValueName}（${item.tagValueCode}）`" :value="item.tagValueId" />
  </el-select>
</el-form-item>
```

```vue
<el-table-column label="标签" min-width="220">
  <template #default="{ row }">
    <el-space v-if="isFormalRow(row)" wrap>
      <el-tag v-for="tag in row.tags || []" :key="`${row.storeProductId}-${tag.tagValueId}`" type="success">
        {{ tag.tagValueName }}
      </el-tag>
      <span v-if="!(row.tags || []).length" class="text-gray-400">-</span>
    </el-space>
    <span v-else class="text-gray-400">-</span>
  </template>
</el-table-column>
```

```vue
<el-button
  link
  type="primary"
  @click="openTagForm(String(scope.row.storeProductId))"
  v-hasPermi="['business:store-product:update']"
>
  管理标签
</el-button>
```

- [ ] **Step 5: 保持影子行约束，批量选择与按钮都只面向正式行**

```ts
const isSelectableRow = (row: StoreProductTable) => isFormalRow(row) && !!row.storeProductId
const openBatchTagForm = () => {
  batchTagFormRef.value.open(checkedIds.value.map(String))
}
```

- [ ] **Step 6: 运行前端 lint，确认门店商品页通过**

Run: `pnpm --dir ytsh-ui-vue3 exec eslint src/api/business/store-product/index.ts src/views/business/store-product/index.vue src/views/business/store-product/StoreProductTagForm.vue src/views/business/store-product/StoreProductBatchTagForm.vue`
Expected: PASS

- [ ] **Step 7: 手动验证页面**

Run: `pnpm --dir ytsh-ui-vue3 dev`
Expected: 本地 dev server 正常启动。

Manual check:
- SPU 列表可按标签筛选、打开单对象标签弹窗、执行批量挂标。
- 门店商品正式行可见标签列和“管理标签”，影子行无入口。
- 门店商品列表可按标签筛选并执行批量挂标。

- [ ] **Step 8: Commit**

```bash
git add ytsh-ui-vue3/src/api/business/store-product/index.ts \
  ytsh-ui-vue3/src/views/business/store-product/index.vue \
  ytsh-ui-vue3/src/views/business/store-product/StoreProductTagForm.vue \
  ytsh-ui-vue3/src/views/business/store-product/StoreProductBatchTagForm.vue

git commit -m "feat: add store product tag ui"
```

---

### Task 7: 跑完整验证并清理交付面

**Files:**
- Test: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationServiceImplTest.java`
- Test: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagServiceImplTest.java`
- Test: `yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagServiceImplTest.java`
- Verify: `ytsh-ui-vue3/src/views/business/product/index.vue`
- Verify: `ytsh-ui-vue3/src/views/business/store-product/index.vue`

- [ ] **Step 1: 跑业务模块全部定向后端测试**

Run: `mvn -pl yudao-module-business -Dtest=TagObjectRelationServiceImplTest,ProductSpuTagServiceImplTest,StoreProductTagServiceImplTest test`
Expected: PASS

- [ ] **Step 2: 跑前端定向 lint**

Run: `pnpm --dir ytsh-ui-vue3 exec eslint src/api/business/product/index.ts src/api/business/store-product/index.ts src/views/business/product/index.vue src/views/business/product/ProductSpuTagForm.vue src/views/business/product/ProductSpuBatchTagForm.vue src/views/business/product/productTagLogic.ts src/views/business/store-product/index.vue src/views/business/store-product/StoreProductTagForm.vue src/views/business/store-product/StoreProductBatchTagForm.vue`
Expected: PASS

- [ ] **Step 3: 人工走一遍黄金路径和影子边界**

Checklist:
- SPU 单对象保存标签成功，列表刷新后可回显。
- SPU 批量挂标成功，结果提示显示成功/失败数。
- 门店商品正式行保存标签成功。
- 门店商品影子行无标签入口，且无法被勾选进入批量挂标。
- 两边按同一标签筛选时，SPU 与 STORE_PRODUCT 查询互不串扰。

- [ ] **Step 4: 汇总风险与落地备注，准备提审**

```text
1. 已执行 object_id bigint -> varchar(64) 迁移脚本。
2. 历史 SPU 标签数据通过数据库隐式或显式字符串化保留。
3. 后续若扩 SKU 标签，只需要新增 OBJECT_TYPE_SKU 与对应业务入口。
```

- [ ] **Step 5: Commit**

```bash
git add yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/tag/TagObjectRelationServiceImplTest.java \
  yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/product/ProductSpuTagServiceImplTest.java \
  yudao-module-business/src/test/java/cn/iocoder/yudao/module/business/service/store/StoreProductTagServiceImplTest.java \
  docs/superpowers/specs/2026-05-13-product-tag-full-mount-design.md \
  docs/superpowers/plans/2026-05-13-product-tag-full-mount.md

git commit -m "test: verify product tag full mount flow"
```

---

## Self-Review

- Spec coverage: 已覆盖统一关系层、SPU/STORE_PRODUCT 双对象、批量挂标、列表回显、按标签筛选、影子行隔离、后端测试与前端验证。
- Placeholder scan: 无 `TODO/TBD/稍后实现/类似上一步` 占位语句。
- Type consistency: 计划统一以 `String objectId` 驱动关系层；SPU 侧通过 `String.valueOf(productSpuId)` 适配，STORE_PRODUCT 侧直接使用字符串主键。
- Key risk fixed in plan: `tag_object_relation.object_id` 当前是 `bigint`，计划已显式纳入 SQL 迁移与 Java 签名改造，避免实现时才发现门店商品无法落标签。
