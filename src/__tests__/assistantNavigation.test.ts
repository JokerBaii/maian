import { describe, expect, it } from 'vitest'
import { isAssistantDestination } from '../utils/assistantNavigation'

describe('assistant action destinations', () => {
  it('accepts settings sections and article cards', () => {
    expect(isAssistantDestination('/pages/mine/settings?section=privacy')).toBe(true)
    expect(isAssistantDestination('/pages/science/detail?id=submission-verified')).toBe(true)
    expect(isAssistantDestination('/pages/agent/index')).toBe(true)
  })
  it('rejects external links and privileged or mutating destinations', () => {
    for (const url of ['https://example.com', 'javascript:alert(1)', '/pages/admin/accounts', '/pages/device/add', '/pages/rescue/index', '//pages/mine/settings']) {
      expect(isAssistantDestination(url)).toBe(false)
    }
  })
})
