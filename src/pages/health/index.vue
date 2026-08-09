<template>
  <view class="page apple-page">
    <view class="health-mast" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="mast-content">
        <view class="mast-copy">
          <text class="mast-title">健康</text>
          <text class="mast-caption">今日生命体征</text>
        </view>
        <view class="mast-device-action" aria-label="查看健康设备" @tap="goBind">
          <text>设备</text>
          <app-icon name="right" :size="14" color="#007AFF" />
        </view>
      </view>
    </view>

    <scroll-view class="page-scroll" scroll-y :show-scrollbar="false">
      <view class="signal-panel">
        <view class="signal-header">
          <view>
            <text class="signal-kicker">生命线</text>
            <text class="signal-title">心率</text>
          </view>
        </view>

        <view class="reading-row">
          <view class="reading-main">
            <app-icon-tile name="heart-filled" tone="coral" />
            <text class="reading-value">{{ displayHeartRate }}</text>
            <text class="reading-unit">BPM</text>
          </view>
          <text class="reading-context">{{ sceneLabel }}</text>
        </view>

        <view class="chart-header">
          <view>
            <text class="chart-title">{{ trendTitle }}</text>
            <view class="range-switch" aria-label="心率趋势时间范围">
              <text
                class="range-option"
                :class="{ 'range-option-active': activeRange === 'hour' }"
                @tap.stop="selectRange('hour')"
              >1小时</text>
              <text
                class="range-option"
                :class="{ 'range-option-active': activeRange === 'day' }"
                @tap.stop="selectRange('day')"
              >24小时</text>
            </view>
          </view>
          <view class="chart-detail" @tap="goDetail">
              <text>完整记录</text>
            <app-icon name="right" :size="13" color="#46627D" />
          </view>
        </view>

        <view class="trend-chart" :class="{ 'trend-chart-switching': trendSwitching }">
          <canvas canvas-id="healthTrendCanvas" id="healthTrendCanvas" class="trend-canvas"></canvas>
          <view v-if="!hasActiveTrend" class="trend-empty">
            <text class="trend-empty-text">暂无心率记录</text>
            <text class="trend-empty-hint">连接穿戴设备后自动同步</text>
          </view>
        </view>
        <view v-if="hasActiveTrend" class="trend-labels">
          <text v-for="label in trendLabels" :key="label">{{ label }}</text>
        </view>

        <view class="metric-row">
          <view class="metric">
            <text class="metric-label">最低</text>
            <text class="metric-value">{{ activeMetrics.min || '--' }}</text>
          </view>
          <view class="metric">
            <text class="metric-label">平均</text>
            <text class="metric-value">{{ activeMetrics.avg || '--' }}</text>
          </view>
          <view class="metric">
            <text class="metric-label">最高</text>
            <text
              class="metric-value"
              :class="{ 'metric-value-alert': activeMetrics.max > heartRateData.maxHeartRate }"
            >{{ activeMetrics.max || '--' }}</text>
          </view>
        </view>
      </view>

      <view v-if="latestAlert" class="alert-strip" @tap="goAlert">
        <view class="alert-signal">
          <view class="alert-dot"></view>
          <view class="alert-copy">
            <text class="alert-label">最近预警</text>
            <text class="alert-message">{{ latestAlert.message }}</text>
          </view>
        </view>
        <view class="alert-meta">
          <text>{{ heartRateData.alerts.length }}条</text>
          <app-icon name="right" :size="14" color="#9A4A50" />
        </view>
      </view>

      <view class="resource-section">
        <view class="section-head">
          <text class="section-title">健康资料</text>
        </view>

        <view class="device-card" @tap="goBind">
          <app-icon-tile
            class="device-mark"
            name="wearable"
            tone="coral"
            :status="isDeviceConnected ? 'online' : 'offline'"
          />
          <view class="device-copy">
            <view class="device-heading">
              <text class="resource-title">穿戴设备</text>
              <view class="device-state" :class="{ 'device-state-live': isDeviceConnected }">
                <view class="device-state-dot"></view>
                <text>{{ isDeviceConnected ? '已连接' : '未连接' }}</text>
              </view>
            </view>
            <text class="resource-desc">
              {{ isDeviceConnected ? heartRateData.wearable.name : '连接手环或手表，同步实时心率' }}
            </text>
          </view>
          <view class="device-action">
            <text>{{ isDeviceConnected ? '管理' : '连接' }}</text>
            <app-icon name="right" :size="14" color="#2E6DD1" />
          </view>
        </view>

        <view class="resource-list">
          <view class="resource-row" @tap="goCheckup">
            <app-icon-tile name="list" tone="violet" />
            <view class="resource-copy">
              <text class="resource-title">体检报告</text>
              <text class="resource-desc">
                {{ latestReport ? `${latestAbnormalCount} 项需关注` : '录入后查看健康分析' }}
              </text>
            </view>
            <view class="resource-trailing">
              <text class="resource-value">{{ latestReport ? latestReport.checkupDate : '录入' }}</text>
              <app-icon name="right" :size="14" color="#C7C7CC" />
            </view>
          </view>

          <view class="resource-row" @tap="goArchive">
            <app-icon-tile name="folder-add-filled" tone="blue" />
            <view class="resource-copy">
              <text class="resource-title">健康档案</text>
              <text class="resource-desc">按时间查看历次体检记录</text>
            </view>
            <view class="resource-trailing">
              <text class="resource-value">{{ healthReports.length }} 份</text>
              <app-icon name="right" :size="14" color="#C7C7CC" />
            </view>
          </view>
        </view>
      </view>

      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import { useHealthMonitoring } from '@/composables/useHealthMonitoring'
import { listHealthReports, type HealthReportResponse } from '@/api/reports'

const { monitoring: heartRateData, loadMonitoring } = useHealthMonitoring()
const healthReports = ref<HealthReportResponse[]>([])
const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)
const activeRange = ref<'hour' | 'day'>('day')
const trendSwitching = ref(false)

const isDeviceConnected = computed(() => heartRateData.value.wearable.connected)
const latestAlert = computed(() => heartRateData.value.alerts[0] || null)
const latestReport = computed(() => healthReports.value[0] || null)
const latestAbnormalCount = computed(() =>
  latestReport.value?.indicators.filter(indicator => indicator.abnormal).length || 0
)
const displayHeartRate = computed(() =>
  isDeviceConnected.value && heartRateData.value.current > 0
    ? heartRateData.value.current
    : '--'
)
const sceneLabel = computed(() => {
  if (!isDeviceConnected.value) return '连接设备后显示实时心率'
  const labels: Record<string, string> = {
    resting: '静息状态',
    exercise: '运动状态',
    sleeping: '睡眠状态'
  }
  return labels[heartRateData.value.scene] || '实时监测'
})
const trendTitle = computed(() => activeRange.value === 'hour' ? '近 1 小时' : '近 24 小时')
const activeTrendData = computed(() =>
  activeRange.value === 'hour' ? heartRateData.value.hourData : heartRateData.value.todayData
)
const trendLabels = computed(() => activeRange.value === 'hour'
  ? ['60分钟前', '45分钟前', '30分钟前', '15分钟前', '现在']
  : ['24小时前', '18小时前', '12小时前', '6小时前', '现在']
)
const activeMetrics = computed(() => {
  if (activeRange.value === 'day') {
    return { min: heartRateData.value.min, avg: heartRateData.value.avg, max: heartRateData.value.max }
  }
  const values = activeTrendData.value.map(point => point.value)
  if (!values.length) return { min: 0, avg: 0, max: 0 }
  return {
    min: Math.min(...values),
    avg: Math.round(values.reduce((sum, value) => sum + value, 0) / values.length),
    max: Math.max(...values)
  }
})
const hasActiveTrend = computed(() => activeTrendData.value.length >= 2)
let chartRenderVersion = 0

async function drawTrendChart() {
  const renderVersion = ++chartRenderVersion
  const data = [...activeTrendData.value]
  const rect = await new Promise<any>(resolve => {
    uni.createSelectorQuery().select('#healthTrendCanvas').boundingClientRect(resolve).exec()
  })
  if (renderVersion !== chartRenderVersion || !rect?.width || !rect?.height) return

  const canvas = uni.createCanvasContext('healthTrendCanvas')
  const width = rect.width
  const height = rect.height
  canvas.clearRect(0, 0, width, height)
  if (data.length < 2) {
    canvas.draw()
    return
  }
  const top = 8
  const bottom = 8
  const chartHeight = height - top - bottom
  const maxValue = 135
  const minValue = 45
  const valueRange = maxValue - minValue
  const x = (index: number) => index * width / (data.length - 1)
  const y = (value: number) => top + (maxValue - value) / valueRange * chartHeight

  canvas.setFillStyle('rgba(36, 150, 107, 0.07)')
  canvas.fillRect(0, y(100), width, y(60) - y(100))
  canvas.setStrokeStyle('#DDE7EE')
  canvas.setLineWidth(1)
  ;[60, 80, 100, 120].forEach(value => {
    canvas.beginPath()
    canvas.moveTo(0, y(value))
    canvas.lineTo(width, y(value))
    canvas.stroke()
  })

  canvas.beginPath()
  canvas.moveTo(x(0), y(data[0].value))
  for (let index = 1; index < data.length; index++) {
    const controlX = (x(index - 1) + x(index)) / 2
    canvas.bezierCurveTo(controlX, y(data[index - 1].value), controlX, y(data[index].value), x(index), y(data[index].value))
  }
  canvas.lineTo(width, height)
  canvas.lineTo(0, height)
  canvas.closePath()
  const fill = canvas.createLinearGradient(0, 0, 0, height)
  fill.addColorStop(0, 'rgba(77, 151, 220, 0.24)')
  fill.addColorStop(1, 'rgba(77, 151, 220, 0.02)')
  canvas.setFillStyle(fill)
  canvas.fill()

  for (let index = 0; index < data.length - 1; index++) {
    const high = Math.max(data[index].value, data[index + 1].value) > heartRateData.value.maxHeartRate
    const controlX = (x(index) + x(index + 1)) / 2
    canvas.beginPath()
    canvas.setStrokeStyle(high ? '#EF4D5D' : '#249C6B')
    canvas.setLineWidth(2.6)
    canvas.moveTo(x(index), y(data[index].value))
    canvas.bezierCurveTo(controlX, y(data[index].value), controlX, y(data[index + 1].value), x(index + 1), y(data[index + 1].value))
    canvas.stroke()
  }
  canvas.draw()
}

async function selectRange(range: 'hour' | 'day') {
  if (range === activeRange.value) return
  trendSwitching.value = true
  activeRange.value = range
  await nextTick()
  await drawTrendChart()
  setTimeout(() => {
    trendSwitching.value = false
  }, 80)
}

onMounted(async () => {
  await loadHealthData()
})

// 健康页是 tab 页：切回时重新拉数据，保证新绑定设备、新预警立即可见。
onShow(() => {
  if (initializedRef) loadHealthData()
})

let initializedRef = false

async function loadHealthData() {
  const [monitoring, reports] = await Promise.allSettled([
    loadMonitoring(),
    listHealthReports()
  ])
  if (monitoring.status !== 'fulfilled') {
    uni.showToast({ title: '健康数据加载失败', icon: 'none' })
  } else {
    await nextTick()
    drawTrendChart()
  }
  if (reports.status === 'fulfilled') {
    healthReports.value = reports.value
  }
  initializedRef = true
}

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
  uni.navigateTo({
    url: latestReport.value
      ? `/pages/checkup/report?id=${encodeURIComponent(latestReport.value.id)}`
      : '/pages/checkup/upload'
  })
}

function goArchive() {
  uni.navigateTo({ url: '/pages/checkup/archive' })
}
</script>

<style lang="scss" scoped>
.page {
  height: calc(100vh - var(--window-top, 0px) - var(--window-bottom, 0px));
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #F2F4F7;
  color: #111820;
}

.health-mast {
  flex: none;
  background: #FFFFFF;
}

.mast-content {
  display: flex;
  align-items: center;
  height: 126rpx;
  padding: 0 36rpx;
  gap: 20rpx;
}

.mast-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.mast-title {
  color: #10151B;
  font-size: 52rpx;
  font-weight: 760;
  line-height: 1.08;
  letter-spacing: -2rpx;
}

.mast-caption {
  margin-top: 7rpx;
  color: #8A949F;
  font-size: 18rpx;
  font-weight: 520;
}

.mast-device-action {
  display: flex;
  flex: none;
  align-items: center;
  gap: 6rpx;
  min-height: 64rpx;
  padding: 0 2rpx 0 14rpx;
  color: #007AFF;
  font-size: 24rpx;
  font-weight: 600;
  transition: opacity 150ms ease;
}

.mast-device-action:active { opacity: .48; }

.page-scroll {
  min-height: 0;
  height: 0;
  flex: 1;
  box-sizing: border-box;
}

.signal-panel {
  margin: 0;
  padding: 24rpx 32rpx 30rpx;
  border: 0;
  border-radius: 0 0 38rpx 38rpx;
  background: #FFFFFF;
}

.signal-header,
.reading-row,
.chart-header,
.metric-row,
.section-head,
.alert-strip,
.alert-signal,
.alert-meta {
  display: flex;
  align-items: center;
}

.signal-header,
.chart-header,
.section-head,
.alert-strip {
  justify-content: space-between;
}

.signal-kicker,
.signal-title,
.reading-context,
.chart-title,
.metric-label,
.metric-value,
.alert-label,
.alert-message,
.section-title,
.section-note,
.resource-title,
.resource-desc {
  display: block;
}

.signal-kicker {
  color: #2E6DD1;
  font-size: 18rpx;
  font-weight: 800;
  letter-spacing: 1rpx;
}

.signal-title {
  margin-top: 4rpx;
  font-size: 38rpx;
  font-weight: 780;
}

.reading-row {
  position: relative;
  min-height: 184rpx;
  box-sizing: border-box;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  margin-top: 20rpx;
  padding: 30rpx 32rpx;
  border-radius: 24rpx;
  background: #F2F2F7;
}

.reading-main {
  display: flex;
  align-items: center;
  z-index: 1;
  gap: 10rpx;
}

.reading-value {
  color: #111820;
  font-size: 82rpx;
  font-weight: 790;
  line-height: 0.95;
  font-variant-numeric: tabular-nums;
  letter-spacing: -2rpx;
}

.reading-unit {
  color: #75889A;
  font-size: 20rpx;
  font-weight: 700;
}

.reading-context {
  margin-top: 12rpx;
  color: #66727E;
  text-align: left;
  font-size: 20rpx;
  line-height: 1.45;
}

.chart-header {
  margin-top: 30rpx;
  padding-top: 0;
  border-top: 0;
}

.chart-title {
  font-size: 26rpx;
  font-weight: 700;
}

.range-switch {
  display: inline-flex;
  align-items: center;
  gap: 2rpx;
  margin-top: 7rpx;
  padding: 3rpx;
  border-radius: 9rpx;
  background: #EDF2F6;
}

.range-option {
  padding: 5rpx 12rpx;
  border-radius: 7rpx;
  color: #7A8C9D;
  font-size: 18rpx;
  line-height: 1.25;
  transition: color 180ms ease, background-color 180ms ease, box-shadow 180ms ease;
}

.range-option-active {
  background: #FFFFFF;
  box-shadow: 0 1rpx 5rpx rgba(35, 63, 89, 0.12);
  color: #245FAE;
  font-weight: 650;
}

.chart-detail {
  display: flex;
  align-items: center;
  gap: 3rpx;
  color: #46627D;
  font-size: 20rpx;
  font-weight: 650;
}

.trend-chart {
  position: relative;
  height: 190rpx;
  margin: 18rpx -8rpx 0;
  overflow: hidden;
  opacity: 1;
  transform: translateY(0);
  transition: opacity 180ms ease, transform 180ms ease;
}
.trend-chart-switching { opacity: 0.35; transform: translateY(3rpx); }
.trend-canvas { width: 100%; height: 190rpx; }

.trend-empty {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
}
.trend-empty-text { color: #68758A; font-size: 25rpx; font-weight: 600; }
.trend-empty-hint { color: #A8B4C4; font-size: 21rpx; }

.trend-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 10rpx;
  color: #9AA8B5;
  font-size: 18rpx;
}

.metric-row {
  margin-top: 18rpx;
  padding: 22rpx 4rpx 0;
  border-top: 1rpx solid #E6EDF2;
}

.metric {
  flex: 1;
}

.metric + .metric {
  padding-left: 22rpx;
  border-left: 1rpx solid #E6EDF2;
}

.metric-label {
  color: #7E8F9F;
  font-size: 18rpx;
}

.metric-value {
  margin-top: 5rpx;
  color: #1B3348;
  font-size: 30rpx;
  font-weight: 730;
  font-variant-numeric: tabular-nums;
}

.metric-value-alert {
  color: #B52832;
}

.alert-strip {
  margin: 22rpx 24rpx 0;
  padding: 24rpx 26rpx;
  border: 0;
  border-radius: 24rpx;
  background: #FFF1F2;
}

.alert-signal {
  min-width: 0;
}

.alert-dot {
  flex: none;
  width: 12rpx;
  height: 12rpx;
  margin-right: 16rpx;
  border: 4rpx solid #F6DADD;
  border-radius: 50%;
  background: #C53540;
}

.alert-copy {
  min-width: 0;
}

.alert-label {
  color: #9A4A50;
  font-size: 18rpx;
  font-weight: 700;
}

.alert-message {
  max-width: 450rpx;
  margin-top: 3rpx;
  overflow: hidden;
  color: #3A3033;
  font-size: 23rpx;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alert-meta {
  flex: none;
  gap: 4rpx;
  margin-left: 12rpx;
  color: #9A4A50;
  font-size: 19rpx;
}

.resource-section {
  margin: 34rpx 24rpx 0;
}

.section-head {
  padding: 0 4rpx 14rpx;
}

.section-title {
  color: #111820;
  font-size: 34rpx;
  font-weight: 780;
}

.section-note {
  color: #8797A6;
  font-size: 19rpx;
}

.device-card {
  display: flex;
  align-items: center;
  min-height: 128rpx;
  padding: 24rpx;
  border: 0;
  border: 0;
  border-radius: 26rpx;
  background: #FFFFFF;
}

.device-card:active,
.resource-row:active {
  background: #F0F6FC;
}

.device-copy {
  min-width: 0;
  flex: 1;
  margin-left: 20rpx;
}

.device-heading {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.resource-title {
  font-size: 24rpx;
  font-weight: 720;
}

.device-state {
  display: flex;
  align-items: center;
  gap: 6rpx;
  color: #75879A;
  font-size: 17rpx;
}

.device-state-dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #A6B1BC;
}

.device-state-live { color: #287858; }
.device-state-live .device-state-dot { background: #23956A; }

.resource-desc {
  margin-top: 5rpx;
  overflow: hidden;
  color: #7D8E9E;
  font-size: 18rpx;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-action {
  display: flex;
  flex: none;
  align-items: center;
  gap: 3rpx;
  margin-left: 14rpx;
  color: #2E6DD1;
  font-size: 18rpx;
  font-weight: 650;
}

.resource-list {
  margin-top: 16rpx;
  overflow: hidden;
  border-radius: 26rpx;
  background: #FFFFFF;
}

.resource-row {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 116rpx;
  box-sizing: border-box;
  gap: 18rpx;
  padding: 20rpx 22rpx;
  background: #FFFFFF;
  transition: background-color 150ms ease, opacity 150ms ease;
}

.resource-row + .resource-row::before {
  position: absolute;
  top: 0;
  right: 22rpx;
  left: 86rpx;
  height: 1rpx;
  background: rgba(60, 60, 67, .14);
  content: '';
}

.resource-copy {
  min-width: 0;
  flex: 1;
}

.resource-trailing {
  display: flex;
  flex: none;
  align-items: center;
  gap: 6rpx;
}

.resource-value {
  color: #8E8E93;
  font-size: 20rpx;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
}

.bottom-safe {
  height: calc(72rpx + env(safe-area-inset-bottom));
}

</style>
