package cn.iocoder.yudao.module.ele.service.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EleStoreGoodsQueryReqBO {

    private String merchantCode;
    private String erpStoreCode;
    private List<String> skuCodeList;
    private Integer pageNo;
    private Integer pageSize;
}
