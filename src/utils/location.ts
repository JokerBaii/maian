export interface AppLocation {
  longitude: number
  latitude: number
  accuracy?: number
  address?: string
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
      fail: () => {
        uni.chooseLocation({
          success: location => resolve({
            longitude: location.longitude,
            latitude: location.latitude,
            address: location.address || location.name
          }),
          fail: reject
        })
      }
    })
  })
}
