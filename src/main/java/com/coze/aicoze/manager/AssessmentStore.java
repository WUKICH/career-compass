package com.coze.aicoze.manager;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按业务 sessionId 存测评会话数据。必须为单例：技能/霍兰德异步作答在线程池执行，
 * 若使用 HTTP session 作用域，异步线程无法激活 {@code session} scope，会触发 ScopeNotActiveException。
 */
@Component
@Slf4j
public class AssessmentStore {

    @Data
    public static class SessionAssessment {
        private String name;
        private Integer userId;
        private String experienceVersion;
        /** 工作年限：会话/Prompt 用中文（如 1-3年）；库表存 0、1-3、3-5、5+、其他 */
        private String workYears;
        private String targetJob;
        private String skill;
        private String expectedCity;
        private String expectedSalary;
        private boolean initialContextSent;
        private String skillQuestion;
        private String careerQuestion;
        private String skillResult;
        private String careerResult;
    }

    private final Map<String, SessionAssessment> sessionMap = new ConcurrentHashMap<>();

    public SessionAssessment init(String sessionId) {
        return sessionMap.computeIfAbsent(sessionId, id -> new SessionAssessment());
    }

    public SessionAssessment get(String sessionId) {
        return sessionMap.get(sessionId);
    }

    public boolean clear(String sessionId) {
        SessionAssessment removed = sessionMap.remove(sessionId);
        if (removed != null) {
            log.debug("已清除会话数据，sessionId: {}", sessionId);
            return true;
        }
        log.debug("会话不存在，无需清除，sessionId: {}", sessionId);
        return false;
    }
}



