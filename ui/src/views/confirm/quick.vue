<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { collectConfirmApi, initChatApi } from '@/api/chat'
import { MAX_CORE_SKILLS } from '@/constants/coreSkills'
import { FULL_VERSION_UNAVAILABLE_MSG, isFullVersionExperienceLabel } from '@/constants/experienceVersion'
import { getFullVersionTemporarilyClosed } from '@/composables/useFullVersionGate'
import { useUserStore } from '@/stores/user'
import { Lightning, Tools, Cpu, Check } from '@element-plus/icons-vue'

/**
 * 快速测试：与 /confirm 相同交互，但字段预填且可直接打开，无需先走聊天/表单。
 * 默认走 initChat（非 collect）；若 store 里已有 collect 待确认数据则仍可按 collect 提交。
 */
const router = useRouter()
const userStore = useUserStore()

const WORK_YEAR_OPTIONS = [
  { label: '应届 / 在校', value: '0' },
  { label: '工作 1～3 年', value: '1-3' },
  { label: '工作 3～5 年', value: '3-5' },
  { label: '工作 5 年以上', value: '5+' }
]

const QUICK_DEFAULTS = {
  experienceVersion: '体验版',
  name: '快速测试用户',
  workYears: '1-3',
  targetJob: 'Java 后端开发',
  expectedCity: '上海',
  expectedSalary: '15k-20k',
  /** 与 UserFormDialog 一致：每项 { name, score }，星级 1～5 */
  skills: [
    { name: 'Spring Boot', score: 3 },
    { name: 'MySQL', score: 3 },
    { name: 'Redis', score: 2 }
  ],
  skill: '',
  resume: '',
  resumeFileName: '',
  isEdit: false,
  sessionId: undefined,
  fromCollect: false
}

function toDbWorkYear(v) {
  if (v == null || v === '') return ''
  const s = String(v).trim()
  if (s === '其他') return ''
  const fromNlp = { 应届: '0', '1-3年': '1-3', '3-5年': '3-5', '5年以上': '5+' }
  if (fromNlp[s] !== undefined) return fromNlp[s]
  if (['0', '1-3', '3-5', '5+'].includes(s)) return s
  if (/^[0-4]$/.test(s)) {
    return { '0': '0', '1': '1-3', '2': '3-5', '3': '5+' }[s] || ''
  }
  return ''
}

const info = userStore.getUserInfo()
const pending = userStore.getPendingConfirmForm()
const initial = { ...QUICK_DEFAULTS, ...(info || {}), ...(pending || {}) }

const fromCollect = computed(() => !!initial.fromCollect)

const formData = reactive({
  experienceVersion: initial.experienceVersion || '',
  name: initial.name || '',
  workYears: initial.workYears || '',
  targetJob: initial.targetJob || '',
  expectedCity: initial.expectedCity || '',
  expectedSalary: initial.expectedSalary || '',
  skill: initial.skill || '',
  skills: Array.isArray(initial.skills) ? initial.skills.slice(0, MAX_CORE_SKILLS) : [],
  resume: initial.resume || '',
  resumeFileName: initial.resumeFileName || '',
  isEdit: !!initial.isEdit,
  sessionId: initial.sessionId
})

formData.workYears = toDbWorkYear(formData.workYears)

/** 有 skills 列表时拼成后端 / 会话使用的 skill 字符串（与 UserFormDialog 一致） */
function skillLineFromForm() {
  if (formData.skills?.length) {
    return formData.skills.map((s) => `${s.name}:${s.score}`).join(', ')
  }
  return (formData.skill || '').trim()
}

const submitting = ref(false)

const validate = () => {
  if (fromCollect.value && !formData.experienceVersion) return '请确认体验版本'
  if (!formData.name) return '请填写姓名'
  if (!formData.targetJob) return '请填写目标岗位'
  if (formData.skills?.length) {
    const rated = formData.skills.some((s) => Number(s.score) > 0)
    if (!rated) return '请至少为一项技能选择熟练度（星级）'
  } else if (!String(formData.skill || '').trim()) {
    return '请填写核心技能'
  }
  return null
}

const submit = async () => {
  const err = validate()
  if (err) {
    ElMessage.warning(err)
    return
  }
  if (await getFullVersionTemporarilyClosed() && isFullVersionExperienceLabel(formData.experienceVersion)) {
    ElMessage.warning(FULL_VERSION_UNAVAILABLE_MSG)
    return
  }
  submitting.value = true
  try {
    if (fromCollect.value) {
      const skillNamesLine =
        formData.skills?.length > 0
          ? formData.skills.map((s) => s.name).join('，')
          : formData.skill
      const answers = {
        experienceVersion: formData.experienceVersion,
        name: formData.name,
        workYears: formData.workYears,
        targetJob: formData.targetJob,
        expectedCity: formData.expectedCity,
        expectedSalary: formData.expectedSalary,
        skill: skillNamesLine
      }
      if (formData.skills?.length) {
        answers.skillRatings = JSON.stringify(
          formData.skills.map((s) => ({
            name: s.name,
            score: Number(s.score) || 0
          }))
        )
      }
      if (formData.resume) {
        answers.resume = formData.resume
      }
      const resp = await collectConfirmApi({
        sessionId: formData.sessionId,
        answers
      })
      if (resp?.code === 1 && resp?.data?.sessionId && resp?.data?.token) {
        const sessionId = resp.data.sessionId
        const token = resp.data.token
        userStore.clearCompletedTests()
        userStore.clearChatMessages()
        userStore.setUserInfo({
          experienceVersion: formData.experienceVersion,
          name: formData.name,
          workYears: formData.workYears,
          targetJob: formData.targetJob,
          expectedCity: formData.expectedCity,
          expectedSalary: formData.expectedSalary,
          skill: formData.skills?.length
            ? formData.skills.map((s) => `${s.name}:${s.score}`).join('，')
            : formData.skill,
          skills: formData.skills?.length ? formData.skills : undefined,
          sessionId,
          token
        })
        localStorage.setItem('loginUser', JSON.stringify({ token, userId: sessionId, username: formData.name }))
        if (resp.data?.content) {
          userStore.setInitialMessage(resp.data.content)
        }
        userStore.clearPendingConfirmForm()
        ElMessage.success('信息已确认，开始职业探索')
        router.push('/chat')
        return
      }
      ElMessage.error(resp?.msg || resp?.data?.msg || '提交失败')
      return
    }

    const skillLine = skillLineFromForm()
    const req = {
      name: formData.name,
      workYears: formData.workYears || undefined,
      targetJob: formData.targetJob,
      expectedCity: formData.expectedCity || undefined,
      expectedSalary: formData.expectedSalary || undefined,
      skill: skillLine,
      skills: formData.skills?.length ? formData.skills : undefined,
      resume: formData.resume || undefined,
      resumeFileName: formData.resumeFileName || undefined
    }
    if (formData.isEdit && formData.sessionId) {
      req.sessionId = formData.sessionId
    }

    const resp = await initChatApi(req)
    if (resp?.code === 1) {
      const sessionId = resp.data?.sessionId
      const token = resp.data?.token
      userStore.clearCompletedTests()
      userStore.clearChatMessages()
      userStore.setUserInfo({
        ...formData,
        skill: skillLine,
        skills: formData.skills?.length ? formData.skills : undefined,
        sessionId,
        token
      })
      if (token) {
        localStorage.setItem('loginUser', JSON.stringify({ token, userId: sessionId, username: formData.name }))
      }
      if (resp.data?.content) {
        userStore.setInitialMessage(resp.data.content)
      }
      userStore.clearPendingConfirmForm()
      ElMessage.success('提交成功，已进入聊天')
      router.push('/chat')
    } else {
      ElMessage.error(resp?.msg || '提交失败')
    }
  } finally {
    submitting.value = false
  }
}

const back = () => {
  router.push('/chat')
}
</script>

<template>
  <div class="page">
    <div class="container">
      <div class="header">
        <div class="logo">
          <el-icon :size="28"><Lightning /></el-icon>
        </div>
        <h1>信息确认（快速测试）</h1>
        <p class="subtitle">
          本页为开发联调用：字段已预填，可直接点「确认提交」走初始化；也可改完再提交。
        </p>
      </div>

      <div class="card">
        <div class="card-header">信息确认与提交</div>
        <div class="content">
          <div class="section">
            <div class="section-title">基本信息</div>
            <div class="grid">
              <div class="item" v-if="fromCollect">
                <div class="label">体验版本</div>
                <el-input
                  v-model="formData.experienceVersion"
                  placeholder="如：体验版 / 完全版"
                  disabled
                  class="readonly-version-input"
                />
              </div>
              <div class="item">
                <div class="label">姓名</div>
                <el-input v-model="formData.name" placeholder="请输入姓名/昵称" />
              </div>
              <div class="item">
                <div class="label">工作年限</div>
                <el-select
                  v-model="formData.workYears"
                  placeholder="请选择"
                  clearable
                  :filterable="false"
                  style="width: 100%"
                >
                  <el-option v-for="o in WORK_YEAR_OPTIONS" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
              </div>
            </div>
          </div>

          <div class="section">
            <div class="section-title">职业意向</div>
            <div class="grid">
              <div class="item">
                <div class="label">目标岗位</div>
                <el-input v-model="formData.targetJob" placeholder="请输入目标岗位" />
              </div>
              <div class="item">
                <div class="label">期望城市</div>
                <el-input v-model="formData.expectedCity" placeholder="请输入期望城市" />
              </div>
              <div class="item">
                <div class="label">期望薪资</div>
                <el-input v-model="formData.expectedSalary" placeholder="例如 10k" />
              </div>
            </div>
          </div>

          <div class="section">
            <div class="section-title">
              <span class="section-icon">
                <el-icon :size="18"><Tools /></el-icon>
              </span>
              核心技能
            </div>
            <div class="skill-subtitle">
              <span class="subtitle-icon">
                <el-icon :size="16"><Cpu /></el-icon>
              </span>
              技能与熟练度
              <span v-if="formData.skills?.length" class="skill-hint">（至少为一项技能打分）</span>
            </div>
            <div class="skills-container" v-if="formData.skills && formData.skills.length">
              <div class="skill-card skill-card-rate" v-for="(s, idx) in formData.skills" :key="'sk-' + s.name + idx">
                <span class="skill-check">
                  <el-icon :size="16"><Check /></el-icon>
                </span>
                <span class="skill-name">{{ s.name }}</span>
                <el-rate
                  v-model="s.score"
                  :max="fromCollect ? 3 : 5"
                  allow-half
                  clearable
                  void-color="#d1d5db"
                  class="skill-el-rate"
                />
              </div>
            </div>
            <el-input
              v-else
              v-model="formData.skill"
              type="textarea"
              :rows="4"
              placeholder="请输入你的核心技能或亮点"
            />
          </div>

          <div class="section" v-if="fromCollect && formData.resume">
            <div class="section-title">简历 / 经历摘要</div>
            <el-input v-model="formData.resume" type="textarea" :rows="4" placeholder="可在此微调" />
          </div>
        </div>

        <div class="footer">
          <button class="btn btn-secondary" @click="back" :disabled="submitting">返回聊天</button>
          <button class="btn btn-primary" @click="submit" :disabled="submitting">
            {{ submitting ? '提交中...' : '确认提交' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page{height:100vh;overflow-y:auto;-webkit-overflow-scrolling:touch;padding:20px;}
.container{max-width:980px;margin:0 auto;}
.header{text-align:center;margin:10px 0 24px;}
.logo{width:58px;height:58px;margin:0 auto 12px;border-radius:50%;background:linear-gradient(135deg,var(--primary),var(--primary-dark));display:flex;align-items:center;justify-content:center;box-shadow:var(--shadow-md);color:#fff;}
.section-icon,.subtitle-icon{display:flex;align-items:center;color:var(--primary);}
h1{font-size:2rem;margin:0;color:var(--text-primary);font-weight:800;}
.subtitle{color:var(--text-secondary);margin-top:8px;}
.card{background:rgba(255,255,255,.92);border:1px solid rgba(255,255,255,.7);border-radius:var(--radius-lg);box-shadow:var(--shadow-lg);overflow:hidden;}
.card-header{background:linear-gradient(135deg,var(--primary),var(--primary-dark));color:#fff;padding:16px 22px;font-weight:800;letter-spacing:.3px;}
.content{padding:22px;}
.section{border-bottom:1px solid var(--border-light);padding-bottom:16px;margin-bottom:16px;}
.section:last-child{border-bottom:none;margin-bottom:0;}
.section-title{color:var(--text-primary);font-weight:800;margin-bottom:10px;display:flex;align-items:center;gap:8px;font-size:1.05rem;}
.section-icon{font-size:1.2rem;}
.skill-subtitle{color:var(--text-secondary);font-weight:600;margin-bottom:12px;display:flex;align-items:center;gap:6px;font-size:.95rem;}
.subtitle-icon{color:var(--primary);font-weight:700;}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:14px;}
.label{color:var(--text-secondary);font-weight:700;margin-bottom:6px;}
.value{background:var(--bg-input);border:1px solid var(--border);border-radius:var(--radius-md);padding:10px 12px;font-weight:600;color:var(--text-primary);}
.block{width:100%;}
.skills-container{display:flex;flex-direction:column;gap:10px;}
.skill-card{background:var(--bg-input);border-radius:var(--radius-md);padding:13px 14px;display:flex;align-items:center;gap:12px;border:1px solid var(--border);}
.skill-check{display:flex;align-items:center;color:var(--primary);flex-shrink:0;}
.skill-name{font-weight:700;color:var(--text-primary);flex:1;}
.skill-rating{display:flex;gap:2px;flex-shrink:0;}
.star{color:#d1d5db;font-size:1.1rem;line-height:1;}
.star.filled{color:#fbbf24;}
.skill-score{color:var(--primary-dark);font-weight:800;font-size:0.95rem;flex-shrink:0;}
.skill-hint{color:var(--text-secondary);font-weight:600;font-size:0.88rem;margin-left:6px;}
.skill-card-rate{flex-wrap:wrap;}
.skill-el-rate{flex-shrink:0;}
.pre{white-space:pre-wrap;line-height:1.5;}
.footer{display:flex;justify-content:flex-end;gap:12px;padding:16px 22px;background:rgba(245,244,241,.75);border-top:1px solid var(--border-light);flex-wrap:wrap;}
.btn{border:none;border-radius:var(--radius-md);padding:12px 24px;font-weight:800;cursor:pointer;transition:all .2s ease;}
.btn:hover{transform:translateY(-1px);}
.btn-primary{background:linear-gradient(135deg,var(--primary),var(--primary-dark));color:#fff;box-shadow:var(--shadow-md);}
.btn-secondary{background:var(--bg-card);color:var(--text-primary);border:1px solid var(--border);}
.btn:disabled{opacity:.6;cursor:not-allowed;transform:none;}

:deep(.el-input__wrapper),
:deep(.el-textarea__inner){
  background: var(--bg-input);
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  box-shadow: none;
}

:deep(.el-input__wrapper.is-focus){
  box-shadow: 0 0 0 2px rgba(232,106,88,.15) !important;
}

:deep(.el-textarea__inner:focus){
  border-color: var(--primary);
  box-shadow: 0 0 0 2px rgba(232,106,88,.15);
}

:deep(.readonly-version-input.is-disabled .el-input__wrapper) {
  background: var(--bg-panel);
  cursor: default;
}
:deep(.readonly-version-input.is-disabled .el-input__inner) {
  color: var(--text-primary);
  -webkit-text-fill-color: var(--text-primary);
  cursor: default;
}
</style>
