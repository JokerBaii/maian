<template>
  <view class="page">
    <!-- 顶部心率实时展示区 -->
    <view class="hero-section">
      <view class="hero-bg"></view>
      <view class="hero-content">
        <!-- 心率主数字 -->
        <view class="heart-main">
          <view class="heart-icon-wrap">
            <text class="heart-icon">&#x2764;</text>
          </view>
          <view class="heart-value-wrap">
            <text class="heart-value">{{ displayHeartRate }}</text>
            <text class="heart-unit">BPM</text>
          </view>
        </view>
        <!-- 状态标签 -->
        <view class="status-badge" :class="'status-badge-' + heartRateData.status">
          <view class="status-badge-dot"></view>
          <text class="status-badge-text">{{ statusLabel }}</text>
        </view>
        <!-- 场景标签 -->
        <view class="scene-tag">
          <text class="scene-tag-text">{{ sceneLabel }}</text>
        </view>
      </view>
    </view>

    <scroll-view class="scroll-body" scroll-y>
      <!-- 心率趋势迷你图 -->
      <view class="card trend-card">
        <view class="card-header">
          <view class="card-title-wrap">
            <view class="card-title-bar"></view>
            <text class="card-title">今日心率趋势</text>
          </view>
          <view class="card-link" @tap="goDetail">
            <text class="card-link-text">详情</text>
            <text class="card-link-arrow">></text>
          </view>
        </view>
        <view class="trend-chart">
          <view
            v-for="(item, idx) in trendBars"
            :key="idx"
            class="trend-bar-wrap"
          >
            <view
              class="trend-bar"
              :style="{ height: item.height + 'rpx', background: item.color }"
            ></view>
          </view>
        </view>
        <view class="trend-labels">
          <text class="trend-label">0时</text>
          <text class="trend-label">6时</text>
          <text class="trend-label">12时</text>
          <text class="trend-label">18时</text>
          <text class="trend-label">23时</text>
        </view>
      </view>

      <!-- 快速统计行 -->
      <view class="stats-row">
        <view class="stat-item">
          <view class="stat-icon-wrap stat-icon-green">
            <text class="stat-icon-text">v</text>
          </view>
          <text class="stat-value">{{ heartRateData.min }}</text>
          <text class="stat-label">最低心率</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <view class="stat-icon-wrap stat-icon-blue">
            <text class="stat-icon-text">~</text>
          </view>
          <text class="stat-value">{{ heartRateData.avg }}</text>
          <text class="stat-label">平均心率</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <view class="stat-icon-wrap stat-icon-red">
            <text class="stat-icon-text">^</text>
          </view>
          <text class="stat-value">{{ heartRateData.max }}</text>
          <text class="stat-label">最高心率</text>
        </view>
      </view>

      <!-- 异常预警卡片 -->
      <view v-if="heartRateData.alerts && heartRateData.alerts.length" class="card alert-card" @tap="goAlert">
        <view class="card-header">
          <view class="card-title-wrap">
            <view class="card-title-bar alert-bar"></view>
            <text class="card-title">异常预警</text>
          </view>
          <view class="card-link">
            <text class="card-link-text">全部</text>
            <text class="card-link-arrow">></text>
          </view>
        </view>
        <view
          v-for="(alert, idx) in heartRateData.alerts"
          :key="idx"
          class="alert-item"
        >
          <view class="alert-dot" :class="alert.type === 'high' ? 'alert-dot-high' : 'alert-dot-low'"></view>
          <view class="alert-info">
            <text class="alert-msg">{{ alert.message }}</text>
            <text class="alert-time">{{ alert.time }}</text>
          </view>
          <text class="alert-value" :class="alert.type === 'high' ? 'alert-value-high' : 'alert-value-low'">
            {{ alert.value }} BPM
          </text>
        </view>
      </view>

      <!-- 设备连接卡片 -->
      <view class="card device-card" @tap="goBind">
        <view class="card-header">
          <view class="card-title-wrap">
            <view class="card-title-bar device-bar"></view>
            <text class="card-title">设备连接</text>
          </view>
        </view>
        <view class="device-info">
          <view class="device-left">
            <view class="device-icon-wrap">
              <text class="device-icon-text">&#x231A;</text>
            </view>
            <view class="device-detail">
              <text class="device-name">{{ user.bindDevice.name }}</text>
              <view class="device-status-row">
                <view class="device-status-dot" :class="user.bindDevice.connected ? 'dot-online' : 'dot-offline'"></view>
                <text class="device-status-text">{{ user.bindDevice.connected ? '已连接' : '未连接' }}</text>
              </view>
            </view>
          </view>
          <view class="device-right">
            <view class="battery-wrap">
              <view class="battery-body">
                <view class="battery-level" :style="{ width: user.bindDevice.battery + '%' }"></view>
              </view>
              <view class="battery-tip"></view>
            </view>
            <text class="battery-text">{{ user.bindDevice.battery }}%</text>
          </view>
        </view>
      </view>

      <!-- 体检报告入口卡片 -->
      <view class="card entry-card" @tap="goCheckup">
        <view class="entry-left">
          <view class="entry-icon-wrap entry-icon-orange">
            <text class="entry-icon-text">📋</text>
          </view>
          <view class="entry-info">
            <text class="entry-title">体检报告</text>
            <text class="entry-desc">最近体检: {{ checkupReport.checkupDate }} · {{ checkupReport.aiAnalysis.abnormalItems.length }}项异常</text>
          </view>
        </view>
        <text class="entry-arrow">></text>
      </view>

      <!-- 健康档案入口卡片 -->
      <view class="card entry-card" @tap="goArchive">
        <view class="entry-left">
          <view class="entry-icon-wrap entry-icon-purple">
            <text class="entry-icon-text">📁</text>
          </view>
          <view class="entry-info">
            <text class="entry-title">健康档案</text>
            <text class="entry-desc">共{{ healthArchive.length }}份体检记录</text>
          </view>
        </view>
        <text class="entry-arrow">></text>
      </view>

      <!-- 底部安全区 -->
      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { mockHeartRateData, mockUser, mockCheckupReport, mockHealthArchive } from '@/mock/data'

const heartRateData = computed(() => mockHeartRateData)
const user = computed(() => mockUser)
const checkupReport = computed(() => mockCheckupReport)
const healthArchive = computed(() => mockHealthArchive)

// 动态心率显示
const displayHeartRate = ref(72)
let heartTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  // 模拟心率微小波动
  heartTimer = setInterval(() => {
    const base = mockHeartRateData.current
    const offset = Math.floor(Math.random() * 5) - 2
    displayHeartRate.value = base + offset
  }, 2000)
})

onUnmounted(() => {
  if (heartTimer) clearInterval(heartTimer)
})

// 状态标签
const statusLabel = computed(() => {
  const map: Record<string, string> = {
    normal: '正常',
    warning: '偏高',
    danger: '偏低'
  }
  return map[heartRateData.value.status] || '正常'
})

const sceneLabel = computed(() => {
  const map: Record<string, string> = {
    resting: '静息状态',
    exercise: '运动状态',
    sleeping: '睡眠状态'
  }
  return map[heartRateData.value.scene] || '静息状态'
})

// 趋势条形图数据
const trendBars = computed(() => {
  const data = heartRateData.value.todayData
  const maxVal = Math.max(...data.map(d => d.value))
  const minVal = Math.min(...data.map(d => d.value))
  const range = maxVal - minVal || 1
  return data.map(item => {
    const h = ((item.value - minVal) / range) * 120 + 20
    let color = '#00B42A'
    if (item.value > 100) color = '#F53F3F'
    else if (item.value > 85) color = '#FF9A2E'
    else if (item.value < 60) color = '#2B6FF0'
    return { height: h, color }
  })
})

// 导航
function goDetail() {
  uni.navigateTo({ url: '/pages/health/detail' })
}
function goAlert() {
  uni.navigateTo({ url: '/pages/health/alert' })
}
function goBind() {
  uni.navigateTo({ url: '/pages/health/bind' })
}
function goCheckup() {
  uni.navigateTo({ url: '/pages/checkup/report?id=' + checkupReport.value.id })
}
function goArchive() {
  uni.navigateTo({ url: '/pages/checkup/archive' })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F0F4FA;
}

/* 顶部心率展示区 */
.hero-section {
  position: relative;
  padding: 40rpx 32rpx 56rpx;
  overflow: hidden;
}
.hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 50%, #7DA8F7 100%);
  border-radius: 0 0 48rpx 48rpx;
}
.hero-content {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 2;
}
.heart-main {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.heart-icon-wrap {
  position: relative;
  width: 80rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.heart-icon {
  font-size: 64rpx;
  color: #FFFFFF;
  animation: heartBeat 1.2s ease-in-out infinite;
  filter: drop-shadow(0 4rpx 12rpx rgba(255, 255, 255, 0.4));
}
@keyframes heartBeat {
  0%, 100% { transform: scale(1); }
  15% { transform: scale(1.25); }
  30% { transform: scale(1); }
  45% { transform: scale(1.15); }
  60% { transform: scale(1); }
}
.heart-value-wrap {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
}
.heart-value {
  font-size: 108rpx;
  font-weight: 800;
  color: #FFFFFF;
  line-height: 1;
  letter-spacing: -2rpx;
  text-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.15);
}
.heart-unit {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.85);
  font-weight: 600;
  letter-spacing: 2rpx;
}
.status-badge {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 24rpx;
  border-radius: 24rpx;
  margin-top: 20rpx;
  backdrop-filter: blur(8px);
}
.status-badge-normal {
  background: rgba(0, 180, 42, 0.25);
}
.status-badge-warning {
  background: rgba(255, 154, 46, 0.25);
}
.status-badge-danger {
  background: rgba(245, 63, 63, 0.25);
}
.status-badge-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
}
.status-badge-normal .status-badge-dot {
  background: #4DC580;
  box-shadow: 0 0 12rpx rgba(0, 180, 42, 0.6);
}
.status-badge-warning .status-badge-dot {
  background: #FFCF8B;
  box-shadow: 0 0 12rpx rgba(255, 154, 46, 0.6);
}
.status-badge-danger .status-badge-dot {
  background: #FF7D7D;
  box-shadow: 0 0 12rpx rgba(245, 63, 63, 0.6);
}
.status-badge-text {
  font-size: 24rpx;
  color: #FFFFFF;
  font-weight: 600;
}
.scene-tag {
  margin-top: 12rpx;
  padding: 6rpx 20rpx;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.15);
  border: 1rpx solid rgba(255, 255, 255, 0.2);
}
.scene-tag-text {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}

/* 滚动内容 */
.scroll-body {
  height: calc(100vh - 280rpx);
  box-sizing: border-box;
}

/* 通用卡片 */
.card {
  margin: 24rpx 24rpx 0;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 28rpx 28rpx 24rpx;
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
.alert-bar {
  background: #F53F3F;
}
.device-bar {
  background: #00B42A;
}
.card-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1D2129;
}
.card-link {
  display: flex;
  align-items: center;
  gap: 4rpx;
}
.card-link-text {
  font-size: 24rpx;
  color: #86909C;
}
.card-link-arrow {
  font-size: 24rpx;
  color: #86909C;
}

/* 趋势图 */
.trend-card {
  margin-top: -24rpx;
  position: relative;
  z-index: 3;
}
.trend-chart {
  display: flex;
  align-items: flex-end;
  gap: 4rpx;
  height: 160rpx;
  padding: 0 4rpx;
}
.trend-bar-wrap {
  flex: 1;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.trend-bar {
  width: 100%;
  max-width: 20rpx;
  border-radius: 6rpx;
  min-height: 8rpx;
  transition: height 0.3s ease;
}
.trend-labels {
  display: flex;
  justify-content: space-between;
  padding: 12rpx 4rpx 0;
}
.trend-label {
  font-size: 20rpx;
  color: #C9CDD4;
}

/* 统计行 */
.stats-row {
  display: flex;
  align-items: center;
  margin: 24rpx 24rpx 0;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 28rpx 0;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.06);
}
.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}
.stat-icon-wrap {
  width: 56rpx;
  height: 56rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stat-icon-green {
  background: rgba(0, 180, 42, 0.1);
}
.stat-icon-blue {
  background: rgba(43, 111, 240, 0.1);
}
.stat-icon-red {
  background: rgba(245, 63, 63, 0.1);
}
.stat-icon-text {
  font-size: 28rpx;
  font-weight: 700;
}
.stat-icon-green .stat-icon-text {
  color: #00B42A;
}
.stat-icon-blue .stat-icon-text {
  color: #2B6FF0;
}
.stat-icon-red .stat-icon-text {
  color: #F53F3F;
}
.stat-value {
  font-size: 40rpx;
  font-weight: 800;
  color: #1D2129;
  line-height: 1;
}
.stat-label {
  font-size: 22rpx;
  color: #86909C;
  font-weight: 500;
}
.stat-divider {
  width: 1rpx;
  height: 60rpx;
  background: #E5E6EB;
}

/* 异常预警 */
.alert-item {
  display: flex;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #F2F3F5;
}
.alert-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.alert-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  margin-right: 16rpx;
  flex-shrink: 0;
}
.alert-dot-high {
  background: #F53F3F;
  box-shadow: 0 0 8rpx rgba(245, 63, 63, 0.4);
}
.alert-dot-low {
  background: #FF9A2E;
  box-shadow: 0 0 8rpx rgba(255, 154, 46, 0.4);
}
.alert-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.alert-msg {
  font-size: 26rpx;
  color: #1D2129;
  font-weight: 500;
}
.alert-time {
  font-size: 22rpx;
  color: #C9CDD4;
}
.alert-value {
  font-size: 28rpx;
  font-weight: 700;
  flex-shrink: 0;
  margin-left: 16rpx;
}
.alert-value-high {
  color: #F53F3F;
}
.alert-value-low {
  color: #FF9A2E;
}

/* 设备卡片 */
.device-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.device-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.device-icon-wrap {
  width: 80rpx;
  height: 80rpx;
  border-radius: 20rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(43, 111, 240, 0.2);
}
.device-icon-text {
  font-size: 40rpx;
  color: #FFFFFF;
}
.device-detail {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.device-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1D2129;
}
.device-status-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.device-status-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
}
.dot-online {
  background: #00B42A;
  box-shadow: 0 0 8rpx rgba(0, 180, 42, 0.4);
}
.dot-offline {
  background: #C9CDD4;
}
.device-status-text {
  font-size: 22rpx;
  color: #86909C;
}
.device-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6rpx;
}
.battery-wrap {
  display: flex;
  align-items: center;
}
.battery-body {
  width: 48rpx;
  height: 22rpx;
  border-radius: 4rpx;
  border: 2rpx solid #86909C;
  padding: 2rpx;
  box-sizing: border-box;
  position: relative;
}
.battery-level {
  height: 100%;
  border-radius: 2rpx;
  background: linear-gradient(90deg, #00B42A 0%, #4DC580 100%);
  transition: width 0.3s ease;
}
.battery-tip {
  width: 4rpx;
  height: 10rpx;
  border-radius: 0 2rpx 2rpx 0;
  background: #86909C;
  margin-left: 2rpx;
}
.battery-text {
  font-size: 20rpx;
  color: #86909C;
  font-weight: 600;
}

/* 入口卡片 */
.entry-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.entry-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.entry-icon-wrap {
  width: 72rpx;
  height: 72rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.entry-icon-orange {
  background: linear-gradient(135deg, #FF9A2E 0%, #FFCF8B 100%);
  box-shadow: 0 4rpx 16rpx rgba(255, 154, 46, 0.2);
}
.entry-icon-purple {
  background: linear-gradient(135deg, #722ED1 0%, #B37FEB 100%);
  box-shadow: 0 4rpx 16rpx rgba(114, 46, 209, 0.2);
}
.entry-icon-text {
  font-size: 36rpx;
  color: #FFFFFF;
}
.entry-info {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}
.entry-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1D2129;
}
.entry-desc {
  font-size: 22rpx;
  color: #86909C;
}
.entry-arrow {
  font-size: 32rpx;
  color: #C9CDD4;
  font-weight: 300;
}

/* 底部安全区 */
.bottom-safe {
  height: 200rpx;
}
</style>
