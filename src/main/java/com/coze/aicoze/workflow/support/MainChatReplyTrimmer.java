package com.coze.aicoze.workflow.support;

import cn.hutool.core.util.StrUtil;

/**
 * 主聊首轮回复软截断，避免 Bot 过长输出影响体验。
 */
public final class MainChatReplyTrimmer {

    private static final String TRUNCATION_SUFFIX = "…（回复较长，可继续提问）";

    private MainChatReplyTrimmer() {
    }

    public static String trimIfNeeded(String content, int maxChars) {
        if (StrUtil.isBlank(content) || maxChars <= 0) {
            return content;
        }
        String trimmed = content.strip();
        if (trimmed.length() <= maxChars) {
            return trimmed;
        }
        int cutAt = maxChars;
        if (cutAt + TRUNCATION_SUFFIX.length() > trimmed.length()) {
            cutAt = Math.max(0, trimmed.length() - TRUNCATION_SUFFIX.length());
        }
        return trimmed.substring(0, cutAt).stripTrailing() + TRUNCATION_SUFFIX;
    }
}
