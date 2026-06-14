package com.coze.aicoze.infrastructure.knowledgegraph.Client;

import com.coze.aicoze.infrastructure.knowledgegraph.config.KnowledgeGraphFeignConfig;
import com.coze.aicoze.infrastructure.knowledgegraph.dto.BatchUpsertResponse;
import com.coze.aicoze.infrastructure.knowledgegraph.dto.PositionsItem;
import com.coze.aicoze.infrastructure.knowledgegraph.dto.RecommendationRequest;
import com.coze.aicoze.infrastructure.knowledgegraph.dto.TalentPayload;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "KnowledgeGraphClient",url = "${kg.service.url}", configuration = KnowledgeGraphFeignConfig.class)
public interface KnowledgeGraphFeignClient {

    @PostMapping("/talents/batch_upsert")
    BatchUpsertResponse batchUpsert(@RequestBody List<TalentPayload> talents);

    @PostMapping("/recommend")
    List<PositionsItem> recommendPositions(@Valid @RequestBody RecommendationRequest recommendationRequest);
}
