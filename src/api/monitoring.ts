import { request } from './http'

export interface HeartRatePoint {
  time: string
  value: number
  scene: 'resting' | 'exercise' | 'sleeping'
}

export interface DailyHeartRate {
  date: string
  avg: number
  min: number
  max: number
}

export interface HeartRateAlert {
  time: string
  value: number
  type: 'high' | 'low'
  message: string
}

export interface WearableDevice {
  id?: string
  deviceIdentifier?: string
  name: string
  type: string
  connected: boolean
  battery: number
}

export interface HealthMonitoringResponse {
  current: number
  min: number
  max: number
  avg: number
  status: string
  scene: string
  todayData: HeartRatePoint[]
  weekData: DailyHeartRate[]
  monthData: DailyHeartRate[]
  alerts: HeartRateAlert[]
  wearable: WearableDevice
  minHeartRate: number
  maxHeartRate: number
}

export function getHealthMonitoring() {
  return request<HealthMonitoringResponse>('/api/v1/health-monitoring')
}

export function getWearableDevice() {
  return request<WearableDevice>('/api/v1/wearable-device')
}

export function saveWearableDevice(data: {
  deviceIdentifier: string
  name: string
  type: string
  connected: boolean
  battery?: number
}) {
  return request<WearableDevice>('/api/v1/wearable-device', {
    method: 'PUT',
    data
  })
}

export function deleteWearableDevice() {
  return request<void>('/api/v1/wearable-device', { method: 'DELETE' })
}

export function createHeartRateReading(data: {
  bpm: number
  scene: 'resting' | 'exercise' | 'sleeping'
  recordedAt?: string
}) {
  return request<{ id: number; bpm: number; scene: string; recordedAt: string }>(
    '/api/v1/heart-rate-readings',
    { method: 'POST', data }
  )
}
