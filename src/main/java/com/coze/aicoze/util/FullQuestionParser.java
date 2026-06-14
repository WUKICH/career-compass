package com.coze.aicoze.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.coze.aicoze.dto.full.FullCareerTestQuestionDto;
import com.coze.aicoze.dto.full.FullSkillTestQuestionDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 完全版 Bot 题目 JSON → {@link com.coze.aicoze.dto.full} 结构，与 {@link ExperienceQuestionParser} 并行。
 */
public final class FullQuestionParser {

    private FullQuestionParser() {
    }

    public static FullSkillTestQuestionDto parseSkillQuestions(String json) {
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
                return buildFullSkillFromNormalized(candidate);
            } catch (Exception e) {
                last = e;
                if (!JsonSanitizerUtil.isLikelyJsonSyntaxParseError(e)) {
                    JsonParseUtil.logJsonParseFailure("完全版技能测试题目 JSON 解析失败", json, n, lastCandidate, e);
                    return null;
                }
            }
        }
        if (last != null) {
            JsonParseUtil.logJsonParseFailure("完全版技能测试题目 JSON 解析失败", json, n, lastCandidate, last);
        }
        return null;
    }

    private static FullSkillTestQuestionDto buildFullSkillFromNormalized(String normalized) {
        JSONObject root = JSONUtil.parseObj(normalized);
        JSONArray list = root.getJSONArray("题目列表");
        if (list == null) {
            list = root.getJSONArray("questionList");
        }
        if (list == null) {
            return null;
        }
        FullSkillTestQuestionDto dto = new FullSkillTestQuestionDto();
        JSONObject distObj = root.getJSONObject("熟练度分布");
        if (distObj == null || distObj.isEmpty()) {
            distObj = root.getJSONObject("proficiencyDistribution");
        }
        if (distObj != null && !distObj.isEmpty()) {
            Map<String, Double> dist = new HashMap<>();
            for (String key : distObj.keySet()) {
                dist.put(key, distObj.getDouble(key));
            }
            dto.setProficiencyDistribution(dist);
        }
        Integer currentRound = root.getInt("当前轮次");
        if (currentRound == null) {
            currentRound = root.getInt("currentRound");
        }
        dto.setCurrentRound(currentRound);
        List<FullSkillTestQuestionDto.SkillQuestionItem> items = new ArrayList<>();
        for (Object obj : list) {
            if (!(obj instanceof JSONObject)) {
                continue;
            }
            JSONObject itemObj = (JSONObject) obj;
            FullSkillTestQuestionDto.SkillQuestionItem item = new FullSkillTestQuestionDto.SkillQuestionItem();
            item.setType(JsonParseUtil.firstNonBlank(itemObj, "类型", "type"));
            item.setCoreSkill(JsonParseUtil.firstNonBlank(itemObj, "核心技能", "coreSkill"));
            item.setStem(JsonParseUtil.firstNonBlank(itemObj, "题干", "stem"));
            item.setDifficulty(JsonParseUtil.firstNonBlank(itemObj, "难度", "难度类型", "difficulty", "difficultyType"));
            item.setCorrectAnswer(JsonParseUtil.firstNonBlank(itemObj, "正确答案", "correctAnswer"));
            item.setRequirement(JsonParseUtil.firstNonBlank(itemObj, "要求", "requirement"));
            JSONArray optionArr = itemObj.getJSONArray("选项");
            if (optionArr == null) {
                optionArr = itemObj.getJSONArray("options");
            }
            if (optionArr != null) {
                List<FullSkillTestQuestionDto.SkillQuestionOption> options = new ArrayList<>();
                for (Object optObj : optionArr) {
                    if (!(optObj instanceof JSONObject)) {
                        continue;
                    }
                    JSONObject optJson = (JSONObject) optObj;
                    FullSkillTestQuestionDto.SkillQuestionOption opt = new FullSkillTestQuestionDto.SkillQuestionOption();
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

    public static FullCareerTestQuestionDto parseCareerQuestions(String json) {
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
                return buildFullCareerFromNormalized(candidate);
            } catch (Exception e) {
                last = e;
                if (!JsonSanitizerUtil.isLikelyJsonSyntaxParseError(e)) {
                    JsonParseUtil.logJsonParseFailure("完全版职业兴趣测试题目 JSON 解析失败", json, n, lastCandidate, e);
                    return null;
                }
            }
        }
        if (last != null) {
            JsonParseUtil.logJsonParseFailure("完全版职业兴趣测试题目 JSON 解析失败", json, n, lastCandidate, last);
        }
        return null;
    }

    private static FullCareerTestQuestionDto buildFullCareerFromNormalized(String normalized) {
        String trimmed = normalized.trim();
        JSONArray list;
        if (trimmed.startsWith("[")) {
            list = JSONUtil.parseArray(normalized);
        } else {
            JSONObject root = JSONUtil.parseObj(normalized);
            list = root.getJSONArray("questions");
            if (list == null) {
                list = root.getJSONArray("questionList");
            }
            if (list == null) {
                list = root.getJSONArray("题目列表");
            }
        }
        if (list == null || list.isEmpty()) {
            return null;
        }
        FullCareerTestQuestionDto dto = new FullCareerTestQuestionDto();
        List<FullCareerTestQuestionDto.CareerQuestionItem> items = new ArrayList<>();
        for (Object obj : list) {
            if (!(obj instanceof JSONObject)) {
                continue;
            }
            JSONObject itemObj = (JSONObject) obj;
            FullCareerTestQuestionDto.CareerQuestionItem item = new FullCareerTestQuestionDto.CareerQuestionItem();
            item.setType(JsonParseUtil.firstNonBlank(itemObj, "类型", "type"));
            item.setStem(JsonParseUtil.firstNonBlank(itemObj, "题干", "stem"));
            item.setPrimaryDimension(JsonParseUtil.firstNonBlank(itemObj, "主维度", "primaryDimension"));
            item.setSecondaryDimension(JsonParseUtil.firstNonBlank(itemObj, "次维度", "secondaryDimension"));
            JSONArray optionArr = itemObj.getJSONArray("选项");
            if (optionArr == null) {
                optionArr = itemObj.getJSONArray("options");
            }
            if (optionArr != null) {
                List<FullCareerTestQuestionDto.CareerQuestionOption> options = new ArrayList<>();
                for (Object optObj : optionArr) {
                    if (!(optObj instanceof JSONObject)) {
                        continue;
                    }
                    JSONObject optJson = (JSONObject) optObj;
                    FullCareerTestQuestionDto.CareerQuestionOption opt = new FullCareerTestQuestionDto.CareerQuestionOption();
                    opt.setKey(optJson.getStr("key"));
                    opt.setText(optJson.getStr("text"));
                    if ((opt.getKey() == null || opt.getKey().isBlank())
                            && (opt.getText() == null || opt.getText().isBlank())) {
                        for (String k : optJson.keySet()) {
                            if (k != null && k.length() == 1) {
                                char c = k.charAt(0);
                                if (c >= 'A' && c <= 'Z') {
                                    opt.setKey(k);
                                    opt.setText(optJson.getStr(k));
                                    break;
                                }
                            }
                        }
                    }
                    options.add(opt);
                }
                item.setOptions(options);
            }
            item.setMultiSelect(SkillQuestionMultiSelectUtil.isMultiSelectCareer(itemObj, item.getType(), item.getStem()));
            items.add(item);
        }
        dto.setQuestions(items);
        return dto;
    }
}
