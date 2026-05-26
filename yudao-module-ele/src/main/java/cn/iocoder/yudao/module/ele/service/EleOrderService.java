package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ele.controller.admin.vo.*;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderFailRecord;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderStatusLog;
import cn.iocoder.yudao.module.ele.service.dto.EleCompensateProgressDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderDetailRespDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderListReqDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderListRespDTO;
import cn.iocoder.yudao.module.ele.service.dto.OrderMessage;

import cn.iocoder.yudao.module.ele.controller.admin.vo.EleOrderFailRecordRespVO;

import java.util.List;
import java.util.Map;

public interface EleOrderService {

    OrderListRespDTO getOrderList(OrderListReqDTO req);

    OrderDetailRespDTO getOrderDetail(String platformStoreId, String merchantCode, String erpStoreCode, String orderId);

    void syncOrders(String platformStoreId, String merchantCode, String erpStoreCode);

    void syncOrders(String platformStoreId, String merchantCode, String erpStoreCode, Long startTime, Long endTime);

    EleCompensateProgressDTO getSyncProgress(String taskId);

    List<EleOrderFailRecord> getFailRecords();

    void retryFailRecord(Long id);

    void retryFailRecord(Long id, boolean overwrite);

    List<EleOrderStatusLog> getStatusLogs(String orderId);

    PageResult<OrderListRespDTO.OrderDetail> getOrdersFromLocal(String platformStoreId, String storeId, Integer status,
            Long startTime, Long endTime,
            Integer pageNo, Integer pageSize,
            String orderId, String channelOrderId, String buyerName, String buyerPhoneSuffix, String skuName,
            String channelType, Integer arriveType, String exceptionType, Integer deliveryMode, String address, String orderSort);

    OrderDetailRespDTO getDetailFromLocal(String orderId);

    Long saveOrUpdateBatch(List<OrderListRespDTO.OrderDetail> orders, String platformStoreId,
            String merchantCode, String erpStoreCode);

    Long saveOrUpdateBatch(List<OrderListRespDTO.OrderDetail> orders, String platformStoreId,
            String merchantCode, String erpStoreCode, boolean overwrite);

    void consumeOrderMessage(OrderMessage message);

    void consumeOrderMessage(OrderMessage message, boolean overwrite);

    
    PageResult<EleOrderFailRecordRespVO> getFailRecordPage(Long storeId, String orderId, String channelOrderId,
            String bizType, String failStage,
            String processStatus,
            Long startTime, Long endTime,
            Integer pageNo, Integer pageSize);

    
    void batchRetryFailRecord(List<Long> ids);

    
    int retryFailRecordsByTimeRange(Long startTime, Long endTime, Boolean overwrite);

    
    List<OrderListRespDTO.OrderDetail> getAllStoreOrdersWithDetails();

    
    void syncAllStores();

    
    void syncAllStores(Long startTime, Long endTime);

    
    java.util.Map<String, Object> syncAllStoresWithResult();

    
    Map<String, Integer> getUnhandledFailCount();

    
    List<Long> getAllFailedIds();

    
    int retryAllFailedRecordsBySpecifiedTime(Long specifiedTime, Boolean overwrite);

    
    Map<String, Object> getSyncConfig();

    
    void scanPendingRetryRecords();

    
    Map<Integer, Long> getStatusCounts(String platformStoreId, Long startTime, Long endTime,
            String orderId, String channelOrderId, String buyerName, String buyerPhoneSuffix, String skuName,
            String channelType, Integer arriveType, String exceptionType, Integer deliveryMode, String address);
}
