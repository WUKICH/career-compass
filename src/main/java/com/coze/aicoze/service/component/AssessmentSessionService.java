package com.coze.aicoze.service.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.coze.aicoze.entity.AssessmentSession;
import com.coze.aicoze.enums.SessionStatus;
import com.coze.aicoze.mapper.AssessmentSessionMapper;
import com.coze.aicoze.workflow.model.FlowPhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssessmentSessionService {

    private final AssessmentSessionMapper sessionMapper;

    public void createOrRefresh(String sessionId, Integer participantId, String experienceVersion) {
        AssessmentSession existing = findBySessionId(sessionId);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            AssessmentSession row = new AssessmentSession();
            row.setSessionId(sessionId);
            row.setParticipantId(participantId);
            row.setExperienceVersion(experienceVersion);
            row.setCurrentPhase(FlowPhase.COLLECT.name());
            row.setStatus(SessionStatus.IN_PROGRESS.name());
            row.setStartedAt(now);
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            sessionMapper.insert(row);
            return;
        }
        LambdaUpdateWrapper<AssessmentSession> w = new LambdaUpdateWrapper<>();
        w.eq(AssessmentSession::getSessionId, sessionId)
                .set(AssessmentSession::getParticipantId, participantId)
                .set(AssessmentSession::getExperienceVersion, experienceVersion)
                .set(AssessmentSession::getStatus, SessionStatus.IN_PROGRESS.name())
                .set(AssessmentSession::getCompletedAt, null)
                .set(AssessmentSession::getUpdatedAt, now);
        sessionMapper.update(null, w);
    }

    public void updatePhase(String sessionId, FlowPhase phase) {
        if (phase == null) {
            return;
        }
        LambdaUpdateWrapper<AssessmentSession> w = new LambdaUpdateWrapper<>();
        w.eq(AssessmentSession::getSessionId, sessionId)
                .set(AssessmentSession::getCurrentPhase, phase.name())
                .set(AssessmentSession::getUpdatedAt, LocalDateTime.now());
        sessionMapper.update(null, w);
    }

    public void markCompleted(String sessionId) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<AssessmentSession> w = new LambdaUpdateWrapper<>();
        w.eq(AssessmentSession::getSessionId, sessionId)
                .set(AssessmentSession::getStatus, SessionStatus.COMPLETED.name())
                .set(AssessmentSession::getCompletedAt, now)
                .set(AssessmentSession::getUpdatedAt, now);
        sessionMapper.update(null, w);
    }

    public Integer findParticipantId(String sessionId) {
        AssessmentSession row = findBySessionId(sessionId);
        return row == null ? null : row.getParticipantId();
    }

    private AssessmentSession findBySessionId(String sessionId) {
        return sessionMapper.selectOne(new LambdaQueryWrapper<AssessmentSession>()
                .eq(AssessmentSession::getSessionId, sessionId)
                .last("LIMIT 1"));
    }
}
