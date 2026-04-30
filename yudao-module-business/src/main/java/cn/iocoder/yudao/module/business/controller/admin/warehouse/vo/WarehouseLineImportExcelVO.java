package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseLineImportExcelVO {

    @ExcelProperty("仓库ID")
    private String warehouseId;

    @ExcelProperty("仓库名称")
    private String warehouseName;

    @ExcelProperty("线路编码")
    private String lineCode;

    @ExcelProperty("线路名称")
    private String lineName;

    @ExcelProperty("下单星期")
    private String orderWeekdays;

    @ExcelProperty("线路状态")
    private Integer lineStatus;

    @ExcelProperty("门店ID")
    private String storeId;

    @ExcelProperty("门店名称")
    private String storeName;

    @ExcelProperty("排序")
    private Integer sortNo;

    @ExcelProperty("备注")
    private String remark;
}
