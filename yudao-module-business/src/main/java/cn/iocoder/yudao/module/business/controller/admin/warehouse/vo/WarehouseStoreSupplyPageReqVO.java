package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 仓库门店供货关系分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class WarehouseStoreSupplyPageReqVO extends PageParam {

    @Schema(description = "仓库ID")
    private String warehouseId;

    @Schema(description = "门店ID")
    private String storeId;

    @Schema(description = "是否主仓")
    private Integer isPrimary;

    @Schema(description = "状态")
    private Integer supplyStatus;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;
}
