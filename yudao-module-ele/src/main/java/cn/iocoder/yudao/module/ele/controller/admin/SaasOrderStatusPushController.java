package cn.iocoder.yudao.module.ele.controller.admin;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.ele.dal.dataobject.EleApiConfig;
import cn.iocoder.yudao.module.ele.dal.mysql.EleApiConfigMapper;
import cn.iocoder.yudao.module.ele.mq.SaasOrderStatusPushProducer;
import cn.iocoder.yudao.module.ele.service.dto.OrderStatusPushMessage;
import cn.iocoder.yudao.module.ele.util.SaasSignUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "SaaS平台推送 - 订单通知")
@RestController
@RequestMapping("/ele/saas/order/push")
@Validated
@TenantIgnore
public class SaasOrderStatusPushController {

    @Resource
    private SaasOrderStatusPushProducer saasOrderStatusPushProducer;

    @Resource
    private EleApiConfigMapper eleApiConfigMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping(consumes = { "application/x-www-form-urlencoded", "application/json", "*/*" })
    @Operation(summary = "接收SaaS订单推送通知（订单创建 + 状态变更）")
    public Map<String, Object> handlePush(HttpServletRequest httpRequest) {
        String cmd = httpRequest.getParameter("cmd");
        String ticket = httpRequest.getParameter("ticket");
        String source = httpRequest.getParameter("source");
        String sign = httpRequest.getParameter("sign");
        String version = httpRequest.getParameter("version");
        String timestampStr = httpRequest.getParameter("timestamp");
        String bodyParam = httpRequest.getParameter("body");

        log.info("【SaaS推送】==== 收到推送通知 START ====");
        log.info("【SaaS推送】请求参数：cmd={}, ticket={}, source={}, sign={}, version={}, timestamp={}, body={}",
                cmd, ticket, source, sign, version, timestampStr, bodyParam);

        if ("push.verify".equals(cmd)) {
            log.info("【SaaS推送】收到push.verify验证请求，直接返回成功");
            return buildVerifyResponse(cmd, ticket, source, version, timestampStr);
        }

        try {
            log.info("【SaaS推送】步骤1: 开始签名验证...");
            validateSignature(httpRequest);
            log.info("【SaaS推送】步骤1: 签名验证通过 ✓");

            log.info("【SaaS推送】步骤2: 解析消息体...");
            OrderStatusPushMessage pushMessage = new OrderStatusPushMessage();
            if (bodyParam != null && !bodyParam.isEmpty()) {
                try {
                    Map<String, Object> bodyMap = objectMapper.readValue(bodyParam, Map.class);
                    pushMessage.setOrderId((String) bodyMap.get("order_id"));
                    pushMessage.setPlatformStoreId((String) bodyMap.get("platform_shop_id"));
                    log.info("【SaaS推送】步骤2: 解析body成功，order_id={}, platform_shop_id={}",
                            pushMessage.getOrderId(), pushMessage.getPlatformStoreId());
                    
                    if (bodyMap.containsKey("erp_store_code")) {
                        pushMessage.setErpStoreCode((String) bodyMap.get("erp_store_code"));
                        log.info("【SaaS推送】步骤2: 解析erp_store_code={}", pushMessage.getErpStoreCode());
                    }
                    if (bodyMap.containsKey("merchant_code")) {
                        pushMessage.setMerchantCode((String) bodyMap.get("merchant_code"));
                        log.info("【SaaS推送】步骤2: 解析merchant_code={}", pushMessage.getMerchantCode());
                    }
                    if (bodyMap.containsKey("status")) {
                        Object statusObj = bodyMap.get("status");
                        if (statusObj instanceof Number) {
                            pushMessage.setStatus(((Number) statusObj).intValue());
                        } else if (statusObj instanceof String) {
                            pushMessage.setStatus(parseInt((String) statusObj));
                        }
                        log.info("【SaaS推送】步骤2: 解析status={}", pushMessage.getStatus());
                    }
                    if (bodyMap.containsKey("channel_order_id")) {
                        pushMessage.setChannelOrderId((String) bodyMap.get("channel_order_id"));
                    }
                } catch (Exception e) {
                    log.error("【SaaS推送】步骤2: 解析body失败，body={}, error={}", bodyParam, e.getMessage());
                    throw new IllegalArgumentException("消息体格式错误");
                }
            } else {
                log.warn("【SaaS推送】步骤2: body为空，无法解析订单信息");
            }

            pushMessage.setTicket(ticket);
            pushMessage.setSource(source);
            pushMessage.setPushTime(System.currentTimeMillis());

            if ("order.create".equals(cmd) || "saas.order.create".equals(cmd)) {
                pushMessage.setCmd("saas.order.create");
                log.info("【SaaS推送】消息类型: 订单创建 [{}]", cmd);
            } else if ("order.status.push".equals(cmd) || "saas.order.status.push".equals(cmd)) {
                pushMessage.setCmd("saas.order.status.push");
                log.info("【SaaS推送】消息类型: 订单状态变更 [{}]", cmd);
            } else if ("order.remind.push".equals(cmd) || "saas.order.remind.push".equals(cmd)) {
                pushMessage.setCmd("saas.order.remind.push");
                log.info("【SaaS推送】消息类型: 催单推送 [{}]", cmd);
            } else if ("saas.order.deliveryStatus.push".equals(cmd)) {
                pushMessage.setCmd("saas.order.deliveryStatus.push");
                log.info("【SaaS推送】消息类型: 配送状态变更 [{}]", cmd);
            } else if ("saas.supply.chain.execution.order.msg".equals(cmd)) {
                pushMessage.setCmd("saas.supply.chain.execution.order.msg");
                log.info("【SaaS推送】消息类型: 供应链订单消息 [{}]", cmd);
            } else if ("saas.reverse.order.status.push".equals(cmd)) {
                pushMessage.setCmd("saas.reverse.order.status.push");
                log.info("【SaaS推送】消息类型: 逆向订单状态变更 [{}]", cmd);
            } else {
                log.warn("【SaaS推送】未知的cmd类型，cmd={}", cmd);
                return buildErrorResponse(cmd, ticket, source, version, 400, "未知的cmd类型");
            }

            log.info("【SaaS推送】步骤3: 投递Kafka消息，orderId={}, cmd={}, platformStoreId={}",
                    pushMessage.getOrderId(), pushMessage.getCmd(), pushMessage.getPlatformStoreId());
            log.info("【SaaS推送】完整推送消息JSON: {}", objectMapper.writeValueAsString(pushMessage));
            saasOrderStatusPushProducer.sendPushMessage(pushMessage)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("【SaaS推送】步骤3: 投递Kafka失败 ✗，orderId={}, cmd={}, error={}",
                                    pushMessage.getOrderId(), cmd, ex.getMessage());
                        } else {
                            log.info("【SaaS推送】步骤3: 投递Kafka成功 ✓，orderId={}, cmd={}, partition={}, offset={}",
                                    pushMessage.getOrderId(), cmd, result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });

            log.info("【SaaS推送】==== 推送通知处理完成 END，orderId={} ====", pushMessage.getOrderId());
            return buildSuccessResponse(cmd, ticket, source, version);

        } catch (SecurityException e) {
            log.error("【SaaS推送】签名验证失败，cmd={}, ticket={}", cmd, ticket);
            return buildErrorResponse(cmd, ticket, source, version, 401, "签名验证失败");
        } catch (IllegalArgumentException e) {
            log.error("【SaaS推送】参数校验失败，cmd={}, error={}", cmd, e.getMessage());
            return buildErrorResponse(cmd, ticket, source, version, 400, e.getMessage());
        } catch (Exception e) {
            log.error("【SaaS推送】处理异常，cmd={}, error={}", cmd, e.getMessage(), e);
            return buildErrorResponse(cmd, ticket, source, version, 500, "系统内部错误");
        }
    }

    private void validateSignature(HttpServletRequest httpRequest) {
        EleApiConfig config = eleApiConfigMapper.selectActive();
        if (config == null || config.getAppSecret() == null || config.getAppSecret().isEmpty()) {
            log.warn("【SaaS推送】appSecret未配置（ele_api_config表无启用记录），跳过签名验证");
            return;
        }

        String sign = httpRequest.getParameter("sign");
        if (sign == null || sign.isEmpty()) {
            throw new SecurityException("签名为空");
        }

        Map<String, String> params = new java.util.TreeMap<>();
        httpRequest.getParameterMap().forEach((key, values) -> {
            if (!"sign".equals(key) && values != null && values.length > 0) {
                params.put(key, values[0] != null ? values[0] : "");
            }
        });

        boolean valid = SaasSignUtil.verifySign(params, config.getAppSecret(), sign);
        if (!valid) {
            log.warn("【SaaS推送】签名验证失败，params={}, calculatedSign未匹配, expectedSign={}", params, sign);
            throw new SecurityException("签名验证失败");
        }
        log.info("【SaaS推送】签名验证通过");
    }

    private Map<String, Object> buildVerifyResponse(String cmd, String ticket, String source,
            String version, String timestampStr) {
        Map<String, Object> body = new HashMap<>();
        body.put("errno", 0);
        body.put("error", "success");

        Map<String, Object> response = new HashMap<>();
        response.put("body", body);
        response.put("cmd", cmd);
        response.put("source", source);
        response.put("ticket", ticket);
        response.put("timestamp", System.currentTimeMillis() / 1000);
        response.put("version", version != null ? version : "3");
        return response;
    }

    private Map<String, Object> buildSuccessResponse(String cmd, String ticket, String source, String version) {
        Map<String, Object> body = new HashMap<>();
        body.put("errno", 0);
        body.put("error", "success");

        Map<String, Object> response = new HashMap<>();
        response.put("body", body);
        response.put("cmd", cmd != null ? cmd : "");
        response.put("source", source != null ? source : "");
        response.put("ticket", ticket != null ? ticket : "");
        response.put("timestamp", System.currentTimeMillis() / 1000);
        response.put("version", version != null ? version : "3");
        return response;
    }

    private Map<String, Object> buildErrorResponse(String cmd, String ticket, String source,
            String version, int errno, String error) {
        Map<String, Object> body = new HashMap<>();
        body.put("errno", errno);
        body.put("error", error);

        Map<String, Object> response = new HashMap<>();
        response.put("body", body);
        response.put("cmd", cmd != null ? cmd : "");
        response.put("source", source != null ? source : "");
        response.put("ticket", ticket != null ? ticket : "");
        response.put("timestamp", System.currentTimeMillis() / 1000);
        response.put("version", version != null ? version : "3");
        return response;
    }

    private Long parseLong(String value) {
        if (value == null)
            return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        if (value == null)
            return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
