import { chromium } from 'playwright'

const baseUrl = process.env.MAIAN_AUDIT_URL || 'http://127.0.0.1:5175/maian/'
const volunteerId = '30000000-0000-0000-0000-000000000002'
const browser = await chromium.launch({ headless: true })
const context = await browser.newContext({
  viewport: { width: 390, height: 844 },
  ignoreHTTPSErrors: true,
  permissions: ['geolocation'],
  geolocation: { longitude: 120.144, latitude: 30.277 }
})
await context.addInitScript(userId => {
  localStorage.setItem('maian:privacy-consent:v1', 'content-audit')
  localStorage.setItem('maian_demo_user_id', userId)
  localStorage.removeItem('maian_demo_access_token')
  localStorage.removeItem('maian_demo_access_token_user')
  localStorage.removeItem('maian_demo_access_token_expiry')
}, volunteerId)
const page = await context.newPage()
const rescueId = '41000000-0000-0000-0000-000000000004'
await page.route(`**/api/v1/rescue-calls/responder-tasks/${rescueId}`, async route => {
  if (route.request().method() !== 'GET') return route.continue()
  const response = await route.fetch()
  const payload = await response.json()
  if (payload?.data) {
    payload.data.status = 'EN_ROUTE_TO_REQUESTER'
    payload.data.liveTracking = {
      responderLatitude: 30.27025,
      responderLongitude: 120.14505,
      capturedAt: new Date().toISOString()
    }
  }
  await route.fulfill({ response, json: payload })
})
await page.goto(`${baseUrl}#/pages/rescue/detail?id=${rescueId}&mode=responder`, { waitUntil: 'domcontentloaded' })
await page.waitForTimeout(1000)

const report = await page.evaluate(() => {
  const visible = element => {
    const style = getComputedStyle(element)
    const rect = element.getBoundingClientRect()
    return style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0
  }
  const countVisible = selector => [...document.querySelectorAll(selector)].filter(visible).length
  const routeVisible = countVisible('.route-card') === 1
  return {
    routeVisible,
    duplicateAedPanelWhileRouting: routeVisible && countVisible('.aed-panel') > 0,
    duplicateMapNavigationButton: countVisible('.map-location-button') > 0,
    navigationActions: [...document.querySelectorAll('.quick-action')]
      .filter(visible)
      .filter(element => element.textContent?.includes('导航')).length,
    emergencyActions: [...document.querySelectorAll('.quick-action')]
      .filter(visible)
      .filter(element => element.textContent?.includes('120')).length,
    duplicateEmergencyCard: countVisible('.safety-card') > 0,
    contradictoryWaitingCard: countVisible('.waiting-card') > 0,
    horizontalOverflow: document.documentElement.scrollWidth > document.documentElement.clientWidth + 1,
    bodyText: document.body.innerText
  }
})
await page.evaluate(() => {
  window.__maianNavigationUrl = ''
  window.open = url => {
    window.__maianNavigationUrl = String(url)
    return window
  }
})
const navigationAction = page.locator('.quick-action').filter({ hasText: '现场导航' })
if (await navigationAction.isVisible()) await navigationAction.click()
report.navigationUrl = await page.evaluate(() => window.__maianNavigationUrl || '')
await page.screenshot({ path: '/tmp/maian-volunteer-rescue-detail.png', fullPage: true })
await browser.close()

const failures = []
if (!report.routeVisible) failures.push('未进入途中路线状态')
if (report.duplicateAedPanelWhileRouting) failures.push('途中重复显示 AED 信息面板')
if (report.duplicateMapNavigationButton) failures.push('地图内仍有重复导航按钮')
if (report.navigationActions > 1) failures.push('现场导航入口重复')
if (report.emergencyActions !== 1) failures.push(`拨打 120 入口数量异常: ${report.emergencyActions}`)
if (report.duplicateEmergencyCard) failures.push('拨打 120 安全卡与快捷入口重复')
if (report.contradictoryWaitingCard) failures.push('已匹配 AED 时仍显示扩大匹配提示')
if (report.horizontalOverflow) failures.push('页面存在横向溢出')
if (!report.navigationUrl.startsWith('https://uri.amap.com/navigation?')) failures.push('现场导航未进入高德路线规划')

console.log(JSON.stringify({
  routeVisible: report.routeVisible,
  duplicateAedPanelWhileRouting: report.duplicateAedPanelWhileRouting,
  duplicateMapNavigationButton: report.duplicateMapNavigationButton,
  navigationActions: report.navigationActions,
  emergencyActions: report.emergencyActions,
  duplicateEmergencyCard: report.duplicateEmergencyCard,
  contradictoryWaitingCard: report.contradictoryWaitingCard,
  horizontalOverflow: report.horizontalOverflow,
  navigationUrl: report.navigationUrl,
  failures
}, null, 2))
if (failures.length) process.exitCode = 1
