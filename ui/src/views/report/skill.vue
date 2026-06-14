<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getWorkflowStatusApi } from '@/api/chat'
import { persistWorkflowFromResponse } from '@/utils/workflowPhase'
import { DataLine } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const navigating = ref(false)

const CAREER_HANDOFF_MESSAGE = '职业兴趣测评即将开始，请做好准备！'

const userInfo = computed(() => userStore.getUserInfo() || {})
const report = computed(() => userStore.getSkillReport())

const totalScore = computed(() => report.value?.totalScore ?? report.value?.['测试综合得分'])
const totalQuestions = computed(() => report.value?.totalQuestions ?? report.value?.['题目总数'])
const correctCount = computed(() => report.value?.correctCount ?? report.value?.['答对题数'])
const accuracy = computed(() => report.value?.accuracy ?? report.value?.['正确率'])
const summary = computed(() => report.value?.summary ?? report.value?.['总结'])
const analysis = computed(() => report.value?.skillAnalysis ?? report.value?.['核心技能分析'] ?? [])

/** 回聊天前确保职业兴趣题已写入 pending，避免 sync 竞态导致按钮不出现 */
async function ensurePendingCareerTest() {
  const sessionId = userStore.getSessionId()
  if (!sessionId) return
  if (!userStore.isTestCompleted('skill')) {
    userStore.markTestAsCompleted('skill')
  }
  const existing = userStore.getPendingCareerTest()
  if (existing?.careerTestQuestions) return

  for (let attempt = 0; attempt < 3; attempt += 1) {
    try {
      const response = await getWorkflowStatusApi(sessionId)
      if (response?.code === 1 && response.data) {
        persistWorkflowFromResponse(sessionId, response.data)
        if (response.data.careerTestQuestions) {
          userStore.setPendingCareerTest({
            testType: 'career',
            content: response.data.content || CAREER_HANDOFF_MESSAGE,
            careerTestQuestions: response.data.careerTestQuestions,
            phase: response.data.phase || null
          })
          return
        }
        const phase = response.data.phase
        if (phase === 'CAREER_ACTIVE' || phase === 'SKILL_DONE') {
          await new Promise((r) => setTimeout(r, 1200))
          continue
        }
      }
      break
    } catch (e) {
      console.warn('ensure pending career test failed', e)
      break
    }
  }
}

async function goChat() {
  if (navigating.value) return
  navigating.value = true
  try {
    await ensurePendingCareerTest()
    router.push('/chat')
  } finally {
    navigating.value = false
  }
}

async function goInterestTest() {
  if (navigating.value) return
  navigating.value = true
  try {
    await ensurePendingCareerTest()
    if (!userStore.getPendingCareerTest()?.careerTestQuestions) {
      ElMessage.warning('职业兴趣题目尚未就绪，返回聊天后将自动刷新入口')
    }
    router.push('/chat')
  } finally {
    navigating.value = false
  }
}

onMounted(() => {
  if (report.value && !userStore.isTestCompleted('skill')) {
    userStore.markTestAsCompleted('skill')
  }
})
</script>

<template>
  <div class="page">
    <div class="container" v-if="report">
      <div class="header">
        <div class="logo">
          <span class="logo-icon">
            <el-icon :size="28"><DataLine /></el-icon>
          </span>
        </div>
        <h1>技能测试结果分析</h1>
        <p class="subtitle">基于您的核心技能和目标岗位生成的测试结果</p>
      </div>

      <div class="card test-info">
        <div class="info-grid">
          <div class="info-item">
            <div class="info-label">测试者</div>
            <div class="info-value">{{ userInfo.name || report.name || '-' }}</div>
          </div>
          <div class="info-item">
            <div class="info-label">目标岗位</div>
            <div class="info-value">{{ report.targetJob || userInfo.targetJob || '-' }}</div>
          </div>
          <div class="info-item">
            <div class="info-label">核心技能</div>
            <div class="info-value">{{ report.skill || userInfo.skill || '-' }}</div>
          </div>
          <div class="info-item">
            <div class="info-label">测试时间</div>
            <div class="info-value">{{ report.testTime || '-' }}</div>
          </div>
        </div>
      </div>

      <div class="card score-card">
        <div class="score-title">技能测试综合得分</div>
        <div class="score-value">{{ totalScore ?? '-' }}</div>
        <div class="score-total">满分100分</div>
        <div class="score-message">{{ summary || '—' }}</div>
        <div class="score-details">
          <div class="detail">
            <div class="detail-value">{{ totalQuestions ?? '-' }}</div>
            <div class="detail-label">题目总数</div>
          </div>
          <div class="detail">
            <div class="detail-value">{{ correctCount ?? '-' }}</div>
            <div class="detail-label">答对题数</div>
          </div>
          <div class="detail">
            <div class="detail-value">{{ accuracy != null ? accuracy + '%' : '-' }}</div>
            <div class="detail-label">正确率</div>
          </div>
        </div>
      </div>

      <div class="analysis-section">
        <div class="section-title">核心技能分析</div>
        <div class="analysis-grid">
          <div v-for="(item, idx) in analysis" :key="idx" class="analysis-item">
            <div class="analysis-header">
              <div class="analysis-name">{{ item.skillName || item.skill || item['核心技能'] || '-' }}</div>
              <div class="analysis-score">{{ item.score || item['评分'] || '-' }}/100</div>
            </div>
            <div class="analysis-bar">
              <div class="analysis-fill" :style="{ width: Math.min(100, Number(item.score || item['评分'] || 0)) + '%' }"></div>
            </div>
            <div class="analysis-text">{{ item.evaluation || item['评价'] || '' }}</div>
          </div>
        </div>
      </div>

      <div class="actions">
        <button class="btn btn-secondary" @click="goChat">返回聊天</button>
        <button class="btn btn-primary" @click="goInterestTest">进入兴趣测试</button>
      </div>
    </div>

    <div class="container" v-else>
      <el-result icon="warning" title="暂无技能测试报告" sub-title="请先完成技能测试后再查看结果">
        <template #extra>
          <el-button type="primary" @click="goChat">返回聊天</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<style scoped>
.page{height:100vh;overflow-y:auto;-webkit-overflow-scrolling:touch;padding:20px;}
.container{max-width:1000px;margin:0 auto;}
.header{text-align:center;margin:10px 0 26px;}
.logo{width:58px;height:58px;margin:0 auto 12px;border-radius:50%;background:linear-gradient(135deg,var(--primary),var(--primary-dark));display:flex;align-items:center;justify-content:center;box-shadow:var(--shadow-md);}
.logo-icon{display:flex;align-items:center;justify-content:center;color:#fff;}
h1{font-size:2rem;margin:0;color:var(--text-primary);font-weight:700;}
.subtitle{color:var(--text-secondary);margin-top:8px;}
.card{background:rgba(255,255,255,.92);border:1px solid rgba(255,255,255,.7);border-radius:var(--radius-lg);box-shadow:var(--shadow-md);padding:20px;margin-bottom:18px;}
.info-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:14px;}
.info-label{color:var(--text-secondary);font-weight:600;margin-bottom:6px;}
.info-value{background:var(--bg-input);border:1px solid var(--border);border-radius:var(--radius-md);padding:10px 12px;font-weight:600;color:var(--text-primary);}
.score-card{text-align:center;background:linear-gradient(145deg,var(--primary-dark),var(--primary));color:#fff;}
.score-title{opacity:.92;}
.score-value{font-size:3.2rem;font-weight:800;line-height:1;margin:10px 0;}
.score-total{opacity:.85;}
.score-message{margin-top:14px;font-size:1.04rem;font-weight:600;}
.score-details{display:flex;justify-content:center;gap:22px;flex-wrap:wrap;margin-top:18px;}
.detail{text-align:center;min-width:120px;}
.detail-value{font-size:1.5rem;font-weight:800;}
.detail-label{opacity:.9;}
.analysis-section{margin-top:4px;}
.section-title{font-size:1.15rem;font-weight:700;color:var(--text-primary);margin:0 0 12px;}
.analysis-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(280px,1fr));gap:14px;}
.analysis-item{background:rgba(255,255,255,.9);border:1px solid var(--border-light);border-radius:var(--radius-md);box-shadow:var(--shadow-sm);padding:16px;}
.analysis-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;}
.analysis-name{font-weight:700;color:var(--text-primary);}
.analysis-score{font-weight:700;color:var(--primary);}
.analysis-bar{height:8px;background:var(--border-light);border-radius:6px;overflow:hidden;margin-bottom:10px;}
.analysis-fill{height:100%;background:linear-gradient(90deg,var(--primary),var(--accent-gold));border-radius:6px;}
.analysis-text{color:var(--text-secondary);line-height:1.5;font-size:.95rem;}
.actions{display:flex;justify-content:center;gap:12px;flex-wrap:wrap;margin:22px 0;}
.btn{border:none;border-radius:var(--radius-md);padding:11px 22px;font-weight:700;cursor:pointer;transition:all .2s ease;}
.btn:hover{transform:translateY(-1px);}
.btn-primary{background:linear-gradient(135deg,var(--primary),var(--primary-dark));color:#fff;box-shadow:var(--shadow-md);}
.btn-secondary{background:var(--bg-card);color:var(--text-primary);border:1px solid var(--border);}
</style>



