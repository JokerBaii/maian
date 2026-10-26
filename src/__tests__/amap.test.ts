import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

describe('map SDK loading', () => {
  let scripts: Array<{ onload: null | (() => void); onerror: null | (() => void); remove: ReturnType<typeof vi.fn> }>

  beforeEach(() => {
    vi.resetModules()
    vi.useFakeTimers()
    vi.stubEnv('VITE_AMAP_KEY', 'test-key')
    vi.stubEnv('VITE_AMAP_SECURITY_CODE', 'test-security-code')
    scripts = []
    vi.stubGlobal('window', {})
    vi.stubGlobal('document', {
      createElement: () => {
        const script = { onload: null, onerror: null, remove: vi.fn() }
        scripts.push(script)
        return script
      },
      head: { appendChild: vi.fn() }
    })
  })

  afterEach(() => {
    vi.clearAllTimers()
    vi.useRealTimers()
    vi.unstubAllGlobals()
    vi.unstubAllEnvs()
  })

  it('bounds a stalled network request and allows a fresh attempt', async () => {
    const { loadAMap } = await import('../common/amap')
    const stalled = loadAMap()
    expect(loadAMap()).toBe(stalled)
    const lateLoad = scripts[0].onload!
    await vi.advanceTimersByTimeAsync(6000)
    expect(await stalled).toBe(false)
    expect(scripts[0].remove).toHaveBeenCalledOnce()

    const retry = loadAMap()
    expect(scripts).toHaveLength(2)
    lateLoad()
    ;(window as any).AMap = {}
    scripts[1].onload!()
    expect(await retry).toBe(true)
  })

  it('falls back when a successful script response does not expose the SDK', async () => {
    const { loadAMap } = await import('../common/amap')
    const loaded = loadAMap()
    scripts[0].onload!()
    expect(await loaded).toBe(false)
    expect(vi.getTimerCount()).toBe(0)
  })

  it('permits retry after a script network error', async () => {
    const { loadAMap } = await import('../common/amap')
    const failed = loadAMap()
    scripts[0].onerror!()
    expect(await failed).toBe(false)
    const retry = loadAMap()
    ;(window as any).AMap = {}
    scripts[1].onload!()
    expect(await retry).toBe(true)
    expect(vi.getTimerCount()).toBe(0)
  })
})
