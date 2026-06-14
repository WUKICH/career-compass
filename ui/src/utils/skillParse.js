import { MAX_CORE_SKILLS } from '@/constants/coreSkills'

const DELIM_PATTERN = /[，,、;；]/

/**
 * 将 profile.skill / 用户输入拆成确认页用的 { name, score } 列表。
 * 与后端 ResumeSkillNormalizer 对齐：有逗号类分隔符按标点拆，否则按空白拆。
 */
export function parseSkillNamesFromProfile(skillStr) {
  if (!skillStr || !String(skillStr).trim()) return []
  const s = String(skillStr).trim()
  if (s === '未明确') return []
  const parts = DELIM_PATTERN.test(s)
    ? s.split(/[，,、;；]+/)
    : s.split(/\s+/)
  return parts
    .map((x) => x.trim())
    .filter(Boolean)
    .slice(0, MAX_CORE_SKILLS)
    .map((name) => ({ name, score: 0 }))
}

/** 一次输入可能含多个技能，返回待添加的名称列表（已去重、截断） */
export function splitSkillInput(raw) {
  if (!raw || !String(raw).trim()) return []
  const names = parseSkillNamesFromProfile(raw).map((x) => x.name)
  return names.length ? names : [String(raw).trim()]
}
