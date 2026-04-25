package cn.iocoder.yudao.module.business.dal.dataobject.store;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 门店归属架构信息 DO
 *
 * 【Why】单独存储归属架构信息的意义：
 * 1. 经营方式和门店类型属于组织架构维度的属性，与物理空间信息（面积）和经营状态（日期）是不同维度的数据
 * 2. 便于按照组织架构进行数据权限控制和报表归类
 * 3. 经营方式和门店类型可能随业务战略调整而变更，独立存储便于历史追踪
 * 4. 支持不同维度的统计分析（如按经营方式统计门店数量、按门店类型分析业务占比）
 *
 * 【What】本表负责存储门店的归属架构属性，包括经营方式和门店类型
 *
 * @author 芋道源码
 */
@TableName("store_affiliation_table")
@KeySequence("store_affiliation_table_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AffiliationTableDO extends BaseDO {

    /**
     * 归属信息ID
     *
     * 【What】归属架构记录的唯一标识
     *
     * 【Constraints】
     * - 自增主键，确保唯一性
     * - 与 storeId 组合唯一
     *
     * 【Pitfalls】
     * - 需确保与 storeId 是一对一关系，防止同一门店出现多条归属记录
     */
    @TableId
    private Long affiliationId;

    /**
     * 门店ID
     *
     * 【What】关联到 store_table.store_id，标识这条归属信息属于哪个门店
     *
     * 【Constraints】
     * - 非空，必须指向已存在的门店
     *
     * 【Pitfalls】
     * - 需与门店主表建立外键关联，删除门店前应先检查关联记录
     */
    private String storeId;

    /**
     * 经营方式
     *
     * 【Why】使用枚举值而非自由文本的原因：
     * - 经营方式是业务核心属性，需要标准化以便统计和分析
     * - 便于系统内进行条件筛选和数据校验
     * - 支持后续扩展枚举值时的平滑升级
     *
     * 【What】枚举值含义：
     * - DIRECT：直营（公司直接运营，100%控股）
     * - AGENCY：代理（通过代理商运营，公司提供品牌和供货）
     * - SELF：自营（店主自主经营，公司提供平台服务）
     * - JOINT：合伙（多方合资，共同运营）
     *
     * 【Constraints】
     * - 必须使用上述枚举值之一，不允许自定义
     * - 非空，门店必须明确其经营方式
     * - 经营方式变更建议走审批流程
     *
     * 【Pitfalls】
     * 【教训2024-04】早期未限制枚举值，曾出现 "直营店"、"直营" 等多种写法，导致统计时数据分散
     * - 建议在数据库层添加 CHECK 约束限制允许的枚举值
     * - 前端使用下拉框或单选框，而非文本输入框
     */
    private String businessMode;

    /**
     * 门店类型
     *
     * 【Why】区分线上线下门店类型的原因：
     * - 不同类型的门店涉及的业务流程不同（O2O 需要配送到家，ONLINE 纯线上）
     * - 便于按类型统计和分析各类门店的经营数据
     * - 不同类型门店可能对应不同的资质要求和合规标准
     *
     * 【What】枚举值含义：
     * - ONLINE：纯线上门店（无实体店，消费者线上下单后由仓库/配送站发货）
     * - O2O：线上线下融合门店（消费者可线上下单，也可到店消费，支持到店自提和配送到家）
     *
     * 【Constraints】
     * - 必须使用上述枚举值之一，不允许自定义
     * - 非空，门店必须明确其类型
     *
     * 【Pitfalls】
     * 【教训2023-12】曾因未区分门店类型，导致统计门店数量时将纯线上门店也计入线下门店数，造成数据失真
     * - 建议在数据库层添加 CHECK 约束限制允许的枚举值
     * - 前端使用下拉框选择
     */
    private String storeType;

}
