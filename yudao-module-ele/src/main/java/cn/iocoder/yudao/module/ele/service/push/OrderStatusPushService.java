package cn.iocoder.yudao.module.ele.service.push;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.websocket.core.sender.WebSocketMessageSender;
import cn.iocoder.yudao.module.ele.websocket.message.OrderRemindMessage;
import cn.iocoder.yudao.module.ele.websocket.message.OrderStatusChangeMessage;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class OrderStatusPushService {

    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private WebSocketMessageSender webSocketMessageSender;

    
    public void pushOrderStatusChange(String orderId, Integer oldStatus, Integer newStatus,
                                       String storeName, String buyerName) {
        try {
            List<Long> userIds = adminUserApi.getUserList(List.of(1L, 2L, 3L)).stream()
                    .map(AdminUserRespDTO::getId)
                    .toList();
            
            for (Long userId : userIds) {
                try {
                    AdminUserRespDTO user = adminUserApi.getUser(userId);
                    if (user == null) {
                        continue;
                    }
                    
                    Boolean pushEnabled = getOrderPushEnabled(user);
                    if (!Boolean.TRUE.equals(pushEnabled)) {
                        continue;
                    }
                    
                    if (!shouldPushStatus(user, newStatus)) {
                        continue;
                    }
                    
                    OrderStatusChangeMessage message = new OrderStatusChangeMessage();
                    message.setOrderId(orderId);
                    message.setOldStatus(oldStatus);
                    message.setNewStatus(newStatus);
                    message.setStoreName(storeName);
                    message.setBuyerName(buyerName);
                    message.setDesktopEnabled(getOrderPushDesktop(user));
                    message.setTimestamp(System.currentTimeMillis());
                    
                    webSocketMessageSender.sendObject(UserTypeEnum.ADMIN.getValue(), 
                            userId, "order-status-change", message);
                    
                    log.info("【订单推送】用户{}收到订单{}状态变更推送，{}→{}",
                            userId, orderId, oldStatus, newStatus);
                            
                } catch (Exception e) {
                    log.error("【订单推送】发送给单个用户失败，userId={}", userId, e);
                }
            }
        } catch (Exception e) {
            log.error("【订单推送】推送失败", e);
        }
    }

    public void pushOrderRemind(String orderId, String channelOrderId, String storeName,
                                String buyerName, Integer remindCount) {
        try {
            List<Long> userIds = adminUserApi.getUserList(List.of(1L, 2L, 3L)).stream()
                    .map(AdminUserRespDTO::getId)
                    .toList();

            for (Long userId : userIds) {
                try {
                    AdminUserRespDTO user = adminUserApi.getUser(userId);
                    if (user == null) {
                        continue;
                    }

                    Boolean pushEnabled = getOrderPushEnabled(user);
                    if (!Boolean.TRUE.equals(pushEnabled)) {
                        continue;
                    }

                    OrderRemindMessage message = new OrderRemindMessage();
                    message.setOrderId(orderId);
                    message.setChannelOrderId(channelOrderId);
                    message.setStoreName(storeName);
                    message.setBuyerName(buyerName);
                    message.setRemindCount(remindCount != null ? remindCount : 1);
                    message.setTimestamp(System.currentTimeMillis());

                    webSocketMessageSender.sendObject(UserTypeEnum.ADMIN.getValue(),
                            userId, "order-remind-push", message);

                    log.info("【催单推送】用户{}收到订单{}催单通知，remindCount={}",
                            userId, orderId, message.getRemindCount());

                } catch (Exception e) {
                    log.error("【催单推送】发送给单个用户失败，userId={}", userId, e);
                }
            }
        } catch (Exception e) {
            log.error("【催单推送】推送失败", e);
        }
    }

    private boolean shouldPushStatus(AdminUserRespDTO user, Integer newStatus) {
        String pushTypes = getOrderPushTypes(user);
        if (pushTypes == null || pushTypes.trim().isEmpty()) {
            return true;
        }
        
        List<String> allowedTypes = Arrays.asList(pushTypes.split(","));
        return allowedTypes.contains(String.valueOf(newStatus));
    }

    private Boolean getOrderPushEnabled(AdminUserRespDTO user) {
        try {
            Object enabled = user.getClass().getMethod("getOrderPushEnabled").invoke(user);
            return enabled != null ? (Boolean) enabled : true;
        } catch (Exception e) {
            return true;
        }
    }

    private String getOrderPushTypes(AdminUserRespDTO user) {
        try {
            Object types = user.getClass().getMethod("getOrderPushTypes").invoke(user);
            return types != null ? (String) types : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean getOrderPushDesktop(AdminUserRespDTO user) {
        try {
            Object desktop = user.getClass().getMethod("getOrderPushDesktop").invoke(user);
            return desktop != null ? (Boolean) desktop : false;
        } catch (Exception e) {
            return false;
        }
    }
}
