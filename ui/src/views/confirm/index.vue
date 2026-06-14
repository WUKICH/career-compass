<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { collectConfirmApi, initChatApi } from '@/api/chat'
import { MAX_CORE_SKILLS } from '@/constants/coreSkills'
import { parseSkillNamesFromProfile, splitSkillInput } from '@/utils/skillParse'
import { FULL_VERSION_UNAVAILABLE_MSG, isFullVersionExperienceLabel } from '@/constants/experienceVersion'
import { getFullVersionTemporarilyClosed } from '@/composables/useFullVersionGate'
import { useUserStore } from '@/stores/user'
import { persistWorkflowFromResponse } from '@/utils/workflowPhase'
import { CircleCheckFilled, Tools, Cpu, Check } from '@element-plus/icons-vue'

// 说明：该页面用于“信息确认 -> 提交初始化”，后续可从表单页/对话框把数据路由到这里（query/state/store均可）。
const router = useRouter()
const userStore = useUserStore()

const WORK_YEAR_OPTIONS = [
  { label: '应届 / 在校', value: '0' },
  { label: '工作 1～3 年', value: '1-3' },
  { label: '工作 3～5 年', value: '3-5' },
  { label: '工作 5 年以上', value: '5+' }
]

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

// 优先使用表单页缓存的临时数据；否则回退到 userInfo（兼容从其它入口进入）
const initial = userStore.getPendingConfirmForm() || userStore.getUserInfo() || {}
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
  // 是否编辑模式（用于携带 sessionId）
  isEdit: !!initial.isEdit,
  sessionId: initial.sessionId
})

formData.workYears = toDbWorkYear(formData.workYears)

const submitting = ref(false)
const hasPayload = computed(() => !!userStore.getPendingConfirmForm())
const newSkillName = ref('')
const skillLimitReached = computed(
  () => Array.isArray(formData.skills) && formData.skills.length >= MAX_CORE_SKILLS
)

onMounted(() => {
  // 如果没有临时数据也没有 userInfo，说明直接访问了 confirm
  const info = userStore.getUserInfo()
  if (!hasPayload.value && !info) {
    ElMessage.warning('请先填写信息')
    router.push('/chat')
    return
  }
  // 兜底：有 skill 字符串但未拆成 skills 时（如旧缓存、仅空格分隔）
  if (fromCollect.value && (!formData.skills?.length) && formData.skill?.trim()) {
    const parsed = parseSkillNamesFromProfile(formData.skill)
    if (parsed.length) {
      formData.skills = parsed
    }
  }
})

const validate = () => {
  const isMissing = (v) => {
    const s = String(v ?? '').trim()
    return !s || s === '未明确'
  }
  if (fromCollect.value && !formData.experienceVersion) return '请确认体验版本'
  if (isMissing(formData.name)) return '请完善姓名（不能为空或“未明确”）'
  if (isMissing(formData.workYears)) return '请完善工作年限（不能为空或“未明确”）'
  if (isMissing(formData.targetJob)) return '请完善目标岗位（不能为空或“未明确”）'
  if (isMissing(formData.expectedCity)) return '请完善期望城市（不能为空或“未明确”）'
  if (isMissing(formData.expectedSalary)) return '请完善期望薪资（不能为空或“未明确”）'
  if (fromCollect.value && formData.skills?.length) {
    const rated = formData.skills.some((s) => Number(s.score) > 0)
    if (!rated) return '请至少为一项技能选择熟练度（星级）'
  } else if (isMissing(formData.skill)) {
    return '请完善核心技能（不能为空或“未明确”）'
  }
  return null
}

const normalizeSkillName = (name) => String(name || '').trim()

const addSkill = () => {
  const raw = normalizeSkillName(newSkillName.value)
  if (!raw) {
    ElMessage.warning('请输入技能名称')
    return
  }
  if (!Array.isArray(formData.skills)) {
    formData.skills = []
  }
  const names = splitSkillInput(raw)
  let added = 0
  for (const name of names) {
    if (formData.skills.length >= MAX_CORE_SKILLS) {
      ElMessage.warning(`最多添加 ${MAX_CORE_SKILLS} 项技能`)
      break
    }
    const key = name.toLowerCase()
    const duplicated = formData.skills.some(
      (s) => normalizeSkillName(s?.name).toLowerCase() === key
    )
    if (duplicated) continue
    formData.skills.push({ name, score: 0 })
    added++
  }
  if (added === 0 && names.length) {
    ElMessage.warning('这些技能已存在')
  }
  newSkillName.value = ''
}

const removeSkill = (index) => {
  if (!Array.isArray(formData.skills) || index < 0 || index >= formData.skills.length) return
  formData.skills.splice(index, 1)
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
          entryViaResume: !!formData.resumeFileName,
          resumeFileName: formData.resumeFileName || undefined,
          sessionId,
          token
        })
        localStorage.setItem('loginUser', JSON.stringify({ token, userId: sessionId, username: formData.name }))
        if (resp.data?.content) {
          userStore.setInitialMessage(resp.data.content)
        }
        persistWorkflowFromResponse(sessionId, resp.data)
        userStore.clearPendingConfirmForm()
        ElMessage.success('信息已确认，开始职业探索')
        router.push('/chat')
        return
      }
      ElMessage.error(resp?.msg || resp?.data?.msg || '提交失败')
      return
    }

    const req = {
      name: formData.name,
      workYears: formData.workYears || undefined,
      targetJob: formData.targetJob,
      expectedCity: formData.expectedCity || undefined,
      expectedSalary: formData.expectedSalary || undefined,
      skill: formData.skill,
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
        entryViaResume: !!formData.resumeFileName,
        sessionId,
        token
      })
      if (token) {
        localStorage.setItem('loginUser', JSON.stringify({ token, userId: sessionId, username: formData.name }))
      }
      if (resp.data?.content) {
        userStore.setInitialMessage(resp.data.content)
      }
      persistWorkflowFromResponse(sessionId, resp.data)
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
          <el-icon :size="28"><CircleCheckFilled /></el-icon>
        </div>
        <h1>信息确认</h1>
        <p class="subtitle">请核对并可直接修改信息，确认无误后再开始后续流程。</p>
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
              <span v-if="fromCollect" class="skill-hint">（最多 {{ MAX_CORE_SKILLS }} 项{{ formData.skills?.length ? '，至少为一项打分' : '' }}）</span>
            </div>
            <div class="skills-container" v-if="fromCollect && formData.skills && formData.skills.length">
              <div class="skill-card skill-card-rate" v-for="(s, idx) in formData.skills" :key="'c-' + s.name + idx">
                <span class="skill-check">
                  <el-icon :size="16"><Check /></el-icon>
                </span>
                <span class="skill-name">{{ s.name }}</span>
                <el-rate
                  v-model="s.score"
                  :max="3"
                  allow-half
                  clearable
                  void-color="#d1d5db"
                  class="skill-el-rate"
                />
                <button
                  type="button"
                  class="skill-action-btn skill-delete-btn"
                  @click="removeSkill(idx)"
                  :disabled="submitting"
                >
                  删除
                </button>
              </div>
            </div>
            <div class="skill-editor" v-if="fromCollect">
              <el-input
                v-model="newSkillName"
                placeholder="新增一个技能，如：Spring Cloud"
                @keydown.enter.prevent="addSkill"
              />
              <button
                type="button"
                class="skill-action-btn skill-add-btn"
                @click="addSkill"
                :disabled="submitting || skillLimitReached"
              >
                新增技能
              </button>
            </div>
            <div class="skills-container" v-else-if="!fromCollect && formData.skills && formData.skills.length">
              <div class="skill-card" v-for="(s, idx) in formData.skills" :key="s.name + idx">
                <span class="skill-check">
                  <el-icon :size="16"><Check /></el-icon>
                </span>
                <span class="skill-name">{{ s.name }}</span>
                <div class="skill-rating">
                  <span 
                    v-for="star in 5" 
                    :key="star" 
                    class="star"
                    :class="{ filled: star <= (s.score || 0) }"
                  >
                    ★
                  </span>
                </div>
                <span class="skill-score">({{ s.score || 0 }}/5)</span>
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

          <!-- <div class="section">
            <div class="section-title">简历信息</div>
            <div class="value block" v-if="formData.resumeFileName">{{ formData.resumeFileName }}</div>
            <div class="value block pre" v-else>{{ formData.resume || '-' }}</div>
          </div> -->
        </div>

        <div class="footer">
          <button class="btn btn-secondary" @click="back" :disabled="submitting">返回修改</button>
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
.skill-check{display:flex;align-items:center;color:var(--primary);flex-shrink:0;}
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

.skill-name{font-weight:700;color:var(--text-primary);flex:1;}
.skill-rating{display:flex;gap:2px;flex-shrink:0;}
.star{color:#d1d5db;font-size:1.1rem;line-height:1;}
.star.filled{color:#fbbf24;}
.skill-score{color:var(--primary-dark);font-weight:800;font-size:0.95rem;flex-shrink:0;}
.skill-hint{color:var(--text-secondary);font-weight:600;font-size:0.88rem;margin-left:6px;}
.skill-card-rate{flex-wrap:wrap;}
.skill-el-rate{flex-shrink:0;}
.skill-editor{display:flex;gap:10px;align-items:center;margin-top:10px;}
.skill-action-btn{
  border:none;
  border-radius:999px;
  padding:9px 14px;
  font-weight:700;
  font-size:.86rem;
  cursor:pointer;
  transition:all .2s ease;
  box-shadow:0 8px 20px rgba(17,24,39,.08);
  white-space:nowrap;
}
.skill-action-btn:hover{transform:translateY(-1px);}
.skill-action-btn:disabled{opacity:.55;cursor:not-allowed;transform:none;box-shadow:none;}
.skill-add-btn{
  color:#fff;
  background:linear-gradient(135deg,#6d5efc,#4f46e5);
}
.skill-add-btn:hover{
  box-shadow:0 10px 24px rgba(79,70,229,.32);
}
.skill-delete-btn{
  color:#7f1d1d;
  background:linear-gradient(135deg,#ffe3e3,#ffd2d2);
  border:1px solid #ffc2c2;
}
.skill-delete-btn:hover{
  box-shadow:0 10px 24px rgba(220,38,38,.18);
}
.pre{white-space:pre-wrap;line-height:1.5;}
.footer{display:flex;justify-content:flex-end;gap:12px;padding:16px 22px;background:rgba(245,244,241,.75);border-top:1px solid var(--border-light);flex-wrap:wrap;}
.btn{
  border:none;
  border-radius:999px;
  padding:12px 22px;
  min-width:120px;
  font-weight:800;
  letter-spacing:.2px;
  cursor:pointer;
  transition:all .22s ease;
  box-shadow:0 10px 24px rgba(17,24,39,.08);
}
.btn:hover{transform:translateY(-1px);}
.btn-primary{
  background:linear-gradient(135deg,#f28c73,#d96a57);
  color:#fff;
}
.btn-primary:hover{
  box-shadow:0 12px 28px rgba(217,106,87,.35);
}
.btn-secondary{
  background:linear-gradient(135deg,#ffffff,#f7f6f3);
  color:var(--text-primary);
  border:1px solid var(--border);
}
.btn-secondary:hover{
  box-shadow:0 12px 28px rgba(107,114,128,.18);
}
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

/* 体验版本仅展示不可改：避免 disabled 默认发灰难读 */
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



