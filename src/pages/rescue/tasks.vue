<template>
  <view class="page">
    <view class="dispatch-stage">
      <view class="hero">
        <view>
          <text class="hero-kicker">EMERGENCY RESPONSE</text>
          <text class="hero-title">生命响应台</text>
          <text class="hero-desc">附近救援力量实时调度</text>
        </view>
        <view class="online-chip" :class="{ 'online-chip-muted': !presenceOnline }">
          <view class="online-dot"></view>
          <text>{{ presenceOnline ? '可响应' : '定位中' }}</text>
        </view>
      </view>

      <view class="response-radar">
        <view class="radar-grid radar-grid-one"></view>
        <view class="radar-grid radar-grid-two"></view>
        <view class="radar-grid radar-grid-three"></view>
        <view class="radar-center">
          <view class="radar-pulse"></view>
          <text>你</text>
        </view>
        <view v-if="offerTasks.length" class="radar-signal radar-signal-one"><view></view></view>
        <view v-if="offerTasks.length > 1" class="radar-signal radar-signal-two"><view></view></view>
        <view v-if="activeTasks.length" class="radar-signal radar-signal-active"><view>AED</view></view>
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
        <view class="metric-divider"></view>
        <view class="dispatch-metric">
          <text class="metric-value metric-value-live">10s</text>
          <text class="metric-label">自动同步</text>
        </view>
      </view>
    </view>

    <view class="dispatch-sheet">
      <view class="sheet-handle"></view>
      <view class="golden-window">
        <view class="vital-mark"><text>＋</text></view>
        <view class="golden-copy">
          <text class="golden-title">守住黄金响应窗口</text>
          <text class="golden-desc">先确认现场安全；危急情况同步拨打 120</text>
        </view>
        <view class="emergency-call" @tap="callEmergency"><text>120</text></view>
      </view>

    <view v-if="loading && !tasks.length" class="empty-card">
      <view class="loading-ring"></view>
      <text class="empty-title">正在同步附近救援任务</text>
      <text class="empty-desc">位置仅用于距离筛选和响应后的救援导航</text>
    </view>

    <template v-else>
      <view v-if="activeTasks.length" class="section">
        <view class="section-head">
          <view>
            <text class="section-title">进行中的救援</text>
            <text class="section-desc">按急救处置节点推进，减少现场漏项</text>
          </view>
          <text class="section-count">{{ activeTasks.length }}</text>
        </view>

        <view v-for="task in activeTasks" :key="task.id" class="trip-card" @tap="goDetail(task)">
          <view class="trip-head">
            <view class="trip-order">
              <text class="trip-order-label">现场救援任务</text>
              <text class="trip-order-id">#{{ task.id.slice(-6).toUpperCase() }}</text>
            </view>
            <view class="status-chip">{{ statusLabel(task.status) }}</view>
          </view>

          <view class="participant-row">
            <view class="participant-avatar">
              <text>{{ participantInitial(task.requester?.displayName) }}</text>
            </view>
            <view class="participant-copy">
              <view class="participant-name-row">
                <text class="participant-name">{{ task.requester?.displayName || '求救者' }}</text>
                <text v-if="task.requester?.verified" class="verified-chip">已实名</text>
              </view>
              <text class="participant-role">你是本次任务的现场施救者</text>
            </view>
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
                <text class="route-label">AED 取用点</text>
                <text class="route-value">{{ task.matchedAed?.name || '已匹配固定 AED' }}</text>
              </view>
            </view>
            <view class="route-row">
              <view class="route-track">
                <view class="route-node route-node-scene"></view>
              </view>
              <view class="route-copy">
                <text class="route-label">救援目的地</text>
                <text class="route-value">{{ task.address || '响应后显示精确地址' }}</text>
              </view>
            </view>
          </view>

          <view class="task-guidance">
            <app-icon name="navigate-filled" :size="16" color="#147452" />
            <text>{{ actionGuidance(task) }}</text>
          </view>

          <view class="response-readout">
            <view>
              <text class="readout-label">当前处置</text>
              <text class="readout-value">{{ nextAction(task)?.label || terminalHint(task) }}</text>
            </view>
            <view v-if="task.matchedAed?.estimatedArrivalSeconds" class="readout-eta">
              <text class="readout-eta-value">{{ formatEta(task.matchedAed.estimatedArrivalSeconds) }}</text>
              <text class="readout-label">匹配 ETA</text>
            </view>
          </view>

          <view class="trip-actions">
            <view class="secondary-action" @tap.stop="goDetail(task)">
              <text>进入救援处置</text>
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

      <view class="section">
        <view class="section-head">
          <view>
            <text class="section-title">附近待响应</text>
            <text class="section-desc">只展示当前可响应范围内的脱敏任务</text>
          </view>
          <text class="section-count section-count-muted">{{ offerTasks.length }}</text>
        </view>

        <view v-if="!offerTasks.length" class="empty-card compact-empty">
          <text class="empty-title">暂无新任务</text>
          <text class="empty-desc">保持可响应状态，服务端会实时推送附近呼救</text>
        </view>

        <view v-for="task in offerTasks" :key="task.id" class="offer-card">
          <view class="offer-head">
            <view class="urgency-chip" :class="`urgency-${task.urgency.toLowerCase()}`">
              {{ urgencyLabel(task.urgency) }}
            </view>
            <text class="distance-value">距你约 {{ formatDistance(task.distanceToRequesterMeters) }}</text>
          </view>
          <text class="offer-title">附近有人需要急救支援</text>
          <text class="offer-privacy">精确位置、联系方式与现场图片将在确认响应后开放</text>
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
    const location = await getCurrentGcj02Location()
    await updateResponderPresence({ ...location, available: true })
    presenceOnline.value = true
    tasks.value = (await listResponderTasks()).content
  } catch (error: any) {
    presenceOnline.value = false
    uni.showToast({ title: error?.message || '救援任务加载失败', icon: 'none' })
  } finally {
    loading.value = false
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
    uni.showToast({ title: error?.message || '响应失败', icon: 'none' })
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
    uni.showToast({ title: error?.message || '状态更新失败', icon: 'none' })
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

function participantInitial(name?: string) {
  return (name || '求').slice(0, 1)
}

function urgencyLabel(value: string) {
  return ({ CRITICAL: '危急', HIGH: '紧急', MEDIUM: '一般' } as Record<string, string>)[value] || value
}

function statusLabel(value: string) {
  return ({
    MATCHING: '等待响应', EN_ROUTE_TO_AED: '赶往 AED',
    EN_ROUTE_TO_REQUESTER: '赶往现场', ARRIVED: '已到达', RESCUING: '施救中',
    PENDING_CONFIRMATION: '待求救者确认', COMPLETED: '已完成', USER_CANCELLED: '已取消',
    NO_RESOURCE: '暂无资源', EXPIRED: '已超时', SYSTEM_FAILED: '系统异常'
  } as Record<string, string>)[value] || value
}

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

function formatEta(seconds: number) {
  return seconds < 60 ? `${seconds}秒` : `${Math.ceil(seconds / 60)}分钟`
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
.page { min-height: 100vh; box-sizing: border-box; overflow: hidden; background: #F3F7FA; color: #172033; }
.dispatch-stage { position: relative; height: 590rpx; padding: calc(20rpx + env(safe-area-inset-top)) 24rpx 0; box-sizing: border-box; overflow: hidden; background: linear-gradient(150deg, #193A68 0%, #235EA7 54%, #387FBE 100%); }
.dispatch-stage::before { content: ''; position: absolute; inset: 0; opacity: .22; background-image: linear-gradient(rgba(255,255,255,.12) 1rpx, transparent 1rpx), linear-gradient(90deg, rgba(255,255,255,.12) 1rpx, transparent 1rpx); background-size: 78rpx 78rpx; transform: rotate(-8deg) scale(1.2); }
.hero { position: relative; z-index: 4; display: flex; align-items: flex-start; justify-content: space-between; padding: 8rpx 4rpx 16rpx; }
.hero-kicker { display: block; color: rgba(255,255,255,.66); font-size: 18rpx; font-weight: 800; letter-spacing: 3rpx; }
.hero-title { display: block; margin-top: 5rpx; color: #FFFFFF; font-size: 38rpx; font-weight: 800; }
.hero-desc { display: block; margin-top: 8rpx; color: rgba(255,255,255,.72); font-size: 22rpx; }
.online-chip { display: flex; align-items: center; gap: 8rpx; margin-top: 6rpx; padding: 10rpx 15rpx; border: 1rpx solid rgba(199, 244, 226, .32); border-radius: 999rpx; background: rgba(12, 44, 72, .28); color: #C8F5E5; font-size: 21rpx; font-weight: 700; backdrop-filter: blur(10px); }
.online-chip-muted { border-color: rgba(255,255,255,.2); background: rgba(16, 37, 62, .25); color: rgba(255,255,255,.7); }
.online-dot { width: 10rpx; height: 10rpx; border-radius: 50%; background: currentColor; }
.response-radar { position: absolute; z-index: 2; top: 130rpx; left: 50%; width: 470rpx; height: 330rpx; transform: translateX(-50%); }
.radar-grid { position: absolute; left: 50%; top: 50%; border: 1rpx solid rgba(218, 239, 255, .25); border-radius: 50%; transform: translate(-50%, -50%); }
.radar-grid-one { width: 130rpx; height: 130rpx; }
.radar-grid-two { width: 260rpx; height: 260rpx; }
.radar-grid-three { width: 430rpx; height: 430rpx; }
.radar-center { position: absolute; left: 50%; top: 50%; display: flex; width: 64rpx; height: 64rpx; align-items: center; justify-content: center; border: 5rpx solid rgba(255,255,255,.95); border-radius: 50%; background: #2E7EE7; box-shadow: 0 0 0 12rpx rgba(60, 146, 255, .2), 0 9rpx 24rpx rgba(3, 26, 53, .28); color: #FFFFFF; font-size: 20rpx; font-weight: 850; transform: translate(-50%, -50%); }
.radar-pulse { position: absolute; inset: -22rpx; border: 2rpx solid rgba(137, 201, 255, .45); border-radius: 50%; animation: radarPulse 2s ease-out infinite; }
@keyframes radarPulse { 0% { opacity: .8; transform: scale(.65); } 100% { opacity: 0; transform: scale(1.35); } }
.radar-signal { position: absolute; display: flex; width: 42rpx; height: 42rpx; align-items: center; justify-content: center; border: 4rpx solid #FFFFFF; border-radius: 50%; background: #D1424C; box-shadow: 0 7rpx 17rpx rgba(7,29,57,.25); }
.radar-signal view { width: 12rpx; height: 12rpx; border-radius: 50%; background: #FFFFFF; }
.radar-signal-one { left: 68rpx; top: 160rpx; }
.radar-signal-two { right: 72rpx; top: 92rpx; }
.radar-signal-active { right: 94rpx; bottom: 26rpx; width: 54rpx; height: 54rpx; background: #16855D; color: #FFFFFF; font-size: 14rpx; font-weight: 900; }
.dispatch-metrics { position: absolute; z-index: 4; left: 24rpx; right: 24rpx; bottom: 44rpx; display: grid; grid-template-columns: 1fr 1rpx 1fr 1rpx 1fr; align-items: center; padding: 18rpx 12rpx; border: 1rpx solid rgba(255,255,255,.2); border-radius: 20rpx; background: rgba(10, 35, 65, .38); backdrop-filter: blur(15px); }
.dispatch-metric { text-align: center; }
.metric-value, .metric-label { display: block; }
.metric-value { color: #FFFFFF; font-size: 28rpx; font-weight: 900; }
.metric-value-alert { color: #FFD4D7; }
.metric-value-live { color: #C8F5E5; }
.metric-label { margin-top: 3rpx; color: rgba(255,255,255,.64); font-size: 18rpx; }
.metric-divider { height: 45rpx; background: rgba(255,255,255,.16); }
.dispatch-sheet { position: relative; z-index: 5; min-height: 55vh; margin-top: -24rpx; padding: 0 24rpx; border-radius: 30rpx 30rpx 0 0; background: #F3F7FA; box-shadow: 0 -10rpx 30rpx rgba(12,34,61,.13); }
.sheet-handle { width: 74rpx; height: 8rpx; margin: 0 auto 13rpx; padding-top: 17rpx; border-bottom: 8rpx solid #C8D1DC; border-radius: 999rpx; }
.golden-window { display: flex; align-items: center; gap: 15rpx; padding: 18rpx 20rpx; border: 1rpx solid #F0D7D9; border-radius: 18rpx; background: linear-gradient(110deg, #FFF7F7, #FFFFFF 68%); }
.vital-mark { display: flex; width: 54rpx; height: 54rpx; flex: none; align-items: center; justify-content: center; border-radius: 15rpx; background: #C93D46; color: #FFFFFF; font-size: 31rpx; font-weight: 800; box-shadow: 0 7rpx 18rpx rgba(201, 61, 70, .17); }
.golden-copy { min-width: 0; flex: 1; }
.golden-title { display: block; color: #712C33; font-size: 23rpx; font-weight: 800; }
.golden-desc { display: block; margin-top: 4rpx; color: #8B6870; font-size: 19rpx; }
.emergency-call { display: flex; min-width: 76rpx; height: 50rpx; align-items: center; justify-content: center; border: 1rpx solid #E8BEC2; border-radius: 999rpx; color: #B52B36; font-size: 22rpx; font-weight: 850; }
.section { margin-top: 24rpx; }
.section-head { display: flex; align-items: flex-end; justify-content: space-between; padding: 0 4rpx 14rpx; }
.section-title { display: block; color: #1E2F49; font-size: 29rpx; font-weight: 780; }
.section-desc { display: block; margin-top: 5rpx; color: #8A97A8; font-size: 21rpx; }
.section-count { min-width: 36rpx; padding: 5rpx 10rpx; border-radius: 10rpx; background: #EAF2FF; color: #1F63D5; text-align: center; font-size: 20rpx; font-weight: 800; }
.section-count-muted { background: #EDF1F5; color: #6C7A8F; }
.empty-card { display: flex; min-height: 300rpx; flex-direction: column; align-items: center; justify-content: center; margin-top: 24rpx; padding: 40rpx 24rpx; border: 1rpx solid #E1E8F0; border-radius: 22rpx; background: #FFFFFF; text-align: center; }
.compact-empty { min-height: 160rpx; margin-top: 0; }
.loading-ring { width: 42rpx; height: 42rpx; margin-bottom: 20rpx; border: 5rpx solid #DFE8F4; border-top-color: #2E6DD1; border-radius: 50%; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.empty-title { color: #32425B; font-size: 25rpx; font-weight: 750; }
.empty-desc { margin-top: 8rpx; color: #8A97A8; font-size: 21rpx; line-height: 1.55; }
.trip-card, .offer-card { margin-bottom: 18rpx; padding: 26rpx; border: 1rpx solid #E0E7EF; border-radius: 22rpx; background: #FFFFFF; }
.trip-head, .offer-head, .participant-name-row, .trip-actions, .offer-meta { display: flex; align-items: center; justify-content: space-between; }
.trip-order { display: flex; align-items: baseline; gap: 10rpx; }
.trip-order-label { color: #253753; font-size: 25rpx; font-weight: 800; }
.trip-order-id { color: #99A5B5; font-size: 18rpx; font-weight: 700; letter-spacing: 1rpx; }
.status-chip { padding: 7rpx 13rpx; border-radius: 10rpx; background: #EAF2FF; color: #1F63D5; font-size: 21rpx; font-weight: 700; }
.participant-row { display: flex; align-items: center; margin-top: 22rpx; padding: 19rpx; border-radius: 16rpx; background: #F4F7FA; }
.participant-avatar { display: flex; width: 62rpx; height: 62rpx; flex: none; align-items: center; justify-content: center; border-radius: 50%; background: #DDEBFA; color: #245FAF; font-size: 26rpx; font-weight: 800; }
.participant-copy { min-width: 0; flex: 1; margin-left: 15rpx; }
.participant-name-row { justify-content: flex-start; gap: 9rpx; }
.participant-name { color: #263852; font-size: 25rpx; font-weight: 800; }
.participant-role { display: block; margin-top: 4rpx; color: #7C899B; font-size: 20rpx; }
.verified-chip { padding: 3rpx 7rpx; border-radius: 6rpx; background: #E5F5EE; color: #147452; font-size: 17rpx; }
.phone-action { display: flex; width: 58rpx; height: 58rpx; align-items: center; justify-content: center; border-radius: 50%; background: #FFFFFF; }
.route-panel { margin-top: 22rpx; }
.route-row { display: flex; min-height: 78rpx; }
.route-track { display: flex; width: 42rpx; flex-direction: column; align-items: center; }
.route-node { z-index: 1; display: flex; width: 24rpx; height: 24rpx; box-sizing: border-box; align-items: center; justify-content: center; border-radius: 50%; }
.route-node-aed { background: #147452; color: #FFFFFF; font-size: 15rpx; font-weight: 900; }
.route-node-scene { border: 6rpx solid #C93D46; background: #FFFFFF; }
.route-line { width: 2rpx; flex: 1; background: #D9E1EA; }
.route-copy { min-width: 0; flex: 1; padding-bottom: 18rpx; }
.route-label { display: block; color: #8B98A9; font-size: 19rpx; }
.route-value { display: block; margin-top: 4rpx; overflow: hidden; color: #2A3B55; font-size: 24rpx; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.task-guidance { display: flex; align-items: center; gap: 10rpx; padding: 15rpx 17rpx; border-radius: 13rpx; background: #EEF8F4; color: #426A5B; font-size: 21rpx; line-height: 1.45; }
.response-readout { display: flex; align-items: flex-end; justify-content: space-between; gap: 20rpx; margin-top: 18rpx; padding: 18rpx 19rpx; border: 1rpx solid #DEE7F1; border-radius: 16rpx; background: linear-gradient(120deg, #F8FAFD, #F2F7FD); }
.readout-label, .readout-value, .readout-eta-value { display: block; }
.readout-label { color: #8794A6; font-size: 18rpx; }
.readout-value { margin-top: 5rpx; color: #233754; font-size: 24rpx; font-weight: 800; }
.readout-eta { flex: none; text-align: right; }
.readout-eta-value { margin-bottom: 4rpx; color: #147452; font-size: 28rpx; font-weight: 900; }
.trip-actions { gap: 12rpx; margin-top: 20rpx; }
.secondary-action, .primary-action, .waiting-action { display: flex; min-height: 72rpx; align-items: center; justify-content: center; border-radius: 13rpx; font-size: 24rpx; font-weight: 750; }
.secondary-action { width: 190rpx; border: 1rpx solid #D7E0EA; color: #53647A; }
.primary-action { flex: 1; background: #1F63D5; color: #FFFFFF; }
.waiting-action { flex: 1; background: #F1F4F7; color: #7E8A9B; }
.offer-head { align-items: center; }
.urgency-chip { padding: 7rpx 13rpx; border-radius: 9rpx; font-size: 21rpx; font-weight: 800; }
.urgency-critical { background: #FBEAEC; color: #B52B36; }
.urgency-high { background: #FFF3E3; color: #A86B1F; }
.urgency-medium { background: #EAF2FF; color: #245FAF; }
.distance-value { color: #1F63D5; font-size: 22rpx; font-weight: 750; }
.offer-title { display: block; margin-top: 20rpx; color: #24364F; font-size: 28rpx; font-weight: 800; }
.offer-privacy { display: block; margin-top: 9rpx; color: #7B889B; font-size: 21rpx; line-height: 1.6; }
.offer-meta { justify-content: flex-start; gap: 24rpx; margin-top: 20rpx; padding-top: 18rpx; border-top: 1rpx solid #EEF1F5; }
.offer-meta-item { display: flex; align-items: center; gap: 7rpx; color: #607087; font-size: 20rpx; }
.accept-action { display: flex; min-height: 76rpx; align-items: center; justify-content: center; margin-top: 22rpx; border-radius: 13rpx; background: #1F63D5; color: #FFFFFF; font-size: 25rpx; font-weight: 800; }
.action-busy { opacity: .55; }
.bottom-safe { height: calc(44rpx + env(safe-area-inset-bottom)); }
</style>
