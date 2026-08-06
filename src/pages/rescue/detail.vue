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

        <view
          v-if="['PENDING', 'MATCHING', 'ACCEPTED', 'RESCUING'].includes(rescueCall.status)"
          class="cancel-card"
        >
          <text class="cancel-desc">误发或现场已缓解？可以取消这次呼救</text>
          <view class="cancel-button" @tap="confirmCancel">取消呼救</view>
        </view>

        <view v-if="feedbackCard" class="feedback-card">
          <template v-if="feedbackSubmitted">
            <view class="feedback-head">
              <view class="feedback-stars">
                <text
                  v-for="star in 5"
                  :key="star"
                  class="feedback-star"
                  :class="{ 'star-filled': star <= feedbackSubmitted.rating }"
                >★</text>
              </view>
              <text class="feedback-done">已评价</text>
            </view>
            <text v-if="feedbackSubmitted.comment" class="feedback-comment">
              {{ feedbackSubmitted.comment }}
            </text>
          </template>
          <template v-else>
            <text class="feedback-title">为这次救援评分</text>
            <view class="feedback-stars">
              <text
                v-for="star in 5"
                :key="star"
                class="feedback-star"
                :class="{ 'star-filled': star <= feedbackRating }"
                @tap="feedbackRating = star"
              >★</text>
            </view>
            <textarea
              v-model="feedbackComment"
              class="feedback-input"
              placeholder="感谢语或补充说明（选填）"
              maxlength="500"
              :auto-height="true"
            />
            <view class="feedback-submit" :class="{ 'feedback-submit-busy': feedbackSubmitting }" @tap="submitFeedback">
              <text>{{ feedbackSubmitting ? '提交中…' : '提交评价' }}</text>
            </view>
          </template>
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
import { addBaseTileLayer } from '@/common/mapTiles'
import { resolveApiUrl } from '@/api/http'
import {
  cancelRescueCall,
  getRescueCall,
  getRescueFeedback,
  retryRescueMatch,
  submitRescueFeedback,
  type RescueCallResponse,
  type RescueFeedbackResponse,
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
let renderedGeometryKey = ''
let matchRetryCount = 0
/** 约 3 分钟内的重试上限（每 3 轮轮询触发一次，轮询间隔 3 秒）。 */
const MATCH_RETRY_LIMIT = 60

const feedbackRating = ref(0)
const feedbackComment = ref('')
const feedbackSubmitting = ref(false)
const feedbackSubmitted = ref<RescueFeedbackResponse | null>(null)

/** 完成后展示评价卡片；呼救方对已完成的救援只能评价一次。 */
const feedbackCard = computed(() => rescueCall.value?.status === 'COMPLETED')

async function loadFeedback() {
  if (!rescueId.value || rescueCall.value?.status !== 'COMPLETED') return
  try {
    feedbackSubmitted.value = await getRescueFeedback(rescueId.value)
  } catch {
    feedbackSubmitted.value = null
  }
}

async function submitFeedback() {
  if (feedbackSubmitting.value || !rescueId.value) return
  if (feedbackRating.value < 1) {
    uni.showToast({ title: '请先选择星级', icon: 'none' })
    return
  }
  feedbackSubmitting.value = true
  try {
    feedbackSubmitted.value = await submitRescueFeedback(rescueId.value, {
      rating: feedbackRating.value,
      comment: feedbackComment.value.trim() || undefined
    })
    uni.showToast({ title: '感谢评价', icon: 'success' })
  } catch (error: any) {
    uni.showToast({ title: error?.message || '评价提交失败，请重试', icon: 'none' })
  } finally {
    feedbackSubmitting.value = false
  }
}

function confirmCancel() {
  uni.showModal({
    title: '取消这次呼救？',
    content: '取消后已匹配的设备将释放，如需帮助请重新发起呼救。',
    confirmText: '取消呼救',
    confirmColor: '#C93D46',
    success: async (result) => {
      if (!result.confirm || !rescueId.value) return
      try {
        const updated = await cancelRescueCall(rescueId.value)
        rescueCall.value = updated
        stopPolling()
        uni.showToast({ title: '已取消呼救', icon: 'success' })
      } catch (error: any) {
        uni.showToast({ title: error?.message || '取消失败，请重试', icon: 'none' })
      }
    }
  })
}

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
    width: 28,
    height: 28,
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
      width: 26,
      height: 32,
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
    // 重新匹配是写操作，每 3 轮（约 9 秒）才试一次并设上限
    if (!latest.matchedAed && latest.status === 'MATCHING' && !showLoading) {
      matchRetryCount += 1
      if (matchRetryCount % 3 === 1 && matchRetryCount <= MATCH_RETRY_LIMIT) {
        latest = await retryRescueMatch(rescueId.value)
      }
    } else if (latest.matchedAed) {
      matchRetryCount = 0
    }
    rescueCall.value = latest
    loading.value = false
    await nextTick()
    // #ifdef H5
    await initMap()
    // #endif
    if (['COMPLETED', 'CANCELLED'].includes(rescueCall.value.status)) {
      stopPolling()
      await loadFeedback()
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
        attributionControl: true,
        preferCanvas: true
      }).setView([latitude, longitude], 15)
      addBaseTileLayer(Leaflet, mapInstance, () => {
        mapUnavailable.value = true
      })
      mapDataLayer = Leaflet.layerGroup().addTo(mapInstance)
      mapUnavailable.value = false
    }

    // 只有位置或匹配结果变化才重绘，否则每轮 fitBounds 会让画面抖动
    const matched = rescueCall.value.matchedAed
    const geometryKey = [
      latitude,
      longitude,
      matched?.deviceId ?? '',
      matched?.latitude ?? '',
      matched?.longitude ?? ''
    ].join('|')
    if (geometryKey === renderedGeometryKey) {
      return
    }
    renderedGeometryKey = geometryKey

    mapDataLayer.clearLayers()
    const marker = Leaflet.divIcon({
      className: 'rescue-location-marker',
      html: '<span><i></i></span>',
      iconSize: [28, 35],
      iconAnchor: [14, 33]
    })
    Leaflet.marker([latitude, longitude], { icon: marker }).addTo(mapDataLayer)
    if (matched) {
      const deviceMarker = Leaflet.divIcon({
        className: 'matched-aed-marker',
        html: '<span>AED</span>',
        iconSize: [32, 32],
        iconAnchor: [16, 16]
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
  width: 25px;
  height: 25px;
  border: 3px solid #FFFFFF;
  border-radius: 50% 50% 50% 6px;
  background: #C22936;
  box-shadow: 0 5px 14px rgba(194, 41, 54, 0.28);
  transform: rotate(-45deg);
}
.rescue-location-marker i {
  position: absolute;
  inset: 7px;
  border-radius: 50%;
  background: #FFFFFF;
}
.matched-aed-marker {
  background: transparent;
  border: 0;
}
.matched-aed-marker span {
  display: flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border: 3px solid #FFFFFF;
  border-radius: 50%;
  background: #16855D;
  box-shadow: 0 4px 12px rgba(22, 133, 93, 0.28);
  color: #FFFFFF;
  font-size: 9px;
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
  padding: 30rpx;
  overflow: hidden;
  border-radius: 24rpx;
  background: #FFFFFF;
  border: 1rpx solid #E1E8F0;
  color: #1C2B45;
}

.status-completed {
  border-color: rgba(34, 168, 122, 0.4);
}

.status-cancelled {
  border-color: #E1E8F0;
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
  font-size: 40rpx;
  font-weight: 800;
  letter-spacing: -1rpx;
  color: #1C2B45;
}

.status-desc {
  display: block;
  margin-top: 14rpx;
  color: #68758A;
  font-size: 25rpx;
  line-height: 1.65;
}

.live-chip {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 9rpx 14rpx;
  border: 1rpx solid #D6E4F5;
  border-radius: 999rpx;
  background: #F2F7FD;
  color: #2E6DD1;
  font-size: 21rpx;
}

.live-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #23956A;
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
  color: #A8B4C4;
  font-size: 20rpx;
}

.progress-step::before {
  content: '';
  position: absolute;
  top: 11rpx;
  right: 50%;
  width: 100%;
  height: 2rpx;
  background: #E1E8F0;
}

.progress-step:first-child::before {
  display: none;
}

.progress-step.active {
  color: #2E6DD1;
}

.progress-step.active::before {
  background: #2E6DD1;
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
  border: 4rpx solid #E1E8F0;
  border-radius: 50%;
  background: #FFFFFF;
}

.progress-step.active .progress-node {
  border-color: #2E6DD1;
  background: #2E6DD1;
}

.progress-step.active:not(:last-child) .progress-node {
  background: #2E6DD1;
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

.cancel-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 24rpx 24rpx 0;
  padding: 22rpx 26rpx;
  border: 1rpx solid #E1E8F0;
  border-radius: 22rpx;
  background: #FFFFFF;
}

.cancel-desc {
  color: #68758A;
  font-size: 23rpx;
}

.cancel-button {
  padding: 10rpx 22rpx;
  border: 1rpx solid #C93D46;
  border-radius: 999rpx;
  color: #C93D46;
  font-size: 23rpx;
  font-weight: 650;
}

.feedback-card {
  margin: 24rpx 24rpx 0;
  padding: 30rpx;
  border: 1rpx solid #E1E8F0;
  border-radius: 22rpx;
  background: #FFFFFF;
}

.feedback-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.feedback-title {
  display: block;
  color: #1C2B45;
  font-size: 30rpx;
  font-weight: 700;
}

.feedback-done {
  color: #23956A;
  font-size: 23rpx;
  font-weight: 650;
}

.feedback-stars {
  display: flex;
  gap: 10rpx;
  margin-top: 20rpx;
}

.feedback-star {
  color: #D9E1EA;
  font-size: 52rpx;
  line-height: 1;
}

.feedback-star.star-filled {
  color: #F5A623;
}

.feedback-comment {
  display: block;
  margin-top: 18rpx;
  color: #4E5969;
  font-size: 24rpx;
  line-height: 1.6;
}

.feedback-input {
  width: 100%;
  box-sizing: border-box;
  margin-top: 20rpx;
  padding: 18rpx;
  border: 1rpx solid #E1E8F0;
  border-radius: 14rpx;
  background: #F7F9FC;
  color: #1C2B45;
  font-size: 24rpx;
  min-height: 100rpx;
}

.feedback-submit {
  margin-top: 20rpx;
  padding: 20rpx 0;
  border: 1rpx solid #2E6DD1;
  border-radius: 14rpx;
  text-align: center;
  color: #2E6DD1;
  font-size: 27rpx;
  font-weight: 700;
}

.feedback-submit-busy {
  opacity: 0.6;
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
