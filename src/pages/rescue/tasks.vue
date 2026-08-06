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
            v-else-if="task.status === 'ACCEPTED'"
            class="primary"
            :class="{ 'action-busy': busyTaskId === task.id }"
            @tap.stop="progress(task, 'RESCUING')"
          >{{ busyTaskId === task.id ? '处理中…' : '开始赶往现场' }}</view>
          <view
            v-else-if="task.status === 'RESCUING'"
            class="complete"
            :class="{ 'action-busy': busyTaskId === task.id }"
            @tap.stop="progress(task, 'COMPLETED')"
          >{{ busyTaskId === task.id ? '处理中…' : '完成救援' }}</view>
          <view v-else class="done">任务已结束</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { acceptRescueTask, listResponderTasks, updateResponderProgress, type RescueCallResponse } from '@/api/rescue'

const tasks = ref<RescueCallResponse[]>([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    tasks.value = (await listResponderTasks()).content
  } catch (error: any) {
    uni.showToast({ title: error?.message || '救援任务加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function goDetail(task: RescueCallResponse) {
  uni.navigateTo({ url: `/pages/rescue/detail?id=${encodeURIComponent(task.id)}` })
}

const busyTaskId = ref('')

async function accept(task: RescueCallResponse) {
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

async function progress(task: RescueCallResponse, status: 'RESCUING' | 'COMPLETED') {
  if (busyTaskId.value) return
  busyTaskId.value = task.id
  try {
    Object.assign(task, await updateResponderProgress(task.id, status))
    uni.showToast({ title: status === 'RESCUING' ? '已开始救援' : '救援已完成', icon: 'success' })
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
  return ({ MATCHING: '等待接单', ACCEPTED: '已接单', RESCUING: '赶往现场', COMPLETED: '已完成', CANCELLED: '已取消' } as Record<string, string>)[value] || value
}
function formatEta(seconds: number) {
  return seconds < 60 ? `${seconds}秒` : `${Math.ceil(seconds / 60)}分钟`
}

onShow(load)
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
