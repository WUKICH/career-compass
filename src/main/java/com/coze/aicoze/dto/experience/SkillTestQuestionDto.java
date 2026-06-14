package com.coze.aicoze.dto.experience;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

/**
 * 技能测试题目整体结构
 */
@Data
public class SkillTestQuestionDto {

    /**
     * 对应 JSON 根对象中的 "题目列表"
     */
    private List<SkillQuestionItem> questionList;

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

        /**
         * 是否多选题（后端根据题型/题干/Bot 字段推断）
         */
        private Boolean multiSelect;
    }

    @Data
    public static class SkillQuestionOption {
        private String key;
        private String text;
    }
}

