const STORAGE_PREFIX = 'wf:phase:v1:';

/**
 * 从 API 响应提取 workflow 字段并持久化到 localStorage。
 * @returns {{ phase?: string, suggestStartSkill?: boolean, skillQuestionsPrepared?: boolean }}
 */
export function persistWorkflowFromResponse(sessionId, data) {
  if (!sessionId || !data) {
    return {};
  }
  const state = {};
  if (data.phase) {
    state.phase = data.phase;
  }
  if (typeof data.suggestStartSkill === 'boolean') {
    state.suggestStartSkill = data.suggestStartSkill;
  }
  if (typeof data.skillQuestionsPrepared === 'boolean') {
    state.skillQuestionsPrepared = data.skillQuestionsPrepared;
  }
  if (Object.keys(state).length > 0) {
    try {
      localStorage.setItem(STORAGE_PREFIX + sessionId, JSON.stringify(state));
    } catch (e) {
      console.warn('persist workflow phase failed', e);
    }
  }
  return state;
}

/** 从 localStorage 恢复 phase / suggestStartSkill / skillQuestionsPrepared */
export function loadWorkflowState(sessionId) {
  if (!sessionId) {
    return {};
  }
  try {
    const raw = localStorage.getItem(STORAGE_PREFIX + sessionId);
    if (!raw) {
      return {};
    }
    return JSON.parse(raw);
  } catch {
    return {};
  }
}

export function clearWorkflowState(sessionId) {
  if (!sessionId) {
    return;
  }
  try {
    localStorage.removeItem(STORAGE_PREFIX + sessionId);
  } catch {
    /* ignore */
  }
}

/** 将 workflow 状态应用到 Vue refs */
export function applyWorkflowToRefs(data, refs) {
  if (!data) {
    return;
  }
  if (data.phase && refs.workflowPhase) {
    refs.workflowPhase.value = data.phase;
  }
  if (typeof data.suggestStartSkill === 'boolean' && refs.suggestStartSkill) {
    refs.suggestStartSkill.value = data.suggestStartSkill;
  }
  if (typeof data.skillQuestionsPrepared === 'boolean' && refs.skillQuestionsPrepared) {
    refs.skillQuestionsPrepared.value = data.skillQuestionsPrepared;
  }
}
