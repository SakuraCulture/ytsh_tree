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

@TableName("tag_value")
@KeySequence("tag_value_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagValueDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private Long dimensionId;

    private String name;

    private String code;

    private String tagMethod;

    private String dataSource;

    private String updateFrequency;

    private String logicDescription;

    private Integer sort;

    private Integer status;

    private Long uniqueDeleted;

}
