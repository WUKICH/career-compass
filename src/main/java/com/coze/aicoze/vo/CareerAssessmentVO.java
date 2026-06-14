package com.coze.aicoze.vo;

import cn.hutool.core.annotation.Alias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class CareerAssessmentVO {
    private String name;

    private String targetJob;

    private String targetCity;

    private String expectedSalary;

    @Alias("核心技能评估")
    private List<SkillAssessment> skillAssessments;
    
    @Alias("霍兰德测试结果")
    private CareerInterestResult careerInterestResult;
    
    @Alias("推荐职位")
    private List<PositionsItem> recommendedPositions;

    /**
     * 仅当用户经简历上传采集且对照 Agent 配置成功时填充；前端可据此展示「简历优化」卡片。
     */
    @Alias("简历优化建议")
    private ResumeOptimizationSection resumeOptimization;

    @Data
    public static class SkillAssessment {
        @Alias("技能")
        private String skill;
        
        @Alias("初始评分")
        private Integer score;

        @Alias("实测评分")
        private String practiceScore;
    }
    
    @Data
    public static class CareerInterestResult {
        @Alias("代码")
        private List<String> codes;
        
        @Alias("描述")
        private String description;
    }

    @Data
    public static class PositionsItem{
        @Alias("公司名")
        @JsonProperty("公司名称")
        private String compName;
        @Alias("公司规模")
        @JsonProperty("公司规模")
        private String compScale;
        @Alias("工作地点")
        @JsonProperty("工作地点")
        private String dq;
        @Alias("岗位名称")
        @JsonProperty("岗位名称")
        private String title;
        @Alias("联系人")
        @JsonProperty("联系人")
        private String recruiterName;
        @Alias("职位薪资")
        @JsonProperty("职位薪资")
        private String salary;
        @Alias("公司行业")
        @JsonProperty("公司行业")
        private String compIndustry;
        @Alias("公司阶段")
        @JsonProperty("公司阶段")
        private String compStage;
        @Alias("职位福利")
        @JsonProperty("职位福利")
        private List<String> labels;

        /** 知识图谱推荐接口返回的匹配得分，供前端展示或排序 */
        @Alias("匹配度")
        @JsonProperty("匹配度")
        private Double matchDegree;

        /** 站外职位详情页链接，前端可做新窗口跳转 */
        @Alias("招聘链接")
        @JsonProperty("招聘链接")
        private String recruitmentUrl;
    }

    @Data
    public static class ResumeOptimizationSection {
        @Alias("摘要")
        private String summary;

        @Alias("建议项")
        private List<ResumeOptimizationItem> items;
    }

    @Data
    public static class ResumeOptimizationItem {
        @Alias("简历表述")
        private String resumeWording;

        @Alias("测评依据")
        private String assessmentBasis;

        @Alias("优化建议")
        private String suggestion;
    }

}