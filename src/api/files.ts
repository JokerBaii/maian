import { ApiRequestError, ensureAccessToken, resolveApiUrl, type ApiResponse } from './http'

export type MediaPurpose = 'HEALTH_REPORT' | 'RESCUE_ATTACHMENT' | 'DEVICE_IMAGE' | 'VEHICLE_IMAGE' | 'SCIENCE_COVER'

export interface FileUploadResponse {
  mediaId: string
  contentType: string
  sizeBytes: number
  url: string
  expiresAt: string
}

export function issueMediaDownload(mediaId: string) {
  return import('./http').then(({ request }) => request<{ url: string; expiresAt: string }>(
    `/api/v1/media/${encodeURIComponent(mediaId)}/download-token`,
    { method: 'POST' }
  ))
}

export async function uploadImage(
  filePath: string,
  purpose: MediaPurpose
): Promise<FileUploadResponse> {
  const accessToken = await ensureAccessToken()
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: resolveApiUrl(`/api/v1/media?purpose=${encodeURIComponent(purpose)}`),
      filePath,
      name: 'file',
      header: { Authorization: `Bearer ${accessToken}` },
      success(response) {
        let body: ApiResponse<{
          mediaId: string
          contentType: string
          sizeBytes: number
          downloadUrl: string
          downloadUrlExpiresAt: string
        }>
        try {
          body = JSON.parse(response.data) as typeof body
        } catch {
          reject(new ApiRequestError('图片上传响应无效'))
          return
        }
        if (response.statusCode >= 200 && response.statusCode < 300 && body.success && body.data) {
          resolve({
            mediaId: body.data.mediaId,
            contentType: body.data.contentType,
            sizeBytes: body.data.sizeBytes,
            url: body.data.downloadUrl,
            expiresAt: body.data.downloadUrlExpiresAt
          })
          return
        }
        reject(new ApiRequestError(
          body.error?.message || `图片上传失败（${response.statusCode}）`,
          body.error?.code,
          body.error?.details,
          response.statusCode
        ))
      },
      fail(error) {
        reject(new ApiRequestError(error.errMsg || '图片上传失败，请检查网络', 'NETWORK_ERROR'))
      }
    })
  })
}
