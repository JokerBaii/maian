import { describe, expect, it } from 'vitest'
import {
  rescueStatusLabel,
  rescueUrgencyLabel,
  submissionStatusLabel,
  userFacingError
} from '../utils/presentation'

describe('user-facing presentation', () => {
  it('never exposes rescue enum values', () => {
    expect(rescueStatusLabel('SYSTEM_FAILED')).toBe('暂未完成匹配')
    expect(rescueStatusLabel('USER_CANCELLED')).toBe('呼救已取消')
    expect(rescueStatusLabel('NEW_SERVER_ENUM')).toBe('状态更新中')
  })

  it('uses safe fallbacks for unknown business values', () => {
    expect(rescueUrgencyLabel('UNKNOWN')).toBe('待确认')
    expect(submissionStatusLabel('UNKNOWN')).toBe('状态更新中')
  })

  it('hides technical exception details from users', () => {
    expect(userFacingError({ message: 'Spring AI HTTP 500 exception' }, '暂时不可用'))
      .toBe('暂时不可用')
    expect(userFacingError({ code: 'OCR_UNAVAILABLE', message: 'provider timeout' }, '识别失败'))
      .toBe('报告识别暂时不可用，你仍可手动填写')
  })
})
