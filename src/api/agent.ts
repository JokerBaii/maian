import { request } from './http'

export interface CoordinationTask {
  id: string
  deviceId: string
  deviceName: string
  triggerLabel: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'ATTENTION' | 'FAILED'
  executionMode: 'PENDING' | 'RULES' | 'SPRING_AI' | 'RULE_FALLBACK'
  summary: string
  createdAt: string
  completedAt?: string
  steps: { module: string; summary: string; occurredAt: string }[]
}

export interface AssistantAction {
  kind: 'PAGE' | 'ARTICLE'
  title: string
  description: string
  url: string
}

export interface AssistantResult {
  reply: string
  executionMode: string
  actions: AssistantAction[]
  steps: string[]
  tasks?: AssistantTask[]
  continuation?: boolean
  conversation?: boolean
}

export interface AssistantContext { page?: string; deviceId?: string; rescueId?: string; reportId?: string; articleId?: string; contactId?: string; submissionId?: string; reminderId?: string; accountId?: string; settingKey?: string; documentId?: string }
export interface AssistantPending { operation: string; editableKeys: string[]; missingKeys: string[] }
export interface AssistantTurn { question: string; reply: string }

export interface AssistantTask { operation: string; parameters: Record<string, string> }

export function listMyCoordinationTasks() {
  return request<CoordinationTask[]>('/api/v1/coordination-tasks/mine')
}

export function retryCoordinationTask(id: string) {
  return request<boolean>(`/api/v1/coordination-tasks/${encodeURIComponent(id)}/retry`, { method: 'POST' })
}

export function listRescueCoordinationTasks(id: string) {
  return request<CoordinationTask[]>(`/api/v1/coordination-tasks/rescue/${encodeURIComponent(id)}`)
}

export function askAssistant(message: string, useModel: boolean, context?: AssistantContext, pending?: AssistantPending, history?: AssistantTurn[]) {
  return request<AssistantResult>('/api/v1/assistant', {
    method: 'POST', data: { message, useModel, context, pending, history }, timeout: 20000
  })
}
