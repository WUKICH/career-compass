<template>
  <div class="test-container">
    <el-card class="test-card">
      <template #header>
        <div class="card-header">
          <div class="header-main">
            <span class="header-title">{{ testType === 'skill' ? '职业技能测试' : '霍兰德职业兴趣测试' }}</span>
            <span class="header-subtitle">请根据真实想法作答，系统将基于结果生成个性化分析</span>
          </div>
          <div class="header-side">
            <el-tag type="warning" effect="plain">共 {{ testQuestions.length || 0 }} 题</el-tag>
          </div>
        </div>
      </template>
      
      <div v-if="loading" class="loading-wrapper">
        <el-skeleton :rows="5" animated />
      </div>
      
      <div v-else-if="testQuestions && testQuestions.length > 0" class="test-content-wrapper">
        <!-- 可滚动的内容区域 -->
        <div class="scrollable-content">
          <!-- 答案填写区域 -->
          <div class="answer-section">
            <el-form ref="answerFormRef" :model="answerData" label-width="100px">
              <!-- 遍历所有题目 -->
              <div v-for="(question, index) in testQuestions" :key="index" class="question-item">
                <!-- 选择题（多选：复选框；单选：单选框） -->
                <div v-if="question.类型 === '选择题' && isMultiChoice(question)" class="choice-question">
                  <div class="question-title" v-html="renderMarkdown(question.题干)"></div>
                  <el-checkbox-group
                    v-model="answerData[`question_${index}`]"
                    class="options-group options-group-multi"
                  >
                    <el-checkbox
                      v-for="option in (question.选项 || question.options || [])"
                      :key="option.key"
                      :label="option.key"
                      class="option-item"
                    >
                      <span class="option-key">{{ option.key }}.</span>
                      <span class="option-text">{{ option.text }}</span>
                    </el-checkbox>
                  </el-checkbox-group>
                </div>
                <div v-else-if="question.类型 === '选择题'" class="choice-question">
                  <div class="question-title" v-html="renderMarkdown(question.题干)"></div>
                  <el-radio-group
                    v-model="answerData[`question_${index}`]"
                    class="options-group"
                  >
                    <el-radio
                      v-for="option in (question.选项 || question.options || [])"
                      :key="option.key"
                      :label="option.key"
                      class="option-item"
                    >
                      <span class="option-key">{{ option.key }}.</span>
                      <span class="option-text">{{ option.text }}</span>
                    </el-radio>
                  </el-radio-group>
                </div>
                
                <!-- 主观题 -->
                <div v-else-if="question.类型 === '主观题'" class="subjective-question">
                  <div class="question-title" v-html="renderMarkdown(question.题干)"></div>
                  <div v-if="question.要求" class="question-requirement">
                    <el-tag type="info" size="small" effect="plain">要求：{{ question.要求 }}</el-tag>
                  </div>
                  <el-input
                    v-model="answerData[`question_${index}`]"
                    type="textarea"
                    :rows="6"
                    placeholder="请输入您的答案"
                    class="subjective-input"
                  />
                </div>
                <!-- 兜底：字段已归一化但类型未识别时，仍按选择题展示 -->
                <div v-else-if="(question.选项 || question.options || []).length" class="choice-question">
                  <div class="question-title" v-html="renderMarkdown(question.题干 || question.stem)"></div>
                  <el-radio-group
                    v-model="answerData[`question_${index}`]"
                    class="options-group"
                  >
                    <el-radio
                      v-for="option in (question.选项 || question.options || [])"
                      :key="option.key"
                      :label="option.key"
                      class="option-item"
                    >
                      <span class="option-key">{{ option.key }}.</span>
                      <span class="option-text">{{ option.text }}</span>
                    </el-radio>
                  </el-radio-group>
                </div>
              </div>
            </el-form>
          </div>
          
          <!-- 底部留白，避免被固定按钮遮挡 -->
          <div class="bottom-spacer"></div>
        </div>
        
        <!-- 固定在底部的提交按钮 -->
        <div class="action-buttons-fixed">
          <el-button
            type="primary"
            @click="handleSubmit"
            :loading="isSubmitting"
            size="large"
            class="submit-btn"
          >
            {{ isSubmitting ? '提交中...' : '提交答案' }}
          </el-button>
        </div>
      </div>
      
      <div v-else-if="!loading" class="error-wrapper">
        <el-result
          icon="error"
          title="未找到测试题目"
          sub-title="请返回聊天页面重新触发测试"
        >
          <template #extra>
            <el-button type="primary" @click="goBack">返回聊天</el-button>
          </template>
        </el-result>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { submitSkillAnswerApi, submitCareerAnswerApi } from '@/api/chat';
import { useUserStore } from '@/stores/user';
import { persistWorkflowFromResponse } from '@/utils/workflowPhase';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const testType = computed(() => route.params.type || 'skill');
const loading = ref(true);
const testData = ref(null);
const testQuestions = ref([]);
const answerFormRef = ref(null);
const isSubmitting = ref(false);

const answerData = ref({});

const CAREER_HANDOFF_MESSAGE = '职业兴趣测评即将开始，请做好准备！';

/** 后端 multiSelect / 题干「多选」→ 复选框 */
function isMultiChoice(question) {
  if (!question) return false;
  if (question.multiSelect === true || question.多选 === true) return true;
  const stem = question.题干 || question.stem || '';
  return /[(（]多选[)）]/.test(stem);
}

function normalizeQuestionType(type, defaultType = '选择题') {
  const raw = type == null ? '' : String(type).trim();
  if (!raw) return defaultType;
  const lower = raw.toLowerCase();
  if (raw === '选择题' || lower === 'choice' || lower === 'single_choice') return '选择题';
  if (raw === '主观题' || lower === 'subjective') return '主观题';
  return raw;
}

function normalizeOptions(options) {
  if (!Array.isArray(options)) return [];
  return options.map((opt) => {
    if (typeof opt === 'string') {
      const m = String(opt).trim().match(/^([A-D])[.、\s]\s*(.*)$/);
      return m ? { key: m[1], text: m[2] } : { key: '', text: String(opt) };
    }
    if (!opt || typeof opt !== 'object') return null;
    return {
      key: opt.key || opt.键 || opt.label || '',
      text: opt.text || opt.内容 || opt.content || opt.label || ''
    };
  }).filter((opt) => opt && (opt.text || opt.key));
}

function normalizeQuestionItem(q, defaultType = '选择题') {
  if (!q || typeof q !== 'object') return q;
  const 类型 = normalizeQuestionType(q.类型 || q.type, defaultType);
  const 题干 = q.题干 || q.stem || q.content || q.question || '';
  const 选项 = normalizeOptions(q.选项 || q.options);
  const mapped = {
    ...q,
    类型,
    题干,
    选项,
    核心技能: q.核心技能 || q.coreSkill || '',
    正确答案: q.正确答案 || q.correctAnswer || '',
    要求: q.要求 || q.requirement || '',
    多选: q.多选 === true || q.multiSelect === true || isMultiChoice({ ...q, 类型, 题干, 选项 })
  };
  return mapped;
}

function normalizeQuestions(list, defaultType = '选择题') {
  if (!Array.isArray(list)) return [];
  return list.map((q) => normalizeQuestionItem(q, defaultType));
}

function extractQuestionList(raw) {
  if (!raw || typeof raw !== 'object') return [];
  if (Array.isArray(raw)) return raw;
  if (Array.isArray(raw.questionList)) return raw.questionList;
  if (Array.isArray(raw.questions)) return raw.questions;
  if (Array.isArray(raw.题目列表)) return raw.题目列表;
  return [];
}

function resetAnswerFields(questions) {
  answerData.value = {};
  questions.forEach((q, index) => {
    if (q.类型 === '选择题') {
      answerData.value[`question_${index}`] = isMultiChoice(q) ? [] : null;
    } else if (q.类型 === '主观题') {
      answerData.value[`question_${index}`] = '';
    }
  });
}

function loadQuestionsFromRaw(raw, kind) {
  if (!raw || typeof raw !== 'object') return false;
  const questionList = extractQuestionList(raw);
  if (!Array.isArray(questionList) || questionList.length === 0) return false;
  testData.value = raw;
  const defaultType = kind === 'career' ? '选择题' : '选择题';
  testQuestions.value = normalizeQuestions(questionList, defaultType);
  const invalid = testQuestions.value.find((q) => !q?.题干 || !(q.选项 || []).length);
  if (invalid && kind === 'career') {
    console.warn('霍兰德题目字段不完整，请检查 Coze 输出或重新生成题目', testQuestions.value);
  }
  resetAnswerFields(testQuestions.value);
  return true;
}

function cachePendingCareerFromResponse(data) {
  if (!data?.careerTestQuestions) return;
  userStore.setPendingCareerTest({
    testType: 'career',
    content: data.content || CAREER_HANDOFF_MESSAGE,
    careerTestQuestions: data.careerTestQuestions,
    phase: data.phase || null
  });
}

/** 后端 CONTINUE：仍有 skillTestQuestions、无 skillReport */
function isSkillContinueResponse(data) {
  return !!(data && !data.skillReport && data.skillTestReady && data.skillTestQuestions);
}

onMounted(() => {
  loading.value = true;
  const kind = testType.value === 'skill' ? 'skill' : 'career';
  let raw = userStore.consumePendingTestPayload(kind);
  if (!raw && kind === 'career') {
    const pending = userStore.getPendingCareerTest();
    if (pending?.careerTestQuestions) {
      raw = pending.careerTestQuestions;
    }
  }
  if (!loadQuestionsFromRaw(raw, kind)) {
    ElMessage.error('未找到测试题目，请返回聊天页点击「开始测试」');
  }
  loading.value = false;
});

// 渲染 Markdown（用于题干中的粗体等格式）
const renderMarkdown = (text) => {
  if (!text) return '';
  
  // 简单的 Markdown 转 HTML（仅支持基本格式）
  let html = text
    .replace(/\*\*(.*?)\*\*/gim, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/gim, '<em>$1</em>')
    .replace(/\n/gim, '<br>');
  
  return html;
};

const handleSubmit = async () => {
  try {
    const answerText = [];

    for (let index = 0; index < testQuestions.value.length; index += 1) {
      const question = testQuestions.value[index];
      const answer = answerData.value[`question_${index}`];
      const questionNum = index + 1;

      if (question.类型 === '选择题') {
        const options = question.选项 || question.options || [];
        const cleanTitle = (question.题干 || '').replace(/\*\*/g, '').replace(/\*/g, '');
        if (isMultiChoice(question)) {
          const keys = Array.isArray(answer) ? [...answer].filter(Boolean).sort() : [];
          if (keys.length === 0) {
            ElMessage.warning(`请完成第 ${questionNum} 道选择题`);
            return;
          }
          const parts = keys.map((k) => {
            const o = options.find((opt) => opt.key === k);
            return o ? `${k}. ${o.text}` : k;
          });
          answerText.push(`${questionNum}. ${cleanTitle}\n答案: ${parts.join('；')}`);
        } else if (answer === null || answer === undefined || answer === '') {
          ElMessage.warning(`请完成第 ${questionNum} 道选择题`);
          return;
        } else {
          const selectedOption = options.find((opt) => opt.key === answer);
          const optionText = selectedOption ? selectedOption.text : '未选择';
          answerText.push(`${questionNum}. ${cleanTitle}\n答案: ${answer}. ${optionText}`);
        }
      } else if (question.类型 === '主观题') {
        const cleanTitle = (question.题干 || '').replace(/\*\*/g, '').replace(/\*/g, '');
        const subjectiveAnswer = answer && String(answer).trim() ? String(answer).trim() : '不会';
        answerText.push(`${questionNum}. ${cleanTitle}\n答案: ${subjectiveAnswer}`);
      }
    }

    if (answerText.length === 0) {
      ElMessage.warning('请至少完成全部选择题');
      return;
    }

    isSubmitting.value = true;
    
    const sessionId = userStore.getSessionId();
    
    if (!sessionId) {
      ElMessage.error('会话ID不存在，请重新初始化');
      router.push('/form');
      return;
    }
    
    // 将答案组合成字符串发送给后端
    const finalAnswer = answerText.join('\n\n');
    
    // 根据测试类型调用不同的接口
    const apiCall = testType.value === 'skill' 
      ? submitSkillAnswerApi 
      : submitCareerAnswerApi;
    
    const response = await apiCall(sessionId, finalAnswer);
    
    if (response && response.code === 1) {
      const data = response.data || {};
      persistWorkflowFromResponse(sessionId, data);

      if (testType.value === 'skill') {
        if (isSkillContinueResponse(data)) {
          if (loadQuestionsFromRaw(data.skillTestQuestions, 'skill')) {
            ElMessage.info(data.content || '请继续完成技能测试。');
          } else {
            ElMessage.warning('续答题目格式无效，请返回聊天页重试。');
          }
          return;
        }

        if (data.skillReport) {
          userStore.markTestAsCompleted('skill');
          userStore.setSkillReport(data.skillReport);
          cachePendingCareerFromResponse(data);
          ElMessage.success('技能测试答案提交成功！正在生成报告...');
          setTimeout(() => {
            router.push('/report/skill');
          }, 300);
          return;
        }

        userStore.markTestAsCompleted('skill');
        cachePendingCareerFromResponse(data);
        ElMessage.warning('技能测评已结束，但未返回结构化报告，请返回聊天继续。');
        setTimeout(() => {
          router.push('/chat');
        }, 800);
        return;
      }

      userStore.markTestAsCompleted(testType.value);
      if (testType.value === 'career') {
        if (data.hollandReport) {
          userStore.setHollandReport(data.hollandReport);
        }
        userStore.setPendingCareerTest({
          postCareerContent: data.content ? String(data.content).trim() : '',
          preliminaryRecommendation: data.preliminaryRecommendation || null,
          careerAssessmentReport: data.careerAssessmentReport || null,
          phase: data.phase || null
        });
        if (data.careerAssessmentReport) {
          userStore.setCareerAssessmentReport(data.careerAssessmentReport);
        }
        ElMessage.success('兴趣测试完成，正在生成报告…');
        setTimeout(() => {
          router.push('/report/holland');
        }, 300);
      }
    } else {
      ElMessage.error(response?.msg || '提交失败，请重试');
    }
  } catch (error) {
    console.error('提交答案失败:', error);
    ElMessage.error(error.message || '提交失败，请重试');
  } finally {
    isSubmitting.value = false;
  }
};

// 错误页面的返回函数
const goBack = () => {
  // 只有在错误页面（没有测试题目）时才能返回聊天页面
  router.push('/chat');
};
</script>

<style scoped>
.test-container {
  display: flex;
  justify-content: center;
  align-items: stretch;
  min-height: 100vh;
  padding: 24px;
}

.test-card {
  width: 100%;
  max-width: 980px;
  height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(255, 255, 255, 0.65);
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: rgba(255, 255, 255, 0.9);
  box-shadow:
    var(--shadow-lg),
    inset 0 1px 0 rgba(255, 255, 255, 0.85);
}

.test-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
}

.header-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.header-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.header-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
}

.header-side {
  padding-top: 2px;
}

.loading-wrapper,
.error-wrapper {
  padding: 34px 24px;
  overflow-y: auto;
  flex: 1;
}

.test-content-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.scrollable-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  padding-bottom: 0;
}

.answer-section {
  width: 100%;
}

.question-item {
  margin-bottom: 18px;
  padding: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94) 0%, rgba(245, 244, 241, 0.72) 100%),
    var(--bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  box-shadow: var(--shadow-sm);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.question-item:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
  border-color: var(--border-hover);
}

.choice-question,
.subjective-question {
  width: 100%;
}

.question-title {
  font-weight: 700;
  font-size: 16px;
  color: var(--text-primary);
  margin-bottom: 14px;
  line-height: 1.6;
  white-space: normal;
  word-break: break-word;
  overflow-wrap: break-word;
}

.question-title :deep(strong) {
  font-weight: 700;
  color: var(--primary);
}

/* 主观题题干常为整段加粗，避免 strong 使用主题色导致与上方选择题不一致 */
.subjective-question .question-title,
.subjective-question .question-title :deep(strong) {
  color: var(--text-primary);
}

.question-title :deep(em) {
  font-style: italic;
  color: var(--text-secondary);
}

.question-requirement {
  margin-bottom: 12px;
}

.options-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
  align-items: flex-start;
  width: 100%;
}

.option-item {
  margin: 0;
  padding: 12px 14px;
  background: var(--bg-input);
  border-radius: var(--radius-md);
  transition: all 0.2s;
  border: 1px solid var(--border);
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  cursor: pointer;
  width: 100%;
  text-align: left;
  white-space: normal;
}

.option-item:hover {
  background: var(--primary-subtle);
  border-color: var(--primary);
}

.option-item.is-checked {
  background: rgba(232, 106, 88, 0.08);
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(232, 106, 88, 0.12);
}

/* 覆盖 Element Plus radio 的默认样式 */
.options-group :deep(.el-radio) {
  margin-right: 0;
  width: 100%;
  height: auto;
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  white-space: normal;
}

.options-group :deep(.el-radio__input) {
  flex-shrink: 0;
  margin-right: 12px;
  margin-top: 2px;
}

.options-group :deep(.el-radio__label) {
  flex: 1;
  min-width: 0;
  width: 100%;
  padding-left: 0;
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  white-space: normal;
  line-height: 1.5;
}

/* 多选：与单选保持一致的换行行为 */
.options-group-multi :deep(.el-checkbox) {
  margin-right: 0;
  width: 100%;
  height: auto;
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  white-space: normal;
}

.options-group-multi :deep(.el-checkbox__input) {
  flex-shrink: 0;
  margin-right: 12px;
  margin-top: 2px;
}

.options-group-multi :deep(.el-checkbox__label) {
  flex: 1;
  min-width: 0;
  width: 100%;
  padding-left: 0;
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  white-space: normal;
  line-height: 1.5;
}

.option-key {
  font-weight: 600;
  color: var(--primary);
  margin-right: 8px;
  min-width: 24px;
  flex-shrink: 0;
}

.option-text {
  flex: 1;
  min-width: 0;
  line-height: 1.5;
  color: var(--text-primary);
  text-align: left;
  white-space: normal;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.subjective-question {
  width: 100%;
}

.subjective-input {
  margin-top: 12px;
}

.subjective-input :deep(.el-textarea__inner) {
  border-radius: var(--radius-md);
  border-color: var(--border);
  background: var(--bg-input);
  color: var(--text-primary);
  min-height: 150px !important;
  box-shadow: none;
}

.subjective-input :deep(.el-textarea__inner:focus) {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(232, 106, 88, 0.12);
}

.bottom-spacer {
  height: 92px;
}

.action-buttons-fixed {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 14px 24px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.76) 0%, rgba(255, 255, 255, 0.96) 100%);
  border-top: 1px solid var(--border-light);
  box-shadow: 0 -8px 24px rgba(45, 41, 38, 0.06);
  backdrop-filter: blur(4px);
  flex-shrink: 0;
  z-index: 10;
}

.submit-btn {
  width: 100%;
  font-weight: 600;
  letter-spacing: 0.3px;
  border-radius: var(--radius-md);
  border: none;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  box-shadow: var(--shadow-md);
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-lg), 0 8px 24px var(--primary-glow);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .test-container {
    padding: 12px;
  }

  .test-card {
    height: calc(100vh - 24px);
  }

  .card-header {
    flex-direction: column;
  }

  .header-title {
    font-size: 18px;
  }

  .scrollable-content {
    padding: 14px;
    padding-bottom: 0;
  }

  .action-buttons-fixed {
    padding: 12px 14px;
  }

  .action-buttons-fixed .el-button {
    width: 100%;
  }
}
</style>
