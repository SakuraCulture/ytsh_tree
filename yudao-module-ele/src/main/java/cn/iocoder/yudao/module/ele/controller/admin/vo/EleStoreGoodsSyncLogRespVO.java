package cn.iocoder.yudao.module.ele.controller.admin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 饿了么门店商品同步日志 Response VO")
@Data
public class EleStoreGoodsSyncLogRespVO {

    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "链路 ID")
    private String traceId;

    @Schema(description = "ticket")
    private String ticket;

    @Schema(description = "接口编码")
    private String apiCode;

    @Schema(description = "接口名称")
    private String apiName;

    @Schema(description = "商家编码")
    private String merchantCode;

    @Schema(description = "ERP 门店编码")
    private String erpStoreCode;

    @Schema(description = "平台 ID")
    private Long platformId;

    @Schema(description = "门店 ID")
    private String storeId;

    @Schema(description = "平台门店编码")
    private String platformStoreId;

    @Schema(description = "SKU 编码")
    private String skuCode;

    @Schema(description = "子 SKU 编码")
    private String subSkuCode;

    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "页码")
    private Integer pageNo;

    @Schema(description = "每页条数")
    private Integer pageSize;

    @Schema(description = "数据条数")
    private Integer dataCount;

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "结果码")
    private String resultCode;

    @Schema(description = "结果消息")
    private String resultMsg;

    @Schema(description = "耗时毫秒")
    private Integer durationMs;

    @Schema(description = "请求报文")
    private String requestBody;

    @Schema(description = "响应报文")
    private String responseBody;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
