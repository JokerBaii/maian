<template>
  <view class="page apple-page">
    <view class="summary-row">
      <text>{{ activeCount ? `${activeCount} 项正在处理` : '最近的设备协同记录' }}</text>
      <text class="refresh" @tap="loadTasks">刷新</text>
    </view>
    <view v-if="errorMessage" class="empty"><text>{{ errorMessage }}</text><text class="refresh" @tap="loadTasks">重新加载</text></view>
    <template v-else>
      <coordination-task-card v-for="task in tasks" :key="task.id" :task="task" allow-retry @retry="retryTask(task.id)" />
      <view v-if="!tasks.length" class="empty">
        <text class="empty-title">{{ loading ? '正在同步协同记录…' : '暂无设备协同任务' }}</text>
        <text>设备状态变化后，系统会自动跟进。</text>
        <text class="refresh" @tap="openDevices">管理我的设备</text>
      </view>
    </template>
    <app-disclosure v-if="tasks.length" title="协同说明" class="coordination-help"><text>设备变化后，系统会检查关联救援、寻找替代资源并跟进结果。每条记录均可展开查看处理过程。</text></app-disclosure>
    <assistant-companion context="devices" />
  </view>
</template>

<script setup lang="ts">
import AssistantCompanion from '@/components/AssistantCompanion.vue'
import AppDisclosure from '@/components/AppDisclosure.vue'
import { computed, ref } from 'vue'
import { onHide, onShow, onUnload } from '@dcloudio/uni-app'
import CoordinationTaskCard from '@/components/CoordinationTaskCard.vue'
import { listMyCoordinationTasks, retryCoordinationTask, type CoordinationTask } from '@/api/agent'
import { userFacingError } from '@/utils/presentation'

const tasks = ref<CoordinationTask[]>([])
const loading = ref(false)
const errorMessage = ref('')
const activeCount = computed(() => tasks.value.filter(task => ['PENDING', 'RUNNING'].includes(task.status)).length)
let timer: ReturnType<typeof setInterval> | null = null
async function loadTasks() {
  if (loading.value) return
  loading.value = true
  try { tasks.value = await listMyCoordinationTasks(); errorMessage.value = '' }
  catch (error) { errorMessage.value = userFacingError(error, '协同记录加载失败') }
  finally { loading.value = false }
}
function stop() { if (timer) clearInterval(timer); timer = null }
function openDevices() { uni.navigateTo({ url: '/pages/device/manage' }) }
let retrying = false
async function retryTask(id: string) {
  if (retrying) return
  retrying = true
  try { await retryCoordinationTask(id); await loadTasks() }
  catch (error) { uni.showToast({ title: userFacingError(error, '重试失败'), icon: 'none' }) }
  finally { retrying = false }
}
onShow(() => { void loadTasks(); stop(); timer = setInterval(loadTasks, 3000) })
onHide(stop)
onUnload(stop)
</script>

<style scoped lang="scss">
.page { min-height: 100vh; box-sizing: border-box; padding: 32rpx 28rpx calc(40rpx + env(safe-area-inset-bottom)); background: #f2f2f7; }
.coordination-help { padding: 8rpx 4rpx; color: #8E8E93; font-size: 23rpx; line-height: 1.65; }
.summary-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; color: #636366; font-size: 24rpx; }
.refresh { display: inline-block; padding: 12rpx 0; color: #2865b6; }
.empty { padding: 48rpx 30rpx; display: flex; flex-direction: column; gap: 20rpx; background: #fff; border-radius: 24rpx; font-size: 26rpx; color: #636366; line-height: 1.7; }
.empty-title { font-size: 30rpx; font-weight: 600; color: #1c1c1e; }
</style>
