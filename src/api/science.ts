import { request } from './http'
import { issueMediaDownload } from './files'

export interface ScienceSubmissionResponse {
  id: string
  title: string
  category: string
  content: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  hasCoverImage: boolean
  coverMediaId?: string
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

type ScienceSubmissionWireResponse = Omit<ScienceSubmissionResponse, 'coverImageUrl'>
type SciencePage = {
  content: ScienceSubmissionWireResponse[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

async function normalizeSubmission(item: ScienceSubmissionWireResponse): Promise<ScienceSubmissionResponse> {
  const coverImageUrl = item.coverMediaId
    ? (await issueMediaDownload(item.coverMediaId)).url
    : undefined
  return { ...item, coverImageUrl }
}

async function normalizePage(page: SciencePage) {
  return { ...page, content: await Promise.all(page.content.map(normalizeSubmission)) }
}

export async function createScienceSubmission(data: {
  title: string
  category: string
  content: string
  coverMediaId?: string
}) {
  return normalizeSubmission(await request<ScienceSubmissionWireResponse>('/api/v1/science-submissions', {
    method: 'POST',
    data
  }))
}

export async function getScienceSubmissionCount() {
  return request<{ count: number }>('/api/v1/science-submissions/count')
}

/** 当前用户的投稿列表，按提交时间倒序。 */
export async function listScienceSubmissions() {
  return normalizePage(await request<SciencePage>(
    '/api/v1/science-submissions?page=0&size=100&sort=submittedAt,desc'
  ))
}

export async function getScienceSubmission(id: string) {
  return normalizeSubmission(await request<ScienceSubmissionWireResponse>(
    `/api/v1/science-submissions/${encodeURIComponent(id)}`
  ))
}

export function deleteScienceSubmission(id: string) {
  return request<void>(`/api/v1/science-submissions/${encodeURIComponent(id)}`, { method: 'DELETE' })
}

/** 已审核通过的投稿，用于科普频道展示。 */
export async function listApprovedScienceSubmissions() {
  return normalizePage(await request<SciencePage>(
    '/api/v1/science-submissions/approved?page=0&size=100&sort=submittedAt,desc'
  ))
}

export async function listPendingScienceSubmissions() {
  return normalizePage(await request<SciencePage>(
    '/api/v1/science-submissions/reviews/pending?page=0&size=100&sort=submittedAt,asc'
  ))
}

export async function reviewScienceSubmission(id: string, approved: boolean, reviewNote?: string) {
  return normalizeSubmission(await request<ScienceSubmissionWireResponse>(
    `/api/v1/science-submissions/${encodeURIComponent(id)}/review`,
    {
      method: 'PATCH' as UniNamespace.RequestOptions['method'],
      data: { approved, reviewNote }
    }
  ))
}
