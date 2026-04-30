package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import lombok.Data;

import java.util.List;

@Data
public class StoreSupplyLineRespVO {

    private String storeId;
    private String storeName;
    private String primaryWarehouseId;
    private String primaryWarehouseName;
    private List<SupplyItem> supplies;
    private List<LineItem> lines;

    @Data
    public static class SupplyItem {
        private String warehouseId;
        private String warehouseName;
        private Integer isPrimary;
        private Integer supplyStatus;
        private String remark;
    }

    @Data
    public static class LineItem {
        private Long lineId;
        private String warehouseId;
        private String warehouseName;
        private String lineCode;
        private String lineName;
        private String orderWeekdays;
        private Integer lineStatus;
        private Integer sortNo;
    }
}
