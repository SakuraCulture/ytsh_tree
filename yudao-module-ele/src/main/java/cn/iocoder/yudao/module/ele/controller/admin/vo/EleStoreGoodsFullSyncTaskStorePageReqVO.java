package cn.iocoder.yudao.module.ele.controller.admin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 饿了么门店商品全量同步任务门店明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class EleStoreGoodsFullSyncTaskStorePageReqVO extends PageParam {

    @Schema(description = "任务 ID")
    private Long taskId;

    @Schema(description = "门店同步状态")
    private String status;

    @Schema(description = "ERP 门店编码")
    private String erpStoreCode;

    @Schema(description = "门店 ID")
    private String storeId;
}
