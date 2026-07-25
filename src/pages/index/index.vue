<template>
  <view class="page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-content">
        <view class="nav-left">
          <view class="app-logo">
            <view class="logo-icon">
              <text class="logo-text">脉</text>
            </view>
            <text class="app-name">脉安驰援</text>
          </view>
        </view>
        <view class="nav-right">
          <view class="nav-icon-btn" @tap="handleSearch">
            <text class="iconfont">🔍</text>
          </view>
          <view class="nav-icon-btn" @tap="handleMessage">
            <view class="badge-dot"></view>
            <text class="iconfont">💬</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 可滚动内容区 -->
    <scroll-view
      class="scroll-content"
      scroll-y
      :style="{ paddingTop: (statusBarHeight + 44) + 'px' }"
    >
      <!-- SOS 紧急呼救区域 -->
      <view class="sos-section">
        <view class="sos-bg"></view>
        <view class="sos-container" @tap="goRescue">
          <view class="sos-pulse-ring sos-pulse-ring-1"></view>
          <view class="sos-pulse-ring sos-pulse-ring-2"></view>
          <view class="sos-pulse-ring sos-pulse-ring-3"></view>
          <view class="sos-btn">
            <text class="sos-text">SOS</text>
            <text class="sos-label">紧急呼救</text>
          </view>
        </view>
        <text class="sos-hint">一键呼叫 · 智能匹配 · 快速救援</text>
      </view>

      <!-- 快捷功能入口 -->
      <view class="quick-actions">
        <view class="action-item" @tap="goDevice">
          <view class="action-icon action-icon-blue">
            <text class="iconfont action-icon-text">📍</text>
          </view>
          <text class="action-label">设备查询</text>
        </view>
        <view class="action-item" @tap="goRescue">
          <view class="action-icon action-icon-red">
            <text class="iconfont action-icon-text">🆘</text>
          </view>
          <text class="action-label">紧急呼救</text>
        </view>
        <view class="action-item" @tap="goHealth">
          <view class="action-icon action-icon-green">
            <text class="iconfont action-icon-text">❤️</text>
          </view>
          <text class="action-label">健康监测</text>
        </view>
        <view class="action-item" @tap="goCheckup">
          <view class="action-icon action-icon-orange">
            <text class="iconfont action-icon-text">📋</text>
          </view>
          <text class="action-label">体检分析</text>
        </view>
      </view>

      <!-- 附近急救设备 -->
      <view class="section">
        <view class="section-header">
          <view class="section-title-wrap">
            <view class="section-title-bar"></view>
            <text class="section-title">附近急救设备</text>
          </view>
          <view class="section-more" @tap="goDevice">
            <text class="section-more-text">查看全部</text>
            <text class="iconfont arrow-icon">›</text>
          </view>
        </view>
        <scroll-view class="device-scroll" scroll-x>
          <view class="device-cards">
            <view
              v-for="device in nearbyDevices"
              :key="device.id"
              class="device-card"
              @tap="goDeviceDetail(device)"
            >
              <view class="device-card-top" :class="device.type === 'fixed' ? 'device-card-fixed' : 'device-card-mobile'">
                <view class="device-type-tag">
                  <text class="device-type-text">{{ device.type === 'fixed' ? '固定' : '移动' }}</text>
                </view>
                <text class="device-category">{{ device.category }}</text>
              </view>
              <view class="device-card-body">
                <text class="device-name">{{ device.name }}</text>
                <text class="device-address">{{ device.address }}</text>
                <view class="device-status-row">
                  <view class="device-status" :class="'status-' + device.status">
                    <view class="status-dot"></view>
                    <text class="status-text">{{ statusLabel(device.status) }}</text>
                  </view>
                  <text class="device-distance">{{ device.distance || '0.8km' }}</text>
                </view>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 健康概览 -->
      <view class="section">
        <view class="section-header">
          <view class="section-title-wrap">
            <view class="section-title-bar"></view>
            <text class="section-title">健康概览</text>
          </view>
          <view class="section-more" @tap="goHealth">
            <text class="section-more-text">详情</text>
            <text class="iconfont arrow-icon">›</text>
          </view>
        </view>
        <view class="health-card" @tap="goHealth">
          <view class="health-card-bg"></view>
          <view class="health-card-content">
            <view class="health-left">
              <view class="heart-rate-main">
                <text class="heart-icon">&#x2764;</text>
                <text class="heart-value">{{ heartRateData.current }}</text>
                <text class="heart-unit">BPM</text>
              </view>
              <view class="heart-rate-status">
                <view class="status-indicator" :class="'status-' + heartRateData.status"></view>
                <text class="status-label">{{ heartStatusLabel(heartRateData.status) }}</text>
              </view>
              <view class="heart-rate-range">
                <text class="range-text">今日 {{ heartRateData.min }}-{{ heartRateData.max }} BPM</text>
              </view>
            </view>
            <view class="health-right">
              <view class="mini-chart">
                <view
                  v-for="(item, idx) in miniChartData"
                  :key="idx"
                  class="chart-bar"
                  :style="{ height: item.height + 'rpx', background: item.color }"
                ></view>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 科普推荐 -->
      <view class="section">
        <view class="section-header">
          <view class="section-title-wrap">
            <view class="section-title-bar"></view>
            <text class="section-title">科普推荐</text>
          </view>
          <view class="section-more" @tap="goScience">
            <text class="section-more-text">更多</text>
            <text class="iconfont arrow-icon">›</text>
          </view>
        </view>
        <scroll-view class="science-scroll" scroll-x>
          <view class="science-cards">
            <view
              v-for="item in scienceList"
              :key="item.id"
              class="science-card"
              @tap="goScienceDetail(item)"
            >
              <view class="science-card-cover">
                <view class="science-cover-placeholder" :class="'science-cover-' + item.category">
                  <text class="science-cover-icon">{{ categoryIcon(item.category) }}</text>
                </view>
                <view class="science-category-tag">
                  <text class="science-category-text">{{ item.categoryLabel }}</text>
                </view>
              </view>
              <view class="science-card-info">
                <text class="science-title">{{ item.title }}</text>
                <view class="science-meta">
                  <text class="science-author">{{ item.author }}</text>
                  <text class="science-views">{{ formatCount(item.viewCount) }}阅读</text>
                </view>
              </view>
            </view>
          </view>
        </scroll-view>
      </view>

      <!-- 底部安全区 -->
      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { mockFixedDevices, mockMobileDevices, mockHeartRateData, mockScienceContents } from '@/mock/data'

// 系统信息
const statusBarHeight = ref(0)
const systemInfo = uni.getSystemInfoSync()
statusBarHeight.value = systemInfo.statusBarHeight || 20

// 附近设备 - 取前5个固定 + 3个移动
const nearbyDevices = computed(() => {
  const fixed = mockFixedDevices
    .filter(d => d.status === 'available')
    .slice(0, 5)
    .map(d => ({ ...d, distance: (Math.random() * 2 + 0.3).toFixed(1) + 'km' }))
  const mobile = mockMobileDevices
    .filter(d => d.online)
    .slice(0, 3)
    .map(d => ({ ...d, distance: (Math.random() * 3 + 0.5).toFixed(1) + 'km' }))
  return [...fixed, ...mobile]
})

// 心率数据
const heartRateData = computed(() => mockHeartRateData)

// 迷你图表数据
const miniChartData = computed(() => {
  const data = mockHeartRateData.todayData
  const maxVal = Math.max(...data.map(d => d.value))
  const minVal = Math.min(...data.map(d => d.value))
  const range = maxVal - minVal || 1
  return data.map(item => {
    const h = ((item.value - minVal) / range) * 80 + 16
    let color = '#2B6FF0'
    if (item.value > 100) color = '#F53F3F'
    else if (item.value > 85) color = '#FF9A2E'
    else if (item.value < 60) color = '#00B42A'
    return { height: h, color }
  })
})

// 科普列表
const scienceList = computed(() => mockScienceContents.slice(0, 6))

// 状态标签
function statusLabel(status: string) {
  const map: Record<string, string> = {
    available: '可用',
    maintenance: '维护中',
    online: '在线',
    offline: '离线'
  }
  return map[status] || status
}

function heartStatusLabel(status: string) {
  const map: Record<string, string> = {
    normal: '心率正常',
    warning: '心率偏高',
    danger: '心率异常'
  }
  return map[status] || status
}

function categoryIcon(category: string) {
  const map: Record<string, string> = {
    device: 'AED',
    emergency: 'SOS',
    health: 'HR',
    exercise: 'RUN'
  }
  return map[category] || 'TIP'
}

function formatCount(count: number) {
  if (count >= 10000) return (count / 10000).toFixed(1) + 'w'
  if (count >= 1000) return (count / 1000).toFixed(1) + 'k'
  return count.toString()
}

// 导航
function goRescue() {
  uni.navigateTo({ url: '/pages/rescue/index' })
}
function goDevice() {
  uni.switchTab({ url: '/pages/map/index' })
}
function goHealth() {
  uni.switchTab({ url: '/pages/health/index' })
}
function goCheckup() {
  uni.navigateTo({ url: '/pages/checkup/upload' })
}
function goScience() {
  uni.navigateTo({ url: '/pages/science/index' })
}
function goDeviceDetail(device: any) {
  uni.navigateTo({ url: '/pages/device/manage?id=' + device.id })
}
function goScienceDetail(item: any) {
  uni.navigateTo({ url: '/pages/science/detail?id=' + item.id })
}
function handleSearch() {
  uni.navigateTo({ url: '/pages/device/manage' })
}
function handleMessage() {
  uni.navigateTo({ url: '/pages/mine/records' })
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
.nav-left {
  display: flex;
  align-items: center;
}
.app-logo {
  display: flex;
  align-items: center;
}
.logo-icon {
  width: 52rpx;
  height: 52rpx;
  border-radius: 14rpx;
  background: rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
}
.logo-text {
  font-size: 30rpx;
  font-weight: 700;
  color: #FFFFFF;
}
.app-name {
  font-size: 36rpx;
  font-weight: 700;
  color: #FFFFFF;
  letter-spacing: 2rpx;
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.nav-icon-btn {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.nav-icon-btn .iconfont {
  font-size: 36rpx;
  color: #FFFFFF;
}
.badge-dot {
  position: absolute;
  top: 12rpx;
  right: 12rpx;
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: #F53F3F;
  border: 2rpx solid #2B6FF0;
}

/* 滚动内容 */
.scroll-content {
  min-height: 100vh;
  box-sizing: border-box;
}

/* SOS 区域 */
.sos-section {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx 0 32rpx;
  overflow: hidden;
}
.sos-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 100%;
  background: linear-gradient(180deg, #2B6FF0 0%, rgba(43, 111, 240, 0.3) 70%, transparent 100%);
}
.sos-container {
  position: relative;
  width: 240rpx;
  height: 240rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}
.sos-pulse-ring {
  position: absolute;
  border-radius: 50%;
  border: 4rpx solid rgba(245, 63, 63, 0.4);
  animation: sosPulse 2s ease-out infinite;
}
.sos-pulse-ring-1 {
  width: 240rpx;
  height: 240rpx;
  animation-delay: 0s;
}
.sos-pulse-ring-2 {
  width: 300rpx;
  height: 300rpx;
  animation-delay: 0.5s;
}
.sos-pulse-ring-3 {
  width: 360rpx;
  height: 360rpx;
  animation-delay: 1s;
}
@keyframes sosPulse {
  0% {
    transform: scale(0.8);
    opacity: 1;
  }
  100% {
    transform: scale(1.4);
    opacity: 0;
  }
}
.sos-btn {
  width: 180rpx;
  height: 180rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #F53F3F 0%, #E02020 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 32rpx rgba(245, 63, 63, 0.5);
  z-index: 3;
}
.sos-text {
  font-size: 56rpx;
  font-weight: 800;
  color: #FFFFFF;
  letter-spacing: 4rpx;
  line-height: 1;
}
.sos-label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.9);
  margin-top: 4rpx;
}
.sos-hint {
  position: relative;
  z-index: 2;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 24rpx;
  letter-spacing: 2rpx;
}

/* 快捷功能 */
.quick-actions {
  display: flex;
  justify-content: space-between;
  padding: 32rpx 40rpx;
  margin: 0 24rpx;
  background: #FFFFFF;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.08);
  margin-top: -16rpx;
  position: relative;
  z-index: 3;
}
.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}
.action-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.action-icon-blue {
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
}
.action-icon-red {
  background: linear-gradient(135deg, #F53F3F 0%, #FF7D7D 100%);
}
.action-icon-green {
  background: linear-gradient(135deg, #00B42A 0%, #4DC580 100%);
}
.action-icon-orange {
  background: linear-gradient(135deg, #FF9A2E 0%, #FFCF8B 100%);
}
.action-icon-text {
  font-size: 40rpx;
  color: #FFFFFF;
}
.action-label {
  font-size: 24rpx;
  color: #4E5969;
  font-weight: 500;
}

/* 通用 section */
.section {
  padding: 32rpx 24rpx 0;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}
.section-title-wrap {
  display: flex;
  align-items: center;
}
.section-title-bar {
  width: 6rpx;
  height: 32rpx;
  border-radius: 3rpx;
  background: #2B6FF0;
  margin-right: 12rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1D2129;
}
.section-more {
  display: flex;
  align-items: center;
  gap: 4rpx;
}
.section-more-text {
  font-size: 24rpx;
  color: #86909C;
}
.arrow-icon {
  font-size: 24rpx;
  color: #86909C;
}

/* 设备卡片横向滚动 */
.device-scroll {
  white-space: nowrap;
}
.device-cards {
  display: inline-flex;
  gap: 20rpx;
  padding-bottom: 8rpx;
}
.device-card {
  display: inline-flex;
  flex-direction: column;
  width: 320rpx;
  background: #FFFFFF;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
}
.device-card-top {
  height: 120rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
  position: relative;
}
.device-card-fixed {
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
}
.device-card-mobile {
  background: linear-gradient(135deg, #00B42A 0%, #4DC580 100%);
}
.device-type-tag {
  background: rgba(255, 255, 255, 0.25);
  border-radius: 8rpx;
  padding: 4rpx 16rpx;
}
.device-type-text {
  font-size: 22rpx;
  color: #FFFFFF;
  font-weight: 500;
}
.device-category {
  font-size: 28rpx;
  color: #FFFFFF;
  font-weight: 700;
}
.device-card-body {
  padding: 20rpx 24rpx 24rpx;
}
.device-name {
  font-size: 26rpx;
  color: #1D2129;
  font-weight: 600;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: normal;
}
.device-address {
  font-size: 22rpx;
  color: #86909C;
  margin-top: 8rpx;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: normal;
  line-height: 1.4;
}
.device-status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;
}
.device-status {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.status-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
}
.status-available .status-dot,
.status-online .status-dot {
  background: #00B42A;
}
.status-maintenance .status-dot,
.status-offline .status-dot {
  background: #F53F3F;
}
.status-text {
  font-size: 22rpx;
  color: #4E5969;
}
.device-distance {
  font-size: 22rpx;
  color: #2B6FF0;
  font-weight: 600;
}

/* 健康概览卡片 */
.health-card {
  position: relative;
  background: #FFFFFF;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.1);
}
.health-card-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #2B6FF0 0%, #1A4FD0 50%, #0D3AAF 100%);
  opacity: 0.03;
}
.health-card-content {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx;
}
.health-left {
  display: flex;
  flex-direction: column;
}
.heart-rate-main {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
}
.heart-icon {
  font-size: 40rpx;
  color: #F53F3F;
  animation: heartBeat 1.2s ease-in-out infinite;
}
@keyframes heartBeat {
  0%, 100% { transform: scale(1); }
  15% { transform: scale(1.15); }
  30% { transform: scale(1); }
  45% { transform: scale(1.1); }
  60% { transform: scale(1); }
}
.heart-value {
  font-size: 72rpx;
  font-weight: 800;
  color: #1D2129;
  line-height: 1;
}
.heart-unit {
  font-size: 24rpx;
  color: #86909C;
  margin-left: 4rpx;
}
.heart-rate-status {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 12rpx;
}
.status-indicator {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
}
.status-indicator.status-normal {
  background: #00B42A;
  box-shadow: 0 0 8rpx rgba(0, 180, 42, 0.4);
}
.status-indicator.status-warning {
  background: #FF9A2E;
  box-shadow: 0 0 8rpx rgba(255, 154, 46, 0.4);
}
.status-indicator.status-danger {
  background: #F53F3F;
  box-shadow: 0 0 8rpx rgba(245, 63, 63, 0.4);
}
.status-label {
  font-size: 26rpx;
  color: #4E5969;
  font-weight: 500;
}
.heart-rate-range {
  margin-top: 8rpx;
}
.range-text {
  font-size: 22rpx;
  color: #86909C;
}

/* 迷你图表 */
.health-right {
  display: flex;
  align-items: flex-end;
}
.mini-chart {
  display: flex;
  align-items: flex-end;
  gap: 4rpx;
  height: 100rpx;
  padding: 0 8rpx;
}
.chart-bar {
  width: 8rpx;
  border-radius: 4rpx;
  min-height: 8rpx;
  transition: height 0.3s ease;
}

/* 科普推荐 */
.science-scroll {
  white-space: nowrap;
}
.science-cards {
  display: inline-flex;
  gap: 20rpx;
  padding-bottom: 8rpx;
}
.science-card {
  display: inline-flex;
  flex-direction: column;
  width: 280rpx;
  background: #FFFFFF;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
}
.science-card-cover {
  position: relative;
  height: 160rpx;
}
.science-cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.science-cover-device {
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
}
.science-cover-emergency {
  background: linear-gradient(135deg, #F53F3F 0%, #FF7D7D 100%);
}
.science-cover-health {
  background: linear-gradient(135deg, #00B42A 0%, #4DC580 100%);
}
.science-cover-exercise {
  background: linear-gradient(135deg, #FF9A2E 0%, #FFCF8B 100%);
}
.science-cover-icon {
  font-size: 48rpx;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 2rpx;
}
.science-category-tag {
  position: absolute;
  top: 16rpx;
  left: 16rpx;
  background: rgba(0, 0, 0, 0.4);
  border-radius: 8rpx;
  padding: 4rpx 12rpx;
}
.science-category-text {
  font-size: 20rpx;
  color: #FFFFFF;
}
.science-card-info {
  padding: 20rpx;
}
.science-title {
  font-size: 26rpx;
  color: #1D2129;
  font-weight: 600;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: normal;
  line-height: 1.5;
}
.science-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12rpx;
}
.science-author {
  font-size: 20rpx;
  color: #86909C;
}
.science-views {
  font-size: 20rpx;
  color: #C9CDD4;
}

/* 底部安全区 */
.bottom-safe {
  height: 180rpx;
}
</style>
