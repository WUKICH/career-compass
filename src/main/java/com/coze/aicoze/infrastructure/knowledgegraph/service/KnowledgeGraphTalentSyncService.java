package com.coze.aicoze.infrastructure.knowledgegraph.service;

import com.coze.aicoze.infrastructure.knowledgegraph.Client.KnowledgeGraphFeignClient;
import com.coze.aicoze.infrastructure.knowledgegraph.assembly.TalentProfileAssembler;
import com.coze.aicoze.infrastructure.knowledgegraph.dto.BatchUpsertResponse;
import com.coze.aicoze.infrastructure.knowledgegraph.assembly.RecommendedPositionAssembler;
import com.coze.aicoze.infrastructure.knowledgegraph.dto.PositionsItem;
import com.coze.aicoze.infrastructure.knowledgegraph.dto.RecommendationRequest;
import com.coze.aicoze.infrastructure.knowledgegraph.dto.TalentPayload;
import com.coze.aicoze.manager.AssessmentStore;
import com.coze.aicoze.vo.CareerAssessmentVO;
import com.coze.aicoze.vo.HollandAnalysisVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 人才画像推送到知识图谱：确认信息 / 技能测评完成 / 霍兰德完成各同步一次（失败仅打日志，不阻断主流程）。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeGraphTalentSyncService {

    private final KnowledgeGraphFeignClient knowledgeGraphFeignClient;

    @Value("${kg.service.enabled:true}")
    private boolean enabled;

    @Value("${kg.service.recommend.top-k:10}")
    private int recommendTopK;

    /** 用户确认基本信息并初始化会话后 */
    public void syncAfterProfileConfirm(Integer userId, AssessmentStore.SessionAssessment session) {
        if (!enabled) {
            log.debug("知识图谱同步已关闭 kg.service.enabled=false，跳过[profile_confirm]");
            return;
        }
        if (userId == null || session == null) {
            log.warn("知识图谱同步跳过[profile_confirm]：userId 或 session 为空");
            return;
        }
        TalentPayload payload = TalentProfileAssembler.assemble(userId, session, null, null);
        upsertBatch(payload, userId, "profile_confirm");
    }

    /** 技能测评阶段结束、已写入 skillResult 后 */
    public void syncAfterSkillTestComplete(Integer userId, AssessmentStore.SessionAssessment session) {
        if (!enabled) {
            log.debug("知识图谱同步已关闭 kg.service.enabled=false，跳过[skill_test]");
            return;
        }
        if (userId == null || session == null) {
            log.warn("知识图谱同步跳过[skill_test]：userId 或 session 为空");
            return;
        }
        String skillJson = session.getSkillResult();
        TalentPayload payload = TalentProfileAssembler.assemble(userId, session, skillJson, null);
        upsertBatch(payload, userId, "skill_test");
    }

    /** 霍兰德测评结束、已解析 hollandCode 后 */
    public void syncAfterHollandReport(Integer userId, AssessmentStore.SessionAssessment session, HollandAnalysisVO holland) {
        if (!enabled) {
            log.debug("知识图谱同步已关闭 kg.service.enabled=false，跳过[holland]");
            return;
        }
        if (userId == null || session == null) {
            log.warn("知识图谱同步跳过[holland]：userId 或 session 为空");
            return;
        }
        TalentPayload payload = TalentProfileAssembler.assemble(
                userId,
                session,
                session.getSkillResult(),
                holland);
        upsertBatch(payload, userId, "holland_test");
    }

    private void upsertBatch(TalentPayload payload, Integer userId, String phase) {
        try {
            if (payload.getId() == null) {
                log.warn("知识图谱同步跳过[{}]：人才 id 为空", phase);
                return;
            }
            log.info("传入知识图谱的数据[{}]，{}", phase, payload);
            BatchUpsertResponse resp = knowledgeGraphFeignClient.batchUpsert(Collections.singletonList(payload));
            log.info("知识图谱人才画像已同步[{}] userId={} code={} message={} count={}",
                    phase,
                    userId,
                    resp != null ? resp.getCode() : -1,
                    resp != null ? resp.getMessage() : null,
                    resp != null ? resp.getCount() : -1);
        } catch (Exception e) {
            log.warn("知识图谱人才画像同步失败[{}] userId={}，不影响测评主流程: {}", phase, userId, e.getMessage());
            log.debug("知识图谱同步异常详情", e);
        }
    }

    /**
     * 拉取知识图谱推荐职位，用于综合评价报告中的「推荐职位」列表（失败仅打日志，返回空列表）。
     */
    public List<CareerAssessmentVO.PositionsItem> fetchRecommendedPositionsForReport(Integer userId) {
        if (!enabled) {
            log.debug("知识图谱已关闭 kg.service.enabled=false，跳过推荐职位拉取");
            return Collections.emptyList();
        }
        if (userId == null) {
            return Collections.emptyList();
        }
        int topK = recommendTopK < 1 ? 10 : recommendTopK;
        RecommendationRequest request = RecommendationRequest.builder()
                .userId(String.valueOf(userId))
                .top_k(topK)
                .build();
        try {
            List<PositionsItem> raw = knowledgeGraphFeignClient.recommendPositions(request);
            return RecommendedPositionAssembler.toCareerItems(raw);
        } catch (Exception e) {
            log.warn("知识图谱推荐职位调用失败 userId={}，不影响测评主流程: {}", userId, e.getMessage());
            log.debug("知识图谱推荐职位异常详情", e);
            return Collections.emptyList();
        }
    }
}
