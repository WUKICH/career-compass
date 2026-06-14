package com.coze.aicoze.util;

import cn.hutool.json.JSONUtil;
import com.coze.aicoze.vo.SkillAnalysisReportVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将技能分析报告转为「调整后技能」落库字符串：仅含核心技能 + 评分，不含评价。
 */
public final class AdjustedStoragePayload {

    private AdjustedStoragePayload() {
    }

    public static String fromReport(SkillAnalysisReportVO report) {
        if (report == null || report.getSkillAnalysis() == null || report.getSkillAnalysis().isEmpty()) {
            return "";
        }
        List<Map<String, Object>> list = new ArrayList<>(report.getSkillAnalysis().size());
        for (SkillAnalysisReportVO.SkillAnalysisVO row : report.getSkillAnalysis()) {
            if (row == null) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("核心技能", row.getSkillName());
            m.put("评分", row.getScore());
            list.add(m);
        }
        return JSONUtil.toJsonStr(list);
    }
}
