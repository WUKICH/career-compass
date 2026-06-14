package com.coze.aicoze.vo;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.util.List;

/**
 * 共识阶段 Bot 输出（JSON）。status=continue 时仅含回复；status=complete 时含共识结论。
 */
@Data
public class ConsensusResultVO {

    /** continue | complete */
    private String status;

    @Alias("回复")
    private String reply;

    @Alias("共识结论")
    private ConsensusSummary consensusSummary;

    @Alias("推荐职位")
    private List<CareerAssessmentVO.PositionsItem> recommendedPositions;

    @Data
    public static class ConsensusSummary {
        @Alias("首选岗位方向")
        private String preferredDirection;

        @Alias("用户偏好关键词")
        private List<String> preferenceKeywords;

        @Alias("排除岗位")
        private List<String> excludedRoles;

        @Alias("备注")
        private String notes;
    }
}
