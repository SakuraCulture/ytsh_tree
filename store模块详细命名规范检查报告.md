# Store 模块详细命名规范检查报告

---

## 一、统一命名规范标准（补充缺失词汇）

### 1.1 新增实体名词

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

### 1.2 新增常见组合词

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
| region_code | 行政区划代码 |

---

## 二、详细命名问题清单

### 优先级 1：方法和变量命名

| 序号 | 文件路径 | 修改前命名 | 修改后命名 |
|-----|---------|----------|----------|
| 1 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableService.java | createTable | createStore |
| 2 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableService.java | updateTable | updateStore |
| 3 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableService.java | deleteTable | deleteStore |
| 4 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableService.java | deleteTableListByIds | deleteStoreListByIds |
| 5 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableService.java | getTable | getStore |
| 6 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableService.java | getTablePage | getStorePage |
| 7 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableService.java | getSpaceTableByStoreId | getSpaceByStoreId |
| 8 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableService.java | getAffiliationTableByStoreId | getAffiliationByStoreId |
| 9 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableService.java | getStatusTableByStoreId | getBusinessStatusByStoreId |
| 10 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableService.java | getFranchiseeTableByStoreId | getFranchiseeByStoreId |
| 11 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableService.java | getContactTableListByStoreId | getContactListByStoreId |
| 12 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | createTable | createStore |
| 13 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | updateTable | updateStore |
| 14 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | deleteTable | deleteStore |
| 15 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | deleteTableListByIds | deleteStoreListByIds |
| 16 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | getTable | getStore |
| 17 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | getTablePage | getStorePage |
| 18 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | getSpaceTableByStoreId | getSpaceByStoreId |
| 19 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | createSpaceTable | createSpace |
| 20 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | updateSpaceTable | updateSpace |
| 21 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | deleteSpaceTableByStoreId | deleteSpaceByStoreId |
| 22 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | deleteSpaceTableByStoreIds | deleteSpaceByStoreIds |
| 23 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | getAffiliationTableByStoreId | getAffiliationByStoreId |
| 24 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | createAffiliationTable | createAffiliation |
| 25 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | updateAffiliationTable | updateAffiliation |
| 26 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | deleteAffiliationTableByStoreId | deleteAffiliationByStoreId |
| 27 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | deleteAffiliationTableByStoreIds | deleteAffiliationByStoreIds |
| 28 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | getStatusTableByStoreId | getBusinessStatusByStoreId |
| 29 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | createStatusTable | createBusinessStatus |
| 30 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | updateStatusTable | updateBusinessStatus |
| 31 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | deleteStatusTableByStoreId | deleteBusinessStatusByStoreId |
| 32 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | deleteStatusTableByStoreIds | deleteBusinessStatusByStoreIds |
| 33 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | getFranchiseeTableByStoreId | getFranchiseeByStoreId |
| 34 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | createFranchiseeTable | createFranchisee |
| 35 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | updateFranchiseeTable | updateFranchisee |
| 36 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | deleteFranchiseeTableByStoreId | deleteFranchiseeByStoreId |
| 37 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | deleteFranchiseeTableByStoreIds | deleteFranchiseeByStoreIds |
| 38 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | getContactTableListByStoreId | getContactListByStoreId |
| 39 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | createContactTableList | createContactList |
| 40 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | updateContactTableList | updateContactList |
| 41 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | deleteContactTableByStoreId | deleteContactByStoreId |
| 42 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | deleteContactTableByStoreIds | deleteContactByStoreIds |
| 43 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | validateTableExists | validateStoreExists |
| 44 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | createTable | createStore |
| 45 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | updateTable | updateStore |
| 46 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | deleteTable | deleteStore |
| 47 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | deleteTableList | deleteStoreList |
| 48 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | getTable | getStore |
| 49 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | getTablePage | getStorePage |
| 50 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | getTableSimpleList | getStoreSimpleList |
| 51 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | exportTableExcel | exportStoreExcel |
| 52 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | getSpaceTableByStoreId | getSpaceByStoreId |
| 53 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | getAffiliationTableByStoreId | getAffiliationByStoreId |
| 54 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | getStatusTableByStoreId | getBusinessStatusByStoreId |
| 55 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | getFranchiseeTableByStoreId | getFranchiseeByStoreId |
| 56 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | getContactTableListByStoreId | getContactListByStoreId |
| 57 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/dataobject/store/TableDO.java | entityStatus | storeStatus |
| 58 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | tableMapper | storeMapper |
| 59 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | existStore | existStore (保留，因为符合语义) |
| 60 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | table | store |

---

### 优先级 2：文件名（类名）

| 序号 | 文件路径 | 修改前命名 | 修改后命名 |
|-----|---------|----------|----------|
| 1 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/dataobject/store/TableDO.java | TableDO | StoreDO |
| 2 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/dataobject/store/AffiliationTableDO.java | AffiliationTableDO | StoreAffiliationDO |
| 3 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/dataobject/store/BusinessStatusTableDO.java | BusinessStatusTableDO | StoreBusinessStatusDO |
| 4 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/dataobject/store/ContactTableDO.java | ContactTableDO | StoreContactDO |
| 5 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/dataobject/store/FranchiseeTableDO.java | FranchiseeTableDO | StoreFranchiseeDO |
| 6 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/dataobject/store/SpaceTableDO.java | SpaceTableDO | StoreSpaceDO |
| 7 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/store/TableMapper.java | TableMapper | StoreMapper |
| 8 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/store/AffiliationTableMapper.java | AffiliationTableMapper | StoreAffiliationMapper |
| 9 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/store/BusinessStatusTableMapper.java | BusinessStatusTableMapper | StoreBusinessStatusMapper |
| 10 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/store/ContactTableMapper.java | ContactTableMapper | StoreContactMapper |
| 11 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/store/FranchiseeTableMapper.java | FranchiseeTableMapper | StoreFranchiseeMapper |
| 12 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/dal/mysql/store/SpaceTableMapper.java | SpaceTableMapper | StoreSpaceMapper |
| 13 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableService.java | TableService | StoreService |
| 14 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/service/store/TableServiceImpl.java | TableServiceImpl | StoreServiceImpl |
| 15 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | TableController | StoreController |
| 16 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/TablePageReqVO.java | TablePageReqVO | StorePageReqVO |
| 17 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/TableRespVO.java | TableRespVO | StoreRespVO |
| 18 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/TableSaveReqVO.java | TableSaveReqVO | StoreSaveReqVO |

---

### 优先级 3：URL路由和错误码

| 序号 | 文件路径 | 修改前命名 | 修改后命名 |
|-----|---------|----------|----------|
| 1 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | /business/table | /business/store |
| 2 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | @PreAuthorize("@ss.hasPermission('business:table:create')") | @PreAuthorize("@ss.hasPermission('business:store:create')") |
| 3 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | @PreAuthorize("@ss.hasPermission('business:table:update')") | @PreAuthorize("@ss.hasPermission('business:store:update')") |
| 4 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | @PreAuthorize("@ss.hasPermission('business:table:delete')") | @PreAuthorize("@ss.hasPermission('business:store:delete')") |
| 5 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | @PreAuthorize("@ss.hasPermission('business:table:query')") | @PreAuthorize("@ss.hasPermission('business:store:query')") |
| 6 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | @PreAuthorize("@ss.hasPermission('business:table:export')") | @PreAuthorize("@ss.hasPermission('business:store:export')") |
| 7 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/TableController.java | @PreAuthorize("@ss.hasPermission('business:table:import')") | @PreAuthorize("@ss.hasPermission('business:store:import')") |
| 8 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/enums/ErrorCodeConstants.java | TABLE_NOT_EXISTS | STORE_NOT_EXISTS |
| 9 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/enums/ErrorCodeConstants.java | TABLE_IMPORT_LIST_IS_EMPTY | STORE_IMPORT_LIST_IS_EMPTY |
| 10 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/enums/ErrorCodeConstants.java | TABLE_CODE_EXISTS | STORE_CODE_EXISTS |
| 11 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/enums/ErrorCodeConstants.java | TABLE_NAME_EXISTS | STORE_NAME_EXISTS |

---

### 优先级 4：VO类字段引用

| 序号 | 文件路径 | 修改前命名 | 修改后命名 |
|-----|---------|----------|----------|
| 1 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/TableSaveReqVO.java | SpaceTableDO | StoreSpaceDO |
| 2 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/TableSaveReqVO.java | AffiliationTableDO | StoreAffiliationDO |
| 3 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/TableSaveReqVO.java | BusinessStatusTableDO | StoreBusinessStatusDO |
| 4 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/TableSaveReqVO.java | FranchiseeTableDO | StoreFranchiseeDO |
| 5 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/TableSaveReqVO.java | ContactTableDO | StoreContactDO |
| 6 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/TableSaveReqVO.java | spaceTable | space |
| 7 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/TableSaveReqVO.java | affiliationTable | affiliation |
| 8 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/TableSaveReqVO.java | statusTable | businessStatus |
| 9 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/TableSaveReqVO.java | franchiseeTable | franchisee |
| 10 | yudao-module-business/src/main/java/cn/iocoder/yudao/module/business/controller/admin/store/vo/TableSaveReqVO.java | contactTables | contacts |

---

## 三、表名问题（如果有数据库表）

| 现有表名 | 建议表名 |
|---------|---------|
| store_table | store_table (保留，因为符合 store + _table 规则) |
| store_affiliation_table | store_affiliation_table (符合规则) |
| store_business_status_table | store_business_status_table (符合规则) |
| store_contact_table | store_contact_table (符合规则) |
| store_franchisee_table | store_franchisee_table (符合规则) |
| store_space_table | store_space_table (符合规则) |

---

## 四、修改建议顺序

1. **第一步**：补充词汇拆解表，添加缺失的词汇（见本报告第一部分）
2. **第二步**：修改优先级 1 的方法和变量命名
3. **第三步**：修改优先级 2 的文件名（类名）
4. **第四步**：修改优先级 3 的 URL 路由和错误码
5. **第五步**：修改优先级 4 的 VO 类字段引用

---

## 五、注意事项

1. 所有修改应确保代码的一致性和可读性
2. 修改后需要进行全面的测试，确保功能正常
3. 建议使用 IDE 的重构功能进行批量重命名
4. 注意更新所有引用该类/方法的地方
