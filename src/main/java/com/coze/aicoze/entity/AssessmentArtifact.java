package com.coze.aicoze.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("assessment_artifact")
public class AssessmentArtifact {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private Integer participantId;
    private String artifactType;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
