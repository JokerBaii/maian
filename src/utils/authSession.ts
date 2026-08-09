const ACCESS_TOKEN_KEY = 'maian_access_token'
const ACCESS_TOKEN_EXPIRY_KEY = 'maian_access_token_expiry'

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
  uni.setStorageSync(ACCESS_TOKEN_KEY, token)
  uni.setStorageSync(ACCESS_TOKEN_EXPIRY_KEY, Date.parse(expiresAt))
}

export function clearAccountSession() {
  uni.removeStorageSync(ACCESS_TOKEN_KEY)
  uni.removeStorageSync(ACCESS_TOKEN_EXPIRY_KEY)
}

export function hasAccountSession() {
  return Boolean(getAccountAccessToken())
}
