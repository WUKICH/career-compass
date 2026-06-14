package com.coze.aicoze.full.flow;

import org.springframework.stereotype.Component;

/**
 * 解析 Coze 回复中的流程标记：仅剥离 marker 文本，不修改会话 mode（由 WorkflowEngine 控制）。
 */
@Component
public class FlowMarkerApplier {

    public String apply(String content, String sessionId) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        return stripAllMarkers(content);
    }

    private static String stripAllMarkers(String content) {
        String cleaned = content;
        cleaned = stripAll(cleaned, "CHAT_END", "SKILL_TEST", "SKILL_END", "MBTI_TEST", "MBTI_END", "HOLLAND_TEST", "HOLLAND_END");
        return cleaned;
    }

    private static String stripAll(String s, String... markers) {
        String out = s;
        for (String m : markers) {
            out = out.replace("[" + m + "]", "").replace(m, "");
        }
        return out;
    }
}
