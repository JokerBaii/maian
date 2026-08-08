<template>
  <view class="page apple-page motion-page-list">
    <view v-if="loading" class="state">正在加载待审核设备…</view>
    <view v-else-if="!devices.length" class="state-card">
      <text class="state-title">暂无待审核设备</text>
      <text class="state-desc">请先切换为普通用户并提交一台设备</text>
    </view>
    <view v-else class="device-list">
      <view
        v-for="device in devices"
        :key="device.id"
        class="device-card"
        :class="{ 'device-card-busy': reviewingId === device.id }"
        @tap="openReviewActions(device)"
      >
        <view class="card-head">
          <text class="device-name">{{ device.name }}</text>
          <text class="pending">待审核</text>
        </view>
        <text class="device-meta">{{ device.type === 'FIXED' ? '固定设备' : '移动设备' }} · {{ device.category }}</text>
        <text class="device-address">{{ device.address }}</text>
        <view class="device-foot">
          <text>{{ device.vehicleInfo || (device.expireDate ? `有效至 ${device.expireDate}` : '查看资料并审核') }}</text>
          <view class="review-action"><text>审核</text><text class="review-arrow">›</text></view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
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

function openReviewActions(device: EmergencyDeviceResponse) {
  if (reviewingId.value) return
  uni.showActionSheet({
    itemList: ['审核通过', '驳回并退回修改'],
    success: ({ tapIndex }) => review(device, tapIndex === 0)
  })
}

function review(device: EmergencyDeviceResponse, approved: boolean) {
  if (reviewingId.value) return
  reviewingId.value = device.id
  uni.showModal({
    title: approved ? '确认审核通过' : '确认驳回',
    content: approved ? `“${device.name}”将进入地图和调度候选。` : `“${device.name}”将退回用户修改。`,
    confirmColor: approved ? '#1F63D5' : '#C93D46',
    success: async result => {
      if (!result.confirm) {
        reviewingId.value = ''
        return
      }
      try {
        await reviewEmergencyDevice(device.id, approved, approved ? '资料审核通过' : '资料需补充后重新提交')
        devices.value = devices.value.filter(item => item.id !== device.id)
        uni.showToast({ title: approved ? '已通过' : '已驳回', icon: 'success' })
      } catch (error: any) {
        uni.showToast({ title: userFacingError(error, '审核失败，请稍后重试'), icon: 'none' })
      } finally {
        reviewingId.value = ''
      }
    },
    fail: () => {
      reviewingId.value = ''
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
.device-list { overflow: hidden; border: 1rpx solid #E1E8F0; border-radius: 18rpx; background: #FFFFFF; }
.device-card { position: relative; margin-left: 24rpx; padding: 22rpx 24rpx 20rpx 0; background: #FFFFFF; }
.device-card::after { content: ''; position: absolute; right: 24rpx; bottom: 0; left: 0; height: 1rpx; background: #E9EEF3; }
.device-card:last-child { border-bottom: 0; }
.device-card:last-child::after { display: none; }
.device-card:active { background: #F8FAFC; }
.device-card-busy { opacity: .55; }
.card-head { display: flex; align-items: center; justify-content: space-between; }
.device-name { font-size: 27rpx; font-weight: 720; }
.pending { padding: 5rpx 10rpx; border-radius: 8rpx; background: #FFF4E5; color: #B66A10; font-size: 19rpx; }
.device-meta, .device-address { display: block; margin-top: 9rpx; font-size: 22rpx; color: #66768A; }
.device-address { color: #26364D; }
.device-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 12rpx; color: #8795A6; font-size: 20rpx; }
.review-action { display: flex; align-items: center; color: #2E6DD1; font-weight: 700; }
.review-arrow { margin-left: 5rpx; font-size: 29rpx; line-height: 1; }
</style>
