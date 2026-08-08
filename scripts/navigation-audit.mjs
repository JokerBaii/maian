import { chromium } from 'playwright'
import { writeFile } from 'node:fs/promises'

const baseUrl = process.env.MAIAN_AUDIT_URL || 'http://127.0.0.1:5174/maian/'
const output = process.env.MAIAN_FLOW_REPORT || '/tmp/maian-navigation-audit.json'
const identityPageUrl = new URL('/api/v1/profile', baseUrl).toString()
const users = {
  user: '30000000-0000-0000-0000-000000000001',
  volunteer: '30000000-0000-0000-0000-000000000002',
  admin: '30000000-0000-0000-0000-000000000003'
}

const scenarios = [
  ['user', 'pages/index/index', '.sos-action', 0, 'pages/rescue/index'],
  ['user', 'pages/index/index', '.quick-action', 0, 'pages/map/index'],
  ['user', 'pages/index/index', '.quick-action', 1, 'pages/device/add'],
  ['user', 'pages/index/index', '.quick-action', 2, 'pages/checkup/upload'],
  ['user', 'pages/index/index', '.quick-action', 3, 'pages/science/index'],
  ['user', 'pages/index/index', '.section-link', 0, 'pages/health/index'],
  ['user', 'pages/index/index', '.classroom-row', 0, 'pages/science/detail'],
  ['user', 'pages/index/index', '.quiz-shortcut', 0, 'pages/science/quiz'],
  ['user', 'pages/health/index', '.chart-detail', 0, 'pages/health/detail'],
  ['user', 'pages/health/index', '.device-card', 0, 'pages/health/bind'],
  ['user', 'pages/health/index', '.resource-card', 0, 'pages/checkup/report'],
  ['user', 'pages/health/index', '.resource-card', 1, 'pages/checkup/archive'],
  ['user', 'pages/mine/index', '.nav-icon-btn', 0, 'pages/mine/settings'],
  ['user', 'pages/mine/index', '.stat-item', 0, 'pages/mine/records'],
  ['user', 'pages/mine/index', '.stat-item', 1, 'pages/device/manage'],
  ['user', 'pages/mine/index', '.stat-item', 2, 'pages/science/submissions'],
  ['user', 'pages/mine/index', '.menu-item', 2, 'pages/mine/contacts'],
  ['user', 'pages/mine/index', '.menu-item', 5, 'pages/mine/demo'],
  ['user', 'pages/mine/index', '.menu-item', 6, 'pages/mine/auth'],
  ['volunteer', 'pages/mine/index', '.menu-item', 2, 'pages/rescue/tasks'],
  ['volunteer', 'pages/rescue/tasks', '.secondary-action', 0, 'pages/rescue/detail'],
  ['admin', 'pages/mine/index', '.menu-item', 2, 'pages/device/review'],
  ['admin', 'pages/mine/index', '.menu-item', 3, 'pages/science/review'],
  ['user', 'pages/device/manage', '.page-add-action', 0, 'pages/device/add'],
  ['user', 'pages/science/index', '.nav-right', 0, 'pages/science/contribute'],
  ['user', 'pages/science/index', '.content-card', 0, 'pages/science/detail'],
  ['user', 'pages/checkup/archive', '.fab-btn', 0, 'pages/checkup/upload'],
  ['user', 'pages/science/video?id=V001', '.quiz-button', 0, 'pages/science/quiz']
]

const browser = await chromium.launch({ headless: true })
const context = await browser.newContext({
  viewport: { width: 390, height: 844 },
  ignoreHTTPSErrors: true,
  permissions: ['geolocation'],
  geolocation: { longitude: 120.144, latitude: 30.277 }
})
await context.addInitScript(() => {
  try {
    localStorage.setItem('maian:privacy-consent:v1', 'navigation-audit')
  } catch {
    // about:blank 等无源页面不可访问 localStorage；进入应用源后会再次执行。
  }
})
const page = await context.newPage()
const results = []

for (const [role, start, selector, index, expected] of scenarios) {
  await page.goto(identityPageUrl, { waitUntil: 'domcontentloaded' })
  await page.evaluate(userId => {
    localStorage.setItem('maian_demo_user_id', userId)
    localStorage.removeItem('maian_demo_access_token')
    localStorage.removeItem('maian_demo_access_token_user')
    localStorage.removeItem('maian_demo_access_token_expiry')
  }, users[role])
  await page.goto('about:blank')
  await page.goto(`${baseUrl}?audit=${encodeURIComponent(`${role}-${start}-${Date.now()}`)}#/${start}`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(620)

  let passed = false
  let error = ''
  try {
    const target = page.locator(selector).nth(index)
    if (!(await target.isVisible())) throw new Error(`control not visible: ${selector}[${index}]`)
    await target.evaluate(element => element.click())
    await page.waitForTimeout(260)
    passed = page.url().includes(`#/${expected}`)
    if (!passed) error = `expected ${expected}, received ${page.url()}`
  } catch (cause) {
    error = cause instanceof Error ? cause.message : String(cause)
  }
  results.push({ role, start, selector, index, expected, passed, error })
}

await browser.close()
const report = {
  checked: results.length,
  passed: results.filter(result => result.passed).length,
  failures: results.filter(result => !result.passed)
}
await writeFile(output, JSON.stringify(report, null, 2))
console.log(JSON.stringify(report, null, 2))
if (report.failures.length) process.exitCode = 1
