const DEMO_USER_STORAGE_KEY = 'maian_demo_user_id'
const DEMO_TOKEN_STORAGE_KEY = 'maian_demo_access_token'
const DEMO_TOKEN_EXPIRY_STORAGE_KEY = 'maian_demo_access_token_expiry'
const DEMO_TOKEN_USER_STORAGE_KEY = 'maian_demo_access_token_user'
const DEMO_ACTIVE_STORAGE_KEY = 'maian_demo_session_active'

/** 前端只保留鉴权白名单，昵称、角色和头像全部从 user_profiles 返回。 */
const DEMO_ACCOUNT_IDS = [
  '30000000-0000-0000-0000-000000000001',
  '30000000-0000-0000-0000-000000000002',
  '30000000-0000-0000-0000-000000000003'
] as const

const DEFAULT_DEMO_USER_ID = DEMO_ACCOUNT_IDS[0]
const VALID_IDS: readonly string[] = DEMO_ACCOUNT_IDS

/**
 * 取当前演示身份。只接受上面三个 id，其余情况统一回落默认身份。
 * 后端 Demo 登录接口会再次校验白名单并签发短时 JWT。
 */
export function getDemoUserId(): string {
  let stored = ''
  try {
    stored = uni.getStorageSync(DEMO_USER_STORAGE_KEY) || ''
  } catch {
    stored = ''
  }
  return VALID_IDS.includes(stored) ? stored : DEFAULT_DEMO_USER_ID
}

export function setDemoUserId(userId: string) {
  if (!VALID_IDS.includes(userId)) return
  uni.setStorageSync(DEMO_USER_STORAGE_KEY, userId)
  uni.setStorageSync(DEMO_ACTIVE_STORAGE_KEY, true)
  clearDemoAccessToken()
}

export function isDemoSessionActive(): boolean {
  const active = uni.getStorageSync(DEMO_ACTIVE_STORAGE_KEY)
  return active === true || active === 'true'
}

export function getDemoAccessToken(): string {
  if (!isDemoSessionActive()) return ''
  const token = uni.getStorageSync(DEMO_TOKEN_STORAGE_KEY) || ''
  const tokenUser = uni.getStorageSync(DEMO_TOKEN_USER_STORAGE_KEY) || ''
  const expiry = Number(uni.getStorageSync(DEMO_TOKEN_EXPIRY_STORAGE_KEY) || 0)
  if (tokenUser !== getDemoUserId() || !token || expiry <= Date.now() + 30_000) {
    clearDemoAccessToken()
    return ''
  }
  return token
}

export function saveDemoAccessToken(token: string, expiresAt: string) {
  uni.setStorageSync(DEMO_TOKEN_STORAGE_KEY, token)
  uni.setStorageSync(DEMO_TOKEN_EXPIRY_STORAGE_KEY, Date.parse(expiresAt))
  uni.setStorageSync(DEMO_TOKEN_USER_STORAGE_KEY, getDemoUserId())
}

export function clearDemoAccessToken() {
  uni.removeStorageSync(DEMO_TOKEN_STORAGE_KEY)
  uni.removeStorageSync(DEMO_TOKEN_EXPIRY_STORAGE_KEY)
  uni.removeStorageSync(DEMO_TOKEN_USER_STORAGE_KEY)
}

export function clearDemoSession() {
  clearDemoAccessToken()
  uni.removeStorageSync(DEMO_ACTIVE_STORAGE_KEY)
}
