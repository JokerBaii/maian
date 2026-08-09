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

export const DEFAULT_MAP_CENTER: AppLocation = {
  longitude: 120.1442,
  latitude: 30.2718,
  accuracy: 25,
  address: '杭州市西湖区黄龙体育馆东门'
}

export function getCurrentGcj02Location(): Promise<AppLocation> {
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

export function openMapNavigation(target: MapNavigationTarget) {
  const navigationUrl = buildAmapNavigationUrl(target)
  const opened = window.open(navigationUrl, '_blank', 'noopener,noreferrer')
  if (!opened) window.location.assign(navigationUrl)
  return undefined

  uni.openLocation({
    latitude: target.latitude,
    longitude: target.longitude,
    name: target.name,
    address: target.address,
    scale: 16,
    fail: () => uni.showToast({ title: '无法打开系统地图', icon: 'none' })
  })
}
