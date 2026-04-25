package cn.iocoder.yudao.module.business.dal.dataobject.category;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 商品类目表（三级树形结构） DO
 *
 * @author 彼岸花
 */
@TableName("product_category_table")
@KeySequence("product_category_table_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTableDO extends BaseDO {

    public static final Long PARENT_ID_ROOT = 0L;

    /**
     * 租户ID
     */
    private Long tenantId;
    /**
     * 类目ID
     */
    @TableId
    private Long categoryId;
    /**
     * 类目编码
     */
    private String categoryCode;
    /**
     * 类目名称
     */
    private String categoryName;
    /**
     * 父类目ID（0表示一级类目）
     */
    private Long parentId;
    /**
     * 层级（1一级/2二级/3三级）
     */
    private Integer categoryLevel;
    /**
     * 类目路径（如：1/2/3）
     */
    private String categoryPath;
    /**
     * 类目图标URL
     */
    private String categoryIcon;
    /**
     * 类目配图URL
     */
    private String categoryImage;
    /**
     * 同级排序
     */
    private Integer sortOrder;
    /**
     * 是否叶子类目（0否 1是）
     */
    private Integer isLeaf;
    /**
     * 状态（0禁用 1启用）
     */
    private Integer status;

    /**
     * 父类目名称（不存储，仅用于前端展示）
     */
    @TableField(exist = false)
    private String parentCategoryName;


}