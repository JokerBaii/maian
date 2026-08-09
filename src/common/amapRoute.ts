import { loadAMap } from './amap'

export type RouteCoordinate = [number, number]
export type AmapRouteMode = 'walk' | 'drive'

/**
 * 演示环境的道路预览兜底：用直角转折表达沿路行进，避免用一条直线穿过建筑。
 * 正式环境仍应使用高德规划结果，该几何不对外声称为真实导航路线。
 */
export function buildDemoRoadPreview(start: RouteCoordinate, end: RouteCoordinate): RouteCoordinate[] {
  const latitudeDelta = end[0] - start[0]
  const longitudeDelta = end[1] - start[1]
  const latitudeDistance = Math.abs(latitudeDelta)
  const longitudeDistance = Math.abs(longitudeDelta * Math.cos(start[0] * Math.PI / 180))

  if (latitudeDistance >= longitudeDistance) {
    return [
      start,
      [start[0] + latitudeDelta * .18, start[1]],
      [start[0] + latitudeDelta * .24, start[1] + longitudeDelta * .36],
      [start[0] + latitudeDelta * .78, start[1] + longitudeDelta * .36],
      [start[0] + latitudeDelta * .84, end[1]],
      end
    ]
  }
  return [
    start,
    [start[0], start[1] + longitudeDelta * .18],
    [start[0] + latitudeDelta * .36, start[1] + longitudeDelta * .24],
    [start[0] + latitudeDelta * .36, start[1] + longitudeDelta * .78],
    [end[0], start[1] + longitudeDelta * .84],
    end
  ]
}

function readCoordinate(point: any): RouteCoordinate | null {
  const longitude = typeof point?.getLng === 'function' ? point.getLng() : point?.lng
  const latitude = typeof point?.getLat === 'function' ? point.getLat() : point?.lat
  if (!Number.isFinite(longitude) || !Number.isFinite(latitude)) return null
  return [latitude, longitude]
}

/** 将高德分路段结果转换成 Leaflet 使用的 [纬度, 经度] 道路折线。 */
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

/**
 * 使用高德路线服务获取真实可通行路径。未配置 JS API、无路线或请求失败时返回 null，
 * 调用方应只显示起终点，不能退化成穿越建筑或水面的伪折线。
 */
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
