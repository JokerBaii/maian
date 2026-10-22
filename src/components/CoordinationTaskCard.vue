<template>
  <view class="coordination-card" :class="{ attention: task.status === 'ATTENTION' || task.status === 'FAILED' }">
    <view class="task-heading" @tap="expanded = !expanded">
      <view class="task-copy">
        <text class="task-name">{{ task.deviceName }}</text>
      </view>
      <text class="task-status">{{ statusLabel }}</text>
    </view>
    <text class="task-summary" :class="{ 'task-summary-expanded': expanded }">{{ task.summary }}</text>
    <text v-if="allowRetry && task.status === 'FAILED'" class="task-expand" @tap="$emit('retry')">重新检查</text>
    <view class="task-footer" @tap="expanded = !expanded">
      <text class="task-meta">{{ task.triggerLabel }} · {{ formatTime(task.createdAt) }}</text>
      <text class="task-expand">{{ expanded ? '收起过程' : '查看过程' }} {{ expanded ? '−' : '+' }}</text>
    </view>
    <view v-if="expanded" class="task-process">
      <view v-for="(step, index) in task.steps" :key="index" class="task-step">
        <text class="step-index">{{ index + 1 }}</text>
        <view class="step-copy">
          <view class="step-header"><text>{{ step.module }}</text><text class="step-time">{{ formatTime(step.occurredAt) }}</text></view>
          <text class="step-summary">{{ step.summary }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { CoordinationTask } from '@/api/agent'

const props = withDefaults(defineProps<{ task: CoordinationTask; allowRetry?: boolean }>(), { allowRetry: false })
defineEmits<{ retry: [] }>()
const expanded = ref(false)
const statusLabel = computed(() => ({
  PENDING: '等待处理', RUNNING: '正在协同', COMPLETED: '已完成', ATTENTION: '需人工核实', FAILED: '处理未完成'
}[props.task.status]))
function formatTime(value: string) {
  const date = new Date(value)
  return `${date.getMonth() + 1}/${date.getDate()} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped lang="scss">
.coordination-card { margin-bottom: 20rpx; padding: 22rpx; border: 1rpx solid rgba(60,60,67,.1); border-radius: 24rpx; background: #fff; color: #1c1c1e; }
.attention { border-color: #e8c8a1; }
.task-heading, .task-footer, .step-header { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }
.task-copy { flex: 1; min-width: 0; }
.task-name { display: block; font-weight: 650; font-size: 28rpx; overflow-wrap: anywhere; }
.task-status { flex-shrink: 0; color: #2865b6; font-size: 22rpx; }
.attention .task-status { color: #94601d; }
.task-summary { display: -webkit-box; margin-top: 12rpx; overflow: hidden; -webkit-box-orient: vertical; -webkit-line-clamp: 2; color: #636366; font-size: 24rpx; line-height: 1.65; }
.task-summary-expanded { display: block; }
.task-footer { margin-top: 14rpx; color: #8e8e93; font-size: 21rpx; }
.task-meta { flex: 1; min-width: 0; }
.task-expand { flex: none; color: #007AFF; padding: 8rpx 0; }
.task-process { margin-top: 16rpx; padding-top: 24rpx; border-top: 1rpx solid #eef0f3; }
.task-step { display: flex; gap: 16rpx; margin-bottom: 24rpx; }
.step-index { width: 34rpx; height: 34rpx; line-height: 34rpx; text-align: center; flex-shrink: 0; border-radius: 50%; color: #2865b6; background: #edf4ff; font-size: 21rpx; }
.step-copy { flex: 1; min-width: 0; }
.step-header { font-size: 24rpx; font-weight: 600; }
.step-time { font-size: 20rpx; font-weight: 400; color: #8e8e93; }
.step-summary { display: block; margin-top: 8rpx; font-size: 24rpx; color: #636366; line-height: 1.6; }
</style>
