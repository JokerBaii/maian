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
  if (typeof __MAIAN_PREVIEW_LOCATION__ !== 'undefined' && __MAIAN_PREVIEW_LOCATION__) {
    return Promise.resolve({ ...__MAIAN_PREVIEW_LOCATION__ })
  }
  // #ifdef H5
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) { reject(new Error('当前浏览器不支持定位')); return }
    navigator.geolocation.getCurrentPosition(result => resolve({
      ...wgs84ToGcj02(result.coords.longitude, result.coords.latitude), accuracy: result.coords.accuracy
    }), reject, { enableHighAccuracy: true, timeout: 8000, maximumAge: 0 })
  })
  // #endif
  // #ifndef H5
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
  // #endif
}

/** Browser GPS uses WGS-84; device locations and the application's AMap base tiles use GCJ-02. */
export function wgs84ToGcj02(longitude: number, latitude: number): AppLocation {
  if (longitude < 72.004 || longitude > 137.8347 || latitude < .8293 || latitude > 55.8271) return { longitude, latitude }
  const x = longitude - 105, y = latitude - 35, pi = Math.PI
  let lat = -100 + 2*x + 3*y + .2*y*y + .1*x*y + .2*Math.sqrt(Math.abs(x))
  lat += (20*Math.sin(6*x*pi) + 20*Math.sin(2*x*pi))*2/3
  lat += (20*Math.sin(y*pi) + 40*Math.sin(y*pi/3))*2/3
  lat += (160*Math.sin(y*pi/12) + 320*Math.sin(y*pi/30))*2/3
  let lng = 300 + x + 2*y + .1*x*x + .1*x*y + .1*Math.sqrt(Math.abs(x))
  lng += (20*Math.sin(6*x*pi) + 20*Math.sin(2*x*pi))*2/3
  lng += (20*Math.sin(x*pi) + 40*Math.sin(x*pi/3))*2/3
  lng += (150*Math.sin(x*pi/12) + 300*Math.sin(x*pi/30))*2/3
  const rad = latitude*pi/180, magic = 1 - .00669342162296594323*Math.sin(rad)**2
  return {
    latitude: latitude + lat*180 / ((6378245*(1-.00669342162296594323) / (magic*Math.sqrt(magic)))*pi),
    longitude: longitude + lng*180 / ((6378245/Math.sqrt(magic)*Math.cos(rad))*pi)
  }
}

/** Great-circle distance in metres, shared by map sorting and live rescue tracking. */
export function haversineMeters(fromLatitude: number, fromLongitude: number, toLatitude: number, toLongitude: number) {
  const radians = (degrees: number) => degrees * Math.PI / 180
  const latitudeDelta = radians(toLatitude - fromLatitude)
  const longitudeDelta = radians(toLongitude - fromLongitude)
  const haversine = Math.sin(latitudeDelta / 2) ** 2
    + Math.cos(radians(fromLatitude)) * Math.cos(radians(toLatitude)) * Math.sin(longitudeDelta / 2) ** 2
  return 6_371_000 * 2 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, haversine))))
}

export function chooseGcj02Location(initial?: AppLocation): Promise<AppLocation> {
  // #ifdef H5
  return new Promise((resolve, reject) => {
    const event = `maian:choose-location:${Date.now()}-${Math.random().toString(36).slice(2)}`
    const receive = (location?: AppLocation) => {
      uni.$off(event, receive)
      if (location) resolve(location)
      else reject(new Error('已取消选择位置'))
    }
    uni.$on(event, receive)
    const query = new URLSearchParams({ event })
    if (initial) {
      query.set('longitude', String(initial.longitude))
      query.set('latitude', String(initial.latitude))
      if (initial.address) query.set('address', initial.address)
    }
    uni.navigateTo({ url: `/pages/location/pick?${query.toString()}`, fail: error => { uni.$off(event, receive); reject(error) } })
  })
  // #endif
  // #ifndef H5
  return new Promise((resolve, reject) => {
    uni.chooseLocation({
      ...(initial && { longitude: initial.longitude, latitude: initial.latitude }),
      success: location => resolve({
        longitude: location.longitude,
        latitude: location.latitude,
        address: location.address || location.name
      }),
      fail: reject
    })
  })
  // #endif
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
  // #ifdef H5
  const navigationUrl = buildAmapNavigationUrl(target)
  const opened = window.open(navigationUrl, '_blank', 'noopener,noreferrer')
  if (!opened) window.location.assign(navigationUrl)
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
