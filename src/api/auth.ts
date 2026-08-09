import { ApiRequestError, type ApiResponse, resolveApiUrl } from './http'
import { saveAccountSession } from '@/utils/authSession'

interface AuthTokenResponse {
  accessToken: string
  expiresAt: string
}

function publicPost(path: string, data: Record<string, string>): Promise<AuthTokenResponse> {
  return new Promise((resolve, reject) => {
    uni.request({
      url: resolveApiUrl(path),
      method: 'POST',
      data,
      header: { Accept: 'application/json', 'Content-Type': 'application/json' },
      success(response) {
        const body = response.data as ApiResponse<AuthTokenResponse>
        if (response.statusCode >= 200 && response.statusCode < 300 && body.success && body.data) {
          resolve(body.data)
          return
        }
        reject(new ApiRequestError(
          body.error?.message || '登录失败',
          body.error?.code || 'AUTH_FAILED',
          body.error?.details || [],
          response.statusCode
        ))
      },
      fail(error) {
        reject(new ApiRequestError(error.errMsg || '网络连接失败', 'NETWORK_ERROR'))
      }
    })
  })
}

export async function loginWithPassword(phone: string, password: string) {
  const auth = await publicPost('/api/v1/auth/login', { phone, password })
  saveAccountSession(auth.accessToken, auth.expiresAt)
}

export async function registerAccount(phone: string, password: string, nickname: string) {
  const auth = await publicPost('/api/v1/auth/register', { phone, password, nickname })
  saveAccountSession(auth.accessToken, auth.expiresAt)
}
