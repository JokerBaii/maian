const destinations = new Set([
  '/pages/mine/settings', '/pages/mine/contacts', '/pages/health/bind',
  '/pages/device/manage', '/pages/science/detail', '/pages/agent/index'
])

export function isAssistantDestination(url: string): boolean {
  if (!url.startsWith('/pages/') || url.includes('#')) return false
  return destinations.has(url.split('?')[0])
}
