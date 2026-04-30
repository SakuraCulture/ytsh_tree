package cn.iocoder.yudao.module.business.service.store;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreSupplyLineRespVO;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(StoreServiceImpl.class)
class StoreSupplyLineQueryTest extends BaseDbUnitTest {

    @Resource
    private StoreService storeService;
    @Resource
    private StoreMapper storeMapper;
    @Resource
    private WarehouseMapper warehouseMapper;
    @Resource
    private WarehouseStoreSupplyMapper warehouseStoreSupplyMapper;
    @Resource
    private WarehouseLineMapper warehouseLineMapper;
    @Resource
    private WarehouseLineStoreMapper warehouseLineStoreMapper;

    @MockBean
    private StorePlatformCacheService storePlatformCacheService;

    @Test
    void getStoreSupplyLineSummary_shouldReturnPrimaryWarehouseAndLines() {
        insertStore();
        insertWarehouse("W001", "默认仓");
        insertSupply("W001", 1);
        Long lineId = insertLine("W001", "L001", "水果线", "1,3,5", 1);
        bindStore(lineId, 1);

        StoreSupplyLineRespVO respVO = storeService.getStoreSupplyLineSummary("S001");

        assertEquals("S001", respVO.getStoreId());
        assertEquals(1, respVO.getSupplies().size());
        assertEquals(1, respVO.getLines().size());
        assertEquals("W001", respVO.getPrimaryWarehouseId());
        assertEquals("1,3,5", respVO.getLines().get(0).getOrderWeekdays());
    }

    @Test
    void getStoreSupplyLineSummary_shouldOrderLinesBySortNoThenId() {
        insertStore();
        insertWarehouse("W001", "默认仓");
        insertSupply("W001", 1);
        Long secondLineId = insertLine("W001", "L002", "第二条线", "2,4,6", 1);
        Long firstLineId = insertLine("W001", "L001", "第一条线", "1,3,5", 1);
        bindStore(secondLineId, 2);
        bindStore(firstLineId, 1);

        StoreSupplyLineRespVO respVO = storeService.getStoreSupplyLineSummary("S001");

        assertEquals(2, respVO.getLines().size());
        assertEquals(firstLineId, respVO.getLines().get(0).getLineId());
        assertEquals(1, respVO.getLines().get(0).getSortNo());
        assertEquals(secondLineId, respVO.getLines().get(1).getLineId());
        assertEquals(2, respVO.getLines().get(1).getSortNo());
    }

    private void insertStore() {
        StoreDO store = new StoreDO();
        store.setStoreId("S001");
        store.setStoreName("示例门店");
        store.setRegionCode("110101");
        store.setAddress("示例地址");
        store.setArea("EAST");
        store.setStoreStatus(1);
        storeMapper.insert(store);
    }

    private void insertWarehouse(String warehouseId, String warehouseName) {
        WarehouseDO warehouse = new WarehouseDO();
        warehouse.setWarehouseId(warehouseId);
        warehouse.setWarehouseName(warehouseName);
        warehouse.setWarehouseStatus(1);
        warehouseMapper.insert(warehouse);
    }

    private void insertSupply(String warehouseId, Integer isPrimary) {
        WarehouseStoreSupplyDO supply = new WarehouseStoreSupplyDO();
        supply.setWarehouseId(warehouseId);
        supply.setStoreId("S001");
        supply.setIsPrimary(isPrimary);
        supply.setSupplyStatus(1);
        warehouseStoreSupplyMapper.insert(supply);
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

    private void bindStore(Long lineId, Integer sortNo) {
        WarehouseLineStoreDO relation = new WarehouseLineStoreDO();
        relation.setLineId(lineId);
        relation.setStoreId("S001");
        relation.setSortNo(sortNo);
        warehouseLineStoreMapper.insert(relation);
    }
}
