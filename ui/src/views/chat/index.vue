<template>
  <div class="chat-container">
    <div class="main-layout">
      <!-- 左侧聊天面板 -->
      <div class="chat-panel">
        <div class="progress-bar">
          <div class="progress-track">
            <div class="progress-fill" :style="{ width: `${progressPercent}%` }"></div>
          </div>
          <span class="progress-text">{{ progressPercent }}%</span>
        </div>
        <div class="stage-bar">
          <div
            v-for="stage in stageList"
            :key="stage.id"
            class="stage-item"
            :class="{
              active: currentStage === stage.id && stage.id !== 5,
              completed: currentStage > stage.id && stage.id !== 5,
              'stage-item--report': stage.id === 5,
              'stage-item--report-active': stage.id === 5 && currentStage === 5,
              'stage-item--report-loading': stage.id === 5 && stageReportLoading
            }"
            :title="stage.id === 5 ? stageReportHintText : undefined"
            @click="stage.id === 5 ? handleStageReportClick() : undefined"
          >
            <span class="stage-n">{{ stage.id }}</span>{{ stage.label }}
          </div>
        </div>
        <p class="stage-hint">遇到任何技术问题，可直接点击步骤5跳过</p>
        <div class="panel-header">
          <div class="panel-title">
            <svg viewBox="0 0 24 24" class="fox-icon" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 6l4-3 4 3 4-3 4 3v6c0 4-3.5 7-8 7s-8-3-8-7V6z"/>
              <circle cx="9" cy="11" r="1"/>
              <circle cx="15" cy="11" r="1"/>
              <path d="M10 15c.5.5 1.1.8 2 .8s1.5-.3 2-.8"/>
            </svg>
            小狐狸 · 职业引导官
          </div>
          <div class="panel-status-wrap">
            <div class="panel-status">
              <span class="status-dot"></span>
              在线陪你玩职业
            </div>
          </div>
        </div>
        <div class="message-list" ref="messageListRef">
          <div v-if="hasPendingConfirm" class="start-screen pending-confirm-screen">
            <div class="start-icon pending-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="8" r="4"></circle>
                <path d="M5.5 20a6.5 6.5 0 0 1 13 0"></path>
              </svg>
            </div>
            <div class="start-title">你还有信息待确认</div>
            <div class="start-desc">请先确认个人信息，确认后才能继续聊天流程。</div>
            <button type="button" class="start-btn" @click="reopenConfirm">打开信息确认页</button>
          </div>
          <div v-else-if="!sessionId && !hasStarted" class="start-screen">
            <div class="start-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
              </svg>
            </div>
            <div class="start-title">欢迎来到职业幻想世界</div>
            <div class="start-desc">
              和戴眼镜的小狐狸一起，像玩游戏一样完成职业身份设定吧！
            </div>
            <div class="landing-version-wrapper">
              <div class="version-title">选择体验版本</div>
              <div class="version-subtitle">根据你的需求选择合适的版本</div>
              <div class="version-cards">
                <div
                  class="version-card trial"
                  :class="{ selected: selectedVersion === 'trial' }"
                  @click="selectedVersion = 'trial'"
                >
                  <div class="version-badge">推荐</div>
                  <div class="version-icon">
                    <el-icon :size="26"><Aim /></el-icon>
                  </div>
                  <div class="version-name">体验版</div>
                  <div class="version-desc">快速了解核心功能，轻松上手</div>
                  <ul class="version-features">
                    <li>基础职业测评</li>
                    <li>简单岗位推荐</li>
                    <li>快速完成流程</li>
                  </ul>
                </div>
                <div
                  class="version-card full"
                  :class="{
                    selected: selectedVersion === 'full',
                    'is-unavailable': fullVersionTemporarilyClosed
                  }"
                  role="button"
                  tabindex="0"
                  @click="onFullVersionCardInteraction"
                  @keydown.enter.prevent="onFullVersionCardInteraction"
                  @keydown.space.prevent="onFullVersionCardInteraction"
                >
                  <div class="version-icon">
                    <el-icon :size="26"><StarFilled /></el-icon>
                  </div>
                  <div class="version-name">完全版</div>
                  <div class="version-desc">完整功能体验，深度职业分析</div>
                  <ul class="version-features">
                    <li>完整职业测评</li>
                    <li>深度岗位匹配</li>
                    <li>详细报告分析</li>
                    <li>个性化建议</li>
                  </ul>
                </div>
              </div>
            </div>
            <button
              type="button"
              class="start-btn"
              :class="{ 'is-disabled': !canStartAdventure }"
              :disabled="!canStartAdventure"
              @click="startMatching"
            >
              {{ !termsAccepted
                ? '请先勾选《用户承诺书》'
                : selectedVersion
                  ? (selectedVersion === 'trial' ? '以体验版开始冒险' : '以完全版开始冒险')
                  : '请选择版本后开始冒险' }}
            </button>
            <div class="start-terms">
              <label class="start-terms-row">
                <input
                  v-model="termsAccepted"
                  type="checkbox"
                  class="start-terms-checkbox"
                />
                <span class="start-terms-text">
                  我已阅读并同意
                  <button
                    type="button"
                    class="start-terms-link"
                    @click.stop.prevent="showAgreementDialog = true"
                  >
                    《{{ USER_AGREEMENT_FILE_NAME }}》
                  </button>
                </span>
              </label>
              <p class="start-terms-hint">
                AI 建议仅供参考，请保持独立判断；请勿输入身份证号等敏感信息。
              </p>
            </div>
            <UserAgreementDialog
              v-model="showAgreementDialog"
              @accept="termsAccepted = true"
            />
          </div>

          <template v-else>
            <div
              v-for="msg in messages"
              :key="msg.id"
              :class="['message', msg.type === 'user' ? 'user' : 'ai']"
            >
              <div class="msg-avatar" :class="msg.type === 'user' ? 'user' : 'ai'">
                <span v-if="msg.type === 'user'" class="msg-avatar-text">你</span>
                <img
                  v-else
                  class="msg-avatar-img"
                  src="/images/ai-avatar.png"
                  alt="小狐狸"
                />
              </div>
              <div class="msg-content">
                <div class="msg-bubble">
                  <div class="msg-header">
                    <span class="msg-title">{{ msg.type === 'user' ? '你 ' : '小狐狸 · 职业引导官' }}</span>
                    <span class="msg-time">{{ msg.timestamp }}</span>
                  </div>
                  <div v-if="msg.type === 'test'" class="test-button-wrapper">
                    <el-button
                      :type="userStore.isTestCompleted(msg.testType) ? 'info' : 'success'"
                      :disabled="userStore.isTestCompleted(msg.testType)"
                      size="large"
                      @click="handleTestButtonClick(msg.testType, msg.testQuestions)"
                      class="test-button"
                      :class="{ 'test-button-disabled': userStore.isTestCompleted(msg.testType) }"
                    >
                      {{ userStore.isTestCompleted(msg.testType) ? (msg.testType === 'skill' ? '技能测试已完成' : '兴趣测试已完成') : (msg.testType === 'skill' ? '开始技能测试' : '职业兴趣测试') }}
                      <el-icon v-if="!userStore.isTestCompleted(msg.testType)" style="margin-left: 8px"><ArrowRight /></el-icon>
                      <el-icon v-else style="margin-left: 8px"><Check /></el-icon>
                    </el-button>
                  </div>
                  <div v-else-if="msg.type === 'startSkillPrep'" class="test-button-wrapper">
                    <el-button
                      disabled
                      size="large"
                      class="test-button test-button-disabled"
                    >
                      题目准备中…
                    </el-button>
                  </div>
                  <div v-else-if="msg.type === 'startSkill'" class="test-button-wrapper">
                    <el-button
                      type="success"
                      size="large"
                      :disabled="isLoading"
                      @click="handleStartSkill"
                      class="test-button"
                    >
                      开始技能测试
                      <el-icon style="margin-left: 8px"><ArrowRight /></el-icon>
                    </el-button>
                  </div>
                  <div v-else-if="msg.type === 'report'" class="test-button-wrapper">
                    <el-button
                      :type="userStore.isTestCompleted('report') ? 'info' : 'success'"
                      :disabled="userStore.isTestCompleted('report')"
                      size="large"
                      @click="handleReportButtonClick"
                      class="test-button"
                      :class="{ 'test-button-disabled': userStore.isTestCompleted('report') }"
                    >
                      {{ userStore.isTestCompleted('report') ? '综合评价报告已查看' : '综合评价报告' }}
                      <el-icon v-if="!userStore.isTestCompleted('report')" style="margin-left: 8px"><ArrowRight /></el-icon>
                      <el-icon v-else style="margin-left: 8px"><Check /></el-icon>
                    </el-button>
                  </div>
                  <div v-else-if="msg.type === 'typing'" class="typing-dots" aria-label="正在输入">
                    <span class="typing-dot dot-1"></span>
                    <span class="typing-dot dot-2"></span>
                    <span class="typing-dot dot-3"></span>
                    <span class="typing-dot dot-4"></span>
                  </div>
                  <div v-else class="msg-text">{{ msg.content }}</div>
                </div>
              </div>
            </div>
            <div v-if="messages.length === 0 && !sessionId && hasStarted" class="empty-tips">加载中…</div>
            <div v-if="messages.length === 0 && hasInitialized && sessionId" class="empty-tips">
              <p>欢迎！{{ userStore.getUserInfo()?.name || '用户' }}，开始与职业助手对话吧！</p>
              <p v-if="initialMessage" class="initial-message">{{ initialMessage }}</p>
            </div>
          </template>
        </div>
        <div v-if="sessionId || hasStarted" class="control-panel">
          <div class="input-area">
            <input
              v-model="inputMessage"
              @keydown="handleKeyPress"
              :placeholder="inputPlaceholder"
              class="message-input"
              :disabled="inputDisabled"
            />
            <button class="send-button" @click="sendMessage" :disabled="!inputMessage.trim() || inputDisabled">发送</button>
            <button class="clear-button" @click="clearChat">清空</button>
          </div>
          <div class="settings">
            <span v-if="hasPendingConfirm" class="warning-text">请先确认信息，当前输入已锁定</span>
          </div>
        </div>
      </div>
      <div class="test-panel">
        <div class="panel-header">
          <div class="panel-title">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
            答题面板
          </div>
        </div>
        <div class="test-content">
          <!-- 版本选择在左侧开始页完成，此处不再重复，避免点击开始后右侧闪屏 -->
          <template v-if="showWelcomePanel">
            <div class="test-section welcome-scene-panel">
              <div class="test-label">第一章</div>
              <div class="test-title">认识你</div>
              <p class="welcome-hint">准备好出发了吗？点击下方按钮或直接在左侧输入「出发！」</p>
              <button type="button" class="cont-btn" :disabled="isLoading" @click="onWelcomeGo">开始第一章 · 认识你</button>
            </div>
          </template>
          <template v-else-if="showNicknamePanel">
            <div class="test-section crystal-section">
              <div class="test-label">水晶球命名</div>
              <div class="test-title">轻点字母泡泡，拼出你的名字（也可在底部输入框直接输入）</div>
              <div class="crystal-ball">
                <div class="crystal-glow"></div>
                <div class="nickname-display">{{ nicknameDraft || '点一点泡泡试试～' }}</div>
              </div>
              <div class="bubble-keyboard">
                <button
                  v-for="ch in bubbleLetters"
                  :key="ch"
                  type="button"
                  class="bubble-key"
                  :disabled="isLoading"
                  @click="nicknameDraft += ch"
                >{{ ch }}</button>
                <button type="button" class="bubble-key special" :disabled="isLoading" @click="nicknameDraft = nicknameDraft.slice(0, -1)">退格</button>
                <button type="button" class="bubble-key special" :disabled="isLoading" @click="nicknameDraft = ''">清空</button>
              </div>
              <div class="nickname-actions">
                <button type="button" class="opt-btn" :disabled="isLoading" @click="pickRandomNickname">
                  <span>随机一个可爱昵称</span>
                </button>
                <button type="button" class="cont-btn" :disabled="isLoading" @click="confirmNickname">确定昵称</button>
              </div>
            </div>
          </template>
          <template v-else-if="showWorkYearsPanel">
            <div class="test-section work-years-section">
              <div class="test-label">工作年限</div>
              <div class="test-title">选择最符合你当前情况的阶段</div>
              <div class="opt-list">
                <button
                  v-for="w in workYearsChoices"
                  :key="w.value"
                  type="button"
                  class="opt-btn work-years-btn"
                  :class="{ selected: pickedWorkYearsValue === w.value }"
                  :disabled="isLoading"
                  @click="pickedWorkYearsValue = w.value"
                ><span>{{ w.label }}</span></button>
              </div>
              <div class="book-detail">
                <template v-if="pickedWorkYearsChoice">
                  <div class="book-open-desc">{{ pickedWorkYearsChoice.hint }}</div>
                  <button type="button" class="cont-btn small" :disabled="isLoading" @click="confirmWorkYears">确认，继续</button>
                </template>
                <template v-else>点选一个选项</template>
              </div>
            </div>
          </template>
          <template v-else-if="showJobPanel">
            <div class="test-section magic-books">
              <div class="test-label">选择你的目标岗位</div>
              <div class="test-title">点击下方按钮查看介绍，再决定是否选择</div>
              <div class="opt-list">
                <button
                  v-for="j in jobOptions"
                  :key="j.id"
                  type="button"
                  class="opt-btn job-btn"
                  :class="{ selected: pickedJobId === j.id }"
                  :disabled="isLoading"
                  @click="pickedJobId = j.id"
                ><span>{{ j.name }}</span></button>
              </div>
              <div class="book-detail">
                <template v-if="pickedJob">
                  <div class="book-open-title">{{ pickedJob.name }}</div>
                  <div v-if="pickedJob.id === '其他'" class="book-open-desc">
                    <input v-model="otherJobText" class="skill-in" type="text" placeholder="请输入目标岗位" @keydown.enter.prevent="confirmJob" />
                  </div>
                  <div v-else class="book-open-desc">{{ pickedJob.desc }}</div>
                  <button type="button" class="cont-btn small" :disabled="isLoading" @click="confirmJob">选择这个岗位</button>
                </template>
                <template v-else>点一个岗位按钮看看它的职责～</template>
              </div>
            </div>
          </template>
          <template v-else-if="showCityPanel">
            <div class="test-section magic-books">
              <div class="test-label">选择你的目标城市</div>
              <div class="test-title">点击下方按钮查看介绍，再决定是否选择</div>
              <div class="opt-list">
                <button
                  v-for="c in cityOptions"
                  :key="c.id"
                  type="button"
                  class="opt-btn city-btn"
                  :class="{ selected: pickedCityId === c.id }"
                  :disabled="isLoading"
                  @click="pickedCityId = c.id"
                ><span>{{ c.name }}</span></button>
              </div>
              <div class="book-detail">
                <template v-if="pickedCity">
                  <div class="book-open-title">{{ pickedCity.name }}</div>
                  <div v-if="pickedCity.id === '其他'" class="book-open-desc">
                    <input v-model="otherCityText" class="skill-in" type="text" placeholder="请输入期望城市" @keydown.enter.prevent="confirmCity" />
                  </div>
                  <div v-else class="book-open-desc">{{ pickedCity.tag }}</div>
                  <button type="button" class="cont-btn small" :disabled="isLoading" @click="confirmCity">选择这个城市</button>
                </template>
                <template v-else>点一个城市按钮看看它的特点～</template>
              </div>
            </div>
          </template>
          <template v-else-if="showSalaryPanel">
            <div class="test-section gold-section">
              <div class="test-label">金币堆滑块</div>
              <div class="test-title">拖动滑块，选择你心中的理想月薪（也可在底部输入框输入，如 10k）</div>
              <div class="gold-display">
                <div class="gold-pile" :data-level="salaryLevel"></div>
                <div class="life-scene">{{ salarySceneText }}</div>
              </div>
              <div class="salary-row">
                <span class="salary-min">6k</span>
                <input v-model.number="salarySlider" type="range" min="6" max="50" step="1" class="salary-range" />
                <span class="salary-max">50k+</span>
              </div>
              <div class="salary-value">当前期望：约 {{ salarySlider }}k / 月</div>
              <div class="salary-actions">
                <button type="button" class="opt-btn" :disabled="isLoading" @click="salaryHintFromBackend"><span>查看行业平均参考</span></button>
              </div>
              <button type="button" class="cont-btn" :disabled="isLoading" @click="confirmSalaryStep">确认月薪，继续</button>
            </div>
          </template>
          <template v-else-if="displayPresetOptions.length">
            <div class="preset-grid">
              <div v-if="showOtherInput" class="preset-other">
                <input
                  v-model="otherInputValue"
                  class="preset-other-input"
                  :disabled="isLoading"
                  placeholder="请输入..."
                  @keydown.enter.prevent="submitOtherInput"
                />
                <button
                  type="button"
                  class="preset-other-send"
                  :disabled="isLoading"
                  @click="submitOtherInput"
                >发送</button>
              </div>
              <button
                v-for="opt in displayPresetOptions"
                :key="opt"
                type="button"
                class="preset-chip"
                :disabled="isLoading"
                @click="onPresetOptionClick(opt)"
              >{{ opt }}</button>
            </div>
          </template>
          <div v-else class="test-empty">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
            <div>{{ panelEmptyHint }}</div>
          </div>
        </div>
        <div v-if="showResumeUploadCard" class="control-panel resume-bottom-upload">
          <div class="input-area resume-bottom-upload-row">
            <div class="resume-bottom-upload-inputlike">
              <span class="resume-bottom-upload-main">简历快速填充（可选）</span>
              <span class="resume-bottom-upload-minor">支持 pdf/doc/docx</span>
            </div>
            <button
              type="button"
              class="resume-bottom-upload-btn"
              :disabled="resumeParsing"
              @click="triggerResumeUpload"
            >
              {{ resumeParsing ? '简历解析中...' : '上传简历' }}
            </button>
          </div>
          <input
            ref="resumeFileInputRef"
            type="file"
            class="resume-upload-input"
            accept=".pdf,.doc,.docx"
            @change="onResumeFileChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, onActivated, nextTick, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowRight, Check, Aim, StarFilled } from '@element-plus/icons-vue';
import { chatApi, chatStreamApi, collectStepApi, previewCollectSkillApi, parseResumeApi, startSkillApi, getWorkflowStatusApi, getPartialReportApi, getHollandReportApi } from '@/api/chat';
import {
  resolveStageReportScope,
  buildLocalProfileReport,
  buildProfileFromCollect,
  hasPartialCollectInfo,
  isCollectResumeEntry,
  stageReportHint,
  isFinalReportStage,
  hasConsensusPositions
} from '@/utils/quickReport';
import { FULL_VERSION_UNAVAILABLE_MSG, isFullVersionExperienceLabel } from '@/constants/experienceVersion';
import { parseSkillNamesFromProfile } from '@/utils/skillParse';
import { getFullVersionTemporarilyClosed } from '@/composables/useFullVersionGate';
import { useUserStore } from '@/stores/user';
import UserAgreementDialog from '@/components/UserAgreementDialog.vue';
import { USER_AGREEMENT_FILE_NAME } from '@/constants/userAgreement';
import {
  persistWorkflowFromResponse,
  loadWorkflowState,
  applyWorkflowToRefs,
  clearWorkflowState
} from '@/utils/workflowPhase';

const router = useRouter();
const userStore = useUserStore();

// 响应式状态
const messages = ref([]);
const inputMessage = ref('');
const messageListRef = ref(null);
const isLoading = ref(false);
const currentStreamingMessageId = ref(null);
const hasInitialized = ref(false);
const initialMessage = ref(null);
const hasStarted = ref(false);
const selectedVersion = ref('');
const termsAccepted = ref(false);
const showAgreementDialog = ref(false);
/** 与后端 app.full-version-temporarily-closed 同步；true 表示完全版入口关闭 */
const fullVersionTemporarilyClosed = ref(true);

const canStartAdventure = computed(() => Boolean(selectedVersion.value) && termsAccepted.value);

// 对话式收集状态（无 sessionId 时使用）
const collectSessionId = ref(null);
const currentQuestion = ref('');
const currentQuestionKey = ref('');
const presetOptions = ref([]);
const collectedAnswers = ref({});
/** 对话中 AI 返回的快捷选项（有 sessionId 时在右侧「快捷选项」展示） */
const chatPresetOptions = ref([]);
const workflowPhase = ref('');
const suggestStartSkill = ref(false);
const skillQuestionsPrepared = ref(false);
let skillPrepPollTimer = null;
const COLLECT_LAST_KEY = 'skill';
const otherInputVisible = ref(false);
const otherInputValue = ref('');

/** 昵称水晶球 */
const bubbleLetters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');
const nicknameDraft = ref('');

/** 岗位 / 城市：名称与后端 preset 一致，介绍文案前端仅作展示辅助 */
const JOB_DESC = {
  前端魔法师: '用代码搭建绚烂界面，与用户最近的施法者。',
  后端守护者: '守护数据与逻辑，保证世界稳定运转的核心。',
  产品策划师: '制定世界规则，规划玩家体验的幕后设计者。',
  界面画师: '负责一切「好看又好用」的地方。',
  数据占星师: '透过数字看趋势，为决策提供预言。',
  其他: '自定义补充说明'
};
/** 传给收集/NLP 为中文（如 1-3年）；落库为 0、1-3、3-5、5+（WorkYearsCodec.toDbValue） */
const workYearsChoices = [
  { value: '应届', label: '应届 / 在校', hint: '尚未全职工作或仍在校，以学习与积累为主。' },
  { value: '1-3年', label: '工作 1～3 年', hint: '已有一定职场经验，处于快速成长期。' },
  { value: '3-5年', label: '工作 3～5 年', hint: '能独立负责模块或项目，向骨干发展。' },
  { value: '5年以上', label: '工作 5 年以上', hint: '经验丰富，可承担复杂任务或带团队。' }
];

const pickedWorkYearsValue = ref('');

const CITY_TAG = {
  上海: '一线 · 互联网密集',
  北京: '一线 · 大厂云集',
  广州: '一线 · 生活烟火',
  杭州: '新一线 · 电商魔法',
  成都: '新一线 · 生活慢节奏',
  深圳: '一线 · 创业之城',
  其他: '自定义补充说明'
};

const pickedJobId = ref('');
const pickedCityId = ref('');
const otherJobText = ref('');
const otherCityText = ref('');

const salarySlider = ref(10);
const resumeParsing = ref(false);
const resumeFileInputRef = ref(null);

const showWelcomePanel = computed(() => {
  return !sessionId.value && hasStarted.value && currentQuestionKey.value === 'welcome';
});

const showNicknamePanel = computed(() => {
  return !sessionId.value && hasStarted.value && currentQuestionKey.value === 'name';
});

const showWorkYearsPanel = computed(() => {
  return !sessionId.value && hasStarted.value && currentQuestionKey.value === 'workYears';
});

const pickedWorkYearsChoice = computed(
  () => workYearsChoices.find((w) => w.value === pickedWorkYearsValue.value) || null
);

const showJobPanel = computed(() => {
  return !sessionId.value && hasStarted.value && currentQuestionKey.value === 'targetJob';
});

const showCityPanel = computed(() => {
  return !sessionId.value && hasStarted.value && currentQuestionKey.value === 'expectedCity';
});

const showSalaryPanel = computed(() => {
  return !sessionId.value && hasStarted.value && currentQuestionKey.value === 'expectedSalary';
});

const jobOptions = computed(() => {
  return (presetOptions.value || []).map((name) => ({
    id: name,
    name,
    desc: JOB_DESC[name] || ''
  }));
});

const cityOptions = computed(() => {
  return (presetOptions.value || []).map((name) => ({
    id: name,
    name,
    tag: CITY_TAG[name] || ''
  }));
});

const pickedJob = computed(() => jobOptions.value.find((j) => j.id === pickedJobId.value) || null);
const pickedCity = computed(() => cityOptions.value.find((c) => c.id === pickedCityId.value) || null);

const salaryLevel = computed(() => {
  const v = salarySlider.value;
  if (v <= 10) return 1;
  if (v <= 20) return 2;
  if (v <= 30) return 3;
  if (v <= 40) return 4;
  return 5;
});

const salarySceneText = computed(() => {
  const level = salaryLevel.value;
  if (level === 1) return '朴素但稳定的开局，小狐狸会帮你优先打基础技能副本。';
  if (level === 2) return '兼顾生活与成长的平衡路线，可以考虑进阶项目历练。';
  if (level === 3) return '中高难度挑战，意味着需要更扎实的项目经验和作品集。';
  if (level === 4) return '高星级 Boss 区间，小狐狸建议你准备好「亮点故事」和「硬核技能」。';
  return '传说级大奖励区，小狐狸决定先给你点掌声，再一起往这个目标慢慢靠近～';
});

// 计算属性：获取 sessionId
const sessionId = computed(() => {
  return userStore.getSessionId();
});

const hasPendingConfirm = computed(() => {
  return !sessionId.value && userStore.hasPendingCollectConfirm();
});

// 当前要在「快捷选项」面板展示的选项：有自定义右侧面板的收集步骤不重复展示 chip
const displayPresetOptions = computed(() => {
  if (sessionId.value) return chatPresetOptions.value;
  const key = currentQuestionKey.value;
  const customPanelKeys = ['experienceVersion', 'welcome', 'name', 'workYears', 'targetJob', 'expectedCity', 'expectedSalary'];
  if (customPanelKeys.includes(key)) return [];
  return presetOptions.value || [];
});

const showOtherInput = computed(() => {
  return !sessionId.value && otherInputVisible.value;
});

const panelEmptyHint = computed(() => {
  if (sessionId.value) return '等待问题...';
  if (hasPendingConfirm.value) return '请先确认个人信息';
  if (!sessionId.value && !hasStarted.value && isLoading.value) return '正在准备收集流程…';
  if (currentQuestionKey.value === 'skill') return '最后一问：请在底部输入框描述你的核心技能';
  if (currentQuestionKey.value === 'workYears') return '请在右侧选择工作年限';
  return '暂无快捷选项';
});

const showResumeUploadCard = computed(() => {
  if (sessionId.value || hasPendingConfirm.value) return false;
  if (!hasStarted.value) return false;
  return true;
});

const stageReportHintText = computed(() =>
  stageReportHint(resolveStageReportScope(currentStage.value, {
    userStore,
    workflowPhase: workflowPhase.value
  }))
);

const stageReportLoading = ref(false);

const handleStageReportClick = async () => {
  const sid = sessionId.value;
  const pendingForm = userStore.getPendingConfirmForm();

  if (!sid) {
    const hasPartial = hasPartialCollectInfo({
      collectedAnswers: collectedAnswers.value,
      pendingForm,
      draftName: nicknameDraft.value
    });
    if (!hasPartial) {
      ElMessage.warning('请先填写至少一项信息');
      return;
    }
    const scope = isCollectResumeEntry(pendingForm, null) ? 'resume' : 'profile';
    const local = buildProfileFromCollect(
      collectedAnswers.value,
      pendingForm,
      nicknameDraft.value
    );
    userStore.setCareerAssessmentReport(local);
    stageReportLoading.value = true;
    try {
      router.push({ path: '/report/career', query: { scope } });
    } finally {
      stageReportLoading.value = false;
    }
    return;
  }

  const scope = resolveStageReportScope(currentStage.value, {
    userStore,
    workflowPhase: workflowPhase.value,
    pendingForm
  });
  if (scope === 'skill') {
    if (userStore.getSkillReport()) {
      router.push('/report/skill');
      return;
    }
    ElMessage.warning('请先完成技能测评');
    return;
  }
  if (scope === 'holland') {
    stageReportLoading.value = true;
    try {
      if (!userStore.getHollandReport()) {
        const resp = await getHollandReportApi(sid);
        if (resp?.code === 1 && resp.data) {
          userStore.setHollandReport(resp.data);
        } else if (!userStore.getHollandReport()) {
          ElMessage.warning(resp?.msg || '暂无兴趣测试报告，请先完成霍兰德测评');
          return;
        }
      }
      router.push('/report/holland');
    } catch (e) {
      console.error('holland report failed', e);
      if (userStore.getHollandReport()) {
        router.push('/report/holland');
      } else {
        ElMessage.error(e?.message || '无法打开兴趣测试报告');
      }
    } finally {
      stageReportLoading.value = false;
    }
    return;
  }
  if (scope === 'full' && userStore.getCareerAssessmentReport()) {
    router.push({ path: '/report/career', query: { scope } });
    return;
  }
  stageReportLoading.value = true;
  try {
    const resp = await getPartialReportApi(sid, scope);
    if (resp && resp.code === 1 && resp.data) {
      userStore.setCareerAssessmentReport(resp.data);
    } else {
      const existing = userStore.getCareerAssessmentReport();
      const fallback = buildLocalProfileReport(userStore.getUserInfo());
      if (existing) {
        userStore.setCareerAssessmentReport(existing);
      } else if (fallback) {
        userStore.setCareerAssessmentReport(fallback);
      }
      if (!(existing || fallback)) {
        ElMessage.warning(resp?.msg || '报告暂不可用，请稍后重试');
        return;
      }
      ElMessage.warning(resp?.msg || '部分报告数据加载失败，展示已有内容');
    }
    router.push({ path: '/report/career', query: { scope } });
  } catch (e) {
    console.error('stage report failed', e);
    const existing = userStore.getCareerAssessmentReport();
    const fallback = buildLocalProfileReport(userStore.getUserInfo());
    if (existing || fallback) {
      if (fallback && !existing) {
        userStore.setCareerAssessmentReport(fallback);
      }
      router.push({ path: '/report/career', query: { scope } });
      ElMessage.warning(e?.message || '部分报告数据加载失败，展示已有内容');
    } else {
      ElMessage.error(e?.message || '无法打开报告，请稍后重试');
    }
  } finally {
    stageReportLoading.value = false;
  }
};

const stageList = [
  { id: 1, label: '信息填写' },
  { id: 2, label: '技能测评' },
  { id: 3, label: '职业人格' },
  { id: 4, label: '岗位推荐' },
  { id: 5, label: '报告输出' }
];

const currentStage = computed(() => {
  if (!sessionId.value || hasPendingConfirm.value) return 1;
  const phase = workflowPhase.value;
  if (phase === 'REPORT_READY' || phase === 'CLOSED' || userStore.isTestCompleted('report')) return 5;
  const fullReport = userStore.getCareerAssessmentReport();
  if (fullReport && hasConsensusPositions(fullReport)) return 5;
  if (['CONSENSUS_ACTIVE', 'PRELIMINARY_REC', 'CAREER_DONE'].includes(phase)
      || userStore.isTestCompleted('career')) return 4;
  if (phase === 'SKILL_DONE' || phase === 'CAREER_ACTIVE'
      || userStore.isTestCompleted('skill') || userStore.getPendingCareerTest()) return 3;
  return 2;
});

const removeSkillStartChatPrompts = () => {
  messages.value = messages.value.filter(
    (m) => m.type !== 'startSkill' && m.type !== 'startSkillPrep'
  );
};

/** 技能测评完成后保留「已完成」按钮（与兴趣测试卡片一致） */
const ensureSkillTestCompletedPrompt = () => {
  if (!userStore.isTestCompleted('skill')) return;
  removeSkillStartChatPrompts();
  if (!messages.value.some((m) => m.type === 'test' && m.testType === 'skill')) {
    addMessage('test', '', null, { testType: 'skill', testQuestions: [] });
    scrollToBottom();
  }
};

const syncSkillStartChatPrompt = () => {
  if (!sessionId.value || hasPendingConfirm.value) return;
  if (userStore.isTestCompleted('skill')) {
    ensureSkillTestCompletedPrompt();
    return;
  }
  const phase = workflowPhase.value;
  if (phase && phase !== 'MAIN_CHAT') {
    removeSkillStartChatPrompts();
    return;
  }

  if (skillQuestionsPrepared.value) {
    messages.value = messages.value.filter((m) => m.type !== 'startSkillPrep');
    if (!messages.value.some((m) => m.type === 'startSkill')) {
      addMessage('startSkill', '');
      scrollToBottom();
    }
    return;
  }

  if (!messages.value.some((m) => m.type === 'startSkillPrep')) {
    addMessage('startSkillPrep', '正在准备技能测评题目，请稍候…');
    scrollToBottom();
  }
};

const stopSkillPrepPoll = () => {
  if (skillPrepPollTimer) {
    clearInterval(skillPrepPollTimer);
    skillPrepPollTimer = null;
  }
};

const startSkillPrepPollIfNeeded = () => {
  stopSkillPrepPoll();
  if (!sessionId.value || userStore.isTestCompleted('skill')) return;
  if (skillQuestionsPrepared.value) return;
  const phase = workflowPhase.value;
  if (phase && phase !== 'MAIN_CHAT') return;

  skillPrepPollTimer = setInterval(async () => {
    try {
      const response = await getWorkflowStatusApi(sessionId.value);
      if (response?.code === 1 && response.data) {
        applyWorkflowFromResponse(response.data);
        if (response.data.skillQuestionsPrepared) {
          stopSkillPrepPoll();
        }
      }
    } catch (e) {
      console.warn('workflow status poll failed', e);
    }
  }, 2000);
};

const applyWorkflowFromResponse = (data) => {
  if (!data) return;
  const sid = sessionId.value;
  const saved = sid ? persistWorkflowFromResponse(sid, data) : {};
  applyWorkflowToRefs({ ...data, ...saved }, { workflowPhase, suggestStartSkill, skillQuestionsPrepared });
  if (skillQuestionsPrepared.value) {
    stopSkillPrepPoll();
  } else {
    startSkillPrepPollIfNeeded();
  }
  syncSkillStartChatPrompt();
};

const handleStartSkill = async () => {
  if (!sessionId.value || isLoading.value) return;
  isLoading.value = true;
  try {
    const response = await startSkillApi(sessionId.value);
    if (response?.code === 1 && response.data) {
      applyWorkflowFromResponse(response.data);
      const data = response.data;
      if (data.skillTestReady && data.skillTestQuestions && !userStore.isTestCompleted('skill')) {
        removeSkillStartChatPrompts();
        suggestStartSkill.value = false;
        userStore.setPendingTestPayload('skill', data.skillTestQuestions);
        router.push({ path: '/test/skill' });
        return;
      }
      if (data.content) {
        addMessage('bot', data.content);
      }
      ElMessage.warning('技能测评题目尚未就绪，请稍候再试');
    } else {
      ElMessage.error(response?.msg || '无法开始技能测评');
    }
  } catch (e) {
    console.error(e);
    ElMessage.error(e?.message || '开始技能测评失败');
  } finally {
    isLoading.value = false;
  }
};

const hasCareerTestMessage = () =>
  messages.value.some((m) => m.type === 'test' && m.testType === 'career');

const hasPostCareerRecommendationMessage = () =>
  messages.value.some(
    (m) =>
      m.type === 'bot' &&
      /初步推荐|推荐以下岗位|是否符合您的预期/.test(String(m.content || ''))
  );

const formatPreliminaryRecommendationContent = (data) => {
  const fromContent = formatBotChatContent(data?.content);
  if (fromContent) return fromContent;
  const pre = data?.preliminaryRecommendation;
  if (!pre) return '';
  const parts = [];
  if (pre.intro) parts.push(String(pre.intro).trim());
  const roles = pre.recommendedRoles || pre['推荐岗位'];
  if (Array.isArray(roles)) {
    roles.forEach((role, idx) => {
      if (!role) return;
      const title = role.title || role['岗位名称'] || '岗位';
      const reason = role.reason || role['匹配理由'] || '';
      parts.push(`${idx + 1}. **${title}**：${reason}`);
    });
  }
  const confirm = pre.confirmQuestion || pre['确认提问'];
  if (confirm) {
    parts.push(String(confirm).trim());
  } else if (roles?.length) {
    parts.push('以上推荐是否符合您的预期？请直接回复您的想法（如：符合 / 不符合 / 还行）。');
  }
  return parts.filter(Boolean).join('\n\n');
};

const showPostCareerFromPayload = (data) => {
  if (!data || userStore.isTestCompleted('report')) return false;
  const phase = data.phase || workflowPhase.value;
  const inPostCareerPhase =
    phase === 'PRELIMINARY_REC' ||
    !!data.preliminaryRecommendation ||
    !!data.postCareerContent ||
    (phase === 'CONSENSUS_ACTIVE' && !data.consensusResult);
  if (!inPostCareerPhase) return false;
  if (hasPostCareerRecommendationMessage()) return false;
  const content = formatPreliminaryRecommendationContent(data);
  if (!content) return false;
  addMessage('bot', content);
  scrollToBottom();
  return true;
};

/** 共识阶段 Bot 回复（continue / complete） */
const handleConsensusFromPayload = (data) => {
  const consensus = data?.consensusResult;
  if (!consensus) return false;
  const status = String(consensus.status || '').toLowerCase();
  if (status === 'complete') {
    if (data.careerAssessmentReport) {
      userStore.setCareerAssessmentReport(data.careerAssessmentReport);
    }
    const displayContent =
      consensus.reply ||
      formatBotChatContent(data.content) ||
      '共识已完成，已为您生成综合评价报告，请点击下方按钮查看。';
    if (displayContent) {
      addMessage('bot', displayContent);
    }
    addMessage('report', '');
    scrollToBottom();
    return true;
  }
  const reply = consensus.reply || formatBotChatContent(data.content);
  if (!reply) return false;
  addMessage('bot', reply);
  scrollToBottom();
  return true;
};

/** 兴趣测试完成后展示初步岗位推荐，并进入共识对话 */
const syncPostCareerChatPrompt = async () => {
  if (!sessionId.value || !userStore.isTestCompleted('career')) return;
  if (hasPostCareerRecommendationMessage()) return;

  const pending = userStore.getPendingCareerTest();
  if (pending?.postCareerContent || pending?.preliminaryRecommendation) {
    if (showPostCareerFromPayload(pending)) {
      userStore.clearPendingCareerTest();
    }
    return;
  }

  try {
    const response = await getWorkflowStatusApi(sessionId.value);
    if (response?.code === 1 && response.data) {
      applyWorkflowFromResponse(response.data);
      showPostCareerFromPayload(response.data);
    }
  } catch (e) {
    console.warn('sync post-career prompt failed', e);
  }
};

const showCareerTestFromPayload = (data) => {
  if (!data?.careerTestQuestions || userStore.isTestCompleted('career')) {
    return false;
  }
  if (hasCareerTestMessage()) {
    return false;
  }
  const content =
    formatBotChatContent(data.content) ||
    '职业兴趣测评已准备好，请点击下方按钮开始作答。';
  if (content) {
    addMessage('bot', content);
  }
  addMessage('test', '', null, {
    testType: 'career',
    testQuestions: data.careerTestQuestions
  });
  return true;
};

/** 技能测评完成后，恢复霍兰德测试入口（内存 / localStorage / workflow/status） */
const syncCareerTestChatPrompt = async () => {
  if (!sessionId.value || !userStore.isTestCompleted('skill')) {
    return;
  }
  if (userStore.isTestCompleted('career') || hasCareerTestMessage()) {
    return;
  }

  const pending = userStore.getPendingCareerTest();
  if (pending?.careerTestQuestions) {
    showCareerTestFromPayload(pending);
    userStore.clearPendingCareerTest();
    return;
  }

  try {
    for (let attempt = 0; attempt < 3; attempt += 1) {
      const response = await getWorkflowStatusApi(sessionId.value);
      if (response?.code !== 1 || !response.data) {
        break;
      }
      const data = response.data;
      const saved = persistWorkflowFromResponse(sessionId.value, data);
      applyWorkflowToRefs(
        { ...data, ...saved },
        { workflowPhase, suggestStartSkill, skillQuestionsPrepared }
      );
      ensureSkillTestCompletedPrompt();
      if (showCareerTestFromPayload(data)) {
        userStore.clearPendingCareerTest();
        return;
      }
      const phase = data.phase;
      if ((phase === 'CAREER_ACTIVE' || phase === 'SKILL_DONE') && attempt < 2) {
        await new Promise((r) => setTimeout(r, 1500));
        continue;
      }
      break;
    }
  } catch (e) {
    console.warn('sync career test prompt failed', e);
  }
};

/** 是否已有最终报告相关的对话入口（文案或按钮） */
const hasReportPromptInChat = () =>
  messages.value.some(
    (m) =>
      m.type === 'report' ||
      (m.type === 'bot' &&
        /综合评价报告|共识已完成.*报告/.test(String(m.content || '')))
  );

/** 最终报告就绪时，在对话区恢复入口（仅用于刷新/返回页面） */
const syncReportReadyPrompt = () => {
  if (
    !isFinalReportStage({
      workflowPhase: workflowPhase.value,
      userStore
    })
  ) {
    return;
  }
  if (!userStore.getCareerAssessmentReport()) return;
  if (hasReportPromptInChat()) return;
  addMessage('bot', '已生成综合评价报告，请点击下方按钮查看。');
  addMessage('report', '');
};

const progressPercent = computed(() => Math.round((currentStage.value / 5) * 100));

/** 开局分步收集（尚无正式 session / token） */
const isCollectingProfile = computed(() => hasStarted.value && !sessionId.value);

const resetWorkflowUiState = () => {
  workflowPhase.value = '';
  suggestStartSkill.value = false;
  skillQuestionsPrepared.value = false;
  stopSkillPrepPoll();
};

const isMainChatInputBlockedByPhase = computed(() => {
  if (!sessionId.value || isCollectingProfile.value) return false;
  const p = workflowPhase.value;
  return p === 'SKILL_ACTIVE' || p === 'CAREER_ACTIVE'
      || p === 'REPORT_READY' || p === 'CLOSED';
});

/** 正式会话 MAIN_CHAT 且技能题未就绪：禁止主聊输入，避免与 startSkill 竞态 */
const isWaitingSkillQuestionsPrep = computed(() => {
  if (!sessionId.value || hasPendingConfirm.value || isCollectingProfile.value) return false;
  if (userStore.isTestCompleted('skill')) return false;
  if (workflowPhase.value !== 'MAIN_CHAT') return false;
  return !skillQuestionsPrepared.value;
});

const inputDisabled = computed(() => {
  return isLoading.value || hasPendingConfirm.value || isMainChatInputBlockedByPhase.value
      || isWaitingSkillQuestionsPrep.value;
});

const inputPlaceholder = computed(() => {
  if (hasPendingConfirm.value) return '请先确认个人信息后再继续对话';
  if (!sessionId.value) {
    return currentQuestion.value || '输入消息，按 Enter 发送...';
  }
  if (isWaitingSkillQuestionsPrep.value) {
    return '技能测评题目准备中，请稍候…';
  }
  if (workflowPhase.value === 'SKILL_ACTIVE' || workflowPhase.value === 'CAREER_ACTIVE') {
    return '测评进行中，请在上方测试卡片中作答';
  }
  if (workflowPhase.value === 'CONSENSUS_ACTIVE') {
    return '共识阶段：请回复对初步推荐岗位的看法…';
  }
  if (workflowPhase.value === 'REPORT_READY' || workflowPhase.value === 'CLOSED') {
    return '报告已生成，请通过下方按钮查看';
  }
  return '输入消息，按 Enter 发送...';
});

/** 避免 Bot 未同步时把报告 JSON 裸显在聊天气泡 */
const formatBotChatContent = (raw) => {
  const text = String(raw || '').trim();
  if (!text) return '';
  if (text.includes('测试综合得分') || text.includes('霍兰德职业兴趣代码')) {
    return '已生成测评相关内容，请通过下方按钮或测试卡片查看，勿在聊天中重复提交。';
  }
  if (text.startsWith('{') && (
    text.includes('"hollandCode"')
    || text.includes('"status"')
    || text.includes('"总结"')
    || text.includes('skillAnalysis')
    || (text.includes('"questions"') && (text.includes('"题干"') || text.includes('"stem"')))
  )) {
    return '职业兴趣测评已准备好，请点击下方测试卡片开始作答。';
  }
  if ((text.includes('####') || text.includes('**1.')) && (text.includes('选择题') || text.includes('主观题'))) {
    return '测评题目已生成，请点击下方「开始技能测试」按钮或测试卡片作答，无需在聊天中重复查看题目。';
  }
  return text;
};

// 点击快捷选项：无 session 走收集步骤，有 session 则作为普通消息发送
const onPresetOptionClick = (opt) => {
  if (sessionId.value) {
    chatPresetOptions.value = [];
    addMessage('user', opt);
    handleNormalRequest(opt);
  } else {
    if (opt === '其他') {
      otherInputVisible.value = true;
      otherInputValue.value = '';
      return;
    }
    otherInputVisible.value = false;
    otherInputValue.value = '';
    sendCollectStep(opt);
  }
};

const submitOtherInput = async () => {
  const text = (otherInputValue.value || '').trim();
  if (!text) {
    ElMessage.warning('请输入内容');
    return;
  }
  otherInputVisible.value = false;
  otherInputValue.value = '';
  await sendCollectStep(text);
};

const resetCollectPanelLocals = () => {
  nicknameDraft.value = '';
  pickedWorkYearsValue.value = '';
  pickedJobId.value = '';
  pickedCityId.value = '';
  otherJobText.value = '';
  otherCityText.value = '';
  salarySlider.value = 10;
};

const clearCollectedAnswers = () => {
  collectedAnswers.value = {};
  userStore.clearPendingConfirmForm();
};

const saveCollectedAnswer = (questionKey, userInput) => {
  if (!questionKey) return;
  collectedAnswers.value[questionKey] = (userInput ?? '').toString().trim();
};

const normalizeWorkYears = (v) => {
  if (v == null || v === '') return '';
  const s = String(v).trim();
  const map = {
    '应届': '0',
    '应届/在校': '0',
    '应届 / 在校': '0',
    '1-3年': '1-3',
    '3-5年': '3-5',
    '5年以上': '5+'
  };
  if (map[s] !== undefined) return map[s];
  if (['0', '1-3', '3-5', '5+'].includes(s)) return s;
  return s;
};

const triggerResumeUpload = () => {
  if (resumeParsing.value) return;
  if (!collectSessionId.value) {
    ElMessage.warning('请先点击“开始冒险”进入信息收集');
    return;
  }
  resumeFileInputRef.value?.click();
};

const onResumeFileChange = async (event) => {
  const file = event?.target?.files?.[0];
  if (!file) return;
  event.target.value = '';

  const fileName = file.name || '';
  const ext = fileName.includes('.') ? fileName.split('.').pop().toLowerCase() : '';
  const allowExt = ['pdf', 'doc', 'docx'];
  if (!allowExt.includes(ext)) {
    ElMessage.warning('仅支持 pdf/doc/docx 格式');
    return;
  }
  const maxBytes = 5 * 1024 * 1024;
  if (file.size > maxBytes) {
    ElMessage.warning('文件大小不能超过 5MB');
    return;
  }

  const fd = new FormData();
  fd.append('file', file);
  if (collectSessionId.value) fd.append('sessionId', collectSessionId.value);
  if (collectedAnswers.value?.experienceVersion) {
    fd.append('experienceVersion', collectedAnswers.value.experienceVersion);
  }

  const typingMessageId = Date.now() + Math.random();
  addMessage('typing', '', typingMessageId);
  resumeParsing.value = true;
  try {
    const resp = await parseResumeApi(fd);
    const data = resp?.data;
    if (resp?.code !== 1 || !data?.profilePreview) {
      messages.value = messages.value.filter((m) => m.id !== typingMessageId);
      addMessage('bot', data?.msg || '简历解析失败，可继续对话收集信息。');
      return;
    }
    messages.value = messages.value.filter((m) => m.id !== typingMessageId);
    addMessage('bot', '简历解析完成，已为你预填信息，去确认页检查一下吧～');

    const p = data.profilePreview || {};
    const skills = parseSkillNamesFromProfile(p.skill);
    userStore.setPendingConfirmForm({
      fromCollect: true,
      sessionId: data.sessionId || collectSessionId.value,
      experienceVersion: collectedAnswers.value.experienceVersion || '',
      name: p.name || '',
      workYears: normalizeWorkYears(p.workYears || ''),
      targetJob: p.targetJob || '',
      expectedCity: p.expectedCity || '',
      expectedSalary: p.expectedSalary || '',
      skill: p.skill || '',
      resume: p.resume || '',
      resumeFileName: fileName,
      skills
    });
    router.push('/confirm');
  } catch (e) {
    console.error(e);
    messages.value = messages.value.filter((m) => m.id !== typingMessageId);
    const errHint = e?.message && !e.message.includes('接口') ? e.message : '简历解析失败，可继续右侧/左侧流程手动填写。';
    addMessage('bot', errHint);
  } finally {
    resumeParsing.value = false;
  }
};

/** 核心技能先调后端 NLP + 收集 Agent，再跳转确认页（不落库） */
const gotoCollectConfirmAfterPreview = async (skillInput, options = {}) => {
  const displayUser = options.displayAs != null ? options.displayAs : skillInput;
  if (!options.skipUserEcho) {
    addMessage('user', displayUser);
  }
  const typingMessageId = Date.now() + Math.random();
  addMessage('typing', '', typingMessageId);
  isLoading.value = true;
  inputMessage.value = '';
  try {
    const res = await previewCollectSkillApi({
      sessionId: collectSessionId.value || undefined,
      userInput: skillInput
    });
    const data = res?.data;
    messages.value = messages.value.filter((m) => m.id !== typingMessageId);
    if (!data || data.code !== 1) {
      addMessage('bot', data?.msg || '请按提示重新填写');
      return;
    }
    const p = data.profilePreview || {};
    const skills = parseSkillNamesFromProfile(p.skill);
    userStore.setPendingConfirmForm({
      fromCollect: true,
      sessionId: data.sessionId,
      experienceVersion: collectedAnswers.value.experienceVersion || '',
      name: p.name || '',
      workYears: p.workYears || '',
      targetJob: p.targetJob || '',
      expectedCity: p.expectedCity || '',
      expectedSalary: p.expectedSalary || '',
      skill: p.skill || '',
      resume: p.resume || '',
      skills
    });
    router.push('/confirm');
  } catch (e) {
    console.error(e);
    messages.value = messages.value.filter((m) => m.id !== typingMessageId);
    ElMessage.error(e?.message || '处理失败，请稍后重试');
  } finally {
    isLoading.value = false;
  }
};

const reopenConfirm = () => {
  if (!hasPendingConfirm.value) return;
  router.push('/confirm');
};

const bootstrapCollect = async (options = {}) => {
  isLoading.value = true;
  try {
    const res = await collectStepApi({ bootstrap: true });
    const data = res?.data;
    if (data?.code === 1) {
      collectSessionId.value = data.sessionId;
      currentQuestionKey.value = data.nextQuestionKey || 'experienceVersion';
      currentQuestion.value = data.nextQuestion || '';
      presetOptions.value = data.presetOptions || [];
      if (data.nextQuestion && !options.skipInitialQuestionMessage) {
        addMessage('bot', data.nextQuestion);
      }
      resetCollectPanelLocals();
      clearCollectedAnswers();
      scrollToBottom();
      return true;
    }
    ElMessage.error('初始化收集流程失败');
    return false;
  } catch (e) {
    console.error(e);
    ElMessage.error(e?.message || '初始化收集流程失败');
    return false;
  } finally {
    isLoading.value = false;
  }
};

/** 顶部栏「重新开始」触发 store 后，同步重置本页 UI */
const resetChatViewToWelcome = () => {
  const collectSid = collectSessionId.value;
  messages.value = [];
  currentStreamingMessageId.value = null;
  collectSessionId.value = null;
  currentQuestion.value = '';
  currentQuestionKey.value = '';
  presetOptions.value = [];
  chatPresetOptions.value = [];
  otherInputVisible.value = false;
  otherInputValue.value = '';
  selectedVersion.value = '';
  termsAccepted.value = false;
  inputMessage.value = '';
  hasStarted.value = false;
  hasInitialized.value = false;
  initialMessage.value = null;
  resetCollectPanelLocals();
  clearCollectedAnswers();
  const sid = userStore.getSessionId();
  if (sid) clearWorkflowState(sid);
  if (collectSid) clearWorkflowState(collectSid);
  resetWorkflowUiState();
  ElMessage.success('已清除本地进度，可以从点击「开始冒险」重新开始');
};

watch(
  () => userStore.chatRestartTick,
  (n, o) => {
    if (o === undefined) return;
    if (n > o) {
      resetChatViewToWelcome();
    }
  }
);

const onFullVersionCardInteraction = () => {
  if (fullVersionTemporarilyClosed.value) {
    ElMessage.warning(FULL_VERSION_UNAVAILABLE_MSG);
    return;
  }
  selectedVersion.value = 'full';
};

const startMatching = async () => {
  if (hasStarted.value) return;

  if (!termsAccepted.value) {
    ElMessage.warning('请先阅读并勾选《用户承诺书》');
    return;
  }

  if (!selectedVersion.value) {
    ElMessage.warning('请先选择一个体验版本');
    return;
  }

  if (
    (await getFullVersionTemporarilyClosed()) &&
    (selectedVersion.value === 'full' || isFullVersionExperienceLabel(selectedVersion.value))
  ) {
    ElMessage.warning(FULL_VERSION_UNAVAILABLE_MSG);
    return;
  }

  const selectedLabel = selectedVersion.value === 'trial' ? '体验版' : '完全版';

  try {
    const sid = userStore.getSessionId();
    userStore.purgeSessionScopedStorage(sid);
    if (sid) clearWorkflowState(sid);
    userStore.clearAll();
    localStorage.removeItem('loginUser');
  } catch (e) {
    console.warn('清除本地存储失败', e);
  }

  resetWorkflowUiState();

  messages.value = [];
  currentStreamingMessageId.value = null;
  collectSessionId.value = null;
  currentQuestion.value = '';
  currentQuestionKey.value = '';
  presetOptions.value = [];
  chatPresetOptions.value = [];
  otherInputVisible.value = false;
  otherInputValue.value = '';
  resetCollectPanelLocals();
  clearCollectedAnswers();

  // 在左侧开始页完成版本选择；此处先不进入「已开始」态，避免右侧答题区出现重复版本 UI 或空状态闪屏
  const ok = await bootstrapCollect({ skipInitialQuestionMessage: true });
  if (!ok) {
    currentQuestionKey.value = '';
    return;
  }
  if (currentQuestionKey.value === 'experienceVersion') {
    await sendCollectStep(selectedLabel, { skipUserEcho: true });
  }
  hasStarted.value = true;
  selectedVersion.value = '';
  termsAccepted.value = false;
};

const onWelcomeGo = () => sendCollectStep('出发！');

const pickRandomNickname = () => {
  const code = Math.random().toString(36).slice(2).padEnd(6, '0').slice(0, 6);
  nicknameDraft.value = `冒险者${code}`;
};

const confirmNickname = async () => {
  const n = (nicknameDraft.value || '').trim();
  if (!n) {
    ElMessage.warning('请先输入昵称，或使用随机昵称');
    return;
  }
  await sendCollectStep(n);
};

const confirmWorkYears = async () => {
  if (!pickedWorkYearsValue.value) {
    ElMessage.warning('请先选择工作年限');
    return;
  }
  const label = pickedWorkYearsChoice.value?.label || pickedWorkYearsValue.value;
  await sendCollectStep(pickedWorkYearsValue.value, { displayAs: label });
};

const confirmJob = async () => {
  if (!pickedJobId.value) {
    ElMessage.warning('请先选择一个岗位');
    return;
  }
  if (pickedJobId.value === '其他') {
    const t = (otherJobText.value || '').trim();
    if (!t) {
      ElMessage.warning('请填写目标岗位');
      return;
    }
    await sendCollectStep(t);
    return;
  }
  await sendCollectStep(pickedJobId.value);
};

const confirmCity = async () => {
  if (!pickedCityId.value) {
    ElMessage.warning('请先选择一个城市');
    return;
  }
  if (pickedCityId.value === '其他') {
    const t = (otherCityText.value || '').trim();
    if (!t) {
      ElMessage.warning('请填写期望城市');
      return;
    }
    await sendCollectStep(t);
    return;
  }
  await sendCollectStep(pickedCityId.value);
};

const salaryHintFromBackend = () => {
  addMessage(
    'bot',
    '根据公开招聘数据，一线城市新人起薪多集中在 8k-15k，中高级多在 20k-40k 区间，不同岗位会有较大差异，我们可以在后续匹配时再精调。'
  );
};

const confirmSalaryStep = async () => {
  await sendCollectStep(`${salarySlider.value}k`);
};

// 添加消息到列表
const addMessage = (type, content, id = null, extra = {}) => {
  const messageId = id || Date.now() + Math.random();
  
  // 如果传入了 id，先尝试查找是否已存在
  if (id) {
    const index = messages.value.findIndex(msg => msg.id === id);
    if (index !== -1) {
      // 消息已存在，更新内容
      messages.value[index].content = content;
      Object.assign(messages.value[index], extra);
      // 保存聊天记录
      userStore.saveChatMessages(messages.value);
      scrollToBottom();
      return messageId;
    }
  }
  
  // 消息不存在，创建新消息
  messages.value.push({
    id: messageId,
    type,
    content,
    timestamp: new Date().toLocaleTimeString(),
    ...extra
  });
  // 保存聊天记录
  userStore.saveChatMessages(messages.value);
  scrollToBottom();
  return messageId;
};

// 发送消息
const sendMessage = async () => {
  if (hasPendingConfirm.value) {
    ElMessage.warning('请先确认个人信息');
    reopenConfirm();
    return;
  }
  const text = inputMessage.value.trim();
  
  if (!text) {
    ElMessage.warning('请输入消息内容');
    return;
  }

  // 无 sessionId：走对话式收集流程
  if (!sessionId.value) {
    await sendCollectStep(text);
    return;
  }

  inputMessage.value = '';
  addMessage('user', text);
  await handleNormalRequest(text);
};

// 对话式收集一步：提交 questionKey + userInput + mode（话术与选项由后端返回）
const sendCollectStep = async (userInput, options = {}) => {
  const questionKey = currentQuestionKey.value || 'experienceVersion';
  if (!options.forceSubmit && questionKey === COLLECT_LAST_KEY) {
    await gotoCollectConfirmAfterPreview(userInput, options);
    return;
  }
  isLoading.value = true;
  inputMessage.value = '';
  const displayUser = options.displayAs != null ? options.displayAs : userInput;
  if (!options.skipUserEcho) {
    addMessage('user', displayUser);
  }
  const typingMessageId = Date.now() + Math.random();
  // 收集阶段也要显示等待动画，和正常对话保持一致
  addMessage('typing', '', typingMessageId);
  try {
    const res = await collectStepApi({
      sessionId: collectSessionId.value || undefined,
      questionKey,
      userInput
    });
    const data = res?.data;
    if (!data) {
      messages.value = messages.value.filter(m => m.id !== typingMessageId);
      ElMessage.error('请求异常');
      return;
    }
    if (data.code === 0) {
      messages.value = messages.value.filter(m => m.id !== typingMessageId);
      addMessage('bot', data.msg || '请按提示重新填写');
      return;
    }
    saveCollectedAnswer(questionKey, userInput);
    if (data.sessionId && data.token) {
      userStore.setUserInfo({ sessionId: data.sessionId, token: data.token });
      localStorage.setItem('loginUser', JSON.stringify({ token: data.token, userId: data.sessionId, username: '' }));
      hasInitialized.value = true;
      if (data.content) addMessage('bot', data.content);
      applyWorkflowFromResponse(data);
      collectSessionId.value = null;
      currentQuestion.value = '';
      currentQuestionKey.value = '';
      presetOptions.value = [];
      otherInputVisible.value = false;
      otherInputValue.value = '';
      resetCollectPanelLocals();
      clearCollectedAnswers();
      messages.value = messages.value.filter(m => m.id !== typingMessageId);
      return;
    }
    messages.value = messages.value.filter(m => m.id !== typingMessageId);
    collectSessionId.value = data.sessionId || collectSessionId.value;
    currentQuestion.value = data.nextQuestion || '';
    currentQuestionKey.value = data.nextQuestionKey || '';
    presetOptions.value = data.presetOptions || [];
    otherInputVisible.value = false;
    otherInputValue.value = '';
    if (data.nextQuestion) addMessage('bot', data.nextQuestion);
    else if (data.msg) addMessage('bot', data.msg);
    resetCollectPanelLocals();
  } catch (e) {
    console.error(e);
    messages.value = messages.value.filter(m => m.id !== typingMessageId);
    ElMessage.error(e?.message || '提交失败');
  } finally {
    isLoading.value = false;
  }
};

// 处理普通请求
const handleNormalRequest = async (text) => {
  isLoading.value = true;
  // 先插入一条“光标/打字中”消息，让用户感知 agent 在线处理中
  const typingMessageId = Date.now() + Math.random();
  addMessage('typing', '', typingMessageId);
  try {
    const response = await chatApi(sessionId.value, text);

    if (response && response.code === 40004) {
      messages.value = messages.value.filter(m => m.id !== typingMessageId);
      ElMessage.warning(response.msg || '当前阶段不允许发送主聊消息');
      chatPresetOptions.value = [];
      return;
    }

    if (response && response.code === 1 && response.data) {
      const data = response.data;
      applyWorkflowFromResponse(data);
      messages.value = messages.value.filter((m) => m.id !== typingMessageId);

      if (data.skillTestReady && data.skillTestQuestions) {
        if (!userStore.isTestCompleted('skill')) {
          addMessage('test', '', null, {
            testType: 'skill',
            testQuestions: data.skillTestQuestions
          });
        }
      } else if (data.careerTestReady && data.careerTestQuestions) {
        if (!userStore.isTestCompleted('career')) {
          addMessage('test', '', null, {
            testType: 'career',
            testQuestions: data.careerTestQuestions
          });
        }
      } else if (handleConsensusFromPayload(data)) {
        /* 共识多轮 / 完成 */
      } else if (data.careerAssessmentReport) {
        userStore.setCareerAssessmentReport(data.careerAssessmentReport);
        const displayContent =
          formatBotChatContent(data.content) ||
          '已生成综合评价报告，请点击下方按钮查看。';
        if (displayContent) {
          addMessage('bot', displayContent);
        }
        addMessage('report', '');
      } else if (
        data.preliminaryRecommendation ||
        data.phase === 'PRELIMINARY_REC' ||
        (data.phase === 'CONSENSUS_ACTIVE' && !data.consensusResult)
      ) {
        showPostCareerFromPayload(data);
      } else if (data.content) {
        const displayContent = formatBotChatContent(data.content);
        if (displayContent) {
          addMessage('bot', displayContent);
        }
      }
      // AI 返回的快捷选项，在右侧「快捷选项」面板展示
      if (Array.isArray(data.presetOptions)) {
        chatPresetOptions.value = data.presetOptions;
      } else {
        chatPresetOptions.value = [];
      }
    } else {
      messages.value = messages.value.filter(m => m.id !== typingMessageId);
      addMessage('bot', response?.msg || '收到空回复');
      chatPresetOptions.value = [];
    }
  } catch (error) {
    console.error('普通请求失败:', error);
    messages.value = messages.value.filter(m => m.id !== typingMessageId);
    const errData = error?.response?.data;
    if (errData?.code === 40004) {
      ElMessage.warning(errData.msg || '当前阶段不允许发送主聊消息');
    } else {
      addMessage('bot', `请求失败: ${error.message}`);
      ElMessage.error('消息发送失败');
    }
    chatPresetOptions.value = [];
  } finally {
    isLoading.value = false;
  }
};

// 处理流式请求
const handleStreamRequest = async (text) => {
  const botMessageId = Date.now() + Math.random();
  currentStreamingMessageId.value = botMessageId;
  addMessage('typing', '', botMessageId);

  try {
    const response = await chatStreamApi(sessionId.value, text);
    
    if (!response.body) {
      throw new Error('响应中没有可读的流');
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';
    let accumulatedContent = '';
    let currentEvent = null;

    const stripLegacyMarkers = (text) => text.replace(/\[(CHAT_END|SKILL_TEST|SKILL_END|HOLLAND_TEST|HOLLAND_END|MBTI_TEST|MBTI_END)\]/g, '');

    try {
      while (true) {
        const { done, value } = await reader.read();
        
        if (done) {
          const finalIndex = messages.value.findIndex(msg => msg.id === botMessageId);
          if (finalIndex !== -1) {
            messages.value[finalIndex].content = stripLegacyMarkers(accumulatedContent).trim();
          }
          currentStreamingMessageId.value = null;
          break;
        }

        buffer += decoder.decode(value, { stream: true });
        
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';
        
        for (const line of lines) {
          const trimmedLine = line.trim();
          
          if (trimmedLine.startsWith('event:')) {
            currentEvent = trimmedLine.substring(6).trim();
          } else if (trimmedLine.startsWith('data:')) {
            const currentData = trimmedLine.substring(5).trim();
            
            if (currentEvent === 'message' && currentData) {
              if (currentData === '[DONE]') {
                const finalIndex = messages.value.findIndex(msg => msg.id === botMessageId);
                if (finalIndex !== -1) {
                  messages.value[finalIndex].content = stripLegacyMarkers(accumulatedContent).trim();
                }
                currentStreamingMessageId.value = null;
                return;
              } else {
                accumulatedContent += currentData;
                const displayContent = stripLegacyMarkers(accumulatedContent);
                const index = messages.value.findIndex(msg => msg.id === botMessageId);
                if (index !== -1) {
                  messages.value[index].type = 'bot';
                  messages.value[index].content = displayContent;
                  scrollToBottom();
                }
              }
            } else if (currentEvent === 'complete' && currentData === '[DONE]') {
              const finalIndex = messages.value.findIndex(msg => msg.id === botMessageId);
              if (finalIndex !== -1) {
                messages.value[finalIndex].content = stripLegacyMarkers(accumulatedContent).trim();
              }
              currentStreamingMessageId.value = null;
              return;
            } else if (!currentEvent && currentData) {
              if (currentData !== '[DONE]') {
                accumulatedContent += currentData;
                const displayContent = stripLegacyMarkers(accumulatedContent);
                const index = messages.value.findIndex(msg => msg.id === botMessageId);
                if (index !== -1) {
                  messages.value[index].type = 'bot';
                  messages.value[index].content = displayContent;
                  scrollToBottom();
                }
              }
            }
          }
        }
      }
    } finally {
      reader.releaseLock();
    }
  } catch (error) {
    console.error('流式请求失败:', error);
    const errorIndex = messages.value.findIndex(msg => msg.id === botMessageId);
    if (errorIndex !== -1) {
      messages.value[errorIndex].content = `请求失败: ${error.message}`;
    }
    currentStreamingMessageId.value = null;
    ElMessage.error('流式连接出错: ' + error.message);
  }
};

// 处理测试按钮点击
const handleTestButtonClick = (testType, testQuestions) => {
  if (userStore.isTestCompleted(testType)) {
    ElMessage.warning('该测试已经完成，无法重复提交');
    return;
  }
  if (!testQuestions || typeof testQuestions !== 'object' || !Array.isArray(testQuestions.questions) || testQuestions.questions.length === 0) {
    ElMessage.error('未找到结构化测试题目，请重新触发测试');
    return;
  }
  userStore.setPendingTestPayload(testType, testQuestions);
  router.push({ path: `/test/${testType}` });
};

// 清空聊天记录
const clearChat = () => {
  messages.value = [];
  currentStreamingMessageId.value = null;
  if (!sessionId.value) {
    collectSessionId.value = null;
    currentQuestion.value = '';
    currentQuestionKey.value = '';
    presetOptions.value = [];
    otherInputVisible.value = false;
    otherInputValue.value = '';
    chatPresetOptions.value = [];
    hasStarted.value = false;
    selectedVersion.value = '';
    resetCollectPanelLocals();
    clearCollectedAnswers();
  } else {
    chatPresetOptions.value = [];
    userStore.clearChatMessages();
  }
  ElMessage.info('已清空对话');
};

// 处理键盘回车
const handleKeyPress = (event) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault();
    if (!inputDisabled.value) {
      sendMessage();
    }
  }
};

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
    }
  });
};

// 显示新消息的方法
const showNewMessage = (message) => {
  if (message) {
    addMessage('bot', message);
    scrollToBottom();
  }
};

const handleReportButtonClick = () => {
  // 检查报告是否已查看
  if (userStore.isTestCompleted('report')) {
    ElMessage.warning('报告已查看，无法重复打开');
    return;
  }
  // 标记报告为已查看
  userStore.markTestAsCompleted('report');
  router.push('/report/career');
};

// 页面加载时初始化
onMounted(() => {
  getFullVersionTemporarilyClosed().then((closed) => {
    fullVersionTemporarilyClosed.value = closed;
  });
  // 检查是否有 sessionId
  if (sessionId.value) {
    hasInitialized.value = true;
    hasStarted.value = true;

    const restored = loadWorkflowState(sessionId.value);
    applyWorkflowToRefs(restored, { workflowPhase, suggestStartSkill, skillQuestionsPrepared });

    const storedMessages = userStore.getChatMessages();
    if (storedMessages && storedMessages.length > 0) {
      messages.value = storedMessages;
    }

    const storedMessage = userStore.getInitialMessage();
    if (storedMessage) {
      initialMessage.value = storedMessage;
      if (messages.value.length === 0) {
        showNewMessage(storedMessage);
      }
      userStore.clearInitialMessage();
    }

    startSkillPrepPollIfNeeded();
    nextTick(() => {
      syncSkillStartChatPrompt();
      syncCareerTestChatPrompt();
      syncPostCareerChatPrompt();
      syncReportReadyPrompt();
    });

    const pendingAfterCareer = userStore.getPendingCareerTest();
    if (pendingAfterCareer?.careerAssessmentReport) {
      userStore.setCareerAssessmentReport(pendingAfterCareer.careerAssessmentReport);
      userStore.clearPendingCareerTest();
      nextTick(() => syncReportReadyPrompt());
    }
  } else {
    // 无 sessionId：先展示启动卡片，点击「开始匹配」后再进入对话式收集
    hasStarted.value = false;
  }
  
  scrollToBottom();
});

onActivated(() => {
  if (!sessionId.value) {
    return;
  }
  nextTick(() => {
    syncSkillStartChatPrompt();
    syncCareerTestChatPrompt();
    syncPostCareerChatPrompt();
    syncReportReadyPrompt();
  });
});

// 组件卸载前保存聊天记录
onBeforeUnmount(() => {
  stopSkillPrepPoll();
  if (sessionId.value && messages.value.length > 0) {
    userStore.saveChatMessages(messages.value);
  }
});

// 监听 store 中的聊天记录变化（用于在其它地方清理聊天记录时同步到当前页面）
watch(
  () => userStore.getChatMessages(),
  (newMessages) => {
    messages.value = newMessages || [];
  }
);

// 监听 store 中的 initialMessage 变化（用于编辑模式下显示更新后的消息）
// 使用 immediate: false 避免在组件挂载时立即触发
watch(
  () => userStore.initialMessage,
  (newMessage, oldMessage) => {
    // 只有在已初始化、有新消息、且消息发生变化时才显示
    if (newMessage && hasInitialized.value && newMessage !== oldMessage) {
      showNewMessage(newMessage);
      // 清除已显示的消息
      userStore.clearInitialMessage();
    }
  },
  { immediate: false }
);
</script>

<style scoped>
.progress-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
  background: linear-gradient(90deg, var(--primary-subtle) 0%, var(--accent-gold-light) 40%, var(--accent-green-light) 100%);
  border-bottom: 1px solid var(--border-light);
}

.progress-track {
  flex: 1;
  height: 8px;
  background: var(--border-light);
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--primary) 0%, var(--accent-gold) 50%, var(--accent-green) 100%);
  border-radius: 4px;
  transition: width 0.4s ease;
  position: relative;
  overflow: hidden;
}

.progress-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.4), transparent);
  animation: progressShine 1.5s ease-in-out infinite;
}

@keyframes progressShine {
  0% { left: -100%; }
  100% { left: 100%; }
}

.progress-text {
  font-size: 12px;
  font-weight: 600;
  color: var(--primary);
}

.stage-bar {
  display: flex;
  gap: 8px;
  padding: 12px 20px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-light);
  overflow-x: auto;
}

.stage-item {
  padding: 8px 14px;
  border-radius: var(--radius-md);
  font-size: 12px;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
  transition: all 0.25s ease;
  position: relative;
}

.stage-item::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: var(--radius-md);
  background: var(--primary-subtle);
  opacity: 0;
  transition: opacity 0.25s ease;
}

.stage-item:hover::before {
  opacity: 1;
}

.stage-item.active {
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(232, 106, 88, 0.35);
}

.stage-item.active::before {
  display: none;
}

.stage-item.completed {
  color: var(--accent-green);
}

.stage-n {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--border);
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.stage-item.active .stage-n {
  background: rgba(255,255,255,0.25);
  color: white;
}

.stage-item.completed .stage-n {
  background: var(--accent-green-light);
  color: var(--accent-green);
}

.stage-hint {
  margin: 0;
  padding: 0 20px 10px;
  font-size: 12px;
  color: var(--text-muted);
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-light);
}

.stage-item--report {
  cursor: pointer;
  color: var(--accent-purple);
  background: var(--accent-purple-light);
}

.stage-item--report:hover::before {
  opacity: 0;
}

.stage-item--report:hover {
  background: rgba(139, 123, 168, 0.18);
  box-shadow: 0 2px 8px rgba(139, 123, 168, 0.25);
}

.stage-item--report .stage-n {
  background: rgba(139, 123, 168, 0.2);
  color: var(--accent-purple);
}

.stage-item--report-active {
  background: linear-gradient(135deg, var(--accent-purple) 0%, #6d5f8a 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(139, 123, 168, 0.4);
}

.stage-item--report-active .stage-n {
  background: rgba(255, 255, 255, 0.25);
  color: white;
}

.stage-item--report-loading {
  opacity: 0.75;
  pointer-events: none;
}

.main-layout {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 20px;
  height: calc(100vh - 140px);
  min-height: 540px;
  padding: 0;
}

.chat-panel {
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow:
    var(--shadow-md),
    0 0 0 1px rgba(232, 106, 88, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  position: relative;
}

.chat-panel::before {
  content: '';
  position: absolute;
  top: 0;
  left: 20px;
  right: 20px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(232, 106, 88, 0.15), transparent);
}

.test-panel {
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow:
    var(--shadow-md),
    0 0 0 1px rgba(232, 106, 88, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  position: relative;
}

.test-panel::before {
  content: '';
  position: absolute;
  top: 0;
  left: 20px;
  right: 20px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(232, 106, 88, 0.15), transparent);
}

.panel-header {
  padding: 14px 18px;
  border-bottom: 1px solid var(--border-light);
  background: linear-gradient(180deg, #FFFFFF 0%, #FAFBFC 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-title svg {
  width: 16px;
  height: 16px;
  color: var(--primary);
}

.panel-title .fox-icon {
  width: 18px;
  height: 18px;
}

.panel-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.panel-status-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.resume-bottom-upload {
  /* keep same shell rhythm as left .control-panel */
}
.settings {
  padding-top: 8px;
  border-top: 1px solid var(--border-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.resume-bottom-upload-row {
  margin-bottom: 12px;
}

.resume-bottom-upload-inputlike {
  flex: 1;
  min-width: 0;
  height: 40px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-input);
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
}

.resume-bottom-upload-main {
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.resume-bottom-upload-minor {
  font-size: 11px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resume-bottom-upload-btn {
  height: 40px;
  border: none;
  border-radius: var(--radius-md);
  padding: 0 14px;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  box-shadow: var(--shadow-md);
}

.resume-bottom-upload-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.resume-bottom-upload-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: var(--accent-green);
  border-radius: 50%;
  box-shadow: 0 0 12px var(--accent-green-light);
  animation: statusPulse 2s ease-in-out infinite;
}

@keyframes statusPulse {
  0%, 100% { box-shadow: 0 0 8px var(--accent-green-light); }
  50% { box-shadow: 0 0 16px var(--accent-green-light); }
}

.test-content {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
  background: var(--bg-page);
}

.test-empty {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 13px;
}

.test-empty svg {
  width: 56px;
  height: 56px;
  margin-bottom: 16px;
  opacity: 0.3;
}

.version-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.version-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 20px;
}

.version-cards {
  display: flex;
  gap: 18px;
  justify-content: center;
  align-items: stretch;
  padding: 18px 22px;
  border-radius: 22px;
  background:
    radial-gradient(circle at 0% 0%, rgba(244, 137, 122, 0.12) 0, transparent 55%),
    radial-gradient(circle at 100% 100%, rgba(255, 194, 148, 0.16) 0, transparent 55%),
    #fff;
  box-shadow:
    0 18px 45px rgba(244, 137, 122, 0.14),
    0 0 0 1px rgba(255, 255, 255, 0.9);
}

.version-card {
  flex: 1;
  min-width: 150px;
  max-width: 190px;
  background: #fff;
  border: 2px solid var(--border);
  border-radius: 18px;
  padding: 18px 14px 16px;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
}

.version-card:hover {
  border-color: var(--primary);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.version-card.selected {
  border-color: #f56c6c;
  background: linear-gradient(145deg, rgba(245, 146, 131, 0.1), rgba(255, 245, 242, 0.95));
  box-shadow:
    0 0 0 1px rgba(245, 146, 131, 0.12),
    0 16px 32px rgba(245, 146, 131, 0.35);
}

.version-icon {
  width: 52px;
  height: 52px;
  margin: 0 auto 10px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.version-icon :deep(.el-icon) {
  font-size: 26px;
}

.version-card.trial .version-icon {
  background: linear-gradient(145deg, #ffe6e1, #ffd4c9);
  color: #e56b5e;
}

.version-card.full .version-icon {
  background: linear-gradient(145deg, #fff7e5, #f9edd5);
  color: #e0a74b;
}

.version-card.is-unavailable {
  cursor: not-allowed;
  opacity: 0.72;
}

.version-card.is-unavailable:hover {
  border-color: var(--border);
  transform: none;
  box-shadow: none;
}

.version-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.version-desc {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
  margin-bottom: 10px;
}

.version-features {
  text-align: left;
  font-size: 11px;
  color: var(--text-muted);
  padding-left: 14px;
}

.version-features li {
  margin-bottom: 4px;
}

.version-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: linear-gradient(135deg, var(--accent-gold), #e6b54a);
  color: white;
  font-size: 10px;
  padding: 2px 7px;
  border-radius: 10px;
  font-weight: 600;
}

.preset-grid {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  padding: 8px 0;
}

.preset-other {
  display: flex;
  gap: 10px;
  align-items: center;
}

.preset-other-input {
  flex: 1;
  background: var(--bg-input);
  padding: 12px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: inherit;
  color: var(--text-primary);
  outline: none;
  transition: all 0.2s;
}

.preset-other-input:focus {
  border-color: var(--primary);
}

.preset-other-input:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.preset-other-send {
  padding: 12px 16px;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  color: white;
  white-space: nowrap;
  box-shadow: var(--shadow-md);
  transition: all 0.25s ease;
}

.preset-other-send:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: var(--shadow-lg), 0 6px 20px var(--primary-glow);
}

.preset-other-send:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.preset-grid .preset-chip {
  width: 100%;
  display: block;
  text-align: center;
}

.chat-container {
  height: 100%;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 8px 24px;
  box-sizing: border-box;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
  background: var(--bg-page);
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
}

.start-screen {
  flex: 1;
  min-height: 360px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 80px 24px;
  position: relative;
}

.start-screen::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, var(--primary-subtle) 0%, transparent 70%);
  border-radius: 50%;
  z-index: 0;
}

.start-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(145deg, var(--primary-subtle) 0%, var(--accent-gold-light) 50%, var(--accent-green-light) 100%);
  border: 3px solid var(--primary);
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  animation: pulse 2s ease-in-out infinite;
  position: relative;
  z-index: 1;
}

.start-icon svg {
  width: 36px;
  height: 36px;
  color: var(--primary);
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 var(--primary-glow); transform: scale(1); }
  50% { box-shadow: 0 0 30px 10px var(--primary-glow); transform: scale(1.05); }
}

.start-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 12px;
  color: var(--text-primary);
  position: relative;
  z-index: 1;
}

.start-desc {
  font-size: 14px;
  color: var(--text-secondary);
  max-width: 360px;
  margin: 0 auto 28px;
  line-height: 1.7;
  position: relative;
  z-index: 1;
}

.landing-version-wrapper {
  position: relative;
  z-index: 1;
  margin-bottom: 24px;
  width: 100%;
  max-width: 520px;
}

.start-terms {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 520px;
  margin-top: 20px;
  padding: 0;
  background: none;
  border: none;
  box-shadow: none;
  text-align: center;
}

.start-terms-row {
  display: inline-flex;
  align-items: flex-start;
  gap: 10px;
  cursor: pointer;
  user-select: none;
  justify-content: center;
}

.start-terms-checkbox {
  width: 16px;
  height: 16px;
  margin-top: 3px;
  accent-color: var(--primary);
  flex-shrink: 0;
  cursor: pointer;
}

.start-terms-text {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-primary);
}

.start-terms-link {
  border: none;
  padding: 0;
  background: none;
  color: #2563eb;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
  transition: color 0.2s ease;
}

.start-terms-link:hover {
  color: #1d4ed8;
  text-decoration: underline;
}

.start-terms-hint {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-muted);
}

.start-btn {
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  border: none;
  padding: 16px 42px;
  border-radius: var(--radius-lg);
  color: white;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  z-index: 1;
  box-shadow: var(--shadow-md);
}

.start-btn:hover {
  transform: translateY(-3px) scale(1.02);
  box-shadow: var(--shadow-xl), 0 12px 30px var(--primary-glow);
}

.start-btn:disabled,
.start-btn.is-disabled {
  cursor: not-allowed;
  opacity: 0.55;
  transform: none;
  box-shadow: var(--shadow-sm);
}

.start-btn:disabled:hover,
.start-btn.is-disabled:hover {
  transform: none;
  box-shadow: var(--shadow-sm);
}

.pending-confirm-screen .start-icon {
  border-color: var(--accent-purple);
  background: linear-gradient(145deg, var(--accent-purple-light) 0%, var(--primary-subtle) 100%);
}

.pending-confirm-screen .start-icon svg {
  color: var(--accent-purple);
}

.empty-tips {
  text-align: center;
  color: var(--text-muted);
  padding: 40px 20px;
  font-size: 14px;
}

.initial-message {
  margin-top: 16px;
  padding: 16px;
  background: var(--primary-subtle);
  border-radius: var(--radius-md);
  color: var(--primary);
  font-size: 14px;
  line-height: 1.6;
}

.message {
  display: flex;
  gap: 12px;
  margin-bottom: 4px;
  animation: fadeIn 0.3s ease-out;
}

.message.user {
  flex-direction: row-reverse;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.msg-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 600;
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.msg-avatar-text {
  line-height: 1;
}

.msg-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.msg-avatar.ai {
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  color: #fff;
  padding: 0;
}

.msg-avatar.user {
  background: linear-gradient(135deg, var(--bg-panel) 0%, var(--border) 100%);
  border: 2px solid var(--border);
}

.msg-content {
  max-width: 85%;
}

.message.user .msg-content {
  text-align: right;
}

.msg-bubble {
  background: var(--bg-card);
  padding: 10px 14px;
  border-radius: var(--radius-md);
  border-bottom-left-radius: var(--radius-sm);
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-primary);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
}

.message.user .msg-bubble {
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  color: #fff;
  border: none;
  border-bottom-left-radius: var(--radius-lg);
  border-bottom-right-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
}

.msg-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
  font-size: 12px;
}

.msg-title {
  font-weight: 600;
}

.msg-time {
  color: var(--text-secondary);
  font-size: 11px;
}

.msg-text {
  white-space: pre-wrap;
  font-size: 14px;
  margin-top: 4px;
}

.test-button-wrapper {
  width: 100%;
  display: flex;
  justify-content: center;
  padding: 16px 0;
}

.test-button {
  font-size: 16px;
  padding: 12px 32px;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  transition: all 0.3s;
}

.test-button:hover:not(.test-button-disabled):not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg), 0 6px 20px var(--primary-glow);
}

.test-button-disabled,
.test-button:disabled {
  background: var(--text-muted) !important;
  border-color: var(--border) !important;
  color: var(--bg-card) !important;
  cursor: not-allowed !important;
  opacity: 0.8;
  box-shadow: none !important;
}

.test-button-disabled:hover,
.test-button:disabled:hover {
  transform: none !important;
  box-shadow: none !important;
}

.control-panel {
  padding: 14px 18px;
  background: var(--bg-card);
  border-top: 1px solid var(--border-light);
  flex-shrink: 0;
}

.preset-chip {
  padding: 12px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  background: var(--bg-input);
  color: var(--text-primary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.preset-chip:hover:not(:disabled) {
  border-color: var(--primary);
  background: var(--primary-subtle);
  color: var(--primary);
}

.preset-chip:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.input-area {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.message-input {
  flex: 1;
  background: var(--bg-input);
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-family: inherit;
  color: var(--text-primary);
  outline: none;
  transition: all 0.2s;
}

.message-input:focus {
  border-color: var(--primary);
}

.message-input:disabled {
  background: var(--border-light);
  cursor: not-allowed;
}

.send-button,
.clear-button {
  padding: 10px 16px;
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
}

.send-button {
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  color: white;
  min-width: 44px;
  box-shadow: var(--shadow-md);
}

.send-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg), 0 6px 20px var(--primary-glow);
}

.send-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.clear-button {
  background: var(--rose);
  color: white;
}

.clear-button:hover {
  background: var(--error);
  box-shadow: var(--shadow-md);
}

/* 设置区域 */
.settings {
  padding-top: 8px;
  border-top: 1px solid var(--border-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.streaming-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 13px;
  color: var(--text-secondary);
}

.streaming-toggle input[type="checkbox"] {
  margin: 0;
  cursor: pointer;
}

.warning-text {
  font-size: 12px;
  color: var(--error);
}

.hint-text {
  font-size: 12px;
  color: var(--text-secondary);
}

/* —— 打字中点点动画（替代旧的 ▌ 光标）—— */
.typing-dots {
  display: flex;
  align-items: center;
  gap: 6px;
}

.typing-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  animation: typingDot 1.2s infinite;
  opacity: 0.55;
}

.typing-dot.dot-1 {
  background: var(--primary);
}
.typing-dot.dot-2 {
  background: var(--accent-gold);
  animation-delay: 0.2s;
}
.typing-dot.dot-3 {
  background: var(--accent-green);
  animation-delay: 0.4s;
}
.typing-dot.dot-4 {
  background: var(--accent-purple);
  animation-delay: 0.6s;
}

@keyframes typingDot {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  30% {
    transform: translateY(-5px);
    opacity: 1;
  }
}

/* —— 原型风格：欢迎 / 水晶球 / 岗位与城市 / 金币（收集流程右栏）—— */
.test-section {
  text-align: left;
}
.test-label {
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}
.test-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
  line-height: 1.4;
}
.welcome-scene-panel .welcome-hint {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 16px;
  line-height: 1.6;
}
.cont-btn {
  width: 100%;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  border: none;
  padding: 14px;
  border-radius: var(--radius-md);
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  margin-top: 8px;
  transition: all 0.2s;
}
.cont-btn:hover:not(:disabled) {
  box-shadow: var(--shadow-lg), 0 6px 20px var(--primary-glow);
  transform: translateY(-2px);
}
.cont-btn.small {
  padding: 8px 14px;
  font-size: 12px;
  margin-top: 10px;
}
.opt-btn {
  width: 100%;
  background:
    radial-gradient(circle at 0% 0%, rgba(244, 137, 122, 0.1) 0, transparent 60%),
    radial-gradient(circle at 100% 100%, rgba(139, 123, 168, 0.1) 0, transparent 60%),
    var(--bg-card);
  border: 1px solid rgba(255, 255, 255, 0.9);
  padding: 12px 14px;
  border-radius: 999px;
  font-size: 13px;
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.15s;
  text-align: left;
  font-family: inherit;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.06);
}
.opt-btn:hover:not(:disabled) {
  border-color: var(--primary);
  transform: translateY(-2px);
}
.crystal-section .crystal-ball {
  width: 160px;
  height: 160px;
  margin: 0 auto 14px;
  position: relative;
  border-radius: 50%;
  background:
    radial-gradient(circle at 30% 20%, rgba(255, 255, 255, 0.9) 0, transparent 55%),
    radial-gradient(circle at 70% 80%, rgba(244, 137, 122, 0.45) 0, transparent 55%),
    radial-gradient(circle at 20% 80%, rgba(139, 123, 168, 0.45) 0, transparent 60%),
    radial-gradient(circle at 80% 20%, rgba(107, 158, 120, 0.5) 0, transparent 60%);
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.16);
  border: 3px solid rgba(255, 255, 255, 0.9);
}
.crystal-glow {
  position: absolute;
  inset: 10px;
  border-radius: 50%;
  box-shadow: 0 0 35px rgba(244, 137, 122, 0.55);
  opacity: 0.45;
  pointer-events: none;
}
.nickname-display {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
  color: var(--primary-dark);
  text-shadow: 0 2px 6px rgba(255, 255, 255, 0.8);
  padding: 0 12px;
  text-align: center;
  word-break: break-all;
}
.bubble-keyboard {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
  margin-bottom: 10px;
}
.bubble-key {
  min-width: 28px;
  height: 28px;
  border-radius: 999px;
  border: none;
  font-size: 11px;
  cursor: pointer;
  color: var(--text-primary);
  background:
    radial-gradient(circle at 30% 20%, #fff 0, #fff 40%, transparent 60%),
    radial-gradient(circle at 70% 80%, rgba(244, 137, 122, 0.35) 0, rgba(244, 137, 122, 0.05) 55%);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  transition: transform 0.1s ease, box-shadow 0.1s ease;
}
.bubble-key:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.16);
}
.bubble-key.special {
  padding: 0 10px;
  font-size: 10px;
  background: linear-gradient(135deg, rgba(232, 106, 88, 0.08), rgba(139, 123, 168, 0.12));
}
.nickname-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 6px;
}
.magic-books {
  background: radial-gradient(circle at 0% 0%, rgba(244, 137, 122, 0.12) 0, transparent 55%),
    radial-gradient(circle at 100% 100%, rgba(139, 123, 168, 0.1) 0, transparent 55%),
    var(--bg-panel);
  border-radius: 16px;
  padding: 14px;
  box-shadow: var(--shadow-md);
}
.opt-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}
.opt-btn.work-years-btn.selected,
.opt-btn.job-btn.selected,
.opt-btn.city-btn.selected {
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  border-color: var(--primary);
  color: white;
}
.book-detail {
  padding: 12px;
  border-radius: 10px;
  background: var(--bg-panel);
  font-size: 13px;
  color: var(--text-secondary);
}
.book-open-title {
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--primary-dark);
}
.book-open-desc {
  margin-bottom: 8px;
  line-height: 1.5;
}
.skill-in {
  width: 100%;
  background: var(--bg-input);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  font-size: 13px;
  color: var(--text-primary);
  box-sizing: border-box;
}
.skill-in:focus {
  outline: none;
  border-color: var(--primary);
}
.gold-display {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 10px;
}
.gold-pile {
  width: 72px;
  height: 54px;
  border-radius: 14px;
  flex-shrink: 0;
  background:
    radial-gradient(circle at 20% 10%, #fff7e3 0, transparent 55%),
    radial-gradient(circle at 80% 15%, #fff2c7 0, transparent 55%);
  position: relative;
  overflow: hidden;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.18);
  transition: transform 0.2s ease;
}
.gold-pile[data-level='1'] { transform: scale(0.86); opacity: 0.85; }
.gold-pile[data-level='2'] { transform: scale(0.95); }
.gold-pile[data-level='3'] { transform: scale(1); }
.gold-pile[data-level='4'] { transform: scale(1.07); }
.gold-pile[data-level='5'] { transform: scale(1.15); }
.life-scene {
  flex: 1;
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--bg-panel);
  border-radius: 10px;
  padding: 8px 10px;
  line-height: 1.5;
}
.salary-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--text-muted);
}
.salary-range {
  flex: 1;
}
.salary-value {
  font-size: 13px;
  color: var(--primary-dark);
  margin-bottom: 10px;
  font-weight: 600;
}
.salary-actions {
  margin-bottom: 10px;
}

.resume-upload-input {
  display: none;
}

@media (max-width: 768px) {
  .panel-status-wrap {
    gap: 6px;
  }
  .resume-bottom-upload {
    padding: 14px 12px;
  }
  .resume-bottom-upload-row {
    margin-bottom: 10px;
  }
  .resume-bottom-upload-inputlike {
    height: 38px;
    padding: 0 10px;
  }
  .resume-bottom-upload-main {
    font-size: 11px;
  }
  .resume-bottom-upload-minor {
    display: none;
  }
  .resume-bottom-upload-btn {
    height: 38px;
    font-size: 11px;
    padding: 0 10px;
  }
  .msg-content {
    max-width: 85%;
  }
  .input-area {
    flex-wrap: wrap;
  }
  .send-button, .clear-button {
    padding: 10px 12px;
    font-size: 13px;
  }
}

@media (max-width: 900px) {
  .main-layout {
    grid-template-columns: 1fr;
    height: auto;
  }
  .chat-panel {
    height: 420px;
  }
  .test-panel {
    min-height: 300px;
  }
}
</style>