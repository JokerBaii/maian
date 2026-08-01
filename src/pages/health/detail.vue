<template>
  <view class="page">
    <view class="scroll-content">
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

      <view v-if="!heartRateData.wearable.connected" class="sample-note">
        <app-icon name="info-filled" :size="15" color="#1F63D5" />
        <text>示例数据 · 连接设备后显示真实记录</text>
      </view>

      <view v-if="activeTab === 'today'" class="chart-section">
        <view class="chart-heading">
          <view>
            <text class="chart-eyebrow">24 小时心率轨迹</text>
            <view class="chart-current">
              <text class="chart-current-value">{{ heartRateData.current }}</text>
              <text class="chart-current-unit">BPM</text>
            </view>
          </view>
          <view class="normal-range">
            <view class="normal-range-dot"></view>
            <text>正常区间 60–100</text>
          </view>
        </view>
        <view class="chart-container">
          <canvas
            canvas-id="heartRateCanvas"
            id="heartRateCanvas"
            class="chart-canvas"
          ></canvas>
        </view>
        <view class="chart-axis">
          <text>00:00</text>
          <text>06:00</text>
          <text>12:00</text>
          <text>18:00</text>
          <text>23:00</text>
        </view>
        <view class="scene-labels">
          <view class="scene-label-item">
            <app-icon-tile name="cloud-filled" tone="violet" />
            <view>
              <text class="scene-label-text">睡眠</text>
              <text class="scene-label-time">00:00–05:00</text>
            </view>
          </view>
          <view class="scene-label-item">
            <app-icon-tile name="heart-filled" tone="green" />
            <view>
              <text class="scene-label-text">静息</text>
              <text class="scene-label-time">06:00–16:00</text>
            </view>
          </view>
          <view class="scene-label-item">
            <app-icon-tile name="fire-filled" tone="amber" />
            <view>
              <text class="scene-label-text">运动</text>
              <text class="scene-label-time">17:00–18:00</text>
            </view>
          </view>
        </view>
      </view>

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
              <app-icon-tile name="heart-filled" tone="green" />
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
              <app-icon-tile name="fire-filled" tone="amber" />
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
              <app-icon-tile name="cloud-filled" tone="violet" />
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

      <view class="bottom-safe"></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import { useHealthMonitoring } from '@/composables/useHealthMonitoring'

const { monitoring: heartRateData, loadMonitoring } = useHealthMonitoring()
const activeTab = ref<'today' | 'week'>('today')

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

    let barColor = 'linear-gradient(180deg, #2E6DD1 0%, #2E6DD1 100%)'
    if (item.max > 110) barColor = 'linear-gradient(180deg, #C93D46 0%, #FF7D7D 100%)'
    else if (item.max > 100) barColor = 'linear-gradient(180deg, #FF9A2E 0%, #FFCF8B 100%)'

    return {
      dateLabel: item.date.replace('-', '/'),
      rangeTop,
      rangeHeight,
      avgTop,
      barColor
    }
  })
})

async function drawTodayChart() {
  const data = heartRateData.value.todayData
  const rect = await new Promise<any>((resolve) => {
    uni.createSelectorQuery()
      .select('#heartRateCanvas')
      .boundingClientRect(resolve)
      .exec()
  })
  if (!rect?.width || !rect?.height || data.length < 2) return

  const canvas = uni.createCanvasContext('heartRateCanvas')
  const cw = rect.width
  const ch = rect.height
  const padLeft = 8
  const padRight = 8
  const padTop = 10
  const padBottom = 8
  const chartW = cw - padLeft - padRight
  const chartH = ch - padTop - padBottom
  const maxVal = 130
  const minVal = 40
  const valRange = maxVal - minVal

  canvas.setStrokeStyle('#E8EEF7')
  canvas.setLineWidth(1)
  const gridLines = [60, 80, 100, 120]
  gridLines.forEach(val => {
    const y = padTop + ((maxVal - val) / valRange) * chartH
    canvas.beginPath()
    canvas.moveTo(padLeft, y)
    canvas.lineTo(cw - padRight, y)
    canvas.stroke()
  })

  const normalTop = padTop + ((maxVal - 100) / valRange) * chartH
  const normalBottom = padTop + ((maxVal - 60) / valRange) * chartH
  canvas.setFillStyle('rgba(34, 160, 107, 0.07)')
  canvas.fillRect(padLeft, normalTop, chartW, normalBottom - normalTop)

  const stepX = chartW / (data.length - 1)
  function getX(i: number) { return padLeft + i * stepX }
  function getY(val: number) { return padTop + ((maxVal - val) / valRange) * chartH }
  function getLineColor(val: number) {
    if (val > 110) return '#F04F5F'
    if (val > 95) return '#F08C2E'
    if (val < 60) return '#4D7FF3'
    return '#22A06B'
  }

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
  gradient.addColorStop(0, 'rgba(43, 111, 240, 0.22)')
  gradient.addColorStop(1, 'rgba(43, 111, 240, 0)')
  canvas.setFillStyle(gradient)
  canvas.fill()

  for (let i = 0; i < data.length - 1; i++) {
    const val1 = data[i].value
    const val2 = data[i + 1].value
    const color = getLineColor(Math.max(val1, val2))

    canvas.beginPath()
    canvas.setStrokeStyle(color)
    canvas.setLineWidth(2.5)
    canvas.moveTo(getX(i), getY(val1))
    const cpx = (getX(i) + getX(i + 1)) / 2
    canvas.bezierCurveTo(cpx, getY(val1), cpx, getY(val2), getX(i + 1), getY(val2))
    canvas.stroke()
  }

  data.forEach((item, i) => {
    if (item.value > 110 || item.value < 55) {
      const x = getX(i)
      const y = getY(item.value)
      const color = getLineColor(item.value)

      canvas.beginPath()
      canvas.arc(x, y, 4.5, 0, 2 * Math.PI)
      canvas.setFillStyle(color)
      canvas.fill()
      canvas.beginPath()
      canvas.arc(x, y, 2, 0, 2 * Math.PI)
      canvas.setFillStyle('#FFFFFF')
      canvas.fill()
    }
  })

  canvas.draw()
}

onMounted(async () => {
  try {
    await loadMonitoring()
  } catch {
    uni.showToast({ title: '健康数据加载失败', icon: 'none' })
  }
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
  color: #2E6DD1;
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

.chart-section {
  margin: 24rpx 24rpx 0;
  background: #FFFFFF;
  border-radius: 28rpx;
  padding: 30rpx 28rpx 24rpx;
  box-shadow: 0 12rpx 40rpx rgba(32, 71, 132, 0.08);
}
.chart-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20rpx;
}
.chart-eyebrow {
  display: block;
  font-size: 23rpx;
  font-weight: 600;
  color: #748198;
}
.chart-current {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
  margin-top: 4rpx;
}
.chart-current-value {
  font-size: 52rpx;
  line-height: 1.1;
  font-weight: 800;
  color: #1C2B45;
  font-variant-numeric: tabular-nums;
}
.chart-current-unit {
  font-size: 21rpx;
  font-weight: 700;
  color: #7D8AA0;
}
.normal-range {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 10rpx 14rpx;
  border-radius: 999rpx;
  background: #ECF8F2;
  font-size: 20rpx;
  font-weight: 600;
  color: #23845D;
}
.normal-range-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #22A06B;
}
.chart-container {
  width: 100%;
  height: 320rpx;
  position: relative;
}
.chart-canvas {
  width: 100%;
  height: 320rpx;
}
.chart-axis {
  display: flex;
  justify-content: space-between;
  margin-top: 8rpx;
  font-size: 18rpx;
  color: #A6B0C0;
}

.scene-labels {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
  margin-top: 24rpx;
}
.scene-label-item {
  display: flex;
  align-items: center;
  gap: 10rpx;
  min-width: 0;
  padding: 14rpx 12rpx;
  border-radius: 16rpx;
  background: #F7F9FC;
}
.scene-label-text {
  display: block;
  font-size: 22rpx;
  font-weight: 700;
  color: #334057;
}
.scene-label-time {
  display: block;
  margin-top: 2rpx;
  font-size: 16rpx;
  color: #98A3B5;
}

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
  background: #2E6DD1;
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
  background: #2E6DD1;
}
.legend-text {
  font-size: 22rpx;
  color: #86909C;
}

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
  color: #2E6DD1;
  line-height: 1.1;
}
.stat-value-green {
  color: #23956A;
}
.stat-value-red {
  color: #C93D46;
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
.alert-bar {
  background: #C93D46;
}
.card-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #20364D;
}

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
.scene-row-info {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.scene-row-label {
  font-size: 28rpx;
  font-weight: 600;
  color: #20364D;
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
  color: #2E6DD1;
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
  background: #C93D46;
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
  color: #C93D46;
}
.type-low {
  color: #FF9A2E;
}
.timeline-value {
  font-size: 28rpx;
  font-weight: 700;
  color: #20364D;
}
.timeline-msg {
  font-size: 24rpx;
  color: #4E5969;
}
.timeline-time {
  font-size: 22rpx;
  color: #C9CDD4;
}

.bottom-safe {
  height: 60rpx;
}
</style>
