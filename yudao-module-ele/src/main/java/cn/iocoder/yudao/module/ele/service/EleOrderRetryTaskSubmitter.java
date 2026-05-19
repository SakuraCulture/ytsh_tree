package cn.iocoder.yudao.module.ele.service;

import cn.iocoder.yudao.module.ele.dal.dataobject.EleOrderFailRecord;
import cn.iocoder.yudao.module.ele.dal.mysql.EleOrderFailRecordMapper;
import cn.iocoder.yudao.module.ele.mq.EleOrderRetryKafkaProducer;
import cn.iocoder.yudao.module.ele.service.dto.OrderRetryMessage;
import cn.iocoder.yudao.framework.common.util.monitor.TracerUtils;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class EleOrderRetryTaskSubmitter {

    @Resource
    private EleOrderFailRecordMapper eleOrderFailRecordMapper;

    @Resource
    private EleOrderRetryKafkaProducer retryKafkaProducer;

    private static final int MAX_RETRY_COUNT = 3;

    public List<Long> submitRetryTasks(List<RetryTask> tasks) {
        List<Long> successFailRecordIds = new ArrayList<>();

        if (tasks == null || tasks.isEmpty()) {
            return successFailRecordIds;
        }

        log.info("【重试Kafka】开始发送重试消息到Kafka，共{}个订单", tasks.size());

        int successCount = 0;
        int failCount = 0;

        for (RetryTask task : tasks) {
            try {
                EleOrderFailRecord record = eleOrderFailRecordMapper.selectById(task.getFailRecordId());
                if (record == null) {
                    log.warn("【重试Kafka】失败记录不存在，跳过，failRecordId={}", task.getFailRecordId());
                    failCount++;
                    continue;
                }

                if ("SUCCESS".equals(record.getProcessStatus())) {
                    log.info("【重试Kafka】失败记录已成功，跳过，orderId={}", task.getOrderId());
                    successCount++;
                    successFailRecordIds.add(task.getFailRecordId());
                    continue;
                }

                if ("RETRYING".equals(record.getProcessStatus())) {
                    log.info("【重试Kafka】失败记录正在重试，跳过，orderId={}", task.getOrderId());
                    successCount++;
                    successFailRecordIds.add(task.getFailRecordId());
                    continue;
                }

                OrderRetryMessage message = new OrderRetryMessage();
                message.setOrderId(task.getOrderId());
                message.setChannelOrderId(task.getChannelOrderId());
                message.setPlatformStoreId(task.getPlatformStoreId());
                message.setMerchantCode(task.getMerchantCode());
                message.setErpStoreCode(task.getErpStoreCode());
                message.setFailRecordId(task.getFailRecordId());
                message.setRetryCount(record.getRetryCount() == null ? 1 : record.getRetryCount() + 1);
                message.setCreateTime(System.currentTimeMillis());
                String traceId = TracerUtils.getTraceId();
                if (StrUtil.isNotBlank(traceId)) {
                    message.setTraceId(traceId);
                }

                boolean sendSuccess = retryKafkaProducer.sendRetryMessageAndWait(message);
                if (sendSuccess) {
                    successCount++;
                    successFailRecordIds.add(task.getFailRecordId());
                } else {
                    failCount++;
                    log.error("【重试Kafka】发送重试消息失败，orderId={}", task.getOrderId());
                }

            } catch (Exception e) {
                failCount++;
                log.error("【重试Kafka】发送重试消息异常，orderId={}, error={}", task.getOrderId(), e.getMessage(), e);
            }
        }

        log.info("【重试Kafka】重试消息发送完成，总数={}, 成功={}, 失败={}", tasks.size(), successCount, failCount);
        return successFailRecordIds;
    }

    public int submitRetryTasksAndIgnoreResult(List<RetryTask> tasks) {
        List<Long> successFailRecordIds = submitRetryTasks(tasks);
        return successFailRecordIds.size();
    }

    @lombok.Data
    public static class RetryTask {
        private String orderId;
        private String channelOrderId;
        private String platformStoreId;
        private String merchantCode;
        private String erpStoreCode;
        private Long failRecordId;
        private Object orderDetail;

        public RetryTask(String orderId, String channelOrderId, String platformStoreId,
                String merchantCode, String erpStoreCode, Long failRecordId, Object orderDetail) {
            this.orderId = orderId;
            this.channelOrderId = channelOrderId;
            this.platformStoreId = platformStoreId;
            this.merchantCode = merchantCode;
            this.erpStoreCode = erpStoreCode;
            this.failRecordId = failRecordId;
            this.orderDetail = orderDetail;
        }
    }
}
