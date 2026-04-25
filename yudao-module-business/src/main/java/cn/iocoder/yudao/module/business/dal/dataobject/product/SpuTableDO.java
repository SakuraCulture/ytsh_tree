package cn.iocoder.yudao.module.business.dal.dataobject.product;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * SPU基础分类 DO
 *
 * @author 芋道源码
 */
@TableName("product_spu_table")
@KeySequence("product_spu_table_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpuTableDO extends BaseDO {

    /**
     * SPU ID
     */
    @TableId
    private Long productSpuId;
    /**
     * SPU编码
     */
    private String productSpuCode;
    /**
     * SPU名称
     */
    private String productSpuName;
    /**
     * 品牌
     */
    private String productBrand;
    /**
     * 分类ID
     */
    private Long categoryId;
    /**
     * 产地
     */
    private String productOrigin;
    /**
     * 生产商
     */
    private String productManufacturer;
    /**
     * 规格模板
     */
    private String productSpecTemplate;
    /**
     * 商品主图URL
     */
    private String productImageUrl;
    /**
     * 商品详情图片
     */
    private String productDetailImages;
    /**
     * 商品描述
     */
    private String productDescription;
    /**
     * 状态(0下架1上架)
     */
    private Integer productSpuStatus;


}