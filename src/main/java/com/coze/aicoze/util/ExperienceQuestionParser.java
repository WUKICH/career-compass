package com.coze.aicoze.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.coze.aicoze.dto.experience.CareerTestQuestionDto;
import com.coze.aicoze.dto.experience.SkillTestQuestionDto;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用 Hutool 将题目 JSON 解析为题目对象，供前端直接展示。
 * 放在 util 层，保持 DTO 纯净。
 */
public final class ExperienceQuestionParser {

    private ExperienceQuestionParser() {
    }

    /**
     * 解析技能测试题目 JSON
     */
    public static SkillTestQuestionDto parseSkillQuestions(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        String n = JsonInputNormalizer.normalizeJsonInput(json);
        if (n == null || n.isBlank()) {
            return null;
        }
        Exception last = null;
        String lastCandidate = null;
        for (String candidate : JsonParseUtil.questionJsonCandidates(n)) {
            lastCandidate = candidate;
            try {
                return buildSkillDtoFromNormalized(candidate);
            } catch (Exception e) {
                last = e;
                if (!JsonSanitizerUtil.isLikelyJsonSyntaxParseError(e)) {
                    JsonParseUtil.logJsonParseFailure("技能测试题目 JSON 解析失败", json, n, lastCandidate, e);
                    return null;
                }
            }
        }
        if (last != null) {
            JsonParseUtil.logJsonParseFailure("技能测试题目 JSON 解析失败", json, n, lastCandidate, last);
        }
        return null;
    }

    private static SkillTestQuestionDto buildSkillDtoFromNormalized(String normalized) {
        JSONObject root = JSONUtil.parseObj(normalized);
        JSONArray list = root.getJSONArray("题目列表");
        if (list == null) {
            list = root.getJSONArray("questionList");
        }
        if (list == null) {
            list = root.getJSONArray("questions");
        }
        if (list == null) {
            return null;
        }
        SkillTestQuestionDto dto = new SkillTestQuestionDto();
        List<SkillTestQuestionDto.SkillQuestionItem> items = new ArrayList<>();
        for (Object obj : list) {
            if (!(obj instanceof JSONObject)) {
                continue;
            }
            JSONObject itemObj = (JSONObject) obj;
            SkillTestQuestionDto.SkillQuestionItem item = new SkillTestQuestionDto.SkillQuestionItem();
            item.setType(JsonParseUtil.firstNonBlank(itemObj, "类型", "type"));
            item.setCoreSkill(JsonParseUtil.firstNonBlank(itemObj, "核心技能", "coreSkill"));
            item.setStem(JsonParseUtil.firstNonBlank(itemObj, "题干", "stem", "content"));
            item.setCorrectAnswer(JsonParseUtil.firstNonBlank(itemObj, "正确答案", "correctAnswer"));
            item.setRequirement(JsonParseUtil.firstNonBlank(itemObj, "要求", "requirement"));

            JSONArray optionArr = itemObj.getJSONArray("选项");
            if (optionArr == null) {
                optionArr = itemObj.getJSONArray("options");
            }
            if (optionArr != null) {
                List<SkillTestQuestionDto.SkillQuestionOption> options = new ArrayList<>();
                for (Object optObj : optionArr) {
                    if (!(optObj instanceof JSONObject)) {
                        continue;
                    }
                    JSONObject optJson = (JSONObject) optObj;
                    SkillTestQuestionDto.SkillQuestionOption opt = new SkillTestQuestionDto.SkillQuestionOption();
                    opt.setKey(optJson.getStr("key"));
                    opt.setText(optJson.getStr("text"));
                    options.add(opt);
                }
                item.setOptions(options);
            }
            item.setMultiSelect(SkillQuestionMultiSelectUtil.isMultiSelect(itemObj, item.getType(), item.getStem()));
            items.add(item);
        }
        dto.setQuestionList(items);
        return dto;
    }

    /**
     * 解析霍兰德测试题目 JSON
     */
    public static CareerTestQuestionDto parseCareerQuestions(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        String n = JsonInputNormalizer.normalizeJsonInput(json);
        if (n == null || n.isBlank()) {
            return null;
        }
        Exception last = null;
        String lastCandidate = null;
        for (String candidate : JsonParseUtil.questionJsonCandidates(n)) {
            lastCandidate = candidate;
            try {
                return buildCareerDtoFromNormalized(candidate);
            } catch (Exception e) {
                last = e;
                if (!JsonSanitizerUtil.isLikelyJsonSyntaxParseError(e)) {
                    JsonParseUtil.logJsonParseFailure("职业兴趣测试题目 JSON 解析失败", json, n, lastCandidate, e);
                    return null;
                }
            }
        }
        if (last != null) {
            JsonParseUtil.logJsonParseFailure("职业兴趣测试题目 JSON 解析失败", json, n, lastCandidate, last);
        }
        return null;
    }

    private static CareerTestQuestionDto buildCareerDtoFromNormalized(String normalized) {
        JSONObject root = JSONUtil.parseObj(normalized);
        JSONArray list = root.getJSONArray("questions");
        if (list == null) {
            list = root.getJSONArray("questionList");
        }
        if (list == null) {
            list = root.getJSONArray("题目列表");
        }
        if (list == null) {
            return null;
        }
        CareerTestQuestionDto dto = new CareerTestQuestionDto();
        List<CareerTestQuestionDto.CareerQuestionItem> items = new ArrayList<>();
        for (Object obj : list) {
            if (!(obj instanceof JSONObject)) {
                continue;
            }
            JSONObject itemObj = (JSONObject) obj;
            CareerTestQuestionDto.CareerQuestionItem item = new CareerTestQuestionDto.CareerQuestionItem();
            String type = JsonParseUtil.firstNonBlank(itemObj, "类型", "type");
            item.setType(StrUtil.isBlank(type) ? "选择题" : type);
            item.setStem(JsonParseUtil.firstNonBlank(itemObj, "题干", "stem", "content"));

            JSONArray optionArr = itemObj.getJSONArray("选项");
            if (optionArr == null) {
                optionArr = itemObj.getJSONArray("options");
            }
            if (optionArr != null) {
                List<CareerTestQuestionDto.CareerQuestionOption> options = new ArrayList<>();
                for (Object optObj : optionArr) {
                    if (optObj instanceof JSONObject optJson) {
                        CareerTestQuestionDto.CareerQuestionOption opt = new CareerTestQuestionDto.CareerQuestionOption();
                        opt.setKey(optJson.getStr("key"));
                        opt.setText(JsonParseUtil.firstNonBlank(optJson, "text", "内容", "label"));
                        options.add(opt);
                    } else if (optObj instanceof String optStr && StrUtil.isNotBlank(optStr)) {
                        CareerTestQuestionDto.CareerQuestionOption opt = parseLegacyOptionString(optStr.trim());
                        if (opt != null) {
                            options.add(opt);
                        }
                    }
                }
                item.setOptions(options);
            }
            item.setMultiSelect(SkillQuestionMultiSelectUtil.isMultiSelectCareer(itemObj, item.getType(), item.getStem()));
            items.add(item);
        }
        dto.setQuestions(items);
        return dto;
    }

    /** 兼容 "A. 选项正文" 字符串选项格式 */
    private static CareerTestQuestionDto.CareerQuestionOption parseLegacyOptionString(String raw) {
        if (StrUtil.isBlank(raw) || raw.length() < 2) {
            return null;
        }
        char keyChar = raw.charAt(0);
        if (keyChar < 'A' || keyChar > 'D') {
            return null;
        }
        String text = raw.substring(1).strip();
        if (text.startsWith(".") || text.startsWith("、")) {
            text = text.substring(1).strip();
        }
        CareerTestQuestionDto.CareerQuestionOption opt = new CareerTestQuestionDto.CareerQuestionOption();
        opt.setKey(String.valueOf(keyChar));
        opt.setText(text);
        return opt;
    }
}
