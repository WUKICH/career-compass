package com.coze.aicoze.util;

import cn.hutool.core.util.StrUtil;

/**
 * 工作年限：
 * <ul>
 *   <li><b>落库</b>（{@link com.coze.aicoze.entity.User#getWorkYears()}）：{@code 0}、{@code 1-3}、{@code 3-5}、{@code 5+}、{@code 其他}</li>
 *   <li><b>传输 / NLP / Prompt</b>：{@code 应届}、{@code 1-3年}、{@code 3-5年}、{@code 5年以上}、{@code 其他}（或「其他」下的自由说明）</li>
 * </ul>
 * 兼容：旧库单字符 {@code 0}–{@code 4}、旧选项串 {@code 1-3} 等与新旧中文选项互转。
 */
public final class WorkYearsCodec {

    /** 与库表、前端选项 value 一致的档位（非数字编码） */
    public static final String DB_FRESH = "0";
    public static final String DB_Y1_3 = "1-3";
    public static final String DB_Y3_5 = "3-5";
    public static final String DB_Y5_PLUS = "5+";
    public static final String DB_OTHER = "其他";

    private static final String[] DB_KEYS = {
            DB_FRESH, DB_Y1_3, DB_Y3_5, DB_Y5_PLUS, DB_OTHER
    };

    /** 给 NLP、会话、首条 Prompt 用的自然语言（与收集页 chip 文案一致时可带「年」） */
    private static final String[] NLP_LABELS = {
            "应届",
            "1-3年",
            "3-5年",
            "5年以上",
            "其他"
    };

    private WorkYearsCodec() {
    }

    /**
     * 任意入参（中文选项、旧档位、旧 0–4）→ 规范落库值 {@code 0} / {@code 1-3} / … / {@code 其他}。
     */
    public static String toDbValue(String raw) {
        if (StrUtil.isBlank(raw)) {
            return "";
        }
        String t = raw.trim();
        int idx = resolveIndex(t);
        if (idx >= 0) {
            return DB_KEYS[idx];
        }
        return DB_OTHER;
    }

    /**
     * 与 {@link #toDbValue(String)} 相同，保留旧方法名便于搜索。
     */
    public static String toStorageCode(String raw) {
        return toDbValue(raw);
    }

    /**
     * 落库值或任意已知入参 → 传输用中文（{@code 1-3} → {@code 1-3年}）。
     */
    public static String toDisplayLabel(String storageOrRaw) {
        if (StrUtil.isBlank(storageOrRaw)) {
            return "";
        }
        String t = storageOrRaw.trim();
        int idx = resolveIndex(t);
        if (idx >= 0) {
            return NLP_LABELS[idx];
        }
        return t;
    }

    /**
     * 会话 / Prompt：预设档位用标准中文；选「其他」且为自由说明时保留原文。
     */
    public static String toPromptText(String raw) {
        if (StrUtil.isBlank(raw)) {
            return "";
        }
        String t = raw.trim();
        String db = toDbValue(t);
        if (DB_OTHER.equals(db)) {
            boolean presetOther = DB_OTHER.equals(t) || NLP_LABELS[4].equals(t);
            if (!presetOther) {
                return t;
            }
        }
        return toDisplayLabel(db);
    }

    /**
     * 识别档位下标：先 NLP 文案，再落库串，再旧 0–4。
     */
    private static int resolveIndex(String t) {
        for (int i = 0; i < NLP_LABELS.length; i++) {
            if (NLP_LABELS[i].equals(t)) {
                return i;
            }
        }
        for (int i = 0; i < DB_KEYS.length; i++) {
            if (DB_KEYS[i].equals(t)) {
                return i;
            }
        }
        if (t.length() == 1) {
            char c = t.charAt(0);
            if (c >= '0' && c <= '4') {
                return c - '0';
            }
        }
        return -1;
    }
}
