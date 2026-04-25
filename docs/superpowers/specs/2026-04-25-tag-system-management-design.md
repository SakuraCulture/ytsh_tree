# 标签体系系统管理一期设计

## 背景

现有 `docs/标签体系说明文档.md` 已经定义了人、货、场三套标签体系，包含 L1 领域、L2 维度类、L3 原子维度、L4 标签值、打标方式、数据源、更新频率和打标逻辑说明。

现有数据库已经具备商品、门店、门店商品、库存、订单等基础业务表，例如 `product_spu_table`、`product_sku_table`、`product_category_table`、`store_table`、`store_product_table`、`store_stock_table`、`order_table`、`order_item_table`。但当前缺少统一的标签元数据表，也没有对象与标签值的统一关系表。

一期目标是先建设“标签体系元数据管理能力”，把标签体系沉淀成可维护、可导入、可扩展的数据资产。自动打标、圈选、看板和算法标签生产放到后续阶段。

## 一期目标

一期建设范围：

1. 支持人、货、场三个对象域的标签体系维护。
2. 支持 L1/L2/L3 标签维度树维护。
3. 支持 L4 标签值维护。
4. 支持维护打标方式、数据源、更新频率、打标逻辑说明。
5. 支持标签启用、停用、排序。
6. 支持从结构化模板导入标签体系。
7. 支持虚拟标签定义管理，先存储组合表达式，不执行圈选。
8. 按 Yudao 后台菜单机制挂载到 `ytsh-ui-vue3`。

一期不建设：

1. 自动打标任务。
2. 规则引擎执行。
3. 算法标签生产。
4. 人群、商品、门店圈选。
5. 标签命中对象统计。
6. 标签效果分析看板。
7. 对象与标签值关系表的生产链路。

## 推荐方案

采用独立标签元数据表，不复用 `system_dict_type` / `system_dict_data` 承载核心标签体系。

原因：

- 系统字典适合简单枚举，不适合承载 L1-L4 层级、对象域、打标方式、数据源、更新频率和逻辑说明。
- 独立表能保持标签语义与商品、门店、订单等业务表解耦。
- 后续可以自然扩展到对象打标结果、规则引擎和圈选能力。

## 数据模型

### 标签维度表 `tag_dimension`

用于维护 L1、L2、L3 层级结构。

核心字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `domain_type` | 对象域：`PRODUCT` 货、`STORE` 场、`MEMBER` 人 |
| `parent_id` | 父级维度，根节点为 0 |
| `level` | 层级：1、2、3 |
| `name` | 维度名称 |
| `code` | 维度编码 |
| `sort` | 排序 |
| `status` | 状态：启用、停用 |
| `description` | 说明 |
| `creator/create_time/updater/update_time/deleted/tenant_id` | Yudao 标准审计字段 |

约束建议：

- 同一租户、同一对象域、同一父级下 `code` 唯一。
- L4 标签值不进入该表，统一放入 `tag_value`。
- 删除维度前校验是否存在子维度或标签值。

### 标签值表 `tag_value`

用于维护 L3 原子维度下的 L4 标签值。

核心字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `dimension_id` | 所属 L3 原子维度 |
| `name` | 标签值名称 |
| `code` | 标签值编码 |
| `tag_method` | 打标方式：`MANUAL`、`RULE`、`ALGORITHM`、`INHERIT` |
| `data_source` | 数据源 |
| `update_frequency` | 更新频率 |
| `logic_description` | 打标逻辑说明 |
| `sort` | 排序 |
| `status` | 状态：启用、停用 |
| `creator/create_time/updater/update_time/deleted/tenant_id` | Yudao 标准审计字段 |

约束建议：

- 同一 L3 维度下 `code` 唯一。
- `dimension_id` 必须指向 level=3 的标签维度。
- 停用标签值不影响历史定义，但后续不再推荐使用。

### 虚拟标签表 `tag_virtual`

用于维护业务常用的聚合标签，例如“露营核心装备”“高价值活跃会员”“门店问题预警”。

核心字段：

| 字段 | 说明 |
| --- | --- |
| `id` | 主键 |
| `domain_type` | 对象域 |
| `name` | 虚拟标签名称 |
| `code` | 虚拟标签编码 |
| `expression_json` | 组合条件表达式 |
| `expression_summary` | 表达式摘要，便于列表展示 |
| `usage_scenario` | 典型用途 |
| `status` | 状态：启用、停用 |
| `creator/create_time/updater/update_time/deleted/tenant_id` | Yudao 标准审计字段 |

一期只存定义，不执行表达式。表达式应尽量引用 `tag_value.id` 或 `tag_value.code`，避免只存自然语言。

表达式示例：

```json
{
  "operator": "AND",
  "conditions": [
    { "dimensionCode": "usage_space", "valueCode": "outdoor_camping" },
    { "dimensionCode": "core_action", "valueCode": "cooking" },
    { "dimensionCode": "function_pain_point", "valueCode": "portable_lightweight" }
  ]
}
```

## 后端设计

建议放在 `yudao-module-business` 下，沿用现有业务模块结构。

包结构建议：

```text
yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/
├── controller/admin/tag/
│   ├── TagDimensionController.java
│   ├── TagValueController.java
│   └── TagVirtualController.java
├── controller/admin/tag/vo/
├── dal/dataobject/tag/
│   ├── TagDimensionDO.java
│   ├── TagValueDO.java
│   └── TagVirtualDO.java
├── dal/mysql/tag/
│   ├── TagDimensionMapper.java
│   ├── TagValueMapper.java
│   └── TagVirtualMapper.java
├── service/tag/
│   ├── TagDimensionService.java
│   ├── TagValueService.java
│   ├── TagVirtualService.java
│   └── impl/
└── convert/tag/
```

接口能力：

### 标签维度

- 创建维度。
- 更新维度。
- 删除维度。
- 获取维度详情。
- 获取维度树。
- 启用/停用维度。

### 标签值

- 创建标签值。
- 更新标签值。
- 删除标签值。
- 分页查询标签值。
- 按 L3 维度查询标签值。
- 启用/停用标签值。
- 导入标签值和维度。

### 虚拟标签

- 创建虚拟标签。
- 更新虚拟标签。
- 删除虚拟标签。
- 分页查询虚拟标签。
- 启用/停用虚拟标签。

权限标识建议：

```text
business:tag-dimension:query
business:tag-dimension:create
business:tag-dimension:update
business:tag-dimension:delete

business:tag-value:query
business:tag-value:create
business:tag-value:update
business:tag-value:delete
business:tag-value:import

business:tag-virtual:query
business:tag-virtual:create
business:tag-virtual:update
business:tag-virtual:delete
```

## 导入设计

一期支持结构化 Excel 导入，不直接解析 Markdown 文档。

推荐模板字段：

| 字段 | 说明 |
| --- | --- |
| `domainType` | `PRODUCT` / `STORE` / `MEMBER` |
| `l1Name` | L1 领域名称 |
| `l1Code` | L1 编码 |
| `l2Name` | L2 维度类名称 |
| `l2Code` | L2 编码 |
| `l3Name` | L3 原子维度名称 |
| `l3Code` | L3 编码 |
| `tagValueName` | L4 标签值名称 |
| `tagValueCode` | L4 标签值编码 |
| `tagMethod` | 打标方式 |
| `dataSource` | 数据源 |
| `updateFrequency` | 更新频率 |
| `logicDescription` | 打标逻辑说明 |
| `sort` | 排序 |

导入规则：

- 维度按 `domainType + parent + code` 幂等创建或更新。
- 标签值按 `dimension + tagValueCode` 幂等创建或更新。
- 单行失败不影响其它行，返回成功数、失败数、失败明细。
- 导入只处理元数据，不生成对象打标结果。

## 前端设计

前端放在 `ytsh-ui-vue3`。

页面建议：

```text
ytsh-ui-vue3/src/views/business/tag/dimension/index.vue
ytsh-ui-vue3/src/views/business/tag/value/index.vue
ytsh-ui-vue3/src/views/business/tag/virtual/index.vue
ytsh-ui-vue3/src/views/business/tag/import/index.vue
```

API 建议：

```text
ytsh-ui-vue3/src/api/business/tag/dimension/index.ts
ytsh-ui-vue3/src/api/business/tag/value/index.ts
ytsh-ui-vue3/src/api/business/tag/virtual/index.ts
```

页面能力：

### 标签维度管理

- 左侧对象域筛选：人、货、场。
- 树形展示 L1/L2/L3。
- 新增、编辑、删除、启停、排序。
- 只允许在 L3 下维护标签值。

### 标签值管理

- 按对象域、L1、L2、L3、状态筛选。
- 列表展示标签值、打标方式、数据源、更新频率、状态。
- 新增、编辑、删除、启停。
- 支持导入入口。

### 虚拟标签管理

- 按对象域、状态筛选。
- 展示名称、表达式摘要、用途、状态。
- 新增、编辑、删除、启停。
- 一期表达式编辑可采用简单 JSON 文本框或条件行编辑器。若控制范围，优先使用 JSON 文本框加基础校验。

## 菜单挂载

建议挂载为后台菜单：

```text
标签体系
├── 标签维度管理
├── 标签值管理
├── 虚拟标签管理
└── 标签导入
```

动态路由通过 Yudao 后台菜单配置完成，不改 `remaining.ts`。

组件路径示例：

```text
business/tag/dimension/index
business/tag/value/index
business/tag/virtual/index
business/tag/import/index
```

## 错误处理与校验

后端校验：

- 对象域必须是合法枚举。
- 维度层级只能是 1、2、3。
- L1 父级必须为 0。
- L2 父级必须是 L1。
- L3 父级必须是 L2。
- 标签值只能挂在 L3 下。
- 删除维度前必须不存在子维度和标签值。
- 删除标签值前，后续如果存在对象打标关系，需要阻止删除；一期可先保留接口边界。
- 虚拟标签表达式必须是合法 JSON，并引用当前对象域下的标签值。

导入错误返回：

- 行号。
- 字段名。
- 错误原因。
- 原始值。

## 测试策略

后端测试：

- 标签维度创建、更新、删除、树查询。
- 标签维度层级校验。
- 标签值只能挂 L3 的校验。
- 重复编码校验。
- 导入幂等校验。
- 虚拟标签表达式基础校验。

前端验证：

- 标签维度树新增、编辑、删除、启停。
- 标签值筛选和维护。
- Excel 导入成功与失败明细展示。
- 虚拟标签创建和编辑。
- 后台菜单挂载后可正常访问。

## 后续扩展

二期可以新增对象打标结果表：

```text
tag_object_relation
- id
- domain_type
- object_type       -- SPU / SKU / STORE / STORE_PRODUCT / MEMBER
- object_id
- tag_value_id
- source_type       -- MANUAL / RULE / ALGORITHM / INHERIT
- confidence
- effective_time
- expire_time
```

后续能力：

- 商品打标。
- 门店打标。
- 门店商品打标。
- 消费者打标。
- 规则引擎执行。
- 虚拟标签解析与圈选。
- 标签覆盖率、使用率、僵尸标签治理看板。

## 结论

一期采用“标签元数据中心先行，打标生产后置”的设计。这样既能快速把现有标签体系说明文档结构化入库，又避免在数据源和算法链路未稳定前过早建设复杂规则引擎。该方案与现有 Yudao 模块结构、动态菜单机制和业务数据库现状匹配，后续可平滑扩展到人、货、场对象打标与圈选能力。
