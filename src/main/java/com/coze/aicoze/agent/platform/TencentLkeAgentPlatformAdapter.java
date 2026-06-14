package com.coze.aicoze.agent.platform;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.enums.AgentType;
import com.coze.aicoze.exception.BusinessException;
import com.coze.aicoze.full.flow.FlowMarkerApplier;
import com.coze.aicoze.service.PlatformOutboundRateLimiter;
import com.coze.openapi.client.chat.model.ChatUsage;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.lke.v20231130.LkeClient;
import com.tencentcloudapi.lke.v20231130.models.GetWsTokenRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Consumer;

@Component
@Slf4j
@RequiredArgsConstructor
public class TencentLkeAgentPlatformAdapter implements AgentPlatformAdapter {

    private final LkeClient lkeClient;
    private final FlowMarkerApplier flowMarkerApplier;
    private final PlatformOutboundRateLimiter platformOutboundRateLimiter;

    @org.springframework.beans.factory.annotation.Value("${tencent.lke.chat-url:https://wss.lke.cloud.tencent.com/adp/v2/chat}")
    private String chatUrl;
    @org.springframework.beans.factory.annotation.Value("${tencent.lke.secret-id:}")
    private String secretId;
    @org.springframework.beans.factory.annotation.Value("${tencent.lke.read-timeout-ms:60000}")
    private int readTimeoutMs;

    @Override
    public String provider() {
        return "tencent-lke";
    }

    @Override
    public PlatformChatResponse chat(String userMessage, String botId, String sessionId, String userId, String conversationId, AgentType agentType) {
        platformOutboundRateLimiter.acquireTencentOrThrow();
        String convId = normalizeConversationId(conversationId != null ? conversationId : sessionId);
        String requestId = normalizeConversationId(UUID.randomUUID().toString());
        String visitorId = normalizeVisitorId(userId, sessionId);
        verifySdkGetWsToken(botId, visitorId);
        String payload = buildChatPayload(requestId, convId, botId, visitorId, userMessage);
        String body = callSse(payload, sessionId);
        String content = extractAssistantTextFromSse(body, sessionId);
        return PlatformChatResponse.builder()
                .content(content)
                .conversationId(convId)
                .usage(null)
                .build();
    }

    @Override
    public void streamChat(String userMessage, String botId, String sessionId, String userId, String conversationId, AgentType agentType, Consumer<String> onContent, Consumer<ChatUsage> onComplete) {
        PlatformChatResponse response = chat(userMessage, botId, sessionId, userId, conversationId, agentType);
        if (StrUtil.isNotBlank(response.getContent())) {
            onContent.accept(response.getContent());
        }
        onComplete.accept(null);
    }

    private void verifySdkGetWsToken(String appKey, String visitorId) {
        if (StrUtil.isBlank(secretId)) {
            return;
        }
        GetWsTokenRequest req = new GetWsTokenRequest();
        req.setType(5L);
        req.setBotAppKey(appKey);
        req.setVisitorBizId(visitorId);
        try {
            lkeClient.GetWsToken(req);
        } catch (TencentCloudSDKException e) {
            // GetWsToken 依赖密钥签名，未配置时不阻断对话主链路（HTTP SSE 仅 AppKey 即可调用）。
            log.warn("LKE GetWsToken verify skipped: {}", e.getMessage());
        }
    }

    private String buildChatPayload(String requestId, String conversationId, String appKey, String visitorId, String userMessage) {
        JSONObject content = new JSONObject();
        content.put("Type", "text");
        content.put("Text", userMessage);
        JSONArray contents = new JSONArray();
        contents.add(content);

        JSONObject payload = new JSONObject();
        payload.put("RequestId", requestId);
        payload.put("ConversationId", conversationId);
        payload.put("AppKey", appKey);
        payload.put("VisitorId", visitorId);
        payload.put("Contents", contents);
        payload.put("Incremental", true);
        payload.put("EnableMultiIntent", true);
        payload.put("Stream", "enable");
        return payload.toString();
    }

    private String callSse(String payload, String sessionId) {
        try {
            URL url = new URL(chatUrl);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(readTimeoutMs);
            conn.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            int status = conn.getResponseCode();
            String body = readConnectionBody(conn, status);
            if (status < 200 || status >= 300) {
                throw mapHttpError(status, body, sessionId);
            }
            return body;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Tencent LKE chat request failed, sessionId={}", sessionId, e);
            if (e instanceof SocketTimeoutException || isTimeoutMessage(e.getMessage())) {
                throw new BusinessException(BizErrorCodes.UPSTREAM_TIMEOUT, "智能体响应超时，请稍后再试。");
            }
            throw new BusinessException(BizErrorCodes.UNKNOWN_SYSTEM, "智能体调用失败，请稍后再试。");
        }
    }

    private static String readConnectionBody(HttpsURLConnection conn, int status) throws IOException {
        InputStream in = status >= 200 && status < 300
                ? conn.getInputStream()
                : (conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream());
        if (in == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }

    private BusinessException mapHttpError(int status, String body, String sessionId) {
        String preview = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        if (preview.length() > 500) {
            preview = preview.substring(0, 500) + "...(truncated)";
        }
        log.warn("LKE HTTP error status={}, sessionId={}, bodyPreview={}", status, sessionId, preview);
        if (status == 429) {
            return new BusinessException(BizErrorCodes.UPSTREAM_BUSY_OR_RATE_LIMIT, "服务繁忙，请稍后再试。");
        }
        if (status == 401 || status == 403) {
            return new BusinessException(BizErrorCodes.UNAUTHORIZED, "鉴权失败，请检查配置。");
        }
        if (status >= 500) {
            return new BusinessException(BizErrorCodes.UPSTREAM_BUSY_OR_RATE_LIMIT, "智能体服务繁忙，请稍后再试。");
        }
        if (status >= 400) {
            return new BusinessException(BizErrorCodes.BUSINESS_EXPECTED_FAILURE, "智能体调用被拒绝，请稍后再试。");
        }
        return new BusinessException(BizErrorCodes.UNKNOWN_SYSTEM, "智能体调用失败，请稍后再试。");
    }

    private static boolean isTimeoutMessage(String msg) {
        if (msg == null) {
            return false;
        }
        String m = msg.toLowerCase();
        return m.contains("timeout") || m.contains("timed out") || msg.contains("超时");
    }

    private String extractAssistantTextFromSse(String sseBody, String sessionId) {
        StringBuilder delta = new StringBuilder();
        String completedText = null;
        boolean completedEventSeen = false;
        String traceId = "";
        String[] lines = sseBody.split("\\r?\\n");
        for (String line : lines) {
            if (!line.startsWith("data:")) {
                continue;
            }
            String data = line.substring(5).trim();
            if (data.isEmpty() || "[DONE]".equals(data)) {
                continue;
            }
            JSONObject obj;
            try {
                obj = JSONUtil.parseObj(data);
            } catch (Exception ignore) {
                continue;
            }
            String type = obj.getStr("Type");
            if ("error".equalsIgnoreCase(type)) {
                JSONObject err = obj.getJSONObject("Error");
                String code = err != null ? String.valueOf(err.get("Code")) : "unknown";
                String message = err != null ? err.getStr("Message") : "unknown error";
                String errTraceId = err != null ? err.getStr("TraceId") : "";
                throw mapLkeSseError(code, message, errTraceId, sessionId);
            }
            if (StrUtil.isBlank(traceId)) {
                traceId = obj.getStr("TraceId", "");
            }
            if ("text.delta".equals(type) || "text.replace".equals(type)) {
                String text = obj.getStr("Text");
                if (StrUtil.isNotBlank(text)) {
                    delta.append(text);
                }
            } else if ("response.completed".equals(type)) {
                completedEventSeen = true;
                JSONObject response = obj.getJSONObject("Response");
                if (response == null) {
                    continue;
                }
                JSONArray messages = response.getJSONArray("Messages");
                if (messages == null) {
                    continue;
                }
                for (int i = 0; i < messages.size(); i++) {
                    JSONObject msg = messages.getJSONObject(i);
                    if (msg == null || !"reply".equals(msg.getStr("Type"))) {
                        continue;
                    }
                    JSONArray contents = msg.getJSONArray("Contents");
                    if (contents == null) {
                        continue;
                    }
                    for (int j = 0; j < contents.size(); j++) {
                        JSONObject c = contents.getJSONObject(j);
                        if (c != null && "text".equals(c.getStr("Type"))) {
                            completedText = c.getStr("Text");
                        }
                    }
                }
            }
        }
        String raw = StrUtil.isNotBlank(completedText) ? completedText : delta.toString();
        if (StrUtil.isBlank(raw)) {
            String preview = sseBody == null ? "" : sseBody.replaceAll("\\s+", " ").trim();
            int maxLen = 1200;
            if (preview.length() > maxLen) {
                preview = preview.substring(0, maxLen) + "...(truncated)";
            }
            log.warn("LKE empty text payload. traceId={}, sessionId={}, ssePreview={}", traceId, sessionId, preview);
            throw new BusinessException(BizErrorCodes.BUSINESS_EXPECTED_FAILURE, "智能体未返回有效内容，请稍后再试。");
        }
        if (!completedEventSeen) {
            // Some requests may timeout before completed event arrives; fallback to collected delta text.
            log.warn("LKE response.completed not received, fallback to accumulated delta. traceId={}, sessionId={}", traceId, sessionId);
        }
        return flowMarkerApplier.apply(raw, sessionId).trim();
    }

    private BusinessException mapLkeSseError(String code, String message, String traceId, String sessionId) {
        log.warn("LKE SSE type=error code={}, traceId={}, sessionId={}, message={}", code, traceId, sessionId, message);
        int biz = classifyLkeError(code, message);
        return new BusinessException(biz, userMessageForLke(biz));
    }

    private static int classifyLkeError(String code, String message) {
        String m = (message == null ? "" : message).toLowerCase();
        String c = (code == null ? "" : code).toLowerCase();
        if (m.contains("quota") || m.contains("额度") || c.contains("quota") || c.contains("resourceinsufficient")) {
            return BizErrorCodes.QUOTA_EXCEEDED;
        }
        if (m.contains("rate") || m.contains("限流") || m.contains("throttl") || c.contains("429")) {
            return BizErrorCodes.UPSTREAM_BUSY_OR_RATE_LIMIT;
        }
        if (m.contains("auth") || m.contains("鉴权") || m.contains("unauthor") || c.contains("auth")) {
            return BizErrorCodes.UNAUTHORIZED;
        }
        if (m.contains("timeout") || m.contains("超时")) {
            return BizErrorCodes.UPSTREAM_TIMEOUT;
        }
        return BizErrorCodes.BUSINESS_EXPECTED_FAILURE;
    }

    private static String userMessageForLke(int biz) {
        if (biz == BizErrorCodes.QUOTA_EXCEEDED) {
            return "额度已用尽或受限，请稍后再试。";
        }
        if (biz == BizErrorCodes.UPSTREAM_BUSY_OR_RATE_LIMIT) {
            return "服务繁忙，请稍后再试。";
        }
        if (biz == BizErrorCodes.UNAUTHORIZED) {
            return "鉴权失败，请检查配置。";
        }
        if (biz == BizErrorCodes.UPSTREAM_TIMEOUT) {
            return "智能体响应超时，请稍后再试。";
        }
        return "智能体调用失败，请稍后再试。";
    }

    private String normalizeConversationId(String source) {
        String base = source == null ? UUID.randomUUID().toString() : source;
        String cleaned = base.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (cleaned.length() < 32) {
            cleaned = (cleaned + "________________________________").substring(0, 32);
        }
        if (cleaned.length() > 64) {
            cleaned = cleaned.substring(0, 64);
        }
        return cleaned;
    }

    private String normalizeVisitorId(String userId, String sessionId) {
        String base = StrUtil.isNotBlank(userId) ? userId : sessionId;
        if (StrUtil.isBlank(base)) {
            base = UUID.randomUUID().toString();
        }
        String cleaned = base.replaceAll("[^a-zA-Z0-9_-]", "_");
        return cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
    }
}
