<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/stores/user';
import { submitFeedbackApi, getPartialReportApi } from '@/api/chat';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const report = computed(() => userStore.getCareerAssessmentReport());

const info = computed(() => ({
  name: report.value?.name || '-',
  targetJob: report.value?.targetJob || '-',
  targetCity: report.value?.targetCity || '-',
  expectedSalary: report.value?.expectedSalary != null ? report.value.expectedSalary : '-'
}));

const skills = computed(() => report.value?.skillAssessments || []);
const interest = computed(() => report.value?.careerInterestResult || {});
const resumeOpt = computed(() => report.value?.resumeOptimization || null);
const recommended = computed(() => report.value?.recommendedPositions || []);

const getJobField = (job, camelKey, cnKey) => {
  if (!job) return null;
  if (typeof job === 'string') return job;
  return job[camelKey] ?? job[cnKey] ?? null;
};

const goChat = () => router.push('/chat');
const goFeedback = () => router.push({ path: '/report/career', query: { page: 'feedback' } });
const goReport = () => router.push({ path: '/report/career', query: {} });

const page = computed(() => (route.query.page === 'feedback' ? 'feedback' : 'report'));

/** profile | resume | skill | career | recommendation | full — 控制阶段性报告可见模块 */
const reportScope = computed(() => {
  const s = route.query.scope;
  const valid = ['profile', 'resume', 'skill', 'career', 'recommendation', 'full'];
  if (valid.includes(s)) return s;
  return 'full';
});

const showInfoCard = computed(() => true);
const showSkills = computed(() =>
  ['skill', 'career', 'recommendation', 'full'].includes(reportScope.value)
);
const showHolland = computed(() =>
  ['career', 'recommendation', 'full'].includes(reportScope.value)
);
const showResumeBlock = computed(() => {
  if (reportScope.value === 'resume') return true;
  return reportScope.value === 'full' && !!resumeOpt.value;
});
const showJobs = computed(() =>
  ['recommendation', 'full'].includes(reportScope.value)
);

const reportTitle = computed(() => {
  const map = {
    profile: '个人信息报告',
    resume: '简历优化建议',
    skill: '技能测评报告',
    career: '职业人格测评',
    recommendation: '岗位推荐报告',
    full: '职业综合评估报告'
  };
  return map[reportScope.value] || map.full;
});

const reportSubtitle = computed(() => {
  const map = {
    profile: '基于您已确认填写的个人信息',
    resume: '基于您上传的简历生成的阶段性优化建议',
    skill: '基于技能测评结果的阶段性分析',
    career: '结合技能测评与霍兰德兴趣的阶段性分析',
    recommendation: '基于测评结果的初步岗位推荐',
    full: '基于技能、兴趣与目标岗位生成的个性化分析'
  };
  return map[reportScope.value] || map.full;
});

const reportReady = computed(() => {
  if (!report.value) return false;
  if (['profile', 'skill', 'career', 'recommendation'].includes(reportScope.value)) {
    return true;
  }
  if (reportScope.value === 'resume') {
    return true;
  }
  return true;
});

const pageLoading = ref(false);

// 计算实测评分的进度条宽度（支持数字或字符串格式）
const getPracticeScoreWidth = (practiceScore) => {
  if (!practiceScore || practiceScore === '-') return 0;
  // 如果是数字，直接使用
  if (typeof practiceScore === 'number') {
    return Math.min(100, practiceScore);
  }
  // 如果是字符串，尝试提取数字
  const match = String(practiceScore).match(/(\d+(?:\.\d+)?)/);
  if (match) {
    const num = parseFloat(match[1]);
    return Math.min(100, num);
  }
  return 0;
};

// 反馈问卷（4题）
const questions = ref([
  {
    text: '本次评估给出的职位推荐，和你之前设想的职业方向相比，是否有新的启发？',
    options: ['完全没有启发', '几乎没有启发', '一般', '有一定启发', '启发非常大']
  },
  {
    text: '参与本次评估后，你觉得对自己擅长的技能领域是否有了更具体的认知？',
    options: ['完全没有', '几乎没有', '一般', '比较具体', '非常具体']
  },
  {
    text: '本次评估是否让你对自身职业发展的可能性有了更全面的了解？',
    options: ['完全没有', '几乎没有', '一般', '比较全面', '非常全面']
  },
  {
    text: '若身边朋友有职业规划的困惑，你是否会建议他尝试本次评估？',
    options: ['绝对不会', '不太会', '一般', '比较会', '非常会']
  }
]);

const answers = ref([null, null, null, null]); // index
const isSubmitting = ref(false);

const canSubmit = computed(() => answers.value.every(v => v !== null));

const selectAnswer = (qIdx, optIdx) => {
  answers.value[qIdx] = optIdx;
};

const submitFeedback = async () => {
  if (!canSubmit.value) {
    ElMessage.warning('请完成所有问题后再提交');
    return;
  }

  const sessionId = userStore.getSessionId();
  if (!sessionId) {
    ElMessage.error('会话ID不存在，请重新初始化');
    router.push('/chat');
    return;
  }

  try {
    isSubmitting.value = true;
    // 组装所有答案为一个列表，每个item包括题号和选项内容
    const feedbackList = questions.value.map((q, index) => {
      const answerIndex = answers.value[index];
      return {
        questionNumber: index + 1,
        answer: q.options[answerIndex]
      };
    });

    // 一次性提交所有答案
    const feedbackData = {
      feedbackList: feedbackList
    };
    
    const resp = await submitFeedbackApi(sessionId, feedbackData);
    if (!(resp && resp.code === 1)) {
      throw new Error(resp?.msg || '提交失败，请重试');
    }

    ElMessage.success('反馈提交成功！感谢您的参与！');
    setTimeout(() => {
      router.push('/chat');
    }, 800);
  } catch (e) {
    console.error('提交反馈失败:', e);
    ElMessage.error(e.message || '提交失败，请重试');
  } finally {
    isSubmitting.value = false;
  }
};

watch(page, (p) => {
  // 进入反馈页时滚动到顶部
  if (p === 'feedback') {
    requestAnimationFrame(() => window.scrollTo(0, 0));
  }
});

const loadScopedReport = async () => {
  if (page.value === 'feedback') return;
  const scope = reportScope.value;
  const sid = userStore.getSessionId();
  if (!sid) {
    if (report.value && (scope === 'profile' || scope === 'resume')) {
      return;
    }
    ElMessage.warning('请先填写至少一项信息');
    router.push('/chat');
    return;
  }
  pageLoading.value = true;
  try {
    const resp = await getPartialReportApi(sid, scope);
    if (resp?.code === 1 && resp.data) {
      userStore.setCareerAssessmentReport(resp.data);
    } else if (!report.value) {
      throw new Error(resp?.msg || '报告加载失败');
    } else {
      ElMessage.warning(resp?.msg || '部分报告数据加载失败，展示已有内容');
    }
  } catch (e) {
    console.error('load scoped report failed', e);
    if (!report.value) {
      ElMessage.warning(e?.message || '暂无报告数据，请先完成对应测评');
      router.push('/chat');
    } else {
      ElMessage.warning(e?.message || '部分报告数据加载失败，展示已有内容');
    }
  } finally {
    pageLoading.value = false;
  }
};

onMounted(() => {
  if (page.value === 'feedback') return;
  if (!report.value || reportScope.value !== 'full') {
    loadScopedReport();
    return;
  }
  if (!reportReady.value) {
    loadScopedReport();
  }
});
</script>

<template>
  <div class="page">
    <!-- 第1页：报告结果 -->
    <div class="container" v-if="page === 'report' && pageLoading" v-loading="true" style="min-height: 240px"></div>

    <div class="container" v-else-if="page === 'report' && report && reportReady">
      <header class="hero" aria-labelledby="report-title">
        <div class="hero-icon" aria-hidden="true">
          <svg class="hero-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
            <path d="M8 21h8M12 17v4M7 4h10l1 3v2a5 5 0 0 1-10 0V7l1-3Z"/>
            <path d="M6 7H4a2 2 0 0 0 2 2h0M18 7h2a2 2 0 0 1-2 2h0"/>
          </svg>
        </div>
        <div class="hero-text">
          <p class="hero-kicker">{{ reportScope === 'full' ? '测评完成' : '阶段性报告' }}</p>
          <h1 id="report-title">{{ reportTitle }}</h1>
          <p class="subtitle">{{ reportSubtitle }}</p>
        </div>
      </header>

      <div class="card info-card" v-if="showInfoCard">
        <div class="section-title">个人信息与评估结果</div>
        <div class="info-grid">
          <div class="info-item">
            <div class="label">姓名</div>
            <div class="value">{{ info.name }}</div>
          </div>
          <div class="info-item">
            <div class="label">目标岗位</div>
            <div class="value">{{ info.targetJob }}</div>
          </div>
          <div class="info-item">
            <div class="label">期望城市</div>
            <div class="value">{{ info.targetCity }}</div>
          </div>
          <div class="info-item">
            <div class="label">期望薪资</div>
            <div class="value">{{ info.expectedSalary }} /月</div>
          </div>
        </div>
      </div>

      <div class="card" v-if="showSkills">
        <div class="section-title with-icon">
          <span>核心技能评估</span>
          <span class="pill">共 {{ skills.length }} 项</span>
        </div>
        <div class="skill-comparison" v-if="skills.length">
          <div class="skill-column">
            <div class="column-header">初始评分</div>
            <div v-for="(s, idx) in skills" :key="'initial-' + idx" class="skill-item">
              <div class="skill-row">
                <div class="skill-name">{{ s.skill || '-' }}</div>
                <div class="skill-score">{{ s.score ?? '-' }}分</div>
              </div>
              <div class="skill-bar">
                <div class="skill-bar-fill initial" :style="{ width: Math.min(100, Number(s.score || 0) * 20) + '%' }"></div>
              </div>
            </div>
          </div>
          <div class="skill-column">
            <div class="column-header">实测评分</div>
            <div v-for="(s, idx) in skills" :key="'practice-' + idx" class="skill-item">
              <div class="skill-row">
                <div class="skill-name">{{ s.skill || '-' }}</div>
                <div class="skill-score">{{ s.practiceScore ?? '-' }}</div>
              </div>
              <div class="skill-bar">
                <div class="skill-bar-fill practice" :style="{ width: getPracticeScoreWidth(s.practiceScore*20) + '%' }"></div>
              </div>
            </div>
          </div>
        </div>
        <div class="empty" v-else>暂无技能数据</div>
      </div>

      <div class="card holland-card" v-if="showHolland">
        <div class="section-title with-icon">
          <span>职业兴趣测试结果</span>
        </div>
        <div class="holland-inner" v-if="interest.codes && interest.codes.length">
          <div class="chips" role="list" aria-label="霍兰德代码">
            <span class="chip" v-for="(c, idx) in interest.codes" :key="c + idx" role="listitem">{{ c }}</span>
          </div>
          <p class="desc holland-desc" v-if="interest.description">{{ interest.description }}</p>
        </div>
        <div class="empty" v-else>暂无兴趣描述</div>
      </div>

      <div class="card resume-card-outer" v-if="showResumeBlock">
        <div class="section-title with-icon">
          <span>简历优化建议</span>
          <span class="pill" v-if="resumeOpt.items && resumeOpt.items.length">共 {{ resumeOpt.items.length }} 条</span>
        </div>

        <div class="resumeopt-summary" v-if="resumeOpt.summary">
          <div class="resumeopt-summary-title">总结</div>
          <div class="resumeopt-summary-text">{{ resumeOpt.summary }}</div>
        </div>

        <div class="resumeopt-list" v-if="resumeOpt.items && resumeOpt.items.length">
          <div class="resumeopt-item" v-for="(it, idx) in resumeOpt.items" :key="idx">
            <div class="resumeopt-item-head">
              <div class="resumeopt-index">{{ idx + 1 }}</div>
              <div class="resumeopt-head-text">
                <div class="resumeopt-label">简历表述</div>
                <div class="resumeopt-value">{{ it.resumeWording || '-' }}</div>
              </div>
            </div>

            <div class="resumeopt-grid">
              <div class="resumeopt-block" v-if="it.assessmentBasis">
                <div class="resumeopt-block-title">
                  <span class="badge badge-amber">测评依据</span>
                </div>
                <div class="resumeopt-block-text">{{ it.assessmentBasis }}</div>
              </div>

              <div class="resumeopt-block" v-if="it.suggestion">
                <div class="resumeopt-block-title">
                  <span class="badge badge-indigo">优化建议</span>
                </div>
                <div class="resumeopt-block-text">{{ it.suggestion }}</div>
              </div>
            </div>
          </div>
        </div>
        <div class="empty" v-else>暂无可对照的优化建议</div>
      </div>

      <div class="card jobs-card" v-if="showJobs">
        <div class="section-title with-icon">
          <span>推荐职位</span>
        </div>
        <div class="recommend-list" v-if="recommended.length">
          <div class="rec-card" v-for="(job, idx) in recommended" :key="idx">
            <div class="rec-header">
              <div class="circle">{{ idx + 1 }}</div>
              <div class="rec-title-group">
                <div class="job-title">{{ getJobField(job, 'title', '岗位名称') || '-' }}</div>
                <div class="comp-name" v-if="getJobField(job, 'compName', '公司名称')">{{ getJobField(job, 'compName', '公司名称') }}</div>
              </div>
            </div>
            <div class="rec-details" v-if="getJobField(job, 'compName', '公司名称') || getJobField(job, 'salary', '职位薪资') || getJobField(job, 'dq', '工作地点') || getJobField(job, 'compIndustry', '公司行业')">
              <div class="detail-row" v-if="getJobField(job, 'salary', '职位薪资')">
                <span class="detail-key">薪资</span>
                <span class="detail-value">{{ getJobField(job, 'salary', '职位薪资') }}</span>
              </div>
              <div class="detail-row" v-if="getJobField(job, 'dq', '工作地点')">
                <span class="detail-key">地点</span>
                <span class="detail-value">{{ getJobField(job, 'dq', '工作地点') }}</span>
              </div>
              <div class="detail-row" v-if="getJobField(job, 'compIndustry', '公司行业') && reportScope === 'recommendation' && !getJobField(job, 'compName', '公司名称')">
                <span class="detail-key">匹配理由</span>
                <span class="detail-value">{{ getJobField(job, 'compIndustry', '公司行业') }}</span>
              </div>
              <div class="detail-row" v-else-if="getJobField(job, 'compIndustry', '公司行业')">
                <span class="detail-key">行业</span>
                <span class="detail-value">{{ getJobField(job, 'compIndustry', '公司行业') }}</span>
              </div>
              <div class="detail-row" v-if="getJobField(job, 'compScale', '公司规模')">
                <span class="detail-key">规模</span>
                <span class="detail-value">{{ getJobField(job, 'compScale', '公司规模') }}</span>
              </div>
              <div class="detail-row" v-if="getJobField(job, 'compStage', '公司阶段')">
                <span class="detail-key">阶段</span>
                <span class="detail-value">{{ getJobField(job, 'compStage', '公司阶段') }}</span>
              </div>
              <div class="detail-row" v-if="getJobField(job, 'recruiterName', '联系人')">
                <span class="detail-key">联系</span>
                <span class="detail-value">{{ getJobField(job, 'recruiterName', '联系人') }}</span>
              </div>
            </div>
            <div class="rec-labels" v-if="getJobField(job, 'labels', '职位福利') && getJobField(job, 'labels', '职位福利').length">
              <span class="label-tag" v-for="(label, lIdx) in getJobField(job, 'labels', '职位福利')" :key="lIdx">{{ label }}</span>
            </div>
          </div>
        </div>
        <div class="empty" v-else>暂无推荐职位</div>
      </div>

      <div class="actions">
        <button class="btn btn-ghost" @click="goChat">返回聊天</button>
        <button v-if="reportScope === 'full'" class="btn" @click="goFeedback">下一页</button>
      </div>
    </div>

    <div class="container" v-else-if="page === 'report' && report && !reportReady && !pageLoading">
      <el-result icon="info" title="暂无该阶段报告内容" sub-title="请完成对应测评后再查看，或返回聊天继续流程">
        <template #extra>
          <el-button type="primary" @click="goChat">返回聊天</el-button>
        </template>
      </el-result>
    </div>

    <!-- 第2页：反馈问卷 -->
    <div class="container" v-else-if="page === 'feedback'">
      <header class="hero hero-compact" aria-labelledby="feedback-title">
        <div class="hero-icon hero-icon-alt" aria-hidden="true">
          <svg class="hero-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8"/>
          </svg>
        </div>
        <div class="hero-text">
          <p class="hero-kicker">最后一步</p>
          <h1 id="feedback-title">评估反馈问卷</h1>
          <p class="subtitle">请花几分钟时间完成以下问卷，帮助我们改进评估系统</p>
        </div>
      </header>

      <div class="card">
        <div class="question-section">
          <div v-for="(q, qIdx) in questions" :key="qIdx" class="question-item">
            <div class="question-title">{{ qIdx + 1 }}. {{ q.text }}</div>
            <div class="options-group">
              <div
                v-for="(opt, optIdx) in q.options"
                :key="optIdx"
                :class="['option-item', { 'is-checked': answers[qIdx] === optIdx }]"
                @click="selectAnswer(qIdx, optIdx)"
              >
                <span class="option-text">{{ opt }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="actions actions-fixed">
        <button class="btn btn-ghost" @click="goReport" :disabled="isSubmitting">上一页</button>
        <button class="btn" @click="submitFeedback" :disabled="!canSubmit || isSubmitting">
          {{ isSubmitting ? '提交中...' : '提交' }}
        </button>
      </div>
      <div class="bottom-spacer"></div>
    </div>

    <div class="container" v-else>
      <el-result icon="warning" title="暂无职业评估报告" sub-title="请先完成技能/职业测试生成报告">
        <template #extra>
          <el-button type="primary" @click="goChat">返回聊天</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<style scoped>
/* Design tokens — 与报告页主色（靛紫 + 青绿）一致 */
.page{
  --report-bg: #eef2ff;
  --report-bg2: #f8fafc;
  --report-surface: #ffffff;
  --report-text: #111827;
  --report-muted: #6b7280;
  --report-border: #e5e7eb;
  --report-primary: #4f46e5;
  --report-primary-soft: rgba(99, 102, 241, 0.12);
  --report-accent: #0d9488;
  --report-radius-lg: 16px;
  --report-radius-md: 12px;
  --report-shadow: 0 1px 2px rgba(17, 24, 39, 0.04), 0 12px 32px rgba(17, 24, 39, 0.08);
  --report-shadow-hover: 0 16px 40px rgba(17, 24, 39, 0.1);

  /* 占满 layout 里 el-main 的内容高度，在本页内纵向滚动并显示滚动条。
     （全局 html/body 为 overflow:hidden，窗口本身不出现滚动条。） */
  height: 100%;
  min-height: 0;
  box-sizing: border-box;
  overflow-y: auto;
  overflow-x: hidden;
  -webkit-overflow-scrolling: touch;
  background: linear-gradient(165deg, var(--report-bg) 0%, var(--report-bg2) 45%, #f1f5f9 100%);
  padding: 20px 16px 32px;
  touch-action: manipulation;
}

.container{ max-width: 1040px; margin: 0 auto; }

/* Hero */
.hero{
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 22px;
  padding: 20px 22px;
  background: var(--report-surface);
  border-radius: var(--report-radius-lg);
  border: 1px solid var(--report-border);
  box-shadow: var(--report-shadow);
}
.hero-compact{ margin-bottom: 16px; padding: 16px 20px; }
.hero-icon{
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 10px 28px rgba(99, 102, 241, 0.35);
}
.hero-icon-alt{
  background: linear-gradient(135deg, #0d9488, #6366f1);
  box-shadow: 0 10px 28px rgba(13, 148, 136, 0.28);
}
.hero-svg{ width: 28px; height: 28px; }
.hero-text{ min-width: 0; }
.hero-kicker{
  margin: 0 0 6px;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--report-primary);
}
h1{
  margin: 0;
  font-size: clamp(1.25rem, 2.5vw, 1.5rem);
  font-weight: 900;
  color: var(--report-text);
  letter-spacing: -0.02em;
  line-height: 1.25;
}
.subtitle{
  margin: 8px 0 0;
  font-size: 15px;
  line-height: 1.55;
  color: var(--report-muted);
  max-width: 52ch;
}

.card{
  background: var(--report-surface);
  border-radius: var(--report-radius-lg);
  box-shadow: var(--report-shadow);
  padding: 20px 22px;
  margin-bottom: 16px;
  border: 1px solid rgba(229, 231, 235, 0.9);
  transition: box-shadow 0.2s ease;
}
.card:hover{ box-shadow: var(--report-shadow-hover); }

.info-card{
  border-color: rgba(99, 102, 241, 0.15);
  background: linear-gradient(180deg, #ffffff 0%, #fafbff 100%);
}

.section-title{
  font-weight: 900;
  color: var(--report-text);
  margin-bottom: 14px;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  letter-spacing: -0.01em;
}
.with-icon .pill{
  margin-left: auto;
  background: var(--report-primary-soft);
  color: #4338ca;
  padding: 6px 12px;
  border-radius: 999px;
  font-weight: 800;
  font-size: 12px;
  border: 1px solid rgba(99, 102, 241, 0.2);
}

.info-grid{
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}
.info-item{
  background: #fff;
  border-radius: var(--report-radius-md);
  border: 1px solid var(--report-border);
  padding: 14px 16px;
  transition: border-color 0.2s ease, transform 0.2s ease;
}
.info-item:hover{
  border-color: rgba(99, 102, 241, 0.35);
  transform: translateY(-1px);
}
.label{ color: var(--report-muted); font-weight: 700; font-size: 13px; margin-bottom: 8px; }
.value{ font-weight: 900; color: var(--report-text); font-size: 16px; }

/* Skills */
.skill-comparison{
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.skill-column{
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  border-radius: var(--report-radius-md);
  background: #f8fafc;
  border: 1px solid var(--report-border);
}
.skill-column:nth-child(2){
  background: linear-gradient(180deg, #f0fdfa 0%, #f8fafc 100%);
  border-color: rgba(13, 148, 136, 0.2);
}
.column-header{
  font-weight: 900;
  font-size: 13px;
  padding: 10px 12px;
  border-radius: 10px;
  text-align: center;
  letter-spacing: 0.02em;
}
.skill-column:nth-child(1) .column-header{
  color: #4338ca;
  background: rgba(99, 102, 241, 0.1);
}
.skill-column:nth-child(2) .column-header{
  color: #0f766e;
  background: rgba(13, 148, 136, 0.12);
}
.skill-item{
  background: #fff;
  border: 1px solid var(--report-border);
  border-radius: var(--report-radius-md);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.skill-row{ display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.skill-name{ font-weight: 800; color: var(--report-text); flex: 1; min-width: 0; font-size: 15px; }
.skill-column:nth-child(1) .skill-score{ color: var(--report-primary); }
.skill-column:nth-child(2) .skill-score{ color: var(--report-accent); }
.skill-score{ font-weight: 900; font-size: 15px; flex-shrink: 0; }
.skill-bar{ height: 10px; background: #e5e7eb; border-radius: 999px; overflow: hidden; margin-top: 2px; }
.skill-bar-fill{ height: 100%; border-radius: 999px; transition: width 0.35s ease-out; }
.skill-bar-fill.initial{ background: linear-gradient(90deg, #6366f1, #8b5cf6); }
.skill-bar-fill.practice{ background: linear-gradient(90deg, #14b8a6, #0ea5e9); }

/* Holland */
.holland-card .holland-inner{
  padding: 8px 4px 4px;
}
.chips{
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
}
.chip{
  width: 68px;
  height: 68px;
  border-radius: 50%;
  border: 4px solid #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.45rem;
  font-weight: 900;
  color: #fff;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}
.chips .chip:nth-child(1){ background: linear-gradient(135deg, #2563eb, #3b82f6); }
.chips .chip:nth-child(2){ background: linear-gradient(135deg, #7c3aed, #a855f7); }
.chips .chip:nth-child(3){ background: linear-gradient(135deg, #e11d48, #f97316); }
.chips .chip:nth-child(4){ background: linear-gradient(135deg, #0d9488, #0ea5e9); }
.chips .chip:nth-child(5){ background: linear-gradient(135deg, #d97706, #ef4444); }
.holland-desc{
  margin: 18px auto 0;
  max-width: 42rem;
  text-align: center;
  color: #374151;
  line-height: 1.65;
  font-size: 15px;
}

/* Jobs */
.recommend-list{ display: flex; flex-direction: column; gap: 14px; }
.rec-card{
  background: linear-gradient(180deg, #fafbff 0%, #f8fafc 100%);
  border: 1px solid var(--report-border);
  border-radius: var(--report-radius-md);
  padding: 18px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}
.rec-card:hover{
  border-color: rgba(99, 102, 241, 0.25);
  box-shadow: 0 8px 24px rgba(17, 24, 39, 0.06);
}
.rec-header{ display: flex; align-items: flex-start; gap: 14px; margin-bottom: 12px; }
.circle{
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  flex-shrink: 0;
  font-size: 15px;
}
.rec-title-group{ flex: 1; min-width: 0; }
.job-title{ font-weight: 900; color: var(--report-text); font-size: 17px; margin-bottom: 4px; line-height: 1.3; }
.comp-name{ color: var(--report-muted); font-size: 14px; font-weight: 600; }
.rec-details{
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--report-border);
}
.detail-row{
  display: grid;
  grid-template-columns: 52px 1fr;
  align-items: start;
  gap: 10px;
}
.detail-key{
  font-size: 12px;
  font-weight: 800;
  color: var(--report-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  padding-top: 2px;
}
.detail-value{ color: #374151; font-size: 14px; font-weight: 600; line-height: 1.5; }
.rec-labels{
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--report-border);
}
.label-tag{
  background: #e0e7ff;
  color: #3730a3;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  border: 1px solid rgba(99, 102, 241, 0.2);
}
.empty{ color: #9ca3af; font-size: 14px; padding: 8px 0; }

.resume-card-outer{
  border-color: rgba(99, 102, 241, 0.2);
  background: linear-gradient(180deg, #ffffff 0%, #fafbff 100%);
}

/* Resume optimization card */
.resumeopt-summary{
  background: linear-gradient(135deg, rgba(99,102,241,.08), rgba(139,92,246,.08));
  border: 1px solid rgba(99,102,241,.18);
  border-radius: 12px;
  padding: 14px;
  margin-top: 8px;
}
.resumeopt-summary-title{
  font-weight: 900;
  color:#111827;
  font-size: 13px;
  letter-spacing: .2px;
  margin-bottom: 6px;
}
.resumeopt-summary-text{
  color:#374151;
  line-height:1.65;
  white-space:pre-wrap;
}
.resumeopt-list{display:flex;flex-direction:column;gap:12px;margin-top:12px;}
.resumeopt-item{
  background:#fff;
  border:1px solid #e5e7eb;
  border-radius:14px;
  padding:14px;
  box-shadow:0 10px 24px rgba(17,24,39,.06);
}
.resumeopt-item-head{display:flex;gap:12px;align-items:flex-start;}
.resumeopt-index{
  width:32px;height:32px;border-radius:10px;
  background:linear-gradient(135deg,#6366f1,#8b5cf6);
  color:#fff;
  font-weight:900;
  display:flex;align-items:center;justify-content:center;
  flex-shrink:0;
}
.resumeopt-head-text{flex:1;min-width:0;}
.resumeopt-label{color:#6b7280;font-weight:700;font-size:12px;margin-bottom:6px;}
.resumeopt-value{color:#111827;font-weight:900;line-height:1.4;word-break:break-word;}
.resumeopt-grid{
  margin-top:12px;
  display:grid;
  grid-template-columns:1fr;
  gap:10px;
}
.resumeopt-block{
  background:#f9fafb;
  border:1px solid #e5e7eb;
  border-radius:12px;
  padding:12px;
}
.resumeopt-block-title{display:flex;align-items:center;gap:8px;margin-bottom:8px;}
.badge{
  display:inline-flex;align-items:center;
  padding:4px 10px;border-radius:999px;
  font-size:12px;font-weight:800;
  border:1px solid transparent;
}
.badge-amber{background:rgba(245,158,11,.12);color:#92400e;border-color:rgba(245,158,11,.25);}
.badge-indigo{background:rgba(99,102,241,.12);color:#3730a3;border-color:rgba(99,102,241,.25);}
.resumeopt-block-text{color:#374151;line-height:1.65;white-space:pre-wrap;word-break:break-word;}

@media (min-width: 860px){
  .resumeopt-grid{grid-template-columns:1fr 1fr;}
}
.actions{
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
  gap: 12px;
  flex-wrap: wrap;
}
.actions-fixed{
  position: sticky;
  bottom: 0;
  z-index: 2;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  padding: 14px 0;
  margin-top: 8px;
  border-top: 1px solid var(--report-border);
  box-shadow: 0 -8px 24px rgba(17, 24, 39, 0.06);
}
.btn{
  border: none;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
  padding: 12px 22px;
  border-radius: 12px;
  font-weight: 800;
  font-size: 15px;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(79, 70, 229, 0.35);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.btn:focus-visible{
  outline: 2px solid var(--report-primary);
  outline-offset: 2px;
}
.btn-ghost{
  background: var(--report-surface);
  color: var(--report-primary);
  border: 2px solid var(--report-border);
  box-shadow: none;
}
.btn:hover:not(:disabled){
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.4);
}
.btn-ghost:hover:not(:disabled){
  border-color: rgba(99, 102, 241, 0.45);
  background: var(--report-primary-soft);
}
.btn:disabled{ opacity: 0.55; cursor: not-allowed; transform: none; box-shadow: none; }

.question-section{ width: 100%; }
.question-item{
  margin-bottom: 16px;
  padding: 18px 20px;
  background: linear-gradient(180deg, #fafbff 0%, #f8fafc 100%);
  border-radius: var(--report-radius-md);
  border: 1px solid var(--report-border);
  border-left: 4px solid var(--report-primary);
  box-shadow: 0 4px 16px rgba(17, 24, 39, 0.04);
}
.question-title{
  font-weight: 900;
  font-size: 15px;
  color: var(--report-text);
  margin-bottom: 14px;
  line-height: 1.55;
}
.options-group{
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 8px;
  align-items: stretch;
  width: 100%;
}
.option-item{
  padding: 14px 16px;
  background: #fff;
  border-radius: 12px;
  transition: border-color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
  border: 2px solid var(--report-border);
  display: flex;
  align-items: center;
  justify-content: flex-start;
  cursor: pointer;
  width: 100%;
}
.option-item:hover{
  background: var(--report-primary-soft);
  border-color: rgba(99, 102, 241, 0.45);
}
.option-item.is-checked{
  background: linear-gradient(90deg, rgba(99, 102, 241, 0.12), rgba(14, 165, 233, 0.08));
  border-color: var(--report-primary);
  font-weight: 800;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.12);
}
.option-text{ flex: 1; line-height: 1.5; color: var(--report-text); }
.bottom-spacer{ height: 80px; }

@media (max-width: 720px){
  .skill-comparison{ grid-template-columns: 1fr; }
}

@media (prefers-reduced-motion: reduce){
  .card,
  .info-item,
  .rec-card,
  .btn,
  .skill-bar-fill,
  .option-item{
    transition: none;
  }
  .btn:hover:not(:disabled),
  .info-item:hover{
    transform: none;
  }
}
</style>

