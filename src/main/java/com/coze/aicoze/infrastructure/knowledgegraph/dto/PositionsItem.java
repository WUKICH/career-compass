package com.coze.aicoze.infrastructure.knowledgegraph.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 知识图谱推荐接口返回的单条职位（字段名与图谱服务 JSON 一致，由 Jackson 反序列化）。
 */
@Data
public class PositionsItem {

    @JsonProperty("公司名")
    @JsonAlias("公司名称")
    private String compName;

    @JsonProperty("公司规模")
    private String compScale;

    @JsonProperty("工作地点")
    private String dq;

    @JsonProperty("岗位名称")
    private String title;

    @JsonProperty("联系人")
    private String recruiterName;

    @JsonProperty("职位薪资")
    private String salary;

    @JsonProperty("公司行业")
    private String compIndustry;

    @JsonProperty("公司阶段")
    private String compStage;

    @JsonProperty("职位福利")
    private List<String> labels;

    @JsonProperty("匹配度")
    private Double matchDegree;

    @JsonProperty("招聘链接")
    private String recruitmentUrl;
}
