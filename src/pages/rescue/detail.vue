<template>
  <view class="page apple-page motion-page-focus">
    <view class="page-scroll">
      <view v-if="loading" class="detail-skeleton motion-enter">
        <view class="skeleton-heading">
          <view><view class="skeleton-title motion-skeleton"></view><view class="skeleton-text motion-skeleton"></view></view>
          <view class="skeleton-eta motion-skeleton"></view>
        </view>
        <view class="skeleton-lifeline motion-skeleton"></view>
        <view class="skeleton-map motion-skeleton"></view>
        <view class="skeleton-action motion-skeleton"></view>
        <view class="skeleton-caption">正在同步救援状态，请保持手机畅通</view>
      </view>

      <view v-else-if="errorMessage" class="state-card">
        <app-icon-tile class="state-icon" name="info-filled" tone="coral" size="large" />
        <text class="state-title">{{ errorMessage }}</text>
        <view class="state-action" @tap="loadDetail">
          <text>重新加载</text>
        </view>
      </view>

      <template v-else-if="rescueCall">
        <view class="rescue-overview motion-enter">
          <view class="status-heading">
            <view class="status-copy">
              <view class="status-kicker"><text>救援动态</text></view>
              <text class="status-line">{{ statusMeta.label }}</text>
              <text class="status-desc">{{ statusMeta.description }}</text>
            </view>
            <view class="status-seal" :class="`urgency-${rescueCall.urgency.toLowerCase()}`">
              <text>{{ urgencyLabel }}</text>
            </view>
          </view>

          <view class="status-context">
            <text>{{ formatTime(rescueCall.createdAt) }}</text>
            <text class="status-context-separator">·</text>
            <text>{{ rescueCall.address }}</text>
          </view>
        </view>

        <view v-if="isAwaitingMatch" class="matching-board motion-enter motion-enter-delay-1">
          <view class="matching-signal" aria-hidden="true"><view></view><view></view><view></view></view>
          <text class="matching-board-title">呼救已送达</text>
          <text class="matching-board-desc">正在联系附近救援人员与 AED</text>
          <view class="matching-board-live"><text>有新进展时会立即通知你</text></view>
        </view>

        <view v-else-if="!hasReachedScene" class="route-card motion-enter motion-enter-delay-1">
          <view class="route-map-wrap">
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
            :polyline="nativePolylines"
            :scale="15"
            show-location
          />
          <!-- #endif -->
          <view v-if="rescueCall.matchedAed" class="tracking-chip">
            <text>{{ trackingLabel }}</text>
          </view>
          </view>
          <view class="route-sheet">
            <view v-if="rescueCall.matchedAed" class="route-telemetry" :class="{ 'route-telemetry-wide': !isResponder }">
              <view class="telemetry-item">
                <text class="telemetry-value">{{ liveEta }}</text>
                <text class="telemetry-label">预计到达</text>
              </view>
              <view class="telemetry-divider"></view>
              <view class="telemetry-item">
                <text class="telemetry-value telemetry-distance">{{ liveDistance }}</text>
                <text class="telemetry-label">距求救现场</text>
              </view>
            </view>
            <view class="route-summary">
              <view class="route-place">
                <view class="place-mark aed-mark">AED</view>
                <view class="place-copy">
                  <text class="place-label">救援资源</text>
                  <text class="place-value">{{ rescueCall.matchedAed?.name || '正在匹配附近 AED' }}</text>
                </view>
              </view>
              <view class="route-connector"><view class="route-flow"></view></view>
              <view class="route-place">
                <view class="place-mark scene-mark"></view>
                <view class="place-copy">
                  <text class="place-label">求救现场</text>
                  <text class="place-value">{{ rescueCall.address }}</text>
                </view>
              </view>
            </view>
          </view>
        </view>

        <view v-else class="arrival-card motion-enter motion-enter-delay-1">
          <view class="arrival-copy">
            <text class="arrival-title">{{ rescueCall.status === 'COMPLETED' ? '本次救援已完成' : '救援人员已到达现场' }}</text>
            <text class="arrival-place">{{ rescueCall.address }}</text>
            <text v-if="rescueCall.status === 'COMPLETED' && rescueDuration" class="arrival-duration">
              救援用时 {{ rescueDuration }}
            </text>
          </view>
          <app-icon name="checkmarkempty" :size="18" color="#248A5A" />
        </view>

        <view v-if="isResponder && responderNextAction" class="current-action motion-enter motion-enter-delay-2">
          <view class="current-action-copy">
            <text class="current-action-label">当前救援节点</text>
            <text class="current-action-title">{{ responderActionGuidance }}</text>
          </view>
          <view class="current-action-button" :class="{ busy: responderActionBusy }" @tap="performDetailAction">
            {{ responderActionBusy ? '同步中…' : responderNextAction.label }}
          </view>
        </view>

        <view v-if="rescueCall.status === 'PENDING_CONFIRMATION' && !isResponder" class="current-action motion-enter motion-enter-delay-2">
          <view class="current-action-copy">
            <text class="current-action-label">等待你的确认</text>
            <text class="current-action-title">施救者已提交完成，请确认现场救援已结束</text>
          </view>
          <view class="current-action-button" @tap="confirmCompletion">确认完成救援</view>
        </view>

        <view class="quick-actions motion-enter motion-enter-delay-3">
          <view v-if="hasCounterpartPhone" class="quick-action" @tap="callCounterpart">
            <app-icon name="phone-filled" :size="18" color="#2E6DD1" />
            <text>{{ isResponder ? '联系求救者' : '联系施救者' }}</text>
            <text class="row-chevron">›</text>
          </view>
          <view v-if="isResponder && !hasReachedScene" class="quick-action" @tap="openRescueLocation">
            <app-icon name="navigate-filled" :size="18" color="#2E6DD1" />
            <text>现场导航</text>
            <text class="row-chevron">›</text>
          </view>
          <view class="quick-action emergency" @tap="callEmergency">
            <app-icon name="phone-filled" :size="18" color="#C93D46" />
            <text>紧急情况拨打 120</text>
            <text class="row-chevron">›</text>
          </view>
        </view>

        <view v-if="counterpart" class="panel coordination-panel">
          <view class="panel-head">
            <text class="panel-title">救援协作</text>
          </view>
          <view class="participant-row participant-row-first">
            <text class="participant-label">我的角色</text>
            <text class="participant-value">{{ isResponder ? '现场施救者' : '求救者' }}</text>
          </view>
          <view class="participant-row">
            <text class="participant-label">协作人员</text>
            <app-avatar :src="counterpart?.avatarUrl" :name="counterpart?.displayName" :size="30" />
            <text class="participant-value">{{ counterpart?.displayName || '待响应' }} · {{ isResponder ? '求救者' : '现场施救者' }}</text>
          </view>
        </view>

        <view v-if="rescueCall.matchedAed && hasReachedScene" class="panel aed-panel">
          <view class="panel-head">
            <text class="panel-title">救援 AED</text>
            <text class="distance-readout">{{ aedDistanceLabel }}</text>
          </view>
          <view class="aed-resource">
            <view class="aed-copy">
              <text class="aed-name">{{ rescueCall.matchedAed.name }}</text>
              <text class="aed-meta">{{ rescueCall.matchedAed.address }}</text>
            </view>
          </view>
        </view>

        <view v-if="!rescueCall.matchedAed && !isAwaitingMatch" class="waiting-card">
          <view class="waiting-pulse"></view>
          <view>
            <text class="waiting-title">正在扩大范围匹配 AED</text>
            <text class="waiting-desc">系统会持续检查新上线的移动车辆与固定设备</text>
          </view>
        </view>

        <view class="panel detail-card">
          <view class="panel-head">
            <text class="panel-title">现场信息</text>
          </view>

          <view class="detail-list">
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

        <view
          v-if="!isResponder && ['PENDING', 'MATCHING'].includes(rescueCall.status)"
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
import 'leaflet/dist/leaflet.css'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import AppAvatar from '@/components/AppAvatar.vue'
import { addBaseTileLayer } from '@/common/mapTiles'
import { planAmapRoute } from '@/common/amapRoute'
import { resolveApiUrl } from '@/api/http'
import {
  cancelRescueCall,
  confirmRescueCompletion,
  getRescueCall,
  getResponderTask,
  performResponderAction,
  getRescueFeedback,
  submitRescueFeedback,
  updateResponderLocation,
  type RescueCallResponse,
  type RescueFeedbackResponse,
  type RescueParticipant
} from '@/api/rescue'
import { issueMediaDownload } from '@/api/files'
import { connectRescueEvents } from '@/utils/rescueEvents'
import { getCurrentGcj02Location, openMapNavigation } from '@/utils/location'
import { rescueStatusPresentation, userFacingError } from '@/utils/presentation'

const rescueId = ref('')
const rescueCall = ref<RescueCallResponse | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const isResponder = ref(false)
const mapUnavailable = ref(false)
let mapInstance: any = null
let mapDataLayer: any = null
let pollingTimer: ReturnType<typeof setInterval> | null = null
let disconnectEvents: (() => void) | null = null
let renderedGeometryKey = ''
let lastResponderLocationSyncAt = 0
const responderActionBusy = ref(false)
const routeProgress = ref(0)
let routeMovingMarker: any = null
const routePlanState = ref<'idle' | 'loading' | 'ready' | 'unavailable'>('idle')

const feedbackRating = ref(0)
const feedbackComment = ref('')
const feedbackSubmitting = ref(false)
const feedbackSubmitted = ref<RescueFeedbackResponse | null>(null)


const feedbackCard = computed(() => !isResponder.value && rescueCall.value?.status === 'COMPLETED')
const rescueDuration = computed(() => {
  const call = rescueCall.value
  if (!call?.completedAt) return ''
  const start = Date.parse(call.acceptedAt || call.createdAt)
  const end = Date.parse(call.completedAt)
  if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) return ''
  return formatDuration(Math.floor((end - start) / 1000))
})

function formatDuration(totalSeconds: number) {
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  if (hours) return `${hours}小时${minutes}分`
  if (minutes) return `${minutes}分${String(seconds).padStart(2, '0')}秒`
  return `${seconds}秒`
}

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
    uni.showToast({ title: userFacingError(error, '评价提交失败，请重试'), icon: 'none' })
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
        uni.showToast({ title: userFacingError(error, '取消失败，请重试'), icon: 'none' })
      }
    }
  })
}

const statusMeta = computed(() => rescueStatusPresentation(rescueCall.value?.status))

const isEnRoute = computed(() => ['EN_ROUTE_TO_AED', 'EN_ROUTE_TO_REQUESTER'].includes(rescueCall.value?.status || ''))
const hasReachedScene = computed(() => ['ARRIVED', 'RESCUING', 'PENDING_CONFIRMATION', 'COMPLETED'].includes(rescueCall.value?.status || ''))
const isAwaitingMatch = computed(() => ['PENDING', 'MATCHING'].includes(rescueCall.value?.status || '') && !rescueCall.value?.matchedAed)
const trackingLabel = computed(() => {
  if (hasReachedScene.value) return '已到达现场'
  if (routePlanState.value === 'loading') return '正在规划道路路线'
  if (routePlanState.value === 'unavailable') return '资源与现场位置'
  if (rescueCall.value?.liveTracking) return '实时驰援轨迹'
  return '建议驰援轨迹'
})
const remainingDistanceMeters = computed(() => {
  const call = rescueCall.value
  const distance = call?.matchedAed?.distanceMeters
  if (!call || distance == null) return null
  if (hasReachedScene.value) return 0
  if (call.liveTracking) {
    return Math.round(haversineMeters(
      call.liveTracking.responderLatitude,
      call.liveTracking.responderLongitude,
      call.latitude,
      call.longitude
    ))
  }
  return Math.max(0, Math.round(distance * (isEnRoute.value ? Math.max(0.06, 1 - routeProgress.value) : 1)))
})
const liveDistance = computed(() => hasReachedScene.value
  ? '现场'
  : remainingDistanceMeters.value == null ? '计算中' : formatDistance(remainingDistanceMeters.value))
const aedDistanceLabel = computed(() => hasReachedScene.value ? '已到场' : liveDistance.value)
const liveEta = computed(() => {
  const call = rescueCall.value
  const seconds = call?.matchedAed?.estimatedArrivalSeconds
  if (seconds == null) return '计算中'
  if (['ARRIVED', 'RESCUING', 'PENDING_CONFIRMATION', 'COMPLETED'].includes(call?.status || '')) return '已到达'
  const initialDistance = Math.max(1, call?.matchedAed?.distanceMeters || 1)
  const multiplier = call?.liveTracking
    ? Math.min(1.5, Math.max(0.06, (remainingDistanceMeters.value || initialDistance) / initialDistance))
    : isEnRoute.value ? Math.max(0.06, 1 - routeProgress.value) : 1
  return formatLiveEta(Math.max(10, Math.round(seconds * multiplier)))
})

const counterpart = computed<RescueParticipant | undefined>(() => {
  if (!rescueCall.value) return undefined
  return isResponder.value ? rescueCall.value.requester : rescueCall.value.responder
})

const hasCounterpartPhone = computed(() => /^1\d{10}$/.test(counterpart.value?.phone || ''))

type DetailTaskAction = 'aed-arrival' | 'aed-pickup' | 'requester-arrival' | 'rescue-start' | 'completion-submission' | 'aed-return'

const responderNextAction = computed<{ action: DetailTaskAction; label: string } | null>(() => {
  const call = rescueCall.value
  if (!isResponder.value || !call) return null
  if (call.status === 'EN_ROUTE_TO_AED') {
    return call.arrivedAtAedAt
      ? { action: 'aed-pickup', label: '已取 AED，赶往现场' }
      : { action: 'aed-arrival', label: '已到达 AED 取用点' }
  }
  if (call.status === 'EN_ROUTE_TO_REQUESTER') return { action: 'requester-arrival', label: '已到达求救者位置' }
  if (call.status === 'ARRIVED') return { action: 'rescue-start', label: '开始现场施救' }
  if (call.status === 'RESCUING') return { action: 'completion-submission', label: '提交完成救援' }
  if (call.status === 'COMPLETED' && call.aedCustodyStatus === 'RETURNING') {
    return { action: 'aed-return', label: '确认已归还 AED' }
  }
  return null
})

const responderActionGuidance = computed(() => {
  const action = responderNextAction.value?.action
  return ({
    'aed-arrival': '导航到设备点后确认到达',
    'aed-pickup': '取出设备后开始赶往现场',
    'requester-arrival': '见到求救者后确认到达',
    'rescue-start': '确认环境安全后开始施救',
    'completion-submission': '现场救援结束后提交双方确认',
    'aed-return': '将固定 AED 归还原位后确认'
  } as Record<string, string>)[action || ''] || ''
})

const urgencyLabel = computed(() => {
  const labels = { CRITICAL: '危急', HIGH: '紧急', MEDIUM: '一般' }
  return labels[rescueCall.value?.urgency || 'MEDIUM']
})

const displayImageUrls = ref<string[]>([])

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
    const matchedAtScene = ['ARRIVED', 'RESCUING', 'PENDING_CONFIRMATION', 'COMPLETED'].includes(rescueCall.value.status)
    markers.push({
      id: 2,
      longitude: matchedAtScene ? rescueCall.value.longitude : matched.longitude,
      latitude: matchedAtScene ? rescueCall.value.latitude : matched.latitude,
      iconPath: matched.type === 'MOBILE'
        ? '/static/map/marker-mobile.png'
        : '/static/map/marker-fixed.png',
      width: 26,
      height: 32,
      callout: {
        content: matchedAtScene ? 'AED 已到场' : `已匹配·${formatEta(matched.estimatedArrivalSeconds)}`,
        color: '#1F63D5',
        fontSize: 13,
        borderRadius: 8,
        bgColor: '#FFFFFF',
        padding: 7,
        display: 'ALWAYS'
      }
    })
  }
  const live = ['ARRIVED', 'RESCUING', 'PENDING_CONFIRMATION', 'COMPLETED'].includes(rescueCall.value.status)
    ? undefined
    : rescueCall.value.liveTracking
  if (live) {
    markers.push({
      id: 3,
      longitude: live.responderLongitude,
      latitude: live.responderLatitude,
      iconPath: '/static/map/marker-mobile.png',
      width: 26,
      height: 32,
      callout: {
        content: live.source === 'MOBILE_AED' ? 'AED 实时位置' : '施救者实时位置',
        color: '#147452',
        fontSize: 12,
        borderRadius: 8,
        bgColor: '#FFFFFF',
        padding: 7,
        display: 'ALWAYS'
      }
    })
  }
  return markers
})

const nativePolylines = computed(() => {
  return []
})

onLoad((query) => {
  rescueId.value = typeof query?.id === 'string' ? query.id : ''
  isResponder.value = query?.mode === 'responder'
  loadDetail()
  if (rescueId.value) {
    pollingTimer = setInterval(() => loadDetail(false), 3000)
    disconnectEvents = connectRescueEvents(event => {
      if (!event.rescueCallId || event.rescueCallId === rescueId.value) loadDetail(false)
    })
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
    const latest = isResponder.value
      ? await loadResponderDetail()
      : await getRescueCall(rescueId.value)
    rescueCall.value = latest
    if (isResponder.value) void syncResponderLocation()
    await refreshAttachmentUrls(latest.attachmentMediaIds)
    loading.value = false
    await nextTick()
    if (hasReachedScene.value || isAwaitingMatch.value) destroyMap()
    else await initMap()
    if (['COMPLETED', 'NO_RESOURCE', 'EXPIRED', 'USER_CANCELLED', 'SYSTEM_FAILED'].includes(rescueCall.value.status)) {
      stopPolling()
      await loadFeedback()
    }
  } catch (error: any) {
    if (showLoading) {
      errorMessage.value = userFacingError(error, '救援状态加载失败')
    }
  } finally {
    if (showLoading) loading.value = false
  }
}

async function loadResponderDetail(): Promise<RescueCallResponse> {
  const task = await getResponderTask(rescueId.value)
  if (!task.detailAvailable || task.latitude == null || task.longitude == null || !task.address) {
    throw new Error('确认响应后才能查看完整救援信息')
  }
  return {
    ...task,
    latitude: task.latitude,
    longitude: task.longitude,
    address: task.address,
    eventSequence: task.eventSequence
  }
}

const attachmentUrlCache = new Map<string, string>()
async function refreshAttachmentUrls(mediaIds: string[]) {
  const urls = await Promise.all(mediaIds.map(async mediaId => {
    if (!attachmentUrlCache.has(mediaId)) {
      const download = await issueMediaDownload(mediaId)
      attachmentUrlCache.set(mediaId, resolveApiUrl(download.url))
    }
    return attachmentUrlCache.get(mediaId) as string
  }))
  displayImageUrls.value = urls
}

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

    const matched = rescueCall.value.matchedAed
    const live = hasReachedScene.value ? undefined : rescueCall.value.liveTracking
    const geometryKey = [
      latitude,
      longitude,
      rescueCall.value.status,
      matched?.deviceId ?? '',
      matched?.latitude ?? '',
      matched?.longitude ?? '',
      live?.responderLatitude ?? '',
      live?.responderLongitude ?? ''
    ].join('|')
    if (geometryKey === renderedGeometryKey) {
      return
    }
    renderedGeometryKey = geometryKey

    mapDataLayer.clearLayers()
    routeMovingMarker = null
    const marker = Leaflet.divIcon({
      className: 'rescue-location-marker',
      html: '<span><i></i></span>',
      iconSize: [28, 35],
      iconAnchor: [14, 33]
    })
    Leaflet.marker([latitude, longitude], { icon: marker }).addTo(mapDataLayer)
    if (matched) {
      const deviceMarker = Leaflet.divIcon({
        className: hasReachedScene.value ? 'matched-aed-marker matched-aed-arrived' : 'matched-aed-marker',
        html: '<span>AED</span>',
        iconSize: [32, 32],
        iconAnchor: [16, 16]
      })
      const routeStart: [number, number] = live
        ? [live.responderLatitude, live.responderLongitude]
        : [matched.latitude, matched.longitude]
      const routeEnd: [number, number] = [latitude, longitude]
      routePlanState.value = 'loading'
      const amapRoute = await planAmapRoute(
        routeStart,
        routeEnd,
        matched.type === 'MOBILE' ? 'drive' : 'walk'
      )
      if (renderedGeometryKey !== geometryKey) return
      const plannedRoute = amapRoute
      routePlanState.value = plannedRoute ? 'ready' : 'unavailable'

      if (plannedRoute) {
        Leaflet.polyline(plannedRoute, {
          color: '#FFFFFF', weight: 12, opacity: .96, lineCap: 'round', lineJoin: 'round'
        }).addTo(mapDataLayer)
        Leaflet.polyline(plannedRoute, {
          className: 'rescue-route-main', color: hasReachedScene.value ? '#248A5A' : '#1677FF',
          weight: 7, opacity: 1, lineCap: 'round', lineJoin: 'round'
        }).addTo(mapDataLayer)
        if (!hasReachedScene.value) {
          Leaflet.polyline(plannedRoute, {
            className: 'rescue-route-flow', color: 'rgba(255,255,255,.72)', weight: 2.5,
            opacity: 1, dashArray: '4 18', lineCap: 'round', lineJoin: 'round'
          }).addTo(mapDataLayer)
        }
      }

      if (!live) {
        const displayedProgress = plannedRoute
          ? (hasReachedScene.value ? 1 : isEnRoute.value ? routeProgress.value : 0)
          : 0
        const markerPosition = plannedRoute
          ? positionAlongRoute(plannedRoute, displayedProgress)
          : routeStart
        routeMovingMarker = Leaflet.marker(markerPosition, { icon: deviceMarker }).addTo(mapDataLayer)
      }
      mapInstance.fitBounds(
        plannedRoute || [routeStart, routeEnd],
        { paddingTopLeft: [38, 58], paddingBottomRight: [38, 44], maxZoom: 18 }
      )
    }
    if (live) {
      const liveMarker = Leaflet.divIcon({
        className: 'matched-aed-marker',
        html: `<span>${live.source === 'MOBILE_AED' ? 'AED' : '施救'}</span>`,
        iconSize: [32, 32],
        iconAnchor: [16, 16]
      })
      Leaflet.marker([live.responderLatitude, live.responderLongitude], { icon: liveMarker })
        .addTo(mapDataLayer)
    }
  } catch {
    mapUnavailable.value = true
  }
}

function destroyMap() {
  renderedGeometryKey = ''
  routePlanState.value = 'idle'
  mapDataLayer = null
  routeMovingMarker = null
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }
}

function positionAlongRoute(points: [number, number][], progress: number): [number, number] {
  if (!points.length) return [0, 0]
  const position = Math.max(0, Math.min(1, progress)) * (points.length - 1)
  const index = Math.min(points.length - 2, Math.floor(position))
  const fraction = position - index
  const current = points[index]
  const next = points[index + 1] || current
  return [
    current[0] + (next[0] - current[0]) * fraction,
    current[1] + (next[1] - current[1]) * fraction
  ]
}

function formatTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString('zh-CN', { hour12: false })
}

function openRescueLocation() {
  if (!rescueCall.value) return
  openMapNavigation({
    latitude: rescueCall.value.latitude,
    longitude: rescueCall.value.longitude,
    name: '救援现场',
    address: rescueCall.value.address,
    mode: 'car'
  })
}

function callEmergency() {
  uni.makePhoneCall({ phoneNumber: '120' })
}

function callCounterpart() {
  if (hasCounterpartPhone.value && counterpart.value?.phone) {
    uni.makePhoneCall({ phoneNumber: counterpart.value.phone })
  }
}

async function performDetailAction() {
  const next = responderNextAction.value
  if (!next || responderActionBusy.value || !rescueId.value) return
  if (next.action === 'completion-submission') {
    uni.showModal({
      title: '确认完成救援？',
      content: '提交后将等待求救者确认，请确保现场已妥善处理。',
      confirmText: '提交完成',
      success: result => {
        if (result.confirm) void executeDetailAction(next.action)
      }
    })
    return
  }
  await executeDetailAction(next.action)
}

async function executeDetailAction(action: DetailTaskAction) {
  responderActionBusy.value = true
  try {
    await performResponderAction(rescueId.value, action)
    await loadDetail(false)
    uni.showToast({ title: '救援状态已同步', icon: 'success' })
  } catch (error: any) {
    uni.showToast({ title: userFacingError(error, '救援进展更新失败'), icon: 'none' })
  } finally {
    responderActionBusy.value = false
  }
}

async function syncResponderLocation() {
  const now = Date.now()
  if (!rescueId.value || now - lastResponderLocationSyncAt < 12_000) return
  lastResponderLocationSyncAt = now
  try {
    const location = await getCurrentGcj02Location()
    await updateResponderLocation(rescueId.value, {
      latitude: location.latitude,
      longitude: location.longitude
    })
  } catch {
  }
}

async function confirmCompletion() {
  if (!rescueId.value || rescueCall.value?.status !== 'PENDING_CONFIRMATION') return
  try {
    rescueCall.value = await confirmRescueCompletion(rescueId.value)
    uni.showToast({ title: '救援已完成', icon: 'success' })
    stopPolling()
    await loadFeedback()
  } catch (error: any) {
    uni.showToast({ title: userFacingError(error, '确认失败，请重试'), icon: 'none' })
  }
}

function formatEta(seconds: number) {
  if (seconds < 60) return `${seconds}秒`
  return `${Math.ceil(seconds / 60)}分钟`
}

function formatLiveEta(seconds: number) {
  if (seconds < 60) return `${seconds}秒`
  if (seconds < 180) return `${Math.floor(seconds / 60)}分${seconds % 60}秒`
  return `${Math.ceil(seconds / 60)}分钟`
}

function haversineMeters(fromLatitude: number, fromLongitude: number, toLatitude: number, toLongitude: number) {
  const radians = (degrees: number) => degrees * Math.PI / 180
  const latitudeDelta = radians(toLatitude - fromLatitude)
  const longitudeDelta = radians(toLongitude - fromLongitude)
  const fromLat = radians(fromLatitude)
  const toLat = radians(toLatitude)
  const haversine = Math.sin(latitudeDelta / 2) ** 2
    + Math.cos(fromLat) * Math.cos(toLat) * Math.sin(longitudeDelta / 2) ** 2
  const clamped = Math.min(1, Math.max(0, haversine))
  return 6_371_000 * 2 * Math.asin(Math.sqrt(clamped))
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
  disconnectEvents?.()
  destroyMap()
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
  position: relative;
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
.matched-aed-marker span::after {
  content: '';
  position: absolute;
  inset: -11px;
  z-index: -1;
  border: 2px solid rgba(35,149,106,.48);
  border-radius: 50%;
  animation: aedBeacon 1.8s ease-out infinite;
}
.matched-aed-arrived span::after { display: none; }
.rescue-route-flow {
  animation: rescueRouteFlow 1.25s linear infinite;
}
@keyframes rescueRouteFlow {
  to { stroke-dashoffset: -32; }
}
@keyframes aedBeacon {
  from { opacity: .9; transform: scale(.55); }
  to { opacity: 0; transform: scale(1.25); }
}
/* #endif */
</style>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #F5F5F7; color: #1C1C1E; }
.page-scroll { min-height: 100vh; padding: 0 24rpx; box-sizing: border-box; }

.rescue-overview { margin: 0 -24rpx; padding: 36rpx 32rpx 32rpx; background: #F5F5F7; }
.status-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 24rpx; }
.status-copy { min-width: 0; flex: 1; }
.status-kicker { display: flex; align-items: center; color: #68798B; font-size: 22rpx; font-weight: 600; }
.status-line { display: block; margin-top: 10rpx; color: #1C1C1E; font-size: 42rpx; font-weight: 750; letter-spacing: -1.5rpx; }
.status-desc { display: block; margin-top: 9rpx; color: #636366; font-size: 25rpx; line-height: 1.45; }
.status-seal { flex: none; padding: 9rpx 14rpx; border-radius: 11rpx; font-size: 22rpx; font-weight: 650; }
.status-context { display: flex; min-width: 0; align-items: center; gap: 8rpx; margin-top: 24rpx; overflow: hidden; color: #8E8E93; font-size: 21rpx; white-space: nowrap; }
.status-context text:last-child { overflow: hidden; text-overflow: ellipsis; }
.status-context-separator { color: #C7C7CC; }

.route-card, .arrival-card, .matching-board, .panel, .current-action, .waiting-card, .cancel-card, .feedback-card { margin: 20rpx 0 0; border: 1rpx solid rgba(60,60,67,.10); border-radius: 24rpx; background: #FFFFFF; box-shadow: 0 1rpx 2rpx rgba(0,0,0,.03), 0 12rpx 34rpx rgba(31,45,61,.05); }
.route-card { overflow: hidden; border-radius: 28rpx; animation: routeSurfaceReveal 320ms cubic-bezier(.22,.78,.24,1) both; }
.arrival-card { display: flex; min-height: 96rpx; align-items: center; gap: 16rpx; padding: 0 22rpx; }
.matching-board { display: flex; min-height: 250rpx; flex-direction: column; align-items: center; justify-content: center; }
.matching-signal { display: flex; height: 46rpx; align-items: center; gap: 8rpx; }
.matching-signal view { width: 7rpx; height: 20rpx; border-radius: 6rpx; background: #007AFF; animation: signalLevel 1.15s cubic-bezier(.4,0,.2,1) infinite; }
.matching-signal view:nth-child(2) { height: 38rpx; animation-delay: .12s; }
.matching-signal view:nth-child(3) { height: 27rpx; animation-delay: .24s; }
.matching-board-title { margin-top: 18rpx; color: #1C1C1E; font-size: 29rpx; font-weight: 700; }
.matching-board-desc { margin-top: 7rpx; color: #636366; font-size: 23rpx; }
.matching-board-live { display: flex; align-items: center; margin-top: 18rpx; color: #8E8E93; font-size: 21rpx; }
@keyframes signalLevel { 0%,100% { opacity: .42; transform: scaleY(.52); } 45% { opacity: 1; transform: scaleY(1); } }
.arrival-copy { min-width: 0; flex: 1; }
.arrival-title, .arrival-place, .arrival-duration { display: block; }
.arrival-title { color: #1C1C1E; font-size: 26rpx; font-weight: 650; }
.arrival-place { margin-top: 5rpx; overflow: hidden; color: #8E8E93; font-size: 22rpx; text-overflow: ellipsis; white-space: nowrap; }
.arrival-duration { margin-top: 7rpx; color: #248A5A; font-size: 23rpx; font-weight: 650; }
.route-map-wrap { position: relative; height: 610rpx; overflow: hidden; background: #E6EDF3; }
.route-card .rescue-map, .route-card .map-unavailable { width: 100%; height: 610rpx; margin: 0; border-radius: 0; }
.route-card .map-unavailable { position: absolute; inset: 0; z-index: 4; }
.tracking-chip { position: absolute; z-index: 500; top: 22rpx; left: 22rpx; display: flex; align-items: center; padding: 12rpx 18rpx; border: 1rpx solid rgba(60,60,67,.10); border-radius: 999rpx; background: rgba(255,255,255,.94); color: #33485C; font-size: 22rpx; font-weight: 650; box-shadow: 0 8rpx 24rpx rgba(35,56,75,.10); backdrop-filter: saturate(150%) blur(18px); }
.route-sheet { position: relative; z-index: 2; background: #FFFFFF; animation: routeSheetReveal 360ms cubic-bezier(.22,.78,.24,1) 90ms both; }
.route-telemetry { display: grid; grid-template-columns: 1fr 1rpx 1fr; align-items: center; gap: 24rpx; padding: 24rpx 28rpx; border-bottom: 1rpx solid rgba(60,60,67,.12); background: #FFFFFF; }
.telemetry-value, .telemetry-label { display: block; }
.telemetry-value { color: #1C1C1E; font-size: 35rpx; font-weight: 720; letter-spacing: -1rpx; }
.telemetry-distance { color: #1677FF; }
.telemetry-label { margin-top: 5rpx; color: #7D8995; font-size: 21rpx; }
.telemetry-divider { height: 42rpx; background: #DCE4EB; }
.route-summary { display: grid; grid-template-columns: minmax(0,1fr) 66rpx minmax(0,1fr); align-items: center; gap: 10rpx; padding: 24rpx 28rpx 28rpx; background: #FFFFFF; }
.route-place { display: flex; min-width: 0; align-items: center; gap: 11rpx; }
.place-mark { display: flex; width: 36rpx; height: 36rpx; flex: none; box-sizing: border-box; align-items: center; justify-content: center; border-radius: 11rpx; }
.aed-mark { background: #E5F5EE; color: #197C59; font-size: 13rpx; font-weight: 900; }
.scene-mark { border: 8rpx solid #C93D46; border-radius: 50%; background: #FFFFFF; }
.place-copy { min-width: 0; flex: 1; }
.place-label, .place-value { display: block; }
.place-label { color: #8795A6; font-size: 20rpx; }
.place-value { margin-top: 4rpx; overflow: hidden; color: #26323E; font-size: 24rpx; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.route-connector { position: relative; height: 2rpx; overflow: hidden; background: #D7E1EA; }
.route-flow { position: absolute; width: 22rpx; height: 2rpx; background: #2E6DD1; animation: routeFlow 1.7s linear infinite; }

.current-action { position: sticky; bottom: calc(12rpx + env(safe-area-inset-bottom)); z-index: 40; display: flex; align-items: center; gap: 18rpx; padding: 18rpx; background: rgba(255,255,255,.94); box-shadow: 0 8rpx 26rpx rgba(0,0,0,.12); backdrop-filter: blur(18px); }
.current-action-copy { min-width: 0; flex: 1; }
.current-action-label, .current-action-title { display: block; }
.current-action-label { color: #728296; font-size: 18rpx; }
.current-action-title { margin-top: 5rpx; color: #2B4159; font-size: 22rpx; font-weight: 700; line-height: 1.4; }
.current-action-button { display: flex; min-width: 220rpx; min-height: 70rpx; box-sizing: border-box; align-items: center; justify-content: center; padding: 0 18rpx; border-radius: 15rpx; background: #007AFF; color: #FFFFFF; text-align: center; font-size: 21rpx; font-weight: 650; transition: transform .15s ease, opacity .15s ease; }
.current-action-button:active { transform: scale(.98); }
.current-action-button.busy { opacity: .55; }

.quick-actions { margin-top: 20rpx; padding: 0 22rpx; border-radius: 20rpx; background: #FFFFFF; }
.quick-action { display: flex; min-height: 88rpx; box-sizing: border-box; align-items: center; gap: 13rpx; border-bottom: 1rpx solid rgba(60,60,67,.14); color: #007AFF; font-size: 21rpx; font-weight: 550; transition: opacity .14s ease; }
.quick-action:last-child { border-bottom: 0; }
.quick-action text:nth-child(2) { min-width: 0; flex: 1; }
.quick-action:active { opacity: .52; }
.quick-action.emergency { color: #FF3B30; }
.row-chevron { color: #C7C7CC; font-size: 34rpx; font-weight: 300; }
.quick-action.disabled { opacity: .38; }

.panel { padding: 22rpx; }
.panel-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 20rpx; }
.panel-title { color: #1C1C1E; font-size: 28rpx; font-weight: 700; }
.participant-row { display: flex; min-height: 82rpx; align-items: center; gap: 18rpx; border-top: 1rpx solid rgba(60,60,67,.14); }
.participant-row-first { margin-top: 13rpx; }
.participant-label { color: #636366; font-size: 23rpx; }
.participant-value { min-width: 0; flex: 1; overflow: hidden; color: #1C1C1E; text-align: right; font-size: 23rpx; font-weight: 550; text-overflow: ellipsis; white-space: nowrap; }

.distance-readout { flex: none; color: #248A5A; font-size: 20rpx; font-weight: 600; }
.aed-resource { display: flex; align-items: center; gap: 14rpx; margin-top: 22rpx; }
.aed-copy { min-width: 0; flex: 1; }
.aed-name, .aed-meta { display: block; }
.aed-name { color: #31475E; font-size: 24rpx; font-weight: 760; }
.aed-meta { margin-top: 4rpx; overflow: hidden; color: #7D8C9D; font-size: 19rpx; text-overflow: ellipsis; white-space: nowrap; }

.waiting-card { display: flex; align-items: center; gap: 18rpx; padding: 22rpx 24rpx; }
.waiting-pulse { width: 16rpx; height: 16rpx; border-radius: 50%; background: #2E6DD1; box-shadow: 0 0 0 9rpx rgba(46,109,209,.1); animation: signalPulse 2.4s ease-out infinite; }
.waiting-title, .waiting-desc { display: block; }
.waiting-title { color: #31475E; font-size: 23rpx; font-weight: 760; }
.waiting-desc { margin-top: 5rpx; color: #8190A0; font-size: 19rpx; }

.detail-card { padding: 22rpx; }
.detail-list { margin-top: 14rpx; }
.detail-row { display: grid; grid-template-columns: 132rpx minmax(0,1fr); align-items: start; gap: 18rpx; padding: 20rpx 0; border-bottom: 1rpx solid rgba(60,60,67,.14); }
.detail-row:last-child { border-bottom: 0; }
.detail-row-block { display: block; }
.detail-label, .detail-value { display: block; font-size: 24rpx; }
.detail-label { color: #7C8B9C; }
.detail-value { min-width: 0; color: #31475E; text-align: right; font-weight: 650; line-height: 1.55; }
.urgency-chip { padding: 7rpx 12rpx; border-radius: 10rpx; font-size: 19rpx; }
.urgency-critical { background: #F9EAEC; color: #B52B36; }
.urgency-high { background: #FFF3E3; color: #A86B1F; }
.urgency-medium { background: #EAF2FC; color: #245FAF; }
.symptom-list { display: flex; flex-wrap: wrap; gap: 10rpx; margin-top: 12rpx; }
.symptom-chip { padding: 7rpx 11rpx; border-radius: 9rpx; background: rgba(118,118,128,.10); color: #3A3A3C; font-size: 20rpx; }
.description { display: block; margin-top: 10rpx; color: #354B62; font-size: 25rpx; line-height: 1.65; }
.scene-section { margin-top: 10rpx; padding-top: 20rpx; border-top: 1rpx solid #EDF1F4; }
.scene-gallery { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 10rpx; margin-top: 12rpx; }
.scene-image { width: 100%; height: 160rpx; border-radius: 12rpx; background: #EDF2F6; }

.cancel-card { display: flex; align-items: center; justify-content: space-between; gap: 18rpx; padding: 20rpx 22rpx; }
.cancel-desc { color: #718195; font-size: 20rpx; }
.cancel-button { flex: none; padding: 8rpx 14rpx; border: 1rpx solid #E0BFC2; border-radius: 999rpx; color: #B8323C; font-size: 19rpx; }
.feedback-card { padding: 24rpx; }
.feedback-head { display: flex; align-items: center; justify-content: space-between; }
.feedback-title { color: #2B4159; font-size: 26rpx; font-weight: 760; }
.feedback-stars { display: flex; gap: 8rpx; margin-top: 16rpx; }
.feedback-head .feedback-stars { margin-top: 0; }
.feedback-star { color: #D8E0E8; font-size: 42rpx; line-height: 1; }
.feedback-star.star-filled { color: #E4A126; }
.feedback-done { color: #23956A; font-size: 20rpx; font-weight: 700; }
.feedback-comment { display: block; margin-top: 14rpx; color: #52667A; font-size: 21rpx; line-height: 1.6; }
.feedback-input { box-sizing: border-box; width: 100%; min-height: 112rpx; margin-top: 16rpx; padding: 15rpx; border: 1rpx solid #DCE5ED; border-radius: 12rpx; background: #F7F9FB; color: #334A61; font-size: 21rpx; }
.feedback-submit { margin-top: 16rpx; padding: 15rpx; border-radius: 13rpx; background: #2E6DD1; color: #FFFFFF; text-align: center; font-size: 22rpx; font-weight: 730; }
.feedback-submit-busy { opacity: .55; }
.bottom-space { height: calc(28rpx + env(safe-area-inset-bottom)); }

.state-card { min-height: 72vh; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40rpx; text-align: center; }
.state-title { margin-top: 22rpx; color: #2A4159; font-size: 28rpx; font-weight: 750; }
.state-action { margin-top: 22rpx; padding: 14rpx 24rpx; border-radius: 13rpx; background: #2E6DD1; color: #FFFFFF; font-size: 21rpx; font-weight: 700; }

.detail-skeleton { padding-top: 24rpx; }
.skeleton-heading { display: flex; align-items: flex-start; justify-content: space-between; padding: 10rpx 4rpx 22rpx; }
.skeleton-title { width: 220rpx; height: 34rpx; border-radius: 999rpx; }
.skeleton-text { width: 360rpx; height: 18rpx; margin-top: 15rpx; border-radius: 999rpx; }
.skeleton-eta { width: 92rpx; height: 54rpx; border-radius: 14rpx; }
.skeleton-lifeline { height: 4rpx; margin: 18rpx 20rpx 34rpx; border-radius: 999rpx; }
.skeleton-map { height: 320rpx; border-radius: 24rpx; }
.skeleton-action { height: 104rpx; margin-top: 18rpx; border-radius: 20rpx; }
.skeleton-caption { padding: 18rpx 0; color: #8593A3; text-align: center; font-size: 19rpx; }

@keyframes signalPulse { 0% { box-shadow: 0 0 0 0 rgba(35,149,106,.24); } 65%,100% { box-shadow: 0 0 0 10rpx rgba(35,149,106,0); } }
@keyframes routeFlow { from { transform: translateX(-22rpx); } to { transform: translateX(54rpx); } }
@keyframes routeSurfaceReveal { from { opacity: .72; transform: translateY(14rpx); } to { opacity: 1; transform: none; } }
@keyframes routeSheetReveal { from { opacity: 0; transform: translateY(28rpx) scale(.985); } to { opacity: 1; transform: none; } }
@keyframes overviewBreath { 0%,100% { opacity: .65; transform: scale(.96); } 50% { opacity: 1; transform: scale(1.05); } }

@media (prefers-reduced-motion: reduce) {
  .waiting-pulse, .route-flow, .route-card, .route-sheet { animation: none; }
  .quick-action, .current-action-button { transition: none; }
}
</style>
