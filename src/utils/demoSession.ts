const DEMO_USER_STORAGE_KEY = 'maian_demo_user_id'
const DEMO_TOKEN_STORAGE_KEY = 'maian_demo_access_token'
const DEMO_TOKEN_EXPIRY_STORAGE_KEY = 'maian_demo_access_token_expiry'
const DEMO_TOKEN_USER_STORAGE_KEY = 'maian_demo_access_token_user'

/**
 * 固定的三个身份，与后端 user_profiles 表中的记录一一对应。
 * 不要改动这里的 id：后端按该 id 判定角色权限和数据归属。
 */
export const demoUsers = [
  {
    id: '30000000-0000-0000-0000-000000000001', role: 'USER', name: '体验用户', title: '普通用户',
    description: '发起呼救、录入设备、查看健康数据'
  },
  {
    id: '30000000-0000-0000-0000-000000000002', role: 'VOLUNTEER', name: '志愿者体验账号', title: '救援志愿者',
    description: '接取附近任务、更新救援进度'
  },
  {
    id: '30000000-0000-0000-0000-000000000003', role: 'ADMIN', name: '审核体验账号', title: '平台审核员',
    description: '审核用户提交的共享急救设备'
  }
] as const

const DEFAULT_DEMO_USER_ID = demoUsers[0].id

const VALID_IDS: readonly string[] = demoUsers.map(user => user.id)

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
  clearDemoAccessToken()
}

export function getDemoAccessToken(): string {
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
