package cn.iocoder.yudao.module.ele.service.traffic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class EleTrafficInterceptor {

    private static final Logger log = LoggerFactory.getLogger(EleTrafficInterceptor.class);

    private final EleTrafficMetricsCollector metricsCollector;
    private final ObjectMapper objectMapper;

    public EleTrafficInterceptor(EleTrafficMetricsCollector metricsCollector, ObjectMapper objectMapper) {
        this.metricsCollector = metricsCollector;
        this.objectMapper = objectMapper;
    }

    public void beforeRequest(String apiCode, String traceId, Object requestParam) {
        try {
            long requestBytes = estimateBytes(requestParam);
            metricsCollector.recordRequest(apiCode, traceId, requestBytes, System.currentTimeMillis());
            log.debug("[流量拦截] apiCode={}, traceId={}, requestSize={} bytes", apiCode, traceId, requestBytes);
        } catch (Exception e) {
            log.warn("[流量拦截] 计算请求大小失败, apiCode={}, error={}", apiCode, e.getMessage());
        }
    }

    public void afterResponse(String apiCode, String traceId, Object responseResult, boolean success, int durationMs) {
        try {
            long responseBytes = estimateBytes(responseResult);
            metricsCollector.recordResponse(traceId, responseBytes, success, durationMs);
            log.debug("[流量拦截] apiCode={}, traceId={}, responseSize={} bytes, success={}, duration={}ms",
                    apiCode, traceId, responseBytes, success, durationMs);
        } catch (Exception e) {
            log.warn("[流量拦截] 计算响应大小失败, apiCode={}, error={}", apiCode, e.getMessage());
        }
    }

    private long estimateBytes(Object obj) {
        if (obj == null) {
            return 0;
        }
        try {
            if (obj instanceof byte[]) {
                return ((byte[]) obj).length;
            }
            if (obj instanceof String) {
                return ((String) obj).getBytes(StandardCharsets.UTF_8).length;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            objectMapper.writeValue(baos, obj);
            return baos.size();
        } catch (IOException e) {
            return obj.toString().getBytes(StandardCharsets.UTF_8).length;
        }
    }
}
