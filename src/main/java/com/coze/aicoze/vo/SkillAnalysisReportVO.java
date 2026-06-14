package com.coze.aicoze.vo;

import cn.hutool.core.annotation.Alias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SkillAnalysisReportVO {

    private String name;

    private String targetJob;

    private String skill;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime testTime;

    @Alias("题目总数")
    private Integer totalQuestions;

    @Alias("答对题数")
    private Integer correctCount;

    @Alias("正确率")
    private Integer accuracy;

    @Alias("测试综合得分")
    private Integer totalScore;

    @Alias("总结")
    private String summary;

    @Alias("核心技能分析")
    private List<SkillAnalysisVO> skillAnalysis;

    @Data
    public static class SkillAnalysisVO {
        @Alias("核心技能")
        private String skillName;

        @Alias("评分")
        private Integer score;

        @Alias("评价")
        private String evaluation;
    }
}
