package cn.iocoder.yudao.module.ele.service.dto;

import lombok.Data;

/**
 * 订单列表请求DTO
 * <p>
 * 用于向翱象(外部平台)请求订单列表数据的请求参数对象.
 * 支持按门店、时间范围、订单状态等条件进行筛选.
 *
 * @author 管理员
 * @since 2024-xx-xx
 */
@Data
public class OrderListReqDTO {

    /**
     * 平台门店ID
     * <p>
     * 关联的门店在系统中的唯一标识(平台门店ID)
     * <p>
     * 可选参数：如果传入此值，将自动填充merchantCode和erpStoreCode
     * <p>
     * 对应系统的platformStoreId字段
     */
    private String platformStoreId;

    /**
     * 商户编码
     * <p>
     * 商家在翱象平台的商户ID
     * <p>
     * 示例值：202406050001
     */
    private String merchantCode;

    /**
     * ERP门店编码
     * <p>
     * 商家门店在ERP系统中的门店编码
     * <p>
     * 示例值：STORE_001
     * <p>
     * <b>必填参数</b>
     */
    private String erpStoreCode;

    /**
     * 订单状态
     * <p>
     * 订单的状态筛选条件
     * <p>
     * 可选值：
     * <ul>
     *   <li>1 - 已支付</li>
     *   <li>2 - 已接单</li>
     *   <li>3 - 已拣货</li>
     *   <li>4 - 已打包</li>
     *   <li>5 - 已发货</li>
     *   <li>6 - 交易成功</li>
     *   <li>-1 - 交易关闭</li>
     *   <li>-2 - 已取消(服务端返回此状态时会跳过该订单)</li>
     * </ul>
     * <p>
     * 为空时表示查询所有状态
     */
    private Integer status;

    /**
     * 查询开始时间
     * <p>
     * 订单创建时间的查询起始点(Unix时间戳，秒级)
     * <p>
     * 示例值：1717200000 (对应2024年6月1日 00:00:00)
     * <p>
     * 为空时默认为当天0点
     */
    private Long startTime;

    /**
     * 查询结束时间
     * <p>
     * 订单创建时间的查询结束点(Unix时间戳，秒级)
     * <p>
     * 示例值：1717286399 (对应2024年6月1日 23:59:59)
     * <p>
     * 为空时默认为当前时间
     */
    private Long endTime;

    /**
     * 每页数量
     * <p>
     * 每页返回的订单数量
     * <p>
     * 默认值：20
     * <p>
     * 注意：最大支持100
     */
    private Integer pageSize = 20;

    /**
     * 游标ID
     * <p>
     * 用于分页查询的游标值
     * <p>
     * 首次请求为空，后续请求使用上次返回的scrollId
     * <p>
     * 当返回的scrollId不为空时，需要继续传入此参数获取下一页
     */
    private String scrollId;
}