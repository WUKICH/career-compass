package com.coze.aicoze.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ReportDTO {
    @JsonProperty("题目总数")
    private Integer num;

    @JsonProperty("答对题数")
    private Integer correctNum;

    @JsonProperty("正确率")
    private Integer accuracy;

    @JsonProperty("测试综合得分")
    private Integer synthesisScore;

    @JsonProperty("总结")
    private String summary;

    @JsonProperty("核心技能分析")
    private List<SkillAnalysisDTO> skillAnalysis;

    @JsonProperty("学习建议")
    private String 学习建议;
    
    // 嵌套DTO类
    @Data
    public static class SkillAnalysisDTO {
        @JsonProperty("核心技能")
        private String skill;
        
        @JsonProperty("评分")
        private Integer score;
        
        @JsonProperty("评价")
        private String evaluation;
    }
}
