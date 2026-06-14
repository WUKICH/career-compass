package com.coze.aicoze.service.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coze.aicoze.entity.AssessmentArtifact;
import com.coze.aicoze.enums.ArtifactType;
import com.coze.aicoze.mapper.AssessmentArtifactMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
@RequiredArgsConstructor
public class AssessmentPersistence {

    private final AssessmentArtifactMapper artifactMapper;
    private final AssessmentOutcomeService outcomeService;
    private final Executor assessmentExecutor;

    public void saveSkill(Integer participantId, String sessionId, String content, String messageType) {
        saveArtifact(participantId, sessionId, content, messageType);
    }

    public void saveCareer(Integer participantId, String sessionId, String content, String messageType) {
        saveArtifact(participantId, sessionId, content, messageType);
    }

    public void saveChatResult(Integer participantId, String sessionId, String content, String messageType) {
        saveArtifact(participantId, sessionId, content, messageType);
    }

    public void saveFeedback(Integer participantId, String sessionId, String content) {
        saveArtifact(participantId, sessionId, content, ArtifactType.FEEDBACK.name());
    }

    private void saveArtifact(Integer participantId, String sessionId, String content, String messageType) {
        ArtifactType type = ArtifactType.fromLegacyMessageType(messageType);
        LocalDateTime now = LocalDateTime.now();
        if (type.isSingleton()) {
            AssessmentArtifact existing = artifactMapper.selectOne(new LambdaQueryWrapper<AssessmentArtifact>()
                    .eq(AssessmentArtifact::getSessionId, sessionId)
                    .eq(AssessmentArtifact::getArtifactType, type.name())
                    .last("LIMIT 1"));
            if (existing != null) {
                existing.setContent(content);
                existing.setUpdatedAt(now);
                artifactMapper.updateById(existing);
                return;
            }
        }
        AssessmentArtifact row = new AssessmentArtifact();
        row.setSessionId(sessionId);
        row.setParticipantId(participantId);
        row.setArtifactType(type.name());
        row.setContent(content);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        artifactMapper.insert(row);
    }

    public CompletableFuture<Void> updateSkillAdjustedAsync(String sessionId, Integer participantId, String skills) {
        return CompletableFuture.runAsync(
                () -> outcomeService.updateSkillAdjusted(sessionId, participantId, skills), assessmentExecutor);
    }

    public CompletableFuture<Void> updateHollandCodeAsync(String sessionId, Integer participantId, String hollandCode) {
        return CompletableFuture.runAsync(
                () -> outcomeService.updateHollandCode(sessionId, participantId, hollandCode), assessmentExecutor);
    }

    public CompletableFuture<Void> updateSalaryAdjustedAsync(String sessionId, Integer participantId, String salaryAdjusted) {
        return CompletableFuture.runAsync(
                () -> outcomeService.updateSalaryAdjusted(sessionId, participantId, salaryAdjusted), assessmentExecutor);
    }

    public CompletableFuture<Void> updateJobAdjustedAsync(String sessionId, Integer participantId, String jobAdjusted) {
        return CompletableFuture.runAsync(
                () -> outcomeService.updateJobAdjusted(sessionId, participantId, jobAdjusted), assessmentExecutor);
    }
}
