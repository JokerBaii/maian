import { request } from '@/api/http'
import type { UserSettingsResponse } from '@/api/user'
import { getAccountAccessToken } from '@/utils/authSession'

/** Both the upload page and Agent must obtain the same explicit external-processing consent. */
export async function ensureHealthProcessingConsent(dependencies = { request, identity: getAccountAccessToken }): Promise<boolean> {
  const owner = dependencies.identity()
  const validate = () => {
    if (!owner || dependencies.identity() !== owner) throw new Error('账号已变化，已停止处理报告')
  }
  validate()
  const settings = await dependencies.request<UserSettingsResponse>('/api/v1/settings')
  validate()
  if (settings.healthDataShare) return true
  const agreed = await new Promise<boolean>(resolve => uni.showModal({
    title: '授权识别体检报告',
    content: '报告图片将发送至百度智能云 OCR 识别文字。继续将开启“健康数据共享”，允许本次及后续报告识别与 AI 信息整理；可随时在隐私设置中关闭。是否同意？',
    confirmText: '同意并继续',
    cancelText: '暂不授权',
    success: result => resolve(Boolean(result.confirm)),
    fail: () => resolve(false)
  }))
  validate()
  if (!agreed) return false
  // Refresh after the dialog so granting consent does not overwrite other recent settings.
  const latest = await dependencies.request<UserSettingsResponse>('/api/v1/settings')
  validate()
  if (!latest.healthDataShare) {
    const { updatedAt: _updatedAt, ...values } = latest
    await dependencies.request('/api/v1/settings', { method: 'PUT', data: { ...values, healthDataShare: true } })
    validate()
    uni.$emit?.('maian:agent-executed', { operation: 'SETTINGS_UPDATE' })
  }
  return true
}
