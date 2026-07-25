<template>
  <view class="page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-content">
        <view class="nav-back" @tap="goBack">
          <text class="back-arrow">&#x2190;</text>
        </view>
        <text class="nav-title">心率详情</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <scroll-view
      class="scroll-content"
      scroll-y
      :style="{ paddingTop: (statusBarHeight + 44) + 'px' }"
    >
      <!-- Tab切换 -->
      <view class="tab-switcher">
        <view
          class="tab-item"
          :class="{ 'tab-active': activeTab === 'today' }"
          @tap="activeTab = 'today'"
        >
          <text class="tab-text">今日</text>
        </view>
        <view
          class="tab-item"
          :class="{ 'tab-active': activeTab === 'week' }"
          @tap="activeTab = 'week'"
        >
          <text class="tab-text">本周</text>
        </view>
        <view class="tab-indicator" :class="{ 'tab-indicator-right': activeTab === 'week' }"></view>
      </view>

      <!-- 今日视图 -->
      <view v-if="activeTab === 'today'" class="chart-section">
        <view class="chart-container">
          <canvas
            canvas-id="heartRateCanvas"
            id="heartRateCanvas"
            class="chart-canvas"
            :style="{ width: '100%', height: '400rpx' }"
          ></canvas>
        </view>
        <!-- 场景标签 -->
        <view class="scene-labels">
          <view class="scene-label-item">
            <view class="scene-dot scene-dot-sleep"></view>
            <text class="scene-label-text">睡眠 0:00-5:00</text>
          </view>
          <view class="scene-label-item">
            <view class="scene-dot scene-dot-rest"></view>
            <text class="scene-label-text">静息 6:00-8:00</text>
          </view>
          <view class="scene-label-item">
            <view class="scene-dot scene-dot-exercise"></view>
            <text class="scene-label-text">运动 9:00-11:00</text>
          </view>
          <view class="scene-label-item">
            <view class="scene-dot scene-dot-rest"></view>
            <text class="scene-label-text">静息 12:00-16:00</text>
          </view>
          <view class="scene-label-item">
            <view class="scene-dot scene-dot-exercise"></view>
            <text class="scene-label-text">运动 17:00-18:00</text>
          </view>
          <view class="scene-label-item">
            <view class="scene-dot scene-dot-rest"></view>
            <text class="scene-label-text">静息 19:00-22:00</text>
          </view>
        </view>
      </view>

      <!-- 本周视图 -->
      <view v-if="activeTab === 'week'" class="chart-section">
        <view class="week-chart">
          <view
            v-for="(item, idx) in weekBars"
            :key="idx"
            class="week-bar-group"
          >
            <view class="week-bar-range">
              <view
                class="week-bar-fill"
                :style="{
                  top: item.rangeTop + 'rpx',
                  height: item.rangeHeight + 'rpx',
                  background: item.barColor
                }"
              ></view>
              <view class="week-bar-avg" :style="{ top: item.avgTop + 'rpx' }"></view>
            </view>
            <text class="week-bar-label">{{ item.dateLabel }}</text>
          </view>
        </view>
        <view class="week-legend">
          <view class="legend-item">
            <view class="legend-bar legend-bar-range"></view>
            <text class="legend-text">心率范围</text>
          </view>
          <view class="legend-item">
            <view class="legend-bar legend-bar-avg"></view>
            <text class="legend-text">平均心率</text>
          </view>
        </view>
      </view>

      <!-- 统计卡片 -->
      <view class="stats-grid">
        <view class="stat-card">
          <text class="stat-value">{{ heartRateData.avg }}</text>
          <text class="stat-label">平均心率</text>
          <text class="stat-unit">BPM</text>
        </view>
        <view class="stat-card">
          <text class="stat-value stat-value-green">{{ heartRateData.min }}</text>
          <text class="stat-label">最低心率</text>
          <text class="stat-unit">BPM</text>
        </view>
        <view class="stat-card">
          <text class="stat-value stat-value-red">{{ heartRateData.max }}</text>
          <text class="stat-label">最高心率</text>
          <text class="stat-unit">BPM</text>
        </view>
        <view class="stat-card">
          <text class="stat-value stat-value-orange">{{ heartRateData.alerts.length }}</text>
          <text class="stat-label">异常次数</text>
          <text class="stat-unit">次</text>
        </view>
      </view>

      <!-- 场景分析 -->
      <view class="card scene-card">
        <view class="card-header">
          <view class="card-title-wrap">
            <view class="card-title-bar"></view>
            <text class="card-title">场景心率分析</text>
          </view>
        </view>
        <view class="scene-breakdown">
          <view class="scene-row">
            <view class="scene-row-left">
              <view class="scene-row-icon scene-icon-blue">
                <text class="scene-icon-text">~</text>
              </view>
              <view class="scene-row-info">
                <text class="scene-row-label">静息心率</text>
                <text class="scene-row-desc">日常活动期间</text>
              </view>
            </view>
            <view class="scene-row-value">
              <text class="scene-row-number">{{ restingAvg }}</text>
              <text class="scene-row-unit">BPM</text>
            </view>
          </view>
          <view class="scene-row">
            <view class="scene-row-left">
              <view class="scene-row-icon scene-icon-orange">
                <text class="scene-icon-text">!</text>
              </view>
              <view class="scene-row-info">
                <text class="scene-row-label">运动心率</text>
                <text class="scene-row-desc">运动期间</text>
              </view>
            </view>
            <view class="scene-row-value">
              <text class="scene-row-number scene-row-number-high">{{ exerciseAvg }}</text>
              <text class="scene-row-unit">BPM</text>
            </view>
          </view>
          <view class="scene-row">
            <view class="scene-row-left">
              <view class="scene-row-icon scene-icon-purple">
                <text class="scene-icon-text">z</text>
              </view>
              <view class="scene-row-info">
                <text class="scene-row-label">睡眠心率</text>
                <text class="scene-row-desc">睡眠期间</text>
              </view>
            </view>
            <view class="scene-row-value">
              <text class="scene-row-number scene-row-number-low">{{ sleepAvg }}</text>
              <text class="scene-row-unit">BPM</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 预警历史 -->
      <view class="card alert-card">
        <view class="card-header">
          <view class="card-title-wrap">
            <view class="card-title-bar alert-bar"></view>
            <text class="card-title">预警记录</text>
          </view>
        </view>
        <view class="alert-timeline">
          <view
            v-for="(alert, idx) in heartRateData.alerts"
            :key="idx"
            class="timeline-item"
          >
            <view class="timeline-dot" :class="alert.type === 'high' ? 'dot-high' : 'dot-low'"></view>
            <view v-if="idx < heartRateData.alerts.length - 1" class="timeline-line"></view>
            <view class="timeline-content">
              <view class="timeline-header">
                <text class="timeline-type" :class="alert.type === 'high' ? 'type-high' : 'type-low'">
                  {{ alert.type === 'high' ? '心率偏高' : '心率偏低' }}
                </text>
                <text class="timeline-value">{{ alert.value }} BPM</text>
              </view>
              <text class="timeline-msg">{{ alert.message }}</text>
              <text class="timeline-time">{{ alert.time }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 底部安全区 -->
      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { mockHeartRateData } from '@/mock/data'

// 系统信息
const statusBarHeight = ref(0)
const systemInfo = uni.getSystemInfoSync()
statusBarHeight.value = systemInfo.statusBarHeight || 20

const heartRateData = computed(() => mockHeartRateData)
const activeTab = ref<'today' | 'week'>('today')

// 场景平均心率
const restingAvg = computed(() => {
  const items = heartRateData.value.todayData.filter(d => d.scene === 'resting')
  if (!items.length) return 0
  return Math.round(items.reduce((s, d) => s + d.value, 0) / items.length)
})

const exerciseAvg = computed(() => {
  const items = heartRateData.value.todayData.filter(d => d.scene === 'exercise')
  if (!items.length) return 0
  return Math.round(items.reduce((s, d) => s + d.value, 0) / items.length)
})

const sleepAvg = computed(() => {
  const items = heartRateData.value.todayData.filter(d => d.scene === 'sleeping')
  if (!items.length) return 0
  return Math.round(items.reduce((s, d) => s + d.value, 0) / items.length)
})

// 本周柱状图数据
const weekBars = computed(() => {
  const data = heartRateData.value.weekData
  const chartHeight = 280
  const globalMax = 130
  const globalMin = 40
  const range = globalMax - globalMin

  return data.map(item => {
    const rangeTop = ((globalMax - item.max) / range) * chartHeight
    const rangeBottom = ((globalMax - item.min) / range) * chartHeight
    const rangeHeight = rangeBottom - rangeTop
    const avgTop = ((globalMax - item.avg) / range) * chartHeight

    let barColor = 'linear-gradient(180deg, #2B6FF0 0%, #5B8DEF 100%)'
    if (item.max > 110) barColor = 'linear-gradient(180deg, #F53F3F 0%, #FF7D7D 100%)'
    else if (item.max > 100) barColor = 'linear-gradient(180deg, #FF9A2E 0%, #FFCF8B 100%)'

    return {
      dateLabel: item.date.slice(5),
      rangeTop,
      rangeHeight,
      avgTop,
      barColor
    }
  })
})

// 绘制今日心率曲线
function drawTodayChart() {
  const data = heartRateData.value.todayData
  const canvas = uni.createCanvasContext('heartRateCanvas')

  const dpr = systemInfo.pixelRatio || 2
  // 使用固定逻辑尺寸
  const cw = 700
  const ch = 400
  const padLeft = 50
  const padRight = 20
  const padTop = 30
  const padBottom = 40
  const chartW = cw - padLeft - padRight
  const chartH = ch - padTop - padBottom

  const maxVal = 130
  const minVal = 40
  const valRange = maxVal - minVal

  // 背景
  canvas.setFillStyle('#FFFFFF')
  canvas.fillRect(0, 0, cw, ch)

  // 网格线
  canvas.setStrokeStyle('#F2F3F5')
  canvas.setLineWidth(1)
  const gridLines = [60, 80, 100, 120]
  gridLines.forEach(val => {
    const y = padTop + ((maxVal - val) / valRange) * chartH
    canvas.beginPath()
    canvas.moveTo(padLeft, y)
    canvas.lineTo(cw - padRight, y)
    canvas.stroke()
    // Y轴标签
    canvas.setFontSize(18)
    canvas.setFillStyle('#C9CDD4')
    canvas.setTextAlign('right')
    canvas.fillText(val.toString(), padLeft - 8, y + 5)
  })

  // 正常范围标注
  const normalTop = padTop + ((maxVal - 100) / valRange) * chartH
  const normalBottom = padTop + ((maxVal - 60) / valRange) * chartH
  canvas.setFillStyle('rgba(0, 180, 42, 0.05)')
  canvas.fillRect(padLeft, normalTop, chartW, normalBottom - normalTop)

  // 绘制曲线 - 分段着色
  const stepX = chartW / (data.length - 1)

  function getX(i: number) { return padLeft + i * stepX }
  function getY(val: number) { return padTop + ((maxVal - val) / valRange) * chartH }

  function getLineColor(val: number) {
    if (val > 100) return '#F53F3F'
    if (val > 85) return '#FF9A2E'
    if (val < 60) return '#2B6FF0'
    return '#00B42A'
  }

  // 绘制渐变填充区域
  canvas.beginPath()
  canvas.moveTo(getX(0), getY(data[0].value))
  for (let i = 1; i < data.length; i++) {
    const cpx = (getX(i - 1) + getX(i)) / 2
    canvas.bezierCurveTo(cpx, getY(data[i - 1].value), cpx, getY(data[i].value), getX(i), getY(data[i].value))
  }
  canvas.lineTo(getX(data.length - 1), padTop + chartH)
  canvas.lineTo(getX(0), padTop + chartH)
  canvas.closePath()

  const gradient = canvas.createLinearGradient(0, padTop, 0, padTop + chartH)
  gradient.addColorStop(0, 'rgba(43, 111, 240, 0.15)')
  gradient.addColorStop(1, 'rgba(43, 111, 240, 0.01)')
  canvas.setFillStyle(gradient)
  canvas.fill()

  // 绘制曲线线条 - 分段着色
  for (let i = 0; i < data.length - 1; i++) {
    const val1 = data[i].value
    const val2 = data[i + 1].value
    const color = getLineColor(Math.max(val1, val2))

    canvas.beginPath()
    canvas.setStrokeStyle(color)
    canvas.setLineWidth(3)
    canvas.moveTo(getX(i), getY(val1))
    const cpx = (getX(i) + getX(i + 1)) / 2
    canvas.bezierCurveTo(cpx, getY(val1), cpx, getY(val2), getX(i + 1), getY(val2))
    canvas.stroke()
  }

  // 数据点
  data.forEach((item, i) => {
    if (i % 3 === 0 || item.value > 100 || item.value < 60) {
      const x = getX(i)
      const y = getY(item.value)
      const color = getLineColor(item.value)

      canvas.beginPath()
      canvas.arc(x, y, 4, 0, 2 * Math.PI)
      canvas.setFillStyle(color)
      canvas.fill()
      canvas.beginPath()
      canvas.arc(x, y, 2, 0, 2 * Math.PI)
      canvas.setFillStyle('#FFFFFF')
      canvas.fill()
    }
  })

  // X轴标签
  canvas.setFontSize(16)
  canvas.setFillStyle('#C9CDD4')
  canvas.setTextAlign('center')
  const xLabels = [0, 6, 12, 18, 23]
  xLabels.forEach(i => {
    canvas.fillText(data[i].time, getX(i), ch - 8)
  })

  canvas.draw()
}

onMounted(() => {
  nextTick(() => {
    setTimeout(() => {
      drawTodayChart()
    }, 300)
  })
})

watch(activeTab, () => {
  if (activeTab.value === 'today') {
    nextTick(() => {
      setTimeout(() => {
        drawTodayChart()
      }, 100)
    })
  }
})

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

/* Tab切换 */
.tab-switcher {
  display: flex;
  position: relative;
  margin: 24rpx 24rpx 0;
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 6rpx;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.06);
}
.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18rpx 0;
  position: relative;
  z-index: 2;
  transition: all 0.3s ease;
}
.tab-text {
  font-size: 28rpx;
  font-weight: 600;
  color: #86909C;
  transition: color 0.3s ease;
}
.tab-active .tab-text {
  color: #2B6FF0;
}
.tab-indicator {
  position: absolute;
  top: 6rpx;
  left: 6rpx;
  width: calc(50% - 6rpx);
  height: calc(100% - 12rpx);
  border-radius: 16rpx;
  background: rgba(43, 111, 240, 0.08);
  transition: left 0.3s ease;
}
.tab-indicator-right {
  left: 50%;
}

/* 图表区域 */
.chart-section {
  margin: 24rpx 24rpx 0;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.06);
}
.chart-container {
  width: 100%;
  height: 400rpx;
  position: relative;
}
.chart-canvas {
  width: 100%;
  height: 400rpx;
}

/* 场景标签 */
.scene-labels {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx 24rpx;
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #F2F3F5;
}
.scene-label-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.scene-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
}
.scene-dot-sleep {
  background: #722ED1;
}
.scene-dot-rest {
  background: #00B42A;
}
.scene-dot-exercise {
  background: #FF9A2E;
}
.scene-label-text {
  font-size: 22rpx;
  color: #86909C;
}

/* 本周柱状图 */
.week-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 320rpx;
  padding: 0 16rpx;
  position: relative;
}
.week-bar-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  position: relative;
}
.week-bar-range {
  width: 40rpx;
  height: 100%;
  position: relative;
}
.week-bar-fill {
  position: absolute;
  left: 0;
  width: 100%;
  border-radius: 8rpx;
  opacity: 0.3;
}
.week-bar-avg {
  position: absolute;
  left: -6rpx;
  width: 52rpx;
  height: 6rpx;
  border-radius: 3rpx;
  background: #2B6FF0;
  box-shadow: 0 2rpx 8rpx rgba(43, 111, 240, 0.3);
}
.week-bar-label {
  font-size: 20rpx;
  color: #86909C;
  margin-top: 12rpx;
}
.week-legend {
  display: flex;
  justify-content: center;
  gap: 32rpx;
  margin-top: 24rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #F2F3F5;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.legend-bar {
  width: 24rpx;
  height: 12rpx;
  border-radius: 3rpx;
}
.legend-bar-range {
  background: rgba(43, 111, 240, 0.3);
}
.legend-bar-avg {
  background: #2B6FF0;
}
.legend-text {
  font-size: 22rpx;
  color: #86909C;
}

/* 统计网格 */
.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx;
  margin: 24rpx 24rpx 0;
}
.stat-card {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.06);
}
.stat-value {
  font-size: 48rpx;
  font-weight: 800;
  color: #2B6FF0;
  line-height: 1.1;
}
.stat-value-green {
  color: #00B42A;
}
.stat-value-red {
  color: #F53F3F;
}
.stat-value-orange {
  color: #FF9A2E;
}
.stat-label {
  font-size: 24rpx;
  color: #86909C;
  font-weight: 500;
}
.stat-unit {
  font-size: 20rpx;
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
.alert-bar {
  background: #F53F3F;
}
.card-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1D2129;
}

/* 场景分析 */
.scene-breakdown {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.scene-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #F2F3F5;
}
.scene-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.scene-row-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.scene-row-icon {
  width: 56rpx;
  height: 56rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.scene-icon-blue {
  background: rgba(43, 111, 240, 0.1);
}
.scene-icon-orange {
  background: rgba(255, 154, 46, 0.1);
}
.scene-icon-purple {
  background: rgba(114, 46, 209, 0.1);
}
.scene-icon-text {
  font-size: 28rpx;
  font-weight: 700;
}
.scene-icon-blue .scene-icon-text {
  color: #2B6FF0;
}
.scene-icon-orange .scene-icon-text {
  color: #FF9A2E;
}
.scene-icon-purple .scene-icon-text {
  color: #722ED1;
}
.scene-row-info {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.scene-row-label {
  font-size: 28rpx;
  font-weight: 600;
  color: #1D2129;
}
.scene-row-desc {
  font-size: 22rpx;
  color: #C9CDD4;
}
.scene-row-value {
  display: flex;
  align-items: baseline;
  gap: 4rpx;
}
.scene-row-number {
  font-size: 40rpx;
  font-weight: 800;
  color: #2B6FF0;
  line-height: 1;
}
.scene-row-number-high {
  color: #FF9A2E;
}
.scene-row-number-low {
  color: #722ED1;
}
.scene-row-unit {
  font-size: 22rpx;
  color: #86909C;
}

/* 预警时间线 */
.alert-timeline {
  padding-left: 8rpx;
}
.timeline-item {
  position: relative;
  padding-left: 40rpx;
  padding-bottom: 28rpx;
}
.timeline-item:last-child {
  padding-bottom: 0;
}
.timeline-dot {
  position: absolute;
  left: 0;
  top: 8rpx;
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  border: 4rpx solid #FFFFFF;
  z-index: 2;
}
.dot-high {
  background: #F53F3F;
  box-shadow: 0 0 8rpx rgba(245, 63, 63, 0.4);
}
.dot-low {
  background: #FF9A2E;
  box-shadow: 0 0 8rpx rgba(255, 154, 46, 0.4);
}
.timeline-line {
  position: absolute;
  left: 8rpx;
  top: 28rpx;
  bottom: 0;
  width: 2rpx;
  background: #E5E6EB;
}
.timeline-content {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}
.timeline-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.timeline-type {
  font-size: 28rpx;
  font-weight: 600;
}
.type-high {
  color: #F53F3F;
}
.type-low {
  color: #FF9A2E;
}
.timeline-value {
  font-size: 28rpx;
  font-weight: 700;
  color: #1D2129;
}
.timeline-msg {
  font-size: 24rpx;
  color: #4E5969;
}
.timeline-time {
  font-size: 22rpx;
  color: #C9CDD4;
}

/* 底部安全区 */
.bottom-safe {
  height: 60rpx;
}
</style>
