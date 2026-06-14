<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getHollandReportApi } from '@/api/chat'
import { Compass } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const pageLoading = ref(false)

/** 霍兰德六型配色（RIASEC） */
const RIASEC_THEMES = {
  R: { gradient: 'linear-gradient(135deg, #3B82F6, #2563EB)', color: '#2563EB', soft: 'rgba(59, 130, 246, 0.14)' },
  I: { gradient: 'linear-gradient(135deg, #6366F1, #4F46E5)', color: '#4F46E5', soft: 'rgba(99, 102, 241, 0.14)' },
  A: { gradient: 'linear-gradient(135deg, #F59E0B, #D97706)', color: '#D97706', soft: 'rgba(245, 158, 11, 0.14)' },
  S: { gradient: 'linear-gradient(135deg, #10B981, #059669)', color: '#059669', soft: 'rgba(16, 185, 129, 0.14)' },
  E: { gradient: 'linear-gradient(135deg, #E86A58, #C45547)', color: '#C45547', soft: 'rgba(232, 106, 88, 0.14)' },
  C: { gradient: 'linear-gradient(135deg, #64748B, #475569)', color: '#475569', soft: 'rgba(100, 116, 139, 0.14)' }
}

const MATCH_THEMES = [
  { accent: '#059669', soft: 'rgba(16, 185, 129, 0.08)' },
  { accent: '#4F46E5', soft: 'rgba(99, 102, 241, 0.08)' },
  { accent: '#D97706', soft: 'rgba(245, 158, 11, 0.08)' }
]

const userInfo = computed(() => userStore.getUserInfo() || {})
const report = computed(() => userStore.getHollandReport())

const hollandCode = computed(() => report.value?.hollandCode || report.value?.['霍兰德职业兴趣代码'] || '')
const typeCombination = computed(() => report.value?.typeCombination || report.value?.['类型组合'] || '')
const codeChips = computed(() => {
  const raw = hollandCode.value
  if (!raw) return []
  return String(raw).trim().split(/\s+/).filter(Boolean)
})

const dimensions = computed(() => {
  const list = report.value?.dimensionAnalysis || report.value?.['霍兰德六个维度分析']
  return Array.isArray(list) ? list : []
})

const jobMatch = computed(() => report.value?.jobMatchAnalysis || report.value?.['与目标岗位匹配度分析'] || null)

const matchSections = computed(() => {
  const jm = jobMatch.value
  if (!jm) return []
  const items = [
    jm.strengthMatch || jm['优势匹配'],
    jm.potentialAdvantage || jm['潜在优势'],
    jm.areaForAttention || jm['需关注方向']
  ]
  return items.filter(Boolean).map((item) => ({
    title: item.title || item['标题'] || '',
    description: item.description || item['描述'] || ''
  }))
})

function parseRiasecKey(label) {
  if (!label) return 'I'
  const m = String(label).match(/\(([RIASEC])\)/i)
  if (m) return m[1].toUpperCase()
  const first = String(label).trim().charAt(0).toUpperCase()
  return RIASEC_THEMES[first] ? first : 'I'
}

function chipStyle(letter) {
  const key = String(letter).trim().charAt(0).toUpperCase()
  const t = RIASEC_THEMES[key] || RIASEC_THEMES.I
  return {
    background: t.gradient,
    boxShadow: `0 8px 20px ${t.soft}`
  }
}

function dimensionItemStyle(dim) {
  const key = parseRiasecKey(dim.type || dim['类型'])
  const t = RIASEC_THEMES[key] || RIASEC_THEMES.I
  return {
    borderLeftColor: t.color,
    background: `linear-gradient(135deg, ${t.soft} 0%, rgba(255, 255, 255, 0.96) 45%)`
  }
}

function dimensionBarStyle(dim) {
  const key = parseRiasecKey(dim.type || dim['类型'])
  const t = RIASEC_THEMES[key] || RIASEC_THEMES.I
  return { background: t.gradient }
}

function dimensionScoreStyle(dim) {
  const key = parseRiasecKey(dim.type || dim['类型'])
  const t = RIASEC_THEMES[key] || RIASEC_THEMES.I
  return { color: t.color }
}

function matchItemStyle(idx) {
  const t = MATCH_THEMES[idx % MATCH_THEMES.length]
  return {
    borderLeftColor: t.accent,
    background: t.soft
  }
}

function dimensionScoreWidth(score) {
  if (!score) return 0
  const m = String(score).match(/(\d+(?:\.\d+)?)\s*\/\s*(\d+(?:\.\d+)?)/)
  if (!m) return 0
  const num = Number(m[1])
  const den = Number(m[2])
  if (!den) return 0
  return Math.min(100, Math.round((num / den) * 100))
}

async function loadReportIfNeeded() {
  if (report.value) return
  const sessionId = userStore.getSessionId()
  if (!sessionId) return
  pageLoading.value = true
  try {
    const resp = await getHollandReportApi(sessionId)
    if (resp?.code === 1 && resp.data) {
      userStore.setHollandReport(resp.data)
    }
  } catch (e) {
    console.warn('load holland report failed', e)
  } finally {
    pageLoading.value = false
  }
}

const goChat = () => router.push('/chat')

const goContinueConsensus = () => router.push('/chat')

onMounted(async () => {
  if (report.value && !userStore.isTestCompleted('career')) {
    userStore.markTestAsCompleted('career')
  }
  await loadReportIfNeeded()
})
</script>

<template>
  <div class="page">
    <div v-if="pageLoading" class="container loading-wrap">
      <el-skeleton :rows="8" animated />
    </div>

    <div class="container" v-else-if="report">
      <div class="header">
        <div class="logo">
          <span class="logo-icon">
            <el-icon :size="28"><Compass /></el-icon>
          </span>
        </div>
        <h1>霍兰德职业兴趣测试报告</h1>
        <p class="subtitle">基于兴趣测评六个维度的个性化分析</p>
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
            <div class="info-label">霍兰德代码</div>
            <div class="info-value">{{ hollandCode || '-' }}</div>
          </div>
          <div class="info-item">
            <div class="info-label">测试时间</div>
            <div class="info-value">{{ report.testTime || '-' }}</div>
          </div>
        </div>
      </div>

      <div class="card code-card" v-if="codeChips.length">
        <div class="section-title section-title--center">兴趣类型组合</div>
        <div class="chips">
          <span
            v-for="(c, idx) in codeChips"
            :key="c + idx"
            class="chip"
            :style="chipStyle(c)"
          >{{ c }}</span>
        </div>
        <p v-if="typeCombination" class="type-combo">{{ typeCombination }}</p>
      </div>

      <div class="card analysis-section">
        <div class="section-title">六个维度分析</div>
        <div class="analysis-grid" v-if="dimensions.length">
          <div
            v-for="(dim, idx) in dimensions"
            :key="idx"
            class="analysis-item"
            :style="dimensionItemStyle(dim)"
          >
            <div class="analysis-header">
              <div class="analysis-name">{{ dim.type || dim['类型'] || '-' }}</div>
              <div class="analysis-score" :style="dimensionScoreStyle(dim)">
                {{ dim.score || dim['得分'] || '-' }}
              </div>
            </div>
            <div class="analysis-bar">
              <div
                class="analysis-fill"
                :style="{
                  width: dimensionScoreWidth(dim.score || dim['得分']) + '%',
                  ...dimensionBarStyle(dim)
                }"
              />
            </div>
            <div class="analysis-text">{{ dim.description || dim['倾向描述'] || '' }}</div>
          </div>
        </div>
        <div v-else class="empty">暂无维度分析数据</div>
      </div>

      <div class="card match-card" v-if="matchSections.length">
        <div class="section-title">与目标岗位匹配度分析</div>
        <div
          v-for="(sec, idx) in matchSections"
          :key="idx"
          class="match-item"
          :style="matchItemStyle(idx)"
        >
          <div class="match-title">{{ sec.title }}</div>
          <div class="match-text">{{ sec.description }}</div>
        </div>
      </div>

      <div class="actions">
        <button class="btn btn-secondary" @click="goChat">返回聊天</button>
        <button class="btn btn-primary" @click="goContinueConsensus">继续岗位推荐</button>
      </div>
    </div>

    <div class="container" v-else>
      <el-result icon="warning" title="暂无兴趣测试报告" sub-title="请先完成霍兰德职业兴趣测试后再查看结果">
        <template #extra>
          <el-button type="primary" @click="goChat">返回聊天</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>

<style scoped>
.page {
  height: 100vh;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 20px;
}
.container {
  max-width: 1000px;
  margin: 0 auto;
}
.loading-wrap {
  padding: 24px 0;
}
.header {
  text-align: center;
  margin: 10px 0 26px;
}
.logo {
  width: 58px;
  height: 58px;
  margin: 0 auto 12px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.25);
}
.logo-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
h1 {
  font-size: 2rem;
  margin: 0;
  color: var(--text-primary);
  font-weight: 700;
}
.subtitle {
  color: var(--text-secondary);
  margin-top: 8px;
}
.card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.7);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  padding: 20px;
  margin-bottom: 18px;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
}
.info-label {
  color: var(--text-secondary);
  font-weight: 600;
  margin-bottom: 6px;
}
.info-value {
  background: var(--bg-input);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  font-weight: 600;
  color: var(--text-primary);
}
.section-title {
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 12px;
}
.section-title--center {
  text-align: center;
  margin-bottom: 16px;
}
.code-card {
  text-align: center;
  padding: 28px 24px;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  justify-content: center;
  align-items: center;
  margin: 8px 0 16px;
}
.chip {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 1.35rem;
  color: #fff;
  letter-spacing: 0.02em;
}
.type-combo {
  color: var(--text-secondary);
  margin: 0 auto;
  max-width: 560px;
  line-height: 1.65;
  font-size: 1.05rem;
}
.analysis-section {
  margin-bottom: 0;
}
.analysis-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}
.analysis-item {
  border: 1px solid var(--border-light);
  border-left-width: 4px;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  padding: 16px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.analysis-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  gap: 8px;
}
.analysis-name {
  font-weight: 700;
  color: var(--text-primary);
}
.analysis-score {
  font-weight: 700;
  flex-shrink: 0;
}
.analysis-bar {
  height: 10px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 10px;
}
.analysis-fill {
  height: 100%;
  border-radius: 6px;
  transition: width 0.5s ease;
}
.analysis-text {
  color: var(--text-secondary);
  line-height: 1.55;
  font-size: 0.95rem;
}
.match-item {
  padding: 14px 16px;
  margin-bottom: 12px;
  border-radius: var(--radius-md);
  border-left: 4px solid transparent;
}
.match-item:last-child {
  margin-bottom: 0;
}
.match-title {
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}
.match-text {
  color: var(--text-secondary);
  line-height: 1.6;
  font-size: 0.95rem;
}
.empty {
  color: var(--text-secondary);
  padding: 12px 0;
  text-align: center;
}
.actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
  margin: 22px 0;
}
.btn {
  border: none;
  border-radius: var(--radius-md);
  padding: 11px 22px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn:hover {
  transform: translateY(-1px);
}
.btn-primary {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: #fff;
  box-shadow: var(--shadow-md);
}
.btn-secondary {
  background: var(--bg-card);
  color: var(--text-primary);
  border: 1px solid var(--border);
}
</style>
