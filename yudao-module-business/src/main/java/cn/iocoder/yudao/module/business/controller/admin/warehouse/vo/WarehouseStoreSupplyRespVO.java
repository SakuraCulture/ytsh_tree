package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 仓库门店供货关系 Response VO")
@Data
@ExcelIgnoreUnannotated
public class WarehouseStoreSupplyRespVO {

    @Schema(description = "主键")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "仓库ID")
    @ExcelProperty("仓库ID")
    private String warehouseId;

    @Schema(description = "仓库名称")
    @ExcelProperty("仓库名称")
    private String warehouseName;

    @Schema(description = "门店ID")
    @ExcelProperty("门店ID")
    private String storeId;

    @Schema(description = "门店名称")
    @ExcelProperty("门店名称")
    private String storeName;

    @Schema(description = "是否主仓")
    @ExcelProperty("是否主仓")
    private Integer isPrimary;

    @Schema(description = "状态")
    @ExcelProperty("状态")
    private Integer supplyStatus;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}
