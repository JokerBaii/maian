
const TILE_URL = 'https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&style=8&x={x}&y={y}&z={z}'

const TILE_OPTIONS = {
  subdomains: ['1', '2', '3', '4'],
  minZoom: 3,
  maxZoom: 18,
  keepBuffer: 2,
  updateWhenIdle: false,
  attribution: '© 高德地图'
}


export function addBaseTileLayer(
  leafletApi: any,
  mapInstance: any,
  onUnavailable?: () => void
) {
  const layer = leafletApi.tileLayer(TILE_URL, TILE_OPTIONS).addTo(mapInstance)

  if (onUnavailable) {
    let errorCount = 0
    let notified = false
    layer.on('tileerror', () => {
      errorCount += 1
      if (!notified && errorCount >= 6) {
        notified = true
        onUnavailable()
      }
    })
    layer.on('tileload', () => {
      errorCount = 0
    })
  }

  return layer
}
