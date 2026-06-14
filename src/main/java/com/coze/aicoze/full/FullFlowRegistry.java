package com.coze.aicoze.full;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 标记「完全版」会话，与体验版分流；生命周期与 session 一致，反馈提交时应 {@link #unregister(String)}。
 */
@Component
public class FullFlowRegistry {

    private final Set<String> fullSessionIds = ConcurrentHashMap.newKeySet();

    public void register(String sessionId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            fullSessionIds.add(sessionId);
        }
    }

    public void unregister(String sessionId) {
        if (sessionId != null) {
            fullSessionIds.remove(sessionId);
        }
    }

    public boolean isFull(String sessionId) {
        return sessionId != null && fullSessionIds.contains(sessionId);
    }
}
