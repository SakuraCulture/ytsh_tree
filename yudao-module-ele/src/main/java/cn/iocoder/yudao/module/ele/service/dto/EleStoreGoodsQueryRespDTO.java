package cn.iocoder.yudao.module.ele.service.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EleStoreGoodsQueryRespDTO {

    private String merchantCode;
    private String storeCode;
    private Integer page;
    private Integer total;
    private Integer pageSize;
    private List<GoodsItem> goodsList = new ArrayList<>();

    @Data
    public static class GoodsItem {
        private String merchantCode;
        private String storeCode;
        private String title;
        private String spuCode;
        private String mainPic;
        private String[] subPics;
        private List<SkuItem> skuList = new ArrayList<>();
    }

    @Data
    public static class SkuItem {
        private String skuCode;
        private String subSkuCode;
        private String specification;
        private Long salePrice;
        private Integer status;
    }
}
