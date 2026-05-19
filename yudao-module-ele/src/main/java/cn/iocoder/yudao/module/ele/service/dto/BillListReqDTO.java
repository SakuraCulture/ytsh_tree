package cn.iocoder.yudao.module.ele.service.dto;

import lombok.Data;

@Data
public class BillListReqDTO {
    private Integer status;
    private String merchantCode;
    private String erpStoreCode;
    private String marketUserId;
    private String billDate;
    private Integer pageNum;
    private Integer pageSize;
}
