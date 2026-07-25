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
}

export function getHealthMonitoring() {
  return request<HealthMonitoringResponse>('/api/v1/health-monitoring')
}
