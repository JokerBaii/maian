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

export interface ScienceContentResponse {
  id: string
  kind: 'ARTICLE' | 'VIDEO'
  category?: string
  title: string
  summary: string
  body?: string
  author: string
  coverPath?: string
  coverMediaId?: string
  coverUrl?: string
  mediaUrl?: string
  sourceName?: string
  sourcePage?: string
  durationLabel?: string
  viewCount: number
  likeCount: number
  collectCount: number
  publishedAt: string
}

async function normalizeContent(item: Omit<ScienceContentResponse, 'coverUrl'>) {
  const coverUrl = item.coverMediaId
    ? (await issueMediaDownload(item.coverMediaId)).url
    : item.coverPath
  return { ...item, coverUrl }
}

export async function listScienceContent(kind: 'ARTICLE' | 'VIDEO') {
  const content = await request<Array<Omit<ScienceContentResponse, 'coverUrl'>>>(
    `/api/v1/science-content?kind=${kind}`
  )
  return Promise.all(content.map(normalizeContent))
}

export async function getScienceContent(id: string) {
  return normalizeContent(await request<Omit<ScienceContentResponse, 'coverUrl'>>(
    `/api/v1/science-content/${encodeURIComponent(id)}`
  ))
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
