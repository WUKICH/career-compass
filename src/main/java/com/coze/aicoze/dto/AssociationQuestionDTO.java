package com.coze.aicoze.dto;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

import java.util.List;

@Data
public class AssociationQuestionDTO {
    @Alias("message")
    private String question;    //原始问题

    @Alias("relevant")
    private List<String> relevantAnswer;  //问题联想相关答案
}
