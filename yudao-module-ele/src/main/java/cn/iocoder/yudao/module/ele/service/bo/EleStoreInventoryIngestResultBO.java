package cn.iocoder.yudao.module.ele.service.bo;

import lombok.Data;

@Data
public class EleStoreInventoryIngestResultBO {

    private String persistStatus;
    private String reasonCode;
    private Long shadowId;
    private Long governanceId;
}
