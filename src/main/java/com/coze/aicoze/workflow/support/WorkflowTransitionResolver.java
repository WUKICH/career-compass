package com.coze.aicoze.workflow.support;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.coze.aicoze.service.flow.TransitionOutcome;
import com.coze.aicoze.util.CozeJsonExtractor;
import com.coze.aicoze.util.JsonParseUtil;
import com.coze.aicoze.vo.HollandAnalysisVO;
import com.coze.aicoze.vo.SkillAnalysisReportVO;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class WorkflowTransitionResolver {

    private static final Pattern INVALID_SUMMARY = Pattern.compile(
            "未作答|未提交|无法.*分析|没有.*答案|无有效|未回答",
            Pattern.CASE_INSENSITIVE);

    public TransitionOutcome resolveSkillOutcome(String skillResult) {
        if (isSkillGradeComplete(skillResult)) {
            return TransitionOutcome.COMPLETE;
        }
        return TransitionOutcome.CONTINUE;
    }

    public TransitionOutcome resolveCareerOutcome(String careerResult) {
        if (isCareerGradeComplete(careerResult)) {
            return TransitionOutcome.COMPLETE;
        }
        return TransitionOutcome.CONTINUE;
    }

    private boolean isSkillGradeComplete(String skillResult) {
        if (StrUtil.isBlank(skillResult)) {
            return false;
        }
        JSONObject obj = parseGradeJson(skillResult);
        if (obj != null) {
            String status = JsonParseUtil.getStr(obj, "status", "状态");
            if ("continue".equalsIgnoreCase(status)) {
                return false;
            }
            if ("complete".equalsIgnoreCase(status)) {
                if (INVALID_SUMMARY.matcher(skillResult).find()) {
                    return false;
                }
                if (isValidNestedSkillReport(obj)) {
                    return true;
                }
                // {"status":"complete","content":"..."} 等无 report 节点时也认可
                if (obj.get("report") == null) {
                    return true;
                }
                return !isInvalidSkillReportPayload(skillResult, obj);
            }
        }
        String json = CozeJsonExtractor.extractLastJsonObject(skillResult);
        SkillAnalysisReportVO report = JsonParseUtil.toBeanQuiet(
                json != null ? json : skillResult, SkillAnalysisReportVO.class);
        if (report == null || StrUtil.isBlank(report.getSummary())) {
            return false;
        }
        if (isInvalidSummary(report.getSummary())) {
            return false;
        }
        Integer total = report.getTotalQuestions();
        if (total != null && total <= 0) {
            return false;
        }
        Integer score = report.getTotalScore();
        if (total != null && total > 0 && score != null && score == 0
                && INVALID_SUMMARY.matcher(report.getSummary()).find()) {
            return false;
        }
        return true;
    }

    private boolean isCareerGradeComplete(String careerResult) {
        if (StrUtil.isBlank(careerResult)) {
            return false;
        }
        JSONObject obj = parseGradeJson(careerResult);
        if (obj != null) {
            String status = JsonParseUtil.getStr(obj, "status", "状态");
            if ("continue".equalsIgnoreCase(status)) {
                return false;
            }
            if ("complete".equalsIgnoreCase(status)) {
                if (isValidNestedHollandReport(obj)) {
                    return true;
                }
                return hasValidHollandReport(careerResult);
            }
        }
        return hasValidHollandReport(careerResult);
    }

    private boolean hasValidHollandReport(String raw) {
        String json = CozeJsonExtractor.extractLastJsonObject(raw);
        HollandAnalysisVO report = JsonParseUtil.toBeanQuiet(
                json != null ? json : raw, HollandAnalysisVO.class);
        if (report == null || StrUtil.isBlank(report.getHollandCode())) {
            return false;
        }
        String code = report.getHollandCode().trim();
        if (code.length() < 2) {
            return false;
        }
        String lower = code.toLowerCase(Locale.ROOT);
        return !lower.contains("未作答") && !lower.contains("unknown");
    }

    private boolean isInvalidSkillReportPayload(String raw, JSONObject statusObj) {
        if (isInvalidSummary(JsonParseUtil.getStr(statusObj, "summary", "总结"))) {
            return true;
        }
        String json = CozeJsonExtractor.extractLastJsonObject(raw);
        SkillAnalysisReportVO report = JsonParseUtil.toBeanQuiet(
                json != null ? json : raw, SkillAnalysisReportVO.class);
        if (report == null) {
            return false;
        }
        if (isInvalidSummary(report.getSummary())) {
            return true;
        }
        Integer total = report.getTotalQuestions();
        return total != null && total <= 0;
    }

    private static boolean isValidNestedSkillReport(JSONObject obj) {
        Object nested = obj.get("report");
        if (!(nested instanceof JSONObject reportObj)) {
            return false;
        }
        String summary = JsonParseUtil.getStr(reportObj, "summary", "总结");
        return !isInvalidSummary(summary);
    }

    private static boolean isValidNestedHollandReport(JSONObject obj) {
        Object nested = obj.get("report");
        if (!(nested instanceof JSONObject reportObj)) {
            return false;
        }
        String code = JsonParseUtil.getStr(reportObj, "hollandCode", "霍兰德职业兴趣代码");
        return StrUtil.isNotBlank(code) && !code.toLowerCase(Locale.ROOT).contains("未作答");
    }

    private static boolean isInvalidSummary(String summary) {
        if (StrUtil.isBlank(summary)) {
            return true;
        }
        return INVALID_SUMMARY.matcher(summary).find();
    }

    private JSONObject parseGradeJson(String raw) {
        String json = CozeJsonExtractor.extractLastJsonObject(raw);
        if (json == null) {
            json = raw;
        }
        JSONObject obj = JsonParseUtil.parseObjSanitizedQuiet(json);
        if (obj == null && JSONUtil.isTypeJSON(json)) {
            try {
                obj = JSONUtil.parseObj(json);
            } catch (Exception ignored) {
                return null;
            }
        }
        return obj;
    }
}
