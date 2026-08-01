<template>
  <view class="page">
    <view class="scroll-content">
      <view v-if="!heartRateData.wearable.connected" class="sample-note">
        <app-icon name="info-filled" :size="15" color="#1F63D5" />
        <text>示例数据 · 连接设备后显示真实预警</text>
      </view>

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
            <app-icon-tile name="notification-filled" tone="coral" />
            <view class="latest-info">
              <text class="latest-label">最近预警</text>
              <text class="latest-msg">{{ latestAlert.message }}</text>
              <text class="latest-time">{{ latestAlert.time }}</text>
            </view>
          </view>
        </view>
      </view>

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

      <view class="card settings-card" @tap="goSettings">
        <view class="settings-left">
          <app-icon-tile name="settings-filled" tone="slate" />
          <view class="settings-info">
            <text class="settings-title">预警阈值设置</text>
            <text class="settings-desc">自定义心率预警上下限</text>
          </view>
        </view>
        <app-icon class="settings-arrow" name="right" :size="14" color="#96A1B3" />
      </view>

      <view class="bottom-safe"></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import { useHealthMonitoring } from '@/composables/useHealthMonitoring'

const { monitoring: heartRateData, loadMonitoring } = useHealthMonitoring()
const alertList = computed(() => heartRateData.value.alerts)
const totalAlerts = computed(() => heartRateData.value.alerts.length)
const latestAlert = computed(() => {
  if (heartRateData.value.alerts.length > 0) {
    return heartRateData.value.alerts[0]
  }
  return { message: '暂无预警', time: '--' }
})

const currentMonth = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}年${now.getMonth() + 1}月`
})

function goSettings() {
  uni.navigateTo({ url: '/pages/mine/settings' })
}

onMounted(async () => {
  try {
    await loadMonitoring()
  } catch {
    uni.showToast({ title: '预警数据加载失败', icon: 'none' })
  }
})

</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F3F7FA;
}

.sample-note {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin: 20rpx 24rpx 0;
  padding: 14rpx 18rpx;
  border-radius: 14rpx;
  background: #EDF4FF;
  color: #41658F;
  font-size: 21rpx;
}

.scroll-content {
  min-height: 100vh;
  box-sizing: border-box;
}

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
  background: linear-gradient(135deg, #2E6DD1 0%, #1A4FD0 50%, #0D3AAF 100%);
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
  color: #2E6DD1;
  line-height: 1;
}
.summary-unit {
  font-size: 24rpx;
  color: #2E6DD1;
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
  color: #20364D;
  font-weight: 600;
}
.latest-time {
  font-size: 22rpx;
  color: #C9CDD4;
}

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
  background: #2E6DD1;
  margin-right: 12rpx;
}
.card-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #20364D;
}

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
  background: #C93D46;
  box-shadow: 0 0 8rpx rgba(245, 63, 63, 0.3), 0 0 0 2rpx #C93D46;
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
  color: #C93D46;
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
  color: #C93D46;
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
.settings-info {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}
.settings-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #20364D;
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

.bottom-safe {
  height: 60rpx;
}
</style>
