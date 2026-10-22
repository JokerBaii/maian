import type { RescueReview } from '@/api/rescueReview'

export function createRescueReviewPolling(
  fetchReview: () => Promise<RescueReview | null>,
  publish: (review: RescueReview | null) => void,
  onError: (error: unknown) => void
) {
  let active = false
  let generation = 0
  let timer: ReturnType<typeof setTimeout> | null = null
  async function refresh(run: number) {
    let again = true
    try {
      const review = await fetchReview()
      if (!active || generation !== run) return
      publish(review)
      again = !!review && (['PENDING', 'RUNNING'].includes(review.status)
        || (review.status === 'COMPLETED' && !review.stale
          && !!(review.data?.returnPending || review.data?.feedbackPending)))
    } catch (error) { if (active && generation === run) onError(error) }
    if (!active || generation !== run) return
    if (again) timer = setTimeout(() => { timer = null; void refresh(run) }, 3000)
    else active = false
  }
  return {
    start() { if (!active) { active = true; void refresh(++generation) } },
    stop() { active = false; generation++; if (timer) clearTimeout(timer); timer = null }
  }
}
