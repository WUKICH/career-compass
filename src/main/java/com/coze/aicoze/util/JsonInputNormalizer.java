package com.coze.aicoze.util;

/**
 * Normalize model-provided JSON payloads（去 Markdown 围栏、按首末大括号粗切一段）。
 * <p>
 * Handles common wrappers like:
 * - ```json { ... } ```
 * - ``` { ... } ```
 * - extra text before/after the outermost {...}
 * <p>
 * 若同一回复里多个根级对象或需避免「首末括号」误切嵌套结构，请改用
 * {@link CozeJsonExtractor#extractLastJsonObject(String)}。
 */
public final class JsonInputNormalizer {

    private JsonInputNormalizer() {
    }

    public static String normalizeJsonInput(String raw) {
        if (raw == null) {
            return null;
        }

        String s = raw.trim();
        if (s.isEmpty()) {
            return s;
        }

        // Remove fenced code blocks: ```json ... ``` or ``` ... ```
        int fenceStart = s.indexOf("```json");
        if (fenceStart != -1) {
            int start = fenceStart + "```json".length();
            int fenceEnd = s.indexOf("```", start);
            if (fenceEnd != -1) {
                s = s.substring(start, fenceEnd).trim();
            } else {
                s = s.substring(start).trim();
            }
        } else if (s.startsWith("```")) {
            int start = 3;
            int firstNewline = s.indexOf('\n', start);
            int fenceEnd = s.lastIndexOf("```");
            if (fenceEnd > start) {
                // Skip optional language marker line like: ```json
                if (firstNewline != -1 && firstNewline < fenceEnd) {
                    String firstLine = s.substring(start, firstNewline).trim();
                    if (!firstLine.isEmpty()
                            && firstLine.length() <= 12
                            && firstLine.matches("[A-Za-z0-9_-]+")) {
                        start = firstNewline + 1;
                    }
                }
                s = s.substring(start, fenceEnd).trim();
            }
        }

        // Extract the outermost {...} if there is extra text.
        // 注意：题干里可能嵌入代码（包含 { } ），不能用 indexOf/lastIndexOf 直接裁剪。
        // 使用「最后一个根级完整 JSON 对象」提取器，忽略 JSON 字符串内的花括号。
        String extracted = CozeJsonExtractor.extractLastJsonObject(s);
        if (extracted != null) {
            s = extracted.trim();
        } else {
            int firstBrace = s.indexOf('{');
            int lastBrace = s.lastIndexOf('}');
            if (firstBrace != -1 && lastBrace != -1 && firstBrace < lastBrace) {
                s = s.substring(firstBrace, lastBrace + 1).trim();
            }
        }

        return s;
    }
}

