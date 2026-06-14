package com.coze.aicoze.workflow.engine;

import com.coze.aicoze.workflow.model.FlowCommand;
import com.coze.aicoze.workflow.model.FlowPhase;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Command 与 Phase 准入表。
 */
public final class PhaseGuard {

    private static final Map<FlowCommand, Set<FlowPhase>> ALLOWED = new EnumMap<>(FlowCommand.class);

    static {
        ALLOWED.put(FlowCommand.COLLECT_STEP, Set.of(FlowPhase.COLLECT));
        ALLOWED.put(FlowCommand.CONFIRM_PROFILE, Set.of(FlowPhase.COLLECT));
        ALLOWED.put(FlowCommand.MAIN_CHAT_MESSAGE, Set.of(FlowPhase.MAIN_CHAT));
        ALLOWED.put(FlowCommand.START_SKILL, Set.of(FlowPhase.MAIN_CHAT));
        ALLOWED.put(FlowCommand.SUBMIT_SKILL_ANSWER, Set.of(FlowPhase.SKILL_ACTIVE));
        ALLOWED.put(FlowCommand.SUBMIT_CAREER_ANSWER, Set.of(FlowPhase.CAREER_ACTIVE));
        ALLOWED.put(FlowCommand.CONSENSUS_MESSAGE, Set.of(FlowPhase.CONSENSUS_ACTIVE));
        ALLOWED.put(FlowCommand.GENERATE_REPORT, Set.of(FlowPhase.CONSENSUS_ACTIVE));
        ALLOWED.put(FlowCommand.SUBMIT_FEEDBACK, Set.of(FlowPhase.REPORT_READY, FlowPhase.CLOSED));
    }

    private PhaseGuard() {
    }

    public static boolean isAllowed(FlowCommand command, FlowPhase phase) {
        Set<FlowPhase> phases = ALLOWED.get(command);
        return phases != null && phases.contains(phase);
    }
}
