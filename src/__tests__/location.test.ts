import { afterEach, describe, expect, it, vi } from 'vitest'
import { buildAmapNavigationUrl, chooseGcj02Location, getCurrentGcj02Location, haversineMeters, wgs84ToGcj02, type AppLocation } from '../utils/location'

afterEach(() => vi.unstubAllGlobals())

describe('browser incident location', () => {
  it('uses the configured local preview point without requesting browser GPS', async () => {
    const location = { longitude: 120.1505, latitude: 30.2805, accuracy: 15, address: '杭州市' }
    const getCurrentPosition = vi.fn()
    vi.stubGlobal('__MAIAN_PREVIEW_LOCATION__', location)
    vi.stubGlobal('navigator', { geolocation: { getCurrentPosition } })
    expect(await getCurrentGcj02Location()).toEqual(location)
    expect(getCurrentPosition).not.toHaveBeenCalled()
  })
  it('converts a GPS reference point to the coordinate system used by devices', () => {
    const point = wgs84ToGcj02(116.397128, 39.916527)
    expect(point.longitude).toBeCloseTo(116.403372494, 7)
    expect(point.latitude).toBeCloseTo(39.917930749, 7)
    expect(wgs84ToGcj02(-.1276, 51.5072)).toEqual({ longitude: -.1276, latitude: 51.5072 })
  })
  it('uses browser GPS without a configured map SDK and keeps its accuracy', async () => {
    const getCurrentPosition = vi.fn((success: PositionCallback, _error?: PositionErrorCallback, _options?: PositionOptions) => success({ coords: { longitude: 116.397128, latitude: 39.916527, accuracy: 15 } } as GeolocationPosition))
    vi.stubGlobal('navigator', { geolocation: { getCurrentPosition } })
    expect(await getCurrentGcj02Location()).toEqual({ ...wgs84ToGcj02(116.397128, 39.916527), accuracy: 15 })
    expect(getCurrentPosition.mock.calls[0][2]).toMatchObject({ maximumAge: 0, timeout: 8000 })
  })
  it('rejects denied GPS instead of returning the preview map center', async () => {
    vi.stubGlobal('navigator', { geolocation: { getCurrentPosition: (_success: unknown, fail: (error: Error) => void) => fail(new Error('permission denied')) } })
    await expect(getCurrentGcj02Location()).rejects.toThrow('permission denied')
  })
})

describe('map navigation', () => {
  it('measures distance consistently for map sorting and rescue tracking, including antipodal points', () => {
    expect(haversineMeters(0, 0, 0, 0)).toBe(0)
    expect(haversineMeters(0, 0, 0, 1)).toBeCloseTo(111194.927, 2)
    expect(haversineMeters(90, 0, -90, 180)).toBeCloseTo(Math.PI * 6371000)
  })

  it('passes the existing device location to the shared picker and only applies a confirmed selection', async () => {
    const events = new Map<string, (location?: AppLocation) => void>()
    const navigateTo = vi.fn()
    const off = vi.fn((event: string) => events.delete(event))
    vi.stubGlobal('uni', { $on: (event: string, handler: (location?: AppLocation) => void) => events.set(event, handler), $off: off, navigateTo })
    const initial = { longitude: 120.15, latitude: 30.28, address: '北门 & 服务台' }
    const pending = chooseGcj02Location(initial)
    const query = new URL(navigateTo.mock.calls[0][0].url, 'https://app.local').searchParams
    expect(query.get('address')).toBe(initial.address)
    expect(query.get('longitude')).toBe('120.15')
    const selected = { longitude: 120.16, latitude: 30.29, address: '南门' }
    events.get(query.get('event')!)!(selected)
    expect(await pending).toEqual(selected)
    expect(initial.address).toBe('北门 & 服务台')
    expect(events.size).toBe(0)
  })

  it('cancels the shared picker without leaking its callback or inventing coordinates', async () => {
    let receive!: () => void
    const off = vi.fn()
    vi.stubGlobal('uni', { $on: (_event: string, handler: () => void) => { receive = handler }, $off: off, navigateTo: vi.fn() })
    const pending = chooseGcj02Location()
    receive()
    await expect(pending).rejects.toThrow('已取消选择位置')
    expect(off).toHaveBeenCalledOnce()
  })

  it('builds an encoded AMap route using GCJ-02 coordinates', () => {
    const url = new URL(buildAmapNavigationUrl({
      longitude: 120.1442,
      latitude: 30.2718,
      name: '黄龙体育馆东门 AED',
      mode: 'walk'
    }))

    expect(url.origin).toBe('https://uri.amap.com')
    expect(url.pathname).toBe('/navigation')
    expect(url.searchParams.get('to')).toBe('120.1442,30.2718,黄龙体育馆东门 AED')
    expect(url.searchParams.get('mode')).toBe('walk')
    expect(url.searchParams.get('coordinate')).toBe('gaode')
    expect(url.searchParams.get('callnative')).toBe('1')
  })
})
