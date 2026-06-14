package com.coze.aicoze.infrastructure.knowledgegraph.assembly;

import cn.hutool.core.util.StrUtil;
import com.coze.aicoze.infrastructure.knowledgegraph.dto.TalentPayload;
import com.coze.aicoze.manager.AssessmentStore;
import com.coze.aicoze.util.JsonParseUtil;
import com.coze.aicoze.vo.HollandAnalysisVO;
import com.coze.aicoze.vo.SkillAnalysisReportVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将测评会话与技能/霍兰德结果组装为 {@link TalentPayload}（与《人才画像接口说明》一致）。
 */
public final class TalentProfileAssembler {

    private static final Pattern DIGITS = Pattern.compile("(\\d+)");

    /**
     * 确认页/会话里 skills 串：多段 {@code 技能名:熟练度}，熟练度可为小数（如 {@code java:1.5，springboot:2.0}）。
     * 与 {@link com.coze.aicoze.service.Impl.CollectServiceImpl#formatSkillRatingsToSkillString} 输出一致。
     */
    private static final Pattern SESSION_SKILL_PAIR = Pattern.compile(
            "([^:：，,;\\n\\r]+?)\\s*[:：]\\s*(\\d+(?:\\.\\d+)?)");

    private TalentProfileAssembler() {
    }

    /**
     * @param userId        人才唯一 ID（使用本系统用户 id）
     * @param session       会话中的用户画像字段
     * @param skillResultJson 技能测评 Bot 返回的 JSON 字符串（与 {@link SkillAnalysisReportVO} 结构一致）
     * @param holland       霍兰德结构化报告（须已含 hollandCode）
     */
    public static TalentPayload assemble(
            Integer userId,
            AssessmentStore.SessionAssessment session,
            String skillResultJson,
            HollandAnalysisVO holland) {

        TalentPayload payload = new TalentPayload();
        payload.setId(userId == null ? null : String.valueOf(userId));
        if (session != null) {
            payload.setName(session.getName());
            payload.setIdealLocation(session.getExpectedCity());
            payload.setTargetPositionType(session.getTargetJob());
            payload.setIdealSalaryK(parseIdealSalaryK(session.getExpectedSalary()));
            payload.setWorkYears(parseNumericYears(session.getWorkYears()));
        }

        payload.setSkills(buildSkills(session, skillResultJson));

        if (holland != null && StrUtil.isNotBlank(holland.getHollandCode())) {
            payload.setHollandCode(holland.getHollandCode().trim());
        }

        return payload;
    }


    /**
     * 期望薪资：可能为 null、纯数字、或带 k 的文案（如 15k）；取第一个合理整数（K），无法解析则不设。
     */
    private static Integer parseIdealSalaryK(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String s = raw.trim();
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException ignored) {
            Matcher m = DIGITS.matcher(s);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
            return null;
        }
    }

    /**
     * 从 {@code workYears} 文案中尝试解析数字年限（如「3 年」）；无法解析或明显不合理则不传。
     */
    private static Integer parseNumericYears(String workYearsField) {
        if (StrUtil.isBlank(workYearsField)) {
            return null;
        }
        Matcher m = DIGITS.matcher(workYearsField);
        if (m.find()) {
            int n = Integer.parseInt(m.group(1));
            // 合理年限区间，避免无关文案中的偶然数字
            if (n >= 0 && n <= 60) {
                return n;
            }
        }
        return null;
    }

    private static List<TalentPayload.Skill> buildSkills(AssessmentStore.SessionAssessment session, String skillResultJson) {
        if (StrUtil.isNotBlank(skillResultJson)) {
            try {
                SkillAnalysisReportVO report = JsonParseUtil.toBeanQuiet(skillResultJson, SkillAnalysisReportVO.class);
                if (report != null && report.getSkillAnalysis() != null && !report.getSkillAnalysis().isEmpty()) {
                    List<TalentPayload.Skill> list = new ArrayList<>();
                    for (SkillAnalysisReportVO.SkillAnalysisVO row : report.getSkillAnalysis()) {
                        if (row == null || StrUtil.isBlank(row.getSkillName())) {
                            continue;
                        }
                        int level = scoreToLevel(row.getScore());
                        list.add(new TalentPayload.Skill(row.getSkillName().trim(), level));
                    }
                    if (!list.isEmpty()) {
                        return list;
                    }
                }
            } catch (Exception ignored) {
                // 回落到用户填写的技能描述
            }
        }
        if (session != null && StrUtil.isNotBlank(session.getSkill())) {
            List<TalentPayload.Skill> fromSession = parseSessionSkillPairs(session.getSkill());
            if (fromSession != null && !fromSession.isEmpty()) {
                return fromSession;
            }
            return Collections.singletonList(new TalentPayload.Skill(session.getSkill().trim(), 2));
        }
        return null;
    }

    /**
     * 解析确认页写入的 {@code name:score} 多段串（英文/中文逗号、分号、换行分隔均可；score 支持小数）。
     * 若整段无法匹配任何 {@code 名:数} 对，返回 null，由调用方回退为「整段文本 + 默认熟练度」。
     */
    private static List<TalentPayload.Skill> parseSessionSkillPairs(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String text = raw.replace('，', ',').trim();
        List<TalentPayload.Skill> list = new ArrayList<>();
        Matcher m = SESSION_SKILL_PAIR.matcher(text);
        while (m.find()) {
            String name = StrUtil.trim(m.group(1));
            if (StrUtil.isBlank(name)) {
                continue;
            }
            try {
                double level = Double.parseDouble(m.group(2));
                list.add(new TalentPayload.Skill(name, level));
            } catch (NumberFormatException ignored) {
                // 跳过异常片段
            }
        }
        return list.isEmpty() ? null : list;
    }

    /**
     * 将模型给出的评分映射为 1/2/3（了解/掌握/熟练）。
     */
    private static int scoreToLevel(Integer score) {
        if (score == null) {
            return 1;
        }
        if (score < 2) {
            return 1;
        }
        if (score < 3) {
            return 2;
        }
        return 3;
    }
}
