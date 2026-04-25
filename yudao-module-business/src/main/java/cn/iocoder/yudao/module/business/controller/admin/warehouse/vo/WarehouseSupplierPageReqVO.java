package cn.iocoder.yudao.module.business.controller.admin.warehouse.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 仓库供应商分页 Request VO")
@Data
public class WarehouseSupplierPageReqVO extends PageParam {

    @Schema(description = "供应商ID", example = "SUP001")
    private String supplierId;

    @Schema(description = "供应商名称", example = "示例供应商")
    private String supplierName;

    @Schema(description = "供应商分类", example = "食品")
    private String categoryName;

    @Schema(description = "负责人", example = "张三")
    private String managerName;

    @Schema(description = "电话", example = "13800000000")
    private String phone;

    @Schema(description = "供应商状态(0停用1正常)", example = "1")
    private Integer supplierStatus;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
