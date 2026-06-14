package com.coze.aicoze.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("assessment_participant")
public class AssessmentParticipant {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    /** 工作年限档位：{@code 0}、{@code 1-3}、{@code 3-5}、{@code 5+}、{@code 其他} */
    private String workYears;
    private String targetJob;
    private String skill;
    private String expectedCity;
    private String expectedSalary;
    @TableField("resume_path")
    private String resume;
    private Boolean resumeSubmitted;
    private String resumeClaims;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
