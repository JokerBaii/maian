<template>
  <view class="page apple-page motion-page-list">
    <view class="response-console motion-enter">
      <view class="console-copy">
        <text class="console-title">{{ presenceOnline ? '可接收附近救援' : '正在确认当前位置' }}</text>
        <text class="console-subtitle">{{ activeTasks.length }} 项进行中 · {{ offerTasks.length }} 项待响应</text>
      </view>
      <view class="online-chip" :class="{ 'online-chip-muted': !presenceOnline }">
        <text>{{ presenceOnline ? '可响应' : '定位中' }}</text>
      </view>
    </view>

    <view class="dispatch-content">
      <view class="golden-window motion-enter motion-enter-delay-1" @tap="callEmergency">
        <app-icon name="phone-filled" :size="18" color="#FF3B30" />
        <text class="golden-title">危急情况请同时拨打 120</text>
        <text class="row-chevron">›</text>
      </view>

    <view v-if="loading && !tasks.length" class="loading-list motion-enter motion-enter-delay-2">
      <view v-for="item in 3" :key="item" class="loading-row">
        <view class="loading-icon motion-skeleton"></view>
        <view class="loading-copy"><view class="loading-title motion-skeleton"></view><view class="loading-text motion-skeleton"></view></view>
      </view>
    </view>

    <template v-else>
      <view v-if="activeTasks.length" class="section motion-enter motion-enter-delay-2">
        <view class="section-head">
          <text class="section-title">进行中</text>
          <text class="section-count">{{ activeTasks.length }} 项</text>
        </view>

        <view v-for="task in activeTasks" :key="task.id" class="trip-card">
          <view class="trip-head">
            <text class="trip-order-label">{{ task.requester?.displayName || '求救者' }}</text>
            <view class="status-chip">{{ statusLabel(task.status) }}</view>
          </view>

          <view class="participant-row">
            <text class="task-address">{{ task.address || '响应后显示精确位置' }}</text>
            <view v-if="task.requester?.phone" class="phone-action" @tap.stop="callRequester(task)">
              <app-icon name="phone-filled" :size="17" color="#1F63D5" />
            </view>
          </view>

          <view class="route-panel">
            <view v-if="task.matchedAedType === 'FIXED'" class="route-row">
              <view class="route-track">
                <view class="route-node route-node-aed">A</view>
                <view class="route-line"></view>
              </view>
              <view class="route-copy">
                <text class="route-label">取 AED</text>
                <text class="route-value">{{ task.matchedAed?.name || '已匹配固定 AED' }}</text>
              </view>
            </view>
            <view class="route-row">
              <view class="route-track">
                <view class="route-node route-node-scene"></view>
              </view>
              <view class="route-copy">
              <text class="route-label">救援现场</text>
                <text class="route-value">{{ task.address || '响应后显示精确地址' }}</text>
              </view>
            </view>
          </view>

          <text class="task-guidance">{{ actionGuidance(task) }}</text>

          <view class="trip-actions">
            <view class="secondary-action" @tap.stop="goDetail(task)">
              <text>详情</text>
            </view>
            <view
              v-if="nextAction(task)"
              class="primary-action"
              :class="{ 'action-busy': busyTaskId === task.id }"
              @tap.stop="progress(task)"
            >
              <text>{{ busyTaskId === task.id ? '同步中…' : nextAction(task)?.label }}</text>
            </view>
            <view v-else class="waiting-action">{{ terminalHint(task) }}</view>
          </view>
        </view>
      </view>

      <view class="section motion-enter motion-enter-delay-3">
        <view class="section-head">
          <text class="section-title">附近</text>
          <text class="section-count section-count-muted">{{ offerTasks.length }} 项</text>
        </view>

        <view v-if="!offerTasks.length" class="empty-card compact-empty">
          <text class="empty-title">暂无新任务</text>
          <text class="empty-desc">附近有新任务时会通知你</text>
        </view>

        <view v-for="task in offerTasks" :key="task.id" class="offer-card">
          <view class="offer-main">
            <view class="offer-symbol" :class="`urgency-${task.urgency.toLowerCase()}`">!</view>
            <view class="offer-copy">
              <view class="offer-head">
                <text class="offer-title">需要急救支援</text>
                <text class="distance-value">{{ formatDistance(task.distanceToRequesterMeters) }}</text>
              </view>
              <text class="offer-privacy">{{ urgencyLabel(task.urgency) }} · {{ task.matchedAedType === 'MOBILE' ? 'AED 随人到场' : '需先取 AED' }} · {{ relativeTime(task.createdAt) }}</text>
            </view>
            <view class="accept-action" :class="{ 'action-busy': busyTaskId === task.id }" @tap="accept(task)">
              <text>{{ busyTaskId === task.id ? '确认中' : '响应' }}</text>
            </view>
          </view>
          <text class="offer-disclosure">响应后显示精确位置与联系方式</text>
        </view>
      </view>
    </template>

    <view class="bottom-safe"></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onHide, onShow } from '@dcloudio/uni-app'
import AppIcon from '@/components/AppIcon.vue'
import {
  acceptRescueTask,
  listResponderTasks,
  performResponderAction,
  updateResponderPresence,
  type ResponderTaskResponse
} from '@/api/rescue'
import { getCurrentGcj02Location } from '@/utils/location'
import { connectRescueEvents } from '@/utils/rescueEvents'
import { rescueStatusLabel, rescueUrgencyLabel, userFacingError } from '@/utils/presentation'

const tasks = ref<ResponderTaskResponse[]>([])
const loading = ref(true)
const presenceOnline = ref(false)
const busyTaskId = ref('')
let refreshTimer: ReturnType<typeof setInterval> | null = null
let disconnectEvents: (() => void) | null = null

const offerTasks = computed(() => tasks.value.filter(task => !task.detailAvailable && task.status === 'MATCHING'))
const activeTasks = computed(() => tasks.value.filter(task => task.detailAvailable))

async function load() {
  if (!tasks.value.length) loading.value = true
  try {
    tasks.value = (await listResponderTasks()).content
  } catch (error: any) {
    uni.showToast({ title: userFacingError(error, '救援任务加载失败'), icon: 'none' })
  } finally {
    loading.value = false
  }

  try {
    const location = await getCurrentGcj02Location()
    await updateResponderPresence({ ...location, available: true })
    presenceOnline.value = true
  } catch {
    presenceOnline.value = false
  }
}

function goDetail(task: ResponderTaskResponse) {
  if (!task.detailAvailable) {
    uni.showToast({ title: '确认响应后可查看精确位置与现场资料', icon: 'none' })
    return
  }
  uni.navigateTo({ url: `/pages/rescue/detail?id=${encodeURIComponent(task.id)}&mode=responder` })
}

async function accept(task: ResponderTaskResponse) {
  if (busyTaskId.value) return
  busyTaskId.value = task.id
  try {
    const accepted = await acceptRescueTask(task.id)
    Object.assign(task, accepted)
    uni.showToast({ title: '响应成功，已开放救援资料', icon: 'success' })
    setTimeout(() => goDetail(task), 350)
  } catch (error: any) {
    uni.showToast({ title: userFacingError(error, '响应失败，请稍后重试'), icon: 'none' })
    await load()
  } finally {
    busyTaskId.value = ''
  }
}

type TaskAction = 'aed-arrival' | 'aed-pickup' | 'requester-arrival' | 'rescue-start' | 'completion-submission' | 'aed-return'

function nextAction(task: ResponderTaskResponse): { action: TaskAction; label: string } | null {
  if (task.status === 'EN_ROUTE_TO_AED') {
    return task.arrivedAtAedAt
      ? { action: 'aed-pickup', label: '已取 AED，赶往现场' }
      : { action: 'aed-arrival', label: '已到达 AED 取用点' }
  }
  if (task.status === 'EN_ROUTE_TO_REQUESTER') return { action: 'requester-arrival', label: '已到达求救者位置' }
  if (task.status === 'ARRIVED') return { action: 'rescue-start', label: '开始现场施救' }
  if (task.status === 'RESCUING') return { action: 'completion-submission', label: '提交完成救援' }
  if (task.status === 'COMPLETED' && task.aedCustodyStatus === 'RETURNING') {
    return { action: 'aed-return', label: '确认已归还 AED' }
  }
  return null
}

async function progress(task: ResponderTaskResponse) {
  const next = nextAction(task)
  if (!next || busyTaskId.value) return
  busyTaskId.value = task.id
  try {
    Object.assign(task, await performResponderAction(task.id, next.action))
    uni.showToast({ title: '救援状态已同步', icon: 'success' })
  } catch (error: any) {
    uni.showToast({ title: userFacingError(error, '救援进展更新失败'), icon: 'none' })
  } finally {
    busyTaskId.value = ''
  }
}

function callRequester(task: ResponderTaskResponse) {
  if (task.requester?.phone) uni.makePhoneCall({ phoneNumber: task.requester.phone })
}

function callEmergency() {
  uni.makePhoneCall({ phoneNumber: '120' })
}

const urgencyLabel = rescueUrgencyLabel
const statusLabel = rescueStatusLabel

function actionGuidance(task: ResponderTaskResponse) {
  if (task.status === 'EN_ROUTE_TO_AED') return task.arrivedAtAedAt ? '请取出 AED 后赶往求救现场' : '请先导航到 AED 取用点'
  if (task.status === 'EN_ROUTE_TO_REQUESTER') return '请按导航赶往求救者位置'
  if (task.status === 'ARRIVED') return '确认环境安全后开始现场施救'
  if (task.status === 'RESCUING') return '救援进行中，必要时同步联系 120'
  if (task.status === 'PENDING_CONFIRMATION') return '已提交完成，等待求救者确认'
  if (task.status === 'COMPLETED' && task.aedCustodyStatus === 'RETURNING') return '救援已完成，请将固定 AED 归还原位'
  return '本次救援任务已结束'
}

function terminalHint(task: ResponderTaskResponse) {
  if (task.status === 'PENDING_CONFIRMATION') return '等待对方确认'
  return '任务已结束'
}

function formatDistance(meters?: number) {
  if (meters == null) return '未知'
  return meters < 1000 ? `${meters}米` : `${(meters / 1000).toFixed(1)}公里`
}

function relativeTime(value: string) {
  const seconds = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 1000))
  if (seconds < 60) return '刚刚发起'
  if (seconds < 3600) return `${Math.floor(seconds / 60)}分钟前`
  return `${Math.floor(seconds / 3600)}小时前`
}

onShow(() => {
  load()
  refreshTimer = setInterval(load, 10000)
  disconnectEvents = connectRescueEvents(() => load())
})

onHide(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  refreshTimer = null
  disconnectEvents?.()
  disconnectEvents = null
})
</script>


<style lang="scss" scoped>
.page { min-height: 100vh; box-sizing: border-box; overflow: visible; padding: 0 24rpx; background: #F5F5F7; color: #1C1C1E; }
.response-console { display: flex; align-items: center; gap: 20rpx; margin: 0 -24rpx; padding: 30rpx 32rpx; background: #F5F5F7; }
.console-copy { min-width: 0; flex: 1; }
.console-title, .console-subtitle { display: block; }
.console-title { color: #1C1C1E; font-size: 30rpx; font-weight: 700; }
.console-subtitle { margin-top: 7rpx; color: #8E8E93; font-size: 23rpx; }
.online-chip { display: flex; flex: none; align-items: center; padding: 9rpx 14rpx; border-radius: 999rpx; background: rgba(0,122,255,.09); color: #007AFF; font-size: 22rpx; font-weight: 650; }
.online-chip-muted { background: #EDF1F5; color: #7C8998; }

.dispatch-content { min-height: 60vh; margin-top: 20rpx; }
.golden-window { display: flex; min-height: 86rpx; box-sizing: border-box; align-items: center; gap: 14rpx; padding: 0 20rpx; border-radius: 20rpx; background: #FFFFFF; color: #FF3B30; }
.golden-title { min-width: 0; flex: 1; font-size: 25rpx; font-weight: 600; }
.row-chevron { color: #C7C7CC; font-size: 34rpx; font-weight: 300; }

.loading-list { margin-top: 18rpx; padding: 8rpx 22rpx; border: 1rpx solid #E1E9F0; border-radius: 22rpx; background: #FFFFFF; }
.loading-row { display: flex; align-items: center; gap: 15rpx; padding: 19rpx 0; border-bottom: 1rpx solid #EEF2F5; }
.loading-row:last-child { border-bottom: 0; }
.loading-icon { width: 56rpx; height: 56rpx; flex: none; border-radius: 18rpx; }
.loading-copy { min-width: 0; flex: 1; }
.loading-title { width: 42%; height: 19rpx; border-radius: 999rpx; }
.loading-text { width: 74%; height: 15rpx; margin-top: 12rpx; border-radius: 999rpx; }

.section { margin-top: 30rpx; }
.section-head { display: flex; align-items: center; justify-content: space-between; padding: 0 8rpx 12rpx; }
.section-title { color: #1C1C1E; font-size: 28rpx; font-weight: 700; }
.section-count { color: #8E8E93; font-size: 22rpx; }
.trip-card, .offer-card, .empty-card { padding: 0 22rpx; border: 1rpx solid rgba(60,60,67,.09); background: #FFFFFF; box-shadow: 0 10rpx 30rpx rgba(31,45,61,.04); }
.trip-card:first-of-type, .offer-card:first-of-type, .empty-card { border-radius: 20rpx 20rpx 0 0; }
.trip-card:last-child, .offer-card:last-child, .empty-card { border-radius: 0 0 20rpx 20rpx; }
.trip-card:only-child, .offer-card:only-child, .empty-card { border-radius: 20rpx; }
.trip-card + .trip-card, .offer-card + .offer-card { border-top: 1rpx solid rgba(60,60,67,.14); }
.trip-head, .offer-head, .trip-actions { display: flex; align-items: center; justify-content: space-between; }
.trip-head { padding-top: 22rpx; }
.trip-order-label { color: #1C1C1E; font-size: 27rpx; font-weight: 650; }
.status-chip { padding: 7rpx 11rpx; border-radius: 10rpx; background: rgba(0,122,255,.10); color: #007AFF; font-size: 21rpx; font-weight: 600; }
.participant-row { display: flex; align-items: center; margin-top: 10rpx; }
.task-address { min-width: 0; flex: 1; overflow: hidden; color: #8E8E93; font-size: 23rpx; text-overflow: ellipsis; white-space: nowrap; }
.phone-action { display: flex; width: 52rpx; height: 52rpx; align-items: center; justify-content: center; border-radius: 50%; background: rgba(0,122,255,.10); }
.route-panel { margin-top: 16rpx; padding: 14rpx 16rpx 0; border-radius: 14rpx; background: rgba(118,118,128,.08); }
.route-row { display: flex; min-height: 65rpx; }
.route-track { display: flex; width: 36rpx; flex-direction: column; align-items: center; }
.route-node { z-index: 1; display: flex; width: 21rpx; height: 21rpx; box-sizing: border-box; align-items: center; justify-content: center; border-radius: 50%; }
.route-node-aed { background: #23956A; color: #FFFFFF; font-size: 12rpx; font-weight: 900; }
.route-node-scene { border: 6rpx solid #C93D46; background: #FFFFFF; }
.route-line { width: 2rpx; flex: 1; background: #C7C7CC; }
.route-copy { min-width: 0; flex: 1; padding-bottom: 15rpx; }
.route-label { display: block; color: #8B98A7; font-size: 20rpx; }
.route-value { display: block; margin-top: 2rpx; overflow: hidden; color: #334A61; font-size: 24rpx; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.task-guidance { display: block; margin-top: 14rpx; color: #636366; font-size: 22rpx; line-height: 1.5; }
.trip-actions { gap: 11rpx; margin-top: 17rpx; }
.secondary-action, .primary-action, .waiting-action { display: flex; min-height: 76rpx; align-items: center; justify-content: center; border-radius: 14rpx; font-size: 24rpx; font-weight: 650; }
.secondary-action { width: 120rpx; background: rgba(118,118,128,.10); color: #007AFF; }
.primary-action { flex: 1; background: #007AFF; color: #FFFFFF; }
.waiting-action { flex: 1; background: #F0F3F6; color: #7E8B9A; }

.urgency-critical { background: #F9EAEC; color: #B52B36; }
.urgency-high { background: #FFF3E3; color: #A86B1F; }
.urgency-medium { background: #EAF2FC; color: #245FAF; }
.offer-main { display: flex; align-items: center; gap: 14rpx; padding: 21rpx 0 13rpx; }
.offer-symbol { display: flex; width: 48rpx; height: 48rpx; flex: none; align-items: center; justify-content: center; border-radius: 50%; font-size: 24rpx; font-weight: 750; }
.offer-copy { min-width: 0; flex: 1; }
.offer-head { gap: 12rpx; }
.distance-value { flex: none; color: #007AFF; font-size: 22rpx; font-weight: 600; }
.offer-title { min-width: 0; flex: 1; color: #1C1C1E; font-size: 26rpx; font-weight: 650; }
.offer-privacy { display: block; margin-top: 6rpx; overflow: hidden; color: #8E8E93; font-size: 21rpx; text-overflow: ellipsis; white-space: nowrap; }
.accept-action { display: flex; min-width: 92rpx; min-height: 62rpx; flex: none; align-items: center; justify-content: center; padding: 0 16rpx; border-radius: 999rpx; background: rgba(0,122,255,.10); color: #007AFF; font-size: 23rpx; font-weight: 650; }
.offer-disclosure { display: block; padding: 0 0 19rpx 62rpx; color: #8E8E93; font-size: 20rpx; }
.action-busy { opacity: .55; }
.empty-card { display: flex; min-height: 140rpx; flex-direction: column; align-items: center; justify-content: center; text-align: center; }
.empty-title { color: #40556C; font-size: 22rpx; font-weight: 740; }
.empty-desc { margin-top: 6rpx; color: #8795A6; font-size: 21rpx; }
.bottom-safe { height: calc(34rpx + env(safe-area-inset-bottom)); }

</style>
