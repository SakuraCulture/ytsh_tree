package cn.iocoder.yudao.module.business.service.store;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreImportExcelVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformInfoRespVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreSaveReqVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreSimpleRespVO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.AffiliationTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.BusinessStatusTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.ContactTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.FranchiseeTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.PlatformDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.PlatformTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.SpaceTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreDO;
import cn.iocoder.yudao.module.business.dal.mysql.store.AffiliationTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.BusinessStatusTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.ContactTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.FranchiseeTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.PlatformMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.PlatformTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.SpaceTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

@Import(StoreServiceImpl.class)
class StoreServiceImplTest extends BaseDbUnitTest {

    @Resource
    private StoreService storeService;
    @Resource
    private StoreMapper storeMapper;
    @Resource
    private SpaceTableMapper spaceTableMapper;
    @Resource
    private AffiliationTableMapper affiliationTableMapper;
    @Resource
    private BusinessStatusTableMapper statusTableMapper;
    @Resource
    private FranchiseeTableMapper franchiseeTableMapper;
    @Resource
    private ContactTableMapper contactTableMapper;
    @Resource
    private PlatformTableMapper platformTableMapper;
    @Resource
    private PlatformMapper platformMapper;
    @Resource
    private PlatformTransactionManager transactionManager;

    @MockBean
    private StorePlatformCacheService storePlatformCacheService;

    @Test
    void updateStore_whenOneToOneChildrenAreNull_shouldDeleteExistingChildren() {
        insertStore("S001", "示例门店");
        insertSpace("S001", "120.50", "20.00");
        insertAffiliation("S001", "DIRECT", "O2O");
        insertStatus("S001", "NORMAL", LocalDate.of(2026, 1, 2), LocalDate.of(2025, 12, 1));
        insertFranchisee("S001", "示例加盟商", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        StoreSaveReqVO reqVO = buildUpdateReq("S001", "示例门店已更新");
        reqVO.setSpaceTable(null);
        reqVO.setAffiliationTable(null);
        reqVO.setStatusTable(null);
        reqVO.setFranchiseeTable(null);

        storeService.updateStore(reqVO);

        assertNull(spaceTableMapper.selectByStoreId("S001"));
        assertNull(affiliationTableMapper.selectByStoreId("S001"));
        assertNull(statusTableMapper.selectByStoreId("S001"));
        assertNull(franchiseeTableMapper.selectByStoreId("S001"));
        assertEquals("示例门店已更新", storeMapper.selectById("S001").getStoreName());
        verify(storePlatformCacheService).syncStorePlatformInfoToRedis();
    }

    @Test
    void updateStore_shouldRefreshCacheAfterCommit() {
        insertStore("S007", "事务提交门店");
        StoreSaveReqVO reqVO = buildUpdateReq("S007", "事务提交门店已更新");

        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> storeService.updateStore(reqVO));

        assertEquals("事务提交门店已更新", storeMapper.selectById("S007").getStoreName());
        verify(storePlatformCacheService).syncStorePlatformInfoToRedis();
    }

    @Test
    void updateStore_whenOuterTransactionRollback_shouldNotRefreshCache() {
        insertStore("S008", "事务回滚门店");
        StoreSaveReqVO reqVO = buildUpdateReq("S008", "事务回滚门店已更新");

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            storeService.updateStore(reqVO);
            status.setRollbackOnly();
        });

        assertEquals("事务回滚门店", storeMapper.selectById("S008").getStoreName());
        verify(storePlatformCacheService, org.mockito.Mockito.never()).syncStorePlatformInfoToRedis();
    }

    @Test
    void updateStore_whenSpaceHasZeroColdStorageArea_shouldKeepSpaceRecord() {
        insertStore("S002", "零冷库门店");

        StoreSaveReqVO reqVO = buildUpdateReq("S002", "零冷库门店");
        SpaceTableDO spaceTable = new SpaceTableDO();
        spaceTable.setBuildingArea(new BigDecimal("88.00"));
        spaceTable.setColdStorageArea(BigDecimal.ZERO);
        reqVO.setSpaceTable(spaceTable);

        storeService.updateStore(reqVO);

        SpaceTableDO selected = spaceTableMapper.selectByStoreId("S002");
        assertNotNull(selected);
        assertEquals(0, new BigDecimal("88.00").compareTo(selected.getBuildingArea()));
        assertEquals(0, BigDecimal.ZERO.compareTo(selected.getColdStorageArea()));
    }

    @Test
    void updateStore_whenContactTablesIsNull_shouldKeepExistingContacts() {
        insertStore("S004", "联系人门店");
        ContactTableDO contact = new ContactTableDO();
        contact.setStoreId("S004");
        contact.setContactName("张三");
        contact.setPhone("13800000000");
        contact.setIsPrimary(1);
        contact.setStatus(1);
        contactTableMapper.insert(contact);

        StoreSaveReqVO reqVO = buildUpdateReq("S004", "联系人门店已更新");
        reqVO.setSpaceTable(null);
        reqVO.setAffiliationTable(null);
        reqVO.setStatusTable(null);
        reqVO.setFranchiseeTable(null);
        reqVO.setContactTables(null);

        storeService.updateStore(reqVO);

        List<ContactTableDO> contacts = contactTableMapper.selectListByStoreId("S004");
        assertEquals(1, contacts.size());
        assertEquals("张三", contacts.get(0).getContactName());
        assertEquals("13800000000", contacts.get(0).getPhone());
    }

    @Test
    void updateStore_whenStatusAndFranchiseeHaveDates_shouldPersistLocalDateFields() {
        insertStore("S003", "日期门店");

        StoreSaveReqVO reqVO = buildUpdateReq("S003", "日期门店");
        BusinessStatusTableDO statusTable = new BusinessStatusTableDO();
        statusTable.setCurrentStatus("NORMAL");
        statusTable.setOpenDate(LocalDate.of(2026, 4, 1));
        statusTable.setSignDate(LocalDate.of(2026, 3, 15));
        reqVO.setStatusTable(statusTable);
        FranchiseeTableDO franchiseeTable = new FranchiseeTableDO();
        franchiseeTable.setFranchiseeName("日期加盟商");
        franchiseeTable.setContractStart(LocalDate.of(2026, 5, 1));
        franchiseeTable.setContractEnd(LocalDate.of(2027, 4, 30));
        reqVO.setFranchiseeTable(franchiseeTable);

        storeService.updateStore(reqVO);

        BusinessStatusTableDO selectedStatus = statusTableMapper.selectByStoreId("S003");
        assertNotNull(selectedStatus);
        assertEquals(LocalDate.of(2026, 4, 1), selectedStatus.getOpenDate());
        assertEquals(LocalDate.of(2026, 3, 15), selectedStatus.getSignDate());
        FranchiseeTableDO selectedFranchisee = franchiseeTableMapper.selectByStoreId("S003");
        assertNotNull(selectedFranchisee);
        assertEquals(LocalDate.of(2026, 5, 1), selectedFranchisee.getContractStart());
        assertEquals(LocalDate.of(2027, 4, 30), selectedFranchisee.getContractEnd());
    }

    @Test
    void getStoreImportExcelList_shouldAggregateOneToOneChildrenForExport() {
        insertStore("S004", "导出门店");
        insertSpace("S004", "120.50", "0.00");
        insertAffiliation("S004", "DIRECT", "O2O");
        insertStatus("S004", "NORMAL", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 3, 15));
        insertFranchisee("S004", "导出加盟商", LocalDate.of(2026, 5, 1), LocalDate.of(2027, 4, 30));

        StorePageReqVO reqVO = new StorePageReqVO();
        reqVO.setPageNo(2);
        reqVO.setPageSize(20);
        List<StoreImportExcelVO> list = storeService.getStoreImportExcelList(reqVO);

        assertEquals(2, reqVO.getPageNo());
        assertEquals(20, reqVO.getPageSize());
        assertEquals(1, list.size());
        StoreImportExcelVO row = list.get(0);
        assertEquals("S004", row.getStoreId());
        assertEquals("导出门店", row.getStoreName());
        assertEquals(0, new BigDecimal("120.50").compareTo(row.getBuildingArea()));
        assertEquals(0, BigDecimal.ZERO.compareTo(row.getColdStorageArea()));
        assertEquals("DIRECT", row.getBusinessMode());
        assertEquals("O2O", row.getStoreType());
        assertEquals("NORMAL", row.getCurrentStatus());
        assertEquals(LocalDate.of(2026, 4, 1), row.getOpenDate());
        assertEquals(LocalDate.of(2026, 3, 15), row.getSignDate());
        assertEquals("导出加盟商", row.getFranchiseeName());
        assertEquals("13800000000", row.getFranchiseePhone());
        assertEquals(0, new BigDecimal("10000.00").compareTo(row.getFranchiseeFee()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(row.getSecurityDeposit()));
        assertEquals(LocalDate.of(2026, 5, 1), row.getContractStart());
        assertEquals(LocalDate.of(2027, 4, 30), row.getContractEnd());
    }

    @Test
    void importStoreList_whenFranchiseeNameBlankButOtherFieldsPresent_shouldCreateFranchisee() {
        StoreImportExcelVO row = buildImportRow("S005", "导入加盟商字段门店");
        row.setFranchiseePhone("13900000000");
        row.setFranchiseeFee(new BigDecimal("20000.00"));
        row.setSecurityDeposit(new BigDecimal("8000.00"));
        row.setContractStart(LocalDate.of(2026, 6, 1));
        row.setContractEnd(LocalDate.of(2027, 5, 31));

        storeService.importStoreList(Collections.singletonList(row), false);

        FranchiseeTableDO selected = franchiseeTableMapper.selectByStoreId("S005");
        assertNotNull(selected);
        assertEquals("13900000000", selected.getFranchiseePhone());
        assertEquals(0, new BigDecimal("20000.00").compareTo(selected.getFranchiseeFee()));
        assertEquals(0, new BigDecimal("8000.00").compareTo(selected.getSecurityDeposit()));
        assertEquals(LocalDate.of(2026, 6, 1), selected.getContractStart());
        assertEquals(LocalDate.of(2027, 5, 31), selected.getContractEnd());
    }

    @Test
    void importStoreList_whenFranchiseeNameBlankButOtherFieldsPresent_shouldUpdateFranchisee() {
        insertStore("S006", "更新加盟商字段门店");
        insertFranchisee("S006", "原加盟商", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        StoreImportExcelVO row = buildImportRow("S006", "更新加盟商字段门店");
        row.setFranchiseePhone("13700000000");
        row.setFranchiseeFee(new BigDecimal("30000.00"));
        row.setSecurityDeposit(new BigDecimal("9000.00"));
        row.setContractStart(LocalDate.of(2026, 7, 1));
        row.setContractEnd(LocalDate.of(2027, 6, 30));

        storeService.importStoreList(Collections.singletonList(row), true);

        FranchiseeTableDO selected = franchiseeTableMapper.selectByStoreId("S006");
        assertNotNull(selected);
        assertEquals("13700000000", selected.getFranchiseePhone());
        assertEquals(0, new BigDecimal("30000.00").compareTo(selected.getFranchiseeFee()));
        assertEquals(0, new BigDecimal("9000.00").compareTo(selected.getSecurityDeposit()));
        assertEquals(LocalDate.of(2026, 7, 1), selected.getContractStart());
        assertEquals(LocalDate.of(2027, 6, 30), selected.getContractEnd());
    }

    @Test
    void getAllPlatformStoresByPlatformCode_whenCacheMiss_shouldReturnDbResultWithoutSync() {
        org.mockito.Mockito.when(storePlatformCacheService.getStorePlatformListFromRedis()).thenReturn(Collections.emptyList());
        insertStore("S012", "全部门店A");
        insertStore("S013", "全部门店B");
        insertPlatform(1L, "ELE", "饿了么");
        insertPlatformTable("S012", 1L, "ELE-001", "饿了么门店A");
        insertPlatformTable("S013", 1L, "ELE-002", "饿了么门店B");

        List<StorePlatformRespVO> result = storeService.getAllPlatformStoresByPlatformCode("ELE");

        assertEquals(2, result.size());
        assertEquals("ELE-002", result.get(0).getPlatformStoreId());
        assertEquals("ELE-001", result.get(1).getPlatformStoreId());
        verify(storePlatformCacheService, org.mockito.Mockito.never()).syncStorePlatformInfoToRedis();
    }

    @Test
    void getOpenPlatformStoresByPlatformCode_whenCacheMiss_shouldReturnDbResultWithoutSync() {
        org.mockito.Mockito.when(storePlatformCacheService.getStorePlatformListFromRedis()).thenReturn(Collections.emptyList());
        insertStore("S014", "开店门店");
        insertStore("S015", "闭店门店");
        StoreDO closedStore = new StoreDO();
        closedStore.setStoreId("S015");
        closedStore.setStoreStatus(0);
        storeMapper.updateById(closedStore);
        insertPlatform(2L, "MEI", "美团");
        insertPlatformTable("S014", 2L, "MEI-001", "美团门店A");
        insertPlatformTable("S015", 2L, "MEI-002", "美团门店B");

        List<StorePlatformRespVO> result = storeService.getOpenPlatformStoresByPlatformCode("MEI");

        assertEquals(1, result.size());
        assertEquals("MEI-001", result.get(0).getPlatformStoreId());
        verify(storePlatformCacheService, org.mockito.Mockito.never()).syncStorePlatformInfoToRedis();
    }

    @Test
    void searchPlatformStoreSimpleList_shouldFilterByPlatformAndKeyword() {
        insertStore("S101", "搜索门店一");
        insertStore("S102", "普通门店");
        insertStore("S103", "搜索门店二");
        insertPlatform(11L, "ELE11", "饿了么十一号");
        insertPlatform(12L, "MEI12", "美团十二号");
        insertPlatformTable("S101", 11L, "ELE-001", "饿了么搜索门店一");
        insertPlatformTable("S102", 11L, "ELE-002", "饿了么普通门店");
        insertPlatformTable("S103", 12L, "MEI-001", "美团搜索门店二");

        List<StoreSimpleRespVO> result = storeService.searchPlatformStoreSimpleList(11L, "搜索", 1, 20);

        assertEquals(1, result.size());
        assertEquals("S101", result.get(0).getStoreId());
        assertEquals("ELE-001", result.get(0).getPlatformStoreId());
    }

    @Test
    void searchPlatformStoreSimpleList_shouldSearchByPlatformStoreId() {
        insertStore("S104", "平台编码搜索门店");
        insertPlatform(3L, "ELE3", "饿了么三号");
        insertPlatformTable("S104", 3L, "ELE-ABC-001", "平台编码搜索门店");

        List<StoreSimpleRespVO> result = storeService.searchPlatformStoreSimpleList(3L, "ABC", 1, 20);

        assertEquals(1, result.size());
        assertEquals("S104", result.get(0).getStoreId());
        assertEquals("ELE-ABC-001", result.get(0).getPlatformStoreId());
    }

    @Test
    void getPlatformStoreSimpleList_shouldReturnCachedMappingsByRequestOrder() {
        StorePlatformInfoRespVO first = new StorePlatformInfoRespVO();
        first.setPlatformId(1L);
        first.setStoreId("S201");
        first.setPlatformStoreId("ELE-001");
        first.setStoreName("缓存门店一");
        first.setStoreStatus(1);
        StorePlatformInfoRespVO second = new StorePlatformInfoRespVO();
        second.setPlatformId(1L);
        second.setStoreId("S202");
        second.setPlatformStoreId("ELE-002");
        second.setStoreName("缓存门店二");
        second.setStoreStatus(0);
        org.mockito.Mockito.when(storePlatformCacheService.getStorePlatformListFromRedis())
                .thenReturn(List.of(first, second));

        List<StoreSimpleRespVO> result = storeService.getPlatformStoreSimpleList(1L, List.of("ELE-002", "ELE-001", "ELE-404"));

        assertEquals(2, result.size());
        assertEquals("ELE-002", result.get(0).getPlatformStoreId());
        assertEquals("缓存门店二", result.get(0).getStoreName());
        assertEquals("ELE-001", result.get(1).getPlatformStoreId());
        assertEquals("缓存门店一", result.get(1).getStoreName());
    }

    @Test
    void getPlatformStoreSimpleList_whenCacheMiss_shouldLoadFromDatabase() {
        org.mockito.Mockito.when(storePlatformCacheService.getStorePlatformListFromRedis()).thenReturn(Collections.emptyList());
        insertStore("S203", "数据库门店一");
        insertStore("S204", "数据库门店二");
        insertPlatform(4L, "ELE4", "饿了么四号");
        insertPlatformTable("S203", 4L, "ELE-101", "数据库门店一");
        insertPlatformTable("S204", 4L, "ELE-102", "数据库门店二");

        List<StoreSimpleRespVO> result = storeService.getPlatformStoreSimpleList(4L, List.of(" ELE-102 ", "ELE-101"));

        assertEquals(2, result.size());
        assertEquals("S204", result.get(0).getStoreId());
        assertEquals("数据库门店二", result.get(0).getStoreName());
        assertEquals("ELE-102", result.get(0).getPlatformStoreId());
        assertEquals("S203", result.get(1).getStoreId());
        assertEquals("数据库门店一", result.get(1).getStoreName());
        assertEquals("ELE-101", result.get(1).getPlatformStoreId());
    }

    @Test
    void syncStorePlatformInfo_shouldCallCacheSyncDirectly() {
        storeService.syncStorePlatformInfo();

        verify(storePlatformCacheService).syncStorePlatformInfoToRedis();
    }

    @Test
    void createStore_shouldRefreshCacheAfterCommit() {
        StoreSaveReqVO reqVO = buildUpdateReq("S009", "新增缓存刷新门店");

        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> storeService.createStore(reqVO));

        assertNotNull(storeMapper.selectById("S009"));
        verify(storePlatformCacheService).syncStorePlatformInfoToRedis();
    }

    @Test
    void importStoreList_shouldRefreshCacheAfterCommit() {
        StoreImportExcelVO row = buildImportRow("S010", "导入缓存刷新门店");

        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> storeService.importStoreList(Collections.singletonList(row), false));

        assertNotNull(storeMapper.selectById("S010"));
        verify(storePlatformCacheService).syncStorePlatformInfoToRedis();
    }

    @Test
    void importStoreList_whenOuterTransactionRollback_shouldNotRefreshCache() {
        StoreImportExcelVO row = buildImportRow("S011", "导入事务回滚门店");

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            storeService.importStoreList(Collections.singletonList(row), false);
            status.setRollbackOnly();
        });

        assertNull(storeMapper.selectById("S011"));
        verify(storePlatformCacheService, org.mockito.Mockito.never()).syncStorePlatformInfoToRedis();
    }

    private void insertStore(String storeId, String storeName) {
        StoreDO store = new StoreDO();
        store.setStoreId(storeId);
        store.setStoreName(storeName);
        store.setRegionCode("110101");
        store.setAddress("示例地址");
        store.setArea("EAST");
        store.setStoreStatus(1);
        storeMapper.insert(store);
    }

    private void insertSpace(String storeId, String buildingArea, String coldStorageArea) {
        SpaceTableDO space = new SpaceTableDO();
        space.setStoreId(storeId);
        space.setBuildingArea(new BigDecimal(buildingArea));
        space.setColdStorageArea(new BigDecimal(coldStorageArea));
        spaceTableMapper.insert(space);
    }

    private void insertAffiliation(String storeId, String businessMode, String storeType) {
        AffiliationTableDO affiliation = new AffiliationTableDO();
        affiliation.setStoreId(storeId);
        affiliation.setBusinessMode(businessMode);
        affiliation.setStoreType(storeType);
        affiliationTableMapper.insert(affiliation);
    }

    private void insertStatus(String storeId, String currentStatus, LocalDate openDate, LocalDate signDate) {
        BusinessStatusTableDO status = new BusinessStatusTableDO();
        status.setStoreId(storeId);
        status.setCurrentStatus(currentStatus);
        status.setOpenDate(openDate);
        status.setSignDate(signDate);
        statusTableMapper.insert(status);
    }

    private void insertFranchisee(String storeId, String franchiseeName, LocalDate contractStart, LocalDate contractEnd) {
        FranchiseeTableDO franchisee = new FranchiseeTableDO();
        franchisee.setStoreId(storeId);
        franchisee.setFranchiseeName(franchiseeName);
        franchisee.setFranchiseePhone("13800000000");
        franchisee.setFranchiseeFee(new BigDecimal("10000.00"));
        franchisee.setSecurityDeposit(new BigDecimal("5000.00"));
        franchisee.setContractStart(contractStart);
        franchisee.setContractEnd(contractEnd);
        franchiseeTableMapper.insert(franchisee);
    }

    private void insertPlatform(Long platformId, String platformCode, String platformName) {
        PlatformDO platform = new PlatformDO();
        platform.setPlatformId(platformId);
        platform.setPlatformCode(platformCode);
        platform.setPlatformName(platformName);
        platform.setStatus(1);
        platform.setSortOrder(0);
        platformMapper.insert(platform);
    }

    private void insertPlatformTable(String storeId, Long platformId, String platformStoreId, String platformStoreName) {
        PlatformTableDO platformTable = new PlatformTableDO();
        platformTable.setStoreId(storeId);
        platformTable.setPlatformId(platformId);
        platformTable.setPlatformStoreId(platformStoreId);
        platformTable.setPlatformStoreName(platformStoreName);
        platformTable.setStatus(1);
        platformTableMapper.insert(platformTable);
    }

    private StoreImportExcelVO buildImportRow(String storeId, String storeName) {
        StoreImportExcelVO row = new StoreImportExcelVO();
        row.setStoreId(storeId);
        row.setStoreName(storeName);
        row.setRegionCode("110101");
        row.setAddress("示例地址");
        row.setArea("EAST");
        row.setStoreStatus(1);
        return row;
    }

    private StoreSaveReqVO buildUpdateReq(String storeId, String storeName) {
        StoreSaveReqVO reqVO = new StoreSaveReqVO();
        reqVO.setStoreId(storeId);
        reqVO.setStoreName(storeName);
        reqVO.setRegionCode("110101");
        reqVO.setAddress("示例地址");
        reqVO.setArea("EAST");
        reqVO.setStoreStatus(1);
        reqVO.setContactTables(Collections.emptyList());
        return reqVO;
    }
}
