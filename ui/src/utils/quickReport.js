/**
 * 步骤5「报告输出」跳转：按当前流程阶段解析 report scope。
 * profile — 仅个人信息
 * resume — 个人信息 + 简历优化
 * skill — 技能测评
 * career — 技能 + 霍兰德
 * recommendation — 含初步/共识岗位推荐
 * full — 完整综合评价
 */

export function isEntryViaResume(userInfo) {
  return !!(userInfo?.entryViaResume || userInfo?.resumeFileName);
}

/** 共识完成后报告是否含真实岗位推荐 */
export function hasConsensusPositions(report) {
  const positions = report?.recommendedPositions;
  return (
    Array.isArray(positions) &&
    positions.length > 0 &&
    !String(positions[0]?.title || positions[0]?.['岗位名称'] || '').includes('暂时获取不到')
  );
}

/** 是否已进入「最终综合评价」阶段（仅对话区展示入口） */
export function isFinalReportStage(ctx) {
  const { workflowPhase, userStore } = ctx;
  const phase = workflowPhase || '';
  const report = userStore.getCareerAssessmentReport();
  if (!report) return false;
  return phase === 'REPORT_READY' || phase === 'CLOSED' || hasConsensusPositions(report);
}

/** 按当前阶段（1–5）决定报告 scope，用于步骤5点击跳转 */
export function resolveStageReportScope(currentStage, ctx) {
  const { userStore, workflowPhase } = ctx;
  const phase = workflowPhase || '';
  const skillDone = userStore.isTestCompleted('skill')
    || ['SKILL_DONE', 'CAREER_ACTIVE', 'CAREER_DONE', 'PRELIMINARY_REC', 'CONSENSUS_ACTIVE', 'REPORT_READY', 'CLOSED'].includes(phase);

  if (currentStage === 1 || (currentStage === 2 && phase === 'MAIN_CHAT' && !skillDone)) {
    const pending = userStore.getPendingConfirmForm?.() || ctx.pendingForm;
    if (isCollectResumeEntry(pending, userStore.getUserInfo())) {
      return 'resume';
    }
    return isEntryViaResume(userStore.getUserInfo()) ? 'resume' : 'profile';
  }
  switch (currentStage) {
    case 2:
      return 'skill';
    case 3:
      return 'holland';
    case 4:
      return 'recommendation';
    case 5:
      return 'full';
    default:
      return 'profile';
  }
}

/** API 失败时从 userInfo / pendingForm / 收集答案 组装最小报告对象 */
export function buildLocalProfileReport(source) {
  if (!source) return null;
  return {
    name: source.name || '-',
    targetJob: source.targetJob || '-',
    targetCity: source.expectedCity || source.targetCity || '-',
    expectedSalary: source.expectedSalary != null && source.expectedSalary !== ''
      ? source.expectedSalary
      : '-'
  };
}

const PROFILE_FIELD_KEYS = [
  'name',
  'workYears',
  'targetJob',
  'expectedCity',
  'expectedSalary',
  'skill',
  'experienceVersion',
  'resume'
];

/** 是否至少有一项个人信息（非空） */
export function hasAnyProfileValue(source) {
  if (!source) return false;
  return PROFILE_FIELD_KEYS.some((key) => {
    const v = source[key];
    return v != null && String(v).trim() !== '';
  });
}

/** 阶段1收集中：是否已有部分信息可展示 */
export function hasPartialCollectInfo(ctx) {
  const { collectedAnswers, pendingForm, draftName } = ctx;
  if (pendingForm?.resumeFileName) return true;
  if (pendingForm && hasAnyProfileValue(pendingForm)) return true;
  if (collectedAnswers && hasAnyProfileValue(collectedAnswers)) return true;
  if (draftName && String(draftName).trim()) return true;
  return false;
}

/** 从收集流程中的零散答案组装 profile 报告 */
export function buildProfileFromCollect(collectedAnswers, pendingForm, draftName) {
  const pending = pendingForm || {};
  const answers = collectedAnswers || {};
  return buildLocalProfileReport({
    name: pending.name || answers.name || draftName || '',
    targetJob: pending.targetJob || answers.targetJob || '',
    expectedCity: pending.expectedCity || answers.expectedCity || '',
    expectedSalary: pending.expectedSalary ?? answers.expectedSalary ?? '',
    workYears: pending.workYears || answers.workYears || ''
  });
}

/** 收集阶段是否为简历入口 */
export function isCollectResumeEntry(pendingForm, userInfo) {
  return !!(
    pendingForm?.resumeFileName ||
    userInfo?.entryViaResume ||
    userInfo?.resumeFileName
  );
}

const LABELS = {
  profile: '个人信息报告',
  resume: '简历优化报告',
  skill: '技能测评报告',
  holland: '霍兰德兴趣报告',
  career: '职业人格报告',
  recommendation: '岗位推荐报告',
  full: '综合评价报告'
};

const HINTS = {
  profile: '查看已填写的个人信息',
  resume: '查看简历优化建议',
  skill: '查看技能测评结果',
  holland: '查看六个维度兴趣分析',
  career: '查看霍兰德兴趣分析',
  recommendation: '查看初步岗位推荐',
  full: '查看完整评价与岗位推荐'
};

export function stageReportLabel(scope) {
  return LABELS[scope] || '查看报告';
}

export function stageReportHint(scope) {
  return HINTS[scope] || '查看当前阶段报告';
}
