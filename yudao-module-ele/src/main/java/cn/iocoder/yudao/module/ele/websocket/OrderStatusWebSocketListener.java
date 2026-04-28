package cn.iocoder.yudao.module.ele.websocket;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.websocket.core.listener.WebSocketMessageListener;
import cn.iocoder.yudao.framework.websocket.core.sender.WebSocketMessageSender;
import cn.iocoder.yudao.framework.websocket.core.util.WebSocketFrameworkUtils;
import cn.iocoder.yudao.module.ele.websocket.message.OrderStatusChangeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class OrderStatusWebSocketListener implements WebSocketMessageListener<OrderStatusChangeMessage> {

    @SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
    @Autowired(required = false)
    private WebSocketMessageSender webSocketMessageSender;

    @Override
    public void onMessage(WebSocketSession session, OrderStatusChangeMessage message) {
        Long fromUserId = WebSocketFrameworkUtils.getLoginUserId(session);
        
        if (message.getOrderId() != null) {
            webSocketMessageSender.sendObject(UserTypeEnum.ADMIN.getValue(), 
                    fromUserId,
                    "order-status-change", 
                    message);
        }
    }

    @Override
    public String getType() {
        return "order-status-change";
    }
}
