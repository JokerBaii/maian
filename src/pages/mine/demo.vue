<template>
  <view class="page apple-page motion-page-sheet">
    <view class="demo-note">
      <text class="demo-title">选择本轮演示身份</text>
      <text class="demo-desc">身份切换只用于在一台设备上展示多角色业务闭环，不代表正式登录。</text>
    </view>

    <view class="identity-list">
      <view
        v-for="item in demoUsers"
        :key="item.id"
        class="identity-card"
        :class="{ active: currentUserId === item.id }"
        @tap="choose(item.id)"
      >
        <view class="avatar">{{ item.name.charAt(0) }}</view>
        <view class="identity-main">
          <view class="identity-title-row">
            <text class="identity-name">{{ item.name }}</text>
            <text class="identity-role">{{ item.title }}</text>
          </view>
          <text class="identity-desc">{{ item.description }}</text>
        </view>
        <text class="check">{{ currentUserId === item.id ? '已选' : '切换' }}</text>
      </view>
    </view>

    <view class="flow-card">
      <text class="flow-title">推荐演示顺序</text>
      <text class="flow-step">1. 普通用户录入设备并发起呼救</text>
      <text class="flow-step">2. 平台审核员审核设备</text>
      <text class="flow-step">3. 救援志愿者确认响应并完成救援</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { demoUsers, getDemoUserId, setDemoUserId } from '@/utils/demoSession'

const currentUserId = ref(getDemoUserId())

function choose(userId: string) {
  setDemoUserId(userId)
  currentUserId.value = userId
  uni.showToast({ title: '演示身份已切换', icon: 'success' })
  setTimeout(() => uni.switchTab({ url: '/pages/mine/index' }), 500)
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; padding: 28rpx; box-sizing: border-box; background: #F3F7FA; color: #172033; }
.demo-note { padding: 8rpx 4rpx 4rpx; }
.demo-title { display: block; font-size: 38rpx; font-weight: 700; color: #1C2B45; }
.demo-desc { display: block; margin-top: 14rpx; font-size: 25rpx; line-height: 1.7; color: #8D9AAF; }
.identity-list { margin-top: 24rpx; }
.identity-card { display: flex; align-items: center; gap: 22rpx; margin-bottom: 18rpx; padding: 28rpx; border: 2rpx solid #E1E8F0; border-radius: 22rpx; background: #FFFFFF; }
.identity-card.active { border-color: #1F63D5; background: #F5F9FF; }
.avatar { display: flex; align-items: center; justify-content: center; width: 76rpx; height: 76rpx; flex: 0 0 76rpx; border-radius: 20rpx; color: #FFFFFF; background: #1F63D5; font-size: 30rpx; font-weight: 700; }
.identity-main { flex: 1; min-width: 0; }
.identity-title-row { display: flex; align-items: center; gap: 12rpx; }
.identity-name { font-size: 30rpx; font-weight: 700; }
.identity-role { padding: 4rpx 12rpx; border-radius: 8rpx; background: #EAF2FF; color: #1F63D5; font-size: 21rpx; }
.identity-desc { display: block; margin-top: 8rpx; color: #68758A; font-size: 24rpx; }
.check { color: #1F63D5; font-size: 24rpx; font-weight: 600; }
.flow-card { margin-top: 28rpx; padding: 28rpx; border-radius: 22rpx; background: #FFFFFF; }
.flow-title { display: block; margin-bottom: 16rpx; font-size: 28rpx; font-weight: 700; }
.flow-step { display: block; margin-top: 10rpx; color: #56627A; font-size: 25rpx; }
</style>
