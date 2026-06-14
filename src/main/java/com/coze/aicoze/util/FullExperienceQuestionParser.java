package com.coze.aicoze.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.coze.aicoze.dto.full.FullCareerTestQuestionDto;
import com.coze.aicoze.dto.full.FullSkillTestQuestionDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 完全版题目 JSON 解析（Hutool），逻辑对齐 {@link ExperienceQuestionParser}，并补充完全版可能多出的字段。
 */
public final class FullExperienceQuestionParser {

    private FullExperienceQuestionParser() {
    }

    /**
     * 解析完全版技能测试题目 JSON（根级可含「熟练度分布」「当前轮次」等）。
     */
    public static FullSkillTestQuestionDto parseSkillQuestions(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            String normalized = JsonInputNormalizer.normalizeJsonInput(json);
            if (normalized == null || normalized.isEmpty()) {
                return null;
            }
            JSONObject root = JSONUtil.parseObj(normalized);
            JSONArray list = root.getJSONArray("题目列表");
            if (list == null) {
                return null;
            }
            FullSkillTestQuestionDto dto = new FullSkillTestQuestionDto();
            dto.setProficiencyDistribution(parseStringDoubleMap(root.getJSONObject("熟练度分布")));
            Integer round = root.getInt("当前轮次");
            if (round == null) {
                round = root.getInt("currentRound");
            }
            dto.setCurrentRound(round);

            List<FullSkillTestQuestionDto.SkillQuestionItem> items = new ArrayList<>();
            for (Object obj : list) {
                if (!(obj instanceof JSONObject)) {
                    continue;
                }
                JSONObject itemObj = (JSONObject) obj;
                FullSkillTestQuestionDto.SkillQuestionItem item = new FullSkillTestQuestionDto.SkillQuestionItem();
                item.setType(itemObj.getStr("类型"));
                item.setCoreSkill(itemObj.getStr("核心技能"));
                item.setStem(itemObj.getStr("题干"));
                item.setDifficulty(itemObj.getStr("难度", itemObj.getStr("difficulty")));
                item.setCorrectAnswer(itemObj.getStr("正确答案"));
                item.setRequirement(itemObj.getStr("要求"));

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
                items.add(item);
            }
            dto.setQuestionList(items);
            return dto;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析完全版霍兰德题目 JSON（题目项可含「主维度」「次维度」等）。
     */
    public static FullCareerTestQuestionDto parseCareerQuestions(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            String normalized = JsonInputNormalizer.normalizeJsonInput(json);
            if (normalized == null || normalized.isEmpty()) {
                return null;
            }
            JSONObject root = JSONUtil.parseObj(normalized);
            JSONArray list = root.getJSONArray("questions");
            if (list == null) {
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
                item.setType(itemObj.getStr("类型"));
                item.setStem(itemObj.getStr("题干"));
                item.setPrimaryDimension(itemObj.getStr("主维度", itemObj.getStr("primaryDimension")));
                item.setSecondaryDimension(itemObj.getStr("次维度", itemObj.getStr("secondaryDimension")));

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
                        options.add(opt);
                    }
                    item.setOptions(options);
                }
                items.add(item);
            }
            dto.setQuestions(items);
            return dto;
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<String, Double> parseStringDoubleMap(JSONObject obj) {
        if (obj == null || obj.isEmpty()) {
            return null;
        }
        Map<String, Double> map = new LinkedHashMap<>();
        for (String key : obj.keySet()) {
            Double v = obj.getDouble(key);
            if (v != null) {
                map.put(key, v);
            }
        }
        return map.isEmpty() ? null : map;
    }
}
