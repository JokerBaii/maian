export interface AppLocation {
  longitude: number
  latitude: number
  accuracy?: number
  address?: string
}

export interface MapNavigationTarget extends AppLocation {
  name: string
  mode?: 'walk' | 'ride' | 'car'
}

export const FIXED_LOCATION: AppLocation = {
  longitude: 120.1442,
  latitude: 30.2718,
  accuracy: 25,
  address: '杭州市西湖区黄龙体育馆东门'
}

export const FIXED_LOCATION_ADDRESS = '杭州市西湖区黄龙体育馆东门'
export const FIXED_LOCATION_SHORT_NAME = '西湖区·黄龙体育馆'
export const isDemoMode = String((import.meta as any).env?.VITE_DEMO_MODE || '').toLowerCase() === 'true'

export function getCurrentGcj02Location(): Promise<AppLocation> {
  if (isDemoMode) return Promise.resolve({ ...FIXED_LOCATION })
  return new Promise((resolve, reject) => {
    uni.getLocation({
      type: 'gcj02',
      isHighAccuracy: true,
      success: result => resolve({
        longitude: result.longitude,
        latitude: result.latitude,
        accuracy: result.accuracy
      }),
      fail: reject
    })
  })
}

/** 只允许在用户明确点击后调用，避免后台定位失败时突然打开选点界面。 */
export function chooseGcj02Location(): Promise<AppLocation> {
  return new Promise((resolve, reject) => {
    uni.chooseLocation({
      success: location => resolve({
        longitude: location.longitude,
        latitude: location.latitude,
        address: location.address || location.name
      }),
      fail: reject
    })
  })
}

export function buildAmapNavigationUrl(target: MapNavigationTarget): string {
  const query = new URLSearchParams({
    to: `${target.longitude},${target.latitude},${target.name}`,
    mode: target.mode || 'walk',
    policy: '1',
    src: '脉安驰援',
    coordinate: 'gaode',
    callnative: '1'
  })
  return `https://uri.amap.com/navigation?${query.toString()}`
}

/**
 * 所有“导航”入口统一走这里：H5 使用高德路线规划，App/小程序交给系统地图。
 * 该方法只能由用户点击触发，避免浏览器拦截新窗口。
 */
export function openMapNavigation(target: MapNavigationTarget) {
  // #ifdef H5
  const navigationUrl = buildAmapNavigationUrl(target)
  const opened = window.open(navigationUrl, '_blank', 'noopener,noreferrer')
  if (!opened) window.location.assign(navigationUrl)
  return undefined
  // #endif

  // #ifndef H5
  uni.openLocation({
    latitude: target.latitude,
    longitude: target.longitude,
    name: target.name,
    address: target.address,
    scale: 16,
    fail: () => uni.showToast({ title: '无法打开系统地图', icon: 'none' })
  })
  // #endif
}
