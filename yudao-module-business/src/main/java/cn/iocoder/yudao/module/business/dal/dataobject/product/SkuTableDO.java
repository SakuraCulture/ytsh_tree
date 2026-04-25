package cn.iocoder.yudao.module.business.dal.dataobject.product;

import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import java.math.BigDecimal;
import java.math.BigDecimal;
import java.math.BigDecimal;
import java.math.BigDecimal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * SKU商品主数据 DO
 *
 * @author 彼岸花
 */
@TableName("product_sku_table")
@KeySequence("product_sku_table_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuTableDO extends BaseDO {

    /**
     * SKU ID
     */
    @TableId
    private Long productSkuId;
    /**
     * SKU编码
     */
    private String productSkuCode;
    /**
     * SKU名称
     */
    private String productSkuName;
    /**
     * 所属SPU
     */
    private Long productSpuId;
    /**
     * 主EAN码(13位)
     */
    private String productSkuEan;
    /**
     * 重量
     */
    private BigDecimal productWeight;
    /**
     * 重量单位
     */
    private String productWeightUnit;
    /**
     * 长度(cm)
     */
    private BigDecimal productLength;
    /**
     * 宽度(cm)
     */
    private BigDecimal productWidth;
    /**
     * 高度(cm)
     */
    private BigDecimal productHeight;
    /**
     * 基准成本价
     */
    private BigDecimal productCostPrice;
    /**
     * 基准零售价
     */
    private BigDecimal productRetailPrice;
    /**
     * SKU主图URL
     */
    private String productImageUrl;
    /**
     * 状态(0下架1上架)
     */
    private Integer productSkuStatus;

}