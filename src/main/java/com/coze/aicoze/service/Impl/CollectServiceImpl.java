package com.coze.aicoze.service.Impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.coze.aicoze.agent.ChatResult;
import com.coze.aicoze.agent.bots.CollectionAgent;
import com.coze.aicoze.agent.bots.ResumeParseAgent;
import com.coze.aicoze.dto.ChatInitRequest;
import com.coze.aicoze.dto.ChatPayload;
import com.coze.aicoze.dto.CollectConfirmRequest;
import com.coze.aicoze.dto.CollectSkillPreviewRequest;
import com.coze.aicoze.dto.CollectStepRequest;
import com.coze.aicoze.common.BizErrorCodes;
import com.coze.aicoze.exception.BusinessException;
import com.coze.aicoze.manager.AssessmentStore;
import com.coze.aicoze.manager.CollectStore;
import com.coze.aicoze.service.AssessmentService;
import com.coze.aicoze.service.CollectService;
import com.coze.aicoze.service.collect.CollectFlowSteps;
import com.coze.aicoze.service.collect.CollectStepFlowExecutor;
import com.coze.aicoze.util.CozeJsonExtractor;
import com.coze.aicoze.util.JsonParseUtil;
import com.coze.aicoze.util.ResumeSkillNormalizer;
import com.coze.aicoze.vo.CollectStepPayload;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectServiceImpl implements CollectService {

    private final CollectStore collectStore;
    private final AssessmentStore assessmentStore;
    private final CollectionAgent collectionAgent;
    private final ResumeParseAgent resumeParseAgent;
    private final AssessmentService assessmentService;
    private final CollectStepFlowExecutor collectStepFlowExecutor;

    private static final Pattern SALARY_PATTERN = Pattern.compile("(?i)(\\d+)\\s*k?");
    /**
     * 落库 {@code user.resume_claims} 的最大字符数（约 2 万汉字量级在 MySQL {@code TEXT} 65KB 限制内仍安全）。
     * 超长时不采用「只保留开头」，而是 {@link #limitResumeClaimsForStorage(String)} 首尾保留，减少项目/自评等在文末的信息丢失。
     */
    private static final int RESUME_CLAIMS_STORAGE_MAX_CHARS = 20_000;

    @Override
    @Transactional
    public CollectStepPayload collectStep(CollectStepRequest request) {
        return collectStepFlowExecutor.execute(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CollectStepPayload confirmCollect(CollectConfirmRequest request) {
        String sessionId = request.getSessionId();
        if (StrUtil.isBlank(sessionId)) {
            return CollectStepPayload.builder().code(0).msg("缺少 sessionId").build();
        }
        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            return CollectStepPayload.builder().code(0).msg("请提交确认信息").sessionId(sessionId).build();
        }
        Map<String, String> answers = collectStore.getOrCreateAnswers(sessionId);
        mergeFinalAnswersFromRequest(answers, request.getAnswers());
        ChatInitRequest initRequest = buildInitRequestFromAnswers(answers);
        initRequest.setSessionId(sessionId);
        boolean fromResumeParse = StrUtil.isNotBlank(answers.get("resumeFileName"));
        initRequest = validateCollectedRequest(initRequest, !fromResumeParse);
        if (initRequest == null) {
            return CollectStepPayload.builder()
                    .code(0)
                    .msg("用户信息输入有误")
                    .sessionId(sessionId)
                    .build();
        }
        if (fromResumeParse) {
            initRequest.setResumeSubmitted(true);
            initRequest.setResumeClaims(StrUtil.blankToDefault(answers.get("resumeClaims"), ""));
        }
        ChatPayload chatPayload = assessmentService.initAndFirstChat(initRequest);
        collectStore.remove(sessionId);
        return CollectStepPayload.builder()
                .code(1)
                .sessionId(chatPayload.getSessionId())
                .token(chatPayload.getToken())
                .content(chatPayload.getContent())
                .phase(chatPayload.getPhase())
                .suggestStartSkill(chatPayload.getSuggestStartSkill())
                .skillQuestionsPrepared(chatPayload.getSkillQuestionsPrepared())
                .build();
    }

    @Override
    public CollectStepPayload previewSkillProfile(CollectSkillPreviewRequest request) {
        String sessionId = StrUtil.isNotBlank(request.getSessionId()) ? request.getSessionId() : null;
        if (StrUtil.isBlank(sessionId)) {
            return CollectStepPayload.builder().code(0).msg("缺少 sessionId").build();
        }
        String raw = request.getUserInput() == null ? "" : request.getUserInput().trim();
        if (StrUtil.isBlank(raw)) {
            return CollectStepPayload.builder().code(0).msg("请输入核心技能").sessionId(sessionId).build();
        }
        Map<String, String> answers = collectStore.getOrCreateAnswers(sessionId);
        if (answers.size() != CollectFlowSteps.STEPS.size() - 1) {
            return CollectStepPayload.builder().code(0).msg("请先完成前面的信息收集").sessionId(sessionId).build();
        }
        CollectFlowSteps.StepConfig skillStep = CollectFlowSteps.STEPS.get(CollectFlowSteps.STEPS.size() - 1);
        String validateError = assessmentService.semanticAnalysis(sessionId, skillStep.getQuestion(), raw);
        if (validateError != null) {
            return CollectStepPayload.builder().code(0).msg(validateError).sessionId(sessionId).build();
        }
        Map<String, String> merged = new LinkedHashMap<>(answers);
        merged.put("skill", raw);
        ChatInitRequest built = buildInitRequestFromAnswers(merged);
        built.setSessionId(sessionId);
        try {
            ChatInitRequest profile = enrichProfileWithCollectionAgent(built);
            if (profile != null && StrUtil.isNotBlank(profile.getSkill())) {
                profile.setSkill(ResumeSkillNormalizer.normalize(profile.getSkill()));
            }
            return CollectStepPayload.builder()
                    .code(1)
                    .sessionId(sessionId)
                    .profilePreview(profile)
                    .build();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("previewSkillProfile failed, sessionId={}", sessionId, e);
            return CollectStepPayload.builder().code(0).msg("结构化画像生成失败，请稍后重试").sessionId(sessionId).build();
        }
    }

    @Override
    public CollectStepPayload parseResumeProfile(String sessionId, String experienceVersion, String resumeFileName,
                                                 String resumeTextForLlm, String resumeTextFull) {
        String sid = StrUtil.isNotBlank(sessionId) ? sessionId : String.valueOf(System.currentTimeMillis());
        if (StrUtil.isBlank(resumeTextForLlm)) {
            return CollectStepPayload.builder().code(0).msg("简历内容为空，请重新上传").sessionId(sid).build();
        }
        Map<String, String> answers = collectStore.getOrCreateAnswers(sid);
        if (StrUtil.isNotBlank(experienceVersion) && StrUtil.isBlank(answers.get("experienceVersion"))) {
            answers.put("experienceVersion", experienceVersion.trim());
        }
        if (StrUtil.isNotBlank(resumeFileName)) {
            answers.put("resumeFileName", resumeFileName.trim());
        }

        String prompt = buildResumeParsePrompt(resumeTextForLlm, resumeFileName);
        try {
            ChatResult result = resumeParseAgent.doChat(prompt, sid);
            String raw = result == null ? null : result.getContent();
            if (StrUtil.isBlank(raw)) {
                return CollectStepPayload.builder().code(0).msg("简历解析失败，请稍后重试").sessionId(sid).build();
            }
            String jsonOnly = CozeJsonExtractor.extractLastJsonObject(raw);
            ChatInitRequest parsed = JsonParseUtil.toBeanQuiet(StrUtil.isNotBlank(jsonOnly) ? jsonOnly : raw, ChatInitRequest.class);
            if (parsed == null) {
                return CollectStepPayload.builder().code(0).msg("简历解析结果格式错误，请稍后重试").sessionId(sid).build();
            }
            ChatInitRequest base = buildInitRequestFromAnswers(answers);
            base.setSessionId(sid);
            ChatInitRequest merged = mergeParsedProfile(base, parsed, experienceVersion, resumeTextForLlm);
            merged.setSkill(ResumeSkillNormalizer.normalize(merged.getSkill()));

            upsertAnswerIfPresent(answers, "name", merged.getName());
            upsertAnswerIfPresent(answers, "workYears", merged.getWorkYears());
            upsertAnswerIfPresent(answers, "targetJob", merged.getTargetJob());
            upsertAnswerIfPresent(answers, "expectedCity", merged.getExpectedCity());
            upsertAnswerIfPresent(answers, "expectedSalary", merged.getExpectedSalary());
            upsertAnswerIfPresent(answers, "skill", merged.getSkill());
            if (StrUtil.isNotBlank(merged.getExperienceVersion())) {
                answers.put("experienceVersion", merged.getExperienceVersion());
            }
            String claimsSource = StrUtil.isNotBlank(resumeTextFull) ? resumeTextFull : resumeTextForLlm;
            answers.put("resumeClaims", limitResumeClaimsForStorage(claimsSource));

            return CollectStepPayload.builder()
                    .code(1)
                    .sessionId(sid)
                    // 简历原文/摘要仅用于后端流程，不透出给前端确认页
                    .profilePreview(ChatInitRequest.builder()
                            .name(merged.getName())
                            .experienceVersion(merged.getExperienceVersion())
                            .workYears(merged.getWorkYears())
                            .targetJob(merged.getTargetJob())
                            .expectedCity(merged.getExpectedCity())
                            .expectedSalary(merged.getExpectedSalary())
                            .skill(merged.getSkill())
                            .resume(null)
                            .sessionId(merged.getSessionId())
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("parseResumeProfile failed, sessionId={}", sid, e);
            return CollectStepPayload.builder().code(0).msg("简历解析失败，请稍后重试").sessionId(sid).build();
        }
    }

    private void mergeFinalAnswersFromRequest(Map<String, String> answers, Map<String, String> requestAnswers) {
        if (requestAnswers == null || requestAnswers.isEmpty()) {
            return;
        }
        for (CollectFlowSteps.StepConfig step : CollectFlowSteps.STEPS) {
            String incoming = requestAnswers.get(step.getKey());
            if (incoming == null) {
                continue;
            }
            answers.put(step.getKey(), incoming.trim());
        }
        String workYears = requestAnswers.get("workYears");
        if (StrUtil.isNotBlank(workYears)) {
            answers.put("workYears", workYears.trim());
        } else if (StrUtil.isNotBlank(requestAnswers.get("major"))) {
            answers.put("workYears", requestAnswers.get("major").trim());
        }
        String sr = requestAnswers.get("skillRatings");
        if (StrUtil.isNotBlank(sr)) {
            String formatted = formatSkillRatingsToSkillString(sr);
            if (StrUtil.isNotBlank(formatted)) {
                answers.put("skill", formatted);
            }
        }
    }

    private ChatInitRequest enrichProfileWithCollectionAgent(ChatInitRequest request) {
        String requestString = request.toString();
        String sessionId = request.getSessionId();
        ChatResult collectionResult = collectionAgent.doChat(requestString, sessionId);
        ChatInitRequest initResult = JsonParseUtil.toBeanQuiet(collectionResult.getContent(), ChatInitRequest.class);
        if (initResult == null) {
            log.error("解析用户信息结果错误，sessionId = {}", sessionId);
            throw new BusinessException(BizErrorCodes.UPSTREAM_TIMEOUT, "智能体响应超时，请稍后再试。");
        }
        initResult.setExperienceVersion(request.getExperienceVersion());
        initResult.setWorkYears(request.getWorkYears());
        initResult.setSessionId(sessionId);
        return initResult;
    }

    private ChatInitRequest validateCollectedRequest(ChatInitRequest request, boolean useCollectionAgent) {
        ChatInitRequest initResult;
        if (!useCollectionAgent) {
            initResult = ChatInitRequest.builder().build();
        } else {
            try {
                initResult = enrichProfileWithCollectionAgent(request);
            } catch (Exception e) {
                // 兜底：当 CollectionAgent 超时/空响应时，不阻断“确认提交”主流程，直接使用用户确认页数据。
                log.warn("validateCollectedRequest fallback to request-only profile, sessionId={}", request.getSessionId(), e);
                initResult = ChatInitRequest.builder().build();
            }
        }
        if (StrUtil.isNotBlank(request.getName())) {
            initResult.setName(request.getName());
        }
        if (StrUtil.isNotBlank(request.getExperienceVersion())) {
            initResult.setExperienceVersion(request.getExperienceVersion());
        }
        if (StrUtil.isNotBlank(request.getWorkYears())) {
            initResult.setWorkYears(request.getWorkYears());
        }
        if (StrUtil.isNotBlank(request.getTargetJob())) {
            initResult.setTargetJob(request.getTargetJob());
        }
        if (StrUtil.isNotBlank(request.getExpectedCity())) {
            initResult.setExpectedCity(request.getExpectedCity());
        }
        if (StrUtil.isNotBlank(request.getExpectedSalary())) {
            initResult.setExpectedSalary(request.getExpectedSalary());
        }
        if (StrUtil.isNotBlank(request.getSkill())) {
            initResult.setSkill(request.getSkill());
        }
        // resume 字段语义：简历文件链接（OSS URL）。当前未接入 OSS，保持为空。
        initResult.setResume("");
        return initResult;
    }

    private String formatSkillRatingsToSkillString(String skillRatingsJson) {
        if (StrUtil.isBlank(skillRatingsJson)) {
            return "";
        }
        JSONArray arr = JSONUtil.parseArray(skillRatingsJson);
        if (arr == null || arr.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject o = arr.getJSONObject(i);
            if (o == null) {
                continue;
            }
            String name = StrUtil.trim(o.getStr("name"));
            if (StrUtil.isBlank(name)) {
                continue;
            }
            Double score = o.getDouble("score");
            if (score == null) {
                score = 0d;
            }
            if (sb.length() > 0) {
                sb.append("，");
            }
            sb.append(name).append(":").append(score);
        }
        return sb.toString();
    }

    private ChatInitRequest buildInitRequestFromAnswers(Map<String, String> answers) {
        String salary = answers.get("expectedSalary");
        if (StrUtil.isNotBlank(salary)) {
            java.util.regex.Matcher m = SALARY_PATTERN.matcher(salary);
            if (m.find()) {
                salary = m.group(1) + "k";
            }
        }
        return ChatInitRequest.builder()
                .name(answers.getOrDefault("name", ""))
                .experienceVersion(answers.getOrDefault("experienceVersion", ""))
                .workYears(answers.getOrDefault("workYears", ""))
                .targetJob(answers.getOrDefault("targetJob", ""))
                .expectedCity(answers.getOrDefault("expectedCity", ""))
                .expectedSalary(salary != null ? salary : "")
                .skill(answers.getOrDefault("skill", ""))
                // resume 字段语义：简历文件链接（OSS URL）。当前未接入 OSS，保持为空。
                .resume("")
                .build();
    }

    private String buildResumeParsePrompt(String resumeText, String resumeFileName) {
        String filePart = StrUtil.isNotBlank(resumeFileName) ? ("文件名：" + resumeFileName + "\n") : "";
        return """
                你是简历结构化助手。请从下面简历内容提取字段，严格只返回一个 JSON 对象，不要输出其它文字。
                字段：name, workYears, targetJob, expectedCity, expectedSalary, skill。
                未知字段输出空字符串；所有字段值为字符串。

                skill（核心技能）提取规则（必须遵守）：
                1. 输出 1～5 项，用中文逗号「，」分隔；无技能信息时填「未明确」。
                2. 每项为 2～12 字的**能力维度/技能标签**（名词或名词短语），面向求职匹配，不要照搬简历原句。
                3. 将同领域细项**合并归纳**为一项（例：体能训练、跳绳、追高 →「青少年体能与专项训练」；带小学生/初中生运动 → 并入「青少年体育教学」）。
                4. 禁止：完整句子、职责过程描述、带「可以/能够/善于/良好/灵活」的软技能长句、带评分的「名称:数字」格式。
                5. 结合 targetJob 优先保留与目标岗位最相关的 5 项以内能力；软技能最多保留 1 项且须凝练（如「家校沟通」）。

                %s简历内容：
                %s""".formatted(filePart, resumeText);
    }

    private ChatInitRequest mergeParsedProfile(ChatInitRequest base, ChatInitRequest parsed, String experienceVersion, String resumeText) {
        if (base == null) {
            base = ChatInitRequest.builder().build();
        }
        if (parsed == null) {
            parsed = ChatInitRequest.builder().build();
        }
        base.setName(firstNonBlank(parsed.getName(), base.getName()));
        base.setWorkYears(firstNonBlank(parsed.getWorkYears(), base.getWorkYears()));
        base.setTargetJob(firstNonBlank(parsed.getTargetJob(), base.getTargetJob()));
        base.setExpectedCity(firstNonBlank(parsed.getExpectedCity(), base.getExpectedCity()));
        base.setExpectedSalary(firstNonBlank(parsed.getExpectedSalary(), base.getExpectedSalary()));
        base.setSkill(firstNonBlank(parsed.getSkill(), base.getSkill()));
        // resume 字段语义：简历文件链接（OSS URL）。当前未接入 OSS，保持为空。
        base.setResume("");

        String exp = firstNonBlank(parsed.getExperienceVersion(), experienceVersion, base.getExperienceVersion());
        base.setExperienceVersion(exp == null ? "" : exp);
        return base;
    }

    private void upsertAnswerIfPresent(Map<String, String> answers, String key, String value) {
        if (answers == null || StrUtil.isBlank(key) || value == null) {
            return;
        }
        answers.put(key, value.trim());
    }

    private static final String RESUME_CLAIMS_MIDDLE_OMITTED_MARK =
            "\n\n...[中间内容已省略；上下分别为简历首部与尾部，供与测评结果对照]...\n\n";

    private static String limitResumeClaimsForStorage(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        String t = text.trim();
        final int max = RESUME_CLAIMS_STORAGE_MAX_CHARS;
        if (t.length() <= max) {
            return t;
        }
        int markLen = RESUME_CLAIMS_MIDDLE_OMITTED_MARK.length();
        int contentBudget = max - markLen;
        if (contentBudget < 2) {
            return t.substring(0, max);
        }
        int headLen = contentBudget * 62 / 100;
        int tailLen = contentBudget - headLen;
        if (headLen < 1) {
            headLen = 1;
            tailLen = contentBudget - 1;
        }
        if (tailLen < 1) {
            tailLen = 1;
            headLen = contentBudget - 1;
        }
        if (headLen + tailLen >= t.length()) {
            return t.substring(0, max);
        }
        return t.substring(0, headLen) + RESUME_CLAIMS_MIDDLE_OMITTED_MARK + t.substring(t.length() - tailLen);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }
}
