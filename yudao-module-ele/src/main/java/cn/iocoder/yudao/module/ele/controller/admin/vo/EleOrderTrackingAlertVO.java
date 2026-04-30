package cn.iocoder.yudao.module.ele.controller.admin.vo;

import lombok.Data;

@Data
public class EleOrderTrackingAlertVO {
    /** 主键ID */
    private Long id;

    /** 订单号 */
    private String orderId;

    /** 平台门店ID */
    private String platformStoreId;

    /** 外部门店编码 */
    private String erpStoreCode;

    /** 当前订单状态 */
    private Integer orderStatus;

    /** 告警级别: WARNING-3天警告, CRITICAL-5天严重 */
    private String alertLevel;

    /** 订单创建时间（秒级时间戳） */
    private Long createTime;

    /** 订单创建时间（格式化字符串） */
    private String createTimeStr;

    /** 已过去天数 */
    private Integer daysElapsed;

    /** 备注 */
    private String remark;
}
