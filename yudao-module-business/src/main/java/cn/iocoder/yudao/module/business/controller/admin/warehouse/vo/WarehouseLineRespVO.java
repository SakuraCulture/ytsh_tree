package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 线路 Response VO")
@Data
@ExcelIgnoreUnannotated
public class WarehouseLineRespVO {

    @Schema(description = "线路ID")
    @ExcelProperty("线路ID")
    private Long lineId;

    @Schema(description = "仓库ID")
    @ExcelProperty("仓库ID")
    private String warehouseId;

    @Schema(description = "仓库名称")
    @ExcelProperty("仓库名称")
    private String warehouseName;

    @Schema(description = "线路编码")
    @ExcelProperty("线路编码")
    private String lineCode;

    @Schema(description = "线路名称")
    @ExcelProperty("线路名称")
    private String lineName;

    @Schema(description = "下单星期")
    private List<Integer> orderWeekdays;

    @Schema(description = "线路状态")
    @ExcelProperty("线路状态")
    private Integer lineStatus;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "参与门店数")
    @ExcelProperty("参与门店数")
    private Integer storeCount;

    @Schema(description = "参与门店ID列表")
    private List<String> storeIds;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
}
