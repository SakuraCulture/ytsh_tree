package cn.iocoder.yudao.module.ele.service.client;

import cn.iocoder.yudao.module.ele.dal.dataobject.EleApiConfig;
import cn.iocoder.yudao.module.ele.service.dto.BillListReqDTO;
import cn.iocoder.yudao.module.ele.service.dto.BillListRespDTO;
import com.alibaba.ocean.rawsdk.common.BizResultWrapper;
import lib.ele.retail.param.SaasGoodsStoreQueryBatchParam;
import lib.ele.retail.param.SaasGoodsStoreQueryBatchResult;
import lib.ele.retail.param.SaasOrderGetParam;
import lib.ele.retail.param.SaasOrderGetResult;
import lib.ele.retail.param.SaasOrderListParam;
import lib.ele.retail.param.SaasOrderListResult;
import lib.ele.retail.param.SaasSkuStockInventoryBatchQueryParam;
import lib.ele.retail.param.SaasSkuStockInventoryBatchQueryResult;

public interface EleOpenApiClient {

    BizResultWrapper<SaasOrderListResult> sendOrderList(EleApiConfig config, SaasOrderListParam param,
                                                        String merchantCode, String platformStoreId,
                                                        String erpStoreCode);

    BizResultWrapper<SaasOrderGetResult> sendOrderDetail(EleApiConfig config, SaasOrderGetParam param,
                                                         String merchantCode, String platformStoreId,
                                                         String erpStoreCode, String orderId);

    BizResultWrapper<SaasGoodsStoreQueryBatchResult> sendStoreGoodsQueryBatch(EleApiConfig config,
                                                                              SaasGoodsStoreQueryBatchParam param,
                                                                              String merchantCode,
                                                                              String platformStoreId,
                                                                              String erpStoreCode);

    BizResultWrapper<SaasSkuStockInventoryBatchQueryResult> sendSkuStockInventoryBatchQuery(EleApiConfig config,
                                                                                             SaasSkuStockInventoryBatchQueryParam param,
                                                                                             String merchantCode,
                                                                                             String platformStoreId,
                                                                                             String erpStoreCode);

    BillListRespDTO getBillList(BillListReqDTO req);
}
