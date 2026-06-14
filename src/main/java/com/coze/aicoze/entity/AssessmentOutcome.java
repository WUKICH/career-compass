package com.coze.aicoze.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("assessment_outcome")
public class AssessmentOutcome {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private Integer participantId;
    private String skillAdjusted;
    private String hollandCode;
    private String salaryAdjusted;
    private String jobAdjusted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
