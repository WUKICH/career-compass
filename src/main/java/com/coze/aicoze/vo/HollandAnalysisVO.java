package com.coze.aicoze.vo;

import cn.hutool.core.annotation.Alias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HollandAnalysisVO {
    private String name;

    private String targetJob;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime testTime;
    
    @Alias("霍兰德职业兴趣代码")
    private String hollandCode;
    
    @Alias("类型组合")
    private String typeCombination;
    
    @Alias("霍兰德六个维度分析")
    private List<DimensionAnalysisVO> dimensionAnalysis;
    
    @Alias("与目标岗位匹配度分析")
    private JobMatchAnalysisVO jobMatchAnalysis;
    
    @Data
    public static class DimensionAnalysisVO {
        @Alias("类型")
        private String type;
        
        @Alias("得分")
        private String score;
        
        @Alias("倾向描述")
        private String description;
    }
    
    @Data
    public static class JobMatchAnalysisVO {
        @Alias("优势匹配")
        private MatchItemVO strengthMatch;
        
        @Alias("潜在优势")
        private MatchItemVO potentialAdvantage;
        
        @Alias("需关注方向")
        private MatchItemVO areaForAttention;
    }
    
    @Data
    public static class MatchItemVO {
        @Alias("标题")
        private String title;
        
        @Alias("描述")
        private String description;
    }
}