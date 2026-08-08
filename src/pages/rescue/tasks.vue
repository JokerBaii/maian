<template>
  <view class="page apple-page motion-page-list">
    <view class="response-console motion-enter">
      <view class="console-head">
        <view>
          <text class="console-title">{{ presenceOnline ? '保持可响应' : '正在获取位置' }}</text>
        </view>
        <view class="online-chip" :class="{ 'online-chip-muted': !presenceOnline }">
          <view class="online-dot"></view>
          <text>{{ presenceOnline ? '可响应' : '定位中' }}</text>
        </view>
      </view>

      <view class="dispatch-metrics">
        <view class="dispatch-metric">
          <text class="metric-value">{{ activeTasks.length }}</text>
          <text class="metric-label">进行中</text>
        </view>
        <view class="metric-divider"></view>
        <view class="dispatch-metric">
          <text class="metric-value metric-value-alert">{{ offerTasks.length }}</text>
          <text class="metric-label">附近待响应</text>
        </view>
      </view>
    </view>

    <view class="dispatch-content">
      <view class="golden-window motion-enter motion-enter-delay-1">
        <view class="vital-mark"><app-icon name="phone-filled" :size="18" color="#C93D46" /></view>
        <view class="golden-copy">
          <text class="golden-title">危急情况请同时拨打 120</text>
        </view>
        <view class="emergency-call" @tap="callEmergency"><text>120</text></view>
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
          <view>
            <text class="section-title">进行中的救援</text>
          </view>
          <text class="section-count">{{ activeTasks.length }}</text>
        </view>

        <view v-for="task in activeTasks" :key="task.id" class="trip-card">
          <view class="trip-head">
            <view class="trip-order">
              <text class="trip-order-label">{{ task.requester?.displayName || '求救者' }}</text>
            </view>
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

          <view class="task-guidance">
            <app-icon name="navigate-filled" :size="16" color="#147452" />
            <text>{{ actionGuidance(task) }}</text>
          </view>

          <view class="trip-actions">
            <view class="secondary-action" @tap.stop="goDetail(task)">
              <text>查看详情</text>
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
          <view>
            <text class="section-title">附近待响应</text>
          </view>
          <text class="section-count section-count-muted">{{ offerTasks.length }}</text>
        </view>

        <view v-if="!offerTasks.length" class="empty-card compact-empty">
          <text class="empty-title">暂无新任务</text>
          <text class="empty-desc">附近有新任务时会通知你</text>
        </view>

        <view v-for="task in offerTasks" :key="task.id" class="offer-card">
          <view class="offer-head">
            <view class="urgency-chip" :class="`urgency-${task.urgency.toLowerCase()}`">
              {{ urgencyLabel(task.urgency) }}
            </view>
            <text class="distance-value">距你约 {{ formatDistance(task.distanceToRequesterMeters) }}</text>
          </view>
          <text class="offer-title">附近有人需要急救支援</text>
          <text class="offer-privacy">响应后显示精确位置和联系方式</text>
          <view class="offer-meta">
            <view class="offer-meta-item">
              <app-icon :name="task.matchedAedType === 'MOBILE' ? 'mobile-device' : 'fixed-device'" :size="16" color="#147452" />
              <text>{{ task.matchedAedType === 'MOBILE' ? '移动 AED 直达' : '需先取固定 AED' }}</text>
            </view>
            <view class="offer-meta-item">
              <app-icon name="calendar" :size="16" color="#66768C" />
              <text>{{ relativeTime(task.createdAt) }}</text>
            </view>
          </view>
          <view
            class="accept-action"
            :class="{ 'action-busy': busyTaskId === task.id }"
            @tap="accept(task)"
          >
            <text>{{ busyTaskId === task.id ? '正在确认响应…' : '响应这次救援' }}</text>
          </view>
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
.page { min-height: 100vh; box-sizing: border-box; overflow: visible; padding: 20rpx 24rpx 0; background: #F2F2F7; color: #1C1C1E; }
.response-console { position: relative; padding: 23rpx 24rpx 20rpx; overflow: hidden; border: 1rpx solid rgba(60,60,67,.14); border-radius: 22rpx; background: #FFFFFF; box-shadow: none; }
.response-console::before { content: ''; position: absolute; top: -160rpx; right: -110rpx; width: 340rpx; height: 340rpx; border: 2rpx solid rgba(46,109,209,.12); border-radius: 50%; box-shadow: 0 0 0 42rpx rgba(46,109,209,.035), 0 0 0 88rpx rgba(46,109,209,.025); animation: consoleBreath 4s ease-in-out infinite; }
.console-head { position: relative; z-index: 1; display: flex; align-items: flex-start; justify-content: space-between; gap: 20rpx; }
.console-title { display: block; }
.console-title { color: #20364D; font-size: 29rpx; font-weight: 760; }
.online-chip { display: flex; align-items: center; gap: 8rpx; margin: 2rpx 0 0; padding: 9rpx 13rpx; border: 0; border-radius: 999rpx; background: #E9F5F0; color: #197C59; font-size: 19rpx; font-weight: 700; backdrop-filter: none; }
.online-chip-muted { background: #EDF1F5; color: #7C8998; }
.online-dot { width: 10rpx; height: 10rpx; border-radius: 50%; background: currentColor; animation: onlinePulse 2.4s ease-out infinite; }
.dispatch-metrics { position: relative; z-index: 1; display: grid; grid-template-columns: 1fr 1rpx 1fr; align-items: center; margin-top: 16rpx; padding: 15rpx 8rpx 0; border-top: 1rpx solid #E4EAF0; }
.dispatch-metric { text-align: center; }
.metric-value, .metric-label { display: block; }
.metric-value { color: #2C435A; font-size: 28rpx; font-weight: 820; }
.metric-value-alert { color: #C98327; }
.metric-label { margin-top: 2rpx; color: #8B98A7; font-size: 17rpx; }
.metric-divider { height: 36rpx; background: #E6ECF1; }

.dispatch-content { min-height: 60vh; padding: 0; margin: 18rpx 0 0; background: transparent; box-shadow: none; }
.golden-window { display: flex; align-items: center; gap: 12rpx; padding: 14rpx 18rpx; border: 1rpx solid #F0D1D5; border-radius: 16rpx; background: #FFF8F8; box-shadow: none; }
.vital-mark { display: flex; width: 48rpx; height: 48rpx; flex: none; align-items: center; justify-content: center; border-radius: 15rpx; background: #F9EAEC; box-shadow: none; }
.golden-copy { min-width: 0; flex: 1; }
.golden-title { display: block; }
.golden-title { color: #71383D; font-size: 21rpx; font-weight: 760; }
.emergency-call { display: flex; min-width: 70rpx; height: 46rpx; align-items: center; justify-content: center; border: 1rpx solid #E8BEC2; border-radius: 999rpx; color: #B52B36; font-size: 20rpx; font-weight: 800; }

.loading-list { margin-top: 18rpx; padding: 8rpx 22rpx; border: 1rpx solid #E1E9F0; border-radius: 22rpx; background: #FFFFFF; }
.loading-row { display: flex; align-items: center; gap: 15rpx; padding: 19rpx 0; border-bottom: 1rpx solid #EEF2F5; }
.loading-row:last-child { border-bottom: 0; }
.loading-icon { width: 56rpx; height: 56rpx; flex: none; border-radius: 18rpx; }
.loading-copy { min-width: 0; flex: 1; }
.loading-title { width: 42%; height: 19rpx; border-radius: 999rpx; }
.loading-text { width: 74%; height: 15rpx; margin-top: 12rpx; border-radius: 999rpx; }

.section { margin-top: 24rpx; }
.section-head { display: flex; align-items: flex-end; justify-content: space-between; padding: 0 3rpx 13rpx; }
.section-title { display: block; }
.section-title { color: #263D55; font-size: 27rpx; font-weight: 780; }
.section-count { min-width: 34rpx; padding: 4rpx 9rpx; border-radius: 10rpx; background: #EAF2FC; color: #2E6DD1; text-align: center; font-size: 18rpx; font-weight: 800; }
.section-count-muted { background: #EDF1F5; color: #728296; }
.trip-card, .offer-card, .empty-card { margin: 0 0 14rpx; padding: 20rpx; border: 1rpx solid rgba(60,60,67,.14); border-radius: 19rpx; background: #FFFFFF; box-shadow: none; }
.trip-card { transition: transform 160ms cubic-bezier(.22,.8,.24,1), box-shadow 240ms ease; }
.trip-card:active { transform: scale(.988); box-shadow: 0 4rpx 12rpx rgba(42,67,92,.04); }
.trip-head, .offer-head, .trip-actions, .offer-meta { display: flex; align-items: center; justify-content: space-between; }
.trip-order { display: flex; align-items: baseline; gap: 9rpx; }
.trip-order-label { color: #2A4159; font-size: 23rpx; font-weight: 780; }
.status-chip { padding: 6rpx 11rpx; border-radius: 9rpx; background: #EAF2FC; color: #2E6DD1; font-size: 18rpx; font-weight: 700; }
.participant-row { display: flex; align-items: center; margin-top: 19rpx; padding: 0; background: transparent; }
.task-address { min-width: 0; flex: 1; overflow: hidden; color: #728296; font-size: 19rpx; text-overflow: ellipsis; white-space: nowrap; }
.phone-action { display: flex; width: 52rpx; height: 52rpx; align-items: center; justify-content: center; border-radius: 17rpx; background: #EDF4FF; }
.route-panel { margin-top: 15rpx; padding: 14rpx 15rpx 0; border: 1rpx solid #E8EEF3; border-radius: 15rpx; background: #FAFCFD; }
.route-row { display: flex; min-height: 65rpx; }
.route-track { display: flex; width: 36rpx; flex-direction: column; align-items: center; }
.route-node { z-index: 1; display: flex; width: 21rpx; height: 21rpx; box-sizing: border-box; align-items: center; justify-content: center; border-radius: 50%; }
.route-node-aed { background: #23956A; color: #FFFFFF; font-size: 12rpx; font-weight: 900; }
.route-node-scene { border: 6rpx solid #C93D46; background: #FFFFFF; }
.route-line { width: 2rpx; flex: 1; background: linear-gradient(#23956A 0 35%, #D9E3EB 35%); background-size: 100% 16rpx; animation: routeLine 1.8s linear infinite; }
.route-copy { min-width: 0; flex: 1; padding-bottom: 15rpx; }
.route-label { display: block; color: #8B98A7; font-size: 17rpx; }
.route-value { display: block; margin-top: 2rpx; overflow: hidden; color: #334A61; font-size: 21rpx; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.task-guidance { display: flex; align-items: center; gap: 9rpx; margin-top: 15rpx; padding: 13rpx 15rpx; border-radius: 14rpx; background: #EEF8F4; color: #416C5D; font-size: 19rpx; }
.trip-actions { gap: 11rpx; margin-top: 17rpx; }
.secondary-action, .primary-action, .waiting-action { display: flex; min-height: 66rpx; align-items: center; justify-content: center; border-radius: 15rpx; font-size: 21rpx; font-weight: 750; }
.secondary-action { width: 150rpx; border: 1rpx solid #D9E2EA; color: #607187; }
.primary-action { flex: 1; background: #2E6DD1; color: #FFFFFF; box-shadow: 0 8rpx 18rpx rgba(46,109,209,.16); }
.waiting-action { flex: 1; background: #F0F3F6; color: #7E8B9A; }

.urgency-chip { padding: 6rpx 11rpx; border-radius: 9rpx; font-size: 18rpx; font-weight: 800; }
.urgency-critical { background: #F9EAEC; color: #B52B36; }
.urgency-high { background: #FFF3E3; color: #A86B1F; }
.urgency-medium { background: #EAF2FC; color: #245FAF; }
.distance-value { color: #2E6DD1; font-size: 20rpx; font-weight: 750; }
.offer-title { display: block; margin-top: 17rpx; color: #2A4159; font-size: 25rpx; font-weight: 780; }
.offer-privacy { display: block; margin-top: 7rpx; color: #7E8D9E; font-size: 19rpx; line-height: 1.55; }
.offer-meta { justify-content: flex-start; gap: 20rpx; margin-top: 16rpx; padding-top: 15rpx; border-top: 1rpx solid #EEF2F5; }
.offer-meta-item { display: flex; align-items: center; gap: 6rpx; color: #607187; font-size: 18rpx; }
.accept-action { display: flex; min-height: 68rpx; align-items: center; justify-content: center; margin-top: 18rpx; border-radius: 15rpx; background: #2E6DD1; color: #FFFFFF; font-size: 22rpx; font-weight: 780; box-shadow: 0 8rpx 18rpx rgba(46,109,209,.16); }
.action-busy { opacity: .55; }
.empty-card { display: flex; min-height: 140rpx; flex-direction: column; align-items: center; justify-content: center; text-align: center; }
.empty-title { color: #40556C; font-size: 22rpx; font-weight: 740; }
.empty-desc { margin-top: 6rpx; color: #8795A6; font-size: 18rpx; }
.bottom-safe { height: calc(34rpx + env(safe-area-inset-bottom)); }

@keyframes onlinePulse { 0% { box-shadow: 0 0 0 0 rgba(35,149,106,.23); } 70%,100% { box-shadow: 0 0 0 9rpx rgba(35,149,106,0); } }
@keyframes routeLine { to { background-position-y: 16rpx; } }
@keyframes consoleBreath { 0%,100% { opacity: .58; transform: scale(.96); } 50% { opacity: 1; transform: scale(1.06); } }

@media (prefers-reduced-motion: reduce) {
  .online-dot, .route-line { animation: none; }
}
</style>
