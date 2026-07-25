import { request } from './http'

export interface ScienceSubmissionResponse {
  id: string
  title: string
  category: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  hasCoverImage: boolean
  coverImageUrl?: string
  submittedAt: string
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
