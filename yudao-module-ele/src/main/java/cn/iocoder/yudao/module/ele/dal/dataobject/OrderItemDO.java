package cn.iocoder.yudao.module.ele.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单商品项表实体类
 * 表名: order_item_table
 */
@TableName("order_item_table")
@Data
public class OrderItemDO {

    /** 子订单ID */
    private String subOrderId;

    /** 订单ID(关联order_table.order_id) */
    private String orderId;

    /** 商品SKU编码 */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 条形码 */
    private String barcode;

    /** 规格描述 */
    private String specification;

    /** 重量(kg) */
    private BigDecimal weight;

    /** 总重量(kg) */
    private BigDecimal totalWeight;

    /** 购买数量 */
    private Integer buyAmount;

    /** 单价(元) */
    private BigDecimal price;

    /** 小计金额(元) */
    private BigDecimal totalFee;

    /** 实付金额(元) */
    private BigDecimal payFee;

    /** 商品类型(0-普通/1-组合/2-赠品)，对应翱象goods_type:0单品->0普通,3组合品->1组合 */
    private Integer productType;

    /** 数量（翱象num字段） */
    private Integer num;

    /** ERP门店编码 */
    private String erpStoreCode;

    /** 创建者 */
    private String creator;

    /** 创建时间 */
    private Long createTime;

    /** 更新人 */
    private String updater;

    /** 更新时间 */
    private Long updateTime;

    /** 是否删除 0-否 1-是 */
    private Boolean deleted;
}