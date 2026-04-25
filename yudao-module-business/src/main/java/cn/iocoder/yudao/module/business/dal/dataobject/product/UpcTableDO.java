package cn.iocoder.yudao.module.business.dal.dataobject.product;

import lombok.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

@TableName("product_upc_table")
@KeySequence("product_upc_table_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcTableDO extends BaseDO {

    @TableId
    private Long productUpcId;
    private Long productSkuId;
    private String productUpcType;
    private String productUpcValue;
    private Integer productUpcIsPrimary;
    private Integer productUpcStatus;

}
