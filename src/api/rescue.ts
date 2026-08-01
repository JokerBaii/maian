import { request } from './http'

export type RescueUrgency = 'CRITICAL' | 'HIGH' | 'MEDIUM'
export type RescueStatus = 'PENDING' | 'MATCHING' | 'ACCEPTED' | 'RESCUING' | 'COMPLETED' | 'CANCELLED'

export interface CreateRescueCallRequest {
  urgency: RescueUrgency
  latitude: number
  longitude: number
  address: string
  description?: string
  symptoms: string[]
  imageUrls: string[]
  clientRequestId?: string
}

export interface RescueCallResponse {
  id: string
  urgency: RescueUrgency
  status: RescueStatus
  latitude: number
  longitude: number
  address: string
  description?: string
  symptoms: string[]
  imageUrls: string[]
  matchedAed?: {
    deviceId: string
    type: 'FIXED' | 'MOBILE'
    name: string
    category: string
    address: string
    longitude: number
    latitude: number
    ownerPhone?: string
    vehicleInfo?: string
    distanceMeters: number
    estimatedArrivalSeconds: number
    strategy: string
    matchedAt: string
  }
  createdAt: string
  updatedAt: string
}

interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export function createRescueCall(payload: CreateRescueCallRequest) {
  return request<RescueCallResponse>('/api/v1/rescue-calls', {
    method: 'POST',
    data: payload
  })
}

export function getRescueCall(id: string) {
  return request<RescueCallResponse>(`/api/v1/rescue-calls/${encodeURIComponent(id)}`)
}

export function retryRescueMatch(id: string) {
  return request<RescueCallResponse>(
    `/api/v1/rescue-calls/${encodeURIComponent(id)}/match-attempts`,
    { method: 'POST' }
  )
}

export function listRescueCalls() {
  return request<PageResponse<RescueCallResponse>>(
    '/api/v1/rescue-calls?page=0&size=100&sort=createdAt,desc'
  )
}
