package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 门店 Response VO")
@Data
@ExcelIgnoreUnannotated
public class StoreRespVO {

    @Schema(description = "门店编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "17246")
    @ExcelProperty("门店编码")
    private String storeId;

    @Schema(description = "门店名称", example = "王五")
    @ExcelProperty("门店名称")
    private String storeName;

    @Schema(description = "行政区划代码")
    @ExcelProperty("行政区划代码")
    private String regionCode;

    @Schema(description = "行政区划名称")
    @ExcelProperty("行政区划名称")
    private String regionName;

    @Schema(description = "详细地址")
    @ExcelProperty("详细地址")
    private String address;

    @Schema(description = "门店区域(EAST华东/NORTH华北/SOUTH华南/WEST华西/CENTRAL华中)")
    @ExcelProperty("门店区域(EAST华东/NORTH华北/SOUTH华南/WEST华西/CENTRAL华中)")
    private String area;

    @Schema(description = "状态(0停用1正常)", example = "1")
    @ExcelProperty("状态(0停用1正常)")
    private Integer storeStatus;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}