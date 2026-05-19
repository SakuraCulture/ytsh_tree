package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.module.ele.controller.admin.vo.BillSyncFailLogVO;
import cn.iocoder.yudao.module.ele.controller.admin.vo.OrderBillVO;

import java.util.List;

public interface EleBillSyncService {

    void syncAllBillsByDate(String billDate);

    void syncAllBillsByDateRange(String startDate, String endDate);

    void syncBillByStoreAndDate(String merchantCode, String platformStoreId, String billDate);

    void retryFailedBillSync();

    void retryFailedBillSyncByLogId(Long logId);

    void retryByOrderId(String orderId);

    OrderBillVO getOrderBillInfo(String orderId);

    List<BillSyncFailLogVO> getFailLogList();

    OrderBillVO getBillSummary(String orderId);
}
