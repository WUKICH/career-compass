package com.coze.aicoze.infrastructure.knowledgegraph.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationRequest {
    
    @NotBlank(message = "人才ID不能为空")
    @JsonProperty("talent_id")
    private String userId;
    
    @NotNull(message = "top_k不能为空")
    @Min(value = 1, message = "top_k至少为1")
    private Integer top_k;
}