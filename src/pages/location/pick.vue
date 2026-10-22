<template>
  <view class="location-picker apple-page">
    <text class="location-picker-hint">移动地图，点击实际现场位置</text>
    <view id="location-picker-map" class="location-picker-map"></view>
    <view class="location-picker-footer">
      <text v-if="unavailable" class="location-picker-error">底图加载失败，请返回后重试定位。</text>
      <text v-else>{{ selected ? `已选择：${selected.latitude.toFixed(5)}, ${selected.longitude.toFixed(5)}` : '尚未选择现场，地图中心不会自动作为呼救位置' }}</text>
      <button class="location-picker-confirm" :disabled="!selected || unavailable" @tap="confirm">使用这个位置</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { DEFAULT_MAP_CENTER, type AppLocation } from '@/utils/location'
// #ifdef H5
import 'leaflet/dist/leaflet.css'
import type { Map as LeafletMap, CircleMarker } from 'leaflet'
import { addBaseTileLayer } from '@/common/mapTiles'
let map: LeafletMap | undefined, marker: CircleMarker | undefined
// #endif
const selected = ref<AppLocation | null>(null), unavailable = ref(false)
let event = '', delivered = false, disposed = false
onLoad(query => {
  const value = query?.event || ''
  if (/^maian:choose-location:[a-z0-9-]+$/.test(value)) event = value
})
onMounted(async () => {
  // #ifdef H5
  try {
    const L = await import('leaflet')
    if (disposed) return
    map = L.map('location-picker-map', { zoomControl: true }).setView([DEFAULT_MAP_CENTER.latitude, DEFAULT_MAP_CENTER.longitude], 14)
    addBaseTileLayer(L, map, () => { unavailable.value = true })
    map.on('click', position => {
      selected.value = { latitude: position.latlng.lat, longitude: position.latlng.lng }
      if (marker) marker.setLatLng(position.latlng)
      else marker = L.circleMarker(position.latlng, { radius: 9, color: '#FFFFFF', fillColor: '#007AFF', fillOpacity: 1, weight: 3 }).addTo(map!)
    })
  } catch { unavailable.value = true }
  // #endif
})
function confirm() {
  if (!selected.value || unavailable.value || !event || delivered) return
  delivered = true
  uni.$emit(event, selected.value)
  uni.navigateBack()
}
onUnload(() => {
  disposed = true
  if (!delivered && event) uni.$emit(event)
  // #ifdef H5
  map?.remove()
  // #endif
})
</script>

<style scoped>
.location-picker { display: flex; flex-direction: column; height: calc(100vh - 44px - env(safe-area-inset-top)); background: #F2F2F7; }
.location-picker-hint { flex: none; padding: 12px 16px; font-size: 13px; color: #636366; }
.location-picker-map { flex: 1; min-height: 0; z-index: 0; }
.location-picker-footer { flex: none; padding: 14px 16px max(14px,env(safe-area-inset-bottom)); background: #FFF; font-size: 12px; color: #636366; }
.location-picker-footer > text { display: block; }
.location-picker-confirm { margin-top: 12px; padding: 10px; border: 0; border-radius: 10px; background: #007AFF; color: #FFF; font-size: 14px; }
.location-picker-confirm::after { border: 0; }.location-picker-confirm[disabled] { background: #E5E5EA; color: #AEAEB2; }
.location-picker-error { color: #B54A45; }
</style>
