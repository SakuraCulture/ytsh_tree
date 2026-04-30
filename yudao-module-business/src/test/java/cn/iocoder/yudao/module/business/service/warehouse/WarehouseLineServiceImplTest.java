package cn.iocoder.yudao.module.business.service.warehouse;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseLineImportExcelVO;
import cn.iocoder.yudao.module.business.controller.admin.warehouse.vo.WarehouseLineSaveReqVO;
import cn.iocoder.yudao.module.business.dal.dataobject.store.StoreDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseLineDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseLineStoreDO;
import cn.iocoder.yudao.module.business.dal.dataobject.warehouse.WarehouseStoreSupplyDO;
import cn.iocoder.yudao.module.business.dal.mysql.store.StoreMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseLineMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseLineStoreMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseMapper;
import cn.iocoder.yudao.module.business.dal.mysql.warehouse.WarehouseStoreSupplyMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.util.Arrays;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.business.enums.ErrorCodeConstants.WAREHOUSE_LINE_WEEKDAY_CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Import({WarehouseLineServiceImpl.class, WarehouseStoreSupplyServiceImpl.class})
class WarehouseLineServiceImplTest extends BaseDbUnitTest {

    @Resource
    private WarehouseLineService warehouseLineService;
    @Resource
    private WarehouseLineMapper warehouseLineMapper;
    @Resource
    private WarehouseLineStoreMapper warehouseLineStoreMapper;
    @Resource
    private WarehouseStoreSupplyMapper warehouseStoreSupplyMapper;
    @Resource
    private WarehouseMapper warehouseMapper;
    @Resource
    private StoreMapper storeMapper;

    @Test
    void createWarehouseLine_whenStoreConflictsOnSameWarehouseAndWeekday_shouldReject() {
        insertWarehouse("W001", "默认仓");
        insertStore("S001", "示例门店");
        insertSupply("W001", "S001");
        Long existingLineId = insertLine("W001", "L001", "水果线", "1,3,5", 1);
        bindStore(existingLineId, "S001", 1);

        WarehouseLineSaveReqVO reqVO = new WarehouseLineSaveReqVO();
        reqVO.setWarehouseId("W001");
        reqVO.setLineCode("L002");
        reqVO.setLineName("面包线");
        reqVO.setOrderWeekdays(Arrays.asList(3, 6));
        reqVO.setLineStatus(1);
        reqVO.setStoreIds(Arrays.asList("S001"));

        assertServiceException(() -> warehouseLineService.createWarehouseLine(reqVO), WAREHOUSE_LINE_WEEKDAY_CONFLICT);
    }

    @Test
    void createWarehouseLine_whenStoreEligibleAndNoOverlap_shouldPersistLineAndStores() {
        insertWarehouse("W001", "默认仓");
        insertStore("S001", "示例门店");
        insertSupply("W001", "S001");

        WarehouseLineSaveReqVO reqVO = new WarehouseLineSaveReqVO();
        reqVO.setWarehouseId("W001");
        reqVO.setLineCode("L001");
        reqVO.setLineName("水果线");
        reqVO.setOrderWeekdays(Arrays.asList(1, 3, 5));
        reqVO.setLineStatus(1);
        reqVO.setStoreIds(Arrays.asList("S001"));

        Long lineId = warehouseLineService.createWarehouseLine(reqVO);

        WarehouseLineDO line = warehouseLineMapper.selectById(lineId);
        assertEquals("1,3,5", line.getOrderWeekdays());
        assertEquals(1, warehouseLineStoreMapper.selectListByLineId(lineId).size());
    }

    @Test
    void createWarehouseLine_whenLineDisabledAndWeekdayOverlaps_shouldAllowPersist() {
        insertWarehouse("W001", "默认仓");
        insertStore("S001", "示例门店");
        insertSupply("W001", "S001");
        Long existingLineId = insertLine("W001", "L001", "水果线", "1,3,5", 1);
        bindStore(existingLineId, "S001", 1);

        WarehouseLineSaveReqVO reqVO = new WarehouseLineSaveReqVO();
        reqVO.setWarehouseId("W001");
        reqVO.setLineCode("L002");
        reqVO.setLineName("停用面包线");
        reqVO.setOrderWeekdays(Arrays.asList(3, 6));
        reqVO.setLineStatus(0);
        reqVO.setStoreIds(Arrays.asList("S001"));

        Long lineId = warehouseLineService.createWarehouseLine(reqVO);

        WarehouseLineDO line = warehouseLineMapper.selectById(lineId);
        assertEquals(0, line.getLineStatus());
        assertEquals("3,6", line.getOrderWeekdays());
    }

    @Test
    void importWarehouseLineList_whenStoreNotEligible_shouldCollectFailure() {
        insertWarehouse("W001", "默认仓");
        insertStore("S001", "示例门店");

        WarehouseLineImportExcelVO row = new WarehouseLineImportExcelVO();
        row.setWarehouseId("W001");
        row.setLineCode("L001");
        row.setLineName("水果线");
        row.setOrderWeekdays("1,3,5");
        row.setStoreId("S001");

        var resp = warehouseLineService.importWarehouseLineList(java.util.List.of(row), false);

        assertEquals(1, resp.getFailureCount());
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

    private void insertSupply(String warehouseId, String storeId) {
        WarehouseStoreSupplyDO relation = new WarehouseStoreSupplyDO();
        relation.setWarehouseId(warehouseId);
        relation.setStoreId(storeId);
        relation.setIsPrimary(1);
        relation.setSupplyStatus(1);
        warehouseStoreSupplyMapper.insert(relation);
    }

    private Long insertLine(String warehouseId, String lineCode, String lineName, String orderWeekdays, Integer lineStatus) {
        WarehouseLineDO line = new WarehouseLineDO();
        line.setWarehouseId(warehouseId);
        line.setLineCode(lineCode);
        line.setLineName(lineName);
        line.setOrderWeekdays(orderWeekdays);
        line.setLineStatus(lineStatus);
        warehouseLineMapper.insert(line);
        return line.getLineId();
    }

    private void bindStore(Long lineId, String storeId, Integer sortNo) {
        WarehouseLineStoreDO relation = new WarehouseLineStoreDO();
        relation.setLineId(lineId);
        relation.setStoreId(storeId);
        relation.setSortNo(sortNo);
        warehouseLineStoreMapper.insert(relation);
    }
}
