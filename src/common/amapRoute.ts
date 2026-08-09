import { loadAMap } from './amap'

export type RouteCoordinate = [number, number]
export type AmapRouteMode = 'walk' | 'drive'

function readCoordinate(point: any): RouteCoordinate | null {
  const longitude = typeof point?.getLng === 'function' ? point.getLng() : point?.lng
  const latitude = typeof point?.getLat === 'function' ? point.getLat() : point?.lat
  if (!Number.isFinite(longitude) || !Number.isFinite(latitude)) return null
  return [latitude, longitude]
}

export function extractAmapRouteCoordinates(result: any): RouteCoordinate[] {
  const steps = result?.routes?.[0]?.steps
  if (!Array.isArray(steps)) return []

  const coordinates: RouteCoordinate[] = []
  for (const step of steps) {
    if (!Array.isArray(step?.path)) continue
    for (const point of step.path) {
      const coordinate = readCoordinate(point)
      if (!coordinate) continue
      const previous = coordinates[coordinates.length - 1]
      if (!previous || previous[0] !== coordinate[0] || previous[1] !== coordinate[1]) {
        coordinates.push(coordinate)
      }
    }
  }
  return coordinates
}

export async function planAmapRoute(
  start: RouteCoordinate,
  end: RouteCoordinate,
  mode: AmapRouteMode
): Promise<RouteCoordinate[] | null> {
  if (!await loadAMap()) return null
  const AMap = (window as any).AMap
  if (!AMap) return null

  const pluginName = mode === 'drive' ? 'AMap.Driving' : 'AMap.Walking'
  return new Promise(resolve => {
    let settled = false
    const finish = (coordinates: RouteCoordinate[] | null) => {
      if (settled) return
      settled = true
      clearTimeout(timeout)
      resolve(coordinates)
    }
    const timeout = setTimeout(() => finish(null), 8000)

    try {
      AMap.plugin(pluginName, () => {
        try {
          const Planner = mode === 'drive' ? AMap.Driving : AMap.Walking
          if (!Planner) {
            finish(null)
            return
          }
          const planner = new Planner({})
          planner.search(
            [start[1], start[0]],
            [end[1], end[0]],
            (status: string, result: any) => {
              if (status !== 'complete') {
                finish(null)
                return
              }
              const coordinates = extractAmapRouteCoordinates(result)
              finish(coordinates.length >= 2 ? coordinates : null)
            }
          )
        } catch {
          finish(null)
        }
      })
    } catch {
      finish(null)
    }
  })
}
