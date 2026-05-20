package cn.iocoder.yudao.module.ele.service.validator;

import lombok.Getter;

@Getter
public class StoreIdentityValidationResult {

    public enum Decision {
        PASS,
        BACKFILL,
        REJECT
    }

    private final Decision decision;
    private final String reasonCode;
    private final String platformStoreId;
    private final String merchantCode;
    private final String erpStoreCode;
    private final String storeId;

    private StoreIdentityValidationResult(Decision decision, String reasonCode, String platformStoreId,
                                          String merchantCode, String erpStoreCode, String storeId) {
        this.decision = decision;
        this.reasonCode = reasonCode;
        this.platformStoreId = platformStoreId;
        this.merchantCode = merchantCode;
        this.erpStoreCode = erpStoreCode;
        this.storeId = storeId;
    }

    public static StoreIdentityValidationResult pass(String platformStoreId, String merchantCode,
                                                     String erpStoreCode, String storeId) {
        return new StoreIdentityValidationResult(Decision.PASS, null, platformStoreId, merchantCode, erpStoreCode, storeId);
    }

    public static StoreIdentityValidationResult backfill(String platformStoreId, String merchantCode,
                                                         String erpStoreCode, String storeId) {
        return new StoreIdentityValidationResult(Decision.BACKFILL, null, platformStoreId, merchantCode, erpStoreCode, storeId);
    }

    public static StoreIdentityValidationResult reject(String reasonCode, String platformStoreId,
                                                       String merchantCode, String erpStoreCode, String storeId) {
        return new StoreIdentityValidationResult(Decision.REJECT, reasonCode, platformStoreId, merchantCode, erpStoreCode, storeId);
    }
}
