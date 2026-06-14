<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { ElMessageBox } from 'element-plus';

const userStore = useUserStore();
const router = useRouter();
const restartBtnRef = ref(null);
const infoOrbVisible = ref(false);
const infoButtonVisible = ref(false);
const orbAnimating = ref(false);
const orbStyle = ref({});
const trailParticles = ref([]);
let trailRafId = null;
let particleIdSeed = 0;
const ORB_SIZE = 28;
const ORB_DURATION_MS = 1050;
const TRAIL_LIFETIME_MS = 1000;
const onResize = () => {
  if (shouldShowInfoButton.value && !orbAnimating.value) {
    infoButtonVisible.value = true;
  }
};

const onRestartClick = async () => {
  try {
    await ElMessageBox.confirm(
      '将清除本页保存的对话、测评与登录状态，并回到欢迎页。是否继续？',
      '重新开始',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
        customClass: 'restart-confirm-dialog',
        confirmButtonClass: 'restart-confirm-btn',
        cancelButtonClass: 'restart-cancel-btn'
      }
    );
  } catch {
    return;
  }
  await userStore.performFullLocalReset();
};

const hasPendingCollectConfirm = computed(() => userStore.hasPendingCollectConfirm());
const shouldShowInfoButton = computed(() => hasPendingCollectConfirm.value && !userStore.getSessionId());

const openConfirm = () => {
  if (!shouldShowInfoButton.value) return;
  router.push('/confirm');
};

const runInfoOrbAnimation = async () => {
  if (!shouldShowInfoButton.value || !restartBtnRef.value) return;
  if (trailRafId) {
    cancelAnimationFrame(trailRafId);
    trailRafId = null;
  }
  const targetRect = restartBtnRef.value.getBoundingClientRect();
  const targetX = targetRect.left - 56 + ORB_SIZE / 2;
  const targetY = targetRect.top + targetRect.height / 2;
  const startX = window.innerWidth * 0.5;
  const startY = window.innerHeight * 0.56;
  const deltaX = targetX - startX;
  const deltaY = targetY - startY;
  const arcLift = Math.min(120, Math.max(45, Math.abs(deltaX) * 0.2));
  const easeOutCubic = (t) => 1 - Math.pow(1 - t, 3);
  const getOrbPoint = (p) => {
    const e = easeOutCubic(p);
    const x = startX + deltaX * e;
    const y = startY + deltaY * e + (-arcLift) * (4 * e * (1 - e));
    return { x, y };
  };
  const setOrbStyleFromCenter = (x, y) => {
    orbStyle.value = {
      left: `${(x - ORB_SIZE / 2).toFixed(2)}px`,
      top: `${(y - ORB_SIZE / 2).toFixed(2)}px`
    };
  };
  infoButtonVisible.value = false;
  infoOrbVisible.value = true;
  orbAnimating.value = true;
  trailParticles.value = [];

  let prevTs = 0;
  const startTs = performance.now();
  const tick = (ts) => {
    const dt = prevTs ? (ts - prevTs) : 16;
    prevTs = ts;
    const elapsed = ts - startTs;
    const p = Math.min(1, elapsed / ORB_DURATION_MS);
    const point = getOrbPoint(p);
    setOrbStyleFromCenter(point.x, point.y);

    // 记录历史轨迹粒子：新粒子亮且大，旧粒子随生命值线性衰减
    trailParticles.value.push({
      id: ++particleIdSeed,
      x: point.x,
      y: point.y,
      life: 0.5
    });
    const lifeDrop = dt / TRAIL_LIFETIME_MS;
    trailParticles.value = trailParticles.value
      .map((particle) => ({ ...particle, life: particle.life - lifeDrop }))
      .filter((particle) => particle.life > 0);

    if (p >= 1) {
      infoOrbVisible.value = false;
      orbAnimating.value = false;
      infoButtonVisible.value = true;
      trailParticles.value = [];
      trailRafId = null;
      return;
    }
    trailRafId = requestAnimationFrame(tick);
  };
  trailRafId = requestAnimationFrame(tick);
};

watch(shouldShowInfoButton, async (val, oldVal) => {
  if (val && !oldVal) {
    await nextTick();
    runInfoOrbAnimation();
  } else if (!val) {
    infoOrbVisible.value = false;
    infoButtonVisible.value = false;
    trailParticles.value = [];
  }
}, { immediate: true });

onMounted(() => {
  window.addEventListener('resize', onResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize);
  if (trailRafId) cancelAnimationFrame(trailRafId);
});
</script>

<template>
  <div class="page-shell">
    <!-- 全局浮动装饰，与 ui-style-example 一致 -->
    <div class="floating-shapes">
      <div class="float-shape"></div>
      <div class="float-shape"></div>
      <div class="float-shape"></div>
      <div class="float-shape"></div>
    </div>

    <div class="common-layout">
      <el-container>
        <el-header>
          <div class="header-inner">
            <div class="header-content">
              <div class="header-logo">
                <div class="logo-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10" />
                    <path d="M12 6v6l4 2" />
                  </svg>
                </div>
                <div class="logo-text-wrap">
                  <div class="logo-text">职业潜能评估</div>
                  <div class="logo-subtitle">AI CAREER ASSESSMENT</div>
                </div>
              </div>
              <div class="header-actions">
                <button
                  v-if="infoButtonVisible && shouldShowInfoButton"
                  type="button"
                  class="header-info-btn"
                  @click="openConfirm"
                  title="个人信息确认"
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="8" r="4" />
                    <path d="M5.5 20a6.5 6.5 0 0 1 13 0" />
                  </svg>
                </button>
                <div
                  v-if="infoOrbVisible && shouldShowInfoButton"
                  class="info-orb"
                  :style="orbStyle"
                  @click="openConfirm"
                >
                </div>
                <span
                  v-for="p in trailParticles"
                  :key="p.id"
                  class="orb-trail-particle"
                  :style="{
                    left: `${(p.x).toFixed(2)}px`,
                    top: `${(p.y).toFixed(2)}px`,
                    width: `${(4 + 15 * p.life).toFixed(2)}px`,
                    height: `${(4 + 15 * p.life).toFixed(2)}px`,
                    opacity: (0.82 * p.life).toFixed(3)
                  }"
                ></span>
                <button ref="restartBtnRef" type="button" class="header-restart-btn" @click="onRestartClick">重新开始</button>
              </div>
            </div>
          </div>
        </el-header>
        <el-container>
          <!-- <el-aside width="200px">历史记录</el-aside> -->
          <el-main>
            <RouterView />
          </el-main>
        </el-container>
      </el-container>
    </div>
  </div>
</template>

<style scoped>
.page-shell {
  height: 100%;
  width: 100%;
  overflow: hidden;
  position: relative;
}

.common-layout {
  height: 100%;
  width: 100%;
  overflow: hidden;
}

.el-header {
  height: 60px;
  width: 100%;
  padding: 0;
  background: transparent;
  border-bottom: none;
  box-shadow: none;
  flex-shrink: 0;
}

.header-inner {
  width: 100%;
  height: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

/* Logo icon with gradient */
.header-logo {
  display: flex;
  align-items: center;
  gap: 14px;
}

.logo-text-wrap {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
}

.logo-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(145deg, var(--primary-light) 0%, var(--primary) 50%, var(--primary-dark) 100%);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 20px rgba(232, 106, 88, 0.35);
  position: relative;
  overflow: hidden;
}

.logo-icon::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(45deg, transparent 40%, rgba(255,255,255,0.3) 50%, transparent 60%);
  animation: logoShine 3s ease-in-out infinite;
}

@keyframes logoShine {
  0%, 100% { transform: translateX(-100%) rotate(45deg); }
  50% { transform: translateX(100%) rotate(45deg); }
}

.logo-icon svg {
  width: 22px;
  height: 22px;
  color: white;
  position: relative;
  z-index: 1;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.3px;
  line-height: 1.25;
}

.logo-subtitle {
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 0.5px;
  text-transform: uppercase;
  line-height: 1.2;
}

.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  position: relative;
}

.header-info-btn {
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  cursor: pointer;
  background: linear-gradient(135deg, var(--accent-purple) 0%, var(--primary-dark) 100%);
  box-shadow: 0 8px 20px rgba(139, 123, 168, 0.35);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.header-info-btn:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-lg), 0 12px 24px rgba(139, 123, 168, 0.35);
}

.header-info-btn svg {
  width: 20px;
  height: 20px;
}

.info-orb {
  position: fixed;
  left: 0;
  top: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  z-index: 2000;
  background:
    radial-gradient(circle at 28% 26%, rgba(255, 255, 255, 0.9) 0, rgba(255, 255, 255, 0.42) 20%, transparent 42%),
    radial-gradient(circle at 72% 74%, rgba(232, 106, 88, 0.58) 0, rgba(139, 123, 168, 0.62) 58%, rgba(91, 124, 153, 0.52) 100%);
  backdrop-filter: blur(2px);
  border: 1px solid rgba(255, 255, 255, 0.45);
  box-shadow: 0 0 16px rgba(232, 106, 88, 0.28), 0 0 30px rgba(139, 123, 168, 0.24);
  cursor: pointer;
  will-change: left, top;
}

.orb-trail-particle {
  position: fixed;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  background: radial-gradient(
    circle,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(232, 106, 88, 0.6) 45%,
    rgba(139, 123, 168, 0.36) 100%
  );
  box-shadow: 0 0 8px rgba(232, 106, 88, 0.22);
  pointer-events: none;
  z-index: 1999;
}

/* 与对话区「清空」一致：豆沙红底、白字、大圆角 */
.header-restart-btn {
  padding: 12px 18px;
  font-size: 13px;
  font-weight: 700;
  font-family: inherit;
  color: #fff;
  background: #c96f6f;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  transition: background 0.2s ease, box-shadow 0.2s ease, transform 0.15s ease;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(201, 111, 111, 0.35);
}

.header-restart-btn:hover {
  background: #b85f5f;
  box-shadow: 0 4px 12px rgba(201, 111, 111, 0.45);
}

.header-restart-btn:active {
  transform: translateY(1px);
  background: #a85555;
}

.header-actions .el-button--primary {
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%);
  border: none;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  transition: all 0.25s ease;
}

.header-actions .el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg), 0 6px 20px var(--primary-glow);
}

.header-actions .el-button--primary:active {
  transform: translateY(0);
}

.el-aside {
  background-color: var(--bg-panel);
  border-right: 1px solid var(--border);
  padding: 20px;
  box-sizing: border-box;
  overflow-y: auto;
}

.el-main {
  padding: 24px;
  background: transparent;
  /* min-height:0 让 flex 子项可收缩，子页面 height:100% + overflow 时滚动条稳定出现 */
  min-height: 0;
  overflow: auto;
  height: calc(100vh - 60px);
  width: 100%;
}

/* 确保 el-container 正确填充高度 */
.el-container {
  height: calc(100vh - 60px);
  width: 100%;
}

.el-container > .el-container {
  height: 100%;
  flex: 1;
  min-height: 0;
}

.el-container > .el-container {
  height: calc(100% - 60px);
  flex: 1;
  min-height: 0;
}

:global(.restart-confirm-dialog) {
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.72);
  box-shadow: var(--shadow-xl);
  background:
    radial-gradient(circle at 8% 0%, rgba(232, 106, 88, 0.1) 0, transparent 36%),
    radial-gradient(circle at 100% 100%, rgba(139, 123, 168, 0.1) 0, transparent 32%),
    rgba(255, 255, 255, 0.98);
  overflow: hidden;
}

:global(.restart-confirm-dialog .el-message-box__header) {
  padding: 18px 18px 8px;
}

:global(.restart-confirm-dialog .el-message-box__title) {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

:global(.restart-confirm-dialog .el-message-box__content) {
  padding: 8px 18px 12px;
}

:global(.restart-confirm-dialog .el-message-box__message) {
  font-size: 16px;
  line-height: 1.7;
  color: var(--text-secondary);
}

:global(.restart-confirm-dialog .el-message-box__status) {
  color: var(--primary);
}

:global(.restart-confirm-dialog .el-message-box__btns) {
  padding: 10px 18px 18px;
}

:global(.restart-confirm-btn) {
  border: none !important;
  border-radius: var(--radius-md) !important;
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-dark) 100%) !important;
  color: #fff !important;
  min-height: 42px;
  padding: 0 22px !important;
  font-size: 16px !important;
  font-weight: 700;
  box-shadow: var(--shadow-md);
}

:global(.restart-confirm-btn:hover) {
  transform: translateY(-1px);
  box-shadow: var(--shadow-lg), 0 8px 24px var(--primary-glow);
}

:global(.restart-cancel-btn) {
  min-height: 42px;
  padding: 0 22px !important;
  font-size: 16px !important;
  border-radius: var(--radius-md) !important;
  border-color: var(--border) !important;
  color: var(--text-primary) !important;
  background: var(--bg-input) !important;
}
</style>
