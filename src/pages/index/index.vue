<template>
  <view class="home-page">
    <scroll-view class="home-scroll" scroll-y>
      <view class="hero" :style="{ paddingTop: statusBarHeight + 'px' }">
        <image
          class="hero-art"
          src="/static/illustrations/rescue-network-hero-light-v1.webp"
          mode="aspectFill"
        />
        <view class="hero-wash"></view>

        <view class="hero-nav">
          <view class="brand-lockup">
            <view class="brand-pulse">
              <view class="pulse-stem pulse-stem-short"></view>
              <view class="pulse-stem pulse-stem-tall"></view>
              <view class="pulse-stem pulse-stem-short"></view>
            </view>
            <view class="brand-copy">
              <text class="brand-name">脉安驰援</text>
              <text class="brand-caption">MAIAN RESPONSE</text>
            </view>
          </view>
          <view class="nav-alert" @tap="openNotifications">
            <app-icon name="notification-filled" :size="20" color="#24415F" />
          </view>
        </view>

        <view class="hero-copy">
          <view class="network-state">
            <view class="network-state-dot"></view>
            <text>{{ devicesLoaded ? '城市急救网络在线' : '正在同步急救网络' }}</text>
          </view>
          <text class="hero-title">让每一次求救，</text>
          <text class="hero-title hero-title-accent">更快被看见</text>
          <text class="hero-desc">连接附近 AED、急救包与志愿者，为关键几分钟争取更多可能。</text>
        </view>

        <view class="hero-location">
          <app-icon name="location-filled" :size="15" color="#2E6DD1" />
          <text class="hero-location-main">{{ locationLabel }}</text>
          <text class="hero-location-sub">{{ locationHint }}</text>
        </view>

        <view class="network-console">
          <view class="console-stat">
            <text class="console-value">{{ availableDeviceCount }}</text>
            <text class="console-label">可用设备</text>
          </view>
          <view class="console-divider"></view>
          <view class="console-stat">
            <text class="console-value">{{ fixedDeviceCount }}</text>
            <text class="console-label">固定设备</text>
          </view>
          <view class="console-divider"></view>
          <view class="console-stat">
            <text class="console-value">{{ mobileDeviceCount }}</text>
            <text class="console-label">移动设备</text>
          </view>
        </view>
      </view>

      <view class="action-deck">
        <view class="sos-action" @tap="openSOS">
          <view class="sos-signal">
            <view class="sos-signal-ring"></view>
            <text class="sos-code">SOS</text>
          </view>
          <view class="sos-copy">
            <text class="sos-title">紧急呼救</text>
            <text class="sos-desc">发送位置并匹配救援资源</text>
          </view>
          <app-icon name="right" :size="22" color="#FFFFFF" />
        </view>

        <view class="quick-grid">
          <view class="quick-action" @tap="openMap">
            <view class="quick-icon quick-icon-blue">
              <app-icon name="map-filled" :size="24" color="#2467C8" />
            </view>
            <view class="quick-copy">
              <text class="quick-title">急救地图</text>
              <text class="quick-desc">{{ nearestResourceText }}</text>
            </view>
          </view>
          <view class="quick-action" @tap="openDevice">
            <view class="quick-icon quick-icon-cyan">
              <app-icon name="plus-filled" :size="24" color="#177E8D" />
            </view>
            <view class="quick-copy">
              <text class="quick-title">共享设备</text>
              <text class="quick-desc">加入城市救援网</text>
            </view>
          </view>
        </view>
      </view>

      <view class="section network-section">
        <view class="section-heading">
          <view>
            <text class="section-kicker">LIVE NETWORK</text>
            <text class="section-title">身边的生命网络</text>
          </view>
          <view class="section-link" @tap="openMap">
            <text>查看地图</text>
            <app-icon name="right" :size="15" color="#2E6DD1" />
          </view>
        </view>

        <view class="signal-band">
          <view class="signal-band-top">
            <view>
              <text class="signal-title">急救网络脉冲</text>
              <text class="signal-subtitle">设备状态由服务端实时同步</text>
            </view>
            <view class="signal-live">
              <view class="signal-live-dot"></view>
              <text>实时</text>
            </view>
          </view>
          <view class="waveform" aria-hidden="true">
            <view
              v-for="(height, index) in waveform"
              :key="index"
              class="wave-bar"
              :class="{ 'wave-bar-hot': index >= 9 && index <= 13 }"
              :style="{ height: height + 'rpx' }"
            ></view>
            <view class="wave-sweep"></view>
          </view>
          <view class="signal-legend">
            <text>固定设备 {{ fixedDeviceCount }}</text>
            <text>移动设备 {{ mobileDeviceCount }}</text>
            <text>当前可用 {{ availableDeviceCount }}</text>
          </view>
        </view>

        <scroll-view class="resource-scroll" scroll-x show-scrollbar="false">
          <view class="resource-track">
            <view
              v-for="resource in nearbyResources"
              :key="resource.name"
              class="resource-item"
              @tap="openMap"
            >
              <view class="resource-top">
                <view class="resource-icon">
                  <app-icon :name="resource.icon" :size="22" color="#2E6DD1" />
                </view>
                <view class="resource-status">
                  <view class="resource-status-dot"></view>
                  <text>可用</text>
                </view>
              </view>
              <text class="resource-distance">{{ resource.distance }}</text>
              <text class="resource-name">{{ resource.name }}</text>
              <text class="resource-place">{{ resource.place }}</text>
            </view>
          </view>
        </scroll-view>
      </view>

      <view class="section health-section">
        <view class="section-heading">
          <view>
            <text class="section-kicker">HEALTH SIGNAL</text>
            <text class="section-title">今日健康信号</text>
          </view>
          <view class="section-link" @tap="openHealth">
            <text>完整数据</text>
            <app-icon name="right" :size="15" color="#2E6DD1" />
          </view>
        </view>

        <view class="health-panel" @tap="openHealth">
          <view class="heart-orbit">
            <view class="orbit orbit-one"></view>
            <view class="orbit orbit-two"></view>
            <view class="heart-core">
              <app-icon name="heart-filled" :size="27" color="#B52832" />
            </view>
          </view>
          <view class="heart-reading">
            <view class="heart-number-line">
              <text class="heart-number">{{ heartValue }}</text>
              <text class="heart-unit">BPM</text>
            </view>
            <text class="heart-state">{{ heartState }}</text>
            <text class="heart-time">{{ heartSource }}</text>
          </view>
          <view class="health-bars">
            <view
              v-for="(height, index) in healthBars"
              :key="index"
              class="health-bar"
              :style="{ height: height + 'rpx' }"
            ></view>
          </view>
        </view>

        <view class="health-metrics">
          <view class="metric">
            <text class="metric-label">最低心率</text>
            <text class="metric-value">{{ metricValue(healthMonitoring.min) }}</text>
            <text class="metric-state">BPM</text>
          </view>
          <view class="metric">
            <text class="metric-label">平均心率</text>
            <text class="metric-value">{{ metricValue(healthMonitoring.avg) }}</text>
            <text class="metric-state">BPM</text>
          </view>
          <view class="metric">
            <text class="metric-label">最高心率</text>
            <text class="metric-value">{{ metricValue(healthMonitoring.max) }}</text>
            <text class="metric-state">BPM</text>
          </view>
        </view>
      </view>

      <view class="section service-section">
        <view class="section-heading">
          <view>
            <text class="section-kicker">CARE SERVICES</text>
            <text class="section-title">把健康主动权握在手里</text>
          </view>
        </view>
        <view class="service-layout">
          <view class="service-primary" @tap="openCheckup">
            <view class="service-icon">
              <app-icon name="folder-add-filled" :size="26" color="#FFFFFF" />
            </view>
            <text class="service-primary-kicker">AI HEALTH REPORT</text>
            <text class="service-primary-title">上传体检报告</text>
            <text class="service-primary-desc">自动整理指标并给出结构化健康建议</text>
            <view class="service-arrow">
              <app-icon name="right" :size="18" color="#2E6DD1" />
            </view>
          </view>
          <view class="service-secondary" @tap="openScience">
            <view class="science-ring">
              <app-icon name="videocam-filled" :size="22" color="#177E8D" />
            </view>
            <text class="service-secondary-title">急救课堂</text>
            <text class="service-secondary-desc">系统学习关键急救动作</text>
            <text class="lesson-meta">已收录 {{ scienceArticles.length }} 篇科普内容</text>
          </view>
        </view>
      </view>

      <view class="safety-note">
        <view class="safety-line"></view>
        <text>如遇危及生命的紧急情况，请立即拨打 120</text>
      </view>
      <view class="bottom-space"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIcon from '@/components/AppIcon.vue'
import { useHealthMonitoring } from '@/composables/useHealthMonitoring'
import { scienceArticles } from '@/data/editorial'
import { listEmergencyDevices, type EmergencyDeviceResponse } from '@/api/devices'

const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)
const waveform = [12, 18, 16, 24, 20, 30, 22, 18, 28, 42, 16, 64, 20, 36, 24, 18, 22, 30, 20, 16, 24, 18, 14]
const { monitoring: healthMonitoring, loadMonitoring } = useHealthMonitoring()

const nearbyResources = ref<{ icon: string; distance: string; name: string; place: string }[]>([])
const deviceSnapshot = ref<EmergencyDeviceResponse[]>([])
const currentCoordinates = ref<{ longitude: number; latitude: number } | null>(null)
const devicesLoaded = ref(false)

const availableDeviceCount = computed(() => (
  deviceSnapshot.value.filter(device => device.status === 'AVAILABLE').length
))
const fixedDeviceCount = computed(() => (
  deviceSnapshot.value.filter(device => device.type === 'FIXED').length
))
const mobileDeviceCount = computed(() => (
  deviceSnapshot.value.filter(device => device.type === 'MOBILE').length
))
const locationLabel = computed(() => (
  currentCoordinates.value ? '当前位置已定位' : '杭州城区急救网络'
))
const locationHint = computed(() => (
  currentCoordinates.value ? '距离已按当前位置计算' : '授权定位后显示距离'
))
const nearestResourceText = computed(() => {
  const nearest = nearbyResources.value[0]
  return nearest && nearest.distance !== '--'
    ? `最近资源 ${nearest.distance}`
    : '查看附近急救资源'
})

const heartValue = computed(() => (
  healthMonitoring.value.current > 0 ? healthMonitoring.value.current : '--'
))
const heartState = computed(() => {
  if (!healthMonitoring.value.current) return '暂无心率记录'
  if (healthMonitoring.value.status === 'warning') return '最近心率偏高'
  if (healthMonitoring.value.status === 'danger') return '最近心率偏低'
  return '最近心率在正常范围'
})
const heartSource = computed(() => (
  healthMonitoring.value.wearable.connected
    ? `${healthMonitoring.value.wearable.name} 已连接`
    : '示例趋势 · 连接设备后同步'
))
const healthBars = computed(() => {
  const values = healthMonitoring.value.todayData
    .filter((_, index) => index % 4 === 0)
    .map(point => point.value)
  if (!values.length) return []
  const min = Math.min(...values)
  const range = Math.max(...values) - min || 1
  return values.map(value => Math.round(18 + ((value - min) / range) * 38))
})

function metricValue(value: number) {
  return value > 0 ? value : '--'
}

function distanceKm(device: EmergencyDeviceResponse, longitude: number, latitude: number) {
  const radians = (value: number) => value * Math.PI / 180
  const earthRadius = 6371
  const latDelta = radians(device.latitude - latitude)
  const lngDelta = radians(device.longitude - longitude)
  const a = Math.sin(latDelta / 2) ** 2
    + Math.cos(radians(latitude)) * Math.cos(radians(device.latitude)) * Math.sin(lngDelta / 2) ** 2
  return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

function currentLocation() {
  return new Promise<{ longitude: number; latitude: number } | null>((resolve) => {
    uni.getLocation({
      type: 'gcj02',
      success: ({ longitude, latitude }) => resolve({ longitude, latitude }),
      fail: () => resolve(null)
    })
  })
}

async function loadNearbyResources() {
  const [devicesResult, locationResult] = await Promise.allSettled([
    listEmergencyDevices(),
    currentLocation(),
    loadMonitoring()
  ])
  if (devicesResult.status === 'rejected') {
    devicesLoaded.value = false
    deviceSnapshot.value = []
    nearbyResources.value = []
    return
  }
  devicesLoaded.value = true
  deviceSnapshot.value = devicesResult.value.content
  const location = locationResult.status === 'fulfilled' ? locationResult.value : null
  currentCoordinates.value = location
  const available = devicesResult.value.content.filter(device => device.status === 'AVAILABLE')
  const ranked = location
    ? available
        .map(device => ({ device, distance: distanceKm(device, location.longitude, location.latitude) }))
        .sort((left, right) => left.distance - right.distance)
    : available.map(device => ({ device, distance: null }))
  nearbyResources.value = ranked
      .slice(0, 3)
      .map(({ device, distance }) => ({
        icon: device.type === 'MOBILE' ? 'staff-filled' : (device.category === 'AED' ? 'heart-filled' : 'plus-filled'),
        distance: distance === null
          ? '--'
          : distance < 1 ? `${Math.round(distance * 1000)} m` : `${distance.toFixed(1)} km`,
        name: device.name,
        place: device.address
      }))
}

function openSOS() {
  uni.navigateTo({ url: '/pages/rescue/index' })
}

function openMap() {
  uni.switchTab({ url: '/pages/map/index' })
}

function openHealth() {
  uni.switchTab({ url: '/pages/health/index' })
}

function openDevice() {
  uni.navigateTo({ url: '/pages/device/add' })
}

function openCheckup() {
  uni.navigateTo({ url: '/pages/checkup/upload' })
}

function openScience() {
  uni.navigateTo({ url: '/pages/science/index' })
}

function openNotifications() {
  uni.showToast({ title: '暂无新的紧急通知', icon: 'none' })
}

onShow(loadNearbyResources)
</script>

<style lang="scss" scoped>
.home-page {
  min-height: 100vh;
  background: #F3F7FC;
  color: #16283C;
}

.home-scroll {
  height: 100vh;
}

.hero {
  position: relative;
  min-height: 760rpx;
  overflow: hidden;
  background: #ECF5FC;
}

.hero-art {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.hero-wash {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    rgba(248, 252, 255, 0.98) 0%,
    rgba(248, 252, 255, 0.84) 34%,
    rgba(238, 247, 253, 0.22) 64%,
    rgba(238, 247, 253, 0.7) 100%
  );
}

.hero-nav,
.hero-copy,
.hero-location,
.network-console {
  position: relative;
  z-index: 2;
}

.hero-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 96rpx;
  padding: 0 32rpx;
}

.brand-lockup {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.brand-pulse {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4rpx;
  width: 54rpx;
  height: 54rpx;
  border: 1rpx solid rgba(46, 109, 209, 0.2);
  border-radius: 14rpx;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(12px);
}

.pulse-stem {
  width: 5rpx;
  border-radius: 4rpx;
  background: #2E6DD1;
}

.pulse-stem-short {
  height: 16rpx;
}

.pulse-stem-tall {
  height: 32rpx;
  background: #B52832;
}

.brand-copy {
  display: flex;
  flex-direction: column;
}

.brand-name {
  color: #18334F;
  font-size: 29rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
}

.brand-caption {
  margin-top: 2rpx;
  color: #6A8198;
  font-size: 15rpx;
  font-weight: 600;
  letter-spacing: 2rpx;
}

.nav-alert {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 68rpx;
  height: 68rpx;
  border: 1rpx solid rgba(36, 65, 95, 0.12);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.84);
  backdrop-filter: blur(12px);
}

.hero-copy {
  width: 69%;
  padding: 54rpx 32rpx 0;
}

.network-state {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  padding: 9rpx 16rpx;
  border: 1rpx solid rgba(34, 132, 90, 0.18);
  border-radius: 24rpx;
  background: rgba(237, 250, 244, 0.88);
  color: #237653;
  font-size: 20rpx;
  font-weight: 600;
}

.network-state-dot,
.signal-live-dot,
.resource-status-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #2A9A68;
  box-shadow: 0 0 0 6rpx rgba(42, 154, 104, 0.1);
}

.hero-title {
  display: block;
  margin-top: 24rpx;
  color: #152D46;
  font-size: 53rpx;
  font-weight: 750;
  line-height: 1.12;
  letter-spacing: -2rpx;
}

.hero-title-accent {
  margin-top: 3rpx;
  color: #2E6DD1;
}

.hero-desc {
  display: block;
  margin-top: 22rpx;
  color: #536C84;
  font-size: 24rpx;
  line-height: 1.65;
}

.hero-location {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  margin: 34rpx 32rpx 0;
  padding: 12rpx 17rpx;
  border: 1rpx solid rgba(46, 109, 209, 0.14);
  border-radius: 12rpx;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(12px);
}

.hero-location-main {
  color: #27435E;
  font-size: 22rpx;
  font-weight: 600;
}

.hero-location-sub {
  padding-left: 10rpx;
  border-left: 1rpx solid #CBD9E7;
  color: #71879C;
  font-size: 19rpx;
}

.network-console {
  position: absolute;
  right: 24rpx;
  bottom: 24rpx;
  left: 24rpx;
  display: flex;
  align-items: center;
  padding: 22rpx 10rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.78);
  border-radius: 18rpx;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 10rpx 28rpx rgba(45, 92, 132, 0.1);
  backdrop-filter: blur(18px);
}

.console-stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.console-value {
  color: #18334F;
  font-size: 35rpx;
  font-weight: 720;
  font-variant-numeric: tabular-nums;
}

.console-label {
  margin-top: 5rpx;
  color: #71879C;
  font-size: 18rpx;
}

.console-divider {
  width: 1rpx;
  height: 48rpx;
  background: #D8E3ED;
}

.action-deck {
  padding: 28rpx 24rpx 0;
}

.sos-action {
  display: flex;
  align-items: center;
  min-height: 118rpx;
  padding: 20rpx 24rpx;
  border-radius: 20rpx;
  background: #B52832;
  box-shadow: 0 10rpx 24rpx rgba(181, 40, 50, 0.18);
}

.sos-signal {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 74rpx;
  height: 74rpx;
  margin-right: 22rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.13);
}

.sos-signal-ring {
  position: absolute;
  inset: -8rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.22);
  border-radius: 50%;
}

.sos-code {
  color: #FFFFFF;
  font-size: 25rpx;
  font-weight: 800;
  letter-spacing: 2rpx;
}

.sos-copy {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.sos-title {
  color: #FFFFFF;
  font-size: 31rpx;
  font-weight: 700;
}

.sos-desc {
  margin-top: 7rpx;
  color: rgba(255, 255, 255, 0.76);
  font-size: 21rpx;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
  margin-top: 16rpx;
}

.quick-action {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 22rpx 20rpx;
  border: 1rpx solid #DCE7F0;
  border-radius: 18rpx;
  background: #FFFFFF;
}

.quick-icon,
.resource-icon,
.service-icon,
.science-ring {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
}

.quick-icon {
  width: 62rpx;
  height: 62rpx;
  margin-right: 14rpx;
  border-radius: 15rpx;
}

.quick-icon-blue {
  background: #EAF2FD;
}

.quick-icon-cyan {
  background: #E6F5F5;
}

.quick-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.quick-title {
  color: #213951;
  font-size: 25rpx;
  font-weight: 650;
}

.quick-desc {
  margin-top: 5rpx;
  overflow: hidden;
  color: #7A8FA3;
  font-size: 18rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section {
  padding: 56rpx 24rpx 0;
}

.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.section-kicker {
  display: block;
  color: #6D8DA9;
  font-size: 17rpx;
  font-weight: 700;
  letter-spacing: 3rpx;
}

.section-title {
  display: block;
  margin-top: 7rpx;
  color: #1B334B;
  font-size: 34rpx;
  font-weight: 720;
  letter-spacing: -1rpx;
}

.section-link {
  display: flex;
  align-items: center;
  gap: 4rpx;
  color: #2E6DD1;
  font-size: 21rpx;
}

.signal-band {
  position: relative;
  overflow: hidden;
  padding: 26rpx;
  border: 1rpx solid #D7E4EF;
  border-radius: 20rpx;
  background: #F9FCFF;
}

.signal-band-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.signal-title {
  display: block;
  color: #203A52;
  font-size: 26rpx;
  font-weight: 650;
}

.signal-subtitle {
  display: block;
  margin-top: 6rpx;
  color: #7B90A4;
  font-size: 19rpx;
}

.signal-live {
  display: flex;
  align-items: center;
  gap: 9rpx;
  padding: 8rpx 13rpx;
  border-radius: 20rpx;
  background: #EAF8F1;
  color: #247653;
  font-size: 18rpx;
}

.signal-live-dot,
.resource-status-dot {
  width: 9rpx;
  height: 9rpx;
  box-shadow: none;
}

.waveform {
  position: relative;
  display: flex;
  align-items: center;
  height: 100rpx;
  gap: 7rpx;
  margin-top: 22rpx;
  overflow: hidden;
}

.waveform::before {
  position: absolute;
  right: 0;
  left: 0;
  height: 1rpx;
  background: #D7E5F0;
  content: '';
}

.wave-bar {
  flex: 1;
  min-width: 3rpx;
  border-radius: 4rpx;
  background: #7AB1E8;
}

.wave-bar-hot {
  background: #2E6DD1;
}

.wave-sweep {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 55%;
  width: 80rpx;
  background: linear-gradient(90deg, transparent, rgba(46, 109, 209, 0.14), transparent);
  animation: sweep 2.8s linear infinite;
}

@keyframes sweep {
  from { transform: translateX(-420rpx); }
  to { transform: translateX(360rpx); }
}

.signal-legend {
  display: flex;
  justify-content: space-between;
  color: #71879B;
  font-size: 18rpx;
}

.resource-scroll {
  width: calc(100% + 24rpx);
  margin-top: 16rpx;
}

.resource-track {
  display: inline-flex;
  gap: 14rpx;
  padding-right: 24rpx;
}

.resource-item {
  width: 250rpx;
  padding: 22rpx;
  border: 1rpx solid #DCE7F0;
  border-radius: 18rpx;
  background: #FFFFFF;
}

.resource-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.resource-icon {
  width: 54rpx;
  height: 54rpx;
  border-radius: 14rpx;
  background: #EAF2FD;
}

.resource-status {
  display: flex;
  align-items: center;
  gap: 7rpx;
  color: #367A5C;
  font-size: 17rpx;
}

.resource-distance {
  display: block;
  margin-top: 28rpx;
  color: #2E6DD1;
  font-size: 34rpx;
  font-weight: 720;
  font-variant-numeric: tabular-nums;
}

.resource-name {
  display: block;
  margin-top: 7rpx;
  color: #243B52;
  font-size: 24rpx;
  font-weight: 650;
}

.resource-place {
  display: block;
  margin-top: 6rpx;
  overflow: hidden;
  color: #7C90A3;
  font-size: 18rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.health-panel {
  display: flex;
  align-items: center;
  padding: 26rpx;
  border: 1rpx solid #D8E4EE;
  border-radius: 20rpx 20rpx 0 0;
  background: #FFFFFF;
}

.heart-orbit {
  position: relative;
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 108rpx;
  height: 108rpx;
}

.orbit {
  position: absolute;
  border: 1rpx solid rgba(181, 40, 50, 0.17);
  border-radius: 50%;
}

.orbit-one {
  width: 96rpx;
  height: 96rpx;
}

.orbit-two {
  width: 72rpx;
  height: 72rpx;
}

.heart-core {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: #FBECEF;
}

.heart-reading {
  flex: 1;
  min-width: 0;
  margin-left: 18rpx;
}

.heart-number-line {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
}

.heart-number {
  color: #18334F;
  font-size: 52rpx;
  font-weight: 730;
  font-variant-numeric: tabular-nums;
}

.heart-unit {
  color: #7C90A3;
  font-size: 18rpx;
  font-weight: 600;
}

.heart-state {
  display: block;
  color: #31516C;
  font-size: 22rpx;
  font-weight: 600;
}

.heart-time {
  display: block;
  margin-top: 4rpx;
  color: #95A5B5;
  font-size: 17rpx;
}

.health-bars {
  display: flex;
  align-items: flex-end;
  gap: 4rpx;
  width: 126rpx;
  height: 70rpx;
}

.health-bar {
  flex: 1;
  min-width: 4rpx;
  border-radius: 4rpx;
  background: #77AEE6;
}

.health-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  border: 1rpx solid #D8E4EE;
  border-top: 0;
  border-radius: 0 0 20rpx 20rpx;
  background: #F9FCFF;
}

.metric {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 22rpx 18rpx;
}

.metric + .metric::before {
  position: absolute;
  top: 22rpx;
  bottom: 22rpx;
  left: 0;
  width: 1rpx;
  background: #DDE7F0;
  content: '';
}

.metric-label {
  color: #7B90A4;
  font-size: 18rpx;
}

.metric-value {
  margin-top: 8rpx;
  color: #203A52;
  font-size: 29rpx;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.metric-state {
  margin-top: 5rpx;
  color: #4C789C;
  font-size: 16rpx;
}

.service-layout {
  display: grid;
  grid-template-columns: 1.18fr 0.82fr;
  gap: 16rpx;
}

.service-primary,
.service-secondary {
  position: relative;
  min-height: 280rpx;
  padding: 24rpx;
  overflow: hidden;
  border-radius: 20rpx;
}

.service-primary {
  background: #2E6DD1;
}

.service-primary::after {
  position: absolute;
  right: -50rpx;
  bottom: -70rpx;
  width: 190rpx;
  height: 190rpx;
  border: 28rpx solid rgba(255, 255, 255, 0.08);
  border-radius: 50%;
  content: '';
}

.service-icon {
  width: 58rpx;
  height: 58rpx;
  border-radius: 15rpx;
  background: rgba(255, 255, 255, 0.15);
}

.service-primary-kicker {
  display: block;
  margin-top: 35rpx;
  color: rgba(255, 255, 255, 0.65);
  font-size: 14rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
}

.service-primary-title {
  display: block;
  margin-top: 8rpx;
  color: #FFFFFF;
  font-size: 29rpx;
  font-weight: 700;
}

.service-primary-desc {
  display: block;
  margin-top: 9rpx;
  color: rgba(255, 255, 255, 0.74);
  font-size: 19rpx;
  line-height: 1.55;
}

.service-arrow {
  position: absolute;
  right: 20rpx;
  bottom: 20rpx;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: #FFFFFF;
}

.service-secondary {
  border: 1rpx solid #CFE3E5;
  background: #EAF7F7;
}

.science-ring {
  width: 58rpx;
  height: 58rpx;
  border: 1rpx solid rgba(23, 126, 141, 0.16);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.72);
}

.service-secondary-title {
  display: block;
  margin-top: 35rpx;
  color: #23545B;
  font-size: 27rpx;
  font-weight: 700;
}

.service-secondary-desc {
  display: block;
  margin-top: 9rpx;
  color: #62848A;
  font-size: 18rpx;
  line-height: 1.55;
}

.lesson-meta {
  display: block;
  margin-top: 9rpx;
  color: #5E8086;
  font-size: 16rpx;
}

.safety-note {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  margin: 52rpx 24rpx 0;
  padding: 20rpx;
  border-top: 1rpx solid #D9E4EE;
  color: #7A8EA1;
  font-size: 19rpx;
}

.safety-line {
  width: 24rpx;
  height: 3rpx;
  border-radius: 2rpx;
  background: #B52832;
}

.bottom-space {
  height: calc(50rpx + env(safe-area-inset-bottom));
}

@media (max-width: 340px) {
  .hero-copy {
    width: 76%;
  }

  .hero-title {
    font-size: 47rpx;
  }

  .quick-action {
    padding: 18rpx 14rpx;
  }

  .quick-icon {
    margin-right: 9rpx;
  }

  .health-bars {
    display: none;
  }
}
</style>
