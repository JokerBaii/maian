import { request } from './http'

export type RescueUrgency = 'CRITICAL' | 'HIGH' | 'MEDIUM'
export type RescueStatus =
  | 'PENDING' | 'MATCHING' | 'EN_ROUTE_TO_AED' | 'EN_ROUTE_TO_REQUESTER'
  | 'ARRIVED' | 'RESCUING' | 'PENDING_CONFIRMATION' | 'COMPLETED'
  | 'NO_RESOURCE' | 'EXPIRED' | 'USER_CANCELLED' | 'SYSTEM_FAILED'

export interface CreateRescueCallRequest {
  urgency: RescueUrgency
  latitude: number
  longitude: number
  address: string
  description?: string
  symptoms: string[]
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
  attachmentMediaIds: string[]
  matchedAed?: {
    deviceId: string
    type: 'FIXED' | 'MOBILE'
    name: string
    category: string
    address: string
    longitude: number
    latitude: number
    contactPhoneMasked?: string
    contactPhone?: string
    vehicleInfo?: string
    distanceMeters: number
    estimatedArrivalSeconds: number
    strategy: string
    matchedAt: string
    custodyStatus?: string
  }
  responderUserId?: string
  aedCustodyStatus?: 'RESERVED' | 'PICKUP_PENDING' | 'IN_CUSTODY' | 'AT_SCENE' | 'RETURNING' | 'RETURNED'
  liveTracking?: {
    responderLatitude: number
    responderLongitude: number
    reportedAt: string
    source: 'RESPONDER' | 'MOBILE_AED'
  }
  matchDeadlineAt?: string
  acceptedAt?: string
  arrivedAtAedAt?: string
  arrivedAt?: string
  rescueStartedAt?: string
  completionSubmittedAt?: string
  confirmationDeadlineAt?: string
  completedAt?: string
  aedReturnedAt?: string
  eventSequence: number
  createdAt: string
  updatedAt: string
}

export interface ResponderTaskResponse {
  id: string
  urgency: RescueUrgency
  status: RescueStatus
  detailAvailable: boolean
  distanceToRequesterMeters?: number
  latitude?: number
  longitude?: number
  address?: string
  description?: string
  symptoms: string[]
  attachmentMediaIds: string[]
  matchedAed?: RescueCallResponse['matchedAed']
  matchedAedType?: 'FIXED' | 'MOBILE'
  aedCustodyStatus?: RescueCallResponse['aedCustodyStatus']
  acceptedAt?: string
  arrivedAtAedAt?: string
  completionSubmittedAt?: string
  confirmationDeadlineAt?: string
  eventSequence: number
  createdAt: string
  updatedAt: string
}

export function attachRescueMedia(id: string, mediaId: string) {
  return request<RescueCallResponse>(`/api/v1/rescue-calls/${encodeURIComponent(id)}/attachments`, {
    method: 'POST',
    data: { mediaId }
  })
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

export function listRescueCalls() {
  return request<PageResponse<RescueCallResponse>>(
    '/api/v1/rescue-calls?page=0&size=100&sort=createdAt,desc'
  )
}

export function listResponderTasks() {
  return request<PageResponse<ResponderTaskResponse>>(
    '/api/v1/rescue-calls/responder-tasks?page=0&size=100&sort=createdAt,desc'
  )
}

export function getResponderTask(id: string) {
  return request<ResponderTaskResponse>(
    `/api/v1/rescue-calls/responder-tasks/${encodeURIComponent(id)}`
  )
}

export function acceptRescueTask(id: string) {
  return request<ResponderTaskResponse>(`/api/v1/rescue-calls/${encodeURIComponent(id)}/acceptance`, {
    method: 'POST'
  })
}

type ResponderAction = 'aed-arrival' | 'aed-pickup' | 'requester-arrival' | 'rescue-start' | 'completion-submission' | 'aed-return'

export function performResponderAction(id: string, action: ResponderAction) {
  return request<ResponderTaskResponse>(
    `/api/v1/rescue-calls/${encodeURIComponent(id)}/${action}`,
    { method: 'POST' }
  )
}

export function updateResponderPresence(payload: {
  latitude: number
  longitude: number
  available: boolean
}) {
  return request<void>('/api/v1/rescue-calls/responder-presence', {
    method: 'PUT',
    data: payload
  })
}

export function confirmRescueCompletion(id: string) {
  return request<RescueCallResponse>(
    `/api/v1/rescue-calls/${encodeURIComponent(id)}/completion-confirmation`,
    { method: 'POST' }
  )
}

/** 呼救方取消未结束的呼救（后端状态机只允许呼救者发起取消）。 */
export function cancelRescueCall(id: string) {
  return request<RescueCallResponse>(
    `/api/v1/rescue-calls/${encodeURIComponent(id)}/cancellation`,
    { method: 'POST' }
  )
}

export interface RescueFeedbackResponse {
  rescueCallId: string
  fromUserId: string
  toUserId?: string
  rating: number
  comment?: string
  createdAt: string
}

export function submitRescueFeedback(id: string, payload: { rating: number; comment?: string }) {
  return request<RescueFeedbackResponse>(
    `/api/v1/rescue-calls/${encodeURIComponent(id)}/feedback`,
    { method: 'POST', data: payload }
  )
}

export function getRescueFeedback(id: string) {
  return request<RescueFeedbackResponse>(`/api/v1/rescue-calls/${encodeURIComponent(id)}/feedback`)
}
