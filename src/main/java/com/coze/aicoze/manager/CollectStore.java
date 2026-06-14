package com.coze.aicoze.manager;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话式收集用户信息时的临时状态：sessionId -> 已填写的 questionKey -> userInput
 */
@Component
public class CollectStore {

    private final Map<String, Map<String, String>> collectIdToAnswers = new ConcurrentHashMap<>();

    public Map<String, String> getOrCreateAnswers(String sessionId) {
        return collectIdToAnswers.computeIfAbsent(sessionId, k -> new LinkedHashMap<>());
    }

    public void remove(String sessionId) {
        collectIdToAnswers.remove(sessionId);
    }
}
