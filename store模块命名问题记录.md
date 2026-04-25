# Store 模块命名问题记录

---

## 一、类名问题（与词汇拆解表不符）

| 现有类名 | 问题说明 |
|---------|---------|
| `TableDO` | 应该用 `StoreDO`，更符合门店业务含义 |
| `TableController` | 应该用 `StoreController` |
| `TableService` | 应该用 `StoreService` |
| `TableServiceImpl` | 应该用 `StoreServiceImpl` |
| `TablePageReqVO` | 应该用 `StorePageReqVO` |
| `TableRespVO` | 应该用 `StoreRespVO` |
| `TableSaveReqVO` | 应该用 `StoreSaveReqVO` |
| `TableMapper` | 应该用 `StoreMapper` |
| `AffiliationTableDO` | 词汇拆解表中无 `affiliation` |
| `BusinessStatusTableDO` | 词汇拆解表中无 `business` |
| `FranchiseeTableDO` | 词汇拆解表中无 `franchisee` |
| `SpaceTableDO` | 词汇拆解表中无 `space` |
| `AffiliationTableMapper` | 同上 |
| `BusinessStatusTableMapper` | 同上 |
| `FranchiseeTableMapper` | 同上 |
| `SpaceTableMapper` | 同上 |

---

## 二、字段名问题（与词汇拆解表不符）

### 2.1 TableDO.java 字段
| 现有字段 | 问题说明 |
|---------|---------|
| `entityStatus` | 词汇拆解表中有 `store_status`，应该用 `storeStatus` |
| `regionCode` | 词汇拆解表中无 `region` |
| `area` | 词汇拆解表中无 `area` |

### 2.2 AffiliationTableDO.java 字段
| 现有字段 | 问题说明 |
|---------|---------|
| `affiliationId` | 词汇拆解表中无 `affiliation` |
| `businessMode` | 词汇拆解表中无 `business`、`mode` |
| `storeType` | 词汇拆解表中有 `store` 和 `type`，但组合 `business_mode` 不存在 |

### 2.3 BusinessStatusTableDO.java 字段
| 现有字段 | 问题说明 |
|---------|---------|
| `storeBusinessStatusId` | 词汇拆解表中无 `business` |
| `currentStatus` | 词汇拆解表中有 `status`，但无 `current` |
| `openDate` | 词汇拆解表中有 `date`，但无 `open` |
| `signDate` | 词汇拆解表中有 `date`，但无 `sign` |

### 2.4 FranchiseeTableDO.java 字段
| 现有字段 | 问题说明 |
|---------|---------|
| `franchiseeId` | 词汇拆解表中无 `franchisee` |
| `franchiseeName` | 词汇拆解表中无 `franchisee` |
| `franchiseePhone` | 词汇拆解表中无 `franchisee` |
| `franchiseeFee` | 词汇拆解表中无 `franchisee` |
| `securityDeposit` | 词汇拆解表中无 `security`、`deposit` |
| `contractStart` | 词汇拆解表中无 `contract` |
| `contractEnd` | 词汇拆解表中无 `contract` |

### 2.5 SpaceTableDO.java 字段
| 现有字段 | 问题说明 |
|---------|---------|
| `storeSpaceId` | 词汇拆解表中无 `space` |
| `buildingArea` | 词汇拆解表中无 `building`、`area` |
| `coldStorageArea` | 词汇拆解表中无 `cold`、`storage`、`area` |

### 2.6 ContactTableDO.java 字段
| 现有字段 | 问题说明 |
|---------|---------|
| `contactRole` | 词汇拆解表中无 `role` |

---

## 三、URL路由问题

| 现有路由 | 问题说明 |
|---------|---------|
| `/business/table` | 应该用 `/business/store` |
| `/business/table/create` | 应该用 `/business/store/create` |
| 等等所有以 `/business/table` 开头的路由 | 同上 |

---

## 四、错误码问题

| 现有错误码 | 问题说明 |
|-----------|---------|
| `TABLE_NOT_EXISTS` | 应该用 `STORE_NOT_EXISTS` |
| `TABLE_IMPORT_LIST_IS_EMPTY` | 应该用 `STORE_IMPORT_LIST_IS_EMPTY` |
| `TABLE_CODE_EXISTS` | 应该用 `STORE_CODE_EXISTS` |
| `TABLE_NAME_EXISTS` | 应该用 `STORE_NAME_EXISTS` |

---

## 五、Service方法名问题

| 现有方法名 | 问题说明 |
|-----------|---------|
| `createTable` | 应该用 `createStore` |
| `updateTable` | 应该用 `updateStore` |
| `deleteTable` | 应该用 `deleteStore` |
| `deleteTableListByIds` | 应该用 `deleteStoreListByIds` |
| `getTable` | 应该用 `getStore` |
| `getTablePage` | 应该用 `getStorePage` |
| `getSpaceTableByStoreId` | 应该用 `getSpaceByStoreId` |
| `getAffiliationTableByStoreId` | 应该用 `getAffiliationByStoreId` |
| `getStatusTableByStoreId` | 应该用 `getStatusByStoreId` |
| `getFranchiseeTableByStoreId` | 应该用 `getFranchiseeByStoreId` |
| `getContactTableListByStoreId` | 应该用 `getContactListByStoreId` |
| `importStoreList` | 这个符合 |

---

## 六、Controller方法名问题

| 现有方法名 | 问题说明 |
|-----------|---------|
| `createTable` | 应该用 `createStore` |
| `updateTable` | 应该用 `updateStore` |
| `deleteTable` | 应该用 `deleteStore` |
| `deleteTableList` | 应该用 `deleteStoreList` |
| `getTable` | 应该用 `getStore` |
| `getTablePage` | 应该用 `getStorePage` |
| `getTableSimpleList` | 应该用 `getStoreSimpleList` |
| `exportTableExcel` | 应该用 `exportStoreExcel` |
| `getSpaceTableByStoreId` | 应该用 `getSpaceByStoreId` |
| `getAffiliationTableByStoreId` | 应该用 `getAffiliationByStoreId` |
| `getStatusTableByStoreId` | 应该用 `getStatusByStoreId` |
| `getFranchiseeTableByStoreId` | 应该用 `getFranchiseeByStoreId` |
| `getContactTableListByStoreId` | 应该用 `getContactListByStoreId` |

---

## 七、需要补充到词汇拆解表的新词

### 7.1 实体名词
| 英文 | 中文 | 说明 |
|------|------|------|
| region | 行政区划 | 行政区划 |
| area | 区域 | 区域 |
| affiliation | 归属 | 归属关系 |
| business | 经营 | 经营 |
| mode | 方式 | 方式 |
| franchisee | 加盟商 | 加盟商 |
| security | 安全/保证金 | 安全/保证金 |
| deposit | 押金 | 押金 |
| contract | 合同 | 合同 |
| space | 空间 | 空间 |
| building | 房屋 | 房屋 |
| cold | 冷 | 冷 |
| storage | 仓储 | 仓储 |
| role | 角色 | 角色 |
| open | 开业 | 开业 |
| sign | 签约 | 签约 |
| current | 当前 | 当前 |

### 7.2 常见组合词
| 英文组合 | 中文组合 |
|----------|----------|
| business_mode | 经营方式 |
| store_type | 门店类型 |
| current_status | 当前状态 |
| open_date | 开业日期 |
| sign_date | 签约日期 |
| security_deposit | 保证金 |
| contract_start | 合同开始日期 |
| contract_end | 合同结束日期 |
| building_area | 房屋面积 |
| cold_storage_area | 冷库面积 |
| contact_role | 联系人角色 |

---

## 总结

Store 模块中大量命名不符合词汇拆解表，主要问题包括：
1. 核心类名使用 `Table` 而不是 `Store`
2. 很多业务词汇（如 franchisee、affiliation、business 等）未在词汇拆解表中定义
3. 部分字段组合不符合词汇拆解表的组合规则

建议：
1. 要么统一修改所有不符合的命名
2. 要么将缺失的词汇补充到词汇拆解表中
