import { afterEach, describe, expect, it, vi } from 'vitest'
import { ensureHealthProcessingConsent } from '../utils/healthProcessingConsent'

const settings = { rescuePush: true, healthAlert: true, scienceUpdate: true, locationShare: false, healthDataShare: false, maxHeartRate: 130, minHeartRate: 50, updatedAt: 'now' }
afterEach(() => vi.unstubAllGlobals())

function setup(agreed: boolean) {
  const showModal = vi.fn(({ success }) => success({ confirm: agreed }))
  vi.stubGlobal('uni', { showModal })
  const request = vi.fn().mockResolvedValue({ ...settings })
  return { request, identity: () => 'owner', showModal }
}

describe('shared health processing consent', () => {
  it('skips another dialog or write when consent already exists', async () => {
    const deps = setup(false)
    deps.request.mockResolvedValue({ ...settings, healthDataShare: true })
    expect(await ensureHealthProcessingConsent(deps)).toBe(true)
    expect(deps.showModal).not.toHaveBeenCalled()
    expect(deps.request).toHaveBeenCalledOnce()
  })
  it('explains the actual provider and does not enable processing when declined', async () => {
    const deps = setup(false)
    expect(await ensureHealthProcessingConsent(deps)).toBe(false)
    expect(deps.showModal.mock.calls[0][0].content).toContain('百度智能云 OCR')
    expect(deps.showModal.mock.calls[0][0].content).toContain('后续')
    expect(deps.request).toHaveBeenCalledOnce()
  })
  it('changes only health consent using settings refreshed after the dialog', async () => {
    const deps = setup(true), latest = { ...settings, scienceUpdate: false, maxHeartRate: 140 }
    deps.request.mockResolvedValueOnce(settings).mockResolvedValueOnce(latest).mockResolvedValueOnce({ ...latest, healthDataShare: true })
    expect(await ensureHealthProcessingConsent(deps)).toBe(true)
    expect(deps.request.mock.calls[2]).toEqual(['/api/v1/settings', { method: 'PUT', data: {
      rescuePush: true, healthAlert: true, scienceUpdate: false, locationShare: false, healthDataShare: true, maxHeartRate: 140, minHeartRate: 50
    } }])
  })
  it('does not enable processing for a different account after the dialog', async () => {
    const deps = setup(true)
    let owner = 'account-a'
    deps.identity = () => owner
    deps.showModal.mockImplementation(({ success }) => { owner = 'account-b'; success({ confirm: true }) })
    await expect(ensureHealthProcessingConsent(deps)).rejects.toThrow('账号已变化')
    expect(deps.request).toHaveBeenCalledOnce()
  })
})
