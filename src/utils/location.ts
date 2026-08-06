export interface AppLocation {
  longitude: number
  latitude: number
  accuracy?: number
}

/** 固定基准位置（GCJ-02）。紧邻黄龙体育馆 AED 约 110m、浙图 AED 约 195m。 */
export const FIXED_LOCATION: AppLocation = {
  longitude: 120.1442,
  latitude: 30.2718,
  accuracy: 25
}

/** 固定位置对应的地址文案，用于呼救和设备录入的地址字段。 */
export const FIXED_LOCATION_ADDRESS = '杭州市西湖区黄龙体育馆东门'

/** 顶部状态栏等空间有限处使用的短地名。 */
export const FIXED_LOCATION_SHORT_NAME = '西湖区·黄龙体育馆'

// 不走浏览器 Geolocation：它要求 HTTPS 且受权限弹窗影响，结果不可复现
export function getCurrentGcj02Location(): Promise<AppLocation> {
  return Promise.resolve({ ...FIXED_LOCATION })
}
