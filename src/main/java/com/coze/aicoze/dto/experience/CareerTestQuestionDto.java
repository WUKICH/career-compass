package com.coze.aicoze.dto.experience;

import lombok.Data;

import java.util.List;

/**
 * 霍兰德职业兴趣测试题目整体结构
 */
@Data
public class CareerTestQuestionDto {

    /**
     * 对应 JSON 根对象中的 "questions"
     */
    private List<CareerQuestionItem> questions;

    @Data
    public static class CareerQuestionItem {
        /**
         * 题目类型：选择题
         */
        private String type;

        /**
         * 题干
         */
        private String stem;

        /**
         * 选项
         */
        private List<CareerQuestionOption> options;

        /** 是否多选题 */
        private Boolean multiSelect;
    }

    @Data
    public static class CareerQuestionOption {
        private String key;
        private String text;
    }
}

