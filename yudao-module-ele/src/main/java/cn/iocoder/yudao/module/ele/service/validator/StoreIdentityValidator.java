package cn.iocoder.yudao.module.ele.service.validator;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StoreIdentityValidator {

    public static final String REASON_CODE_STORE_IDENTITY_MISMATCH = "STORE_IDENTITY_MISMATCH";

    public StoreIdentityValidationResult validate(String platformStoreId,
                                                  String merchantCode,
                                                  String erpStoreCode,
                                                  String storeId,
                                                  StoreIdentityInput localMapping,
                                                  String upstreamStoreCode,
                                                  String upstreamMerchantCode) {
        String normalizedPlatformStoreId = normalize(platformStoreId);
        String normalizedMerchantCode = normalize(merchantCode);
        String normalizedErpStoreCode = normalize(erpStoreCode);
        String normalizedStoreId = normalize(storeId);
        String normalizedLocalPlatformStoreId = localMapping == null ? null : normalize(localMapping.getPlatformStoreId());
        String normalizedLocalMerchantCode = localMapping == null ? null : normalize(localMapping.getMerchantCode());
        String normalizedLocalStoreId = localMapping == null ? null : normalize(localMapping.getStoreId());
        String normalizedUpstreamStoreCode = normalize(upstreamStoreCode);
        String normalizedUpstreamMerchantCode = normalize(upstreamMerchantCode);

        String resolvedPlatformStoreId = firstNonBlank(normalizedPlatformStoreId, normalizedErpStoreCode,
                normalizedLocalPlatformStoreId);
        String resolvedMerchantCode = firstNonBlank(normalizedMerchantCode, normalizedLocalMerchantCode,
                normalizedUpstreamMerchantCode);
        String resolvedStoreId = firstNonBlank(normalizedStoreId, normalizedLocalStoreId);
        String resolvedErpStoreCode = firstNonBlank(normalizedErpStoreCode, resolvedPlatformStoreId);

        // log.info("[门店身份校验] erpStoreCode={}, 输入: platformStoreId={}, merchantCode={}, storeId={}, local[platformStoreId={}, merchantCode={}, storeId={}, upstream[storeCode={}, merchantCode={}]",
        //         erpStoreCode, platformStoreId, merchantCode, storeId,
        //         localMapping != null ? localMapping.getPlatformStoreId() : "null",
        //         localMapping != null ? localMapping.getMerchantCode() : "null",
        //         localMapping != null ? localMapping.getStoreId() : "null",
        //         upstreamStoreCode, upstreamMerchantCode);
        
        // log.info("[门店身份校验] erpStoreCode={}, 标准化: platformStoreId={}, merchantCode={}, storeId={}, local[platformStoreId={}, merchantCode={}, storeId={}]",
        //         erpStoreCode, normalizedPlatformStoreId, normalizedMerchantCode, normalizedStoreId,
        //         normalizedLocalPlatformStoreId, normalizedLocalMerchantCode, normalizedLocalStoreId);
        
        // log.info("[门店身份校验] erpStoreCode={}, 解析: platformStoreId={}, merchantCode={}, storeId={}, erpStoreCode={}",
        //         erpStoreCode, resolvedPlatformStoreId, resolvedMerchantCode, resolvedStoreId, resolvedErpStoreCode);

        if (hasConflict(resolvedPlatformStoreId, normalizedPlatformStoreId, normalizedErpStoreCode,
                normalizedLocalPlatformStoreId)
                || hasConflict(resolvedMerchantCode, normalizedMerchantCode, normalizedLocalMerchantCode,
                normalizedUpstreamMerchantCode)
                || hasConflict(resolvedStoreId, normalizedStoreId, normalizedLocalStoreId)) {
            // log.warn("[门店身份校验] erpStoreCode={}, 字段冲突: resolved[platformStoreId={}, merchantCode={}, storeId={}] vs candidates",
            //         erpStoreCode, resolvedPlatformStoreId, resolvedMerchantCode, resolvedStoreId);
            // log.warn("[门店身份校验] erpStoreCode={}, platformStoreId冲突: resolved={}, candidates=[platformStoreId={}, erpStoreCode={}, localPlatformStoreId={}]",
            //         erpStoreCode, resolvedPlatformStoreId, normalizedPlatformStoreId, normalizedErpStoreCode, normalizedLocalPlatformStoreId);
            // log.warn("[门店身份校验] erpStoreCode={}, merchantCode冲突: resolved={}, candidates=[merchantCode={}, localMerchantCode={}, upstreamMerchantCode={}]",
            //         erpStoreCode, resolvedMerchantCode, normalizedMerchantCode, normalizedLocalMerchantCode, normalizedUpstreamMerchantCode);
            // log.warn("[门店身份校验] erpStoreCode={}, storeId冲突: resolved={}, candidates=[storeId={}, localStoreId={}]",
            //         erpStoreCode, resolvedStoreId, normalizedStoreId, normalizedLocalStoreId);
            return StoreIdentityValidationResult.reject(REASON_CODE_STORE_IDENTITY_MISMATCH,
                    resolvedPlatformStoreId, resolvedMerchantCode, resolvedErpStoreCode, resolvedStoreId);
        }
        if (!isCompleteIdentity(normalizedLocalPlatformStoreId, normalizedLocalMerchantCode, normalizedLocalStoreId)) {
            // log.warn("[门店身份校验] erpStoreCode={}, local映射不完整: platformStoreId={}, merchantCode={}, storeId={}",
            //         erpStoreCode, normalizedLocalPlatformStoreId, normalizedLocalMerchantCode, normalizedLocalStoreId);
            return StoreIdentityValidationResult.reject(REASON_CODE_STORE_IDENTITY_MISMATCH,
                    resolvedPlatformStoreId, resolvedMerchantCode, resolvedErpStoreCode, resolvedStoreId);
        }

        boolean requiresBackfill = StrUtil.isBlank(normalizedPlatformStoreId)
                || StrUtil.isBlank(normalizedMerchantCode)
                || StrUtil.isBlank(normalizedErpStoreCode)
                || StrUtil.isBlank(normalizedStoreId);
        if (requiresBackfill) {
            if (isCompleteIdentity(resolvedPlatformStoreId, resolvedMerchantCode, resolvedStoreId)) {
                // log.info("[门店身份校验] erpStoreCode={}, 需要回填但解析完整", erpStoreCode);
                return StoreIdentityValidationResult.backfill(resolvedPlatformStoreId, resolvedMerchantCode,
                        resolvedErpStoreCode, resolvedStoreId);
            }
            // log.warn("[门店身份校验] erpStoreCode={}, 需要回填但解析不完整", erpStoreCode);
            return StoreIdentityValidationResult.reject(REASON_CODE_STORE_IDENTITY_MISMATCH,
                    resolvedPlatformStoreId, resolvedMerchantCode, resolvedErpStoreCode, resolvedStoreId);
        }
        return StoreIdentityValidationResult.pass(resolvedPlatformStoreId, resolvedMerchantCode,
                resolvedErpStoreCode, resolvedStoreId);
    }

    private boolean isCompleteIdentity(String platformStoreId, String merchantCode, String storeId) {
        return StrUtil.isNotBlank(platformStoreId)
                && StrUtil.isNotBlank(merchantCode)
                && StrUtil.isNotBlank(storeId);
    }

    private boolean hasConflict(String canonicalValue, String... candidates) {
        if (StrUtil.isBlank(canonicalValue)) {
            return false;
        }
        for (String candidate : candidates) {
            if (StrUtil.isNotBlank(candidate) && !StrUtil.equals(canonicalValue, candidate)) {
                return true;
            }
        }
        return false;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String normalize(String value) {
        return StrUtil.trimToNull(value);
    }

    @Getter
    public static class StoreIdentityInput {

        private final String storeId;
        private final String platformStoreId;
        private final String merchantCode;

        public StoreIdentityInput(String storeId, String platformStoreId, String merchantCode) {
            this.storeId = storeId;
            this.platformStoreId = platformStoreId;
            this.merchantCode = merchantCode;
        }
    }
}
