package cn.iocoder.yudao.module.business.dal.dataobject.store;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 门店联系人通讯录 DO
 *
 * ==============================================================
 * 【Why - 为什么要使用 1:N 关联而不是 1:1】
 * ==============================================================
 *
 * 设计决策：为什么一个门店可以有多条联系人记录？
 *
 * 方案A（1:1 关联）：一个门店只能有一个联系人
 * - 优点：查询简单
 * - 缺点：
 *   1. 一个门店通常有多个联系人（老板、店长、财务、物流等）
 *   2. 只能存储一个人，丢失其他联系人信息
 *   3. 联系人变更时需要覆盖原记录，无法保留历史
 *
 * 方案B（1:N 关联）：一个门店对应多条联系人记录
 * - 优点：
 *   1. 支持多个联系人，满足业务实际需求
 *   2. 每个联系人可设置类型和角色，便于分类管理
 *   3. 保留联系人变更历史
 * - 缺点：查询更复杂，需要处理多条记录
 *
 * 最终选择：方案B
 * - 业务上确实需要多个联系人
 * - 不同角色承担不同职责，需要区分
 * - 保留历史联系人便于追溯
 *
 * ==============================================================
 * 【What - 这个表存储什么】
 * ==============================================================
 * 存储门店的联系人信息：
 * - 联系人基本信息（姓名、电话）
 * - 联系人类型（公司/门店/供应商/物流/其他）
 * - 业务角色（运营/督导/财务/老板/经理/采购/仓库）
 * - 主联系人标识
 *
 * ==============================================================
 * 【Constraints - 约束条件】
 * ==============================================================
 * - contactId：自增主键
 * - storeId：外键，关联 store_table.storeId
 * - isPrimary：同一门店只能有一个主联系人
 *
 * ==============================================================
 * 【Pitfalls - 已知陷阱与教训】
 * ==============================================================
 * - 【教训2024-04】联系人 ID 频繁变化导致第三方关联失效
 *   → 修复：改用 diffList 增量更新算法，保留原有 ID
 * - 【陷阱】isPrimary 约束未在数据库层保证，可能存在多个主联系人
 * - 【风险】联系人离职后记录未处理，仍可收到业务通知
 * - 【教训2023-07】联系人删除后未同步清理关联，订单无法显示联系人
 *
 * @author 彼岸花
 * @see StoreDO
 */
@TableName("store_contact_table")
@KeySequence("store_contact_table_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactTableDO extends BaseDO {

    /**
     * 联系人ID
     *
     * 【What】数据库自增主键，唯一标识一条联系人记录
     *
     * 【Why - 为什么要用自增主键】
     * - 技术主键，用于数据库唯一标识
     * - 注意：同一门店的联系人可能有多个 franchiseeId（如果重名）
     * - contactId 是最可靠的唯一标识
     *
     * 【Pitfalls】
     * - 【重要】contactId 变化会导致第三方关联失效
     *   → 不要在第三方系统中存储 contactId
     *   → 使用 contactName + phone 组合作为业务标识
     */
    @TableId
    private Long contactId;

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
     * 联系人姓名
     *
     * 【What】联系人的真实姓名或常用称呼
     *
     * 【Constraints】
     * - 最大50字符
     * - 允许重名（不同门店可能有同名联系人）
     *
     * 【Pitfalls】
     * - 【边界】使用昵称/别名可能导致沟通障碍
     * - 【风险】联系人改名后历史记录不可追溯
     */
    private String contactName;

    /**
     * 联系人类型
     *
     * 【What】标识联系人属于哪个业务域
     *
     * 【Why - 为什么要区分类型】
     * - 不同类型的联系人由不同部门负责跟进
     * - 便于分类查询和管理
     *
     * 【Constraints】
     * - COMPANY - 公司内部人员
     * - STORE - 门店人员
     * - SUPPLIER - 供应商联系人
     * - LOGISTICS - 物流联系人
     * - OTHER - 其他
     *
     * 【Pitfalls】
     * - 【风险】数据库可能存储非法枚举值
     * - 【建议】使用枚举类校验，避免硬编码字符串
     */
    private String contactType;

    /**
     * 业务角色
     *
     * 【What】标识联系人承担的职责角色
     *
     * 【Why - 为什么要区分角色】
     * - 不同角色有不同的业务权限和职责
     * - 便于精准触达（找对人）
     *
     * 【Constraints】
     * - OPERATION - 运营负责人
     * - SUPERVISOR - 督导
     * - FINANCE - 财务
     * - OWNER - 门店老板
     * - MANAGER - 门店经理
     * - PROCUREMENT - 采购负责人
     * - WAREHOUSE - 仓库负责人
     *
     * 【Pitfalls】
     * - 【边界】一人多角色如何处理？→ 可以有多条记录
     */
    private String contactRole;

    /**
     * 联系电话
     *
     * 【What】联系人的电话号码，用于电话、短信、微信等联系方式
     *
     * 【Constraints】
     * - 最大20字符
     * - 支持格式：手机号、座机、400电话、微信ID等
     * - 建议格式：+86-138-0000-0000
     *
     * 【Pitfalls】
     * - 【风险】存储多种格式导致无法统一处理
     * - 【风险】号码变更后历史记录无法追溯
     * - 【隐私】手机号明文存储存在隐私风险
     */
    private String phone;

    /**
     * 是否主要联系人
     *
     * 【What】标识此联系人是否为门店的主要联系人
     *
     * 【Why - 为什么要标识主要联系人】
     * - 快速找到最关键的联系人
     * - 批量通知时优先联系主要联系人
     *
     * 【Constraints】
     * - 0 - 否
     * - 1 - 是
     * - 同一门店建议只有一个 isPrimary=1
     *
     * 【Pitfalls】
     * - 【陷阱】isPrimary=1 可能存在多个，需在 Service 层保证唯一性
     * - 【边界】删除主要联系人后，其他联系人是否自动升级？
     */
    private Integer isPrimary;

    /**
     * 状态
     *
     * 【What】标识联系人的可用状态
     *
     * 【Constraints】
     * - 0 - 禁用（已离职/停用）
     * - 1 - 启用（正常）
     * - 默认值为 1
     *
     * 【Pitfalls】
     * - 【风险】禁用后业务通知仍可能发送到该联系人
     */
    private Integer status;

    /**
     * 备注
     *
     * 【What】额外的信息备注，如特殊关系、注意事项等
     *
     * 【Constraints】
     * - 最大500字符
     * - 可为空
     */
    private String remark;

}
