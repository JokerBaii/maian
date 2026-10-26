import { ASSISTANT_SESSION_KEY } from './assistantPersistence'
const ACCESS_TOKEN_KEY = 'maian_access_token'
const ACCESS_TOKEN_EXPIRY_KEY = 'maian_access_token_expiry'
const sessionListeners = new Set<() => void>()

/** Clear account-scoped memory and native resources when login identity changes. */
export function onAccountSessionChange(listener: () => void) {
  sessionListeners.add(listener)
  return () => { sessionListeners.delete(listener) }
}

export function getAccountAccessToken(): string {
  const token = uni.getStorageSync(ACCESS_TOKEN_KEY) || ''
  const expiry = Number(uni.getStorageSync(ACCESS_TOKEN_EXPIRY_KEY) || 0)
  if (!token || expiry <= Date.now() + 30_000) {
    clearAccountSession()
    return ''
  }
  return token
}

export function saveAccountSession(token: string, expiresAt: string) {
  const changed = token !== uni.getStorageSync(ACCESS_TOKEN_KEY)
  uni.setStorageSync(ACCESS_TOKEN_KEY, token)
  uni.setStorageSync(ACCESS_TOKEN_EXPIRY_KEY, Date.parse(expiresAt))
  if (changed) sessionListeners.forEach(listener => listener())
}

export function clearAccountSession() {
  const hadSession = Boolean(uni.getStorageSync(ACCESS_TOKEN_KEY))
  uni.removeStorageSync(ASSISTANT_SESSION_KEY)
  uni.removeStorageSync(ACCESS_TOKEN_KEY)
  uni.removeStorageSync(ACCESS_TOKEN_EXPIRY_KEY)
  if (hadSession) sessionListeners.forEach(listener => listener())
}

export function hasAccountSession() {
  return Boolean(getAccountAccessToken())
}
