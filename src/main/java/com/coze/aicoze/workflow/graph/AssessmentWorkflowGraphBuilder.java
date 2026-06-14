package com.coze.aicoze.workflow.graph;

import com.coze.aicoze.workflow.model.FlowPhase;
import com.coze.aicoze.workflow.model.FlowVariant;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 构建 LangGraph4j 状态图（与 {@link FlowPhase} 对齐），启动时编译校验结构。
 */
@Component
@Slf4j
public class AssessmentWorkflowGraphBuilder {

    static class AssessmentGraphState extends AgentState {
        static final Map<String, Channel<?>> SCHEMA = Map.of(
                "phase", Channels.base(() -> FlowPhase.COLLECT.name()),
                "variant", Channels.base(() -> FlowVariant.TRIAL.name())
        );

        AssessmentGraphState(Map<String, Object> data) {
            super(data);
        }
    }

    @PostConstruct
    void compileGraphAtStartup() {
        try {
            buildGraph().compile();
            log.info("LangGraph4j assessment workflow graph compiled successfully");
        } catch (Exception e) {
            log.warn("LangGraph4j graph compile failed (transitions still validated by adapter): {}", e.getMessage());
        }
    }

    StateGraph<AssessmentGraphState> buildGraph() throws GraphStateException {
        var noop = node_async((AssessmentGraphState s) -> Map.<String, Object>of());
        return new StateGraph<>(AssessmentGraphState.SCHEMA, AssessmentGraphState::new)
                .addNode("collect", noop)
                .addNode("mainChat", noop)
                .addNode("skillActive", noop)
                .addNode("skillDone", noop)
                .addNode("careerActive", noop)
                .addNode("careerDone", noop)
                .addNode("preliminaryRec", noop)
                .addNode("consensusActive", noop)
                .addNode("reportReady", noop)
                .addNode("closed", noop)
                .addEdge(START, "collect")
                .addEdge("collect", "mainChat")
                .addEdge("mainChat", "skillActive")
                .addConditionalEdges("skillActive",
                        edge_async(s -> "complete"),
                        Map.of("continue", "skillActive", "complete", "skillDone"))
                .addEdge("skillDone", "careerActive")
                .addConditionalEdges("careerActive",
                        edge_async(s -> "complete"),
                        Map.of("continue", "careerActive", "complete", "careerDone"))
                .addEdge("careerDone", "preliminaryRec")
                .addEdge("preliminaryRec", "consensusActive")
                .addConditionalEdges("consensusActive",
                        edge_async(s -> "complete"),
                        Map.of("continue", "consensusActive", "complete", "reportReady"))
                .addEdge("reportReady", "closed")
                .addEdge("closed", END);
    }

    static Set<String> trialEdges() {
        return Set.of(
                edge(FlowPhase.COLLECT, FlowPhase.MAIN_CHAT),
                edge(FlowPhase.CONFIRMED, FlowPhase.MAIN_CHAT),
                edge(FlowPhase.MAIN_CHAT, FlowPhase.SKILL_ACTIVE),
                edge(FlowPhase.SKILL_ACTIVE, FlowPhase.SKILL_DONE),
                edge(FlowPhase.SKILL_DONE, FlowPhase.CAREER_ACTIVE),
                edge(FlowPhase.CAREER_ACTIVE, FlowPhase.CAREER_DONE),
                edge(FlowPhase.CAREER_DONE, FlowPhase.PRELIMINARY_REC),
                edge(FlowPhase.PRELIMINARY_REC, FlowPhase.CONSENSUS_ACTIVE),
                edge(FlowPhase.CONSENSUS_ACTIVE, FlowPhase.CONSENSUS_ACTIVE),
                edge(FlowPhase.CONSENSUS_ACTIVE, FlowPhase.REPORT_READY),
                edge(FlowPhase.REPORT_READY, FlowPhase.CLOSED)
        );
    }

    static Set<String> fullEdges() {
        var edges = new ArrayList<>(trialEdges());
        edges.add(edge(FlowPhase.SKILL_ACTIVE, FlowPhase.SKILL_ACTIVE));
        edges.add(edge(FlowPhase.CAREER_ACTIVE, FlowPhase.CAREER_ACTIVE));
        return Set.copyOf(edges);
    }

    private static String edge(FlowPhase from, FlowPhase to) {
        return from.name() + "->" + to.name();
    }
}
