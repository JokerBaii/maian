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
              <view class="status-line">
                <view class="signal-dot"></view>
                <text>{{ statusMeta.label }}</text>
              </view>
              <text class="status-desc">{{ statusMeta.description }}</text>
            </view>
            <view class="status-seal" :class="`urgency-${rescueCall.urgency.toLowerCase()}`">
              <text>{{ urgencyLabel }}</text>
            </view>
          </view>

          <view class="lifeline" :class="{ 'lifeline-complete': statusMeta.step === 3 }">
            <view class="lifeline-track"></view>
            <view class="lifeline-fill" :style="{ width: `${lifelinePercent}%` }">
              <view class="lifeline-pulse"></view>
            </view>
            <view
              v-for="(step, index) in progressSteps"
              :key="step"
              class="lifeline-step"
              :class="{ active: index <= statusMeta.step }"
              :style="{ left: `${index * (100 / (progressSteps.length - 1))}%` }"
            >
              <view class="lifeline-node"></view>
              <text>{{ step }}</text>
            </view>
          </view>
        </view>

        <view v-if="isAwaitingMatch" class="matching-board motion-enter motion-enter-delay-1">
          <view class="matching-signal" aria-hidden="true">
            <view></view><view></view><view></view><view></view><view></view>
          </view>
          <text class="matching-board-title">呼救已送达</text>
          <text class="matching-board-desc">正在联系附近救援人员与 AED</text>
          <view class="matching-board-live"><view></view><text>救援进展实时更新</text></view>
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
            :scale="15"
            show-location
          />
          <!-- #endif -->
          <view v-if="rescueCall.matchedAed" class="tracking-chip">
            <view class="signal-dot"></view>
            <text>{{ trackingLabel }}</text>
          </view>
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
            <view class="telemetry-progress"><view :style="{ width: `${routeVisualPercent}%` }"></view></view>
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

        <view v-else class="arrival-card motion-enter motion-enter-delay-1">
          <view class="arrival-mark">
            <app-icon name="checkmarkempty" :size="25" color="#FFFFFF" />
          </view>
          <view class="arrival-copy">
            <text class="arrival-title">已到达救援现场</text>
            <text class="arrival-place">{{ rescueCall.address }}</text>
          </view>
          <view class="arrival-state">{{ rescueCall.status === 'COMPLETED' ? '已完成' : '现场中' }}</view>
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

        <view class="quick-actions motion-enter motion-enter-delay-3" :class="{ 'quick-actions-two': (!isResponder || hasReachedScene) && hasCounterpartPhone, 'quick-actions-one': !hasCounterpartPhone }">
          <view v-if="hasCounterpartPhone" class="quick-action" @tap="callCounterpart">
            <app-icon name="phone-filled" :size="18" color="#2E6DD1" />
            <text>{{ isResponder ? '联系求救者' : '联系施救者' }}</text>
          </view>
          <view v-if="isResponder && !hasReachedScene" class="quick-action" @tap="openRescueLocation">
            <app-icon name="navigate-filled" :size="18" color="#2E6DD1" />
            <text>现场导航</text>
          </view>
          <view class="quick-action emergency" @tap="callEmergency">
            <app-icon name="phone-filled" :size="18" color="#C93D46" />
            <text>拨打 120</text>
          </view>
        </view>

        <view v-if="counterpart" class="panel coordination-panel">
          <view class="panel-head">
            <text class="panel-title">救援协作</text>
            <view class="sync-chip"><view class="signal-dot"></view><text>协同中</text></view>
          </view>
          <view class="participant-pair">
            <view class="participant-person">
              <app-icon-tile name="author" tone="blue" />
              <view class="participant-copy">
                <text class="participant-role">你</text>
                <text class="participant-name">{{ isResponder ? '现场施救者' : '求救者' }}</text>
              </view>
            </view>
            <view class="pair-connector"><view></view><text>实时协作</text><view></view></view>
            <view class="participant-person participant-person-end">
              <app-icon-tile name="author" tone="green" />
              <view class="participant-copy">
                <text class="participant-role">{{ counterpart?.displayName || '待响应' }}</text>
                <text class="participant-name">{{ isResponder ? '求救者' : '现场施救者' }}</text>
              </view>
            </view>
          </view>
        </view>

        <view v-if="rescueCall.matchedAed && hasReachedScene" class="panel aed-panel">
          <view class="panel-head">
            <text class="panel-title">救援 AED</text>
            <text class="distance-readout">{{ aedDistanceLabel }}</text>
          </view>
          <view class="aed-resource">
            <app-icon-tile :name="rescueCall.matchedAed.type === 'MOBILE' ? 'mobile-device' : 'fixed-device'" tone="green" />
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
            <view class="urgency-chip" :class="`urgency-${rescueCall.urgency.toLowerCase()}`">
              {{ urgencyLabel }}
            </view>
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
// #ifdef H5
import 'leaflet/dist/leaflet.css'
// #endif
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import { addBaseTileLayer } from '@/common/mapTiles'
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
import { getCurrentGcj02Location, isDemoMode, openMapNavigation } from '@/utils/location'
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
const routeProgress = ref(0.18)
let routeAnimationFrame: number | null = null
let routeAnimationStartedAt = 0
let routeMovingMarker: any = null
let routeCoordinates: [number, number][] = []

const feedbackRating = ref(0)
const feedbackComment = ref('')
const feedbackSubmitting = ref(false)
const feedbackSubmitted = ref<RescueFeedbackResponse | null>(null)

/** 完成后展示评价卡片；呼救方对已完成的救援只能评价一次。 */
const feedbackCard = computed(() => !isResponder.value && rescueCall.value?.status === 'COMPLETED')

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

const progressSteps = ['求助', '响应', '处置', '完成']
const statusMeta = computed(() => rescueStatusPresentation(rescueCall.value?.status))

const isEnRoute = computed(() => ['EN_ROUTE_TO_AED', 'EN_ROUTE_TO_REQUESTER'].includes(rescueCall.value?.status || ''))
const hasReachedScene = computed(() => ['ARRIVED', 'RESCUING', 'PENDING_CONFIRMATION', 'COMPLETED'].includes(rescueCall.value?.status || ''))
const isAwaitingMatch = computed(() => ['PENDING', 'MATCHING'].includes(rescueCall.value?.status || '') && !rescueCall.value?.matchedAed)
const lifelinePercent = computed(() => Math.max(0, Math.min(100, statusMeta.value.step * 33.333)))
const routeVisualPercent = computed(() => {
  const status = rescueCall.value?.status || ''
  if (['ARRIVED', 'RESCUING', 'PENDING_CONFIRMATION', 'COMPLETED'].includes(status)) return 100
  if (isEnRoute.value) return Math.round(routeProgress.value * 100)
  return 8
})
const trackingLabel = computed(() => {
  if (hasReachedScene.value) return '已到达现场'
  if (rescueCall.value?.liveTracking) return '实时驰援轨迹'
  if (isDemoMode && isEnRoute.value) return '演示驰援轨迹'
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
    // #ifdef H5
    if (hasReachedScene.value || isAwaitingMatch.value) destroyMap()
    else await initMap()
    // #endif
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

    stopRouteAnimation()
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
      routeCoordinates = buildRescueRoute(routeStart, routeEnd)
      Leaflet.polyline(routeCoordinates, {
        color: '#AFC2D5', weight: 8, opacity: 0.78, lineCap: 'round'
      }).addTo(mapDataLayer)
      Leaflet.polyline(routeCoordinates, hasReachedScene.value
        ? { className: 'rescue-route-complete', color: '#23956A', weight: 5, opacity: .9, lineCap: 'round' }
        : {
            className: 'rescue-route-flow', color: '#2676E5', weight: 5,
            opacity: 1, dashArray: '7 14', lineCap: 'round'
          }
      ).addTo(mapDataLayer)
      const displayedProgress = hasReachedScene.value ? 1 : routeProgress.value
      routeMovingMarker = Leaflet.marker(positionAlongRoute(routeCoordinates, displayedProgress), { icon: deviceMarker })
        .addTo(mapDataLayer)
      mapInstance.fitBounds(
        routeCoordinates,
        { padding: [40, 40], maxZoom: 17 }
      )
      if (isDemoMode && isEnRoute.value && !live) startRouteAnimation()
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
  stopRouteAnimation()
  renderedGeometryKey = ''
  mapDataLayer = null
  routeMovingMarker = null
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }
}
// #endif

function buildRescueRoute(start: [number, number], end: [number, number]): [number, number][] {
  const latitudeDelta = end[0] - start[0]
  const longitudeDelta = end[1] - start[1]
  const bendLatitude = -longitudeDelta * 0.12
  const bendLongitude = latitudeDelta * 0.12
  return Array.from({ length: 18 }, (_, index) => {
    const progress = index / 17
    const curve = Math.sin(progress * Math.PI)
    return [
      start[0] + latitudeDelta * progress + bendLatitude * curve,
      start[1] + longitudeDelta * progress + bendLongitude * curve
    ]
  })
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

function startRouteAnimation() {
  stopRouteAnimation()
  routeAnimationStartedAt = performance.now() - routeProgress.value * 26000
  const animate = (now: number) => {
    const raw = ((now - routeAnimationStartedAt) % 30000) / 26000
    routeProgress.value = raw > 1 ? 0.94 : 0.12 + raw * 0.82
    routeMovingMarker?.setLatLng(positionAlongRoute(routeCoordinates, routeProgress.value))
    routeAnimationFrame = requestAnimationFrame(animate)
  }
  routeAnimationFrame = requestAnimationFrame(animate)
}

function stopRouteAnimation() {
  if (routeAnimationFrame != null) cancelAnimationFrame(routeAnimationFrame)
  routeAnimationFrame = null
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
    // 位置同步失败不阻断救援主流程，下一轮轮询会自动重试。
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
  stopRouteAnimation()
  disconnectEvents?.()
  // #ifdef H5
  destroyMap()
  // #endif
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
.page { min-height: 100vh; background: #F3F7FA; color: #20364D; }
.page-scroll { min-height: 100vh; padding: 0 24rpx; box-sizing: border-box; }

.rescue-overview { position: relative; margin: 0 -24rpx; padding: 34rpx 28rpx 52rpx; overflow: hidden; border-bottom: 1rpx solid rgba(60,60,67,.12); border-radius: 0; background: #F8F8FA; }
.rescue-overview::before { display: none; }
.status-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 24rpx; }
.status-copy { min-width: 0; flex: 1; }
.status-line { position: relative; z-index: 1; display: flex; align-items: center; gap: 13rpx; color: #183755; font-size: 41rpx; font-weight: 820; letter-spacing: -1rpx; }
.signal-dot { width: 13rpx; height: 13rpx; flex: none; border-radius: 50%; background: #23956A; box-shadow: 0 0 0 0 rgba(35,149,106,.25); animation: signalPulse 2s ease-out infinite; }
.status-desc { position: relative; z-index: 1; display: block; margin-top: 10rpx; color: #637B93; font-size: 23rpx; line-height: 1.5; }
.status-seal { position: relative; z-index: 1; flex: none; padding: 10rpx 16rpx; border-radius: 13rpx; font-size: 20rpx; font-weight: 800; }
.lifeline { position: relative; z-index: 1; height: 76rpx; margin: 33rpx 18rpx 0; }
.lifeline-track, .lifeline-fill { position: absolute; top: 10rpx; left: 0; height: 3rpx; border-radius: 999rpx; }
.lifeline-track { width: 100%; background: #DCE5ED; }
.lifeline-fill { background: #2E6DD1; transition: width .6s cubic-bezier(.22,.8,.24,1); }
.lifeline-pulse { position: absolute; top: -7rpx; right: -8rpx; width: 17rpx; height: 17rpx; border-radius: 50%; background: #2E6DD1; box-shadow: 0 0 0 8rpx rgba(46,109,209,.11); }
.lifeline-complete .lifeline-fill, .lifeline-complete .lifeline-pulse { background: #23956A; }
.lifeline-step { position: absolute; top: 0; display: flex; width: 92rpx; flex-direction: column; align-items: center; gap: 12rpx; color: #9AA7B5; font-size: 18rpx; transform: translateX(-50%); }
.lifeline-step:first-of-type { align-items: flex-start; transform: none; }
.lifeline-step:last-of-type { align-items: flex-end; transform: translateX(-100%); }
.lifeline-node { width: 20rpx; height: 20rpx; box-sizing: border-box; border: 4rpx solid #DCE5ED; border-radius: 50%; background: #F3F7FA; transition: all .35s ease; }
.lifeline-step.active { color: #49627D; }
.lifeline-step.active .lifeline-node { border-color: #2E6DD1; background: #2E6DD1; }

.route-card, .arrival-card, .matching-board, .panel, .current-action, .waiting-card, .cancel-card, .feedback-card { margin: 0 0 18rpx; border: 1rpx solid #E1E9F0; border-radius: 24rpx; background: #FFFFFF; box-shadow: 0 8rpx 22rpx rgba(42,67,92,.045); }
.route-card { position: relative; z-index: 3; margin-top: -24rpx; overflow: hidden; border-color: rgba(255,255,255,.9); box-shadow: 0 18rpx 44rpx rgba(35,65,98,.14); }
.arrival-card { position: relative; z-index: 3; display: flex; min-height: 154rpx; align-items: center; gap: 20rpx; margin-top: -24rpx; padding: 28rpx; overflow: hidden; border: 0; background: #E8F5EE; box-shadow: none; }
.arrival-card::after { display: none; }
.arrival-card > * { position: relative; z-index: 1; }
.matching-board { position: relative; z-index: 3; display: flex; min-height: 330rpx; flex-direction: column; align-items: center; justify-content: center; margin-top: -24rpx; overflow: hidden; border: 0; background: #EDF4FC; }
.matching-signal { display: flex; height: 68rpx; align-items: center; gap: 10rpx; padding: 0 24rpx; border-radius: 18rpx; background: rgba(255,255,255,.72); }
.matching-signal view { width: 7rpx; height: 18rpx; border-radius: 6rpx; background: #2E6DD1; animation: signalLevel 1.15s cubic-bezier(.4,0,.2,1) infinite; }
.matching-signal view:nth-child(2) { height: 34rpx; animation-delay: .12s; }
.matching-signal view:nth-child(3) { height: 50rpx; background: #C72C38; animation-delay: .24s; }
.matching-signal view:nth-child(4) { height: 30rpx; animation-delay: .36s; }
.matching-signal view:nth-child(5) { animation-delay: .48s; }
.matching-board-title { margin-top: 25rpx; color: #1F3B57; font-size: 34rpx; font-weight: 800; letter-spacing: -.5rpx; }.matching-board-desc { margin-top: 8rpx; color: #64778B; font-size: 21rpx; }
.matching-board-live { display: flex; align-items: center; gap: 8rpx; margin-top: 24rpx; color: #47705E; font-size: 18rpx; font-weight: 650; }
.matching-board-live view { width: 9rpx; height: 9rpx; border-radius: 50%; background: #248A5A; box-shadow: 0 0 0 0 rgba(36,138,90,.22); animation: signalPulse 2s ease-out infinite; }
@keyframes signalLevel { 0%,100% { opacity: .42; transform: scaleY(.52); } 45% { opacity: 1; transform: scaleY(1); } }
.arrival-mark { display: flex; width: 76rpx; height: 76rpx; flex: none; align-items: center; justify-content: center; border-radius: 50%; background: #248A5A; box-shadow: 0 0 0 12rpx rgba(36,138,90,.09); animation: arrivalConfirm 420ms cubic-bezier(.2,.75,.2,1) both; }
.arrival-copy { min-width: 0; flex: 1; }
.arrival-title, .arrival-place { display: block; }
.arrival-title { color: #173F31; font-size: 31rpx; font-weight: 800; }
.arrival-place { margin-top: 6rpx; overflow: hidden; color: #587268; font-size: 21rpx; text-overflow: ellipsis; white-space: nowrap; }
.arrival-state { flex: none; padding: 7rpx 11rpx; border-radius: 10rpx; background: #E9F5F0; color: #197C59; font-size: 18rpx; font-weight: 750; }
@keyframes arrivalConfirm { from { opacity: 0; transform: scale(.82); } to { opacity: 1; transform: none; } }
.route-map-wrap { position: relative; height: 390rpx; overflow: hidden; background: #E6EDF3; }
.route-card .rescue-map, .route-card .map-unavailable { width: 100%; height: 390rpx; margin: 0; border-radius: 0; }
.route-card .map-unavailable { position: absolute; inset: 0; z-index: 4; }
.tracking-chip { position: absolute; z-index: 500; top: 18rpx; left: 18rpx; display: flex; align-items: center; gap: 9rpx; padding: 10rpx 15rpx; border: 1rpx solid rgba(255,255,255,.8); border-radius: 999rpx; background: rgba(255,255,255,.93); color: #49627D; font-size: 19rpx; font-weight: 700; backdrop-filter: blur(10px); }
.route-telemetry { position: absolute; z-index: 500; left: 18rpx; right: 88rpx; bottom: 18rpx; display: grid; grid-template-columns: 1fr 1rpx 1fr; align-items: center; gap: 16rpx; padding: 15rpx 18rpx 18rpx; overflow: hidden; border: 1rpx solid rgba(255,255,255,.8); border-radius: 18rpx; background: rgba(255,255,255,.94); box-shadow: 0 9rpx 25rpx rgba(30,56,84,.16); backdrop-filter: blur(12px); }
.route-telemetry-wide { right: 18rpx; }
.telemetry-value, .telemetry-label { display: block; }
.telemetry-value { color: #1C3B5C; font-size: 27rpx; font-weight: 850; }
.telemetry-distance { color: #167B58; }
.telemetry-label { margin-top: 2rpx; color: #7F8D9D; font-size: 16rpx; }
.telemetry-divider { height: 42rpx; background: #DCE4EB; }
.telemetry-progress { position: absolute; left: 0; right: 0; bottom: 0; height: 5rpx; background: #DDE7EF; }
.telemetry-progress view { height: 100%; border-radius: 999rpx; background: linear-gradient(90deg,#23956A,#2E6DD1); transition: width .35s linear; }
.route-summary { display: grid; grid-template-columns: minmax(0,1fr) 54rpx minmax(0,1fr); align-items: center; gap: 8rpx; padding: 20rpx 22rpx; }
.route-place { display: flex; min-width: 0; align-items: center; gap: 11rpx; }
.place-mark { display: flex; width: 36rpx; height: 36rpx; flex: none; box-sizing: border-box; align-items: center; justify-content: center; border-radius: 11rpx; }
.aed-mark { background: #E5F5EE; color: #197C59; font-size: 13rpx; font-weight: 900; }
.scene-mark { border: 8rpx solid #C93D46; border-radius: 50%; background: #FFFFFF; }
.place-copy { min-width: 0; flex: 1; }
.place-label, .place-value { display: block; }
.place-label { color: #8795A6; font-size: 17rpx; }
.place-value { margin-top: 3rpx; overflow: hidden; color: #31475E; font-size: 20rpx; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.route-connector { position: relative; height: 2rpx; overflow: hidden; background: #D7E1EA; }
.route-flow { position: absolute; width: 22rpx; height: 2rpx; background: #2E6DD1; animation: routeFlow 1.7s linear infinite; }

.current-action { display: flex; align-items: center; gap: 18rpx; padding: 22rpx; border-color: #C8DCF5; background: #FFFFFF; box-shadow: none; }
.current-action-copy { min-width: 0; flex: 1; }
.current-action-label, .current-action-title { display: block; }
.current-action-label { color: #728296; font-size: 18rpx; }
.current-action-title { margin-top: 5rpx; color: #2B4159; font-size: 22rpx; font-weight: 700; line-height: 1.4; }
.current-action-button { display: flex; min-width: 238rpx; min-height: 68rpx; box-sizing: border-box; align-items: center; justify-content: center; padding: 0 18rpx; border-radius: 16rpx; background: #2E6DD1; color: #FFFFFF; text-align: center; font-size: 21rpx; font-weight: 760; box-shadow: 0 8rpx 18rpx rgba(46,109,209,.18); transition: transform .15s ease, opacity .15s ease; }
.current-action-button:active { transform: scale(.98); }
.current-action-button.busy { opacity: .55; }

.quick-actions { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 12rpx; margin: 0 0 18rpx; }
.quick-actions-two { grid-template-columns: repeat(2,minmax(0,1fr)); }
.quick-actions-one { grid-template-columns: 1fr; }
.quick-action { display: flex; min-width: 0; min-height: 70rpx; box-sizing: border-box; align-items: center; justify-content: center; gap: 9rpx; padding: 0 10rpx; border: 1rpx solid #DEE7EF; border-radius: 18rpx; background: #FFFFFF; color: #4E6279; font-size: 20rpx; font-weight: 700; transition: transform .16s ease, background .16s ease; }
.quick-action:active { transform: scale(.975); background: #F5F8FB; }
.quick-action.emergency { border-color: #F0D4D7; color: #B8323C; }
.quick-action.disabled { opacity: .38; }

.panel { padding: 24rpx; }
.coordination-panel { margin-bottom: 0; border-bottom: 0; border-radius: 24rpx 24rpx 0 0; box-shadow: none; }
.coordination-panel + .aed-panel { border-radius: 0 0 24rpx 24rpx; box-shadow: 0 10rpx 26rpx rgba(42,67,92,.055); }
.panel-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 20rpx; }
.panel-title { display: block; }
.panel-title { color: #263D55; font-size: 28rpx; font-weight: 780; }
.sync-chip { display: flex; align-items: center; gap: 8rpx; padding: 8rpx 12rpx; border-radius: 999rpx; background: #EEF8F4; color: #197C59; font-size: 18rpx; font-weight: 700; }
.participant-pair { display: flex; align-items: center; gap: 12rpx; margin-top: 21rpx; }
.participant-person { display: flex; min-width: 0; flex: 1; align-items: center; }
.participant-person-end { flex-direction: row-reverse; text-align: right; }
.participant-copy { min-width: 0; flex: 1; margin-left: 14rpx; }
.participant-person-end .participant-copy { margin-right: 14rpx; margin-left: 0; }
.participant-role, .participant-name { display: block; }
.participant-role { color: #334B63; font-size: 23rpx; font-weight: 760; }
.participant-name { margin-top: 4rpx; color: #7D8C9D; font-size: 19rpx; font-weight: 400; }
.pair-connector { display: flex; width: 106rpx; flex: none; align-items: center; color: #8190A0; font-size: 15rpx; white-space: nowrap; }
.pair-connector view { height: 1rpx; flex: 1; background: #D8E3EC; }
.pair-connector text { margin: 0 6rpx; }

.distance-readout { flex: none; color: #23956A; font-size: 25rpx; font-weight: 800; }
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

.detail-card { padding: 24rpx; box-shadow: 0 8rpx 22rpx rgba(42,67,92,.045); }
.detail-list { margin-top: 14rpx; }
.detail-row { display: grid; grid-template-columns: 132rpx minmax(0,1fr); align-items: start; gap: 18rpx; padding: 19rpx 0; border-bottom: 1rpx solid #EDF1F4; }
.detail-row:last-child { border-bottom: 0; }
.detail-row-block { display: block; }
.detail-label, .detail-value { display: block; font-size: 21rpx; }
.detail-label { color: #7C8B9C; }
.detail-value { min-width: 0; color: #31475E; text-align: right; font-weight: 650; line-height: 1.55; }
.urgency-chip { padding: 7rpx 12rpx; border-radius: 10rpx; font-size: 19rpx; }
.urgency-critical { background: #F9EAEC; color: #B52B36; }
.urgency-high { background: #FFF3E3; color: #A86B1F; }
.urgency-medium { background: #EAF2FC; color: #245FAF; }
.symptom-list { display: flex; flex-wrap: wrap; gap: 10rpx; margin-top: 12rpx; }
.symptom-chip { padding: 8rpx 12rpx; border-radius: 9rpx; background: #EEF4FA; color: #425A72; font-size: 20rpx; }
.description { display: block; margin-top: 10rpx; color: #354B62; font-size: 22rpx; line-height: 1.7; }
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
@keyframes overviewBreath { 0%,100% { opacity: .65; transform: scale(.96); } 50% { opacity: 1; transform: scale(1.05); } }

@media (prefers-reduced-motion: reduce) {
  .signal-dot, .waiting-pulse, .route-flow { animation: none; }
  .lifeline-fill, .lifeline-node, .quick-action, .current-action-button { transition: none; }
}
</style>
