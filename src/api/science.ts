import { request } from './http'

export interface ScienceSubmissionResponse {
  id: string
  title: string
  category: string
  content: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  hasCoverImage: boolean
  coverImageUrl?: string
  submittedAt: string
  reviewNote?: string
  reviewedAt?: string
}

export interface ScienceArticleInteractionResponse {
  articleId: string
  liked: boolean
  collected: boolean
  updatedAt?: string
}

export function getScienceArticleInteraction(articleId: string) {
  return request<ScienceArticleInteractionResponse>(
    `/api/v1/science-articles/${encodeURIComponent(articleId)}/interaction`
  )
}

export function updateScienceArticleInteraction(
  articleId: string,
  data: { liked: boolean; collected: boolean }
) {
  return request<ScienceArticleInteractionResponse>(
    `/api/v1/science-articles/${encodeURIComponent(articleId)}/interaction`,
    { method: 'PUT', data }
  )
}

export function createScienceSubmission(data: {
  title: string
  category: string
  content: string
  coverImageUrl?: string
}) {
  return request<ScienceSubmissionResponse>('/api/v1/science-submissions', {
    method: 'POST',
    data
  })
}

export async function getScienceSubmissionCount() {
  return request<{ count: number }>('/api/v1/science-submissions/count')
}

/** 当前用户的投稿列表，按提交时间倒序。 */
export function listScienceSubmissions() {
  return request<{
    content: ScienceSubmissionResponse[]
    page: number
    size: number
    totalElements: number
    totalPages: number
    first: boolean
    last: boolean
  }>('/api/v1/science-submissions?page=0&size=100&sort=submittedAt,desc')
}

export function getScienceSubmission(id: string) {
  return request<ScienceSubmissionResponse>(`/api/v1/science-submissions/${encodeURIComponent(id)}`)
}

export function deleteScienceSubmission(id: string) {
  return request<void>(`/api/v1/science-submissions/${encodeURIComponent(id)}`, { method: 'DELETE' })
}

/** 已审核通过的投稿，用于科普频道展示。 */
export function listApprovedScienceSubmissions() {
  return request<{
    content: ScienceSubmissionResponse[]
    page: number
    size: number
    totalElements: number
    totalPages: number
    first: boolean
    last: boolean
  }>('/api/v1/science-submissions/approved?page=0&size=100&sort=submittedAt,desc')
}

export function listPendingScienceSubmissions() {
  return request<{
    content: ScienceSubmissionResponse[]
    page: number
    size: number
    totalElements: number
    totalPages: number
    first: boolean
    last: boolean
  }>('/api/v1/science-submissions/reviews/pending?page=0&size=100&sort=submittedAt,asc')
}

export function reviewScienceSubmission(id: string, approved: boolean, reviewNote?: string) {
  return request<ScienceSubmissionResponse>(
    `/api/v1/science-submissions/${encodeURIComponent(id)}/review`,
    {
      method: 'PATCH' as UniNamespace.RequestOptions['method'],
      data: { approved, reviewNote }
    }
  )
}
