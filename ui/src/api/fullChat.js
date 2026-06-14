import request from '@/utils/request'

/** 完全版对话 API（与 chat.js 分流；响应体为 FullChatPayload 结构） */

export const initFullChatApi = (data) => {
  return request.post('/full/chat/init', data)
}

export const fullChatApi = (sessionId, userMessage) => {
  return request.post('/full/chat/send', { sessionId, message: userMessage })
}

export const submitFullSkillAnswerApi = (sessionId, answer) => {
  return request.post('/full/chat/skill/answer', { sessionId, answer })
}

export const submitFullCareerAnswerApi = (sessionId, answer) => {
  return request.post('/full/chat/career/answer', { sessionId, answer })
}
