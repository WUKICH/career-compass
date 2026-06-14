package com.coze.aicoze.full;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 完全版会话侧数据：技能测评轮次等（与 {@link com.coze.aicoze.manager.AssessmentStore} 并行，不混入体验版字段）。
 */
@Component
public class FullAssessmentStore {

    private final Map<String, AtomicInteger> skillRounds = new ConcurrentHashMap<>();

    public void bumpSkillRound(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return;
        }
        skillRounds.computeIfAbsent(sessionId, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public int getSkillRound(String sessionId) {
        if (sessionId == null) {
            return 0;
        }
        AtomicInteger n = skillRounds.get(sessionId);
        return n == null ? 0 : n.get();
    }

    public void clear(String sessionId) {
        if (sessionId != null) {
            skillRounds.remove(sessionId);
        }
    }
}
