<template>
  <view class="location-picker apple-page">
    <view class="location-search">
      <input v-model="keyword" placeholder="搜索地点名称或地址" confirm-type="search" @input="clearSearch" @confirm="search" />
      <button :disabled="searching || !keyword.trim()" @tap="search">{{ searching ? '搜索中' : '搜索' }}</button>
    </view>
    <scroll-view v-if="results.length" scroll-y class="location-results">
      <view v-for="(place, index) in results" :key="index" class="location-result" @tap="selectLocation(place)">
        <text>{{ place.name }}</text><text class="location-result-address">{{ place.address }}</text>
      </view>
    </scroll-view>
    <text class="location-picker-hint">搜索地点，或点击地图选择实际位置</text>
    <view id="location-picker-map" class="location-picker-map"></view>
    <view class="location-picker-footer">
      <text v-if="unavailable" class="location-picker-error">底图加载失败，请返回后重试定位。</text>
      <text v-else-if="selected">{{ selected.address || `已选择：${selected.latitude.toFixed(5)}, ${selected.longitude.toFixed(5)}` }}</text>
      <text v-else>尚未选择位置，地图中心不会自动作为现场或设备位置</text>
      <button class="location-picker-confirm" :disabled="!selected || unavailable" @tap="confirm">使用这个位置</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { DEFAULT_MAP_CENTER, type AppLocation } from '@/utils/location'
import { loadAMap } from '@/common/amap'
// #ifdef H5
import 'leaflet/dist/leaflet.css'
import type { Map as LeafletMap, CircleMarker } from 'leaflet'
import { addBaseTileLayer } from '@/common/mapTiles'
let map: LeafletMap | undefined, marker: CircleMarker | undefined
let leaflet: typeof import('leaflet')
// #endif
interface Place extends AppLocation { name: string }
const selected = ref<AppLocation | null>(null), unavailable = ref(false)
const keyword = ref(''), searching = ref(false), results = ref<Place[]>([])
let event = '', delivered = false, disposed = false, searchVersion = 0, selectionVersion = 0
onLoad(query => {
  const value = query?.event || ''
  if (/^maian:choose-location:[a-z0-9-]+$/.test(value)) event = value
  if (query?.longitude != null && query?.latitude != null) {
    const longitude = Number(query.longitude), latitude = Number(query.latitude)
    if (Number.isFinite(longitude) && Math.abs(longitude) <= 180 && Number.isFinite(latitude) && Math.abs(latitude) <= 90) {
      selected.value = { longitude, latitude, address: query.address || undefined }
    }
  }
})
onMounted(async () => {
  // #ifdef H5
  try {
    leaflet = await import('leaflet')
    if (disposed) return
    const center = selected.value || DEFAULT_MAP_CENTER
    map = leaflet.map('location-picker-map', { zoomControl: true }).setView([center.latitude, center.longitude], 14)
    addBaseTileLayer(leaflet, map, () => { unavailable.value = true })
    if (selected.value) selectLocation(selected.value)
    map.on('click', position => { void selectLocation({ latitude: position.latlng.lat, longitude: position.latlng.lng }, true) })
  } catch { unavailable.value = true }
  // #endif
})

// Search and address lookup are optional: map selection still works when the SDK is unavailable.
function amapRequest<T>(plugin: string, request: (sdk: any, finish: (value: T | null) => void) => void): Promise<T | null> {
  return new Promise(resolve => {
    let finished = false
    const finish = (value: T | null) => {
      if (finished) return
      finished = true
      clearTimeout(timer)
      resolve(value)
    }
    const timer = setTimeout(() => finish(null), 6500)
    loadAMap().then(loaded => {
      if (!loaded || disposed || finished) { finish(null); return }
      const sdk = (window as any).AMap
      sdk.plugin([plugin], () => {
        if (disposed || finished) { finish(null); return }
        try { request(sdk, finish) } catch { finish(null) }
      })
    }).catch(() => finish(null))
  })
}
function clearSearch() {
  searchVersion++
  searching.value = false
  results.value = []
}
async function search() {
  const text = keyword.value.trim()
  if (!text || searching.value) return
  const version = ++searchVersion
  searching.value = true
  const places = await amapRequest<Place[]>('AMap.PlaceSearch', (sdk, finish) => {
    new sdk.PlaceSearch({ pageSize: 10 }).search(text, (status: string, result: any) => {
      finish(status === 'complete' ? (result.poiList?.pois || []).filter((poi: any) => poi.location).map((poi: any) => ({
        name: poi.name, address: typeof poi.address === 'string' && poi.address ? poi.address : poi.name,
        longitude: Number(poi.location.lng), latitude: Number(poi.location.lat)
      })) : null)
    })
  })
  if (disposed || version !== searchVersion) return
  searching.value = false
  results.value = places || []
  if (!places?.length) uni.showToast({ title: places ? '未找到相关地点' : '地点搜索暂不可用，可直接点击地图', icon: 'none' })
}
async function selectLocation(location: AppLocation, resolveAddress = false) {
  const version = ++selectionVersion
  selected.value = { ...location }
  clearSearch()
  // #ifdef H5
  if (map) {
    const point: [number, number] = [location.latitude, location.longitude]
    if (marker) marker.setLatLng(point)
    else marker = leaflet.circleMarker(point, { radius: 9, color: '#FFFFFF', fillColor: '#007AFF', fillOpacity: 1, weight: 3 }).addTo(map)
    map.panTo(point)
  }
  // #endif
  if (!resolveAddress) return
  const address = await amapRequest<string>('AMap.Geocoder', (sdk, finish) => {
    new sdk.Geocoder().getAddress([location.longitude, location.latitude], (status: string, result: any) => {
      finish(status === 'complete' ? result.regeocode?.formattedAddress || null : null)
    })
  })
  if (!disposed && version === selectionVersion && address) selected.value = { ...location, address }
}
function confirm() {
  if (!selected.value || unavailable.value || !event || delivered) return
  delivered = true
  uni.$emit(event, { ...selected.value })
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
.location-search { display: flex; align-items: center; gap: 8px; padding: 12px 16px 0; }
.location-search input { flex: 1; min-width: 0; padding: 10px; background: #FFF; border-radius: 10px; font-size: 14px; }
.location-search button { flex: none; margin: 0; padding: 0 12px; background: transparent; color: #007AFF; font-size: 14px; }
.location-search button::after { border: 0; }
.location-results { flex: none; max-height: 220px; background: #FFF; }
.location-result { padding: 12px 16px; border-bottom: 1px solid #F2F2F7; font-size: 14px; }
.location-result-address { display: block; margin-top: 4px; color: #636366; font-size: 12px; }
.location-picker-hint { flex: none; padding: 12px 16px; font-size: 13px; color: #636366; }
.location-picker-map { flex: 1; min-height: 0; z-index: 0; }
.location-picker-footer { flex: none; padding: 14px 16px max(14px,env(safe-area-inset-bottom)); background: #FFF; font-size: 12px; color: #636366; }
.location-picker-footer > text { display: block; }
.location-picker-confirm { margin-top: 12px; padding: 10px; border: 0; border-radius: 10px; background: #007AFF; color: #FFF; font-size: 14px; }
.location-picker-confirm::after { border: 0; }.location-picker-confirm[disabled] { background: #E5E5EA; color: #AEAEB2; }
.location-picker-error { color: #B54A45; }
</style>
