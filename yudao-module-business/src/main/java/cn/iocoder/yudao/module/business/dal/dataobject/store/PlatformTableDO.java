package cn.iocoder.yudao.module.business.dal.dataobject.store;

import lombok.*;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 门店平台关联信息 DO
 *
 * ==============================================================
 * 【Why - 为什么要支持多平台关联】
 * ==============================================================
 *
 * 设计决策：为什么要拆出独立的平台关联表？
 *
 * 方案A（主表存储）：在 StoreDO 中存储单个 platformId
 * - 优点：查询简单，无需 JOIN
 * - 缺点：
 *   1. 一个门店可能同时在多个平台运营（美团、饿了么、抖音等）
 *   2. 单平台存储无法支持多平台场景
 *   3. 不同平台的门店 ID、费率等配置不同
 *
 * 方案B（独立表）：拆出 store_platform_table
 * - 优点：
 *   1. 支持一个门店关联多个平台
 *   2. 每个平台有独立的配置（平台门店ID、费率、结算账户等）
 *   3. 便于按平台维度统计和分析
 * - 缺点：需要 JOIN 查询（但频率较低，可接受）
 *
 * 最终选择：方案B
 * - 业务上门店确实需要多平台运营
 * - 不同平台有不同的门店 ID 和配置
 * - 支持平台维度的数据统计和分析
 *
 * ==============================================================
 * 【What - 这个表存储什么】
 * ==============================================================
 * 存储门店与外部平台的关联关系：
 * - 关联的平台信息
 * - 平台分配的门店ID（platformStoreId）
 * - 平台费率（佣金率）
 * - 结算账户信息
 *
 * ==============================================================
 * 【Constraints - 约束条件】
 * ==============================================================
 * - storePlatformId：自增主键
 * - storeId：外键，关联 store_table.storeId
 * - platformId：外键，关联 platform_table.platformId
 * - platformStoreId：平台分配的门店ID，用于与平台对接
 * - commissionRate：佣金率，使用 BigDecimal，百分比格式（如 0.15 表示 15%）
 *
 * ==============================================================
 * 【Pitfalls - 已知陷阱与教训】
 * ==============================================================
 * - 【陷阱】跨平台 ID 冲突问题
 *   → 不同平台的 storeId 可能相同，但实际是不同门店
 *   → 必须使用 (storeId + platformId) 组合唯一标识
 * - 【陷阱】platformStoreId 为空的门店无法与平台对接
 *   → 门店状态变为正常前必须先获取 platformStoreId
 * - 【教训2024-02】未校验 platformId 有效性
 *   → 问题：平台删除后，关联数据仍存在，导致查询报错
 *   → 修复：删除平台时级联删除关联，或设置软删除
 * - 【风险】commissionRate 与平台实际费率不一致
 *   → 建议：定期同步平台费率数据
 *
 * @author 彼岸花
 * @see StoreDO
 * @see PlatformDO
 */
@TableName("store_platform_table")
@KeySequence("store_platform_table_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformTableDO extends BaseDO {

    /**
     * 平台关联ID
     *
     * 【What】数据库自增主键，唯一标识一条平台关联记录
     */
    @TableId
    private Long storePlatformId;

    /**
     * 门店ID
     *
     * 【What】外键，关联 store_table.storeId
     *
     * 【Constraints】
     * - 非空
     * - 关联 store_table.storeId
     */
    private String storeId;

    /**
     * 平台ID
     *
     * 【What】外键，关联 platform_table.platformId
     *
     * 【Why - 为什么要冗余存储 platformId】
     * - 便于按平台维度查询
     * - 避免 JOIN platform_table 即可获取平台信息
     *
     * 【Constraints】
     * - 非空
     * - 关联 platform_table.platformId
     *
     * 【Pitfalls】
     * - 【风险】关联的平台被删除后，此记录成为孤儿数据
     *   → 建议：设置软删除或级联删除
     */
    private Long platformId;

    /**
     * 平台门店ID
     *
     * 【What】平台（如美团、饿了么）分配给该门店的唯一标识
     *
     * 【Why - 为什么要存储平台门店ID】
     * - 与外部平台对接时需要使用平台分配的 ID
     * - 不同平台有不同的 ID 生成规则
     * - 用于接收平台推送的订单、结算等数据
     *
     * 【Constraints】
     * - 最大50字符
     * - 可为空（门店尚未在平台开店时）
     * - 格式由平台定义，可能包含字母和数字
     *
     * 【Pitfalls】
     * - 【陷阱】platformStoreId 为空时，无法与平台对接
     *   → 必须先获取 platformStoreId 才能正常运营
     * - 【陷阱】不同平台的 platformStoreId 可能相同
     *   → 必须结合 platformId 一起使用
     * - 【风险】platformStoreId 变更后未同步
     *   → 可能导致平台推送的数据无法匹配
     */
    private String platformStoreId;

    /**
     * 平台门店名称
     *
     * 【What】在平台上显示的门店名称
     *
     * 【Why - 为什么要单独存储】
     * - 平台显示名称可能与实际名称不同
     * - 便于在系统中展示平台上的名称
     *
     * 【Constraints】
     * - 最大100字符
     * - 可为空
     */
    private String platformStoreName;

    /**
     * 代理类型
     *
     * 【What】门店与平台的合作模式
     *
     * 【Constraints】
     * - 直连（DIRECT）：门店直接与平台合作
     * - 代理（AGENCY）：通过代理商与平台合作
     *
     * 【Pitfalls】
     * - 【风险】代理模式下费率计算可能更复杂
     */
    private String agentType;

    /**
     * 佣金率
     *
     * 【What】平台收取的佣金比例
     *
     * 【Why - 为什么要存储佣金率】
     * - 用于财务结算和对账
     * - 不同门店、不同平台可能有不同的费率
     *
     * 【Constraints】
     * - 使用 BigDecimal 类型，精度4位小数
     * - 格式：0.1500 表示 15%
     * - 可为空
     *
     * 【Pitfalls】
     * - 【风险】佣金率与平台实际费率不一致
     *   → 建议：定期同步平台费率数据
     * - 【边界】佣金率可能为0（特殊合作）
     */
    private BigDecimal commissionRate;

    /**
     * 结算账户
     *
     * 【What】门店在该平台的结算账户信息
     *
     * 【Why - 为什么要存储结算账户】
     * - 用于接收平台的结算款项
     * - 便于财务对账
     *
     * 【Constraints】
     * - 最大100字符
     * - 可为空
     * - 格式由平台定义
     *
     * 【Pitfalls】
     * - 【风险】账户信息变更后未及时更新
     *   → 可能导致结算款项无法到账
     */
    private String settlementAccount;

    /**
     * 状态
     *
     * 【What】标识该平台关联的可用状态
     *
     * 【Constraints】
     * - 0 - 停用（门店已从平台下线）
     * - 1 - 启用（正常运营）
     * - 默认值为 1
     *
     * 【Pitfalls】
     * - 【风险】停用后平台推送的数据如何处理？
     * - 【风险】状态变更未同步到平台
     */
    private Integer status;

}
