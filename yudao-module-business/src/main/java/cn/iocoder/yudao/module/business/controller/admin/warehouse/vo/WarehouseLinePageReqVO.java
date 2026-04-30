package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 线路分页 Request VO")
@Data
public class WarehouseLinePageReqVO extends PageParam {

    @Schema(description = "仓库ID")
    private String warehouseId;

    @Schema(description = "线路编码")
    private String lineCode;

    @Schema(description = "线路名称")
    private String lineName;

    @Schema(description = "线路状态")
    private Integer lineStatus;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;
}
