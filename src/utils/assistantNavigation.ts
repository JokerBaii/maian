const destinations = new Set([
  '/pages/mine/settings', '/pages/mine/contacts', '/pages/health/bind',
  '/pages/device/manage', '/pages/science/detail', '/pages/agent/index',
  '/pages/checkup/archive', '/pages/checkup/report', '/pages/science/submissions', '/pages/mine/records', '/pages/rescue/detail',
  '/pages/rescue/index', '/pages/map/index', '/pages/health/index', '/pages/science/index'
])

export function isAssistantDestination(url: string): boolean {
  if (!url.startsWith('/pages/') || url.includes('#')) return false
  return destinations.has(url.split('?')[0])
}

export function navigateAssistant(action: { url: string }): Promise<void> {
  if (!isAssistantDestination(action.url)) return Promise.reject(new Error('这个入口暂时打不开'))
  return new Promise((resolve, reject) => {
    const options = { url: action.url, success: () => resolve(), fail: () => reject(new Error('页面打开失败，请重试')) }
    if (['/pages/map/index', '/pages/health/index'].includes(action.url)) uni.switchTab(options)
    else uni.navigateTo(options)
  })
}
