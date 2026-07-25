<template>
  <view class="map-page">
    <!-- 顶部搜索与筛选区域 -->
    <view class="top-overlay" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="search-bar">
        <view class="search-icon-wrap">
          <text class="search-icon">🔍</text>
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
          <text class="clear-icon">&#x2715;</text>
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

    <!-- 地图容器 -->
    <view id="map-container" class="map-container"></view>

    <!-- 设备详情弹窗 -->
    <view v-if="selectedDevice && !isNavigating" class="device-popup" :class="{ 'device-popup-show': popupVisible }">
      <view class="popup-header">
        <view class="popup-type-tag" :class="'popup-type-' + selectedDevice.type">
          <text class="popup-type-text">{{ selectedDevice.type === 'fixed' ? '固定' : '移动' }}</text>
        </view>
        <view class="popup-category-tag">
          <text class="popup-category-text">{{ selectedDevice.category }}</text>
        </view>
        <view class="popup-close" @tap="closePopup">
          <text class="popup-close-icon">&#x2715;</text>
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
          <text class="popup-distance">{{ selectedDevice._distance || '0.8km' }}</text>
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
          <text class="popup-btn-icon">📞</text>
          <text class="popup-btn-text">联系</text>
        </view>
        <view class="popup-btn popup-btn-nav" @tap="handleNavigate(selectedDevice)">
          <text class="popup-btn-icon">🧭</text>
          <text class="popup-btn-text">导航</text>
        </view>
      </view>
    </view>

    <!-- 底部设备列表抽屉 -->
    <view
      v-if="!isNavigating"
      class="bottom-drawer"
      :style="{ transform: 'translateY(' + drawerOffset + 'rpx)' }"
      @touchstart="onDrawerTouchStart"
      @touchmove="onDrawerTouchMove"
      @touchend="onDrawerTouchEnd"
    >
      <view class="drawer-handle">
        <view class="drawer-handle-bar"></view>
      </view>
      <view class="drawer-header">
        <text class="drawer-title">附近设备</text>
        <text class="drawer-count">{{ filteredDevices.length }}台</text>
      </view>
      <scroll-view class="drawer-list" scroll-y>
        <view
          v-for="device in filteredDevices"
          :key="device.id"
          class="device-card"
          @tap="selectDevice(device)"
        >
          <view class="device-card-left">
            <view class="device-icon-wrap" :class="'device-icon-' + device.type">
              <text class="device-icon-text">{{ device.category === 'AED' ? 'AED' : '急救' }}</text>
            </view>
          </view>
          <view class="device-card-center">
            <view class="device-card-name-row">
              <text class="device-card-name">{{ device.name }}</text>
              <view class="device-type-mini" :class="'type-mini-' + device.type">
                <text class="type-mini-text">{{ device.type === 'fixed' ? '固定' : '移动' }}</text>
              </view>
            </view>
            <view class="device-card-meta">
              <view class="device-card-category">
                <text class="category-text">{{ device.category }}</text>
              </view>
              <text class="device-card-dot">&#xB7;</text>
              <text class="device-card-addr">{{ device.address }}</text>
            </view>
            <view class="device-card-bottom">
              <view class="device-card-status" :class="'card-status-' + deviceStatusClass(device)">
                <view class="card-status-dot"></view>
                <text class="card-status-text">{{ deviceStatusLabel(device) }}</text>
              </view>
              <text class="device-card-distance">{{ device._distance || '0.8km' }}</text>
            </view>
          </view>
          <view class="device-card-right">
            <view class="nav-btn" @tap.stop="handleNavigate(device)">
              <text class="nav-btn-icon">🧭</text>
            </view>
          </view>
        </view>
        <view v-if="filteredDevices.length === 0" class="empty-state">
          <text class="empty-icon">🔍</text>
          <text class="empty-text">附近暂无设备</text>
        </view>
        <!-- 底部安全区 -->
        <view class="drawer-bottom-safe"></view>
      </scroll-view>
    </view>

    <!-- 导航面板 -->
    <view v-if="isNavigating" class="nav-panel" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-panel-header">
        <view class="nav-panel-back" @tap="cancelNavigation">
          <text class="nav-back-icon">&#x2190;</text>
        </view>
        <text class="nav-panel-title">导航中</text>
        <view class="nav-panel-placeholder"></view>
      </view>
      <view class="nav-route-info">
        <view class="nav-route-left">
          <text class="nav-route-dest">{{ navTargetName }}</text>
          <text class="nav-route-detail">距离 {{ navDistance }} · 预计 {{ navDuration }}</text>
        </view>
        <view class="nav-route-right" @tap="cancelNavigation">
          <text class="nav-cancel-text">结束导航</text>
        </view>
      </view>
    </view>

    <!-- 导航步骤面板 -->
    <view v-if="isNavigating && currentStep" class="nav-step-panel">
      <view class="nav-step-content">
        <text class="nav-step-action">{{ currentStep.action }}</text>
        <text class="nav-step-desc">{{ currentStep.instruction }}</text>
      </view>
      <view class="nav-step-distance">
        <text class="nav-step-dist-text">{{ currentStep.distance }}</text>
      </view>
    </view>

    <!-- 我的位置按钮 -->
    <view class="locate-btn" :class="{ 'locate-btn-nav': isNavigating }" @tap="locateMe">
      <text class="locate-icon">📍</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { mockFixedDevices, mockMobileDevices } from '@/mock/data'

// 系统信息
const statusBarHeight = ref(0)
const systemInfo = uni.getSystemInfoSync()
statusBarHeight.value = systemInfo.statusBarHeight || 20

// 搜索与筛选
const searchText = ref('')
const activeFilter = ref('all')
const filterTabs = [
  { key: 'all', label: '全部' },
  { key: 'fixed', label: '固定设备' },
  { key: 'mobile', label: '移动设备' }
]

// 地图实例
let mapInstance: any = null
let markerMap: Record<string, any> = {}
let drivingRoute: any = null // 路线规划实例
let routePolyline: any = null // 路线折线
let startMarker: any = null // 起点标记
let endMarker: any = null // 终点标记
let navTimer: any = null // 导航模拟定时器

// 导航状态
const isNavigating = ref(false)
const navTargetName = ref('')
const navDistance = ref('')
const navDuration = ref('')
const currentStep = ref<any>(null)
const navSteps = ref<any[]>([])
const currentStepIndex = ref(0)

// 模拟当前位置（杭州中心）
const myLocation = ref([120.15, 30.28])

// 设备数据 - 添加模拟距离
const allDevices = computed(() => {
  const fixed = mockFixedDevices.map(d => ({
    ...d,
    _distance: (Math.random() * 3 + 0.3).toFixed(1) + 'km'
  }))
  const mobile = mockMobileDevices.map(d => ({
    ...d,
    _distance: (Math.random() * 5 + 0.5).toFixed(1) + 'km'
  }))
  return [...fixed, ...mobile]
})

// 筛选后的设备
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

// 选中设备与弹窗
const selectedDevice = ref<any>(null)
const popupVisible = ref(false)

// 抽屉
const drawerExpanded = ref(false)
const drawerOffset = ref(420)
const touchStartY = ref(0)
const touchCurrentY = ref(0)

// 设备状态
function deviceStatusClass(device: any) {
  if (device.type === 'fixed') {
    return device.status === 'available' ? 'online' : 'offline'
  }
  return device.online ? 'online' : 'offline'
}

function deviceStatusLabel(device: any) {
  if (device.type === 'fixed') {
    return device.status === 'available' ? '可用' : '维护中'
  }
  return device.online ? '在线' : '离线'
}

// 加载高德地图
const loadAMap = () => {
  return new Promise((resolve) => {
    if ((window as any).AMap) { resolve(true); return }
    ;(window as any)._AMapSecurityConfig = { securityJsCode: '2b1374475410bd35525b1e1770ad69d1' }
    const script = document.createElement('script')
    script.src = 'https://webapi.amap.com/maps?v=2.0&key=d8ce027a3ead033d535a5a99bb81490f'
    script.onload = () => resolve(true)
    document.head.appendChild(script)
  })
}

// 初始化地图
async function initMap() {
  await loadAMap()
  const AMap = (window as any).AMap

  mapInstance = new AMap.Map('map-container', {
    zoom: 13,
    center: [120.15, 30.28],
    mapStyle: 'amap://styles/whitesmoke',
    resizeEnable: true
  })

  addMarkers()
}

// 添加设备标记
function addMarkers() {
  if (!mapInstance || !(window as any).AMap) return
  const AMap = (window as any).AMap

  // 清除旧标记
  Object.values(markerMap).forEach((m: any) => mapInstance.remove(m))
  markerMap = {}

  const devices = filteredDevices.value
  devices.forEach(device => {
    const statusCls = deviceStatusClass(device)
    let color = '#2B6FF0' // 固定-蓝色
    if (device.type === 'mobile') {
      color = statusCls === 'online' ? '#00B42A' : '#C9CDD4' // 移动-绿色/灰色
    } else {
      color = statusCls === 'online' ? '#2B6FF0' : '#C9CDD4' // 固定-蓝色/灰色
    }

    const markerContent = document.createElement('div')
    markerContent.innerHTML = `
      <div style="
        display: flex;
        flex-direction: column;
        align-items: center;
        cursor: pointer;
      ">
        <div style="
          width: 40px;
          height: 40px;
          border-radius: 50%;
          background: ${color};
          display: flex;
          align-items: center;
          justify-content: center;
          box-shadow: 0 2px 8px rgba(0,0,0,0.2);
          border: 2px solid #fff;
        ">
          <span style="color: #fff; font-size: 12px; font-weight: 700;">${device.category === 'AED' ? 'AED' : '急救'}</span>
        </div>
        <div style="
          width: 0;
          height: 0;
          border-left: 6px solid transparent;
          border-right: 6px solid transparent;
          border-top: 8px solid ${color};
          margin-top: -2px;
        "></div>
      </div>
    `

    const marker = new AMap.Marker({
      position: new AMap.LngLat(device.longitude, device.latitude),
      content: markerContent,
      offset: new AMap.Pixel(-20, -52),
      title: device.name
    })

    marker.on('click', () => {
      selectDevice(device)
    })

    mapInstance.add(marker)
    markerMap[device.id] = marker
  })
}

// 选中设备
function selectDevice(device: any) {
  selectedDevice.value = device
  popupVisible.value = true

  // 移动地图到设备位置
  if (mapInstance) {
    mapInstance.setCenter([device.longitude, device.latitude])
  }
}

// 关闭弹窗
function closePopup() {
  popupVisible.value = false
  setTimeout(() => {
    selectedDevice.value = null
  }, 300)
}

// 联系设备
function handleCallDevice() {
  if (selectedDevice.value) {
    uni.makePhoneCall({
      phoneNumber: selectedDevice.value.ownerPhone || '0571-120'
    })
  }
}

// 导航 - 使用高德地图路线规划
async function handleNavigate(device: any) {
  if (!mapInstance || !(window as any).AMap) return
  const AMap = (window as any).AMap

  // 关闭弹窗
  closePopup()

  // 清除之前的路线
  clearNavigation()

  uni.showLoading({ title: '正在规划路线...' })

  try {
    // 加载 Driving 插件
    await new Promise((resolve, reject) => {
      AMap.plugin(['AMap.Driving'], () => {
        resolve(true)
      })
      setTimeout(() => reject(new Error('插件加载超时')), 10000)
    })

    const origin = new AMap.LngLat(myLocation.value[0], myLocation.value[1])
    const destination = new AMap.LngLat(device.longitude, device.latitude)

    // 创建起点标记
    const startContent = document.createElement('div')
    startContent.innerHTML = `
      <div style="
        width: 32px; height: 32px; border-radius: 50%;
        background: #2B6FF0; display: flex; align-items: center; justify-content: center;
        box-shadow: 0 2px 8px rgba(43,111,240,0.4); border: 2px solid #fff;
      ">
        <span style="color: #fff; font-size: 14px; font-weight: 700;">起</span>
      </div>
    `
    startMarker = new AMap.Marker({
      position: origin,
      content: startContent,
      offset: new AMap.Pixel(-16, -16),
      zIndex: 120
    })
    mapInstance.add(startMarker)

    // 创建终点标记
    const endContent = document.createElement('div')
    endContent.innerHTML = `
      <div style="
        width: 32px; height: 32px; border-radius: 50%;
        background: #F53F3F; display: flex; align-items: center; justify-content: center;
        box-shadow: 0 2px 8px rgba(245,63,63,0.4); border: 2px solid #fff;
      ">
        <span style="color: #fff; font-size: 14px; font-weight: 700;">终</span>
      </div>
    `
    endMarker = new AMap.Marker({
      position: destination,
      content: endContent,
      offset: new AMap.Pixel(-16, -16),
      zIndex: 120
    })
    mapInstance.add(endMarker)

    // 路线规划
    drivingRoute = new AMap.Driving({
      map: mapInstance,
      policy: AMap.DrivingPolicy?.LEAST_TIME || 0,
      hideMarkers: true, // 隐藏默认标记，使用自定义标记
      showTraffic: true
    })

    drivingRoute.search(origin, destination, (status: string, result: any) => {
      uni.hideLoading()

      if (status === 'complete' && result.routes && result.routes.length > 0) {
        const route = result.routes[0]
        const distanceMeters = route.distance || 0
        const durationSeconds = route.time || 0

        // 格式化距离和时间
        navDistance.value = distanceMeters >= 1000
          ? (distanceMeters / 1000).toFixed(1) + '公里'
          : distanceMeters + '米'
        navDuration.value = durationSeconds >= 3600
          ? Math.floor(durationSeconds / 3600) + '小时' + Math.floor((durationSeconds % 3600) / 60) + '分钟'
          : Math.ceil(durationSeconds / 60) + '分钟'
        navTargetName.value = device.name

        // 解析导航步骤
        const steps: any[] = []
        if (route.steps && route.steps.length > 0) {
          route.steps.forEach((step: any, idx: number) => {
            const stepDist = step.distance || 0
            let action = '直行'
            if (idx === 0) action = '出发'
            else if (step.action === '左转') action = '左转'
            else if (step.action === '右转') action = '右转'
            else if (step.action === '调头') action = '调头'
            else if (step.action === '左前方向') action = '左前方行驶'
            else if (step.action === '右前方向') action = '右前方行驶'
            else if (step.action === '左后方向') action = '左后方行驶'
            else if (step.action === '右后方向') action = '右后方行驶'
            else if (step.action === '到达终点' || idx === route.steps.length - 1) action = '到达目的地'

            steps.push({
              action,
              instruction: step.instruction || step.action || '继续行驶',
              distance: stepDist >= 1000 ? (stepDist / 1000).toFixed(1) + 'km' : stepDist + 'm',
              road: step.road || '',
              lnglat: step.start_location ? [step.start_location.lng, step.start_location.lat] : null
            })
          })
        }
        navSteps.value = steps
        currentStepIndex.value = 0
        if (steps.length > 0) {
          currentStep.value = steps[0]
        }

        // 进入导航模式
        isNavigating.value = true

        // 调整视野包含整条路线
        if (route.bounds) {
          mapInstance.setBounds(route.bounds, false, [80, 80, 80, 280])
        }

        // 启动导航模拟
        startNavSimulation(steps)

        uni.showToast({
          title: '路线规划成功',
          icon: 'success'
        })
      } else {
        uni.showToast({
          title: '路线规划失败，请重试',
          icon: 'none'
        })
        clearNavigation()
      }
    })
  } catch (e) {
    uni.hideLoading()
    uni.showToast({
      title: '导航插件加载失败',
      icon: 'none'
    })
  }
}

// 导航模拟 - 逐步推进
function startNavSimulation(steps: any[]) {
  if (navTimer) clearInterval(navTimer)

  let stepIdx = 0
  navTimer = setInterval(() => {
    stepIdx++
    if (stepIdx >= steps.length) {
      // 到达目的地
      clearInterval(navTimer)
      navTimer = null
      currentStep.value = {
        action: '到达目的地',
        instruction: '您已到达目的地附近',
        distance: '0m'
      }
      uni.showToast({
        title: '已到达目的地附近',
        icon: 'success',
        duration: 3000
      })
      setTimeout(() => {
        cancelNavigation()
      }, 3000)
      return
    }

    currentStepIndex.value = stepIdx
    currentStep.value = steps[stepIdx]

    // 移动地图中心到当前步骤位置
    if (steps[stepIdx].lnglat && mapInstance) {
      mapInstance.setCenter(steps[stepIdx].lnglat)
    }
  }, 5000)
}

// 取消导航
function cancelNavigation() {
  clearNavigation()
  isNavigating.value = false
  navTargetName.value = ''
  navDistance.value = ''
  navDuration.value = ''
  currentStep.value = null
  navSteps.value = []
  currentStepIndex.value = 0

  // 恢复地图视角
  if (mapInstance) {
    mapInstance.setCenter([120.15, 30.28])
    mapInstance.setZoom(13)
  }
}

// 清除导航路线和标记
function clearNavigation() {
  if (navTimer) {
    clearInterval(navTimer)
    navTimer = null
  }
  if (drivingRoute) {
    drivingRoute.clear()
    drivingRoute = null
  }
  if (routePolyline) {
    mapInstance && mapInstance.remove(routePolyline)
    routePolyline = null
  }
  if (startMarker) {
    mapInstance && mapInstance.remove(startMarker)
    startMarker = null
  }
  if (endMarker) {
    mapInstance && mapInstance.remove(endMarker)
    endMarker = null
  }
}

// 搜索
function handleSearch() {
  addMarkers()
}

// 定位
function locateMe() {
  if (mapInstance) {
    mapInstance.setCenter([120.15, 30.28])
    mapInstance.setZoom(14)
  }
  uni.showToast({
    title: '已定位到当前位置',
    icon: 'none'
  })
}

// 抽屉拖拽
function onDrawerTouchStart(e: any) {
  touchStartY.value = e.touches[0].clientY
  touchCurrentY.value = e.touches[0].clientY
}

function onDrawerTouchMove(e: any) {
  touchCurrentY.value = e.touches[0].clientY
  const diff = touchCurrentY.value - touchStartY.value
  const base = drawerExpanded.value ? 0 : 420
  let offset = base + diff
  if (offset < 0) offset = 0
  if (offset > 420) offset = 420
  drawerOffset.value = offset
}

function onDrawerTouchEnd() {
  const diff = touchCurrentY.value - touchStartY.value
  if (drawerExpanded.value) {
    // 已展开，向上滑收起
    if (diff > 60) {
      drawerExpanded.value = false
      drawerOffset.value = 420
    } else {
      drawerOffset.value = 0
    }
  } else {
    // 已收起，向下滑展开
    if (diff < -60) {
      drawerExpanded.value = true
      drawerOffset.value = 0
    } else {
      drawerOffset.value = 420
    }
  }
}

onMounted(() => {
  nextTick(() => {
    initMap()
  })
})

onUnmounted(() => {
  clearNavigation()
})
</script>

<style lang="scss" scoped>
.map-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

/* 顶部搜索与筛选 */
.top-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: linear-gradient(180deg, rgba(255,255,255,0.98) 0%, rgba(255,255,255,0.95) 80%, rgba(255,255,255,0) 100%);
  padding-bottom: 16rpx;
}
.search-bar {
  display: flex;
  align-items: center;
  margin: 0 24rpx;
  height: 80rpx;
  background: #F2F3F5;
  border-radius: 40rpx;
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
  color: #1D2129;
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
.clear-icon {
  font-size: 20rpx;
  color: #FFFFFF;
}
.filter-tabs {
  display: flex;
  padding: 16rpx 24rpx 0;
  gap: 8rpx;
}
.filter-tab {
  position: relative;
  padding: 12rpx 32rpx;
  border-radius: 32rpx;
  background: rgba(255,255,255,0.8);
  transition: all 0.3s ease;
}
.filter-tab-active {
  background: #2B6FF0;
}
.filter-tab-text {
  font-size: 26rpx;
  color: #4E5969;
  font-weight: 500;
}
.filter-tab-active .filter-tab-text {
  color: #FFFFFF;
  font-weight: 600;
}
.filter-tab-indicator {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 32rpx;
  height: 4rpx;
  border-radius: 2rpx;
  background: #FFFFFF;
}

/* 地图容器 */
.map-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

/* 设备详情弹窗 */
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
  color: #2B6FF0;
}
.popup-type-mobile .popup-type-text {
  color: #00B42A;
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
.popup-close-icon {
  font-size: 22rpx;
  color: #86909C;
}
.popup-body {
  margin-bottom: 24rpx;
}
.popup-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #1D2129;
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
  background: #00B42A;
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
  color: #2B6FF0;
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
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
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

/* 底部抽屉 */
.bottom-drawer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 150;
  background: #FFFFFF;
  border-radius: 32rpx 32rpx 0 0;
  box-shadow: 0 -4rpx 32rpx rgba(0,0,0,0.08);
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease;
}
.drawer-handle {
  display: flex;
  justify-content: center;
  padding: 16rpx 0 8rpx;
}
.drawer-handle-bar {
  width: 64rpx;
  height: 8rpx;
  border-radius: 4rpx;
  background: #E5E6EB;
}
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8rpx 32rpx 20rpx;
}
.drawer-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1D2129;
}
.drawer-count {
  font-size: 24rpx;
  color: #2B6FF0;
  font-weight: 600;
  background: rgba(43, 111, 240, 0.08);
  padding: 4rpx 16rpx;
  border-radius: 16rpx;
}
.drawer-list {
  flex: 1;
  padding: 0 24rpx;
  max-height: 55vh;
}

/* 设备卡片 */
.device-card {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #F2F3F5;
}
.device-card:last-child {
  border-bottom: none;
}
.device-card-left {
  margin-right: 20rpx;
}
.device-icon-wrap {
  width: 80rpx;
  height: 80rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.device-icon-fixed {
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
}
.device-icon-mobile {
  background: linear-gradient(135deg, #00B42A 0%, #4DC580 100%);
}
.device-icon-text {
  font-size: 24rpx;
  font-weight: 700;
  color: #FFFFFF;
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
  font-size: 28rpx;
  font-weight: 600;
  color: #1D2129;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.device-type-mini {
  padding: 2rpx 12rpx;
  border-radius: 6rpx;
  flex-shrink: 0;
}
.type-mini-fixed {
  background: rgba(43, 111, 240, 0.1);
}
.type-mini-mobile {
  background: rgba(0, 180, 42, 0.1);
}
.type-mini-text {
  font-size: 20rpx;
  font-weight: 600;
}
.type-mini-fixed .type-mini-text {
  color: #2B6FF0;
}
.type-mini-mobile .type-mini-text {
  color: #00B42A;
}
.device-card-meta {
  display: flex;
  align-items: center;
  margin-top: 8rpx;
}
.device-card-category {
  padding: 2rpx 10rpx;
  background: #F2F3F5;
  border-radius: 6rpx;
}
.category-text {
  font-size: 20rpx;
  color: #4E5969;
}
.device-card-dot {
  font-size: 20rpx;
  color: #C9CDD4;
  margin: 0 8rpx;
}
.device-card-addr {
  font-size: 22rpx;
  color: #86909C;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.device-card-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10rpx;
}
.device-card-status {
  display: flex;
  align-items: center;
  gap: 6rpx;
}
.card-status-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
}
.card-status-online .card-status-dot {
  background: #00B42A;
}
.card-status-offline .card-status-dot {
  background: #C9CDD4;
}
.card-status-text {
  font-size: 22rpx;
  color: #4E5969;
}
.device-card-distance {
  font-size: 24rpx;
  color: #2B6FF0;
  font-weight: 600;
}
.device-card-right {
  margin-left: 16rpx;
}
.nav-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(43, 111, 240, 0.3);
  transition: all 0.3s ease;
}
.nav-btn:active {
  transform: scale(0.92);
}
.nav-btn-icon {
  font-size: 32rpx;
}

/* 空状态 */
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

/* 定位按钮 */
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

/* 底部安全区 */
.drawer-bottom-safe {
  height: 40rpx;
}

/* 导航面板 */
.nav-panel {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 300;
  background: linear-gradient(135deg, #2B6FF0 0%, #4A8BFF 100%);
  padding-bottom: 16rpx;
}
.nav-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 88rpx;
  padding: 0 24rpx;
}
.nav-panel-back {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.nav-back-icon {
  font-size: 40rpx;
  color: #FFFFFF;
  font-weight: 600;
}
.nav-panel-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #FFFFFF;
}
.nav-panel-placeholder {
  width: 64rpx;
}
.nav-route-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 24rpx;
  padding: 20rpx 24rpx;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 16rpx;
}
.nav-route-left {
  flex: 1;
}
.nav-route-dest {
  font-size: 30rpx;
  font-weight: 700;
  color: #FFFFFF;
  display: block;
}
.nav-route-detail {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 6rpx;
  display: block;
}
.nav-route-right {
  padding: 12rpx 28rpx;
  background: rgba(255, 255, 255, 0.25);
  border-radius: 28rpx;
}
.nav-cancel-text {
  font-size: 26rpx;
  color: #FFFFFF;
  font-weight: 600;
}

/* 导航步骤面板 */
.nav-step-panel {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 300;
  background: #FFFFFF;
  border-radius: 32rpx 32rpx 0 0;
  padding: 28rpx 32rpx 40rpx;
  box-shadow: 0 -4rpx 32rpx rgba(0, 0, 0, 0.1);
}
.nav-step-content {
  display: flex;
  flex-direction: column;
}
.nav-step-action {
  font-size: 36rpx;
  font-weight: 800;
  color: #2B6FF0;
  margin-bottom: 8rpx;
}
.nav-step-desc {
  font-size: 26rpx;
  color: #4E5969;
  line-height: 1.5;
}
.nav-step-distance {
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #F2F3F5;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.nav-step-dist-text {
  font-size: 28rpx;
  color: #86909C;
  font-weight: 500;
}

/* 导航模式下的定位按钮位置调整 */
.locate-btn-nav {
  bottom: 280rpx !important;
}
</style>
