package com.coze.aicoze.service.collect;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 体验版信息收集：步骤顺序与每步话术、选项、是否跳过语义校验（短指令等）。
 */
public final class CollectFlowSteps {

    private CollectFlowSteps() {
    }

    public static final List<StepConfig> STEPS = Arrays.asList(
            new StepConfig("experienceVersion", "在开始之前，请选择你想体验的版本，不同版本包含的功能有所不同哦～", Collections.emptyList(), true),
            new StepConfig("welcome", "小狐狸上线啦！这里是职业幻想世界的大门，等会我们会一起完成你的「职业身份设定」。准备好出发了吗？", Collections.emptyList(), true),
            new StepConfig("name", "第一章 · 起名仪式：在职业世界里，你需要一个专属的冒险者昵称。可以用字母拼出名字，也可以直接输入～", Collections.emptyList(), false),
            new StepConfig("workYears", "第二章 · 工作年限：请选择与你最接近的在职阶段（与后续匹配维度相关）。", Arrays.asList("应届", "1-3年", "3-5年", "5年以上"), false),
            new StepConfig("targetJob", "第三章 · 目标岗位：选一个你最感兴趣的岗位吧～", Arrays.asList("前端开发工程师", "后端开发工程师", "产品经理", "UI/UX 设计师", "数据分析师", "其他"), false),
            new StepConfig("expectedCity", "第四章 · 城市选择：选择一个你想要发展的城市吧～", Arrays.asList("上海", "北京", "广州", "杭州", "成都", "深圳", "其他"), false),
            new StepConfig("expectedSalary", "第五章 · 金币心愿：请选择你心中的期望月薪（可输入如 10k，或使用右侧滑块）。", Arrays.asList("8k", "10k", "15k", "20k", "25k", "其他"), false),
            new StepConfig("skill", "最后，请简单描述你的核心技能或亮点，方便顾问更懂你。", Collections.emptyList(), false)
    );

    @Data
    @AllArgsConstructor
    public static class StepConfig {
        private final String key;
        private final String question;
        private final List<String> presetOptions;
        private final boolean skipSemantic;
    }
}
