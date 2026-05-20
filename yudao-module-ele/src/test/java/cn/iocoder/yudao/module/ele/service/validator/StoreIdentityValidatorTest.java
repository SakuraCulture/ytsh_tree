package cn.iocoder.yudao.module.ele.service.validator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StoreIdentityValidatorTest {

    private final StoreIdentityValidator validator = new StoreIdentityValidator();

    @Test
    public void testValidate_whenAllIdentifiersConsistent_thenPass() {
        StoreIdentityValidator.StoreIdentityInput localMapping = buildLocalMapping("store-1", "platform-store-1", "merchant-1");

        StoreIdentityValidationResult result = validator.validate(
                "platform-store-1",
                "merchant-1",
                "platform-store-1",
                "store-1",
                localMapping,
                "platform-store-1",
                "merchant-1");

        assertEquals(StoreIdentityValidationResult.Decision.PASS, result.getDecision());
        assertNull(result.getReasonCode());
        assertEquals("platform-store-1", result.getPlatformStoreId());
        assertEquals("merchant-1", result.getMerchantCode());
        assertEquals("platform-store-1", result.getErpStoreCode());
        assertEquals("store-1", result.getStoreId());
    }

    @Test
    public void testValidate_whenCanonicalIdentifiersNeedBackfill_thenBackfill() {
        StoreIdentityValidator.StoreIdentityInput localMapping = buildLocalMapping("store-1", "platform-store-1", "merchant-1");

        StoreIdentityValidationResult result = validator.validate(
                null,
                null,
                null,
                null,
                localMapping,
                "platform-store-1",
                "merchant-1");

        assertEquals(StoreIdentityValidationResult.Decision.BACKFILL, result.getDecision());
        assertNull(result.getReasonCode());
        assertEquals("platform-store-1", result.getPlatformStoreId());
        assertEquals("merchant-1", result.getMerchantCode());
        assertEquals("platform-store-1", result.getErpStoreCode());
        assertEquals("store-1", result.getStoreId());
    }

    @Test
    public void testValidate_whenMerchantCodeConflicts_thenReject() {
        StoreIdentityValidator.StoreIdentityInput localMapping = buildLocalMapping("store-1", "platform-store-1", "merchant-local");

        StoreIdentityValidationResult result = validator.validate(
                "platform-store-1",
                "merchant-request",
                "platform-store-1",
                "store-1",
                localMapping,
                "platform-store-1",
                "merchant-upstream");

        assertEquals(StoreIdentityValidationResult.Decision.REJECT, result.getDecision());
        assertEquals(StoreIdentityValidator.REASON_CODE_STORE_IDENTITY_MISMATCH, result.getReasonCode());
        assertEquals("platform-store-1", result.getPlatformStoreId());
        assertEquals("merchant-request", result.getMerchantCode());
        assertEquals("platform-store-1", result.getErpStoreCode());
        assertEquals("store-1", result.getStoreId());
    }

    @Test
    public void testValidate_whenStoreIdCannotBeBackfilledCompletely_thenReject() {
        StoreIdentityValidator.StoreIdentityInput localMapping = buildLocalMapping(null, "platform-store-1", "merchant-1");

        StoreIdentityValidationResult result = validator.validate(
                null,
                null,
                null,
                null,
                localMapping,
                "platform-store-1",
                "merchant-1");

        assertEquals(StoreIdentityValidationResult.Decision.REJECT, result.getDecision());
        assertEquals(StoreIdentityValidator.REASON_CODE_STORE_IDENTITY_MISMATCH, result.getReasonCode());
        assertEquals("platform-store-1", result.getPlatformStoreId());
        assertEquals("merchant-1", result.getMerchantCode());
        assertEquals("platform-store-1", result.getErpStoreCode());
        assertNull(result.getStoreId());
    }

    @Test
    public void testValidate_whenLocalMappingMissing_thenReject() {
        StoreIdentityValidationResult result = validator.validate(
                "platform-store-1",
                "merchant-1",
                "platform-store-1",
                "store-1",
                null,
                "platform-store-1",
                "merchant-1");

        assertEquals(StoreIdentityValidationResult.Decision.REJECT, result.getDecision());
        assertEquals(StoreIdentityValidator.REASON_CODE_STORE_IDENTITY_MISMATCH, result.getReasonCode());
        assertEquals("platform-store-1", result.getPlatformStoreId());
        assertEquals("merchant-1", result.getMerchantCode());
        assertEquals("platform-store-1", result.getErpStoreCode());
        assertEquals("store-1", result.getStoreId());
    }

    @Test
    public void testValidate_whenPartialPlatformIdentifiersConflict_thenReject() {
        StoreIdentityValidator.StoreIdentityInput localMapping = buildLocalMapping("store-1", "platform-store-local", "merchant-1");

        StoreIdentityValidationResult result = validator.validate(
                "platform-store-request",
                null,
                null,
                "store-1",
                localMapping,
                "platform-store-upstream",
                "merchant-1");

        assertEquals(StoreIdentityValidationResult.Decision.REJECT, result.getDecision());
        assertEquals(StoreIdentityValidator.REASON_CODE_STORE_IDENTITY_MISMATCH, result.getReasonCode());
        assertEquals("platform-store-request", result.getPlatformStoreId());
        assertEquals("merchant-1", result.getMerchantCode());
        assertEquals("platform-store-request", result.getErpStoreCode());
        assertEquals("store-1", result.getStoreId());
    }

    private StoreIdentityValidator.StoreIdentityInput buildLocalMapping(String storeId, String platformStoreId, String merchantCode) {
        return new StoreIdentityValidator.StoreIdentityInput(storeId, platformStoreId, merchantCode);
    }
}
