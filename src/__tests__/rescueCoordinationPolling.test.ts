import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { CoordinationTask } from '../api/agent'
import { createRescueCoordinationPolling } from '../utils/rescueCoordinationPolling'

const tasks = (status: CoordinationTask['status']) => [{ id: 'task', status } as CoordinationTask]

describe('rescue coordination follow-up polling', () => {
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => { vi.clearAllTimers(); vi.useRealTimers() })

  it('receives late completion after the rescue is terminal and then stops', async () => {
    const fetch = vi.fn().mockResolvedValueOnce(tasks('ATTENTION')).mockResolvedValue(tasks('COMPLETED'))
    const publish = vi.fn()
    const polling = createRescueCoordinationPolling(fetch, publish, () => ({ status: 'NO_RESOURCE' }))
    polling.start()
    await vi.advanceTimersByTimeAsync(0)
    expect(publish).toHaveBeenLastCalledWith(tasks('ATTENTION'))
    await vi.advanceTimersByTimeAsync(3000)
    expect(publish).toHaveBeenLastCalledWith(tasks('COMPLETED'))
    await vi.advanceTimersByTimeAsync(9000)
    expect(fetch).toHaveBeenCalledTimes(2)
  })

  it('retries a failed request even after rescue polling has stopped', async () => {
    const fetch = vi.fn().mockRejectedValueOnce(new Error('offline')).mockResolvedValue(tasks('COMPLETED'))
    const publish = vi.fn()
    const polling = createRescueCoordinationPolling(fetch, publish, () => ({ status: 'SYSTEM_FAILED' }))
    polling.start()
    await vi.advanceTimersByTimeAsync(3000)
    expect(fetch).toHaveBeenCalledTimes(2)
    expect(publish).toHaveBeenLastCalledWith(tasks('COMPLETED'))
    polling.stop()
  })

  it('ignores a hidden page request and restarts without stale results overwriting the new result', async () => {
    let finishOld: (value: CoordinationTask[]) => void = () => {}
    const pending = new Promise<CoordinationTask[]>(resolve => { finishOld = resolve })
    const fetch = vi.fn().mockReturnValueOnce(pending).mockResolvedValue(tasks('COMPLETED'))
    const publish = vi.fn()
    const polling = createRescueCoordinationPolling(fetch, publish, () => ({ status: 'COMPLETED' }))
    polling.start()
    polling.stop()
    polling.start()
    await vi.advanceTimersByTimeAsync(0)
    finishOld(tasks('RUNNING'))
    await vi.advanceTimersByTimeAsync(9000)
    expect(publish).toHaveBeenCalledTimes(1)
    expect(publish).toHaveBeenLastCalledWith(tasks('COMPLETED'))
    expect(fetch).toHaveBeenCalledTimes(2)
  })

  it('keeps following a completed rescue while the AED is still being returned', async () => {
    let custody: 'RETURNING' | 'RETURNED' = 'RETURNING'
    const fetch = vi.fn().mockResolvedValue([])
    const polling = createRescueCoordinationPolling(fetch, vi.fn(),
      () => ({ status: 'COMPLETED', aedCustodyStatus: custody }))
    polling.start()
    await vi.advanceTimersByTimeAsync(3000)
    expect(fetch).toHaveBeenCalledTimes(2)
    custody = 'RETURNED'
    await vi.advanceTimersByTimeAsync(3000)
    await vi.advanceTimersByTimeAsync(9000)
    expect(fetch).toHaveBeenCalledTimes(3)
  })

  it('does not overlap slow requests or stop watching an active rescue with no records yet', async () => {
    let finish: (value: CoordinationTask[]) => void = () => {}
    const fetch = vi.fn().mockReturnValueOnce(new Promise<CoordinationTask[]>(resolve => { finish = resolve }))
      .mockResolvedValue(tasks('PENDING'))
    const polling = createRescueCoordinationPolling(fetch, vi.fn(), () => ({ status: 'MATCHING' }))
    polling.start(); polling.start()
    await vi.advanceTimersByTimeAsync(6000)
    expect(fetch).toHaveBeenCalledTimes(1)
    finish([])
    await vi.advanceTimersByTimeAsync(3000)
    expect(fetch).toHaveBeenCalledTimes(2)
    polling.stop()
    await vi.advanceTimersByTimeAsync(9000)
    expect(fetch).toHaveBeenCalledTimes(2)
  })

  it('does a final fresh read when rescue ends during an older empty request', async () => {
    let state: 'MATCHING' | 'NO_RESOURCE' = 'MATCHING'
    let finish: (value: CoordinationTask[]) => void = () => {}
    const fetch = vi.fn().mockReturnValueOnce(new Promise<CoordinationTask[]>(resolve => { finish = resolve }))
      .mockResolvedValueOnce(tasks('RUNNING')).mockResolvedValue(tasks('COMPLETED'))
    const publish = vi.fn()
    const polling = createRescueCoordinationPolling(fetch, publish, () => ({ status: state }))
    polling.start()
    state = 'NO_RESOURCE'
    finish([])
    await vi.advanceTimersByTimeAsync(3000)
    expect(publish).toHaveBeenLastCalledWith(tasks('RUNNING'))
    await vi.advanceTimersByTimeAsync(3000)
    expect(publish).toHaveBeenLastCalledWith(tasks('COMPLETED'))
    await vi.advanceTimersByTimeAsync(9000)
    expect(fetch).toHaveBeenCalledTimes(3)
  })
})
