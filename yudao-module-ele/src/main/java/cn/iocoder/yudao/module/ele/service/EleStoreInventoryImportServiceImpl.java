package cn.iocoder.yudao.module.ele.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.module.business.controller.admin.store.vo.StorePlatformRespVO;
import cn.iocoder.yudao.module.business.service.store.StoreService;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryImportExcelVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.EleStoreInventoryImportRespVO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreInventoryIngestResultBO;
import cn.iocoder.yudao.module.ele.service.bo.EleStoreInventoryIngestRowBO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EleStoreInventoryImportServiceImpl implements EleStoreInventoryImportService {

    private static final Long ELE_PLATFORM_ID = 1L;

    @Resource
    private StoreService storeService;
    @Resource
    private EleStoreInventoryIngestService ingestService;

    @Override
    public EleStoreInventoryImportRespVO importRows(List<EleStoreInventoryImportExcelVO> rows) {
        EleStoreInventoryImportRespVO respVO = new EleStoreInventoryImportRespVO();
        if (CollUtil.isEmpty(rows)) {
            return respVO;
        }
        Map<String, StorePlatformRespVO> storeMap = storeService.getOpenPlatformStores(ELE_PLATFORM_ID).stream()
                .filter(store -> StrUtil.isNotBlank(store.getPlatformStoreId()))
                .collect(Collectors.toMap(store -> StrUtil.trim(store.getPlatformStoreId()), Function.identity(),
                        (first, second) -> first, LinkedHashMap::new));
        for (int i = 0; i < rows.size(); i++) {
            int rowNo = i + 1;
            EleStoreInventoryImportExcelVO row = rows.get(i);
            String validationError = validateRow(row);
            if (validationError != null) {
                addFailure(respVO, rowNo, row == null ? null : row.getSkuCode(), validationError);
                continue;
            }
            String erpStoreCode = StrUtil.trim(row.getErpStoreCode());
            StorePlatformRespVO store = storeMap.get(erpStoreCode);
            if (store == null) {
                addFailure(respVO, rowNo, row.getSkuCode(), "ERP门店编码未匹配到平台门店");
                continue;
            }
            EleStoreInventoryIngestResultBO ingestResult = ingestService.ingest(toIngestRow(row, store));
            if (EleStoreInventoryIngestService.PERSIST_STATUS_FORMAL.equals(ingestResult.getPersistStatus())) {
                respVO.setFormalSuccessCount(respVO.getFormalSuccessCount() + 1);
                continue;
            }
            if (EleStoreInventoryIngestService.PERSIST_STATUS_SHADOW.equals(ingestResult.getPersistStatus())) {
                respVO.setShadowSuccessCount(respVO.getShadowSuccessCount() + 1);
                respVO.setGovernanceCount(respVO.getGovernanceCount() + 1);
                continue;
            }
            addFailure(respVO, rowNo, row.getSkuCode(), "库存写入结果未知");
        }
        return respVO;
    }

    private EleStoreInventoryIngestRowBO toIngestRow(EleStoreInventoryImportExcelVO row, StorePlatformRespVO store) {
        EleStoreInventoryIngestRowBO ingestRow = new EleStoreInventoryIngestRowBO();
        ingestRow.setPlatformId(ELE_PLATFORM_ID);
        ingestRow.setMerchantCode(StrUtil.trim(store.getSettlementAccount()));
        ingestRow.setErpStoreCode(StrUtil.trim(row.getErpStoreCode()));
        ingestRow.setPlatformStoreId(StrUtil.trim(store.getPlatformStoreId()));
        ingestRow.setStoreId(StrUtil.trim(store.getStoreId()));
        ingestRow.setSkuCode(StrUtil.trim(row.getSkuCode()));
        ingestRow.setSubSkuCode(StrUtil.trim(row.getSubSkuCode()));
        ingestRow.setAvailableForSale(row.getAvailableForSale());
        ingestRow.setReservedAmount(row.getReservedAmount());
        ingestRow.setPhysicalStockTotalAmount(row.getPhysicalStockTotalAmount());
        ingestRow.setPhysicalStockAvailableAmount(row.getPhysicalStockAvailableAmount());
        ingestRow.setPhysicalStockOccupiedAmount(row.getPhysicalStockOccupiedAmount());
        ingestRow.setPhysicalStockIntransitAmount(row.getPhysicalStockIntransitAmount());
        ingestRow.setRawPayload(JSONUtil.toJsonStr(row));
        return ingestRow;
    }

    private String validateRow(EleStoreInventoryImportExcelVO row) {
        if (row == null) {
            return "导入行不能为空";
        }
        if (StrUtil.isBlank(row.getErpStoreCode())) {
            return "ERP门店编码不能为空";
        }
        if (StrUtil.isBlank(row.getSkuCode())) {
            return "SKU编码不能为空";
        }
        if (row.getPhysicalStockTotalAmount() == null) {
            return "总库存不能为空";
        }
        if (row.getAvailableForSale() == null) {
            return "可售库存不能为空";
        }
        if (row.getReservedAmount() == null) {
            return "预留库存不能为空";
        }
        if (row.getPhysicalStockAvailableAmount() == null) {
            return "物理可用库存不能为空";
        }
        if (row.getPhysicalStockOccupiedAmount() == null) {
            return "物理占用库存不能为空";
        }
        if (row.getPhysicalStockIntransitAmount() == null) {
            return "物理在途库存不能为空";
        }
        return null;
    }

    private void addFailure(EleStoreInventoryImportRespVO respVO, int rowNo, String skuCode, String message) {
        respVO.setFailureCount(respVO.getFailureCount() + 1);
        EleStoreInventoryImportRespVO.FailureItem item = new EleStoreInventoryImportRespVO.FailureItem();
        item.setRowNo(rowNo);
        item.setSkuCode(StrUtil.trim(skuCode));
        item.setMessage(message);
        respVO.getFailureList().add(item);
    }
}
