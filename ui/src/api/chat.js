import request from '@/utils/request'

// 初始化聊天接口（提交表单）
export const initChatApi = (data) => {
  return request.post('/chat/init', data)
}

// 对话式收集个人信息一步
export const collectStepApi = (data) => {
  return request.post('/chat/collect', data)
}

/** 信息确认页最终提交（与分步 collect 分离） */
export const collectConfirmApi = (data) => {
  return request.post('/chat/collect/confirm', data)
}

/** 核心技能输入后：NLP + 收集 Agent 返回结构化画像（不落库），再进入确认页 */
export const previewCollectSkillApi = (data) => {
  return request.post('/chat/collect/preview-skill', data)
}

/** 上传并解析简历：返回结构化 profilePreview（不落库） */
export const parseResumeApi = (formData) => {
  return request.post('/chat/collect/resume/parse', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 普通聊天接口（新版本，需要 sessionId）
export const chatApi = (sessionId, userMessage) => {
  return request.post('/chat/send', {
    sessionId,
    message: userMessage
  })
}

/** 双通道：显式开始技能测评 */
export const startSkillApi = (sessionId) => {
  return request.post('/chat/start-skill', { sessionId })
}

/** 轮询 workflow 状态（题目预生成进度） */
export const getWorkflowStatusApi = (sessionId) => {
  return request.get('/chat/workflow/status', { params: { sessionId } })
}

/** 阶段性综合报告：scope = resume | skill | career | full */
export const getPartialReportApi = (sessionId, scope) => {
  return request.get('/chat/report/partial', { params: { sessionId, scope } })
}

/** 霍兰德兴趣测试完整报告（含 dimensionAnalysis） */
export const getHollandReportApi = (sessionId) => {
  return request.get('/chat/report/holland', { params: { sessionId } })
}

// 流式聊天接口 - 使用 fetch API 来接收 Server-Sent Events
export const chatStreamApi = async (sessionId, userMessage) => {
  const baseURL = '/ai/match/api'
  const url = `${baseURL}/chat/stream?sessionId=${encodeURIComponent(sessionId)}&userMessage=${encodeURIComponent(userMessage)}`
  
  // 获取JWT token
  let token = null;
  try {
    const loginUser = JSON.parse(localStorage.getItem('loginUser') || 'null');
    if (loginUser?.token) {
      token = loginUser.token;
    } else {
      const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null');
      if (userInfo?.token) {
        token = userInfo.token;
      }
    }
  } catch (e) {
    console.error('获取token失败', e);
  }
  
  const headers = {
    'Content-Type': 'application/json',
  };
  
  // 如果有token，添加到请求头
  if (token) {
    headers.token = token;
  }
  
  const response = await fetch(url, {
    method: 'POST',
    headers: headers,
  })
  
  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`)
  }
  
  return response
}

// 提交技能测试答案
export const submitSkillAnswerApi = (sessionId, answer) => {
  return request.post('/chat/skill/answer', {
    sessionId,
    answer
  })
}

// 提交职业测试答案
export const submitCareerAnswerApi = (sessionId, answer) => {
  return request.post('/chat/career/answer', {
    sessionId,
    answer
  })
}

// 提交评估反馈
export const submitFeedbackApi = (sessionId, feedbackData) => {
  return request.post('/chat/feedback', {
    sessionId,
    ...feedbackData
  })
}