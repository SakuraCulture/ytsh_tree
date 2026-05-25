package cn.iocoder.yudao.module.business.dal.dataobject.store;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 门店商品 DO
 *
 * @author 彼岸花
 */
@TableName("store_product_table")
@KeySequence("store_product_table_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreProductDO extends BaseDO {

    /**
     * 门店商品ID
     */
    @TableId(type = IdType.INPUT)
    private String storeProductId;
    /**
     * 门店ID
     */
    private String storeId;
    /**
     * SKU ID
     */
    private String productSkuId;
    /**
     * 归属
     */
    private String storeProductOwnership;
    /**
     * POS状态
     */
    private String storeProductPosStatus;
    /**
     * 价格
     */
    private BigDecimal storeProductPrice;
    /**
     * 是否启用(0否1是)
     */
    private Integer storeProductIsActive;
    /**
     * 首次上架时间
     */
    private LocalDate storeProductFirstDate;
    /**
     * 上架时间
     */
    private LocalDateTime storeProductShelfTime;
    /**
     * 商品来源(1门店 0地采)
     */
    private Integer goodsSource;

}