package com.coze.aicoze.dto;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

@Data
public class SemanticAnalysisDTO {
    @Alias("相关性")
    private String relevance;
    @Alias("回复")
    private String reply;

    /**
     * 新版协议字段（推荐）：PASS / CLARIFY / BLOCK
     */
    @Alias("verdict")
    private String verdict;

    /**
     * 新版协议字段（推荐）：0~1 置信度/相关度
     */
    @Alias("score")
    private Double score;

    /**
     * 新版协议字段（可选）：将用户回答做规范化/改写，供后续流程使用（不影响闸门）
     */
    @Alias("rewrite")
    private String rewrite;
}
