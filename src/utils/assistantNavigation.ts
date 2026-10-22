const destinations = new Set([
  '/pages/mine/settings', '/pages/mine/contacts', '/pages/health/bind',
  '/pages/device/manage', '/pages/science/detail', '/pages/agent/index',
  '/pages/checkup/archive', '/pages/checkup/report', '/pages/science/submissions', '/pages/mine/records', '/pages/rescue/detail'
])

export function isAssistantDestination(url: string): boolean {
  if (!url.startsWith('/pages/') || url.includes('#')) return false
  return destinations.has(url.split('?')[0])
}
