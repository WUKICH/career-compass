package com.coze.aicoze.infrastructure.knowledgegraph.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * 人才画像请求体，对应 TalentPayload 数组
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TalentPayload {
    /**
     * 人才唯一 ID (必填)
     */
    private String id;

    /**
     * 姓名 (可选)
     */
    private String name;

    /**
     *画像来源
     */

    private String assessmentType = "system";
    /**
     * 技能列表 (可选)
     */
    private List<Skill> skills;

    /**
     * 理想薪资（K/月）(可选)
     */
    private Integer idealSalaryK;

    /**
     * 理想工作地点 (可选)
     */
    private String idealLocation;

    /**
     * 工作年限 (可选)
     */
    private Integer workYears;

    /**
     * 目标岗位 (可选)
     */
    private String targetPositionType;

    /**
     * 霍兰德测试代码 (可选)
     */
    private String hollandCode;
    /**
     * 内部技能对象
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Skill {
        /**
         * 技能名称 (必填)
         */
        private String name;

        /**
         * 技能熟练度 (必填)，支持 1/2/3 或 1.0/2.0/3.0
         */
        private Number level; // 使用 Number 可同时接收整数和小数
    }
}