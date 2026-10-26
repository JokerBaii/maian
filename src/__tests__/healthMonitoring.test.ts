import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { HealthMonitoringResponse } from '../api/monitoring'

const fetchMonitoring = vi.hoisted(() => vi.fn())
vi.mock('../api/monitoring', () => ({ getHealthMonitoring: fetchMonitoring }))

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>(done => { resolve = done })
  return { promise, resolve }
}

beforeEach(() => {
  vi.resetModules()
  fetchMonitoring.mockReset()
  const data = new Map<string, unknown>()
  vi.stubGlobal('uni', {
    getStorageSync: (key: string) => data.get(key),
    setStorageSync: (key: string, value: unknown) => data.set(key, value),
    removeStorageSync: (key: string) => data.delete(key)
  })
})
afterEach(() => vi.unstubAllGlobals())

async function setup() {
  const session = await import('../utils/authSession')
  const health = (await import('../composables/useHealthMonitoring')).useHealthMonitoring()
  const signIn = (account: string) => session.saveAccountSession(account, new Date(Date.now() + 3600_000).toISOString())
  signIn('account-a')
  const response = (current: number): HealthMonitoringResponse => ({ ...health.monitoring.value, current })
  return { ...session, ...health, signIn, response }
}

describe('account-scoped health monitoring', () => {
  it('clears readings and wearable state immediately on logout, ignoring an old response', async () => {
    const state = await setup(), request = deferred<HealthMonitoringResponse>()
    fetchMonitoring.mockResolvedValueOnce(state.response(85)).mockReturnValueOnce(request.promise)
    await state.loadMonitoring()
    state.updateWearable({ name: '个人手环', type: 'bluetooth', connected: true, battery: 80 })
    const pending = state.loadMonitoring(true), old = state.response(90)
    state.clearAccountSession()
    expect(state.monitoring.value.current).toBe(0)
    expect(state.monitoring.value.wearable.type).toBe('none')
    request.resolve(old)
    await pending
    expect(state.monitoring.value.current).toBe(0)
    expect(state.monitoring.value.wearable.type).toBe('none')
  })

  it('does not share an old account request or clear the new account request on completion', async () => {
    const state = await setup(), first = deferred<HealthMonitoringResponse>(), second = deferred<HealthMonitoringResponse>()
    fetchMonitoring.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise)
    const oldRequest = state.loadMonitoring(), oldData = state.response(150)
    state.signIn('account-b')
    const newRequest = state.loadMonitoring()
    first.resolve(oldData)
    await oldRequest
    expect(state.monitoring.value.current).toBe(0)
    const sharedRequest = state.loadMonitoring()
    expect(fetchMonitoring).toHaveBeenCalledTimes(2)
    second.resolve(state.response(72))
    await Promise.all([newRequest, sharedRequest])
    expect(state.monitoring.value.current).toBe(72)
  })

  it('keeps the latest forced refresh when responses arrive out of order', async () => {
    const state = await setup(), first = deferred<HealthMonitoringResponse>(), second = deferred<HealthMonitoringResponse>()
    fetchMonitoring.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise)
    const oldRequest = state.loadMonitoring(), newRequest = state.loadMonitoring(true)
    second.resolve(state.response(70))
    await newRequest
    first.resolve(state.response(140))
    await oldRequest
    expect(state.monitoring.value.current).toBe(70)
  })

  it('clears cached health data when an expired session is detected', async () => {
    const state = await setup()
    fetchMonitoring.mockResolvedValue(state.response(80))
    await state.loadMonitoring()
    vi.spyOn(Date, 'now').mockReturnValue(Date.now() + 3600_001)
    expect(state.getAccountAccessToken()).toBe('')
    expect(state.monitoring.value.current).toBe(0)
    vi.restoreAllMocks()
  })
})
