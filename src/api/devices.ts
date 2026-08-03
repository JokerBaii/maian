import { request } from './http'

export type EmergencyDeviceType = 'FIXED' | 'MOBILE'
export type EmergencyDeviceStatus = 'PENDING_REVIEW' | 'AVAILABLE' | 'RESERVED' | 'MAINTENANCE' | 'OFFLINE' | 'EXPIRED' | 'REJECTED'

export interface EmergencyDeviceResponse {
  id: string
  type: EmergencyDeviceType
  category: string
  name: string
  address: string
  longitude: number
  latitude: number
  status: EmergencyDeviceStatus
  ownerPhone?: string
  serviceTime?: string
  expireDate?: string
  owner?: string
  vehicleInfo?: string
  serviceRange?: number
  instructions?: string
  imageUrls: string[]
  vehicleImageUrls: string[]
  lastLocationAt?: string
  reviewNote?: string
  reviewedAt?: string
  createdAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export function listEmergencyDevices() {
  return request<PageResponse<EmergencyDeviceResponse>>(
    '/api/v1/emergency-devices?page=0&size=100&sort=createdAt,desc'
  )
}

export function listMyEmergencyDevices() {
  return request<PageResponse<EmergencyDeviceResponse>>(
    '/api/v1/emergency-devices/mine?page=0&size=100&sort=createdAt,desc'
  )
}

export function listPendingEmergencyDevices() {
  return request<PageResponse<EmergencyDeviceResponse>>(
    '/api/v1/emergency-devices/reviews/pending?page=0&size=100&sort=createdAt,asc'
  )
}

export function reviewEmergencyDevice(id: string, approved: boolean, reviewNote?: string) {
  return request<EmergencyDeviceResponse>(`/api/v1/emergency-devices/${id}/review`, {
    method: 'PATCH' as UniNamespace.RequestOptions['method'],
    data: { approved, reviewNote }
  })
}

export interface SaveEmergencyDeviceRequest {
  type: EmergencyDeviceType
  category: string
  name: string
  address: string
  longitude: number
  latitude: number
  ownerPhone?: string
  serviceTime?: string
  expireDate?: string
  owner?: string
  vehicleInfo?: string
  serviceRange?: number
  instructions?: string
  imageUrls: string[]
  vehicleImageUrls: string[]
}

export function getEmergencyDevice(id: string) {
  return request<EmergencyDeviceResponse>(`/api/v1/emergency-devices/${id}`)
}

export function createEmergencyDevice(data: SaveEmergencyDeviceRequest) {
  return request<EmergencyDeviceResponse>('/api/v1/emergency-devices', {
    method: 'POST',
    data
  })
}

export function updateEmergencyDevice(id: string, data: SaveEmergencyDeviceRequest) {
  return request<EmergencyDeviceResponse>(`/api/v1/emergency-devices/${id}`, {
    method: 'PUT',
    data
  })
}

export function updateEmergencyDeviceStatus(id: string, status: EmergencyDeviceStatus) {
  return request<EmergencyDeviceResponse>(`/api/v1/emergency-devices/${id}/status`, {
    method: 'PATCH' as UniNamespace.RequestOptions['method'],
    data: { status }
  })
}

export function updateEmergencyDeviceLocation(
  id: string,
  location: { longitude: number; latitude: number; address: string }
) {
  return request<EmergencyDeviceResponse>(`/api/v1/emergency-devices/${id}/location`, {
    method: 'PATCH' as UniNamespace.RequestOptions['method'],
    data: location
  })
}

export function deleteEmergencyDevice(id: string) {
  return request<boolean>(`/api/v1/emergency-devices/${id}`, {
    method: 'DELETE'
  })
}
