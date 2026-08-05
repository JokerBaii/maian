const DEMO_USER_STORAGE_KEY = 'maian_demo_user_id'

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
 * 取当前身份。只接受上面三个 id，其余情况统一回落到默认身份。
 * 后端要求该请求头为合法用户标识，非法值会导致所有接口报错。
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
}
