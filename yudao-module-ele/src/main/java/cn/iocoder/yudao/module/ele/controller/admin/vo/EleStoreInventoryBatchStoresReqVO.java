package cn.iocoder.yudao.module.ele.controller.admin.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class EleStoreInventoryBatchStoresReqVO {

    @NotEmpty(message = "门店列表不能为空")
    private List<String> platformStoreIds;
}
