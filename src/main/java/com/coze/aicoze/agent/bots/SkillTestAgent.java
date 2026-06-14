package com.coze.aicoze.agent.bots;

import cn.hutool.core.util.StrUtil;
import com.coze.aicoze.agent.BaseChatAgent;
import com.coze.aicoze.agent.ChatResult;
import com.coze.aicoze.enums.AgentType;
import com.coze.aicoze.full.AssessmentBotRouting;
import com.coze.aicoze.manager.AssessmentStore;
import com.coze.aicoze.manager.ContextManager;
import com.coze.aicoze.util.JsonInputNormalizer;
import com.coze.aicoze.util.JsonSanitizerUtil;
import com.coze.aicoze.util.PromptFactory;
import com.coze.aicoze.workflow.config.WorkflowProperties;
import com.coze.openapi.client.chat.model.ChatUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

@Component
@Slf4j
@RequiredArgsConstructor
public class SkillTestAgent extends BaseChatAgent {

    private final AssessmentStore assessmentStore;
    private final ContextManager contextManager;
    private final AssessmentBotRouting assessmentBotRouting;
    private final Executor assessmentExecutor;
    private final PromptFactory promptFactory;
    private final WorkflowProperties workflowProperties;
    @Value("${agent.platform.skill:${agent.platform.default:coze}}")
    private String platform;

    @Override
    protected AgentType getAgentType() {
        return AgentType.SKILL_TEST;
    }

    @Override
    protected String getConfiguredPlatform() {
        return platform;
    }

    @Override
    public ChatResult doChat(String userMessage, String sessionId) {
        AssessmentStore.SessionAssessment sessionAssessment = assessmentStore.get(sessionId);
        userMessage = buildSkillPrompt(userMessage, sessionAssessment.getSkill(), sessionAssessment.getTargetJob());

        String experienceVersion = sessionAssessment == null ? null : sessionAssessment.getExperienceVersion();
        String botId = assessmentBotRouting.skillBotIdForAssessment(sessionId, experienceVersion);
        ChatResult skillAgentResult = chat(userMessage, botId, sessionId);
        boolean fullFlow = assessmentBotRouting.isFullFlowSession(sessionId);
        String skillRaw = skillAgentResult.getContent();
        String skillStored = skillRaw == null ? null : JsonSanitizerUtil.normalizePunctuation(skillRaw);
        if (StrUtil.isNotBlank(skillStored)) {
            String jsonOnly = JsonInputNormalizer.normalizeJsonInput(skillStored);
            if (StrUtil.isNotBlank(jsonOnly)) {
                skillStored = jsonOnly;
            }
        }
        if (sessionAssessment.getSkillQuestion() == null) {
            sessionAssessment.setSkillQuestion(skillStored);
            skillAgentResult.setContent("技能测评题目已生成，请点击「开始能力测评」进入答题。");
        } else if (fullFlow) {
            sessionAssessment.setSkillQuestion(skillStored);
        }
        return skillAgentResult;
    }

    @Override
    public void doChatStream(String userMessage, String sessionId, Consumer<String> onContent, Consumer<ChatUsage> onComplete) {
        AssessmentStore.SessionAssessment sessionAssessment = assessmentStore.get(sessionId);
        userMessage = buildSkillPrompt(userMessage, sessionAssessment.getSkill().toString(), sessionAssessment.getTargetJob());
        String experienceVersion = sessionAssessment == null ? null : sessionAssessment.getExperienceVersion();
        String botId = assessmentBotRouting.skillBotIdForAssessment(sessionId, experienceVersion);
        chatStream(userMessage, botId, sessionId, onContent, onComplete);
    }

    public CompletableFuture<String> skillAssessmentAsync(String sessionId, Integer userId, String answer) {
        AssessmentStore.SessionAssessment sessionAssessment = assessmentStore.get(sessionId);
        final String experienceVersion = sessionAssessment == null ? null : sessionAssessment.getExperienceVersion();
        final String botId = assessmentBotRouting.skillBotIdForAssessment(sessionId, experienceVersion);
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = promptFactory.buildSkillAssessmentPrompt(answer);
                String resultContent = createPollAndExtractAssistantText(prompt, botId, sessionId);
                return resultContent == null || resultContent.isEmpty() ? null : JsonSanitizerUtil.normalizePunctuation(resultContent);
            } catch (Exception e) {
                throw new RuntimeException("技能测试答案评估失败", e);
            }
        }, assessmentExecutor);
    }

    private String buildSkillPrompt(String userMessage, String skill, String targetJob) {
        return String.format("""
                %s,
                用户目标岗位:「%s」和核心技能:「%s」。
                """, userMessage, targetJob, skill);
    }
}
