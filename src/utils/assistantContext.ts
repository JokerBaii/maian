import type { AssistantContext } from '@/api/agent'

export function pageAssistantContext(page: string, query: Record<string,string> = {}, subject: AssistantContext = {}): AssistantContext {
  const context: AssistantContext = { page: page.replace(/^\//, '') }
  const key = ({ 'pages/device/add': 'deviceId', 'pages/rescue/detail': 'rescueId', 'pages/checkup/report': 'reportId', 'pages/science/detail': 'articleId' } as const)[context.page as 'pages/device/add']
  if (key && query.id) context[key] = query.id
  for (const [name,value] of Object.entries(subject)) {
    if (value && ['deviceId','rescueId','reportId','articleId','contactId','submissionId','reminderId','accountId','settingKey'].includes(name)) (context as Record<string,string>)[name] = value
  }
  return context
}

export function readAssistantContext(subject: AssistantContext = {}): AssistantContext {
  if (typeof getCurrentPages !== 'function') return { ...subject }
  const pages = getCurrentPages(), current = pages[pages.length - 1] as any
  if (!current) return { ...subject }
  // The full assistant retains the business page that opened it.
  const page = current.route === 'pages/assistant/index' ? pages[pages.length - 2] as any : current
  return pageAssistantContext(page?.route || 'pages/index/index', page?.options || page?.$page?.options || {}, subject)
}

export function ordinalIndex(text: string): number | undefined {
  const match = text.match(/第([一二两三四五六七八九十]|\d{1,2})(?:个|台|篇|份|条|项)?/)
  if (!match) return
  const number = Number(match[1]) || ({ 一:1, 二:2, 两:2, 三:3, 四:4, 五:5, 六:6, 七:7, 八:8, 九:9, 十:10 } as Record<string,number>)[match[1]]
  return number > 0 ? number - 1 : undefined
}
