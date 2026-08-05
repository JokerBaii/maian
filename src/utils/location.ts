export interface AppLocation {
  longitude: number
  latitude: number
  accuracy?: number
}

/**
 * 固定基准位置：黄龙体育馆东门（GCJ-02）。
 *
 * 该点紧邻已入库的黄龙体育馆东门 AED（约 110m）与浙江图书馆 AED（约 195m），
 * 便于稳定复现就近匹配结果。坐标系与地图底图一致，见 common/mapTiles.ts。
 */
export const FIXED_LOCATION: AppLocation = {
  longitude: 120.1442,
  latitude: 30.2718,
  accuracy: 25
}

/** 固定位置对应的地址文案，用于呼救和设备录入的地址字段。 */
export const FIXED_LOCATION_ADDRESS = '杭州市西湖区黄龙体育馆东门'

/** 顶部状态栏等空间有限处使用的短地名。 */
export const FIXED_LOCATION_SHORT_NAME = '西湖区·黄龙体育馆'

/**
 * 返回统一的基准位置。
 *
 * 不依赖浏览器 Geolocation：该接口只在 HTTPS 或 localhost 下可用，
 * 且受权限弹窗和设备定位精度影响，无法保证每次结果一致。
 */
export function getCurrentGcj02Location(): Promise<AppLocation> {
  return Promise.resolve({ ...FIXED_LOCATION })
}
