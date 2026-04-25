package cn.iocoder.yudao.module.business.controller.admin.store.vo;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 门店分页 Request VO")
@Data
public class StorePageReqVO extends PageParam {

    @Schema(description = "门店编码", example = "S001")
    private String storeId;

    @Schema(description = "门店名称", example = "王五")
    private String storeName;

    @Schema(description = "行政区划代码")
    private String regionCode;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "门店区域(EAST华东/NORTH华北/SOUTH华南/WEST华西/CENTRAL华中)")
    private String area;

    @Schema(description = "状态(0停用1正常)", example = "1")
    private Integer storeStatus;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}