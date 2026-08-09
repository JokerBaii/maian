import { request } from './http'
import type { PageResponse } from './devices'

export type AccountRole = 'USER' | 'VOLUNTEER' | 'ADMIN'

export interface AdminUserResponse {
  id: string
  nickname: string
  phone: string
  role: AccountRole
  verified: boolean
  createdAt: string
}

export function listAccounts() {
  return request<PageResponse<AdminUserResponse>>('/api/v1/admin/users?page=0&size=100')
}

export function updateAccountRole(id: string, role: AccountRole) {
  return request<AdminUserResponse>(`/api/v1/admin/users/${id}/role`, {
    method: 'PATCH' as UniNamespace.RequestOptions['method'],
    data: { role }
  })
}
