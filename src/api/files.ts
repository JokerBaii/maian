import { ApiRequestError, resolveApiUrl, type ApiResponse } from './http'

export interface FileUploadResponse {
  url: string
}

export function uploadImage(filePath: string): Promise<FileUploadResponse> {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: resolveApiUrl('/api/v1/files/images'),
      filePath,
      name: 'file',
      success(response) {
        let body: ApiResponse<FileUploadResponse>
        try {
          body = JSON.parse(response.data) as ApiResponse<FileUploadResponse>
        } catch {
          reject(new ApiRequestError('图片上传响应无效'))
          return
        }

        if (
          response.statusCode >= 200
          && response.statusCode < 300
          && body.success
          && body.data
        ) {
          resolve(body.data)
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
