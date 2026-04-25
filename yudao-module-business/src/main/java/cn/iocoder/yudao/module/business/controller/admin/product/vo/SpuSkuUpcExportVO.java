package cn.iocoder.yudao.module.business.controller.admin.product.vo;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - SPU/SKU/UPC导出数据 VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ExcelIgnoreUnannotated
public class SpuSkuUpcExportVO {

    @ExcelProperty("SPU ID")
    @Schema(description = "SPU ID")
    private Long productSpuId;

    @ExcelProperty("SPU编码")
    @Schema(description = "SPU编码")
    private String productSpuCode;

    @ExcelProperty("SPU名称")
    @Schema(description = "SPU名称")
    private String productSpuName;

    @ExcelProperty("品牌")
    @Schema(description = "品牌")
    private String productBrand;

    @ExcelProperty("分类ID")
    @Schema(description = "分类ID")
    private Long categoryId;

    @ExcelProperty("产地")
    @Schema(description = "产地")
    private String productOrigin;

    @ExcelProperty("生产商")
    @Schema(description = "生产商")
    private String productManufacturer;

    @ExcelProperty("规格模板")
    @Schema(description = "规格模板")
    private String productSpecTemplate;

    @ExcelProperty("SPU状态")
    @Schema(description = "SPU状态(0下架1上架)")
    private Integer productSpuStatus;

    @ExcelProperty("创建时间")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @ExcelProperty("SKU编码")
    @Schema(description = "SKU编码")
    private String productSkuCode;

    @ExcelProperty("SKU名称")
    @Schema(description = "SKU名称")
    private String productSkuName;

    @ExcelProperty("EAN码")
    @Schema(description = "EAN码(13位)")
    private String productSkuEan;

    @ExcelProperty("重量")
    @Schema(description = "重量")
    private BigDecimal productWeight;

    @ExcelProperty("重量单位")
    @Schema(description = "重量单位")
    private String productWeightUnit;

    @ExcelProperty("长度(cm)")
    @Schema(description = "长度(cm)")
    private BigDecimal productLength;

    @ExcelProperty("宽度(cm)")
    @Schema(description = "宽度(cm)")
    private BigDecimal productWidth;

    @ExcelProperty("高度(cm)")
    @Schema(description = "高度(cm)")
    private BigDecimal productHeight;

    @ExcelProperty("成本价")
    @Schema(description = "基准成本价")
    private BigDecimal productCostPrice;

    @ExcelProperty("零售价")
    @Schema(description = "基准零售价")
    private BigDecimal productRetailPrice;

    @ExcelProperty("SKU主图URL")
    @Schema(description = "SKU主图URL")
    private String skuImageUrl;

    @ExcelProperty("SKU状态")
    @Schema(description = "SKU状态(0下架1上架)")
    private Integer productSkuStatus;

    @ExcelProperty("UPC码类型")
    @Schema(description = "UPC码类型(UPC-A/EAN-13/CODE128)")
    private String productUpcType;

    @ExcelProperty("UPC码值")
    @Schema(description = "UPC码值")
    private String productUpcValue;

    @ExcelProperty("是否主码")
    @Schema(description = "是否主码(0否1是)")
    private Integer productUpcIsPrimary;

    @ExcelProperty("UPC状态")
    @Schema(description = "UPC状态(0禁用1启用)")
    private Integer productUpcStatus;
}