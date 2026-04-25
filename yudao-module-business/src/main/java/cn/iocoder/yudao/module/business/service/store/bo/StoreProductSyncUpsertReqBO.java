package cn.iocoder.yudao.module.business.service.store.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreProductSyncUpsertReqBO {

    @NotBlank(message = "门店ID不能为空")
    private String storeId;

    @NotBlank(message = "商品SKU ID不能为空")
    private String productSkuId;

    private String storeProductOwnership;

    private String storeProductPosStatus;

    private BigDecimal storeProductPrice;

    private Integer storeProductIsActive;

    private LocalDate storeProductFirstDate;

    private LocalDateTime storeProductShelfTime;
}
