package cn.iocoder.yudao.module.ele.service.validator;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import org.springframework.stereotype.Component;

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
        String resolvedStoreId = firstNonBlank(normalizedStoreId, normalizedLocalStoreId, normalizedUpstreamStoreCode);
        String resolvedErpStoreCode = firstNonBlank(normalizedErpStoreCode, resolvedPlatformStoreId);

        if (hasConflict(resolvedPlatformStoreId, normalizedPlatformStoreId, normalizedErpStoreCode,
                normalizedLocalPlatformStoreId)
                || hasConflict(resolvedMerchantCode, normalizedMerchantCode, normalizedLocalMerchantCode,
                normalizedUpstreamMerchantCode)
                || hasConflict(resolvedStoreId, normalizedStoreId, normalizedLocalStoreId, normalizedUpstreamStoreCode)) {
            return StoreIdentityValidationResult.reject(REASON_CODE_STORE_IDENTITY_MISMATCH,
                    resolvedPlatformStoreId, resolvedMerchantCode, resolvedErpStoreCode, resolvedStoreId);
        }
        if (!isCompleteIdentity(resolvedPlatformStoreId, resolvedMerchantCode, resolvedStoreId)) {
            return StoreIdentityValidationResult.reject(REASON_CODE_STORE_IDENTITY_MISMATCH,
                    resolvedPlatformStoreId, resolvedMerchantCode, resolvedErpStoreCode, resolvedStoreId);
        }

        boolean requiresBackfill = StrUtil.isBlank(normalizedPlatformStoreId)
                || StrUtil.isBlank(normalizedMerchantCode)
                || StrUtil.isBlank(normalizedErpStoreCode)
                || StrUtil.isBlank(normalizedStoreId);
        if (requiresBackfill) {
            if (isCompleteIdentity(resolvedPlatformStoreId, resolvedMerchantCode, resolvedStoreId)) {
                return StoreIdentityValidationResult.backfill(resolvedPlatformStoreId, resolvedMerchantCode,
                        resolvedErpStoreCode, resolvedStoreId);
            }
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
