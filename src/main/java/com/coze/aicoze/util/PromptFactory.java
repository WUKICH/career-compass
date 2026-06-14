package com.coze.aicoze.util;

import org.springframework.stereotype.Component;

@Component
public class PromptFactory {

  /** 完全版 init 静态欢迎语（不调 Chat Bot 首轮）。 */
    public String buildWelcomeMessage(String targetJob, String expectedCity) {
        String job = (targetJob == null || targetJob.isBlank()) ? "你的目标岗位" : targetJob;
        String city = (expectedCity == null || expectedCity.isBlank()) ? "期望城市" : expectedCity;
        return String.format("""
                资料已确认成功！我是你的 AI 职业测评助手。
                你已设定目标岗位为「%s」，期望工作城市为「%s」。
                接下来我们可以聊聊你的职业规划；若准备好了，可点击「开始能力测评」进入技能测试。
                """, job, city);
    }

    /** 体验版确认后首轮固定欢迎语：简短确认资料并引导进入技能测评（不调主聊 Bot）。 */
    public String buildTrialFirstReply(String targetJob, String expectedCity) {
        String job = (targetJob == null || targetJob.isBlank()) ? "你的目标岗位" : targetJob.strip();
        String city = (expectedCity == null || expectedCity.isBlank()) ? "" : expectedCity.strip();
        if (city.isEmpty()) {
            return """
                    你好，我是小狐狸·职业引导官。
                    已收到你的资料，目标岗位为「%s」。
                    正在为你准备技能测评题目，准备完成后请点击下方「开始能力测评」。
                    """.formatted(job);
        }
        return """
                你好，我是小狐狸·职业引导官。
                已收到你的资料，目标岗位「%s」，期望城市「%s」。
                正在为你准备技能测评题目，准备完成后请点击下方「开始能力测评」。
                """.formatted(job, city);
    }

    public String buildFirstMessage(String experienceVersion, String workYears, String skill, String targetJob, String expectedCity, String expectedSalary) {
        String wy = (workYears == null || workYears.isBlank()) ? "未填写" : workYears;
        return String.format("""
                以下是用户基本信息：
                工作年限/年资：%s
                目标岗位：%s
                核心技能：%s
                期望城市：%s
                期望薪资：%s。
                """,
                wy, targetJob, skill, expectedCity, expectedSalary);
    }

    public String buildSkillAssessmentPrompt(String answer) {
        return """
                【阶段 B — 答题分析】
                用户已提交技能测试作答。请按系统提示词「阶段 B」输出合法 JSON（含题目总数、答对题数、正确率、测试综合得分、总结、核心技能分析），然后换行单独输出 [SKILL_END]。
                说明：主观题未填写的，系统已记为「不会」，请纳入各核心技能评分与评价。

                以下是用户作答：
                """
                + answer;
    }

    public String buildSkillTestPrompt(String skill, String targetJob) {
        String skills = (skill == null || skill.isBlank()) ? "未填写" : skill.strip();
        String job = (targetJob == null || targetJob.isBlank()) ? "未填写" : targetJob.strip();
        return String.format("""
                【阶段 A — 仅生成题目】
                目标岗位：「%s」
                核心技能：「%s」

                请生成技能测试题，严格只输出一个 JSON 对象（不要 Markdown、不要代码块围栏、不要前后说明），根对象有且仅有键「题目列表」。

                题量（固定）：恰好 5 道选择题 + 恰好 2 道主观题，共 7 道；须覆盖上述每一项核心技能（可一题多技能，但每个技能至少有一题归属）。

                选择题字段：类型=选择题，核心技能，题干，选项（恰好4项 key A-D + text），正确答案（A/B/C/D 之一）。
                主观题字段：类型=主观题，核心技能，题干，要求（可选）。

                禁止输出：题目总数、得分、核心技能分析、[SKILL_END] 及任何非 JSON 内容。

                示例结构（勿照抄内容）：
                {"题目列表":[{"类型":"选择题","核心技能":"示例","题干":"**题干**","选项":[{"key":"A","text":"..."},{"key":"B","text":"..."},{"key":"C","text":"..."},{"key":"D","text":"..."}],"正确答案":"A"},{"类型":"主观题","核心技能":"示例","题干":"**开放题**","要求":"简要说明"}]}
                """, job, skills);
    }

    public String buildCareerAssessmentPrompt(String answer) {
        return """
                【阶段 B — 霍兰德答题分析】
                用户已提交 5 道霍兰德情境选择题作答。请按系统提示词「阶段 B」与固定计分规则（主维+2、副维+1）汇总六维得分，输出合法 JSON（霍兰德职业兴趣代码、类型组合、霍兰德六个维度分析、与目标岗位匹配度分析），然后换行单独输出 [HOLLAND_END]。
                须使用阶段 A 题目中各选项的「计分」字段，不得臆造加分。

                以下是用户作答：
                """
                + answer;
    }

    public String buildCareerTestPrompt(String skill, String targetJob) {
        String skills = (skill == null || skill.isBlank()) ? "未填写" : skill.strip();
        String job = (targetJob == null || targetJob.isBlank()) ? "未填写" : targetJob.strip();
        return String.format("""
                【阶段 A — 仅生成霍兰德题目】
                目标岗位：「%s」
                核心技能：「%s」

                请生成 RIASEC 霍兰德职业兴趣测试题，严格只输出一个 JSON 对象（不要 Markdown、不要代码块围栏、不要前后说明），根键必须为「questions」，数组长度恰好 5。

                每题结构：类型=选择题，题干，本题主维度（R/I/A/S/E/C 单字母），本题副维度（单字母且不同于主维度），选项（恰好4项，每项含 key A-D、text、计分{主维,副维}）。
                5 题全部选项的计分合并后，R/I/A/S/E/C 六维每个至少出现 1 次。
                选项 text 中不得出现维度代号或「现实型」等字样。

                JSON 闭合后另起一行输出：[HOLLAND_TEST]
                禁止输出：霍兰德职业兴趣代码、六维分析、岗位匹配、[HOLLAND_END] 及任何非 JSON 说明。

                示例结构（勿照抄内容，须凑满 5 题）：
                {"questions":[{"类型":"选择题","题干":"**情境**...","本题主维度":"I","本题副维度":"E","选项":[{"key":"A","text":"...","计分":{"主维":"I","副维":"C"}},{"key":"B","text":"...","计分":{"主维":"E","副维":"S"}},{"key":"C","text":"...","计分":{"主维":"C","副维":"I"}},{"key":"D","text":"...","计分":{"主维":"A","副维":"S"}}]}]}
                [HOLLAND_TEST]
                """, job, skills);
    }

    public String buildSemanticAnalysisMessage(String agentQuestion, String userAnswer) {
        return String.format("""
                问题：%s;
                用户回答：%s。
                """, agentQuestion, userAnswer);
    }

    /**
     * 联想 Agent 的「用户消息」：主助手输出被整段传入，需与 Bot 侧提示词一致——
     * {@code relevant} 中每一项都是用户可一键发送的完整答句，且应覆盖段内所有待答点，避免只贴第一个问法、碎词或与问题无关。
     */
    public String buildAssociationUserMessage(String assistantMessage) {
        String body = assistantMessage == null ? "" : assistantMessage.strip();
        String header = """
                【输入说明】
                下面 <<<ASSISTANT>>> 与 <<<END>>> 之间，是「主对话助手」刚刚发给用户的完整消息（可含多个连续提问、说明或寒暄；不是用户的提问）。

                【输出要求】
                只输出一个 JSON 对象（不要 Markdown、不要前后说明），且键名必须如下：
                {"message":"...","relevant":["...","...","..."]}

                - message：不超过 80 字，客观概括助手在向用户收集哪些信息；若有多问，请概括整体，不要只写第一句。
                - relevant：固定 3 条（若确有困难再 4 条）。每一条必须是用户发给助手的**一整条完整中文回复**（可 1～3 句，单条一般不超过 200 字），且必须**同时合理覆盖** ASSISTANT 段里所有需要用户回答的要点；禁止只答第一个问题；禁止输出互不相关的单词/标签碎片；禁止与助手问题明显无关的内容。若助手只有一个问题，则给 3 条不同角度的完整短答。

                <<<ASSISTANT>>>
                """;
        String footer = """

                <<<END>>>
                """;
        return header + body + footer;
    }

    /**
     * 根据简历摘录与综合评价 JSON，生成「简历表述 vs 测评结果」对照建议（模型须只输出一个 JSON 对象）。
     */
    public String buildPreliminaryRecommendationPrompt(String targetJob, String skill, String expectedCity,
                                                       String skillReportJson, String hollandReportJson) {
        return """
                【任务 — 初步岗位推荐】
                根据下方技能测评 JSON 与霍兰德测评 JSON，为用户生成 3 个初步推荐岗位。
                严格只输出一个合法 JSON 对象（不要 Markdown、不要代码块、不要前后说明），结构如下：
                {"status":"ready","引导语":"...","推荐岗位":[{"岗位名称":"...","匹配理由":"..."},...共3项],"确认提问":"以上推荐是否符合您的预期？请回复：符合 / 不符合 / 还行"}

                目标岗位：%s
                核心技能：%s
                期望城市：%s

                技能测评 JSON：
                %s

                霍兰德测评 JSON：
                %s
                """.formatted(
                blank(targetJob), blank(skill), blank(expectedCity),
                blank(skillReportJson), blank(hollandReportJson));
    }

    public String buildConsensusUserMessage(String preliminaryRecJson, String skillReportJson,
                                            String hollandReportJson, String userMessage) {
        return """
                【共识对话 — 用户本轮回复】
                请根据系统提示词中的共识规则处理用户回复。
                仅输出一个合法 JSON 对象（不要 Markdown）：
                - 若仍需追问：{"status":"continue","回复":"..."}
                - 若共识已完成：{"status":"complete","共识结论":{"首选岗位方向":"...","用户偏好关键词":["..."],"排除岗位":["..."],"备注":"..."},"推荐职位":[{"岗位名称":"...","匹配度":"...","职位薪资":"..."}]}

                初步推荐 JSON：
                %s

                技能测评 JSON：
                %s

                霍兰德测评 JSON：
                %s

                用户回复：
                %s
                """.formatted(
                blank(preliminaryRecJson), blank(skillReportJson),
                blank(hollandReportJson), blank(userMessage));
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? "（无）" : s.strip();
    }

    public String buildResumeOptimizationPrompt(String resumeClaimsExcerpt, String careerAssessmentJson) {
        String claims = resumeClaimsExcerpt == null ? "" : resumeClaimsExcerpt;
        String report = careerAssessmentJson == null ? "" : careerAssessmentJson;
        return """
                你是简历与测评对照顾问。请对比「简历摘录」与「综合测评报告 JSON」中的核心技能评估、霍兰德等事实，找出简历中可能夸大、与测评不一致或表述不够稳妥之处，给出可执行的修改建议。
                若摘录中无明显可对照点，可输出较少条目或一条总结性建议。
                严格只输出一个 JSON 对象，不要 Markdown、不要其它说明文字。JSON 结构必须为：
                {"摘要":"string","建议项":[{"简历表述":"string","测评依据":"string","优化建议":"string"}]}
                字段名必须完全一致（中文键名）。建议项可为数组，允许为空数组。

                简历摘录：
                """
                + claims
                + "\n\n综合测评报告（JSON）：\n"
                + report;
    }
}


