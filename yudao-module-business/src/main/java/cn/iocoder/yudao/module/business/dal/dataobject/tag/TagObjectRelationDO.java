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

import java.time.LocalDateTime;

@TableName("tag_object_relation")
@KeySequence("tag_object_relation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagObjectRelationDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private String domainType;

    private String objectType;

    private String objectId;

    private Long tagValueId;

    private String sourceType;

    private String sourceRef;

    private Integer status;

    private LocalDateTime effectiveTime;

    private LocalDateTime expireTime;

    private Long uniqueDeleted;

}
