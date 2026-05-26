package cn.iocoder.yudao.module.ele.service.support;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.business.service.store.StoreService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;


@Component
public class EleStorePlatformResolver {

    @Resource
    private StoreService storeService;

    public Resolved resolve(String platformStoreId, String erpStoreCode) {
        String input = StrUtil.trim(platformStoreId);
        String requestedErp = StrUtil.trim(erpStoreCode);

        if (StrUtil.isNotBlank(input)) {
            StorePlatformRespVO store = storeService.getPlatformTableByPlatformStoreId(input);
            if (store != null && StrUtil.isNotBlank(store.getPlatformStoreId())) {
                String canonical = StrUtil.trim(store.getPlatformStoreId());
                return new Resolved(canonical, canonical, store.getStoreId(), store.getSettlementAccount());
            }
        }

        if (StrUtil.isNotBlank(requestedErp)) {
            StorePlatformRespVO store = storeService.getPlatformTableByPlatformStoreId(requestedErp);
            if (store != null && StrUtil.isNotBlank(store.getPlatformStoreId())) {
                String canonical = StrUtil.trim(store.getPlatformStoreId());
                return new Resolved(canonical, canonical, store.getStoreId(), store.getSettlementAccount());
            }
        }

        String fallbackPlatform = input;
        String fallbackErp = StrUtil.isNotBlank(requestedErp) ? requestedErp : input;
        return new Resolved(fallbackPlatform, fallbackErp, null, null);
    }

    public record Resolved(String platformStoreId, String erpStoreCode, String storeId, String merchantCode) {
    }
}
