import { chromium } from 'playwright'
import { mkdir, writeFile } from 'node:fs/promises'
import path from 'node:path'

const baseUrl = process.env.MAIAN_AUDIT_URL || 'http://127.0.0.1:5174/maian/'
const outputDir = process.env.MAIAN_AUDIT_OUTPUT || '/tmp/maian-ui-audit'
const settleMs = Number(process.env.MAIAN_AUDIT_SETTLE_MS || 620)
const viewportWidth = Number(process.env.MAIAN_AUDIT_VIEWPORT_WIDTH || 390)
const viewportHeight = Number(process.env.MAIAN_AUDIT_VIEWPORT_HEIGHT || 844)
const expectFixedLocation = process.env.MAIAN_EXPECT_FIXED_LOCATION === 'true'
const identityPageUrl = new URL('/api/v1/profile', baseUrl).toString()

const USER_ID = '30000000-0000-0000-0000-000000000001'
const VOLUNTEER_ID = '30000000-0000-0000-0000-000000000002'
const ADMIN_ID = '30000000-0000-0000-0000-000000000003'

const pages = [
  ['user', USER_ID, 'pages/index/index'],
  ['user', USER_ID, 'pages/map/index'],
  ['user', USER_ID, 'pages/health/index'],
  ['user', USER_ID, 'pages/mine/index'],
  ['user', USER_ID, 'pages/rescue/index'],
  ['user', USER_ID, 'pages/rescue/detail?id=41000000-0000-0000-0000-000000000004'],
  ['user', USER_ID, 'pages/device/add'],
  ['user', USER_ID, 'pages/device/manage'],
  ['user', USER_ID, 'pages/health/bind'],
  ['user', USER_ID, 'pages/health/detail'],
  ['user', USER_ID, 'pages/health/alert'],
  ['user', USER_ID, 'pages/checkup/upload'],
  ['user', USER_ID, 'pages/checkup/report'],
  ['user', USER_ID, 'pages/checkup/archive'],
  ['user', USER_ID, 'pages/science/detail?id=S001'],
  ['user', USER_ID, 'pages/science/video?id=V001'],
  ['user', USER_ID, 'pages/science/quiz'],
  ['user', USER_ID, 'pages/science/contribute'],
  ['user', USER_ID, 'pages/science/submissions'],
  ['user', USER_ID, 'pages/science/index'],
  ['user', USER_ID, 'pages/mine/auth'],
  ['user', USER_ID, 'pages/mine/contacts'],
  ['user', USER_ID, 'pages/mine/records'],
  ['user', USER_ID, 'pages/mine/settings'],
  ['user', USER_ID, 'pages/mine/demo'],
  ['volunteer', VOLUNTEER_ID, 'pages/index/index'],
  ['volunteer', VOLUNTEER_ID, 'pages/mine/index'],
  ['volunteer', VOLUNTEER_ID, 'pages/device/manage'],
  ['admin', ADMIN_ID, 'pages/device/review'],
  ['volunteer', VOLUNTEER_ID, 'pages/rescue/tasks'],
  ['admin', ADMIN_ID, 'pages/index/index'],
  ['admin', ADMIN_ID, 'pages/mine/index'],
  ['admin', ADMIN_ID, 'pages/device/manage'],
  ['user', USER_ID, 'pages/legal/index?type=privacy'],
  ['admin', ADMIN_ID, 'pages/science/review']
]

await mkdir(outputDir, { recursive: true })
const browser = await chromium.launch({ headless: true })
const context = await browser.newContext({
  viewport: { width: viewportWidth, height: viewportHeight },
  deviceScaleFactor: 1,
  ignoreHTTPSErrors: true,
  permissions: ['geolocation'],
  geolocation: { longitude: 120.144, latitude: 30.277 }
})
await context.addInitScript(() => {
  try {
    localStorage.setItem('maian:privacy-consent:v1', 'ui-audit')
  } catch {
    // about:blank 等无源页面不可访问 localStorage；进入应用源后会再次执行。
  }
})
const page = await context.newPage()
const pageErrors = []
page.on('pageerror', error => pageErrors.push(error.message))

const results = []
for (const [role, userId, route] of pages) {
  await page.goto(identityPageUrl, { waitUntil: 'domcontentloaded' })
  await page.evaluate(({ userId }) => {
    localStorage.setItem('maian_demo_user_id', userId)
    localStorage.setItem('maian:privacy-consent:v1', new Date().toISOString())
    localStorage.removeItem('maian_demo_access_token')
    localStorage.removeItem('maian_demo_access_token_user')
    localStorage.removeItem('maian_demo_access_token_expiry')
  }, { userId })
  await page.goto('about:blank')
  await page.goto(`${baseUrl}?audit=${encodeURIComponent(`${role}-${route}-${Date.now()}`)}#/${route}`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(settleMs)

  const snapshot = await page.evaluate(() => {
    const text = document.body.innerText
    const technical = text.match(/\b[A-Z]{2,}_[A-Z_]+\b|Spring\s*AI|DeepSeek|\bOCR\b|\bJWT\b|\bSSE\b|WebSocket|服务端|后端|数据库/gi) || []
    const verticalScrollers = [...document.querySelectorAll('*')].filter(element => {
      const style = getComputedStyle(element)
      return ['auto', 'scroll'].includes(style.overflowY) && element.scrollHeight > element.clientHeight + 2
    })
    const appRect = document.querySelector('uni-app')?.getBoundingClientRect()
    const fixedLayerOverflow = appRect
      ? [...document.querySelectorAll('*')].some(element => {
          const style = getComputedStyle(element)
          const rect = element.getBoundingClientRect()
          return style.position === 'fixed'
            && rect.width > 0
            && (rect.left < appRect.left - 1 || rect.right > appRect.right + 1)
        })
      : false
    const desktopFrameInvalid = window.innerWidth >= 768 && appRect
      ? Math.abs(appRect.width - 430) > 1 || Math.abs((appRect.left + appRect.right) / 2 - window.innerWidth / 2) > 1
      : false
    return {
      title: document.title,
      technical: [...new Set(technical)],
      horizontalOverflow: document.documentElement.scrollWidth > document.documentElement.clientWidth + 1,
      nestedVerticalScroll: verticalScrollers.length > 1,
      fixedLayerOverflow,
      desktopFrameInvalid,
      emptyBody: text.trim().length < 8,
      fixedLocationReady: text.includes('杭州市西湖区黄龙体育馆东门')
    }
  })
  const slug = `${role}-${route}`.replace(/[?=&/]+/g, '-').replace(/[^a-zA-Z0-9\u4e00-\u9fa5-]/g, '')
  await page.screenshot({ path: path.join(outputDir, `${slug}.png`), fullPage: true })
  results.push({ role, route, ...snapshot })
}

await browser.close()
const report = {
  checked: results.length,
  technicalLeaks: results.filter(item => item.technical.length),
  horizontalOverflow: results.filter(item => item.horizontalOverflow).map(item => item.route),
  nestedVerticalScroll: results.filter(item => item.nestedVerticalScroll).map(item => item.route),
  fixedLayerOverflow: results.filter(item => item.fixedLayerOverflow).map(item => `${item.role}:${item.route}`),
  desktopFrameInvalid: results.filter(item => item.desktopFrameInvalid).map(item => `${item.role}:${item.route}`),
  emptyPages: results.filter(item => item.emptyBody).map(item => item.route),
  fixedLocationMissing: expectFixedLocation
    ? results.filter(item => item.route === 'pages/rescue/index' && !item.fixedLocationReady).map(item => item.role)
    : [],
  pageErrors: [...new Set(pageErrors)]
}
await writeFile(path.join(outputDir, 'report.json'), JSON.stringify(report, null, 2))
console.log(JSON.stringify(report, null, 2))
