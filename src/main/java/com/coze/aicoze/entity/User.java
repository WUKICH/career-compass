package com.coze.aicoze.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API 层沿用的参与者模型，映射 {@code assessment_participant} 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("assessment_participant")
public class User extends AssessmentParticipant {
}
