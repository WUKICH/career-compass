package com.coze.aicoze.service.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.coze.aicoze.entity.AssessmentOutcome;
import com.coze.aicoze.mapper.AssessmentOutcomeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class AssessmentOutcomeService {

    private final AssessmentOutcomeMapper outcomeMapper;

    public void updateSkillAdjusted(String sessionId, Integer participantId, String skills) {
        upsertField(sessionId, participantId, skills, AssessmentOutcome::setSkillAdjusted,
                w -> w.set(AssessmentOutcome::getSkillAdjusted, skills), "调整后技能");
    }

    public void updateHollandCode(String sessionId, Integer participantId, String hollandCode) {
        upsertField(sessionId, participantId, hollandCode, AssessmentOutcome::setHollandCode,
                w -> w.set(AssessmentOutcome::getHollandCode, hollandCode), "霍兰德代码");
    }

    public void updateSalaryAdjusted(String sessionId, Integer participantId, String salaryAdjusted) {
        upsertField(sessionId, participantId, salaryAdjusted, AssessmentOutcome::setSalaryAdjusted,
                w -> w.set(AssessmentOutcome::getSalaryAdjusted, salaryAdjusted), "调整后薪资");
    }

    public void updateJobAdjusted(String sessionId, Integer participantId, String jobAdjusted) {
        upsertField(sessionId, participantId, jobAdjusted, AssessmentOutcome::setJobAdjusted,
                w -> w.set(AssessmentOutcome::getJobAdjusted, jobAdjusted), "调整后岗位");
    }

    private void upsertField(String sessionId,
                             Integer participantId,
                             String value,
                             BiConsumer<AssessmentOutcome, String> inserter,
                             Function<LambdaUpdateWrapper<AssessmentOutcome>, LambdaUpdateWrapper<AssessmentOutcome>> updater,
                             String label) {
        try {
            AssessmentOutcome existing = outcomeMapper.selectOne(new LambdaQueryWrapper<AssessmentOutcome>()
                    .eq(AssessmentOutcome::getSessionId, sessionId)
                    .last("LIMIT 1"));
            LocalDateTime now = LocalDateTime.now();
            if (existing == null) {
                AssessmentOutcome row = new AssessmentOutcome();
                row.setSessionId(sessionId);
                row.setParticipantId(participantId);
                inserter.accept(row, value);
                row.setCreatedAt(now);
                row.setUpdatedAt(now);
                outcomeMapper.insert(row);
                return;
            }
            LambdaUpdateWrapper<AssessmentOutcome> w = new LambdaUpdateWrapper<>();
            w.eq(AssessmentOutcome::getSessionId, sessionId);
            updater.apply(w);
            w.set(AssessmentOutcome::getUpdatedAt, now);
            int rows = outcomeMapper.update(null, w);
            if (rows != 1) {
                log.warn("测评结果更新{}异常，期望 1 条，实际 {}，sessionId={}", label, rows, sessionId);
            }
        } catch (Exception e) {
            log.error("更新测评结果{}失败，sessionId={}", label, sessionId, e);
        }
    }
}
