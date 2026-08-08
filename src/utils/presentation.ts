import type { RescueStatus } from '@/api/rescue'
import type { EmergencyDeviceResponse } from '@/api/devices'

export interface RescueStatusPresentation {
  label: string
  description: string
  step: number
}

export const RESCUE_STATUS_PRESENTATION: Record<RescueStatus, RescueStatusPresentation> = {
  PENDING: { label: '呼救已发出', description: '救援请求已送达，请保持电话畅通', step: 0 },
  MATCHING: { label: '正在匹配救援力量', description: '正在联系附近可用设备与志愿者', step: 1 },
  EN_ROUTE_TO_AED: { label: '施救者正在取用 AED', description: '设备已锁定，施救者正在赶往取用点', step: 2 },
  EN_ROUTE_TO_REQUESTER: { label: '施救者正在赶来', description: '请留在安全且显眼的位置并保持电话畅通', step: 2 },
  ARRIVED: { label: '施救者已到达', description: '请按照现场指引配合救援', step: 2 },
  RESCUING: { label: '救援进行中', description: '请遵循现场人员指导并保持通道畅通', step: 2 },
  PENDING_CONFIRMATION: { label: '等待确认完成', description: '施救者已提交结果，请确认本次救援是否完成', step: 2 },
  COMPLETED: { label: '救援已完成', description: '本次救援已经结束，感谢每一位参与者', step: 3 },
  NO_RESOURCE: { label: '暂未找到救援资源', description: '当前附近暂无可调度 AED，请立即拨打 120', step: 1 },
  EXPIRED: { label: '本次匹配已结束', description: '长时间未匹配成功，请立即拨打 120', step: 1 },
  USER_CANCELLED: { label: '呼救已取消', description: '本次呼救已经取消，如仍需帮助请重新发起', step: 0 },
  SYSTEM_FAILED: { label: '暂未完成匹配', description: '救援匹配暂未完成，请立即拨打 120', step: 1 }
}

const DEFAULT_RESCUE_PRESENTATION: RescueStatusPresentation = {
  label: '状态更新中',
  description: '正在获取最新救援进展，请稍候',
  step: 1
}

export function rescueStatusPresentation(status?: string): RescueStatusPresentation {
  return RESCUE_STATUS_PRESENTATION[status as RescueStatus] || DEFAULT_RESCUE_PRESENTATION
}

export function rescueStatusLabel(status?: string): string {
  return rescueStatusPresentation(status).label
}

export function rescueUrgencyLabel(urgency?: string): string {
  return ({
    CRITICAL: '危急',
    HIGH: '紧急',
    MEDIUM: '一般',
    LOW: '较低'
  } as Record<string, string>)[String(urgency || '').toUpperCase()] || '待确认'
}

export function deviceStatusLabel(device: Pick<EmergencyDeviceResponse, 'type' | 'status' | 'mobilePresenceStatus'>): string {
  if (device.type === 'MOBILE' && device.status === 'AVAILABLE') {
    return ({ ONLINE: '在线', STALE: '位置待更新', OFFLINE: '离线' } as Record<string, string>)[
      device.mobilePresenceStatus || 'OFFLINE'
    ] || '状态更新中'
  }
  return ({
    PENDING_REVIEW: '待审核',
    AVAILABLE: device.type === 'FIXED' ? '可用' : '在线',
    RESERVED: '救援使用中',
    DISABLED: device.type === 'FIXED' ? '暂停使用' : '离线',
    EXPIRED: '已过有效期',
    REJECTED: '未通过审核'
  } as Record<string, string>)[device.status] || '状态更新中'
}

export function submissionStatusLabel(status?: string): string {
  return ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '未通过' } as Record<string, string>)[
    String(status || '').toUpperCase()
  ] || '状态更新中'
}

const SAFE_ERROR_MESSAGES: Record<string, string> = {
  NETWORK_ERROR: '网络连接不稳定，请稍后重试',
  AUTH_FAILED: '登录状态已失效，请重新进入',
  FORBIDDEN: '当前账号无法进行此操作',
  RESOURCE_NOT_FOUND: '相关内容不存在或已被移除',
  ROUTE_NOT_FOUND: '该功能暂时不可用',
  INVALID_STATE_TRANSITION: '当前状态已发生变化，请刷新后重试',
  VALIDATION_FAILED: '填写内容有误，请检查后重试',
  DATA_CONFLICT: '内容已经发生变化，请刷新后重试',
  HEALTH_ANALYSIS_UNAVAILABLE: '健康分析暂时不可用，请稍后重试',
  OCR_UNAVAILABLE: '报告识别暂时不可用，你仍可手动填写',
  STORAGE_CAPACITY_EXCEEDED: '存储空间暂时不足，请稍后重试',
  UPLOAD_TOO_LARGE: '图片过大，请压缩后重新上传',
  INTERNAL_ERROR: '服务暂时不可用，请稍后重试'
}

export function userFacingError(error: unknown, fallback: string): string {
  const candidate = error as { code?: string; message?: string } | null
  if (candidate?.code && SAFE_ERROR_MESSAGES[candidate.code]) return SAFE_ERROR_MESSAGES[candidate.code]
  const message = candidate?.message?.trim()
  if (!message) return fallback
  if (/\b(?:spring|deepseek|ocr|jwt|api|sql|http|exception|failed|undefined|null)\b|\bat\s+[\w.$]+\(|\/api\//i.test(message)) {
    return fallback
  }
  return message
}
