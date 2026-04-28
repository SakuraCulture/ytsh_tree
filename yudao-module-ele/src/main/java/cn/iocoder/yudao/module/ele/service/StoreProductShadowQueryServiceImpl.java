package cn.iocoder.yudao.module.ele.service;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StoreProductPageReqVO;
import cn.iocoder.yudao.module.business.service.store.StoreProductShadowQueryService;
import cn.iocoder.yudao.module.business.service.store.bo.StoreProductShadowRowBO;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleStoreGoodsShadowDO;
import cn.iocoder.yudao.module.ele.dal.mysql.EleStoreGoodsShadowMapper;
import cn.iocoder.yudao.module.ele.enums.EleStoreGoodsShadowStatus;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class StoreProductShadowQueryServiceImpl implements StoreProductShadowQueryService {

    private static final Set<String> ACTIVE_MATCH_STATUSES = Set.of(
            EleStoreGoodsShadowStatus.UNMATCHED,
            EleStoreGoodsShadowStatus.CONFLICT);

    @Resource
    private EleStoreGoodsShadowMapper eleStoreGoodsShadowMapper;

    @Override
    public long countActiveShadowRows(StoreProductPageReqVO pageReqVO, boolean excludeFormalRows) {
        if (pageReqVO == null) {
            return 0;
        }
        return eleStoreGoodsShadowMapper.selectActiveCount(
                ACTIVE_MATCH_STATUSES,
                TenantContextHolder.getTenantId(),
                pageReqVO.getStoreId(),
                pageReqVO.getSkuCode(),
                pageReqVO.getSkuName(),
                pageReqVO.getMatchStatus(),
                excludeFormalRows);
    }

    @Override
    public List<StoreProductShadowRowBO> listActiveShadowRows(StoreProductPageReqVO pageReqVO, boolean excludeFormalRows,
                                                             int offset, int limit) {
        if (pageReqVO == null || limit <= 0) {
            return Collections.emptyList();
        }
        List<EleStoreGoodsShadowDO> rows = eleStoreGoodsShadowMapper.selectActivePage(
                ACTIVE_MATCH_STATUSES,
                TenantContextHolder.getTenantId(),
                pageReqVO.getStoreId(),
                pageReqVO.getSkuCode(),
                pageReqVO.getSkuName(),
                pageReqVO.getMatchStatus(),
                excludeFormalRows,
                Math.max(offset, 0),
                limit);
        return buildRows(rows);
    }

    @Override
    public List<StoreProductShadowRowBO> listActiveShadowRows(StoreProductPageReqVO pageReqVO, Set<String> formalRowKeys) {
        if (pageReqVO == null) {
            return Collections.emptyList();
        }
        List<EleStoreGoodsShadowDO> rows = eleStoreGoodsShadowMapper.selectActiveList(
                ACTIVE_MATCH_STATUSES,
                pageReqVO.getStoreId(),
                null,
                pageReqVO.getSkuCode(),
                pageReqVO.getSkuName());
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        return buildRows(rows.stream()
                .filter(row -> StrUtil.isBlank(pageReqVO.getMatchStatus())
                        || StrUtil.equals(pageReqVO.getMatchStatus(), row.getMatchStatus()))
                .filter(row -> formalRowKeys == null
                        || !formalRowKeys.contains(buildRowKey(row.getStoreId(), row.getSkuCode())))
                .toList());
    }

    private List<StoreProductShadowRowBO> buildRows(List<EleStoreGoodsShadowDO> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(this::buildRow)
                .filter(Objects::nonNull)
                .toList();
    }

    private StoreProductShadowRowBO buildRow(EleStoreGoodsShadowDO row) {
        if (row == null) {
            return null;
        }
        StoreProductShadowRowBO bo = new StoreProductShadowRowBO();
        bo.setShadowId(row.getId());
        bo.setStoreId(row.getStoreId());
        bo.setErpStoreCode(row.getErpStoreCode());
        bo.setPlatformStoreId(row.getPlatformStoreId());
        bo.setSkuCode(row.getSkuCode());
        bo.setSpuCode(row.getSpuCode());
        bo.setProductName(row.getTitle());
        bo.setSpecification(row.getSpecification());
        bo.setPrice(row.getSalePrice());
        bo.setPosStatus(parseInteger(row.getPosStatus()));
        bo.setIsActive(row.getIsActive());
        bo.setMatchStatus(row.getMatchStatus());
        bo.setCreateTime(row.getCreateTime());
        return bo;
    }

    private String buildRowKey(String storeId, String skuCode) {
        return StrUtil.trim(storeId) + "#" + StrUtil.trim(skuCode);
    }

    private Integer parseInteger(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
