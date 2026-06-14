package com.coze.aicoze.workflow.store;

import com.coze.aicoze.workflow.config.WorkflowProperties;
import com.coze.aicoze.workflow.model.FlowPhase;
import com.coze.aicoze.workflow.model.SessionWorkflowState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WorkflowStateStore {

    private final WorkflowProperties workflowProperties;
    private final ObjectMapper objectMapper;
    private final Optional<StringRedisTemplate> redisTemplate;
    private final ConcurrentHashMap<String, SessionWorkflowState> memory = new ConcurrentHashMap<>();

    @Value("${app.redis.enabled:false}")
    private boolean redisEnabled;

    public WorkflowStateStore(WorkflowProperties workflowProperties,
                              ObjectMapper objectMapper,
                              ObjectProvider<StringRedisTemplate> redisProvider) {
        this.workflowProperties = workflowProperties;
        this.objectMapper = objectMapper;
        this.redisTemplate = Optional.ofNullable(redisProvider.getIfAvailable());
    }

    public SessionWorkflowState getOrCreate(String sessionId) {
        return memory.computeIfAbsent(sessionId, id -> loadFromRedis(id).orElseGet(() ->
                SessionWorkflowState.builder()
                        .sessionId(id)
                        .phase(FlowPhase.COLLECT)
                        .updatedAt(Instant.now())
                        .build()));
    }

    public SessionWorkflowState get(String sessionId) {
        SessionWorkflowState cached = memory.get(sessionId);
        if (cached != null) {
            return cached;
        }
        return loadFromRedis(sessionId).map(s -> {
            memory.put(sessionId, s);
            return s;
        }).orElse(null);
    }

    public void save(SessionWorkflowState state) {
        state.setUpdatedAt(Instant.now());
        memory.put(state.getSessionId(), state);
        persistToRedis(state);
    }

    public void clear(String sessionId) {
        memory.remove(sessionId);
        redisTemplate.ifPresent(redis -> redis.delete(key(sessionId)));
    }

    private Optional<SessionWorkflowState> loadFromRedis(String sessionId) {
        if (!redisEnabled || redisTemplate.isEmpty()) {
            return Optional.empty();
        }
        try {
            String json = redisTemplate.get().opsForValue().get(key(sessionId));
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, SessionWorkflowState.class));
        } catch (Exception e) {
            log.warn("Load workflow state from redis failed, sessionId={}", sessionId, e);
            return Optional.empty();
        }
    }

    private void persistToRedis(SessionWorkflowState state) {
        if (!redisEnabled || redisTemplate.isEmpty()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(state);
            redisTemplate.get().opsForValue().set(
                    key(state.getSessionId()),
                    json,
                    Duration.ofSeconds(workflowProperties.getState().getTtlSeconds()));
        } catch (JsonProcessingException e) {
            log.warn("Persist workflow state failed, sessionId={}", state.getSessionId(), e);
        }
    }

    private String key(String sessionId) {
        return workflowProperties.getState().getKeyPrefix() + sessionId;
    }
}
