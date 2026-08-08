import {
  clearDemoAccessToken,
  getDemoAccessToken,
  getDemoUserId,
  saveDemoAccessToken
} from '@/utils/demoSession'

export interface ApiError {
  code: string
  message: string
  details: string[]
}

export interface ApiResponse<T> {
  success: boolean
  data?: T
  error?: ApiError
  timestamp: string
}

export class ApiRequestError extends Error {
  code: string
  details: string[]
  statusCode?: number

  constructor(message: string, code = 'REQUEST_FAILED', details: string[] = [], statusCode?: number) {
    super(message)
    this.name = 'ApiRequestError'
    this.code = code
    this.details = details
    this.statusCode = statusCode
  }
}

const apiBaseUrl = ((import.meta as any).env?.VITE_API_BASE_URL || '').replace(/\/$/, '')

export function resolveApiUrl(path: string) {
  if (/^(?:[a-z]+:)?\/\//i.test(path) || path.startsWith('data:') || path.startsWith('blob:')) {
    return path
  }
  const resolvedPath = `${apiBaseUrl}${path}`
  if (typeof window !== 'undefined') {
    return new URL(resolvedPath, window.location.origin).toString()
  }
  return resolvedPath
}

export function request<T>(
  path: string,
  options: {
    method?: UniNamespace.RequestOptions['method']
    data?: UniNamespace.RequestOptions['data']
    timeout?: number
  } = {}
): Promise<T> {
  return authenticatedRequest(path, options, true)
}

async function authenticatedRequest<T>(
  path: string,
  options: {
    method?: UniNamespace.RequestOptions['method']
    data?: UniNamespace.RequestOptions['data']
    timeout?: number
  },
  allowRefresh: boolean
): Promise<T> {
  const accessToken = await ensureAccessToken()
  return new Promise((resolve, reject) => {
    uni.request({
      url: resolveApiUrl(path),
      method: options.method || 'GET',
      data: options.data,
      timeout: options.timeout || 10000,
      header: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`
      },
      async success(response: UniNamespace.RequestSuccessCallbackResult) {
        if (response.statusCode === 204) {
          resolve(undefined as T)
          return
        }
        const body = response.data as unknown as ApiResponse<T>
        if (response.statusCode >= 200 && response.statusCode < 300 && body.success && body.data !== undefined) {
          resolve(body.data)
          return
        }

        if (response.statusCode === 401 && allowRefresh) {
          clearDemoAccessToken()
          try {
            resolve(await authenticatedRequest<T>(path, options, false))
          } catch (error) {
            reject(error)
          }
          return
        }

        reject(new ApiRequestError(
          body.error?.message || `请求失败（${response.statusCode}）`,
          body.error?.code,
          body.error?.details,
          response.statusCode
        ))
      },
      fail(error: UniNamespace.GeneralCallbackResult) {
        reject(new ApiRequestError(error.errMsg || '网络连接失败，请稍后重试', 'NETWORK_ERROR'))
      }
    })
  })
}

export async function ensureAccessToken(): Promise<string> {
  const existing = getDemoAccessToken()
  if (existing) return existing

  const response = await new Promise<UniNamespace.RequestSuccessCallbackResult>((resolve, reject) => {
    uni.request({
      url: resolveApiUrl('/api/v1/auth/demo'),
      method: 'POST',
      data: { userId: getDemoUserId() },
      header: { 'Content-Type': 'application/json', Accept: 'application/json' },
      success: resolve,
      fail: reject
    })
  })
  const body = response.data as ApiResponse<{ accessToken: string; expiresAt: string }>
  if (response.statusCode < 200 || response.statusCode >= 300 || !body.success || !body.data) {
    throw new ApiRequestError(
      body.error?.message || '登录失败',
      body.error?.code || 'AUTH_FAILED',
      body.error?.details || [],
      response.statusCode
    )
  }
  saveDemoAccessToken(body.data.accessToken, body.data.expiresAt)
  return body.data.accessToken
}
