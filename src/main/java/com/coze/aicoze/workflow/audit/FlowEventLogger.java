package com.coze.aicoze.workflow.audit;

import com.coze.aicoze.entity.FlowEvent;
import com.coze.aicoze.mapper.FlowEventMapper;
import com.coze.aicoze.workflow.model.FlowPhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 流程阶段变更审计：结构化日志 + flow_event 表持久化（失败不阻断 transition）。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FlowEventLogger {

    @Autowired(required = false)
    private FlowEventMapper flowEventMapper;

    public void logTransition(String sessionId, Integer participantId, FlowPhase from, FlowPhase to, String trigger, String detail) {
        log.info("flow_event sessionId={} participantId={} from={} to={} trigger={} detail={}",
                sessionId, participantId, from, to, trigger, detail == null ? "" : detail);
        persist(sessionId, participantId, from == null ? null : from.name(), to.name(), trigger, detail);
    }

    public void logRejected(String sessionId, FlowPhase phase, String command, String reason) {
        log.warn("flow_reject sessionId={} phase={} command={} reason={}",
                sessionId, phase, command, reason);
    }

    private void persist(String sessionId, Integer participantId, String fromPhase, String toPhase, String trigger, String detail) {
        if (flowEventMapper == null) {
            return;
        }
        try {
            FlowEvent row = new FlowEvent();
            row.setSessionId(sessionId);
            row.setParticipantId(participantId);
            row.setFromPhase(fromPhase);
            row.setToPhase(toPhase);
            row.setTriggerName(trigger);
            row.setDetail(detail);
            row.setCreatedAt(LocalDateTime.now());
            flowEventMapper.insert(row);
        } catch (Exception e) {
            log.warn("flow_event persist failed sessionId={} from={} to={}", sessionId, fromPhase, toPhase, e);
        }
    }
}
