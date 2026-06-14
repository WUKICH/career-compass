import { fetchClientFeatures } from '@/api/config'

let cached = null

/** 调试用：切换环境后如需立即重新拉开关可调用 */
export function resetFullVersionGateCache() {
  cached = null
}

/**
 * @returns {Promise<boolean>} true 表示完全版仍关闭（需拦截）；拉取失败时按 true 保守处理
 */
export async function getFullVersionTemporarilyClosed() {
  if (cached === true || cached === false) {
    return cached
  }
  try {
    const r = await fetchClientFeatures()
    if (r?.code === 1 && r?.data && typeof r.data.fullVersionTemporarilyClosed === 'boolean') {
      cached = r.data.fullVersionTemporarilyClosed
    } else {
      cached = true
    }
  } catch {
    cached = true
  }
  return cached
}
