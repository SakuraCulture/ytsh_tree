package cn.iocoder.yudao.module.business.service.warehouse;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplyImportExcelVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplyPageReqVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplySaveReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseStoreSupplyDO;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseStoreSupplyMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_STORE_SUPPLY_DUPLICATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(WarehouseStoreSupplyServiceImpl.class)
class WarehouseStoreSupplyServiceImplTest extends BaseDbUnitTest {

    @Resource
    private WarehouseStoreSupplyService warehouseStoreSupplyService;
    @Resource
    private WarehouseStoreSupplyMapper warehouseStoreSupplyMapper;
    @Resource
    private WarehouseMapper warehouseMapper;
    @Resource
    private StoreMapper storeMapper;

    @Test
    void createWarehouseStoreSupply_shouldPersistPrimaryRelation() {
        insertWarehouse("W001", "默认仓");
        insertStore("S001", "示例门店");

        WarehouseStoreSupplySaveReqVO reqVO = new WarehouseStoreSupplySaveReqVO();
        reqVO.setWarehouseId("W001");
        reqVO.setStoreId("S001");
        reqVO.setIsPrimary(1);
        reqVO.setSupplyStatus(1);
        reqVO.setRemark("主仓");

        Long id = warehouseStoreSupplyService.createWarehouseStoreSupply(reqVO);

        WarehouseStoreSupplyDO relation = warehouseStoreSupplyMapper.selectById(id);
        assertEquals("W001", relation.getWarehouseId());
        assertEquals("S001", relation.getStoreId());
        assertEquals(1, relation.getIsPrimary());
    }

    @Test
    void createWarehouseStoreSupply_whenDuplicatePair_shouldReject() {
        insertWarehouse("W001", "默认仓");
        insertStore("S001", "示例门店");
        insertRelation("W001", "S001", 1, 1);

        WarehouseStoreSupplySaveReqVO reqVO = new WarehouseStoreSupplySaveReqVO();
        reqVO.setWarehouseId("W001");
        reqVO.setStoreId("S001");
        reqVO.setIsPrimary(0);
        reqVO.setSupplyStatus(1);

        assertServiceException(() -> warehouseStoreSupplyService.createWarehouseStoreSupply(reqVO), WAREHOUSE_STORE_SUPPLY_DUPLICATE);
    }

    @Test
    void updateWarehouseStoreSupply_whenSettingPrimary_shouldClearSiblingPrimaryFlag() {
        insertWarehouse("W001", "一仓");
        insertWarehouse("W002", "二仓");
        insertStore("S001", "示例门店");
        Long firstId = insertRelation("W001", "S001", 1, 1);
        Long secondId = insertRelation("W002", "S001", 0, 1);

        WarehouseStoreSupplySaveReqVO reqVO = new WarehouseStoreSupplySaveReqVO();
        reqVO.setId(secondId);
        reqVO.setWarehouseId("W002");
        reqVO.setStoreId("S001");
        reqVO.setIsPrimary(1);
        reqVO.setSupplyStatus(1);

        warehouseStoreSupplyService.updateWarehouseStoreSupply(reqVO);

        assertEquals(0, warehouseStoreSupplyMapper.selectById(firstId).getIsPrimary());
        assertEquals(1, warehouseStoreSupplyMapper.selectById(secondId).getIsPrimary());
    }

    @Test
    void getWarehouseStoreSupplyPage_shouldSupportWarehouseFilter() {
        insertWarehouse("W001", "一仓");
        insertWarehouse("W002", "二仓");
        insertStore("S001", "门店一");
        insertStore("S002", "门店二");
        insertRelation("W001", "S001", 1, 1);
        insertRelation("W002", "S002", 1, 1);

        WarehouseStoreSupplyPageReqVO reqVO = new WarehouseStoreSupplyPageReqVO();
        reqVO.setWarehouseId("W001");

        PageResult<?> page = warehouseStoreSupplyService.getWarehouseStoreSupplyPage(reqVO);

        assertEquals(1, page.getList().size());
        assertEquals("S001", ((cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseStoreSupplyRespVO) page.getList().get(0)).getStoreId());
    }

    @Test
    void importWarehouseStoreSupplyList_whenDuplicateRowsExist_shouldCollectFailures() {
        insertWarehouse("W001", "默认仓");
        insertStore("S001", "示例门店");
        insertRelation("W001", "S001", 1, 1);

        WarehouseStoreSupplyImportExcelVO row = new WarehouseStoreSupplyImportExcelVO();
        row.setWarehouseId("W001");
        row.setStoreId("S001");
        row.setIsPrimary(0);
        row.setSupplyStatus(1);

        var resp = warehouseStoreSupplyService.importWarehouseStoreSupplyList(java.util.List.of(row), false);

        assertEquals(1, resp.getFailureCount());
    }

    private Long insertRelation(String warehouseId, String storeId, Integer isPrimary, Integer supplyStatus) {
        WarehouseStoreSupplyDO relation = new WarehouseStoreSupplyDO();
        relation.setWarehouseId(warehouseId);
        relation.setStoreId(storeId);
        relation.setIsPrimary(isPrimary);
        relation.setSupplyStatus(supplyStatus);
        warehouseStoreSupplyMapper.insert(relation);
        return relation.getId();
    }

    private void insertWarehouse(String warehouseId, String warehouseName) {
        WarehouseDO warehouse = new WarehouseDO();
        warehouse.setWarehouseId(warehouseId);
        warehouse.setWarehouseCode(warehouseId);
        warehouse.setWarehouseName(warehouseName);
        warehouse.setWarehouseStatus(1);
        warehouse.setIsDefault(0);
        warehouse.setTenantId(1L);
        warehouseMapper.insert(warehouse);
    }

    private void insertStore(String storeId, String storeName) {
        StoreDO store = StoreDO.builder()
                .storeId(storeId)
                .storeName(storeName)
                .regionCode("110101")
                .address("示例地址")
                .area("EAST")
                .storeStatus(1)
                .build();
        storeMapper.insert(store);
    }
}
