import { ensureAccessToken, resolveApiUrl } from '@/api/http'

export interface RescueRealtimeEvent {
  rescueCallId?: string
  type: string
  sequence: number
  occurredAt: string
}

/**
 * 前台 H5 使用带 Bearer Token 的 fetch 读取 SSE；App 端保留轮询兜底。
 * 返回值必须在页面隐藏/卸载时调用，避免残留连接。
 */
export function connectRescueEvents(onEvent: (event: RescueRealtimeEvent) => void) {
  let disposed = false
  let controller: AbortController | null = null

  // #ifdef H5
  controller = new AbortController()
  ;(async () => {
    try {
      const token = await ensureAccessToken()
      const response = await fetch(resolveApiUrl('/api/v1/rescue-events/stream'), {
        headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
        signal: controller?.signal
      })
      if (!response.ok || !response.body) return
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      while (!disposed) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const frames = buffer.split('\n\n')
        buffer = frames.pop() || ''
        for (const frame of frames) {
          const data = frame.split('\n')
            .filter(line => line.startsWith('data:'))
            .map(line => line.slice(5).trim())
            .join('\n')
          if (!data) continue
          try { onEvent(JSON.parse(data) as RescueRealtimeEvent) } catch { /* ignore malformed frame */ }
        }
      }
    } catch {
      // 网络中断时由页面轮询继续保障状态同步。
    }
  })()
  // #endif

  return () => {
    disposed = true
    controller?.abort()
    controller = null
  }
}
