package com.coze.aicoze.service.collect;

import cn.hutool.core.util.StrUtil;
import com.coze.aicoze.dto.CollectStepRequest;
import com.coze.aicoze.manager.AssessmentStore;
import com.coze.aicoze.manager.CollectStore;
import com.coze.aicoze.service.AssessmentService;
import com.coze.aicoze.vo.CollectStepPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 收集向导单步提交编排：bootstrap、顺序校验、语义校验、写答案、下一步话术。
 */
@Component
@RequiredArgsConstructor
public class CollectStepFlowExecutor {

    private final CollectStore collectStore;
    private final AssessmentStore assessmentStore;
    private final AssessmentService assessmentService;

    public CollectStepPayload execute(CollectStepRequest request) {
        String sessionId = StrUtil.isNotBlank(request.getSessionId()) ? request.getSessionId() : String.valueOf(System.currentTimeMillis());
        List<CollectFlowSteps.StepConfig> steps = CollectFlowSteps.STEPS;

        if (Boolean.TRUE.equals(request.getBootstrap())) {
            collectStore.getOrCreateAnswers(sessionId);
            CollectFlowSteps.StepConfig first = steps.get(0);
            return CollectStepPayload.builder()
                    .code(1)
                    .nextQuestion(first.getQuestion())
                    .nextQuestionKey(first.getKey())
                    .presetOptions(first.getPresetOptions())
                    .sessionId(sessionId)
                    .build();
        }

        Map<String, String> answers = collectStore.getOrCreateAnswers(sessionId);
        int currentIndex = answers.size();
        if (currentIndex >= steps.size()) {
            return CollectStepPayload.builder()
                    .code(0)
                    .msg("信息已收集完成，请前往确认页提交")
                    .sessionId(sessionId)
                    .build();
        }

        CollectFlowSteps.StepConfig currentStep = steps.get(currentIndex);
        if (!currentStep.getKey().equals(request.getQuestionKey())) {
            return CollectStepPayload.builder().code(0).msg("请按顺序回答当前问题：" + currentStep.getQuestion()).build();
        }

        String userInput = request.getUserInput() == null ? "" : request.getUserInput().trim();
        String validateError = null;
        if (!currentStep.isSkipSemantic()) {
            validateError = assessmentService.semanticAnalysis(sessionId, steps.get(currentIndex).getQuestion(), userInput);
        }
        if (validateError != null) {
            return CollectStepPayload.builder()
                    .code(0)
                    .msg(validateError)
                    .sessionId(sessionId)
                    .build();
        }

        answers.put(currentStep.getKey(), userInput);
        if ("experienceVersion".equals(currentStep.getKey())) {
            AssessmentStore.SessionAssessment session = assessmentStore.init(sessionId);
            session.setExperienceVersion(userInput);
        }

        boolean isLast = (currentIndex + 1) >= steps.size();
        if (isLast) {
            return CollectStepPayload.builder()
                    .code(1)
                    .sessionId(sessionId)
                    .msg("信息已收集，请前往确认页核对并提交")
                    .build();
        }

        CollectFlowSteps.StepConfig nextStep = steps.get(currentIndex + 1);
        String nextQuestionText = decorateNextQuestion(nextStep, answers);
        return CollectStepPayload.builder()
                .code(1)
                .msg(null)
                .nextQuestion(nextQuestionText)
                .nextQuestionKey(nextStep.getKey())
                .presetOptions(nextStep.getPresetOptions())
                .sessionId(sessionId)
                .build();
    }

    private String decorateNextQuestion(CollectFlowSteps.StepConfig nextStep, Map<String, String> answers) {
        String base = nextStep.getQuestion();
        switch (nextStep.getKey()) {
            case "welcome": {
                String v = answers.get("experienceVersion");
                if (StrUtil.isNotBlank(v)) {
                    return "好的，你选择了「" + v + "」！让我们开始职业探索之旅吧～ 🚀\n\n" + base;
                }
                return base;
            }
            case "workYears": {
                String n = answers.get("name");
                if (StrUtil.isNotBlank(n)) {
                    return "好的，「" + n + "」冒险者，起名仪式完成！叮铃～ ✨\n\n" + base;
                }
                return base;
            }
            case "targetJob": {
                String wy = answers.get("workYears");
                if (StrUtil.isNotBlank(wy)) {
                    return "你的工作年限阶段小狐狸记下啦～\n\n" + base;
                }
                return base;
            }
            case "expectedCity": {
                String j = answers.get("targetJob");
                if (StrUtil.isNotBlank(j)) {
                    return "「" + j + "」是个很棒的岗位，小狐狸已经帮你记下啦～\n\n" + base;
                }
                return base;
            }
            case "expectedSalary": {
                String c = answers.get("expectedCity");
                if (StrUtil.isNotBlank(c)) {
                    return "「" + c + "」是个很棒的城市，小狐狸已经帮你记下啦～\n\n" + base;
                }
                return base;
            }
            case "skill": {
                String sal = answers.get("expectedSalary");
                if (StrUtil.isNotBlank(sal)) {
                    return "期望薪资「" + sal + "」小狐狸记下啦～\n\n" + base;
                }
                return base;
            }
            default:
                return base;
        }
    }
}
