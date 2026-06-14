package com.coze.aicoze.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SSE {@code event:error} 的 data 负载，与 docs/agent-platform-hardening-checklist.md 一致。
 */
public final class SseErrorJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SseErrorJson() {
    }

    public static String format(int code, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", code);
        m.put("message", message == null ? "" : message);
        try {
            return MAPPER.writeValueAsString(m);
        } catch (JsonProcessingException e) {
            return "{\"code\":" + BizErrorCodes.UNKNOWN_SYSTEM + ",\"message\":\"服务暂时异常，请稍后重试\"}";
        }
    }
}
