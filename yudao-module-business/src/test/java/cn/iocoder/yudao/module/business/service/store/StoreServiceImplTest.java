package cn.iocoder.yudao.module.business.service.store;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreImportExcelVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreSaveReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.AffiliationTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.BusinessStatusTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.ContactTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.FranchiseeTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.SpaceTableDO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreDO;
import cn.iocoder.yudao.module.business.dal.mysql.store.AffiliationTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.BusinessStatusTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.ContactTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.FranchiseeTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.SpaceTableMapper;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

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
