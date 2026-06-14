package com.coze.aicoze.dto.full;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 技能测试题目整体结构
 */
@Data
public class FullSkillTestQuestionDto {

    /**
     * 对应 JSON 根对象中的 "题目列表"
     */
    private List<SkillQuestionItem> questionList;

    /**
     * 熟练度分布（完全版 Bot 可能返回）
     */
    private Map<String, Double> proficiencyDistribution;

    /**
     * 当前轮次（完全版 Bot 可能返回）
     */
    private Integer currentRound;

    @Data
    public static class SkillQuestionItem {
        /**
         * 题目类型：选择题 / 主观题
         */
        private String type;

        /**
         * 核心技能
         */
        private String coreSkill;

        /**
         * 题干（可以包含 Markdown）
         */
        private String stem;

        /**
         * 难度（含 Bot 返回的「难度类型」）
         */
        private String difficulty;

        /**
         * 选项（仅选择题有）
         */
        private List<SkillQuestionOption> options;

        /**
         * 正确答案（仅选择题有）
         */
        private String correctAnswer;

        /**
         * 作答要求（仅主观题有）
         */
        @JsonIgnore
        private String requirement;

        /** 是否多选题 */
        private Boolean multiSelect;
    }

    @Data
    public static class SkillQuestionOption {
        private String key;
        private String text;
    }
}

