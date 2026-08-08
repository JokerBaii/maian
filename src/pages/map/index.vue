<template>
  <view class="map-page">
    <view class="top-overlay" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="search-bar">
        <view class="search-icon-wrap">
          <app-icon class="search-icon" name="search" :size="18" color="#56627A" />
        </view>
        <input
          class="search-input"
          v-model="searchText"
          placeholder="搜索附近急救设备"
          placeholder-class="search-placeholder"
          confirm-type="search"
          @confirm="handleSearch"
        />
        <view v-if="searchText" class="search-clear" @tap="searchText = ''">
          <app-icon name="closeempty" :size="17" color="#FFFFFF" />
        </view>
      </view>
      <view class="filter-tabs">
        <view
          v-for="tab in filterTabs"
          :key="tab.key"
          class="filter-tab"
          :class="{ 'filter-tab-active': activeFilter === tab.key }"
          @tap="activeFilter = tab.key"
        >
          <text class="filter-tab-text">{{ tab.label }}</text>
          <view v-if="activeFilter === tab.key" class="filter-tab-indicator"></view>
        </view>
      </view>
    </view>

    <!-- #ifdef H5 -->
    <view id="map-container" class="map-container"></view>
    <view v-if="mapUnavailable" class="map-fallback">
      <view class="map-grid"></view>
      <view class="radar-caption">
        <view class="radar-caption-state">
          <view class="radar-caption-dot"></view>
          <text>救援资源持续在线</text>
        </view>
        <text class="radar-caption-title">附近生命网络</text>
        <text class="radar-caption-desc">急救设备数据已连接 · 可使用系统地图导航</text>
      </view>
      <view class="rescue-radar">
        <view class="radar-ring radar-ring-one"></view>
        <view class="radar-ring radar-ring-two"></view>
        <view class="radar-ring radar-ring-three"></view>
        <view class="radar-cross radar-cross-horizontal"></view>
        <view class="radar-cross radar-cross-vertical"></view>
        <view class="radar-sweep"></view>
        <view
          v-for="node in radarNodes"
          :key="node.label"
          class="radar-node"
          :class="'radar-node-' + node.type"
          :style="{ left: node.left, top: node.top }"
        >
          <view class="radar-node-core">
            <image class="radar-node-glyph" :src="node.glyph" mode="aspectFit" />
          </view>
          <text class="radar-node-label">{{ node.label }}</text>
        </view>
        <view class="radar-self">
          <view class="radar-self-ring"></view>
          <view class="radar-self-core">
            <app-icon name="navigate-filled" :size="18" color="#FFFFFF" />
          </view>
        </view>
      </view>
    </view>
    <!-- #endif -->
    <!-- #ifndef H5 -->
    <map
      id="native-rescue-map"
      class="map-container"
      :longitude="myLocation?.[0] ?? FIXED_LOCATION.longitude"
      :latitude="myLocation?.[1] ?? FIXED_LOCATION.latitude"
      :markers="nativeMarkers"
      :scale="13"
      show-location
      enable-3D
      @markertap="handleNativeMarkerTap"
    />
    <!-- #endif -->

    <view v-if="selectedDevice" class="device-popup" :class="{ 'device-popup-show': popupVisible }">
      <view class="popup-header">
        <view class="popup-type-tag" :class="'popup-type-' + selectedDevice.type">
          <text class="popup-type-text">{{ selectedDevice.type === 'fixed' ? '固定' : '移动' }}</text>
        </view>
        <view class="popup-category-tag">
          <text class="popup-category-text">{{ selectedDevice.category }}</text>
        </view>
        <view class="popup-close" @tap="closePopup">
          <app-icon name="closeempty" :size="17" color="#748198" />
        </view>
      </view>
      <view class="popup-body">
        <text class="popup-name">{{ selectedDevice.name }}</text>
        <text class="popup-address">{{ selectedDevice.address }}</text>
        <view class="popup-info-row">
          <view class="popup-status" :class="'popup-status-' + deviceStatusClass(selectedDevice)">
            <view class="popup-status-dot"></view>
            <text class="popup-status-text">{{ deviceStatusLabel(selectedDevice) }}</text>
          </view>
          <text class="popup-distance">{{ selectedDevice._distance || '--' }}</text>
        </view>
        <view v-if="selectedDevice.type === 'mobile' && (selectedDevice as any).vehicleInfo" class="popup-vehicle">
          <text class="popup-vehicle-label">车辆信息</text>
          <text class="popup-vehicle-text">{{ (selectedDevice as any).vehicleInfo }}</text>
        </view>
        <view v-if="selectedDevice.type === 'mobile' && (selectedDevice as any).serviceTime" class="popup-vehicle">
          <text class="popup-vehicle-label">可用时段</text>
          <text class="popup-vehicle-text">{{ (selectedDevice as any).serviceTime }}</text>
        </view>
      </view>
      <view class="popup-footer">
        <view class="popup-btn popup-btn-call" @tap="handleCallDevice">
          <app-icon class="popup-btn-icon" name="phone-filled" :size="17" color="#FFFFFF" />
          <text class="popup-btn-text">联系</text>
        </view>
        <view class="popup-btn popup-btn-nav" @tap="handleNavigate(selectedDevice)">
          <app-icon class="popup-btn-icon" name="navigate-filled" :size="17" color="#FFFFFF" />
          <text class="popup-btn-text">导航</text>
        </view>
      </view>
    </view>

    <view
      class="bottom-drawer"
      :class="{
        'bottom-drawer-expanded': drawerExpanded,
        'bottom-drawer-dragging': drawerDragging
      }"
      :style="{ transform: 'translateY(' + drawerOffset + 'rpx)' }"
    >
      <view
        class="drawer-drag-zone"
        @tap="toggleDrawer"
        @touchstart.stop="onDrawerTouchStart"
        @touchmove.stop.prevent="onDrawerTouchMove"
        @touchend.stop="onDrawerTouchEnd"
        @touchcancel.stop="onDrawerTouchEnd"
      >
        <view class="drawer-handle">
          <view class="drawer-handle-bar"></view>
        </view>
        <view class="drawer-header">
          <view class="drawer-heading">
            <view class="drawer-title-row">
              <text class="drawer-title">附近设备</text>
              <text class="drawer-count">{{ filteredDevices.length }}台</text>
            </view>
            <view class="drawer-summary">
              <view class="drawer-live-dot"></view>
              <text>{{ availableDeviceCount }}台可调度 · 已按距离由近到远排序</text>
            </view>
          </view>
          <view class="drawer-toggle">
            <text>{{ drawerExpanded ? '收起' : '展开' }}</text>
            <app-icon
              :name="drawerExpanded ? 'down' : 'up'"
              :size="15"
              color="#2E6DD1"
            />
          </view>
        </view>
      </view>
      <scroll-view
        class="drawer-list"
        scroll-y
        :show-scrollbar="false"
        :enhanced="true"
        :bounces="true"
        @touchstart.stop
        @touchmove.stop
      >
        <view
          v-for="(device, index) in filteredDevices"
          :key="device.id"
          class="device-card"
          :class="'device-row-' + deviceStatusClass(device)"
          @tap="selectDevice(device)"
        >
          <view class="device-card-icon" :class="'device-icon-' + deviceStatusClass(device)">
            <image class="device-card-glyph" :src="markerAsset(device)" mode="aspectFit" />
          </view>
          <view class="device-card-center">
            <view class="device-card-name-row">
              <text class="device-card-name">{{ device.name }}</text>
              <view v-if="index === 0 && deviceStatusClass(device) === 'online'" class="nearest-tag">
                <text>距你最近</text>
              </view>
            </view>
            <view class="device-card-meta">
              <text class="device-card-addr">{{ device.address }}</text>
            </view>
            <view class="device-card-bottom">
              <view class="status-chip" :class="'chip-' + deviceStatusClass(device)">
                <view class="card-status-dot"></view>
                <text class="card-status-text">{{ deviceStatusLabel(device) }}</text>
              </view>
              <text class="device-card-kind">{{ device.category }} / {{ device.type === 'fixed' ? '固定设备' : '移动设备' }}</text>
            </view>
          </view>
          <view class="device-card-right">
            <text class="device-card-distance">{{ device._distance }}</text>
            <view class="nav-btn" @tap.stop="handleNavigate(device)">
              <text>导航</text>
              <app-icon class="nav-btn-icon" name="right" :size="13" color="#2E6DD1" />
            </view>
          </view>
        </view>
        <view v-if="filteredDevices.length === 0" class="empty-state">
          <app-icon class="empty-icon" name="search" :size="38" color="#8994A8" />
          <text class="empty-text">附近暂无设备</text>
        </view>
        <view class="drawer-bottom-safe"></view>
      </scroll-view>
    </view>

    <view class="locate-btn" @tap="locateMe">
      <app-icon class="locate-icon" name="location-filled" :size="24" color="#1F63D5" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
// #ifdef H5
import 'leaflet/dist/leaflet.css'
// #endif
import AppIcon from '@/components/AppIcon.vue'
import { getCurrentGcj02Location, FIXED_LOCATION, isDemoMode } from '@/utils/location'
import { loadAMap } from '@/common/amap'
import { addBaseTileLayer } from '@/common/mapTiles'
import { listEmergencyDevices, type EmergencyDeviceResponse } from '@/api/devices'

const statusBarHeight = ref(0)
const systemInfo = uni.getSystemInfoSync()
statusBarHeight.value = systemInfo.statusBarHeight || 20

const searchText = ref('')
const activeFilter = ref('all')
const mapUnavailable = ref(false)
const filterTabs = [
  { key: 'all', label: '全部' },
  { key: 'fixed', label: '固定设备' },
  { key: 'mobile', label: '移动设备' }
]
const radarNodes = [
  { label: 'AED', glyph: '/static/icons/entry/aed.png', type: 'device', left: '18%', top: '58%' },
  { label: '急救箱', glyph: '/static/icons/entry/rescue-kit.png', type: 'kit', left: '70%', top: '24%' },
  { label: '志愿者', glyph: '/static/icons/entry/volunteer.png', type: 'volunteer', left: '75%', top: '70%' },
  { label: 'AED', glyph: '/static/icons/entry/aed.png', type: 'device', left: '30%', top: '19%' }
]

let mapInstance: any = null
let mapProvider: 'amap' | 'leaflet' | null = null
let leafletApi: any = null
let markerMap: Record<string, {
  marker: any
  iconKey: string
  longitude: number
  latitude: number
}> = {}
let deviceById: Record<string, any> = {}
/** 自身位置标记。H5 分支没有原生 show-location，需要自己画。 */
let selfMarker: any = null
/** 自身位置标记的内联结构。样式必须内联，原因见 buildLeafletIcon。 */
const SELF_MARKER_HTML =
  '<span style="position:relative;display:block;width:16px;height:16px;border-radius:50%;'
  + 'background:rgba(47,115,232,0.22)">'
  + '<i style="position:absolute;inset:4px;border-radius:50%;border:1.5px solid #FFFFFF;'
  + 'background:#2F73E8;box-shadow:0 1px 4px rgba(47,115,232,0.5)"></i></span>'

// 固定坐标只是 Demo Mode 的演示位置；正式模式在真实定位前不伪造用户位置。
const myLocation = ref<number[] | null>(isDemoMode
  ? [FIXED_LOCATION.longitude, FIXED_LOCATION.latitude]
  : null)
const remoteDevices = ref<any[] | null>(null)

function calculateDistance(longitude: number, latitude: number) {
  if (!myLocation.value) return Number.POSITIVE_INFINITY
  const [myLongitude, myLatitude] = myLocation.value
  const toRadians = (value: number) => value * Math.PI / 180
  const latDelta = toRadians(latitude - myLatitude)
  const lngDelta = toRadians(longitude - myLongitude)
  const a = Math.sin(latDelta / 2) ** 2
    + Math.cos(toRadians(myLatitude)) * Math.cos(toRadians(latitude))
    * Math.sin(lngDelta / 2) ** 2
  return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

function formatDistance(distance: number) {
  if (!Number.isFinite(distance)) return '定位中'
  return distance < 1 ? `${Math.max(50, Math.round(distance * 1000))}m` : `${distance.toFixed(1)}km`
}

function mapRemoteDevice(device: EmergencyDeviceResponse) {
  const type = device.type.toLowerCase()
  const online = device.status === 'AVAILABLE'
  return {
    ...device,
    type,
    status: type === 'mobile' ? (online ? 'online' : 'offline') : device.status.toLowerCase(),
    online
  }
}

const allDevices = computed(() => {
  const source = remoteDevices.value || []
  return source
    .map(device => {
      const distance = calculateDistance(device.longitude, device.latitude)
      return {
        ...device,
        _distance: formatDistance(distance),
        _distanceValue: distance
      }
    })
    .sort((left, right) => left._distanceValue - right._distanceValue)
})

async function loadDevices() {
  try {
    const result = await listEmergencyDevices()
    remoteDevices.value = result.content.map(mapRemoteDevice)
  } catch {
    remoteDevices.value = []
    uni.showToast({ title: '急救设备数据加载失败', icon: 'none' })
  }
}

const filteredDevices = computed(() => {
  let list = allDevices.value
  if (activeFilter.value === 'fixed') {
    list = list.filter(d => d.type === 'fixed')
  } else if (activeFilter.value === 'mobile') {
    list = list.filter(d => d.type === 'mobile')
  }
  if (searchText.value) {
    const kw = searchText.value.toLowerCase()
    list = list.filter(d =>
      d.name.toLowerCase().includes(kw) ||
      d.address.toLowerCase().includes(kw) ||
      d.category.toLowerCase().includes(kw)
    )
  }
  return list
})

const availableDeviceCount = computed(() => (
  filteredDevices.value.filter(device => deviceStatusClass(device) === 'online').length
))

const nativeMarkers = computed(() => filteredDevices.value.map((device, index) => ({
  id: index + 1,
  longitude: device.longitude,
  latitude: device.latitude,
  iconPath: markerAsset(device, true),
  width: 24,
  height: 30,
  callout: {
    content: device.category,
    color: '#1C2B45',
    fontSize: 12,
    borderRadius: 8,
    bgColor: '#FFFFFF',
    padding: 6,
    display: 'BYCLICK'
  }
})))

function markerAsset(device: any, _png = false) {
  const extension = 'png'
  const type = device.type === 'mobile' ? 'mobile' : 'fixed'
  const state = deviceStatusClass(device) === 'online' ? '' : '-offline'
  const filename = `marker-${type}${state}.${extension}`
  // #ifdef H5
  const baseUrl = ((import.meta as any).env?.BASE_URL || '/').replace(/\/?$/, '/')
  return `${baseUrl}static/map/${filename}`
  // #endif
  // #ifndef H5
  return `/static/map/${filename}`
  // #endif
}

function preloadMarkerAssets() {
  // #ifdef H5
  if (typeof Image === 'undefined') return
  const assets = [
    { type: 'fixed', status: 'available' },
    { type: 'fixed', status: 'maintenance' },
    { type: 'mobile', status: 'online', online: true },
    { type: 'mobile', status: 'offline', online: false }
  ]
  assets.forEach(device => {
    const image = new Image()
    image.decoding = 'async'
    image.src = markerAsset(device)
  })
  // #endif
}

const selectedDevice = ref<any>(null)
const popupVisible = ref(false)

const drawerExpanded = ref(false)
const drawerOffset = ref(420)
const drawerDragging = ref(false)
const touchStartY = ref(0)
const touchCurrentY = ref(0)
const drawerRpxRatio = 750 / Math.max(systemInfo.windowWidth || 375, 1)

function deviceStatusClass(device: any) {
  if (device.status === 'reserved') return 'reserved'
  if (device.type === 'fixed') {
    return device.status === 'available' ? 'online' : 'offline'
  }
  return device.online ? 'online' : 'offline'
}

function deviceStatusLabel(device: any) {
  if (device.status === 'reserved') return '救援占用中'
  if (device.type === 'fixed') {
    return device.status === 'available' ? '可用' : '维护中'
  }
  return device.online ? '在线' : '离线'
}

// #ifdef H5
async function initMap() {
  const loaded = await loadAMap()
  if (!loaded) {
    await initLeafletMap()
    return
  }
  mapUnavailable.value = false
  mapProvider = 'amap'
  const AMap = (window as any).AMap
  const center = myLocation.value || [FIXED_LOCATION.longitude, FIXED_LOCATION.latitude]

  // 与 Leaflet 分支保持一致，统一以当前位置为中心
  mapInstance = new AMap.Map('map-container', {
    zoom: 14,
    center: [...center],
    mapStyle: 'amap://styles/whitesmoke',
    resizeEnable: true
  })

  updateSelfMarker()
  addMarkers()
}

async function initLeafletMap() {
  try {
    const module = await import('leaflet')
    leafletApi = module.default || module
    mapProvider = 'leaflet'
    mapUnavailable.value = false
    const center = myLocation.value || [FIXED_LOCATION.longitude, FIXED_LOCATION.latitude]
    mapInstance = leafletApi.map('map-container', {
      zoomControl: false,
      attributionControl: true,
      preferCanvas: true
    }).setView([center[1], center[0]], 14)
    addBaseTileLayer(leafletApi, mapInstance, () => {
      mapUnavailable.value = true
      uni.showToast({ title: '底图加载失败，已切换为资源视图', icon: 'none', duration: 2000 })
    })
    leafletApi.control.zoom({ position: 'bottomright' }).addTo(mapInstance)
    updateSelfMarker()
    addMarkers()
  } catch {
    mapProvider = null
    mapUnavailable.value = true
    uni.showToast({ title: '地图底图加载失败', icon: 'none', duration: 2000 })
  }
}
// #endif

/** 画出或移动自身位置标记，让用户能看到自己与附近设备的相对位置。 */
function updateSelfMarker() {
  if (!mapInstance || !myLocation.value) return
  const [longitude, latitude] = myLocation.value

  if (mapProvider === 'leaflet') {
    if (selfMarker) {
      selfMarker.setLatLng([latitude, longitude])
      return
    }
    selfMarker = leafletApi.marker([latitude, longitude], {
      icon: leafletApi.divIcon({
        className: 'rescue-self-marker',
        html: SELF_MARKER_HTML,
        iconSize: [16, 16],
        iconAnchor: [8, 8]
      }),
      zIndexOffset: 1000,
      interactive: false
    }).addTo(mapInstance)
    return
  }

  const AMap = (window as any).AMap
  if (!AMap) return
  const position = new AMap.LngLat(longitude, latitude)
  if (selfMarker) {
    selfMarker.setPosition(position)
    return
  }
  const content = document.createElement('div')
  content.className = 'rescue-self-marker'
  content.innerHTML = SELF_MARKER_HTML
  selfMarker = new AMap.Marker({
    position,
    content,
    offset: new AMap.Pixel(-8, -8),
    zIndex: 200
  })
  mapInstance.add(selfMarker)
}

/** 标记外观只由类型和状态决定，用于判断是否需要重建图标。 */
function markerIconKey(device: any) {
  return `${device.type}-${deviceStatusClass(device)}`
}

// 按 id 复用标记，只处理增删和真正变化的部分；全量重建会导致明显卡顿
function addMarkers() {
  if (!mapInstance || typeof window === 'undefined') return
  if (mapProvider !== 'leaflet' && (mapProvider !== 'amap' || !(window as any).AMap)) return

  const devices = filteredDevices.value
  const nextIds = new Set(devices.map(device => String(device.id)))
  deviceById = {}
  devices.forEach(device => {
    deviceById[String(device.id)] = device
  })

  Object.keys(markerMap).forEach(id => {
    if (nextIds.has(id)) return
    const entry = markerMap[id]
    if (mapProvider === 'leaflet') {
      entry.marker.remove()
    } else {
      mapInstance.remove(entry.marker)
    }
    delete markerMap[id]
  })

  devices.forEach(device => {
    const id = String(device.id)
    const iconKey = markerIconKey(device)
    const existing = markerMap[id]

    if (existing) {
      if (existing.iconKey !== iconKey) {
        if (mapProvider === 'leaflet') {
          existing.marker.setIcon(buildLeafletIcon(device))
        } else {
          existing.marker.setContent(buildAmapContent(device))
        }
        existing.iconKey = iconKey
      }
      if (existing.longitude !== device.longitude || existing.latitude !== device.latitude) {
        if (mapProvider === 'leaflet') {
          existing.marker.setLatLng([device.latitude, device.longitude])
        } else {
          existing.marker.setPosition(new (window as any).AMap.LngLat(device.longitude, device.latitude))
        }
        existing.longitude = device.longitude
        existing.latitude = device.latitude
      }
      return
    }

    let marker: any
    if (mapProvider === 'leaflet') {
      marker = leafletApi.marker([device.latitude, device.longitude], { icon: buildLeafletIcon(device) })
        .addTo(mapInstance)
        .on('click', () => selectDeviceById(id))
    } else {
      const AMap = (window as any).AMap
      marker = new AMap.Marker({
        position: new AMap.LngLat(device.longitude, device.latitude),
        content: buildAmapContent(device),
        offset: new AMap.Pixel(-9, -21),
        title: device.name
      })
      marker.on('click', () => selectDeviceById(id))
      mapInstance.add(marker)
    }

    markerMap[id] = {
      marker,
      iconKey,
      longitude: device.longitude,
      latitude: device.latitude
    }
  })
}

// divIcon 由 Leaflet 动态创建，拿不到 scoped 的 data-v 属性，尺寸必须内联
function buildLeafletIcon(device: any) {
  return leafletApi.divIcon({
    className: 'rescue-leaflet-marker',
    html: `<img src="${markerAsset(device)}" alt="" style="display:block;width:18px;height:22px;filter:drop-shadow(0 2px 4px rgba(28,54,82,0.22))">`,
    iconSize: [18, 22],
    iconAnchor: [9, 21]
  })
}

function buildAmapContent(device: any) {
  const markerContent = document.createElement('div')
  const markerImage = document.createElement('img')
  markerImage.src = markerAsset(device)
  markerImage.alt = ''
  markerImage.width = 18
  markerImage.height = 22
  markerImage.style.display = 'block'
  markerContent.appendChild(markerImage)
  return markerContent
}

/** 标记复用后点击回调不能捕获旧的设备对象，统一按 id 取当前数据。 */
function selectDeviceById(id: string) {
  const device = deviceById[id]
  if (device) selectDevice(device)
}

function selectDevice(device: any) {
  selectedDevice.value = device
  popupVisible.value = true

  if (mapInstance) {
    if (mapProvider === 'leaflet') {
      mapInstance.panTo([device.latitude, device.longitude])
    } else {
      mapInstance.setCenter([device.longitude, device.latitude])
    }
  }
}

function handleNativeMarkerTap(event: any) {
  const markerId = Number(event.detail?.markerId)
  const device = filteredDevices.value[markerId - 1]
  if (device) selectDevice(device)
}

function closePopup() {
  popupVisible.value = false
  setTimeout(() => {
    selectedDevice.value = null
  }, 300)
}

function handleCallDevice() {
  const phoneNumber = selectedDevice.value?.ownerPhone?.trim()
  if (!phoneNumber) {
    uni.showToast({ title: '该设备未登记联系电话', icon: 'none' })
    return
  }
  uni.makePhoneCall({ phoneNumber })
}

function handleNavigate(device: any) {
  closePopup()
  uni.openLocation({
    longitude: Number(device.longitude),
    latitude: Number(device.latitude),
    name: device.name,
    address: device.address,
    scale: 16,
    fail: () => {
      uni.showToast({ title: '无法打开系统地图', icon: 'none' })
    }
  })
}

function handleSearch() {
  addMarkers()
}

async function locateMe(showResult = true) {
  const applyLocation = (longitude: number, latitude: number) => {
    myLocation.value = [longitude, latitude]
    updateSelfMarker()
    if (mapInstance) {
      if (mapProvider === 'leaflet') {
        mapInstance.setView([latitude, longitude], 14)
      } else {
        mapInstance.setCenter(myLocation.value)
        mapInstance.setZoom(14)
      }
    }
    // #ifndef H5
    uni.createMapContext('native-rescue-map').moveToLocation({ longitude, latitude })
    // #endif
    if (showResult) uni.showToast({ title: '定位已更新', icon: 'success' })
  }

  return getCurrentGcj02Location().then((result) => {
    applyLocation(result.longitude, result.latitude)
  })
}

function onDrawerTouchStart(e: any) {
  drawerDragging.value = true
  touchStartY.value = e.touches[0].clientY
  touchCurrentY.value = e.touches[0].clientY
}

function onDrawerTouchMove(e: any) {
  touchCurrentY.value = e.touches[0].clientY
  const diff = (touchCurrentY.value - touchStartY.value) * drawerRpxRatio
  const base = drawerExpanded.value ? 0 : 420
  let offset = base + diff
  if (offset < 0) offset = 0
  if (offset > 420) offset = 420
  drawerOffset.value = offset
}

function onDrawerTouchEnd() {
  drawerDragging.value = false
  const diff = touchCurrentY.value - touchStartY.value
  if (drawerExpanded.value) {
    if (diff > 60) {
      drawerExpanded.value = false
      drawerOffset.value = 420
    } else {
      drawerOffset.value = 0
    }
  } else {
    if (diff < -60) {
      drawerExpanded.value = true
      drawerOffset.value = 0
    } else {
      drawerOffset.value = 420
    }
  }
}

function toggleDrawer() {
  if (Math.abs(touchCurrentY.value - touchStartY.value) > 8) return
  drawerExpanded.value = !drawerExpanded.value
  drawerOffset.value = drawerExpanded.value ? 0 : 420
}

onMounted(() => {
  nextTick(async () => {
    preloadMarkerAssets()
    await locateMe(false)
    const devicesPromise = loadDevices()
    // #ifdef H5
    const mapPromise = initMap()
    // #endif
    await devicesPromise
    // #ifdef H5
    await mapPromise
    // #endif
    initializedRef = true
  })
})

// 地图是 tab 页：切回时重新拉设备，保证"审核上架新设备"等变化立即可见。
// initMap 只跑一次，这里只刷数据不重建地图。
onShow(() => {
  if (!initializedRef) return
  loadDevices()
  scheduleMarkerUpdate()
})

// 搜索输入是逐字触发的，合并到一帧后再更新标记，避免每敲一个字都跑一遍。
let markerUpdateTimer: ReturnType<typeof setTimeout> | null = null
let initializedRef = false

function scheduleMarkerUpdate() {
  if (markerUpdateTimer) clearTimeout(markerUpdateTimer)
  markerUpdateTimer = setTimeout(() => {
    markerUpdateTimer = null
    addMarkers()
  }, 120)
}

watch([activeFilter, searchText, remoteDevices, myLocation], scheduleMarkerUpdate)

onUnmounted(() => {
  if (markerUpdateTimer) {
    clearTimeout(markerUpdateTimer)
    markerUpdateTimer = null
  }
  if (mapProvider === 'leaflet' && mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }
  markerMap = {}
  deviceById = {}
  selfMarker = null
})
</script>

<style>
/* #ifdef H5 */
.rescue-leaflet-marker {
  background: transparent;
  border: 0;
}
.rescue-leaflet-marker img {
  display: block;
  width: 18px;
  height: 22px;
  filter: drop-shadow(0 2px 4px rgba(28, 54, 82, 0.22));
}

/* 自身位置：蓝色圆点加光圈，与设备标记明显区分 */
.rescue-self-marker {
  background: transparent;
  border: 0;
}
.rescue-self-marker span {
  position: relative;
  display: block;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: rgba(47, 115, 232, 0.22);
}
.rescue-self-marker i {
  position: absolute;
  inset: 4px;
  border-radius: 50%;
  border: 1.5px solid #FFFFFF;
  background: #2F73E8;
  box-shadow: 0 1px 4px rgba(47, 115, 232, 0.5);
}
.leaflet-control-attribution {
  font-size: 9px;
}
/* #endif */
</style>

<style lang="scss" scoped>
.map-page {
  position: relative;
  width: 100%;
  height: calc(100vh - var(--window-top, 0px) - var(--window-bottom, 0px));
  overflow: hidden;
  background: #EDF5FB;
}

.top-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  padding-bottom: 18rpx;
  border-bottom: 1rpx solid rgba(143, 169, 191, 0.18);
  background: rgba(247, 251, 254, 0.9);
  backdrop-filter: blur(16px);
}
.search-bar {
  display: flex;
  align-items: center;
  margin: 0 24rpx;
  height: 80rpx;
  border: 1rpx solid #D7E4EF;
  border-radius: 16rpx;
  background: #FFFFFF;
  padding: 0 24rpx;
}
.search-icon-wrap {
  margin-right: 12rpx;
}
.search-icon {
  font-size: 28rpx;
}
.search-input {
  flex: 1;
  height: 80rpx;
  font-size: 28rpx;
  color: #20364D;
}
.search-placeholder {
  color: #C9CDD4;
  font-size: 28rpx;
}
.search-clear {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: #C9CDD4;
  display: flex;
  align-items: center;
  justify-content: center;
}
.filter-tabs {
  display: flex;
  padding: 16rpx 24rpx 0;
  gap: 8rpx;
}
.filter-tab {
  position: relative;
  padding: 12rpx 32rpx;
  border: 1rpx solid #D9E5EF;
  border-radius: 12rpx;
  background: rgba(255,255,255,0.72);
  transition: background 150ms ease, border-color 150ms ease;
}
.filter-tab-active {
  border-color: #2E6DD1;
  background: #E8F1FD;
}
.filter-tab-text {
  font-size: 26rpx;
  color: #4E5969;
  font-weight: 500;
}
.filter-tab-active .filter-tab-text {
  color: #245FAF;
  font-weight: 600;
}
.filter-tab-indicator {
  display: none;
}

.map-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  background: #EDF5FB;
}

.map-fallback {
  position: fixed;
  inset: 0;
  z-index: 2;
  overflow: hidden;
  background:
    radial-gradient(circle at 50% 45%, rgba(86, 166, 226, 0.12), transparent 42%),
    linear-gradient(180deg, #F6FBFF 0%, #EAF4FA 100%);
}

.map-grid {
  position: absolute;
  inset: 0;
  opacity: 0.35;
  background-image:
    linear-gradient(rgba(79, 130, 171, 0.09) 1rpx, transparent 1rpx),
    linear-gradient(90deg, rgba(79, 130, 171, 0.09) 1rpx, transparent 1rpx);
  background-size: 58rpx 58rpx;
  transform: perspective(700px) rotateX(52deg) scale(1.35);
  transform-origin: center 45%;
}

.radar-caption {
  position: absolute;
  top: 245rpx;
  left: 30rpx;
  z-index: 3;
}

.radar-caption-state {
  display: inline-flex;
  align-items: center;
  gap: 9rpx;
  padding: 8rpx 14rpx;
  border: 1rpx solid rgba(42, 154, 104, 0.16);
  border-radius: 20rpx;
  background: rgba(235, 249, 242, 0.88);
  color: #287654;
  font-size: 18rpx;
}

.radar-caption-dot {
  width: 9rpx;
  height: 9rpx;
  border-radius: 50%;
  background: #2A9A68;
}

.radar-caption-title {
  display: block;
  margin-top: 13rpx;
  color: #1D3C57;
  font-size: 35rpx;
  font-weight: 720;
}

.radar-caption-desc {
  display: block;
  margin-top: 6rpx;
  color: #72899D;
  font-size: 19rpx;
}

.rescue-radar {
  position: absolute;
  top: 390rpx;
  left: 50%;
  width: 560rpx;
  height: 560rpx;
  transform: translateX(-50%);
}

.radar-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  border: 1rpx solid rgba(46, 109, 209, 0.2);
  border-radius: 50%;
  transform: translate(-50%, -50%);
}

.radar-ring-one { width: 180rpx; height: 180rpx; }
.radar-ring-two { width: 360rpx; height: 360rpx; }
.radar-ring-three { width: 540rpx; height: 540rpx; }

.radar-cross {
  position: absolute;
  top: 50%;
  left: 50%;
  background: rgba(46, 109, 209, 0.11);
  transform: translate(-50%, -50%);
}

.radar-cross-horizontal { width: 540rpx; height: 1rpx; }
.radar-cross-vertical { width: 1rpx; height: 540rpx; }

.radar-sweep {
  position: absolute;
  inset: 10rpx;
  border-radius: 50%;
  background: conic-gradient(from 0deg, rgba(46, 109, 209, 0.2), transparent 20%, transparent 100%);
  animation: radarSweep 4s linear infinite;
}

@keyframes radarSweep {
  to { transform: rotate(360deg); }
}

.radar-self {
  position: absolute;
  top: 50%;
  left: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72rpx;
  height: 72rpx;
  transform: translate(-50%, -50%);
}

.radar-self-ring {
  position: absolute;
  inset: 0;
  border: 1rpx solid rgba(46, 109, 209, 0.28);
  border-radius: 50%;
  animation: selfPulse 2s ease-out infinite;
}

@keyframes selfPulse {
  from { transform: scale(0.7); opacity: 1; }
  to { transform: scale(1.5); opacity: 0; }
}

.radar-self-core,
.radar-node-core {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.radar-node-glyph {
  width: 25rpx;
  height: 25rpx;
}

.radar-self-core {
  width: 50rpx;
  height: 50rpx;
  background: #2E6DD1;
  box-shadow: 0 6rpx 18rpx rgba(46, 109, 209, 0.24);
}

.radar-node {
  position: absolute;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  transform: translate(-50%, -50%);
}

.radar-node-core {
  width: 42rpx;
  height: 42rpx;
  border: 4rpx solid rgba(255, 255, 255, 0.86);
  background: #2E6DD1;
  box-shadow: 0 5rpx 16rpx rgba(46, 109, 209, 0.2);
}

.radar-node-kit .radar-node-core { background: #168293; }
.radar-node-volunteer .radar-node-core { background: #2A8B61; }

.radar-node-label {
  padding: 4rpx 9rpx;
  border: 1rpx solid rgba(108, 137, 161, 0.13);
  border-radius: 8rpx;
  background: rgba(255, 255, 255, 0.88);
  color: #4D6780;
  font-size: 16rpx;
}

.device-popup {
  position: fixed;
  bottom: 460rpx;
  left: 24rpx;
  right: 24rpx;
  z-index: 200;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 8rpx 40rpx rgba(0,0,0,0.12);
  transform: translateY(100rpx);
  opacity: 0;
  transition: all 0.3s ease;
}
.device-popup-show {
  transform: translateY(0);
  opacity: 1;
}
.popup-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 20rpx;
}
.popup-type-tag {
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
}
.popup-type-fixed {
  background: rgba(43, 111, 240, 0.1);
}
.popup-type-mobile {
  background: rgba(0, 180, 42, 0.1);
}
.popup-type-text {
  font-size: 22rpx;
  font-weight: 600;
}
.popup-type-fixed .popup-type-text {
  color: #2E6DD1;
}
.popup-type-mobile .popup-type-text {
  color: #23956A;
}
.popup-category-tag {
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
  background: #F2F3F5;
}
.popup-category-text {
  font-size: 22rpx;
  color: #4E5969;
  font-weight: 500;
}
.popup-close {
  margin-left: auto;
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: #F2F3F5;
  display: flex;
  align-items: center;
  justify-content: center;
}
.popup-body {
  margin-bottom: 24rpx;
}
.popup-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #20364D;
  display: block;
  line-height: 1.4;
}
.popup-address {
  font-size: 24rpx;
  color: #86909C;
  margin-top: 8rpx;
  display: block;
  line-height: 1.4;
}
.popup-info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;
}
.popup-status {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.popup-status-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
}
.popup-status-online .popup-status-dot {
  background: #23956A;
  box-shadow: 0 0 8rpx rgba(0, 180, 42, 0.4);
}
.popup-status-offline .popup-status-dot {
  background: #C9CDD4;
}
.popup-status-text {
  font-size: 24rpx;
  color: #4E5969;
}
.popup-distance {
  font-size: 26rpx;
  color: #2E6DD1;
  font-weight: 600;
}
.popup-vehicle {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 12rpx;
  padding: 12rpx 16rpx;
  background: #F7F8FA;
  border-radius: 12rpx;
}
.popup-vehicle-label {
  font-size: 22rpx;
  color: #86909C;
  white-space: nowrap;
}
.popup-vehicle-text {
  font-size: 24rpx;
  color: #4E5969;
  font-weight: 500;
}
.popup-footer {
  display: flex;
  gap: 16rpx;
}
.popup-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  height: 76rpx;
  border-radius: 38rpx;
  transition: all 0.3s ease;
}
.popup-btn:active {
  transform: scale(0.97);
}
.popup-btn-call {
  background: #F2F3F5;
}
.popup-btn-nav {
  background: linear-gradient(135deg, #2E6DD1 0%, #2E6DD1 100%);
  box-shadow: 0 6rpx 24rpx rgba(43, 111, 240, 0.3);
}
.popup-btn-icon {
  font-size: 28rpx;
}
.popup-btn-text {
  font-size: 28rpx;
  font-weight: 600;
}
.popup-btn-call .popup-btn-text {
  color: #4E5969;
}
.popup-btn-nav .popup-btn-text {
  color: #FFFFFF;
}

.bottom-drawer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 150;
  height: 72vh;
  max-height: 1160rpx;
  overflow: hidden;
  background: #FFFFFF;
  border: 1rpx solid rgba(146, 169, 191, 0.22);
  border-bottom: 0;
  border-radius: 30rpx 30rpx 0 0;
  box-shadow: 0 -10rpx 36rpx rgba(34, 68, 101, 0.12);
  display: flex;
  flex-direction: column;
  transition: transform 280ms cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
}
.bottom-drawer-dragging {
  transition: none;
}
.drawer-drag-zone {
  flex: 0 0 auto;
  border-bottom: 1rpx solid var(--network-line);
  background: var(--network-paper);
  touch-action: none;
}
.drawer-handle {
  display: flex;
  justify-content: center;
  padding: 13rpx 0 7rpx;
}
.drawer-handle-bar {
  width: 58rpx;
  height: 7rpx;
  border-radius: 4rpx;
  background: #C9D5E0;
}
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 5rpx 28rpx 22rpx;
}
.drawer-heading {
  min-width: 0;
}
.drawer-title-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.drawer-title {
  color: var(--network-ink);
  font-size: 32rpx;
  font-weight: 720;
  letter-spacing: 1rpx;
}
.drawer-count {
  color: var(--network-action);
  font-size: 21rpx;
  font-weight: 600;
}
.drawer-summary {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 7rpx;
  color: var(--network-muted);
  font-size: 20rpx;
}
.drawer-live-dot {
  width: 10rpx;
  height: 10rpx;
  flex: 0 0 auto;
  border-radius: 50%;
  background: var(--network-online);
}
.drawer-toggle {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 4rpx;
  min-width: 76rpx;
  height: 50rpx;
  justify-content: center;
  color: var(--network-action);
  font-size: 21rpx;
  font-weight: 600;
}
.drawer-list {
  flex: 1;
  min-height: 0;
  height: 0;
  box-sizing: border-box;
  padding: 0 24rpx;
  background: #FFFFFF;
}

.device-card {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 148rpx;
  padding: 21rpx 8rpx 21rpx 17rpx;
  border-bottom: 1rpx solid var(--network-line);
  box-sizing: border-box;
}
.device-card:last-child {
  border-bottom: none;
}
/* 类型图标：让列表首列有可扫读的图形锚点，不再是整片文字 */
.device-card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 60rpx;
  width: 60rpx;
  height: 60rpx;
  margin-right: 16rpx;
  border-radius: 16rpx;
  background: rgba(46, 109, 209, 0.08);
}
.device-icon-offline {
  background: rgba(201, 205, 212, 0.22);
}
.device-icon-reserved {
  background: rgba(217, 123, 18, 0.1);
}
.device-card-glyph {
  width: 30rpx;
  height: 36rpx;
}
.device-card-center {
  flex: 1;
  min-width: 0;
}
.device-card-name-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.device-card-name {
  color: var(--network-ink);
  font-size: 27rpx;
  font-weight: 650;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.nearest-tag {
  flex: 0 0 auto;
  color: var(--network-action);
  font-size: 18rpx;
  font-weight: 650;
}
.device-card-meta {
  display: flex;
  align-items: center;
  margin-top: 8rpx;
}
.device-card-addr {
  font-size: 22rpx;
  color: var(--network-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.device-card-bottom {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 11rpx;
}
/* 状态做成带底色的胶囊，让列表在纯文字之外有可扫读的层次 */
.status-chip {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
}
.chip-online {
  background: rgba(35, 149, 106, 0.1);
}
.chip-offline {
  background: rgba(201, 205, 212, 0.24);
}
.chip-reserved {
  background: rgba(217, 123, 18, 0.12);
}
.card-status-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
}
.chip-online .card-status-dot {
  background: var(--network-online);
}
.chip-offline .card-status-dot {
  background: #C9CDD4;
}
.chip-reserved .card-status-dot {
  background: var(--network-warning);
}
.card-status-text {
  font-size: 21rpx;
  font-weight: 600;
}
.chip-online .card-status-text {
  color: #1C7A57;
}
.chip-offline .card-status-text {
  color: #7A8494;
}
.chip-reserved .card-status-text {
  color: #B4680C;
}
.device-card-kind {
  overflow: hidden;
  color: var(--network-muted);
  font-size: 20rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.device-card-distance {
  color: var(--network-ink);
  font-size: 24rpx;
  font-weight: 700;
}
.device-card-right {
  display: flex;
  flex: 0 0 auto;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  margin-left: 18rpx;
}
.nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rpx;
  color: var(--network-action);
  font-size: 21rpx;
  transition: opacity 150ms ease;
}
.nav-btn:active {
  opacity: 0.58;
}
.nav-btn-icon {
  font-size: 20rpx;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 0;
}
.empty-icon {
  font-size: 64rpx;
  margin-bottom: 16rpx;
}
.empty-text {
  font-size: 28rpx;
  color: #C9CDD4;
}

.locate-btn {
  position: fixed;
  right: 24rpx;
  bottom: 500rpx;
  z-index: 100;
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  background: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 24rpx rgba(0,0,0,0.12);
  transition: all 0.3s ease;
}
.locate-btn:active {
  transform: scale(0.92);
}
.locate-icon {
  font-size: 36rpx;
}

.drawer-bottom-safe {
  height: calc(36rpx + env(safe-area-inset-bottom));
}

</style>
