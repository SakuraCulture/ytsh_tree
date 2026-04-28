package cn.iocoder.yudao.module.ele.service.bo;

import lombok.Data;

@Data
public class EleStoreGoodsPageSyncResult {

    private Integer pageNo;
    private Integer pageSize;
    private Integer total;
    private Integer syncCount;
    private Integer successCount;
    private Integer failCount;
    private Integer governanceCount;
    private Integer shadowCount;
}
