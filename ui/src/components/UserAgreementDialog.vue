<template>
  <el-dialog
    v-model="visibleModel"
    width="640px"
    top="6vh"
    :close-on-click-modal="true"
    class="user-agreement-dialog"
    append-to-body
    destroy-on-close
  >
    <template #header>
      <div class="agreement-dialog-header">
        <span class="agreement-dialog-icon">
          <el-icon :size="28"><Document /></el-icon>
        </span>
        <div>
          <div class="agreement-dialog-title">{{ USER_AGREEMENT_FILE_NAME }}</div>
          <div class="agreement-dialog-subtitle">请仔细阅读以下条款与补充说明</div>
        </div>
      </div>
    </template>

    <div class="agreement-dialog-body">
      <p v-for="(line, idx) in USER_AGREEMENT_INTRO" :key="'intro-' + idx" class="agreement-paragraph">
        {{ line }}
      </p>

      <section v-for="section in USER_AGREEMENT_SECTIONS" :key="section.title" class="agreement-section">
        <h3 class="agreement-section-title">{{ section.title }}</h3>
        <p
          v-for="(p, pIdx) in section.paragraphs || []"
          :key="section.title + '-p-' + pIdx"
          class="agreement-paragraph"
        >
          {{ p }}
        </p>
        <ul v-if="section.bullets?.length" class="agreement-bullets">
          <li v-for="(b, bIdx) in section.bullets" :key="section.title + '-b-' + bIdx">{{ b }}</li>
        </ul>
        <div v-for="(item, iIdx) in section.items || []" :key="section.title + '-i-' + iIdx" class="agreement-item">
          <div v-if="item.subtitle" class="agreement-item-subtitle">{{ item.subtitle }}</div>
          <p class="agreement-paragraph">{{ item.text }}</p>
        </div>
      </section>

      <section class="agreement-section agreement-supplement">
        <h3 class="agreement-section-title">{{ USER_AGREEMENT_SUPPLEMENT.title }}</h3>
        <ul class="agreement-bullets">
          <li v-for="(item, idx) in USER_AGREEMENT_SUPPLEMENT.items" :key="'sup-' + idx">{{ item }}</li>
        </ul>
      </section>

      <div class="agreement-confirm-box">
        {{ USER_AGREEMENT_CONFIRM_LINE }}
      </div>
    </div>

    <template #footer>
      <button type="button" class="agreement-close-btn" @click="visibleModel = false">关闭</button>
      <button type="button" class="agreement-accept-btn" @click="onAccept">我已阅读并同意</button>
    </template>
  </el-dialog>
</template>

<script setup>
import { Document } from '@element-plus/icons-vue';
import {
  USER_AGREEMENT_FILE_NAME,
  USER_AGREEMENT_INTRO,
  USER_AGREEMENT_SECTIONS,
  USER_AGREEMENT_SUPPLEMENT,
  USER_AGREEMENT_CONFIRM_LINE
} from '@/constants/userAgreement';

const visibleModel = defineModel({ type: Boolean, default: false });

const emit = defineEmits(['accept']);

const onAccept = () => {
  emit('accept');
  visibleModel.value = false;
};
</script>

<style scoped>
.agreement-dialog-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.agreement-dialog-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary);
}

.agreement-dialog-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.agreement-dialog-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: var(--text-secondary);
}

.agreement-dialog-body {
  max-height: min(58vh, 520px);
  overflow-y: auto;
  padding-right: 4px;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.75;
}

.agreement-section {
  margin-top: 20px;
}

.agreement-section-title {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}

.agreement-paragraph {
  margin: 0 0 10px;
}

.agreement-item {
  margin-bottom: 12px;
}

.agreement-item-subtitle {
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.agreement-bullets {
  margin: 0 0 10px;
  padding-left: 20px;
}

.agreement-bullets li {
  margin-bottom: 6px;
}

.agreement-supplement {
  padding: 14px 16px;
  background: var(--secondary-light);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
}

.agreement-confirm-box {
  margin-top: 18px;
  padding: 14px 16px;
  background: var(--primary-subtle);
  border: 1px solid rgba(232, 106, 88, 0.18);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.7;
}

.agreement-close-btn,
.agreement-accept-btn {
  border: none;
  border-radius: var(--radius-md);
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.agreement-close-btn {
  background: var(--bg-panel);
  color: var(--text-secondary);
  margin-right: 10px;
}

.agreement-close-btn:hover {
  background: var(--border-light);
}

.agreement-accept-btn {
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  color: #fff;
  box-shadow: var(--shadow-sm);
}

.agreement-accept-btn:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}
</style>

<style>
.user-agreement-dialog.el-dialog {
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.user-agreement-dialog .el-dialog__header {
  margin-right: 0;
  padding: 20px 24px 12px;
  border-bottom: 1px solid var(--border-light);
}

.user-agreement-dialog .el-dialog__body {
  padding: 16px 24px 8px;
}

.user-agreement-dialog .el-dialog__footer {
  padding: 12px 24px 20px;
  border-top: 1px solid var(--border-light);
}
</style>
