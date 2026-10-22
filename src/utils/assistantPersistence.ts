import type { AssistantEntry } from '@/composables/useAssistantSession'
import type { AssistantContext } from '@/api/agent'

export const ASSISTANT_SESSION_KEY = 'maian:assistant-session:v1'
export interface AssistantSnapshot { entries: AssistantEntry[]; draft: string; useModel: boolean; scene: AssistantContext }
export interface AssistantPersistence {
  load(owner: string): AssistantSnapshot | undefined
  save(owner: string, value: AssistantSnapshot): void
  clear(): void
}
// A session-bound marker, never another copy of the access token.
function marker(value: string) {
  let a = 2166136261, b = 5381
  for (let i = 0; i < value.length; i++) { a = Math.imul(a ^ value.charCodeAt(i), 16777619); b = Math.imul(b, 33) ^ value.charCodeAt(i) }
  return `${a >>> 0}:${b >>> 0}:${value.length}`
}
export const assistantPersistence: AssistantPersistence = {
  load(owner) {
    try {
      const raw = uni.getStorageSync(ASSISTANT_SESSION_KEY)
      if (!raw) return
      const saved = JSON.parse(raw)
      if (saved.version !== 1 || saved.owner !== marker(owner) || Date.now() - saved.at > 86400000 || !Array.isArray(saved.value?.entries)) { this.clear(); return }
      return saved.value
    } catch { this.clear() }
  },
  save(owner, value) {
    if (!owner) return
    try {
      const raw = JSON.stringify({ version: 1, owner: marker(owner), at: Date.now(), value })
      // Never leave a stale checkpoint behind if the new snapshot cannot be saved.
      if (raw.length > 1500000) { this.clear(); return }
      uni.setStorageSync(ASSISTANT_SESSION_KEY, raw)
    } catch { this.clear() }
  },
  clear() { try { uni.removeStorageSync(ASSISTANT_SESSION_KEY) } catch { /* Storage may be unavailable. */ } }
}
