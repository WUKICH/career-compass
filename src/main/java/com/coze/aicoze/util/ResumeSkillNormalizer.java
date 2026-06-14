package com.coze.aicoze.util;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 简历解析产出的 skill 字段后处理：去重、剔除句子式描述、最多保留 5 项核心技能。
 */
public final class ResumeSkillNormalizer {

    public static final int MAX_CORE_SKILLS = 5;

    private static final Pattern DELIM_IN_TEXT = Pattern.compile("[，,、;；]");
    private static final Pattern SPLIT_BY_DELIM = Pattern.compile("[，,、;；]+");
    private static final Pattern SPLIT_BY_SPACE = Pattern.compile("\\s+");
    /** 去掉尾部「:数字」评分片段（模型偶发输出） */
    private static final Pattern TRAILING_SCORE = Pattern.compile("[:：]\\s*\\d+(?:\\.\\d+)?\\s*$");
    private static final Pattern SENTENCE_LIKE = Pattern.compile(".*(可以|能够|善于|良好|灵活|各种|问题).*[。.]?$");

    private ResumeSkillNormalizer() {
    }

    public static String normalize(String rawSkill) {
        if (StrUtil.isBlank(rawSkill)) {
            return "";
        }
        String trimmed = rawSkill.trim();
        if ("未明确".equals(trimmed)) {
            return trimmed;
        }
        List<String> parts = new ArrayList<>();
        for (String segment : splitSegments(trimmed)) {
            String name = normalizeOne(segment);
            if (StrUtil.isNotBlank(name)) {
                parts.add(name);
            }
        }
        if (parts.isEmpty()) {
            return "";
        }
        Set<String> seen = new LinkedHashSet<>();
        List<String> ordered = new ArrayList<>();
        for (String part : parts) {
            String key = part.toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                continue;
            }
            ordered.add(part);
            if (ordered.size() >= MAX_CORE_SKILLS) {
                break;
            }
        }
        return String.join("，", ordered);
    }

    /** 有标点分隔符按标点拆，否则按空白拆（与前端 parseSkillNamesFromProfile 一致）。 */
    static String[] splitSegments(String trimmed) {
        if (DELIM_IN_TEXT.matcher(trimmed).find()) {
            return SPLIT_BY_DELIM.split(trimmed);
        }
        return SPLIT_BY_SPACE.split(trimmed);
    }

    private static String normalizeOne(String segment) {
        if (StrUtil.isBlank(segment)) {
            return "";
        }
        String name = segment.trim();
        name = TRAILING_SCORE.matcher(name).replaceAll("").trim();
        if (name.isEmpty() || "未明确".equals(name)) {
            return "";
        }
        if (name.length() > 24 || SENTENCE_LIKE.matcher(name).matches()) {
            return "";
        }
        return name;
    }
}
