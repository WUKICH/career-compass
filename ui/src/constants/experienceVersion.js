/** 与后端 AssessmentBotRouting.isFullVersionLabel 一致：文案含「完全」即视为完全版 */
export const FULL_VERSION_UNAVAILABLE_MSG = '该功能还在开发中，暂不开放'

export function isFullVersionExperienceLabel(value) {
  return String(value ?? '').includes('完全')
}
