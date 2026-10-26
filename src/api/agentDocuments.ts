import { getAccountAccessToken } from '@/utils/authSession'
import { ApiRequestError, ensureAccessToken, resolveApiUrl } from './http'

export interface AgentDocument { id: string; name: string; sizeBytes: number; contentType: string; text?: string; notice?: string; createdAt: string }
export interface AgentFile { path: string; name: string; size: number }
export interface DocumentAnalysis {
  kind: 'DOCUMENT' | 'REPORT' | 'UNREADABLE'
  summary: string
  executionMode: string
  truncated: boolean
  report?: { hospital?: string; checkupDate?: string; indicators: { name: string; value: string; unit?: string; referenceRange?: string; abnormal: boolean }[] } | null
}

export async function uploadAgentDocument(file: AgentFile): Promise<AgentDocument> {
  const token = await ensureAccessToken()
  // #ifdef H5
  try {
    const blob = await (await fetch(file.path)).blob()
    const data = new FormData(); data.append('file', blob, file.name)
    const response = await fetch(resolveApiUrl('/api/v1/assistant/documents'), { method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: data })
    const body = await response.json()
    if (!response.ok || !body.success) throw new ApiRequestError(body.error?.message || '文件上传失败', body.error?.code, [], response.status)
    return body.data
  } catch (error) {
    if (error instanceof ApiRequestError) throw error
    throw new ApiRequestError('文件上传连接中断，请检查我的文档后再重试', 'NETWORK_ERROR')
  }
  // #endif
  // #ifndef H5
  return new Promise((resolve, reject) => uni.uploadFile({
    url: resolveApiUrl('/api/v1/assistant/documents'), filePath: file.path, name: 'file',
    header: { Authorization: `Bearer ${token}` },
    success(response) {
      try {
        const body = JSON.parse(response.data)
        if (response.statusCode >= 200 && response.statusCode < 300 && body.success) resolve(body.data)
        else reject(new ApiRequestError(body.error?.message || '文件上传失败', body.error?.code, [], response.statusCode))
      } catch { reject(new ApiRequestError('文件上传响应无效')) }
    },
    fail(error) { reject(new ApiRequestError(error.errMsg || '文件上传失败', 'NETWORK_ERROR')) }
  }))
  // #endif
}

/** Must be called from a user gesture, including H5's native file chooser. */
export async function chooseAgentFile(images = false, allowImages = false): Promise<AgentFile | null> {
  const owner = getAccountAccessToken()
  const file = await pickAgentFile(images, allowImages)
  if (file && owner !== getAccountAccessToken()) {
    // #ifdef H5
    if (file.path.startsWith('blob:')) URL.revokeObjectURL(file.path)
    // #endif
    throw new Error('账号已切换，请重新选择文件')
  }
  return file
}
function pickAgentFile(images: boolean, allowImages: boolean): Promise<AgentFile | null> {
  return new Promise((resolve, reject) => {
    // #ifdef H5
    const input = document.createElement('input')
    input.type = 'file'; input.accept = images ? '.jpg,.jpeg,.png' : '.pdf,.docx,.txt,.md,.csv' + (allowImages ? ',.jpg,.jpeg,.png' : '')
    input.style.display = 'none'; input.className = 'assistant-native-file'
    document.body.append(input)
    const cleanup = () => input.remove()
    input.onchange = () => {
      const file = input.files?.[0]
      if (!file) { cleanup(); resolve(null); return }
      resolve({ path: URL.createObjectURL(file), name: file.name, size: file.size }); cleanup()
    }
    input.addEventListener('cancel', () => { cleanup(); resolve(null) }, { once: true })
    input.click()
    return
    // #endif
    // #ifndef H5
    const success = (result: any) => {
      const file = result.tempFiles?.[0]
      resolve(file ? { path: file.path || file.tempFilePath, name: file.name || (images ? '检查单.png' : '文档.txt'), size: file.size } : null)
    }
    const fail = (error: any) => /cancel/.test(error.errMsg || '') ? resolve(null) : reject(error)
    if (images) uni.chooseImage({ count: 1, success: result => resolve({ path: result.tempFilePaths[0], name: '检查单.jpg', size: (result.tempFiles as any)?.[0]?.size || 0 }), fail })
    else if (typeof (uni as any).chooseMessageFile === 'function') (uni as any).chooseMessageFile({ count: 1, type: allowImages ? 'all' : 'file', success, fail })
    else if (typeof (uni as any).chooseFile === 'function') (uni as any).chooseFile({ count: 1, success, fail })
    else reject(new Error('当前端暂不支持文档选择，请使用网页版；检查单仍可选择图片。'))
    // #endif
  })
}
