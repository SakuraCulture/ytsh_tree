package cn.iocoder.yudao.module.business.service.store;

import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductPageReqVO;
import cn.iocoder.yudao.module.business.service.store.bo.StoreProductShadowRowBO;

import java.util.List;
import java.util.Set;

public interface StoreProductShadowQueryService {

    long countActiveShadowRows(StoreProductPageReqVO pageReqVO, boolean excludeFormalRows);

    List<StoreProductShadowRowBO> listActiveShadowRows(StoreProductPageReqVO pageReqVO, boolean excludeFormalRows,
                                                       int offset, int limit);

    List<StoreProductShadowRowBO> listActiveShadowRows(StoreProductPageReqVO pageReqVO, Set<String> formalRowKeys);
}
