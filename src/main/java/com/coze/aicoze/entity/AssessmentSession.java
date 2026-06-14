package com.coze.aicoze.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("assessment_session")
public class AssessmentSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private Integer participantId;
    private String experienceVersion;
    private String currentPhase;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
