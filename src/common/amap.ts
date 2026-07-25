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

  loadingPromise = new Promise<boolean>((resolve) => {
    ;(window as any)._AMapSecurityConfig = { securityJsCode: securityCode }
    const script = document.createElement('script')
    script.id = 'amap-web-js'
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(mapKey)}`
    script.onload = () => resolve(true)
    script.onerror = () => {
      loadingPromise = null
      script.remove()
      resolve(false)
    }
    document.head.appendChild(script)
  })

  return loadingPromise
}
