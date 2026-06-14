<template>
  <el-dialog
    v-model="visible"
  width="880px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="!isRequired"
    :before-close="handleBeforeClose"
  class="user-form-dialog"
>
  <template #title>
    <div class="dialog-title">
      <span class="dialog-title__icon">
        <el-icon :size="22"><Folder /></el-icon>
      </span>
      <div>
        <div class="dialog-title__main">个人信息与职业意向</div>
        <div class="dialog-title__sub">完善信息，获得更贴合的职业分析</div>
      </div>
    </div>
  </template>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
    label-width="0"
    class="form-body"
  >
    <div class="card-grid">
      <div class="card">
        <div class="card-header">
          <span class="card-icon">
            <el-icon :size="18"><User /></el-icon>
          </span>
          <span class="card-title">基础信息</span>
        </div>
        <div class="grid two">
          <el-form-item prop="name" class="field">
            <div class="field-label"><span class="dot" />姓名</div>
        <el-input
          v-model="formData.name"
          placeholder="请输入您的姓名"
          clearable
              prefix-icon="User"
        />
      </el-form-item>
          <el-form-item prop="workYears" class="field">
            <div class="field-label"><span class="dot" />工作年限</div>
            <el-select
              v-model="formData.workYears"
              placeholder="请选择"
              clearable
              :filterable="false"
            >
              <el-option label="应届/在校" value="0" />
              <el-option label="工作1-3年" value="1-3" />
              <el-option label="工作3-5年" value="3-5" />
              <el-option label="工作5年以上" value="5+" />
            </el-select>
      </el-form-item>
        </div>
        <div class="grid two">
          <el-form-item prop="targetJob" class="field">
            <div class="field-label"><span class="dot" />目标岗位</div>
        <el-input
          v-model="formData.targetJob"
              placeholder="例如：数据分析师 / Java工程师"
          clearable
              prefix-icon="Briefcase"
        />
      </el-form-item>
          <el-form-item prop="expectedCity" class="field">
            <div class="field-label"><span class="dot" />期望城市</div>
            <el-select
          v-model="formData.expectedCity"
              placeholder="请选择"
              filterable
          clearable
            >
              <el-option label="北京" value="北京" />
              <el-option label="上海" value="上海" />
              <el-option label="广州" value="广州" />
              <el-option label="深圳" value="深圳" />
              <el-option label="杭州" value="杭州" />
              <el-option label="南京" value="南京" />
              <el-option label="其它" value="其它" />
            </el-select>
      </el-form-item>
        </div>

        <div class="grid two">
          <el-form-item prop="expectedSalary" class="field">
            <div class="field-label"><span class="dot" />期望薪资（元/月）</div>
            <el-input-number
          v-model="formData.expectedSalary"
              :min="1000"
              :max="1000000"
              :step="500"
              style="width: 100%"
              controls-position="right"
        />
      </el-form-item>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <span class="card-icon">
            <el-icon :size="18"><Star /></el-icon>
          </span>
          <span class="card-title">核心技能</span>
          <span class="card-sub">最多添加 {{ MAX_CORE_SKILLS }} 项，并为每项打分</span>
        </div>
        <div class="skill-input">
        <el-input
            v-model="formData.skillInput"
            placeholder="如：Python / 数据分析 / PPT"
          clearable
            class="skill-name"
        />
          <el-rate
            v-model="formData.skillRating"
            :max="5"
            :low-threshold="2"
            :high-threshold="4"
            show-score
          />
          <el-button
            type="primary"
            :disabled="skillLimitReached || !formData.skillInput || !formData.skillRating"
            @click="addSkill"
          >
            添加技能
          </el-button>
        </div>
        <div class="skill-list" v-if="formData.skills.length">
          <div
            v-for="(item, idx) in formData.skills"
            :key="item.name + idx"
            class="skill-chip"
          >
            <div class="skill-name">{{ item.name }}</div>
            <el-rate v-model="item.score" disabled :max="5" />
            <el-button type="text" @click="removeSkill(idx)" class="remove-btn">移除</el-button>
          </div>
        </div>
        <el-alert
          v-else
          type="info"
          :closable="false"
          title="请添加至少一项技能"
          class="empty-tip"
        />
      </div>

      <!-- <div class="card">
        <div class="card-header">
          <span class="card-icon">
            <el-icon :size="18"><Wallet /></el-icon>
          </span>
          <span class="card-title">期望薪资</span>
        </div>
        <div class="grid">
          <el-form-item prop="resume" class="field">
            <div class="field-label"><span class="dot" />简历</div>
            <el-upload
              class="upload"
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleResumeChange"
              :on-remove="handleResumeRemove"
              accept=".pdf,.doc,.docx,.txt"
            >
              <el-button type="primary" plain>上传文件</el-button>
              <span class="upload-tip" v-if="formData.resumeFileName">{{ formData.resumeFileName }}</span>
              <span class="upload-tip" v-else>支持 pdf / doc / docx / txt</span>
            </el-upload>
      </el-form-item>
        </div>
      </div> -->
    </div>
    </el-form>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button
          v-if="!isRequired"
          @click="handleCancel"
        >
          取消
        </el-button>
        <el-button
          type="primary"
          @click="handleSubmit"
          :loading="isSubmitting"
        >
          {{ isSubmitting ? '提交中...' : (isEditMode ? '更新信息' : '确认并开始对话') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, nextTick } from 'vue';
import { ElMessage } from 'element-plus';
import { Folder, User, Star, Wallet } from '@element-plus/icons-vue';
import { MAX_CORE_SKILLS } from '@/constants/coreSkills';
import { useUserStore } from '@/stores/user';
import { useRouter } from 'vue-router';

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  isRequired: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update:modelValue', 'success']);

const router = useRouter();
const userStore = useUserStore();
const formRef = ref(null);
const isSubmitting = ref(false);

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
});

const isEditMode = computed(() => {
  return !!userStore.getSessionId();
});

const formData = reactive({
  name: '',
  workYears: '',
  targetJob: '',
  expectedCity: '',
  expectedSalary: 3000,
  skills: [],
  skillInput: '',
  skillRating: 0,
  resume: '',
  resumeFileName: ''
});

const rules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  workYears: [
    { required: true, message: '请选择工作年限', trigger: 'change' }
  ],
  targetJob: [
    { required: true, message: '请输入目标岗位', trigger: 'blur' }
  ],
  expectedCity: [
    { required: true, message: '请选择期望城市', trigger: 'change' }
  ],
  expectedSalary: [
    { required: true, message: '请输入期望薪资', trigger: 'change' }
  ],
  resume: [
    { required: false }
  ]
};

/** 与后端 WorkYearsCodec 一致：表单 value 为 0、1-3、3-5、5+；兼容旧 0–4、中文与已弃用「其他」 */
function normalizeWorkYearsForForm(v) {
  if (v == null || v === '') return '';
  const s = String(v).trim();
  if (s === '其他') return '';
  const fromNlp = {
    应届: '0',
    '1-3年': '1-3',
    '3-5年': '3-5',
    '5年以上': '5+'
  };
  if (fromNlp[s] !== undefined) return fromNlp[s];
  const canonical = { '0': '0', '1-3': '1-3', '3-5': '3-5', '5+': '5+' };
  if (canonical[s] !== undefined) return canonical[s];
  if (/^[0-4]$/.test(s)) {
    const map = { '0': '0', '1': '1-3', '2': '3-5', '3': '5+' };
    return map[s] || '';
  }
  return '';
}

// 加载已有用户信息（优先从 pendingConfirmForm 加载，用于从确认页返回时回显）
const loadUserInfo = () => {
  // 优先从 pendingConfirmForm 加载（从确认页返回时使用）
  const pendingForm = userStore.getPendingConfirmForm();
  if (pendingForm) {
    formData.name = pendingForm.name || '';
    formData.workYears = normalizeWorkYearsForForm(pendingForm.workYears);
    formData.targetJob = pendingForm.targetJob || '';
    formData.expectedCity = pendingForm.expectedCity || '';
    formData.expectedSalary = Number(pendingForm.expectedSalary) || 20000;
    // 优先使用 skills 数组
    if (pendingForm.skills && Array.isArray(pendingForm.skills)) {
      formData.skills = pendingForm.skills.map(s => ({ ...s }));
    } else if (pendingForm.skill) {
      // 兼容字符串格式
      formData.skills = pendingForm.skill.split(',').map(k => {
        const match = k.trim().match(/^(.+?):(\d+)$/);
        if (match) {
          return { name: match[1], score: parseInt(match[2]) || 4 };
        }
        return { name: k.trim(), score: 4 };
      }).filter(s => s.name);
    }
    formData.resume = pendingForm.resume || '';
    formData.resumeFileName = pendingForm.resumeFileName || '';
    return;
  }
  
  // 如果没有 pendingForm，则从 userInfo 加载（编辑模式）
  const userInfo = userStore.getUserInfo();
  if (userInfo) {
    formData.name = userInfo.name || '';
    formData.workYears = normalizeWorkYearsForForm(userInfo.workYears);
    formData.targetJob = userInfo.targetJob || '';
    formData.expectedCity = userInfo.expectedCity || '';
    formData.expectedSalary = Number(userInfo.expectedSalary) || 20000;
    // 尝试从旧的字符串技能里恢复
    if (userInfo.skills && Array.isArray(userInfo.skills)) {
      formData.skills = userInfo.skills.map(s => ({ ...s }));
    } else if (userInfo.skill) {
      formData.skills = userInfo.skill.split(',').map(k => {
        const match = k.trim().match(/^(.+?):(\d+)$/);
        if (match) {
          return { name: match[1], score: parseInt(match[2]) || 4 };
        }
        return { name: k.trim(), score: 4 };
      }).filter(s => s.name);
    }
    formData.resume = userInfo.resume || '';
    formData.resumeFileName = userInfo.resumeFileName || '';
  }
};

// 监听对话框显示，加载用户信息
watch(visible, (newVal) => {
  if (newVal) {
    // 先确保从 localStorage 恢复 pendingConfirmForm 数据
    userStore.getPendingConfirmForm();
    // 使用 nextTick 确保数据恢复后再加载
    nextTick(() => {
    loadUserInfo();
    });
  }
});

const handleBeforeClose = (done) => {
  if (props.isRequired) {
    ElMessage.warning('请先填写表单信息');
    return;
  }
  done();
};

const handleCancel = () => {
  visible.value = false;
};

const skillLimitReached = computed(() => formData.skills.length >= MAX_CORE_SKILLS);

const addSkill = () => {
  if (!formData.skillInput || !formData.skillRating) return;
  if (skillLimitReached.value) {
    ElMessage.warning(`最多添加 ${MAX_CORE_SKILLS} 项技能`);
    return;
  }
  formData.skills.push({
    name: formData.skillInput.trim(),
    score: formData.skillRating
  });
  formData.skillInput = '';
  formData.skillRating = 0;
};

const removeSkill = (idx) => {
  formData.skills.splice(idx, 1);
};

const handleResumeChange = (file) => {
  if (!file || !file.raw) return;
  formData.resumeFileName = file.name;
  const reader = new FileReader();
  reader.onload = (e) => {
    formData.resume = e.target?.result || '';
  };
  reader.readAsDataURL(file.raw);
};

const handleResumeRemove = () => {
  formData.resume = '';
  formData.resumeFileName = '';
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  
  try {
    await formRef.value.validate();
    
    isSubmitting.value = true;
    
    // 先把表单数据缓存到 store，跳转到二次确认页，由确认页再提交初始化
    const skillString = formData.skills.map(s => `${s.name}:${s.score}`).join(', ');
    userStore.setPendingConfirmForm({
      name: formData.name,
      workYears: formData.workYears,
      targetJob: formData.targetJob,
      expectedCity: formData.expectedCity,
      expectedSalary: formData.expectedSalary,
      skill: skillString || '',
      skills: formData.skills,
      resume: formData.resume || '',
      resumeFileName: formData.resumeFileName || '',
      // 是否编辑模式：确认页用于决定是否携带 sessionId
        isEdit: isEditMode.value,
      sessionId: isEditMode.value ? userStore.getSessionId() : undefined
      });
      
        visible.value = false;
    // 跳到确认页
    router.push('/confirm');
  } catch (error) {
    console.error('表单验证或提交失败:', error);
    if (error.message && !error.message.includes('validate')) {
      ElMessage.error(error.message || '提交失败，请重试');
    }
  } finally {
    isSubmitting.value = false;
  }
};

// 页面加载时，如果是编辑模式且对话框打开，加载已有信息
onMounted(() => {
  if (visible.value && isEditMode.value) {
    loadUserInfo();
  }
});
</script>

<style scoped>
.user-form-dialog :deep(.el-dialog__body) {
  padding-top: 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 4px;
}

.dialog-title {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #1f2937;
}
.dialog-title__icon {
  display: flex;
  align-items: center;
  color: var(--primary, #e86a58);
}
.dialog-title__main {
  font-weight: 700;
  font-size: 16px;
}
.dialog-title__sub {
  color: #6b7280;
  font-size: 12px;
}
.form-body {
  padding: 0 6px 6px;
}
.card-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.card {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 14px;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  color: #111827;
  font-weight: 700;
}
.card-icon {
  display: flex;
  align-items: center;
  color: var(--primary, #e86a58);
}
.card-title {
  font-size: 15px;
}
.card-sub {
  margin-left: auto;
  color: #6b7280;
  font-weight: 500;
  font-size: 12px;
}
.grid.two {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.field {
  width: 100%;
}
.field-label {
  margin-bottom: 6px;
  color: #4b5563;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}
.dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  background: #6366f1;
  border-radius: 50%;
}
.skill-input {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.skill-name {
  flex: 1 1 200px;
}
.skill-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.skill-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px;
}
.skill-chip .skill-name {
  min-width: 120px;
  font-weight: 600;
  color: #111827;
}
.remove-btn {
  margin-left: auto;
  color: #ef4444;
}
.empty-tip {
  margin-top: 8px;
}
.upload {
  width: 100%;
}
.upload-tip {
  margin-left: 10px;
  color: #6b7280;
}
</style>
