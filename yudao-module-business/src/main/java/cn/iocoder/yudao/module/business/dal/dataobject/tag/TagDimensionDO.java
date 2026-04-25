package cn.iocoder.yudao.module.business.dal.dataobject.tag;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 标签维度 DO
 */
@TableName("tag_dimension")
@KeySequence("tag_dimension_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagDimensionDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private String domainType;

    private Long parentId;

    private Integer level;

    private String name;

    private String code;

    private Integer sort;

    private Integer status;

    private String description;

    /**
     * 软删除唯一键释放字段。未删除为 0，删除前更新为当前 id。
     */
    private Long uniqueDeleted;

}
