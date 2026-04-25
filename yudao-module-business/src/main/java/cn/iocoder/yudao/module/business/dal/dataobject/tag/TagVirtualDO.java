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

@TableName("tag_virtual")
@KeySequence("tag_virtual_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagVirtualDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private String domainType;

    private String name;

    private String code;

    private String expressionJson;

    private String expressionSummary;

    private String usageScenario;

    private Integer status;

    /**
     * 逻辑删除占位键
     */
    private Long uniqueDeleted;

}
