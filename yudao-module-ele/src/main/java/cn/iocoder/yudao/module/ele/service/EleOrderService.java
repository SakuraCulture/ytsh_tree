package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderFailRecord;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderStatusLog;
import cn.iocoder.yudao.module.ele.service.dto.EleCompensateProgressDTO;
import cn.iocoder.yudao.module.ele.service.dto.EleSyncSubmitRespDTO;
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

    EleSyncSubmitRespDTO submitSyncTask(String platformStoreId, String merchantCode, String erpStoreCode,
            Long startTime, Long endTime, boolean compensate);

    EleCompensateProgressDTO getSyncProgress(String taskId);

    List<EleOrderFailRecord> getFailRecords();

    void retryFailRecord(Long id);

    void retryFailRecord(Long id, boolean overwrite);

    List<EleOrderStatusLog> getStatusLogs(String orderId);

    PageResult<OrderListRespDTO.OrderDetail> getOrdersFromLocal(String platformStoreId, Integer status,
            Long startTime, Long endTime,
            Integer pageNo, Integer pageSize);

    OrderDetailRespDTO getDetailFromLocal(String orderId);

    Long saveOrUpdateBatch(List<OrderListRespDTO.OrderDetail> orders, String platformStoreId,
            String merchantCode, String erpStoreCode);

    Long saveOrUpdateBatch(List<OrderListRespDTO.OrderDetail> orders, String platformStoreId,
            String merchantCode, String erpStoreCode, boolean overwrite);

    void consumeOrderMessage(OrderMessage message);

    void consumeOrderMessage(OrderMessage message, boolean overwrite);

    /**
     * 分页查询失败记录
     */
    PageResult<EleOrderFailRecordRespVO> getFailRecordPage(Long storeId, String orderId, String channelOrderId,
            String bizType, String failStage,
            String processStatus,
            Long startTime, Long endTime,
            Integer pageNo, Integer pageSize);

    /**
     * 批量重试失败记录
     */
    void batchRetryFailRecord(List<Long> ids);

    /**
     * 按时间范围批量重试失败记录
     *
     * @param startTime 开始时间（毫秒级时间戳）
     * @param endTime   结束时间（毫秒级时间戳）
     * @param overwrite 是否覆盖已存在订单
     * @return 成功重试的记录数
     */
    int retryFailRecordsByTimeRange(Long startTime, Long endTime, Boolean overwrite);

    /**
     * 查询所有门店的订单列表及详情
     *
     * @return 所有订单列表（包含详情）
     */
    List<OrderListRespDTO.OrderDetail> getAllStoreOrdersWithDetails();

    /**
     * 手动触发全部门店订单同步
     */
    void syncAllStores();

    /**
     * 手动触发全部门店订单同步（支持自定义时间范围）
     *
     * @param startTime 起始时间（秒级时间戳，可选）
     * @param endTime   结束时间（秒级时间戳，可选）
     */
    void syncAllStores(Long startTime, Long endTime);

    /**
     * 手动触发全部门店订单同步（返回同步结果）
     *
     * @return 同步结果Map，包含totalCount、successCount、failCount、elapsedSeconds、completed、failedStores
     */
    java.util.Map<String, Object> syncAllStoresWithResult();

    /**
     * 获取未处理失败记录统计
     *
     * @return 统计Map，包含totalUnhandleCount、pendingRetryCount、failedCount
     */
    Map<String, Integer> getUnhandledFailCount();

    /**
     * 获取所有FAILED状态的失败记录ID列表
     *
     * @return ID列表
     */
    List<Long> getAllFailedIds();

    /**
     * 按指定时间点批量重试失败记录（每天定时执行）
     *
     * @param specifiedTime 指定的时间点（毫秒级时间戳，通常为当天 00:00:00）
     * @param overwrite     是否覆盖已存在订单
     * @return 成功重试的记录数
     */
    int retryAllFailedRecordsBySpecifiedTime(Long specifiedTime, Boolean overwrite);

    /**
     * 获取订单同步配置信息
     *
     * @return 配置Map，包含syncIntervalMinutes（同步间隔分钟数）、syncIntervalMs（同步间隔毫秒数）
     */
    Map<String, Object> getSyncConfig();

    /**
     * 扫描待重试的失败记录并重新提交
     * 
     * 由定时任务 {@link cn.iocoder.yudao.module.ele.job.EleOrderRetryScanJob} 调用
     */
    void scanPendingRetryRecords();
}
