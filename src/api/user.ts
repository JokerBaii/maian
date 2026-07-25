import { request } from './http'

export interface UserProfileResponse {
  id: string
  nickname: string
  phone: string
  role: string
  realName?: string
  idCard?: string
  verified: boolean
  createdAt: string
}

export interface EmergencyContactResponse {
  id: string
  name: string
  phone: string
  relation: string
  createdAt: string
}

export interface SaveEmergencyContactRequest {
  name: string
  phone: string
  relation: string
}

export function getCurrentProfile() {
  return request<UserProfileResponse>('/api/v1/profile')
}

export function verifyIdentity(realName: string, idCard: string) {
  return request<UserProfileResponse>('/api/v1/profile/identity-verification', {
    method: 'POST',
    data: { realName, idCard }
  })
}

export function listEmergencyContacts() {
  return request<EmergencyContactResponse[]>('/api/v1/emergency-contacts')
}

export function createEmergencyContact(data: SaveEmergencyContactRequest) {
  return request<EmergencyContactResponse>('/api/v1/emergency-contacts', {
    method: 'POST',
    data
  })
}

export function updateEmergencyContact(id: string, data: SaveEmergencyContactRequest) {
  return request<EmergencyContactResponse>(`/api/v1/emergency-contacts/${id}`, {
    method: 'PUT',
    data
  })
}

export function deleteEmergencyContact(id: string) {
  return request<boolean>(`/api/v1/emergency-contacts/${id}`, {
    method: 'DELETE'
  })
}
