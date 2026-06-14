package com.coze.aicoze.util;

import cn.hutool.core.util.StrUtil;

/** Coze 模型输出中的 JSON 提取与「系统 JSON 片段」识别。 */
public final class CozeJsonExtractor {

    private CozeJsonExtractor() {
    }

    /**
     * 从混杂文本中提取“最后一个完整的根级 JSON 对象”。
     *
     * <p>常见场景：</p>
     * - 模型会在 JSON 前后输出说明文字
     * - Coze 可能拼接多个 JSON：{@code {}{"a":1}}
     *
     * <p>注意：这里提取的是“根级”对象，避免误把数组里的最后一个子对象当成结果，导致丢失根级字段。</p>
     */
    public static String extractLastJsonObject(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        String trimmed = content.trim();

        // 更稳健的提取策略：
        // - 扫描整段文本，在字符串外统计 {} 深度
        // - 每次深度回到 0 时，记录一个“根级完整对象”
        // - 最终返回最后一个根级对象
        int depth = 0;
        int start = -1;
        String lastRootJson = null;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);

            if (inString) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == '"') {
                    inString = false;
                }
                continue;
            }

            // not inString
            if (c == '"') {
                inString = true;
                continue;
            }

            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                if (depth > 0) {
                    depth--;
                    if (depth == 0 && start != -1) {
                        String jsonStr = trimmed.substring(start, i + 1);
                        if (jsonStr.trim().startsWith("{") && jsonStr.trim().endsWith("}")) {
                            lastRootJson = jsonStr;
                        }
                        start = -1;
                    }
                }
            }
        }

        return lastRootJson;
    }

    /**
     * 轮询消息中过滤“看起来像 JSON 的片段”（启发式判断）。
     */
    public static boolean isJsonContent(String content) {
        if (StrUtil.isBlank(content)) {
            return false;
        }

        String trimmed = content.trim();

        // 空 JSON 对象：{} / { }
        if (trimmed.matches("\\{\\s*}")) {
            return true;
        }

        // 一些系统字段（Coze 会在 messages 里夹杂）
        return trimmed.contains("\"msg_type\"")
                || trimmed.contains("\"from_module\"")
                || trimmed.contains("\"plugin\"")
                || trimmed.contains("\"recruiterPhoto\"");
    }
}

