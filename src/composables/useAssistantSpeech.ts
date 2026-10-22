import { ref } from 'vue'
import { getAccountAccessToken } from '@/utils/authSession'

export interface SpeechEngine {
  lang: string; continuous: boolean; interimResults: boolean; maxAlternatives: number
  onresult: ((event: { results: ArrayLike<{ isFinal: boolean; 0: { transcript: string } }> }) => void) | null
  onerror: ((event: { error: string }) => void) | null
  onend: (() => void) | null
  start(): void; stop(): void; abort(): void
}
export function browserSpeechFactory(): (() => SpeechEngine) | undefined {
  // #ifdef H5
  if (typeof window === 'undefined' || !window.isSecureContext) return
  const Engine = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
  if (Engine) return () => new Engine()
  // #endif
}

export function createAssistantSpeech(onText: (text: string) => void, factory = browserSpeechFactory(), identity = getAccountAccessToken) {
  const available = !!factory, listening = ref(false), transcript = ref(''), error = ref('')
  let active: SpeechEngine | undefined, finalText = '', owner = ''
  let timer: ReturnType<typeof setTimeout> | undefined, ownerTimer: ReturnType<typeof setInterval> | undefined
  function release() { clearTimeout(timer); clearInterval(ownerTimer); listening.value = false; active = undefined }
  function cancel() {
    const engine = active
    release(); transcript.value = ''; finalText = ''; error.value = ''
    if (engine) { engine.onresult = null; engine.onerror = null; engine.onend = null; try { engine.abort() } catch { /* Already ended. */ } }
  }
  function stop() {
    if (!active) return
    clearTimeout(timer)
    try { active.stop() } catch { cancel(); return }
    timer = setTimeout(() => { cancel(); error.value = '语音识别超时，请重试或输入文字' }, 5000)
  }
  function start() {
    if (!factory || active || !identity()) return
    error.value = ''; transcript.value = ''; finalText = ''; owner = identity()
    try {
      const engine = factory(); active = engine; listening.value = true
      engine.lang = 'zh-CN'; engine.continuous = false; engine.interimResults = true; engine.maxAlternatives = 1
      engine.onresult = event => {
        if (active !== engine) return
        if (identity() !== owner) { cancel(); return }
        const results = Array.from(event.results)
        finalText = results.filter(result => result.isFinal).map(result => result[0].transcript).join('').trim()
        transcript.value = results.map(result => result[0].transcript).join('').slice(0, 300)
      }
      engine.onerror = event => {
        if (active !== engine) return
        const messages: Record<string,string> = { 'not-allowed':'未获得麦克风权限，可以继续输入文字', 'service-not-allowed':'当前浏览器语音服务不可用，请输入文字', 'audio-capture':'未检测到可用麦克风', 'no-speech':'没有听清，请再说一次', network:'语音服务连接失败，请重试或输入文字' }
        cancel(); error.value = messages[event.error] || '语音识别未完成，请重试或输入文字'
      }
      engine.onend = () => {
        if (active !== engine) return
        const text = finalText, sameOwner = identity() === owner
        release(); transcript.value = ''; finalText = ''
        if (!sameOwner) return
        if (!text) { error.value = '没有听清，请再说一次'; return }
        if (text.length > 300) { error.value = '这段话太长，请分成两次说'; return }
        onText(text)
      }
      ownerTimer = setInterval(() => { if (identity() !== owner) cancel() }, 250)
      timer = setTimeout(stop, 45000)
      engine.start()
    } catch { cancel(); error.value = '无法启动语音，请重试或输入文字' }
  }
  return { available, listening, transcript, error, start, stop, cancel }
}
