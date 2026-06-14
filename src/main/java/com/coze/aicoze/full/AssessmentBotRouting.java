package com.coze.aicoze.full;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 体验版 / 完全版测评：Bot ID 选择与完全版会话判定（与 {@link FullFlowRegistry} 配合）。
 */
@Component
public class AssessmentBotRouting {

    private final FullFlowRegistry fullFlowRegistry;
    private final String skillPlatform;
    private final String careerPlatform;
    private final String fullSkillPlatform;
    private final String fullCareerPlatform;

    private final String cozeExperienceSkillBotId;
    private final String cozeFullSkillBotId;
    private final String cozeExperienceCareerBotId;
    private final String cozeFullCareerBotId;

    private final String tencentExperienceSkillBotId;
    private final String tencentFullSkillBotId;
    private final String tencentExperienceCareerBotId;
    private final String tencentFullCareerBotId;

    public AssessmentBotRouting(
            @Autowired(required = false) FullFlowRegistry fullFlowRegistry,
            @Value("${agent.platform.skill:${agent.platform.default:coze}}") String skillPlatform,
            @Value("${agent.platform.career:${agent.platform.default:coze}}") String careerPlatform,
            @Value("${agent.platform.full-skill:${agent.platform.skill:${agent.platform.default:coze}}}") String fullSkillPlatform,
            @Value("${agent.platform.full-career:${agent.platform.career:${agent.platform.default:coze}}}") String fullCareerPlatform,
            @Value("${coze.skill-client.bot-id:}") String cozeExperienceSkillBotId,
            @Value("${coze.full.skill-client.bot-id:}") String cozeFullSkillBotId,
            @Value("${coze.career-client.bot-id:}") String cozeExperienceCareerBotId,
            @Value("${coze.full.career-client.bot-id:}") String cozeFullCareerBotId,
            @Value("${tencent.lke.skill-client.app-key:}") String tencentExperienceSkillBotId,
            @Value("${tencent.lke.full.skill-client.app-key:}") String tencentFullSkillBotId,
            @Value("${tencent.lke.career-client.app-key:}") String tencentExperienceCareerBotId,
            @Value("${tencent.lke.full.career-client.app-key:}") String tencentFullCareerBotId) {
        this.fullFlowRegistry = fullFlowRegistry;
        this.skillPlatform = skillPlatform;
        this.careerPlatform = careerPlatform;
        this.fullSkillPlatform = fullSkillPlatform;
        this.fullCareerPlatform = fullCareerPlatform;
        this.cozeExperienceSkillBotId = cozeExperienceSkillBotId;
        this.cozeFullSkillBotId = cozeFullSkillBotId;
        this.cozeExperienceCareerBotId = cozeExperienceCareerBotId;
        this.cozeFullCareerBotId = cozeFullCareerBotId;
        this.tencentExperienceSkillBotId = tencentExperienceSkillBotId;
        this.tencentFullSkillBotId = tencentFullSkillBotId;
        this.tencentExperienceCareerBotId = tencentExperienceCareerBotId;
        this.tencentFullCareerBotId = tencentFullCareerBotId;
    }

    /** 用户选择的体验版本文案中含「完全」时走完全版 Bot。 */
    public boolean isFullVersionLabel(String experienceVersion) {
        return experienceVersion != null && experienceVersion.contains("完全");
    }

    public String skillBotId(String experienceVersion) {
        return isFullVersionLabel(experienceVersion) ? fullSkillBotIdByPlatform() : experienceSkillBotIdByPlatform();
    }

    public String careerBotId(String experienceVersion) {
        return isFullVersionLabel(experienceVersion) ? fullCareerBotIdByPlatform() : experienceCareerBotIdByPlatform();
    }

    /**
     * 通过 {@link FullFlowRegistry} 注册的会话（POST /full/chat/init）固定走完全版多轮 Bot，
     * 与用户在表单里填的「体验版/完全版」文案解耦。
     */
    public String skillBotIdForAssessment(String sessionId, String experienceVersion) {
        if (isFullFlowSession(sessionId)) {
            return fullSkillBotIdByPlatform();
        }
        return skillBotId(experienceVersion);
    }

    public String careerBotIdForAssessment(String sessionId, String experienceVersion) {
        if (isFullFlowSession(sessionId)) {
            return fullCareerBotIdByPlatform();
        }
        return careerBotId(experienceVersion);
    }

    /** 当前 session 是否走完全版 /full/chat 流程（多轮技能题等）。 */
    public boolean isFullFlowSession(String sessionId) {
        return fullFlowRegistry != null && fullFlowRegistry.isFull(sessionId);
    }

    private String experienceSkillBotIdByPlatform() {
        return "coze".equalsIgnoreCase(skillPlatform) ? cozeExperienceSkillBotId : tencentExperienceSkillBotId;
    }

    private String fullSkillBotIdByPlatform() {
        return "coze".equalsIgnoreCase(fullSkillPlatform) ? cozeFullSkillBotId : tencentFullSkillBotId;
    }

    private String experienceCareerBotIdByPlatform() {
        return "coze".equalsIgnoreCase(careerPlatform) ? cozeExperienceCareerBotId : tencentExperienceCareerBotId;
    }

    private String fullCareerBotIdByPlatform() {
        return "coze".equalsIgnoreCase(fullCareerPlatform) ? cozeFullCareerBotId : tencentFullCareerBotId;
    }
}
