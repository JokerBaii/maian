import { request } from './http'

export interface RescueReview {
  rescueCallId: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED'
  executionMode: 'PENDING' | 'RULES' | 'SPRING_AI' | 'RULE_FALLBACK'
  summary: string
  stale: boolean
  updatedAt: string
  completedAt?: string
  steps: { module: string; summary: string; occurredAt: string }[]
  data?: {
    metrics: { label: string; seconds: number | null }[]
    timeline: { type: string; summary: string; occurredAt: string }[]
    returnPending: boolean
    feedbackPending: boolean
    custodyStatus?: string
    articleIds: string[]
  }
  articles: { id: string; title: string; summary: string }[]
}

export async function getRescueReview(id: string) {
  return await request<RescueReview | null>(`/api/v1/rescue-reviews/${encodeURIComponent(id)}`) ?? null
}

export function requestRescueReview(id: string, useModel: boolean) {
  return request<RescueReview>(`/api/v1/rescue-reviews/${encodeURIComponent(id)}`, { method: 'POST', data: { useModel } })
}
