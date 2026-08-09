import { describe, expect, it } from 'vitest'
import { extractAmapRouteCoordinates } from '../common/amapRoute'

describe('AMap rescue route', () => {
  it('extracts road geometry as Leaflet latitude-longitude coordinates', () => {
    const result = {
      routes: [{
        steps: [
          { path: [{ lng: 120.1, lat: 30.1 }, { lng: 120.2, lat: 30.2 }] },
          {
            path: [
              { lng: 120.2, lat: 30.2 },
              { getLng: () => 120.3, getLat: () => 30.3 }
            ]
          }
        ]
      }]
    }

    expect(extractAmapRouteCoordinates(result)).toEqual([
      [30.1, 120.1],
      [30.2, 120.2],
      [30.3, 120.3]
    ])
  })

  it('returns an empty path for an invalid route response', () => {
    expect(extractAmapRouteCoordinates({ routes: [] })).toEqual([])
  })
})
