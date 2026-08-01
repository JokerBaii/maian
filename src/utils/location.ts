export interface AppLocation {
  longitude: number
  latitude: number
  accuracy?: number
}

const PI = Math.PI
const AXIS = 6378245.0
const ECCENTRICITY = 0.006693421622965943

function outsideChina(longitude: number, latitude: number) {
  return longitude < 72.004 || longitude > 137.8347 || latitude < 0.8293 || latitude > 55.8271
}

function transformLatitude(longitude: number, latitude: number) {
  let value = -100 + 2 * longitude + 3 * latitude + 0.2 * latitude * latitude
    + 0.1 * longitude * latitude + 0.2 * Math.sqrt(Math.abs(longitude))
  value += (20 * Math.sin(6 * longitude * PI) + 20 * Math.sin(2 * longitude * PI)) * 2 / 3
  value += (20 * Math.sin(latitude * PI) + 40 * Math.sin(latitude / 3 * PI)) * 2 / 3
  value += (160 * Math.sin(latitude / 12 * PI) + 320 * Math.sin(latitude * PI / 30)) * 2 / 3
  return value
}

function transformLongitude(longitude: number, latitude: number) {
  let value = 300 + longitude + 2 * latitude + 0.1 * longitude * longitude
    + 0.1 * longitude * latitude + 0.1 * Math.sqrt(Math.abs(longitude))
  value += (20 * Math.sin(6 * longitude * PI) + 20 * Math.sin(2 * longitude * PI)) * 2 / 3
  value += (20 * Math.sin(longitude * PI) + 40 * Math.sin(longitude / 3 * PI)) * 2 / 3
  value += (150 * Math.sin(longitude / 12 * PI) + 300 * Math.sin(longitude / 30 * PI)) * 2 / 3
  return value
}

/** Convert browser WGS-84 coordinates to the GCJ-02 system used by mainland maps. */
export function wgs84ToGcj02(longitude: number, latitude: number): AppLocation {
  if (outsideChina(longitude, latitude)) return { longitude, latitude }

  let latitudeDelta = transformLatitude(longitude - 105, latitude - 35)
  let longitudeDelta = transformLongitude(longitude - 105, latitude - 35)
  const radianLatitude = latitude / 180 * PI
  let magic = Math.sin(radianLatitude)
  magic = 1 - ECCENTRICITY * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  latitudeDelta = latitudeDelta * 180 / ((AXIS * (1 - ECCENTRICITY)) / (magic * sqrtMagic) * PI)
  longitudeDelta = longitudeDelta * 180 / (AXIS / sqrtMagic * Math.cos(radianLatitude) * PI)
  return {
    longitude: longitude + longitudeDelta,
    latitude: latitude + latitudeDelta
  }
}

/**
 * Get a GCJ-02 location without requiring an H5 map-provider key.
 * Browsers expose WGS-84 through the standard Geolocation API, while native
 * uni-app targets can request GCJ-02 directly from the platform.
 */
export function getCurrentGcj02Location(): Promise<AppLocation> {
  return new Promise((resolve, reject) => {
    // #ifdef H5
    if (!navigator.geolocation) {
      reject(new Error('当前浏览器不支持定位'))
      return
    }
    navigator.geolocation.getCurrentPosition(
      (position) => {
        resolve({
          ...wgs84ToGcj02(position.coords.longitude, position.coords.latitude),
          accuracy: position.coords.accuracy
        })
      },
      () => reject(new Error('无法获取当前位置，请授权定位后重试')),
      { enableHighAccuracy: true, timeout: 10_000, maximumAge: 5_000 }
    )
    // #endif

    // #ifndef H5
    uni.getLocation({
      type: 'gcj02',
      isHighAccuracy: true,
      success: ({ longitude, latitude, accuracy }) => resolve({ longitude, latitude, accuracy }),
      fail: () => reject(new Error('无法获取当前位置，请授权定位后重试'))
    })
    // #endif
  })
}
