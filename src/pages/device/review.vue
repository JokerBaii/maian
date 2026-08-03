<template>
  <view class="page">
    <view v-if="loading" class="state">正在加载待审核设备…</view>
    <view v-else-if="!devices.length" class="state-card">
      <text class="state-title">暂无待审核设备</text>
      <text class="state-desc">请先切换为普通用户并提交一台设备</text>
    </view>
    <view v-else class="device-list">
      <view v-for="device in devices" :key="device.id" class="device-card">
        <view class="card-head">
          <text class="device-name">{{ device.name }}</text>
          <text class="pending">待审核</text>
        </view>
        <text class="device-meta">{{ device.type === 'FIXED' ? '固定设备' : '移动设备' }} · {{ device.category }}</text>
        <text class="device-address">{{ device.address }}</text>
        <text v-if="device.vehicleInfo" class="device-detail">车辆信息：{{ device.vehicleInfo }}</text>
        <text v-if="device.expireDate" class="device-detail">有效期：{{ device.expireDate }}</text>
        <view class="actions">
          <view class="reject-btn" @tap="review(device, false)">驳回</view>
          <view class="approve-btn" @tap="review(device, true)">审核通过</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { listPendingEmergencyDevices, reviewEmergencyDevice, type EmergencyDeviceResponse } from '@/api/devices'

const devices = ref<EmergencyDeviceResponse[]>([])
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    devices.value = (await listPendingEmergencyDevices()).content
  } catch (error: any) {
    uni.showToast({ title: error?.message || '待审核设备加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function review(device: EmergencyDeviceResponse, approved: boolean) {
  uni.showModal({
    title: approved ? '确认审核通过' : '确认驳回',
    content: approved ? `“${device.name}”将进入地图和调度候选。` : `“${device.name}”将退回用户修改。`,
    confirmColor: approved ? '#1F63D5' : '#C93D46',
    success: async result => {
      if (!result.confirm) return
      try {
        await reviewEmergencyDevice(device.id, approved, approved ? '资料审核通过' : '资料需补充后重新提交')
        devices.value = devices.value.filter(item => item.id !== device.id)
        uni.showToast({ title: approved ? '已通过' : '已驳回', icon: 'success' })
      } catch (error: any) {
        uni.showToast({ title: error?.message || '审核失败', icon: 'none' })
      }
    }
  })
}

onShow(load)
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; padding: 20rpx 24rpx 40rpx; box-sizing: border-box; background: #F3F7FA; color: #172033; }
.state, .state-card { padding: 56rpx 30rpx; text-align: center; border-radius: 18rpx; background: #FFFFFF; color: #68758A; }
.state-title { display: block; color: #172033; font-size: 30rpx; font-weight: 700; }
.state-desc { display: block; margin-top: 12rpx; font-size: 24rpx; }
.device-list { margin-top: 0; }
.device-card { margin-bottom: 18rpx; padding: 28rpx; border-radius: 22rpx; background: #FFFFFF; border: 1rpx solid #E1E8F0; }
.card-head { display: flex; align-items: center; justify-content: space-between; }
.device-name { font-size: 30rpx; font-weight: 700; }
.pending { padding: 6rpx 14rpx; border-radius: 10rpx; background: #FFF4E5; color: #B66A10; font-size: 22rpx; }
.device-meta, .device-address, .device-detail { display: block; margin-top: 12rpx; font-size: 25rpx; color: #56627A; }
.device-address { color: #26364D; }
.actions { display: flex; gap: 16rpx; margin-top: 26rpx; }
.reject-btn, .approve-btn { flex: 1; padding: 20rpx 0; border-radius: 14rpx; text-align: center; font-size: 26rpx; font-weight: 600; }
.reject-btn { color: #C93D46; background: #FFF0F0; }
.approve-btn { color: #FFFFFF; background: #1F63D5; }
</style>
