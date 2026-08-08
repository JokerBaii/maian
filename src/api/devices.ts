import { request } from './http'
import { issueMediaDownload } from './files'
import {
  toServiceWindows,
  type DeviceServiceWindow,
  type WeekDay
} from '@/utils/deviceSchedule'

export type { DeviceServiceWindow, WeekDay } from '@/utils/deviceSchedule'

export type EmergencyDeviceType = 'FIXED' | 'MOBILE'
export type EmergencyDeviceStatus = 'PENDING_REVIEW' | 'AVAILABLE' | 'RESERVED' | 'DISABLED' | 'EXPIRED' | 'REJECTED'

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
  serviceWindows: DeviceServiceWindow[]
  expireDate?: string
  owner?: string
  vehicleInfo?: string
  serviceRange?: number
  instructions?: string
  imageUrls: string[]
  vehicleImageUrls: string[]
  imageMediaIds: string[]
  vehicleImageMediaIds: string[]
  lastLocationAt?: string
  mobilePresenceStatus?: 'ONLINE' | 'STALE' | 'OFFLINE'
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

type EmergencyDeviceWireResponse = Omit<EmergencyDeviceResponse, 'imageUrls' | 'vehicleImageUrls'>

async function resolveMediaUrls(mediaIds: string[]) {
  return Promise.all(mediaIds.map(async mediaId => (await issueMediaDownload(mediaId)).url))
}

async function normalizeDevice(device: EmergencyDeviceWireResponse): Promise<EmergencyDeviceResponse> {
  const [imageUrls, vehicleImageUrls] = await Promise.all([
    resolveMediaUrls(device.imageMediaIds || []),
    resolveMediaUrls(device.vehicleImageMediaIds || [])
  ])
  return { ...device, imageUrls, vehicleImageUrls }
}

async function normalizePage(page: PageResponse<EmergencyDeviceWireResponse>) {
  return { ...page, content: await Promise.all(page.content.map(normalizeDevice)) }
}

export async function listEmergencyDevices() {
  return normalizePage(await request<PageResponse<EmergencyDeviceWireResponse>>(
    '/api/v1/emergency-devices?page=0&size=100&sort=createdAt,desc'
  ))
}

export async function listMyEmergencyDevices() {
  return normalizePage(await request<PageResponse<EmergencyDeviceWireResponse>>(
    '/api/v1/emergency-devices/mine?page=0&size=100&sort=createdAt,desc'
  ))
}

export async function listPendingEmergencyDevices() {
  return normalizePage(await request<PageResponse<EmergencyDeviceWireResponse>>(
    '/api/v1/emergency-devices/reviews/pending?page=0&size=100&sort=createdAt,asc'
  ))
}

export async function reviewEmergencyDevice(id: string, approved: boolean, reviewNote?: string) {
  return normalizeDevice(await request<EmergencyDeviceWireResponse>(`/api/v1/emergency-devices/${id}/review`, {
    method: 'PATCH' as UniNamespace.RequestOptions['method'],
    data: { approved, reviewNote }
  }))
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

export async function getEmergencyDevice(id: string) {
  return normalizeDevice(await request<EmergencyDeviceWireResponse>(`/api/v1/emergency-devices/mine/${id}`))
}

function toServerPayload(data: SaveEmergencyDeviceRequest) {
  const { serviceTime, imageUrls, vehicleImageUrls, ...fields } = data
  return {
    ...fields,
    serviceWindows: toServiceWindows(serviceTime),
    imageMediaIds: imageUrls.map(toMediaId),
    vehicleImageMediaIds: vehicleImageUrls.map(toMediaId)
  }
}

function toMediaId(value: string) {
  if (/^[0-9a-f]{8}-[0-9a-f-]{27}$/i.test(value)) return value
  const match = value.match(/\/api\/v1\/media\/([0-9a-f-]{36})\/content/i)
  if (!match) throw new Error('设备图片尚未完成上传')
  return match[1]
}

export async function createEmergencyDevice(data: SaveEmergencyDeviceRequest) {
  return normalizeDevice(await request<EmergencyDeviceWireResponse>('/api/v1/emergency-devices', {
    method: 'POST',
    data: toServerPayload(data)
  }))
}

export async function updateEmergencyDevice(id: string, data: SaveEmergencyDeviceRequest) {
  return normalizeDevice(await request<EmergencyDeviceWireResponse>(`/api/v1/emergency-devices/${id}`, {
    method: 'PUT',
    data: toServerPayload(data)
  }))
}

export async function enableEmergencyDevice(id: string) {
  return normalizeDevice(await request<EmergencyDeviceWireResponse>(`/api/v1/emergency-devices/${id}/enable`, {
    method: 'POST'
  }))
}

export async function disableEmergencyDevice(id: string) {
  return normalizeDevice(await request<EmergencyDeviceWireResponse>(`/api/v1/emergency-devices/${id}/disable`, {
    method: 'POST'
  }))
}

export async function updateEmergencyDeviceLocation(
  id: string,
  location: { longitude: number; latitude: number; address: string }
) {
  return normalizeDevice(await request<EmergencyDeviceWireResponse>(`/api/v1/emergency-devices/${id}/location`, {
    method: 'PATCH' as UniNamespace.RequestOptions['method'],
    data: location
  }))
}

export function deleteEmergencyDevice(id: string) {
  return request<boolean>(`/api/v1/emergency-devices/${id}`, {
    method: 'DELETE'
  })
}
