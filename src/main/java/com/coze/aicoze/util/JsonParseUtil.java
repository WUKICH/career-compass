package com.coze.aicoze.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.haibiiin.json.repair.JSONRepair;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * JSON 解析工具（基于 Hutool）。
 *
 * <p>目标：</p>
 * - 收拢项目内重复的 try/catch 与净化逻辑
 * - 让调用方自己决定：解析失败是吞掉（返回 null）还是抛业务异常
 */
@Slf4j
public final class JsonParseUtil {

    private static final JSONRepair LLM_JSON_REPAIR = new JSONRepair();

    /** 解析失败时写入日志的 JSON 最大长度，避免单条日志过大。 */
    private static final int FAILED_JSON_LOG_MAX_CHARS = 4_000;

    private JsonParseUtil() {
    }

    /**
     * 解析失败时记录原文、规范化结果及最后一次尝试的候选串（均截断）。
     */
    public static void logJsonParseFailure(
            String label,
            String rawJson,
            String normalizedJson,
            String lastCandidate,
            Exception cause) {
        if (!log.isWarnEnabled()) {
            return;
        }
        String err = cause == null ? "unknown" : cause.getMessage();
        log.warn(
                "{}，error={}，rawLen={}，normalizedLen={}，lastCandidateLen={}，raw={}，normalized={}，lastCandidate={}",
                label,
                err,
                lengthOf(rawJson),
                lengthOf(normalizedJson),
                lengthOf(lastCandidate),
                truncateForFailedJsonLog(rawJson),
                truncateForFailedJsonLog(normalizedJson),
                truncateForFailedJsonLog(lastCandidate),
                cause);
    }

    public static String truncateForFailedJsonLog(String json) {
        if (json == null) {
            return "<null>";
        }
        if (json.length() <= FAILED_JSON_LOG_MAX_CHARS) {
            return json;
        }
        return json.substring(0, FAILED_JSON_LOG_MAX_CHARS)
                + "...[truncated, total " + json.length() + " chars]";
    }

    private static int lengthOf(String s) {
        return s == null ? 0 : s.length();
    }

    /**
     * 解析为 Bean：失败返回 null（不抛异常）。
     *
     * <p>会按序尝试：原始文本 → 标点/尾随逗号/串内引号修复 → Jackson 规范化（{@link JsonSanitizerUtil#prepareModelJson}）
     * → 题目用结构修复规范化（{@link JsonSanitizerUtil#prepareModelJsonWithStructuralRepair}）→ {@code json-repair} 库。</p>
     */
    public static <T> T toBeanQuiet(String json, Class<T> clazz) {
        if (StrUtil.isBlank(json) || clazz == null) {
            return null;
        }
        String normalized = JsonInputNormalizer.normalizeJsonInput(json);
        Exception last = null;
        String lastCandidate = null;
        for (String candidate : questionJsonCandidates(StrUtil.isNotBlank(normalized) ? normalized : json.trim())) {
            if (StrUtil.isBlank(candidate)) {
                continue;
            }
            lastCandidate = candidate;
            try {
                return JSONUtil.toBean(candidate, clazz);
            } catch (Exception e) {
                last = e;
            }
        }
        if (last != null) {
            logJsonParseFailure(
                    "JSON 解析失败（已尝试净化与 LLM JSON 修复），目标类型=" + clazz.getSimpleName(),
                    json,
                    normalized,
                    lastCandidate,
                    last);
        }
        return null;
    }

    /**
     * Bot 题目 / 报告等 JSON 的解析候选串（去重、有序）：原始 → 本地修复 → prepare → 结构修复 → json-repair。
     */
    public static List<String> questionJsonCandidates(String json) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        String trim = json.trim();
        addCandidate(set, trim);

        String polished = JsonSanitizerUtil.applyLocalLlmRepairs(trim);
        addCandidate(set, polished);

        addCandidate(set, JsonSanitizerUtil.prepareModelJson(trim));
        addCandidate(set, JsonSanitizerUtil.prepareModelJson(polished));
        addCandidate(set, JsonSanitizerUtil.prepareModelJsonWithStructuralRepair(trim));
        addCandidate(set, JsonSanitizerUtil.prepareModelJsonWithStructuralRepair(polished));

        addJsonRepairCandidates(set, trim);
        addJsonRepairCandidates(set, polished);

        return new ArrayList<>(set);
    }

    private static void addJsonRepairCandidates(LinkedHashSet<String> set, String source) {
        if (StrUtil.isBlank(source)) {
            return;
        }
        try {
            String repaired = LLM_JSON_REPAIR.handle(source);
            if (StrUtil.isNotBlank(repaired)) {
                addCandidate(set, repaired);
                addCandidate(set, JsonSanitizerUtil.prepareModelJson(repaired));
                addCandidate(set, JsonSanitizerUtil.prepareModelJsonWithStructuralRepair(repaired));
            }
        } catch (Exception ignored) {
        }
    }

    private static void addCandidate(LinkedHashSet<String> set, String candidate) {
        if (StrUtil.isNotBlank(candidate)) {
            set.add(candidate);
        }
    }

    /**
     * 依次取键，返回第一个非空字符串；无则 {@code null}。
     */
    public static String firstNonBlank(JSONObject obj, String... keys) {
        if (obj == null || keys == null) {
            return null;
        }
        for (String k : keys) {
            if (k == null) {
                continue;
            }
            String v = obj.getStr(k);
            if (StrUtil.isNotBlank(v)) {
                return v;
            }
        }
        return null;
    }

    /**
     * 先净化再解析为 JSONObject：失败返回 null（不抛异常）。
     */
    public static JSONObject parseObjSanitizedQuiet(String rawJson) {
        if (StrUtil.isBlank(rawJson)) {
            return null;
        }
        try {
            String sanitized = JsonSanitizerUtil.sanitize(rawJson);
            if (StrUtil.isBlank(sanitized)) {
                return null;
            }
            return JSONUtil.parseObj(sanitized);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String getStr(JSONObject obj, String primaryKey, String fallbackKey) {
        if (obj == null) {
            return null;
        }
        String v = obj.getStr(primaryKey);
        if (StrUtil.isNotBlank(v)) {
            return v;
        }
        return fallbackKey == null ? v : obj.getStr(fallbackKey);
    }
}

