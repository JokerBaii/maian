let loadingPromise: Promise<boolean> | null = null

export function loadAMap(): Promise<boolean> {
  if (typeof window === 'undefined' || typeof document === 'undefined') {
    return Promise.resolve(false)
  }
  if ((window as any).AMap) {
    return Promise.resolve(true)
  }
  if (loadingPromise) {
    return loadingPromise
  }

  const env = (import.meta as any).env || {}
  const mapKey = env.VITE_AMAP_KEY
  const securityCode = env.VITE_AMAP_SECURITY_CODE
  if (!mapKey || !securityCode) {
    return Promise.resolve(false)
  }

  const attempt = new Promise<boolean>((resolve) => {
    ;(window as any)._AMapSecurityConfig = { securityJsCode: securityCode }
    const script = document.createElement('script')
    let settled = false
    const finish = (loaded: boolean) => {
      if (settled) return
      settled = true
      clearTimeout(timeout)
      script.onload = null
      script.onerror = null
      if (!loaded) script.remove()
      resolve(loaded)
    }
    const timeout = setTimeout(() => finish(false), 6000)
    script.id = 'amap-web-js'
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(mapKey)}`
    script.onload = () => finish(Boolean((window as any).AMap))
    script.onerror = () => finish(false)
    document.head.appendChild(script)
  })
  loadingPromise = attempt
  void attempt.then(loaded => {
    if (!loaded && loadingPromise === attempt) loadingPromise = null
  })

  return attempt
}
