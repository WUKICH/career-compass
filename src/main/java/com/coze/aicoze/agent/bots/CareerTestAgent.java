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
public class CareerTestAgent extends BaseChatAgent {

    private final AssessmentStore assessmentStore;
    private final ContextManager contextManager;
    private final AssessmentBotRouting assessmentBotRouting;
    private final Executor assessmentExecutor;
    private final PromptFactory promptFactory;
    private final WorkflowProperties workflowProperties;
    @Value("${agent.platform.career:${agent.platform.default:coze}}")
    private String platform;

    @Override
    protected AgentType getAgentType() {
        return AgentType.CAREER_TEST;
    }

    @Override
    protected String getConfiguredPlatform() {
        return platform;
    }

    @Override
    public ChatResult doChat(String userMessage, String sessionId) {
        AssessmentStore.SessionAssessment sessionAssessment = assessmentStore.get(sessionId);
        userMessage = buildCareerPrompt(userMessage, sessionAssessment.getSkill().toString(), sessionAssessment.getTargetJob());
        String experienceVersion = sessionAssessment == null ? null : sessionAssessment.getExperienceVersion();
        String botId = assessmentBotRouting.careerBotIdForAssessment(sessionId, experienceVersion);
        ChatResult careerAgentResult = chat(userMessage, botId, sessionId);
        String careerRaw = careerAgentResult.getContent();
        String careerStored = careerRaw == null ? null : JsonSanitizerUtil.normalizePunctuation(careerRaw);
        boolean fullFlow = assessmentBotRouting.isFullFlowSession(sessionId);
        if (StrUtil.isNotBlank(careerStored)) {
            String jsonOnly = JsonInputNormalizer.normalizeJsonInput(careerStored);
            if (StrUtil.isNotBlank(jsonOnly)) {
                careerStored = jsonOnly;
            }
        }
        if (sessionAssessment.getCareerQuestion() == null) {
            sessionAssessment.setCareerQuestion(careerStored);
            careerAgentResult.setContent("霍兰德题目已预生成，将在技能测评完成后展示。");
        } else if (fullFlow) {
            sessionAssessment.setCareerQuestion(careerStored);
        } else {
            careerAgentResult.setContent("职业兴趣测评即将开始，请做好准备！");
        }
        return careerAgentResult;
    }

    @Override
    public void doChatStream(String userMessage, String sessionId, Consumer<String> onContent, Consumer<ChatUsage> onComplete) {
        AssessmentStore.SessionAssessment sessionAssessment = assessmentStore.get(sessionId);
        userMessage = buildCareerPrompt(userMessage, sessionAssessment.getSkill().toString(), sessionAssessment.getTargetJob());
        String experienceVersion = sessionAssessment == null ? null : sessionAssessment.getExperienceVersion();
        String botId = assessmentBotRouting.careerBotIdForAssessment(sessionId, experienceVersion);
        chatStream(userMessage, botId, sessionId, onContent, onComplete);
    }

    public CompletableFuture<String> careerAssessmentAsync(String sessionId, Integer userId, String answer) {
        AssessmentStore.SessionAssessment sessionAssessment = assessmentStore.get(sessionId);
        final String experienceVersion = sessionAssessment == null ? null : sessionAssessment.getExperienceVersion();
        final String botId = assessmentBotRouting.careerBotIdForAssessment(sessionId, experienceVersion);
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = promptFactory.buildCareerAssessmentPrompt(answer);
                String resultContent = createPollAndExtractAssistantText(prompt, botId, sessionId);
                return resultContent == null || resultContent.isEmpty() ? null : JsonSanitizerUtil.normalizePunctuation(resultContent);
            } catch (Exception e) {
                throw new RuntimeException("职业测试答案评估失败", e);
            }
        }, assessmentExecutor);
    }

    private String buildCareerPrompt(String userMessage, String skill, String targetJob) {
        return String.format("""
                %s,
                用户目标岗位「%s」和核心技能「%s」。
                """, userMessage, targetJob, skill);
    }
}
