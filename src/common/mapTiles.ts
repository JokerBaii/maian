/**
 * Leaflet 底图瓦片配置。
 *
 * 使用高德栅格瓦片而非 OpenStreetMap：国内网络访问 tile.openstreetmap.org
 * 会超时，且高德瓦片与 App 全局使用的 GCJ-02 坐标系一致（见 utils/location.ts），
 * 直接用 WGS-84 底图会让标记偏移数百米。该瓦片地址不需要 API Key。
 */
const TILE_URL = 'https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&style=8&x={x}&y={y}&z={z}'

const TILE_OPTIONS = {
  subdomains: ['1', '2', '3', '4'],
  minZoom: 3,
  maxZoom: 18,
  // 瓦片超时后保留上一层级画面，避免整屏空白
  keepBuffer: 2,
  updateWhenIdle: false,
  attribution: '© 高德地图'
}

/** 在给定 Leaflet 实例上挂载底图。 */
export function addBaseTileLayer(leafletApi: any, mapInstance: any) {
  return leafletApi.tileLayer(TILE_URL, TILE_OPTIONS).addTo(mapInstance)
}
