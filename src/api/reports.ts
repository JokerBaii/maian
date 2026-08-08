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
  sourceMediaId?: string
  riskLevel: HealthRiskLevel
  summary: string
  indicators: HealthIndicatorRequest[]
  recommendations: string[]
  disclaimer: string
  analysisSource: 'RULE_BASED_V2' | 'SPRING_AI'
  createdAt: string
}

export interface CreateHealthReportRequest {
  checkupDate: string
  hospital: string
  sourceMediaId?: string
  indicators: HealthIndicatorRequest[]
}

export interface ReportRecognitionResponse {
  notice: string
  hospital: string
  checkupDate: string
  indicators: HealthIndicatorRequest[]
  rawLines: string[]
}

/** 上传原图后识别关键指标，返回结果需人工核对再保存。 */
export function recognizeHealthReport(mediaId: string) {
  return request<ReportRecognitionResponse>('/api/v1/health-reports/recognition', {
    method: 'POST',
    data: { mediaId },
    timeout: 20000
  })
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

export function deleteHealthReport(id: string) {
  return request<void>(`/api/v1/health-reports/${encodeURIComponent(id)}`, {
    method: 'DELETE'
  })
}
