import { afterEach, describe, expect, it, vi } from 'vitest'
import { buildAmapNavigationUrl, getCurrentGcj02Location, wgs84ToGcj02 } from '../utils/location'

afterEach(() => vi.unstubAllGlobals())

describe('browser incident location', () => {
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
