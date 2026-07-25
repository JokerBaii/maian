<template>
  <view class="page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-content">
        <view class="nav-back" @tap="goBack">
          <text class="back-arrow">&#x2190;</text>
        </view>
        <text class="nav-title">异常预警</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <scroll-view
      class="scroll-content"
      scroll-y
      :style="{ paddingTop: (statusBarHeight + 44) + 'px' }"
    >
      <!-- 预警概览卡片 -->
      <view class="summary-card">
        <view class="summary-bg"></view>
        <view class="summary-content">
          <view class="summary-main">
            <view class="summary-left">
              <text class="summary-count">{{ totalAlerts }}</text>
              <text class="summary-unit">次</text>
            </view>
            <view class="summary-info">
              <text class="summary-label">本月预警次数</text>
              <text class="summary-date">{{ currentMonth }}</text>
            </view>
          </view>
          <view class="summary-divider"></view>
          <view class="summary-latest">
            <view class="latest-icon">
              <text class="latest-icon-text">!</text>
            </view>
            <view class="latest-info">
              <text class="latest-label">最近预警</text>
              <text class="latest-msg">{{ latestAlert.message }}</text>
              <text class="latest-time">{{ latestAlert.time }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 预警列表 -->
      <view class="card list-card">
        <view class="card-header">
          <view class="card-title-wrap">
            <view class="card-title-bar"></view>
            <text class="card-title">预警记录</text>
          </view>
        </view>
        <view class="timeline">
          <view
            v-for="(alert, idx) in alertList"
            :key="idx"
            class="timeline-item"
          >
            <view class="timeline-dot-wrap">
              <view class="timeline-dot" :class="alert.type === 'high' ? 'dot-high' : 'dot-low'"></view>
              <view v-if="idx < alertList.length - 1" class="timeline-line"></view>
            </view>
            <view class="timeline-card">
              <view class="timeline-card-header">
                <view class="alert-type-badge" :class="alert.type === 'high' ? 'badge-high' : 'badge-low'">
                  <text class="badge-text">{{ alert.type === 'high' ? '偏高' : '偏低' }}</text>
                </view>
                <text class="timeline-card-time">{{ alert.time }}</text>
              </view>
              <view class="timeline-card-body">
                <view class="alert-value-row">
                  <text class="alert-value-label">心率</text>
                  <text class="alert-value-number" :class="alert.type === 'high' ? 'value-high' : 'value-low'">
                    {{ alert.value }}
                  </text>
                  <text class="alert-value-unit">BPM</text>
                </view>
                <text class="alert-message">{{ alert.message }}</text>
              </view>
              <view class="timeline-card-footer">
                <view class="ref-range">
                  <text class="ref-label">正常范围</text>
                  <text class="ref-value">60-100 BPM</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 预警设置入口 -->
      <view class="card settings-card" @tap="goSettings">
        <view class="settings-left">
          <view class="settings-icon">
            <text class="settings-icon-text">&#x2699;</text>
          </view>
          <view class="settings-info">
            <text class="settings-title">预警阈值设置</text>
            <text class="settings-desc">自定义心率预警上下限</text>
          </view>
        </view>
        <text class="settings-arrow">></text>
      </view>

      <!-- 底部安全区 -->
      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { mockHeartRateData } from '@/mock/data'

// 系统信息
const statusBarHeight = ref(0)
const systemInfo = uni.getSystemInfoSync()
statusBarHeight.value = systemInfo.statusBarHeight || 20

// 预警数据
const alertList = computed(() => mockHeartRateData.alerts)
const totalAlerts = computed(() => mockHeartRateData.alerts.length)
const latestAlert = computed(() => {
  if (mockHeartRateData.alerts.length > 0) {
    return mockHeartRateData.alerts[0]
  }
  return { message: '暂无预警', time: '--' }
})

const currentMonth = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}年${now.getMonth() + 1}月`
})

function goSettings() {
  uni.showToast({
    title: '预警设置功能开发中',
    icon: 'none'
  })
}

function goBack() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F0F4FA;
}

/* 导航栏 */
.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
}
.nav-bar-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 88rpx;
  padding: 0 32rpx;
}
.nav-back {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.back-arrow {
  font-size: 40rpx;
  color: #FFFFFF;
  font-weight: 600;
}
.nav-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #FFFFFF;
}
.nav-placeholder {
  width: 64rpx;
}

/* 滚动内容 */
.scroll-content {
  min-height: 100vh;
  box-sizing: border-box;
}

/* 概览卡片 */
.summary-card {
  position: relative;
  margin: 24rpx 24rpx 0;
  background: #FFFFFF;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.08);
}
.summary-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #2B6FF0 0%, #1A4FD0 50%, #0D3AAF 100%);
  opacity: 0.04;
}
.summary-content {
  position: relative;
  padding: 32rpx;
  z-index: 2;
}
.summary-main {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.summary-left {
  display: flex;
  align-items: baseline;
  gap: 4rpx;
}
.summary-count {
  font-size: 72rpx;
  font-weight: 800;
  color: #2B6FF0;
  line-height: 1;
}
.summary-unit {
  font-size: 24rpx;
  color: #2B6FF0;
  font-weight: 600;
}
.summary-info {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.summary-label {
  font-size: 26rpx;
  color: #4E5969;
  font-weight: 500;
}
.summary-date {
  font-size: 22rpx;
  color: #C9CDD4;
}
.summary-divider {
  height: 1rpx;
  background: #F2F3F5;
  margin: 24rpx 0;
}
.summary-latest {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.latest-icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: rgba(245, 63, 63, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.latest-icon-text {
  font-size: 28rpx;
  color: #F53F3F;
  font-weight: 700;
}
.latest-info {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  flex: 1;
}
.latest-label {
  font-size: 24rpx;
  color: #86909C;
  font-weight: 500;
}
.latest-msg {
  font-size: 28rpx;
  color: #1D2129;
  font-weight: 600;
}
.latest-time {
  font-size: 22rpx;
  color: #C9CDD4;
}

/* 通用卡片 */
.card {
  margin: 24rpx 24rpx 0;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.06);
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}
.card-title-wrap {
  display: flex;
  align-items: center;
}
.card-title-bar {
  width: 6rpx;
  height: 28rpx;
  border-radius: 3rpx;
  background: #2B6FF0;
  margin-right: 12rpx;
}
.card-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1D2129;
}

/* 时间线 */
.timeline {
  padding-left: 4rpx;
}
.timeline-item {
  display: flex;
  gap: 20rpx;
  padding-bottom: 24rpx;
}
.timeline-item:last-child {
  padding-bottom: 0;
}
.timeline-dot-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  width: 28rpx;
  flex-shrink: 0;
}
.timeline-dot {
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  border: 4rpx solid #FFFFFF;
  box-shadow: 0 0 0 2rpx #E5E6EB;
  z-index: 2;
  flex-shrink: 0;
}
.dot-high {
  background: #F53F3F;
  box-shadow: 0 0 8rpx rgba(245, 63, 63, 0.3), 0 0 0 2rpx #F53F3F;
}
.dot-low {
  background: #FF9A2E;
  box-shadow: 0 0 8rpx rgba(255, 154, 46, 0.3), 0 0 0 2rpx #FF9A2E;
}
.timeline-line {
  flex: 1;
  width: 2rpx;
  background: #E5E6EB;
  margin-top: 8rpx;
}

/* 时间线卡片 */
.timeline-card {
  flex: 1;
  background: #F7F8FA;
  border-radius: 16rpx;
  padding: 20rpx;
  margin-top: -4rpx;
}
.timeline-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}
.alert-type-badge {
  padding: 4rpx 16rpx;
  border-radius: 12rpx;
}
.badge-high {
  background: rgba(245, 63, 63, 0.08);
}
.badge-low {
  background: rgba(255, 154, 46, 0.08);
}
.badge-text {
  font-size: 22rpx;
  font-weight: 600;
}
.badge-high .badge-text {
  color: #F53F3F;
}
.badge-low .badge-text {
  color: #FF9A2E;
}
.timeline-card-time {
  font-size: 22rpx;
  color: #C9CDD4;
}
.timeline-card-body {
  margin-bottom: 12rpx;
}
.alert-value-row {
  display: flex;
  align-items: baseline;
  gap: 6rpx;
  margin-bottom: 8rpx;
}
.alert-value-label {
  font-size: 24rpx;
  color: #86909C;
}
.alert-value-number {
  font-size: 44rpx;
  font-weight: 800;
  line-height: 1;
}
.value-high {
  color: #F53F3F;
}
.value-low {
  color: #FF9A2E;
}
.alert-value-unit {
  font-size: 22rpx;
  color: #86909C;
}
.alert-message {
  font-size: 26rpx;
  color: #4E5969;
  line-height: 1.5;
}
.timeline-card-footer {
  padding-top: 12rpx;
  border-top: 1rpx solid #E5E6EB;
}
.ref-range {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.ref-label {
  font-size: 22rpx;
  color: #C9CDD4;
}
.ref-value {
  font-size: 22rpx;
  color: #86909C;
  font-weight: 600;
}

/* 设置入口 */
.settings-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.settings-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.settings-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 20rpx;
  background: linear-gradient(135deg, #86909C 0%, #C9CDD4 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(134, 144, 156, 0.2);
}
.settings-icon-text {
  font-size: 36rpx;
  color: #FFFFFF;
}
.settings-info {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}
.settings-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1D2129;
}
.settings-desc {
  font-size: 22rpx;
  color: #86909C;
}
.settings-arrow {
  font-size: 32rpx;
  color: #C9CDD4;
  font-weight: 300;
}

/* 底部安全区 */
.bottom-safe {
  height: 60rpx;
}
</style>
