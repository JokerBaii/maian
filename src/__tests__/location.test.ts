import { describe, expect, it } from 'vitest'
import { buildAmapNavigationUrl } from '../utils/location'

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
