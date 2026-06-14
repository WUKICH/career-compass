package com.coze.aicoze.vo;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.util.List;

/**
 * 霍兰德完成后、共识前的初步岗位推荐（JSON）。
 */
@Data
public class PreliminaryRecommendationVO {

    /** ready | error */
    private String status;

    @Alias("引导语")
    private String intro;

    @Alias("推荐岗位")
    private List<RecommendedRole> recommendedRoles;

    @Alias("确认提问")
    private String confirmQuestion;

    @Data
    public static class RecommendedRole {
        @Alias("岗位名称")
        private String title;

        @Alias("匹配理由")
        private String reason;
    }
}
