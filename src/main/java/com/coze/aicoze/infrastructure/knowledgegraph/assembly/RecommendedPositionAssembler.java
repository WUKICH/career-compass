package com.coze.aicoze.infrastructure.knowledgegraph.assembly;

import com.coze.aicoze.infrastructure.knowledgegraph.dto.PositionsItem;
import com.coze.aicoze.vo.CareerAssessmentVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 知识图谱 /recommend 返回的职位条目 → 综合评价报告中的「推荐职位」列表。
 */
public final class RecommendedPositionAssembler {

    private RecommendedPositionAssembler() {
    }

    public static List<CareerAssessmentVO.PositionsItem> toCareerItems(List<PositionsItem> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<CareerAssessmentVO.PositionsItem> out = new ArrayList<>(raw.size());
        for (PositionsItem p : raw) {
            if (p == null) {
                continue;
            }
            CareerAssessmentVO.PositionsItem item = new CareerAssessmentVO.PositionsItem();
            item.setCompName(p.getCompName());
            item.setCompScale(p.getCompScale());
            item.setDq(p.getDq());
            item.setTitle(p.getTitle());
            item.setRecruiterName(p.getRecruiterName());
            item.setSalary(p.getSalary());
            item.setCompIndustry(p.getCompIndustry());
            item.setCompStage(p.getCompStage());
            item.setLabels(p.getLabels());
            item.setMatchDegree(p.getMatchDegree());
            item.setRecruitmentUrl(p.getRecruitmentUrl());
            out.add(item);
        }
        return out;
    }
}
