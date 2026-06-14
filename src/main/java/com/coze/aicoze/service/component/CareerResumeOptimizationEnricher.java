package com.coze.aicoze.service.component;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.coze.aicoze.agent.ChatResult;
import com.coze.aicoze.agent.bots.ResumeOptimizeAgent;
import com.coze.aicoze.entity.User;
import com.coze.aicoze.mapper.UsersMapper;
import com.coze.aicoze.util.CozeJsonExtractor;
import com.coze.aicoze.util.JsonParseUtil;
import com.coze.aicoze.util.PromptFactory;
import com.coze.aicoze.vo.CareerAssessmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 在综合评价（CHAT_END）解析完成后，对经简历采集的用户二次调用 Agent，填充 {@link CareerAssessmentVO#getResumeOptimization()}。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CareerResumeOptimizationEnricher {

    private final UsersMapper usersMapper;
    private final ResumeOptimizeAgent resumeOptimizeAgent;
    private final PromptFactory promptFactory;

    public void enrichIfApplicable(Integer userId, String sessionId, CareerAssessmentVO report) {
        if (userId == null || report == null) {
            return;
        }
        User user = usersMapper.selectById(userId);
        if (user == null || !Boolean.TRUE.equals(user.getResumeSubmitted())) {
            return;
        }
        if (StrUtil.isBlank(user.getResumeClaims())) {
            return;
        }
        if (!resumeOptimizeAgent.isConfigured()) {
            log.debug("Skip resume optimization: bot not configured, userId={}", userId);
            return;
        }
        String reportJson = JSONUtil.toJsonStr(report);
        String prompt = promptFactory.buildResumeOptimizationPrompt(user.getResumeClaims(), reportJson);
        try {
            ChatResult result = resumeOptimizeAgent.doChat(prompt, sessionId);
            String raw = result == null ? null : result.getContent();
            if (StrUtil.isBlank(raw)) {
                log.warn("Resume optimization empty response, userId={}", userId);
                return;
            }
            String jsonOnly = CozeJsonExtractor.extractLastJsonObject(raw);
            CareerAssessmentVO.ResumeOptimizationSection section = JsonParseUtil.toBeanQuiet(
                    StrUtil.isNotBlank(jsonOnly) ? jsonOnly : raw,
                    CareerAssessmentVO.ResumeOptimizationSection.class);
            if (section == null) {
                log.warn("Resume optimization JSON parse failed, userId={}", userId);
                return;
            }
            report.setResumeOptimization(section);
        } catch (Exception e) {
            log.warn("Resume optimization call failed, userId={}", userId, e);
        }
    }
}
