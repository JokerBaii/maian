import type { AssistantTask, AssistantAction, AssistantContext } from '@/api/agent'
import type { AgentDocument } from '@/api/agentDocuments'
import type { AppLocation, MapNavigationTarget } from '@/utils/location'

type Option = { value: string; label: string }
export interface TaskField { key: string; label: string; type?: 'number' | 'textarea' | 'select'; options?: Option[]; optional?: boolean; autoResolved?: boolean }
export interface TaskRun extends AssistantTask {
  title: string
  phase: 'QUEUED' | 'RUNNING' | 'INPUT' | 'FILE' | 'CONFIRM' | 'DONE' | 'ERROR' | 'UNCERTAIN' | 'CANCELED'
  fields: TaskField[]
  steps: string[]
  summary: string
  error?: string
  items: { title: string; detail: string; documentId?: string; navigation?: MapNavigationTarget; reference?: AssistantContext }[]
  links: AssistantAction[]
  document?: AgentDocument
  mediaId?: string
  fileName?: string
  rescue?: { location?: AppLocation; locatedAt?: number; locationError?: string; resourceError?: string }
  context?: AssistantContext
  output?: AssistantContext
  location?: AppLocation
  locationRequired?: boolean
  target?: any
  autoRouted?: boolean
  mutationStarted?: boolean
  restored?: boolean
}
export interface TaskEnvironment {
  api: (path: string, method?: UniNamespace.RequestOptions['method'] | 'PATCH', data?: any, timeout?: number) => Promise<any>
  currentLocation: () => Promise<AppLocation>
}
interface TaskDefinition {
  title: string
  confirm?: boolean
  createsRecord?: boolean
  locationRequired?: boolean
  refresh?: boolean
  source?: string
  fields?: TaskField[]
  rescueAction?: string
  list?: { path: string; title: string; destination?: string }
}

export const SETTINGS: Record<string, string> = { rescuePush: '呼救推送', healthAlert: '健康预警', scienceUpdate: '科普更新', locationShare: '位置共享', healthDataShare: '健康数据共享', maxHeartRate: '最高心率', minHeartRate: '最低心率' }
export const field = (key: string, label: string, type?: TaskField['type'], optional = false): TaskField => ({ key, label, type, optional })
export const select = (key: string, label: string, options: Option[]): TaskField => ({ key, label, type: 'select', options })
const approvalOptions = [{ value: 'true', label: '通过' }, { value: 'false', label: '驳回' }]

/** One definition per allowed operation: form, target, confirmation and persistence policy. */
export const TASK_DEFINITIONS: Record<string, TaskDefinition> = {
  VOLUNTEER_AVAILABILITY: { title: '设置救援响应状态' },
  REMINDER_CREATE: { title: '创建站内提醒', createsRecord: true },
  REMINDER_LIST: { title: '查看提醒' },
  REMINDER_DISMISS: { title: '结束提醒', refresh: true, source: '/api/v1/assistant/reminders' },
  CONTACT_UPDATE: { title: '修改紧急联系人', refresh: true, source: '/api/v1/emergency-contacts' },
  CONTACT_CALL: { title: '联系紧急联系人', confirm: true, source: '/api/v1/emergency-contacts' },
  DEVICE_UPDATE: { title: '修改设备资料', refresh: true, source: '/api/v1/emergency-devices/mine' },
  DEVICE_LOCATION: { title: '更新设备位置', confirm: true, locationRequired: true, source: '/api/v1/emergency-devices/mine' },
  DEVICE_NEARBY: { title: '查找附近AED', locationRequired: true },
  DEVICE_NAVIGATE: { title: '导航到设备', source: '/api/v1/emergency-devices' },
  DOCUMENT_AUTO: { title: '接收文件并办理', createsRecord: true },
  SCIENCE_DELETE: { title: '删除我的稿件', confirm: true, source: '/api/v1/science-submissions' },
  SCIENCE_INTERACTION: { title: '更新文章收藏与点赞', refresh: true, source: '/api/v1/science-content?kind=ARTICLE' },
  SCIENCE_READ: { title: '阅读文章', source: '/api/v1/science-content?kind=ARTICLE' },
  WEARABLE_READ: { title: '查看穿戴设备' },
  WEARABLE_UNBIND: { title: '解绑穿戴设备', confirm: true },
  RESCUE_OFFERS: { title: '查看可接救援' },
  RESCUE_PROGRESS: { title: '查看救援进展', source: 'rescue-targets' },
  RESCUE_ACCEPT: { title: '接受救援任务', confirm: true, source: 'rescue-targets', rescueAction: 'acceptance' },
  RESCUE_AED_ARRIVAL: { title: '确认到达取用点', confirm: true, source: 'rescue-targets', rescueAction: 'aed-arrival' },
  RESCUE_AED_PICKUP: { title: '确认取到AED', confirm: true, source: 'rescue-targets', rescueAction: 'aed-pickup' },
  RESCUE_ARRIVAL: { title: '确认到达现场', confirm: true, source: 'rescue-targets', rescueAction: 'requester-arrival' },
  RESCUE_START: { title: '开始施救', confirm: true, source: 'rescue-targets', rescueAction: 'rescue-start' },
  RESCUE_COMPLETE: { title: '提交救援完成', confirm: true, source: 'rescue-targets', rescueAction: 'completion-submission' },
  RESCUE_RETURN: { title: '确认归还AED', confirm: true, source: 'rescue-targets', rescueAction: 'aed-return' },
  RESCUE_CONFIRM_COMPLETION: { title: '确认救援完成', confirm: true, source: 'rescue-targets', rescueAction: 'completion-confirmation' },
  RESCUE_REPLACE: { title: '更换故障AED', confirm: true, source: 'rescue-targets', rescueAction: 'aed-replacement' },
  RESCUE_RESOURCE_FAILURE: { title: '结束故障设备调度', confirm: true, source: 'rescue-targets', rescueAction: 'resource-failure' },
  RESCUE_NAVIGATE: { title: '救援导航', source: 'rescue-targets' },
  SETTINGS_READ: { title: '查看设置' },
  SETTINGS_UPDATE: { title: '修改设置', refresh: true },
  CONTACT_LIST: { title: '查看紧急联系人', list: { path: '/api/v1/emergency-contacts', title: '紧急联系人', destination: '/pages/mine/contacts' } },
  CONTACT_SAVE: { title: '保存紧急联系人', createsRecord: true, fields: [field('name', '姓名'), field('phone', '手机号'), field('relation', '关系')] },
  CONTACT_DELETE: { title: '删除联系人', confirm: true, source: '/api/v1/emergency-contacts' },
  DEVICE_LIST: { title: '查看我的设备', list: { path: '/api/v1/emergency-devices/mine', title: '我的设备', destination: '/pages/device/manage' } },
  DEVICE_CREATE: { title: '登记急救设备', confirm: true, createsRecord: true, locationRequired: true, fields: [field('name', '设备名称'), field('address', '设备地址'), select('type', '设备类型', [{ value: 'FIXED', label: '固定设备' }, { value: 'MOBILE', label: '移动设备' }]), field('serviceTime', '服务时间')] },
  DEVICE_ENABLE: { title: '恢复设备', confirm: true, source: '/api/v1/emergency-devices/mine' },
  DEVICE_DISABLE: { title: '停用设备', confirm: true, source: '/api/v1/emergency-devices/mine' },
  DEVICE_FAULT: { title: '上报设备故障', confirm: true, source: '/api/v1/emergency-devices/mine' },
  DEVICE_DELETE: { title: '删除设备', confirm: true, source: '/api/v1/emergency-devices/mine' },
  DOCUMENT_UPLOAD: { title: '上传保存文档', createsRecord: true },
  DOCUMENT_LIST: { title: '查看我的文档', list: { path: '/api/v1/assistant/documents', title: '我的文档' } },
  DOCUMENT_DELETE: { title: '删除文档', confirm: true, source: '/api/v1/assistant/documents' },
  REPORT_IMPORT: { title: '整理体检报告', confirm: true, createsRecord: true, fields: [field('hospital', '检查机构'), field('checkupDate', '检查日期（YYYY-MM-DD）'), field('indicators', '指标（每行：名称 数值 单位）', 'textarea')] },
  REPORT_LIST: { title: '查看体检报告', list: { path: '/api/v1/health-reports', title: '体检报告', destination: '/pages/checkup/archive' } },
  REPORT_DELETE: { title: '删除报告', confirm: true, source: '/api/v1/health-reports' },
  SCIENCE_SUBMIT: { title: '提交科普稿件', createsRecord: true, fields: [field('title', '稿件标题'), select('category', '稿件分类', [{ value: 'device', label: '设备知识' }, { value: 'emergency', label: '急救知识' }, { value: 'health', label: '健康知识' }, { value: 'exercise', label: '运动健康' }]), field('content', '稿件正文', 'textarea')] },
  SCIENCE_MINE: { title: '查看我的投稿', list: { path: '/api/v1/science-submissions', title: '我的投稿', destination: '/pages/science/submissions' } },
  SCIENCE_REVIEW_LIST: { title: '查看待审核稿件', list: { path: '/api/v1/science-submissions/reviews/pending', title: '待审核稿件' } },
  SCIENCE_REVIEW: { title: '审核科普稿件', confirm: true, source: '/api/v1/science-submissions/reviews/pending', fields: [select('approved', '审核结果', approvalOptions), field('reviewNote', '审核说明', 'textarea', true)] },
  DEVICE_REVIEW_LIST: { title: '查看待审核设备', list: { path: '/api/v1/emergency-devices/reviews/pending', title: '待审核设备' } },
  DEVICE_REVIEW: { title: '审核设备', confirm: true, source: '/api/v1/emergency-devices/reviews/pending', fields: [select('approved', '审核结果', approvalOptions), field('reviewNote', '审核说明', 'textarea', true)] },
  RESCUE_LIST: { title: '查看我的救援', list: { path: '/api/v1/rescue-calls', title: '我的救援', destination: '/pages/mine/records' } },
  RESCUE_REVIEW: { title: '生成救援复盘', source: '/api/v1/rescue-calls' },
  RESCUE_FEEDBACK: { title: '提交救援评价', createsRecord: true, source: '/api/v1/rescue-calls', fields: [field('rating', '评分（1–5）', 'number'), field('comment', '评价内容', 'textarea', true)] },
  RESCUE_CREATE: { title: '发起呼救', confirm: true, createsRecord: true, fields: [field('address', '现场位置'), field('description', '现场情况', 'textarea', true)] },
  RESCUE_CANCEL: { title: '取消呼救', confirm: true, source: '/api/v1/rescue-calls' },
  COORDINATION_LIST: { title: '查看资源协同', list: { path: '/api/v1/coordination-tasks/mine', title: '资源协同', destination: '/pages/agent/index' } },
  COORDINATION_RETRY: { title: '重试协同任务', refresh: true, source: '/api/v1/coordination-tasks/mine' },
  HEALTH_STATUS: { title: '查看健康监测' },
  PROFILE_READ: { title: '查看个人资料' },
  ADMIN_USERS: { title: '查看账户', list: { path: '/api/v1/admin/users', title: '账户' } },
  ADMIN_ROLE: { title: '调整账户角色', confirm: true, source: '/api/v1/admin/users', fields: [select('role', '账户角色', [{ value: 'USER', label: '普通用户' }, { value: 'VOLUNTEER', label: '志愿者' }, { value: 'ADMIN', label: '管理员' }])] },
}

export function referenceKey(op: string): keyof AssistantContext | undefined {
  if (['REMINDER_LIST','REMINDER_DISMISS'].includes(op)) return 'reminderId'
  if (op.startsWith('ADMIN_')) return 'accountId'
  if (op.startsWith('CONTACT_')) return 'contactId'
  if (op.startsWith('DEVICE_')) return 'deviceId'
  if (op.startsWith('RESCUE_')) return 'rescueId'
  if (op.startsWith('REPORT_')) return 'reportId'
  if (['SCIENCE_INTERACTION','SCIENCE_READ'].includes(op)) return 'articleId'
  if (op.startsWith('SCIENCE_')) return 'submissionId'
}
export const referenceFor = (op: string, id: string): AssistantContext | undefined => {
  const key = referenceKey(op); return key ? { [key]: id } : undefined
}
export function rescueEligible(op: string, item: any): boolean {
  const states: Record<string,string[]> = {
    RESCUE_ACCEPT:['MATCHING'], RESCUE_AED_ARRIVAL:['EN_ROUTE_TO_AED'], RESCUE_AED_PICKUP:['EN_ROUTE_TO_AED'], RESCUE_ARRIVAL:['EN_ROUTE_TO_REQUESTER'], RESCUE_START:['ARRIVED'], RESCUE_COMPLETE:['RESCUING'], RESCUE_CONFIRM_COMPLETION:['PENDING_CONFIRMATION'], RESCUE_REPLACE:['EN_ROUTE_TO_AED'], RESCUE_RESOURCE_FAILURE:['EN_ROUTE_TO_AED'], RESCUE_REVIEW:['COMPLETED','USER_CANCELLED','SYSTEM_FAILED','EXPIRED','NO_RESOURCE']
  }
  if (states[op] && !states[op].includes(item.status)) return false
  if (op === 'RESCUE_AED_PICKUP' && !item.arrivedAtAedAt) return false
  if (op === 'RESCUE_RETURN' && !['COMPLETED','USER_CANCELLED','SYSTEM_FAILED','EXPIRED','NO_RESOURCE'].includes(item.status)) return false
  if (op === 'RESCUE_RETURN' && (!item.aedCustodyStatus || ['RETURNED','RESERVED','PICKUP_PENDING'].includes(item.aedCustodyStatus))) return false
  return true
}
