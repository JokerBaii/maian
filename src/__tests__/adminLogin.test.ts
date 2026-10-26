import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { loginPlatformAdmin, loginWithPassword } from '../api/auth'
import { clearAccountSession, saveAccountSession } from '../utils/authSession'
import { getCurrentProfile } from '../api/user'

vi.mock('../utils/authSession', () => ({
  clearAccountSession: vi.fn(),
  saveAccountSession: vi.fn()
}))
vi.mock('../api/user', () => ({ getCurrentProfile: vi.fn() }))

beforeEach(() => {
  vi.clearAllMocks()
  vi.stubGlobal('uni', {
    request: vi.fn(({ success }) => success({
      statusCode: 200,
      data: { success: true, data: { accessToken: 'test-token', expiresAt: '2026-12-01T00:00:00Z' } }
    }))
  })
})
afterEach(() => vi.unstubAllGlobals())

describe('platform administrator login', () => {
  it('accepts the administrator role returned by the server', async () => {
    vi.mocked(getCurrentProfile).mockResolvedValue({ role: 'ADMIN' } as any)
    await loginPlatformAdmin('19900000003', 'test-password')
    expect(uni.request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/api/v1/auth/login',
      data: { phone: '19900000003', password: 'test-password' }
    }))
    expect(saveAccountSession).toHaveBeenCalledWith('test-token', '2026-12-01T00:00:00Z')
    expect(clearAccountSession).not.toHaveBeenCalled()
  })

  it.each(['USER', 'VOLUNTEER'])('rejects %s accounts and clears their login session', async role => {
    vi.mocked(getCurrentProfile).mockResolvedValue({ role } as any)
    await expect(loginPlatformAdmin('19900000001', 'test-password')).rejects.toMatchObject({ code: 'ADMIN_REQUIRED' })
    expect(clearAccountSession).toHaveBeenCalledOnce()
  })

  it('clears the session if the administrator role cannot be verified', async () => {
    vi.mocked(getCurrentProfile).mockRejectedValue(new Error('network unavailable'))
    await expect(loginPlatformAdmin('19900000003', 'test-password')).rejects.toThrow('network unavailable')
    expect(clearAccountSession).toHaveBeenCalledOnce()
  })

  it('keeps the login form error when authentication fails', async () => {
    vi.mocked(uni.request).mockImplementation(({ success }: any) => success({
      statusCode: 401,
      data: { success: false, error: { code: 'INVALID_CREDENTIALS', message: '手机号或密码错误' } }
    }) as any)
    await expect(loginPlatformAdmin('19900000003', 'wrong-password')).rejects.toMatchObject({ code: 'INVALID_CREDENTIALS' })
    expect(saveAccountSession).not.toHaveBeenCalled()
    expect(getCurrentProfile).not.toHaveBeenCalled()
  })

  it('continues to accept other roles through regular user login', async () => {
    await loginWithPassword('19900000001', 'test-password')
    expect(saveAccountSession).toHaveBeenCalledOnce()
    expect(getCurrentProfile).not.toHaveBeenCalled()
  })
})
