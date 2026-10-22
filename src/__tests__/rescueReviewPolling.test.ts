import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { RescueReview } from '../api/rescueReview'
import { createRescueReviewPolling } from '../utils/rescueReviewPolling'

const report = (status: RescueReview['status'], options: Partial<RescueReview> = {}) => ({
  rescueCallId: 'call', status, stale: false, steps: [], articles: [], ...options
} as RescueReview)

describe('rescue review polling', () => {
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => { vi.clearAllTimers(); vi.useRealTimers() })

  it('receives background progress and a completed review, then stops when there are no pending actions', async () => {
    const fetch = vi.fn().mockResolvedValueOnce(report('PENDING')).mockResolvedValueOnce(report('RUNNING'))
      .mockResolvedValue(report('COMPLETED'))
    const publish = vi.fn()
    const polling = createRescueReviewPolling(fetch, publish, vi.fn())
    polling.start()
    await vi.advanceTimersByTimeAsync(6000)
    expect(publish).toHaveBeenLastCalledWith(report('COMPLETED'))
    await vi.advanceTimersByTimeAsync(9000)
    expect(fetch).toHaveBeenCalledTimes(3)
  })

  it('watches pending follow-ups until the stored review becomes stale', async () => {
    const data = { returnPending: true, feedbackPending: true } as NonNullable<RescueReview['data']>
    const fetch = vi.fn().mockResolvedValueOnce(report('COMPLETED', { data }))
      .mockResolvedValue(report('COMPLETED', { data, stale: true }))
    const publish = vi.fn()
    const polling = createRescueReviewPolling(fetch, publish, vi.fn())
    polling.start()
    await vi.advanceTimersByTimeAsync(3000)
    expect(publish).toHaveBeenLastCalledWith(report('COMPLETED', { data, stale: true }))
    await vi.advanceTimersByTimeAsync(9000)
    expect(fetch).toHaveBeenCalledTimes(2)
  })

  it('retries a network failure and lets an absent review remain user initiated', async () => {
    const fetch = vi.fn().mockRejectedValueOnce(new Error('offline')).mockResolvedValue(null)
    const onError = vi.fn(), publish = vi.fn()
    const polling = createRescueReviewPolling(fetch, publish, onError)
    polling.start()
    await vi.advanceTimersByTimeAsync(3000)
    expect(onError).toHaveBeenCalledTimes(1)
    expect(publish).toHaveBeenCalledWith(null)
    await vi.advanceTimersByTimeAsync(9000)
    expect(fetch).toHaveBeenCalledTimes(2)
  })

  it('drops hidden-page responses and can restart for a newly requested review', async () => {
    let finish: (value: RescueReview) => void = () => {}
    const fetch = vi.fn().mockReturnValueOnce(new Promise<RescueReview>(resolve => { finish = resolve }))
      .mockResolvedValue(report('FAILED'))
    const publish = vi.fn()
    const polling = createRescueReviewPolling(fetch, publish, vi.fn())
    polling.start(); polling.stop(); polling.start()
    await vi.advanceTimersByTimeAsync(0)
    finish(report('RUNNING'))
    await vi.advanceTimersByTimeAsync(9000)
    expect(publish).toHaveBeenCalledTimes(1)
    expect(publish).toHaveBeenLastCalledWith(report('FAILED'))
    expect(fetch).toHaveBeenCalledTimes(2)
  })
})
