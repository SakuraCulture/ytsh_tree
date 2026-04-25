package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 仓库 Response VO")
@Data
@ExcelIgnoreUnannotated
public class WarehouseRespVO {

    @Schema(description = "仓库ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "W001")
    @ExcelProperty("仓库ID")
    private String warehouseId;

    @Schema(description = "仓库编码", example = "WH001")
    @ExcelProperty("仓库编码")
    private String warehouseCode;

    @Schema(description = "仓库名称", example = "华东一仓")
    @ExcelProperty("仓库名称")
    private String warehouseName;

    @Schema(description = "仓库类型", example = "成品仓")
    @ExcelProperty("仓库类型")
    private String warehouseType;

    @Schema(description = "行政区划代码")
    @ExcelProperty("行政区划代码")
    private String regionCode;

    @Schema(description = "详细地址")
    @ExcelProperty("详细地址")
    private String address;

    @Schema(description = "仓库状态(0停用1正常)", example = "1")
    @ExcelProperty("仓库状态")
    private Integer warehouseStatus;

    @Schema(description = "是否默认仓(0否1是)", example = "0")
    @ExcelProperty("是否默认仓")
    private Integer isDefault;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
