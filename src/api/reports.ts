import { request } from './http'

export type HealthRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'

export interface HealthIndicatorRequest {
  name: string
  value: string
  unit?: string
  referenceRange?: string
  abnormal: boolean
}

export interface HealthReportResponse {
  id: string
  checkupDate: string
  hospital: string
  sourceImageUrl?: string
  riskLevel: HealthRiskLevel
  summary: string
  indicators: HealthIndicatorRequest[]
  recommendations: string[]
  disclaimer: string
  analysisSource: 'RULE_BASED' | 'SPRING_AI'
  createdAt: string
}

export interface CreateHealthReportRequest {
  checkupDate: string
  hospital: string
  sourceImageUrl?: string
  indicators: HealthIndicatorRequest[]
}

export function createHealthReport(payload: CreateHealthReportRequest) {
  return request<HealthReportResponse>('/api/v1/health-reports', {
    method: 'POST',
    data: payload,
    timeout: 15000
  })
}

export function listHealthReports() {
  return request<HealthReportResponse[]>('/api/v1/health-reports')
}

export function getHealthReport(id: string) {
  return request<HealthReportResponse>(`/api/v1/health-reports/${encodeURIComponent(id)}`)
}
