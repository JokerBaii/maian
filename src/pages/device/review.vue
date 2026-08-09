<template>
  <view class="page apple-page motion-page-list">
    <view class="queue-heading">
      <text class="queue-count">{{ loading ? '正在同步' : `${devices.length} 台待确认` }}</text>
      <text class="queue-hint">核对设备位置与有效信息</text>
    </view>
    <view v-if="loading" class="state">正在加载设备…</view>
    <view v-else-if="!devices.length" class="state-card">
      <text class="state-title">设备都已处理</text>
      <text class="state-desc">新提交的共享设备会出现在这里</text>
    </view>
    <view v-else class="review-list">
      <app-list-row
        v-for="device in devices"
        :key="device.id"
        class="review-row"
        :class="{ 'review-row-busy': reviewingId === device.id }"
        :icon="device.type === 'FIXED' ? 'fixed-device' : 'mobile-device'"
        :tone="device.type === 'FIXED' ? 'blue' : 'green'"
        :title="device.name"
        :summary="device.address"
        :meta="`${device.type === 'FIXED' ? '固定设备' : '移动设备'} · ${device.category} · ${device.vehicleInfo || (device.expireDate ? `有效至 ${device.expireDate}` : '资料待核验')}`"
        @tap="openReviewActions(device)"
      />
    </view>
    <app-action-sheet
      v-model:visible="sheetVisible"
      :title="selectedDevice?.name || ''"
      message="核对资料后选择处理结果"
      :actions="reviewActions"
      @select="handleReviewAction"
    />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppActionSheet, { type AppActionSheetItem } from '@/components/AppActionSheet.vue'
import AppListRow from '@/components/AppListRow.vue'
import { listPendingEmergencyDevices, reviewEmergencyDevice, type EmergencyDeviceResponse } from '@/api/devices'
import { userFacingError } from '@/utils/presentation'

const devices = ref<EmergencyDeviceResponse[]>([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    devices.value = (await listPendingEmergencyDevices()).content
  } catch (error: any) {
    uni.showToast({ title: userFacingError(error, '待审核设备加载失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

const reviewingId = ref('')
const sheetVisible = ref(false)
const selectedDevice = ref<EmergencyDeviceResponse | null>(null)
const reviewActions: AppActionSheetItem[] = [
  { label: '确认设备可用' },
  { label: '退回补充资料', destructive: true }
]

function openReviewActions(device: EmergencyDeviceResponse) {
  if (reviewingId.value) return
  selectedDevice.value = device
  sheetVisible.value = true
}

function handleReviewAction(index: number) {
  if (!selectedDevice.value) return
  review(selectedDevice.value, index === 0)
}

async function review(device: EmergencyDeviceResponse, approved: boolean) {
  if (reviewingId.value) return
  reviewingId.value = device.id
  try {
    await reviewEmergencyDevice(device.id, approved, approved ? '资料审核通过' : '资料需补充后重新提交')
    devices.value = devices.value.filter(item => item.id !== device.id)
    selectedDevice.value = null
    uni.showToast({ title: approved ? '设备已加入共享地图' : '已退回提交者', icon: 'success' })
  } catch (error: any) {
    uni.showToast({ title: userFacingError(error, '处理失败，请稍后重试'), icon: 'none' })
  } finally {
    reviewingId.value = ''
  }
}

onShow(load)
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; padding: 20rpx 0 40rpx; box-sizing: border-box; background: #F2F2F7; color: #1C1C1E; }
.queue-heading { padding: 4rpx 28rpx 18rpx; }
.queue-count { display: block; font-size: 34rpx; font-weight: 750; letter-spacing: -.03em; }
.queue-hint { display: block; margin-top: 3rpx; color: #8E8E93; font-size: 22rpx; }
.state, .state-card { margin: 0 24rpx; padding: 72rpx 30rpx; text-align: center; border-radius: 20rpx; background: #FFFFFF; color: #8E8E93; }
.state-title { display: block; color: #1C1C1E; font-size: 29rpx; font-weight: 650; }
.state-desc { display: block; margin-top: 10rpx; font-size: 22rpx; }
.review-list { padding-left: 24rpx; background: #FFFFFF; }
.review-row-busy { opacity: .48; pointer-events: none; }
</style>
