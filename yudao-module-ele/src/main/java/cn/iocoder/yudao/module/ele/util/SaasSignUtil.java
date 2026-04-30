package cn.iocoder.yudao.module.ele.util;

import cn.hutool.crypto.digest.DigestUtil;
import java.util.Map;
import java.util.stream.Collectors;

public class SaasSignUtil {

    public static boolean verifySign(Map<String, String> params, String appSecret, String sign) {
        params.remove("sign");
        params.put("secret", appSecret);
        String sortedParams = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + (e.getValue() != null ? e.getValue() : ""))
                .collect(Collectors.joining("&"));
        String calculatedSign = DigestUtil.md5Hex(sortedParams).toUpperCase();
        return calculatedSign.equals(sign);
    }

    public static boolean verify(Object reqDTO, String appSecret) {
        try {
            String cmd = (String) reqDTO.getClass().getMethod("getCmd").invoke(reqDTO);
            String sign = (String) reqDTO.getClass().getMethod("getSign").invoke(reqDTO);

            Map<String, String> params = new java.util.TreeMap<>();
            params.put("cmd", cmd);
            params.put("secret", appSecret);

            try {
                Object version = reqDTO.getClass().getMethod("getVersion").invoke(reqDTO);
                if (version != null) params.put("version", version.toString());
            } catch (NoSuchMethodException e) {}

            try {
                Object timestamp = reqDTO.getClass().getMethod("getTimestamp").invoke(reqDTO);
                if (timestamp != null) params.put("timestamp", timestamp.toString());
            } catch (NoSuchMethodException e) {}

            try {
                Object orderId = reqDTO.getClass().getMethod("getOrderId").invoke(reqDTO);
                if (orderId != null) params.put("orderId", orderId.toString());
            } catch (NoSuchMethodException e) {}

            try {
                Object status = reqDTO.getClass().getMethod("getStatus").invoke(reqDTO);
                if (status != null) params.put("status", status.toString());
            } catch (NoSuchMethodException e) {}

            try {
                Object merchantCode = reqDTO.getClass().getMethod("getMerchantCode").invoke(reqDTO);
                if (merchantCode != null) params.put("merchantCode", merchantCode.toString());
            } catch (NoSuchMethodException e) {}

            try {
                Object erpStoreCode = reqDTO.getClass().getMethod("getErpStoreCode").invoke(reqDTO);
                if (erpStoreCode != null) params.put("erpStoreCode", erpStoreCode.toString());
            } catch (NoSuchMethodException e) {}

            try {
                Object channelOrderId = reqDTO.getClass().getMethod("getChannelOrderId").invoke(reqDTO);
                if (channelOrderId != null) params.put("channelOrderId", channelOrderId.toString());
            } catch (NoSuchMethodException e) {}

            try {
                Object ticket = reqDTO.getClass().getMethod("getTicket").invoke(reqDTO);
                if (ticket != null) params.put("ticket", ticket.toString());
            } catch (NoSuchMethodException e) {}

            String sortedParams = params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + (e.getValue() != null ? e.getValue() : ""))
                    .collect(Collectors.joining("&"));
            String calculatedSign = DigestUtil.md5Hex(sortedParams).toUpperCase();
            return calculatedSign.equals(sign);
        } catch (Exception e) {
            throw new RuntimeException("签名验证异常", e);
        }
    }

    public static String buildVerify(Object reqDTO, String appSecret) {
        try {
            String cmd = (String) reqDTO.getClass().getMethod("getCmd").invoke(reqDTO);

            Map<String, String> params = new java.util.TreeMap<>();
            params.put("cmd", cmd);
            params.put("secret", appSecret);

            try {
                Object version = reqDTO.getClass().getMethod("getVersion").invoke(reqDTO);
                if (version != null) params.put("version", version.toString());
            } catch (NoSuchMethodException e) {}

            try {
                Object timestamp = reqDTO.getClass().getMethod("getTimestamp").invoke(reqDTO);
                if (timestamp != null) params.put("timestamp", timestamp.toString());
            } catch (NoSuchMethodException e) {}

            String sortedParams = params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + (e.getValue() != null ? e.getValue() : ""))
                    .collect(Collectors.joining("&"));
            return DigestUtil.md5Hex(sortedParams).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("构建验证签名异常", e);
        }
    }
}
