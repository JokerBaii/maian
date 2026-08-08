<template>
  <view class="page">
    <view class="hero">
      <text class="hero-title">附近救援任务</text>
      <text class="hero-desc">处理接单、赶往现场和完成救援的状态联动。</text>
    </view>

    <view v-if="loading" class="empty">正在同步救援任务…</view>
    <view v-else-if="!tasks.length" class="empty">暂无可响应或已参与的任务</view>
    <view v-else class="task-list">
      <view v-for="task in tasks" :key="task.id" class="task-card" @tap="goDetail(task)">
        <view class="task-head">
          <text class="urgency">{{ urgencyLabel(task.urgency) }}</text>
          <text class="status">{{ statusLabel(task.status) }}</text>
        </view>
        <text class="task-address">{{ task.address }}</text>
        <text class="task-desc">{{ task.description || task.symptoms.join('、') || '现场需要急救支援' }}</text>
        <text v-if="task.matchedAed" class="aed">已匹配：{{ task.matchedAed.name }} · 约{{ formatEta(task.matchedAed.estimatedArrivalSeconds) }}</text>
        <view class="actions">
          <view
            v-if="task.status === 'MATCHING'"
            class="primary"
            :class="{ 'action-busy': busyTaskId === task.id }"
            @tap.stop="accept(task)"
          >{{ busyTaskId === task.id ? '处理中…' : '立即接单' }}</view>
          <view
            v-else-if="nextAction(task)"
            class="primary"
            :class="{ 'action-busy': busyTaskId === task.id }"
            @tap.stop="progress(task)"
          >{{ busyTaskId === task.id ? '处理中…' : nextAction(task)?.label }}</view>
          <view v-else class="done">任务已结束</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onHide, onShow } from '@dcloudio/uni-app'
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
let refreshTimer: ReturnType<typeof setInterval> | null = null
let disconnectEvents: (() => void) | null = null

async function load() {
  loading.value = true
  try {
    const location = await getCurrentGcj02Location()
    await updateResponderPresence({ ...location, available: true })
    tasks.value = (await listResponderTasks()).content
  } catch (error: any) {
    uni.showToast({ title: error?.message || '救援任务加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function goDetail(task: ResponderTaskResponse) {
  if (!task.detailAvailable) {
    uni.showToast({ title: '接单后可查看精确位置与现场资料', icon: 'none' })
    return
  }
  uni.navigateTo({ url: `/pages/rescue/detail?id=${encodeURIComponent(task.id)}&mode=responder` })
}

const busyTaskId = ref('')

async function accept(task: ResponderTaskResponse) {
  if (busyTaskId.value) return
  busyTaskId.value = task.id
  try {
    Object.assign(task, await acceptRescueTask(task.id))
    uni.showToast({ title: '接单成功', icon: 'success' })
  } catch (error: any) {
    uni.showToast({ title: error?.message || '接单失败', icon: 'none' })
    await load()
  } finally {
    busyTaskId.value = ''
  }
}

type TaskAction = 'aed-arrival' | 'aed-pickup' | 'requester-arrival' | 'rescue-start' | 'completion-submission' | 'aed-return'

function nextAction(task: ResponderTaskResponse): { action: TaskAction; label: string } | null {
  if (task.status === 'EN_ROUTE_TO_AED') {
    return task.arrivedAtAedAt
      ? { action: 'aed-pickup', label: '已取出 AED，赶往现场' }
      : { action: 'aed-arrival', label: '已到达 AED 取用点' }
  }
  if (task.status === 'EN_ROUTE_TO_REQUESTER') return { action: 'requester-arrival', label: '已到达求救者位置' }
  if (task.status === 'ARRIVED') return { action: 'rescue-start', label: '开始现场施救' }
  if (task.status === 'RESCUING') return { action: 'completion-submission', label: '完成救援' }
  if (task.status === 'COMPLETED' && task.aedCustodyStatus === 'RETURNING') {
    return { action: 'aed-return', label: '已归还 AED' }
  }
  return null
}

async function progress(task: ResponderTaskResponse) {
  const next = nextAction(task)
  if (!next) return
  if (busyTaskId.value) return
  busyTaskId.value = task.id
  try {
    Object.assign(task, await performResponderAction(task.id, next.action))
    uni.showToast({ title: '状态已同步', icon: 'success' })
  } catch (error: any) {
    uni.showToast({ title: error?.message || '状态更新失败', icon: 'none' })
  } finally {
    busyTaskId.value = ''
  }
}

function urgencyLabel(value: string) {
  return ({ CRITICAL: '危急', HIGH: '紧急', MEDIUM: '一般' } as Record<string, string>)[value] || value
}
function statusLabel(value: string) {
  return ({
    MATCHING: '等待接单', EN_ROUTE_TO_AED: '赶往 AED',
    EN_ROUTE_TO_REQUESTER: '赶往现场', ARRIVED: '已到达', RESCUING: '施救中',
    PENDING_CONFIRMATION: '待求救者确认', COMPLETED: '已完成', USER_CANCELLED: '已取消',
    NO_RESOURCE: '暂无资源', EXPIRED: '已超时', SYSTEM_FAILED: '系统异常'
  } as Record<string, string>)[value] || value
}
function formatEta(seconds: number) {
  return seconds < 60 ? `${seconds}秒` : `${Math.ceil(seconds / 60)}分钟`
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
.page { min-height: 100vh; padding: 24rpx; box-sizing: border-box; background: #F3F7FA; color: #172033; }
.hero { padding: 8rpx 4rpx 4rpx; }
.hero-title { display: block; font-size: 38rpx; font-weight: 700; color: #1C2B45; }
.hero-desc { display: block; margin-top: 10rpx; color: #8D9AAF; font-size: 24rpx; }
.empty { margin-top: 22rpx; padding: 60rpx 20rpx; border-radius: 22rpx; background: #FFFFFF; text-align: center; color: #68758A; }
.task-list { margin-top: 22rpx; }
.task-card { margin-bottom: 18rpx; padding: 28rpx; border-radius: 22rpx; background: #FFFFFF; border: 1rpx solid #E1E8F0; }
.task-head { display: flex; justify-content: space-between; align-items: center; }
.urgency { color: #C93D46; font-size: 27rpx; font-weight: 700; }
.status { padding: 6rpx 14rpx; border-radius: 10rpx; background: #EAF2FF; color: #1F63D5; font-size: 22rpx; }
.task-address { display: block; margin-top: 18rpx; font-size: 29rpx; font-weight: 700; }
.task-desc { display: block; margin-top: 10rpx; color: #56627A; font-size: 25rpx; line-height: 1.6; }
.aed { display: block; margin-top: 16rpx; color: #147452; font-size: 24rpx; }
.actions { margin-top: 24rpx; }
.primary, .complete, .done { padding: 18rpx 0; border-radius: 12rpx; text-align: center; font-size: 27rpx; font-weight: 700; }
.primary { color: #1F63D5; background: #FFFFFF; border: 1rpx solid #1F63D5; }
.complete { color: #147452; background: #FFFFFF; border: 1rpx solid #158F63; }
.done { color: #8D9AAF; background: #F5F7FA; border: 1rpx solid #E1E8F0; }
.action-busy { opacity: 0.55; }
</style>
