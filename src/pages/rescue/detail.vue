<template>
  <view class="page">
    <view class="page-scroll">
      <view v-if="loading" class="state-card">
        <view class="loading-ring"></view>
        <text class="state-title">正在同步救援状态</text>
        <text class="state-desc">请保持手机畅通</text>
      </view>

      <view v-else-if="errorMessage" class="state-card">
        <app-icon-tile class="state-icon" name="info-filled" tone="coral" size="large" />
        <text class="state-title">{{ errorMessage }}</text>
        <view class="state-action" @tap="loadDetail">
          <text>重新加载</text>
        </view>
      </view>

      <template v-else-if="rescueCall">
        <view class="status-panel" :class="`status-${rescueCall.status.toLowerCase()}`">
          <view class="status-head">
            <view>
              <text class="status-kicker">RESCUE STATUS</text>
              <text class="status-title">{{ statusMeta.label }}</text>
            </view>
            <view class="live-chip">
              <view class="live-dot"></view>
              <text>实时同步</text>
            </view>
          </view>
          <text class="status-desc">{{ statusMeta.description }}</text>
          <view class="status-progress">
            <view
              v-for="(step, index) in progressSteps"
              :key="step"
              class="progress-step"
              :class="{ active: index <= statusMeta.step }"
            >
              <view class="progress-node">
                <app-icon
                  v-if="index < statusMeta.step"
                  name="checkmarkempty"
                  :size="12"
                  color="#FFFFFF"
                />
              </view>
              <text>{{ step }}</text>
            </view>
          </view>
        </view>

        <view v-if="rescueCall.matchedAed" class="match-card">
          <view class="section-head">
            <view>
              <text class="section-kicker">FASTEST AED</text>
              <text class="section-title">已锁定最快到达资源</text>
            </view>
            <view class="eta-badge">
              <text class="eta-value">{{ formatEta(rescueCall.matchedAed.estimatedArrivalSeconds) }}</text>
              <text class="eta-label">预计到达</text>
            </view>
          </view>
          <view class="match-resource">
            <app-icon-tile
              :name="rescueCall.matchedAed.type === 'MOBILE' ? 'mobile-device' : 'fixed-device'"
              :tone="rescueCall.matchedAed.type === 'MOBILE' ? 'green' : 'blue'"
            />
            <view class="match-copy">
              <text class="match-name">{{ rescueCall.matchedAed.name }}</text>
              <text class="match-meta">
                {{ rescueCall.matchedAed.type === 'MOBILE' ? '移动 AED 车辆' : '固定 AED 取送' }}
                · {{ formatDistance(rescueCall.matchedAed.distanceMeters) }}
              </text>
              <text v-if="rescueCall.matchedAed.vehicleInfo" class="match-vehicle">
                {{ rescueCall.matchedAed.vehicleInfo }}
              </text>
            </view>
          </view>
          <view class="match-actions">
            <view
              v-if="rescueCall.matchedAed.ownerPhone"
              class="match-action match-action-secondary"
              @tap="callMatchedDevice"
            >
              <app-icon name="phone-filled" :size="16" color="#1F63D5" />
              <text>联系资源方</text>
            </view>
            <view class="match-action match-action-primary" @tap="openMatchedDevice">
              <app-icon name="navigate-filled" :size="16" color="#FFFFFF" />
              <text>查看设备位置</text>
            </view>
          </view>
        </view>

        <view v-else class="waiting-card">
          <view class="waiting-pulse"></view>
          <view>
            <text class="waiting-title">正在扩大范围匹配 AED</text>
            <text class="waiting-desc">系统会持续检查新上线的移动车辆与固定设备</text>
          </view>
        </view>

        <view class="map-card">
          <view class="section-head">
            <view>
              <text class="section-kicker">LIVE LOCATION</text>
              <text class="section-title">呼救位置</text>
            </view>
            <view class="section-link" @tap="openRescueLocation">
              <app-icon name="navigate-filled" :size="15" color="#1F63D5" />
              <text>导航</text>
            </view>
          </view>
          <!-- #ifdef H5 -->
          <view id="rescue-detail-map" class="rescue-map"></view>
          <view v-if="mapUnavailable" class="map-unavailable">
            <app-icon name="map-filled" :size="30" color="#6F7F99" />
            <text>底图加载失败，可使用系统地图导航</text>
          </view>
          <!-- #endif -->
          <!-- #ifndef H5 -->
          <map
            class="rescue-map"
            :longitude="rescueCall.longitude"
            :latitude="rescueCall.latitude"
            :markers="nativeMarkers"
            :scale="15"
            show-location
          />
          <!-- #endif -->
          <view class="location-row">
            <view class="location-icon">
              <app-icon name="location-filled" :size="18" color="#1F63D5" />
            </view>
            <view class="location-copy">
              <text class="location-address">{{ rescueCall.address }}</text>
              <text class="location-coordinates">
                {{ rescueCall.latitude.toFixed(5) }}, {{ rescueCall.longitude.toFixed(5) }}
              </text>
            </view>
          </view>
        </view>

        <view class="detail-card">
          <view class="section-head">
            <view>
              <text class="section-kicker">CALL DETAILS</text>
              <text class="section-title">呼救信息</text>
            </view>
            <view class="urgency-chip" :class="`urgency-${rescueCall.urgency.toLowerCase()}`">
              {{ urgencyLabel }}
            </view>
          </view>

          <view class="detail-list">
            <view class="detail-row">
              <text class="detail-label">呼救编号</text>
              <text class="detail-value detail-id">{{ rescueCall.id }}</text>
            </view>
            <view class="detail-row">
              <text class="detail-label">发起时间</text>
              <text class="detail-value">{{ formatTime(rescueCall.createdAt) }}</text>
            </view>
            <view v-if="rescueCall.symptoms.length" class="detail-row detail-row-block">
              <text class="detail-label">症状信息</text>
              <view class="symptom-list">
                <text v-for="symptom in rescueCall.symptoms" :key="symptom" class="symptom-chip">
                  {{ symptom }}
                </text>
              </view>
            </view>
            <view v-if="rescueCall.description" class="detail-row detail-row-block">
              <text class="detail-label">现场描述</text>
              <text class="description">{{ rescueCall.description }}</text>
            </view>
          </view>

          <view v-if="displayImageUrls.length" class="scene-section">
            <text class="detail-label">现场照片</text>
            <view class="scene-gallery">
              <image
                v-for="(imageUrl, index) in displayImageUrls"
                :key="imageUrl"
                class="scene-image"
                :src="imageUrl"
                mode="aspectFill"
                @tap="previewSceneImage(index)"
              />
            </view>
          </view>
        </view>

        <view class="safety-card">
          <app-icon-tile name="phone-filled" tone="coral" />
          <view class="safety-copy">
            <text class="safety-title">危及生命请立即拨打 120</text>
            <text class="safety-desc">平台协同不能替代专业急救，请遵循调度人员指导</text>
          </view>
          <view class="call-button" @tap="callEmergency">
            <text>拨打</text>
          </view>
        </view>

        <view class="bottom-space"></view>
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
// #ifdef H5
import 'leaflet/dist/leaflet.css'
// #endif
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import { resolveApiUrl } from '@/api/http'
import {
  getRescueCall,
  retryRescueMatch,
  type RescueCallResponse,
  type RescueStatus
} from '@/api/rescue'

const rescueId = ref('')
const rescueCall = ref<RescueCallResponse | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const mapUnavailable = ref(false)
let mapInstance: any = null
let mapDataLayer: any = null
let pollingTimer: ReturnType<typeof setInterval> | null = null

const progressSteps = ['已呼救', '匹配中', '救援中', '已完成']
const statusMap: Record<RescueStatus, { label: string; description: string; step: number }> = {
  PENDING: { label: '呼救已发出', description: '救援请求已进入调度队列，请保持电话畅通', step: 0 },
  MATCHING: { label: '正在匹配资源', description: '平台正在查找附近设备与救援力量', step: 1 },
  ACCEPTED: { label: '救援已响应', description: '救援人员已接单，请留在安全且显眼的位置', step: 2 },
  RESCUING: { label: '救援进行中', description: '请遵循专业人员指导并保持现场通道畅通', step: 2 },
  COMPLETED: { label: '救援已完成', description: '本次救援流程已结束，感谢每一位参与者', step: 3 },
  CANCELLED: { label: '呼救已取消', description: '本次呼救已经取消，如仍需帮助请重新发起', step: 0 }
}

const statusMeta = computed(() => statusMap[rescueCall.value?.status || 'PENDING'])

const urgencyLabel = computed(() => {
  const labels = { CRITICAL: '危急', HIGH: '紧急', MEDIUM: '一般' }
  return labels[rescueCall.value?.urgency || 'MEDIUM']
})

const displayImageUrls = computed(() => (
  rescueCall.value?.imageUrls.map(resolveApiUrl) || []
))

const nativeMarkers = computed(() => {
  if (!rescueCall.value) return []
  const markers: any[] = [{
    id: 1,
    longitude: rescueCall.value.longitude,
    latitude: rescueCall.value.latitude,
    iconPath: '/static/icons/tab-map-active.png',
    width: 38,
    height: 38,
    callout: {
      content: '呼救位置',
      color: '#B52832',
      fontSize: 13,
      borderRadius: 8,
      bgColor: '#FFFFFF',
      padding: 7,
      display: 'ALWAYS'
    }
  }]
  const matched = rescueCall.value.matchedAed
  if (matched) {
    markers.push({
      id: 2,
      longitude: matched.longitude,
      latitude: matched.latitude,
      iconPath: matched.type === 'MOBILE'
        ? '/static/map/marker-mobile.png'
        : '/static/map/marker-fixed.png',
      width: 34,
      height: 42,
      callout: {
        content: `已匹配·${formatEta(matched.estimatedArrivalSeconds)}`,
        color: '#1F63D5',
        fontSize: 13,
        borderRadius: 8,
        bgColor: '#FFFFFF',
        padding: 7,
        display: 'ALWAYS'
      }
    })
  }
  return markers
})

onLoad((query) => {
  rescueId.value = typeof query?.id === 'string' ? query.id : ''
  loadDetail()
  if (rescueId.value) {
    pollingTimer = setInterval(() => loadDetail(false), 3000)
  }
})

async function loadDetail(showLoading = true) {
  if (!rescueId.value) {
    loading.value = false
    errorMessage.value = '缺少呼救编号'
    return
  }

  if (showLoading) loading.value = true
  errorMessage.value = ''
  try {
    let latest = await getRescueCall(rescueId.value)
    if (!latest.matchedAed && latest.status === 'MATCHING' && !showLoading) {
      latest = await retryRescueMatch(rescueId.value)
    }
    rescueCall.value = latest
    loading.value = false
    await nextTick()
    // #ifdef H5
    await initMap()
    // #endif
    if (['COMPLETED', 'CANCELLED'].includes(rescueCall.value.status)) {
      stopPolling()
    }
  } catch (error: any) {
    if (showLoading) {
      errorMessage.value = error?.message || '救援状态加载失败'
    }
  } finally {
    if (showLoading) loading.value = false
  }
}

// #ifdef H5
async function initMap() {
  if (!rescueCall.value) return
  try {
    const module = await import('leaflet')
    const Leaflet = module.default || module
    const { latitude, longitude } = rescueCall.value
    if (!mapInstance) {
      mapInstance = Leaflet.map('rescue-detail-map', {
        zoomControl: false,
        attributionControl: true
      }).setView([latitude, longitude], 15)
      Leaflet.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '© OpenStreetMap'
      }).addTo(mapInstance)
      mapDataLayer = Leaflet.layerGroup().addTo(mapInstance)
    }
    mapDataLayer.clearLayers()
    const marker = Leaflet.divIcon({
      className: 'rescue-location-marker',
      html: '<span><i></i></span>',
      iconSize: [38, 48],
      iconAnchor: [19, 44]
    })
    Leaflet.marker([latitude, longitude], { icon: marker }).addTo(mapDataLayer)
    const matched = rescueCall.value.matchedAed
    if (matched) {
      const deviceMarker = Leaflet.divIcon({
        className: 'matched-aed-marker',
        html: '<span>AED</span>',
        iconSize: [42, 42],
        iconAnchor: [21, 21]
      })
      Leaflet.marker([matched.latitude, matched.longitude], { icon: deviceMarker })
        .addTo(mapDataLayer)
      Leaflet.polyline(
        [[latitude, longitude], [matched.latitude, matched.longitude]],
        { color: '#2F73E8', weight: 3, opacity: 0.72, dashArray: '8 8' }
      ).addTo(mapDataLayer)
      mapInstance.fitBounds(
        [[latitude, longitude], [matched.latitude, matched.longitude]],
        { padding: [42, 42], maxZoom: 15 }
      )
    }
    mapUnavailable.value = false
  } catch {
    mapUnavailable.value = true
  }
}
// #endif

function formatTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString('zh-CN', { hour12: false })
}

function openRescueLocation() {
  if (!rescueCall.value) return
  uni.openLocation({
    latitude: rescueCall.value.latitude,
    longitude: rescueCall.value.longitude,
    name: '呼救位置',
    address: rescueCall.value.address,
    scale: 16,
    fail: () => uni.showToast({ title: '无法打开系统地图', icon: 'none' })
  })
}

function callEmergency() {
  uni.makePhoneCall({ phoneNumber: '120' })
}

function callMatchedDevice() {
  const phoneNumber = rescueCall.value?.matchedAed?.ownerPhone
  if (phoneNumber) uni.makePhoneCall({ phoneNumber })
}

function openMatchedDevice() {
  const device = rescueCall.value?.matchedAed
  if (!device) return
  uni.openLocation({
    latitude: device.latitude,
    longitude: device.longitude,
    name: device.name,
    address: device.address,
    scale: 16,
    fail: () => uni.showToast({ title: '无法打开系统地图', icon: 'none' })
  })
}

function formatEta(seconds: number) {
  if (seconds < 60) return `${seconds}秒`
  return `${Math.ceil(seconds / 60)}分钟`
}

function formatDistance(meters: number) {
  return meters < 1000 ? `${meters}米` : `${(meters / 1000).toFixed(1)}公里`
}

function stopPolling() {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

function previewSceneImage(index: number) {
  uni.previewImage({
    current: displayImageUrls.value[index],
    urls: displayImageUrls.value
  })
}

onUnmounted(() => {
  stopPolling()
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }
})
</script>

<style>
/* #ifdef H5 */
.rescue-location-marker {
  background: transparent;
  border: 0;
}
.rescue-location-marker span {
  position: relative;
  display: block;
  width: 34px;
  height: 34px;
  border: 4px solid #FFFFFF;
  border-radius: 50% 50% 50% 8px;
  background: #C22936;
  box-shadow: 0 8px 20px rgba(194, 41, 54, 0.28);
  transform: rotate(-45deg);
}
.rescue-location-marker i {
  position: absolute;
  inset: 10px;
  border-radius: 50%;
  background: #FFFFFF;
}
.matched-aed-marker {
  background: transparent;
  border: 0;
}
.matched-aed-marker span {
  display: flex;
  width: 42px;
  height: 42px;
  align-items: center;
  justify-content: center;
  border: 4px solid #FFFFFF;
  border-radius: 50%;
  background: #16855D;
  box-shadow: 0 6px 16px rgba(22, 133, 93, 0.28);
  color: #FFFFFF;
  font-size: 11px;
  font-weight: 800;
}
/* #endif */
</style>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F2F6FB;
  color: #17243A;
}

.page-scroll {
  min-height: 100vh;
}

.state-card {
  min-height: 72vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
}

.state-icon {
  box-shadow: 0 12rpx 34rpx rgba(35, 65, 112, 0.1);
}

.loading-ring {
  width: 54rpx;
  height: 54rpx;
  border: 6rpx solid #DCE7F8;
  border-top-color: #2F73E8;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.state-title {
  margin-top: 28rpx;
  font-size: 32rpx;
  font-weight: 700;
}

.state-desc {
  margin-top: 10rpx;
  color: #75839A;
  font-size: 25rpx;
}

.state-action {
  margin-top: 30rpx;
  padding: 18rpx 34rpx;
  border-radius: 18rpx;
  background: #2F73E8;
  color: #FFFFFF;
  font-size: 26rpx;
  font-weight: 700;
}

.status-panel,
.map-card,
.detail-card,
.safety-card {
  margin: 24rpx 24rpx 0;
  border-radius: 26rpx;
}

.status-panel {
  padding: 34rpx 30rpx 30rpx;
  overflow: hidden;
  background:
    radial-gradient(circle at 88% 4%, rgba(255, 255, 255, 0.2), transparent 32%),
    linear-gradient(135deg, #275FBF 0%, #347DEB 100%);
  color: #FFFFFF;
  box-shadow: 0 16rpx 42rpx rgba(41, 101, 199, 0.24);
}

.status-completed {
  background: linear-gradient(135deg, #11815D 0%, #22A87A 100%);
}

.status-cancelled {
  background: linear-gradient(135deg, #68758A 0%, #8894A6 100%);
}

.status-head,
.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.status-kicker,
.section-kicker {
  display: block;
  font-size: 20rpx;
  font-weight: 700;
  letter-spacing: 3rpx;
}

.status-kicker {
  color: rgba(255, 255, 255, 0.68);
}

.status-title {
  display: block;
  margin-top: 8rpx;
  font-size: 42rpx;
  font-weight: 800;
  letter-spacing: -1rpx;
}

.status-desc {
  display: block;
  margin-top: 18rpx;
  color: rgba(255, 255, 255, 0.82);
  font-size: 25rpx;
  line-height: 1.65;
}

.live-chip {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 9rpx 14rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.2);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.12);
  font-size: 21rpx;
}

.live-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #9EF0C8;
  box-shadow: 0 0 0 6rpx rgba(158, 240, 200, 0.12);
}

.status-progress {
  display: flex;
  justify-content: space-between;
  margin-top: 34rpx;
}

.progress-step {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  color: rgba(255, 255, 255, 0.48);
  font-size: 20rpx;
}

.progress-step::before {
  content: '';
  position: absolute;
  top: 11rpx;
  right: 50%;
  width: 100%;
  height: 2rpx;
  background: rgba(255, 255, 255, 0.2);
}

.progress-step:first-child::before {
  display: none;
}

.progress-step.active {
  color: #FFFFFF;
}

.progress-step.active::before {
  background: rgba(255, 255, 255, 0.75);
}

.progress-node {
  position: relative;
  z-index: 1;
  width: 24rpx;
  height: 24rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 4rpx solid rgba(255, 255, 255, 0.45);
  border-radius: 50%;
  background: #3477DB;
}

.progress-step.active .progress-node {
  border-color: #FFFFFF;
  background: #FFFFFF;
}

.progress-step.active:not(:last-child) .progress-node {
  background: rgba(255, 255, 255, 0.18);
}

.map-card,
.detail-card,
.match-card {
  padding: 28rpx;
  background: #FFFFFF;
  box-shadow: 0 10rpx 28rpx rgba(38, 63, 103, 0.07);
}

.match-resource {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-top: 24rpx;
  padding: 20rpx;
  border-radius: 18rpx;
  background: #F3F7FC;
}

.match-copy { min-width: 0; flex: 1; }
.match-name,
.match-meta,
.match-vehicle { display: block; }
.match-name { font-size: 27rpx; font-weight: 800; }
.match-meta { margin-top: 6rpx; color: #53647E; font-size: 22rpx; }
.match-vehicle { margin-top: 6rpx; color: #7A889D; font-size: 21rpx; }

.eta-badge { text-align: right; }
.eta-value { display: block; color: #16855D; font-size: 32rpx; font-weight: 900; }
.eta-label { display: block; margin-top: 2rpx; color: #7A889D; font-size: 19rpx; }

.match-actions {
  display: flex;
  justify-content: flex-end;
  gap: 14rpx;
  margin-top: 20rpx;
}

.match-action {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  min-height: 64rpx;
  padding: 0 20rpx;
  border-radius: 15rpx;
  font-size: 22rpx;
  font-weight: 700;
}
.match-action-secondary { background: #EDF4FF; color: #1F63D5; }
.match-action-primary { background: #1F63D5; color: #FFFFFF; }

.waiting-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin: 24rpx 24rpx 0;
  padding: 24rpx 28rpx;
  border: 1rpx solid #DDE7F5;
  border-radius: 22rpx;
  background: #FFFFFF;
}
.waiting-pulse {
  width: 18rpx;
  height: 18rpx;
  flex: none;
  border-radius: 50%;
  background: #2F73E8;
  box-shadow: 0 0 0 10rpx rgba(47, 115, 232, 0.12);
}
.waiting-title,
.waiting-desc { display: block; }
.waiting-title { font-size: 25rpx; font-weight: 800; }
.waiting-desc { margin-top: 6rpx; color: #75839A; font-size: 21rpx; line-height: 1.45; }

.section-kicker {
  color: #7A8AA3;
}

.section-title {
  display: block;
  margin-top: 5rpx;
  font-size: 32rpx;
  font-weight: 800;
}

.section-link {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 12rpx 16rpx;
  border-radius: 14rpx;
  background: #EDF4FF;
  color: #1F63D5;
  font-size: 23rpx;
  font-weight: 700;
}

.rescue-map,
.map-unavailable {
  height: 360rpx;
  margin-top: 24rpx;
  overflow: hidden;
  border-radius: 20rpx;
}

.map-unavailable {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  background: #EDF2F8;
  color: #6F7F99;
  font-size: 23rpx;
}

.location-row {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-top: 22rpx;
}

.location-icon {
  flex: none;
  width: 58rpx;
  height: 58rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 17rpx;
  background: #EDF4FF;
}

.location-copy {
  min-width: 0;
  flex: 1;
}

.location-address,
.location-coordinates {
  display: block;
}

.location-address {
  font-size: 26rpx;
  font-weight: 700;
  line-height: 1.45;
}

.location-coordinates {
  margin-top: 5rpx;
  color: #93A0B4;
  font-size: 21rpx;
}

.urgency-chip {
  padding: 9rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 700;
}

.urgency-critical {
  background: #FFF0F1;
  color: #C22936;
}

.urgency-high {
  background: #FFF4E8;
  color: #C76713;
}

.urgency-medium {
  background: #EDF4FF;
  color: #1F63D5;
}

.detail-list {
  margin-top: 20rpx;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #E9EEF5;
}

.detail-row:last-child {
  border-bottom: 0;
}

.detail-row-block {
  display: block;
}

.detail-label {
  flex: none;
  color: #7B899E;
  font-size: 24rpx;
}

.detail-value {
  min-width: 0;
  text-align: right;
  font-size: 24rpx;
  font-weight: 600;
}

.detail-id {
  max-width: 430rpx;
  overflow-wrap: anywhere;
  color: #53627A;
  font-size: 21rpx;
}

.symptom-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 15rpx;
}

.symptom-chip {
  padding: 9rpx 14rpx;
  border-radius: 12rpx;
  background: #F0F5FC;
  color: #3C4F6B;
  font-size: 22rpx;
}

.description {
  display: block;
  margin-top: 13rpx;
  font-size: 25rpx;
  line-height: 1.7;
}

.scene-section {
  padding-top: 22rpx;
  border-top: 1rpx solid #E9EEF5;
}

.scene-gallery {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12rpx;
  margin-top: 15rpx;
}

.scene-image {
  width: 100%;
  height: 176rpx;
  border-radius: 16rpx;
  background: #EDF2F8;
}

.safety-card {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 24rpx;
  background: #FFF2F2;
  border: 1rpx solid #FAD6D8;
}

.safety-copy {
  min-width: 0;
  flex: 1;
}

.safety-title,
.safety-desc {
  display: block;
}

.safety-title {
  color: #A31F2A;
  font-size: 24rpx;
  font-weight: 800;
}

.safety-desc {
  margin-top: 5rpx;
  color: #94666B;
  font-size: 20rpx;
  line-height: 1.5;
}

.call-button {
  flex: none;
  padding: 13rpx 20rpx;
  border-radius: 14rpx;
  background: #C22936;
  color: #FFFFFF;
  font-size: 23rpx;
  font-weight: 800;
}

.bottom-space {
  height: calc(36rpx + env(safe-area-inset-bottom));
}
</style>
