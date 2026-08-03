export const DEMO_USER_STORAGE_KEY = 'maian_demo_user_id'

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

export function getDemoUserId() {
  return uni.getStorageSync(DEMO_USER_STORAGE_KEY) || demoUsers[0].id
}

export function setDemoUserId(userId: string) {
  uni.setStorageSync(DEMO_USER_STORAGE_KEY, userId)
}
