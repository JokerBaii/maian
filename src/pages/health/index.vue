<template>
  <view class="page apple-page">
    <view class="health-mast" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="mast-content">
        <view class="mast-signal" aria-hidden="true">
          <view class="mast-beat mast-beat-short"></view>
          <view class="mast-beat mast-beat-tall"></view>
          <view class="mast-beat mast-beat-short"></view>
        </view>
        <view class="mast-copy">
          <text class="mast-title">健康监测</text>
          <text class="mast-caption">HEALTH SIGNAL</text>
        </view>
        <view class="mast-state" :class="{ 'mast-state-live': isDeviceConnected }">
          <view class="mast-state-dot"></view>
          <text>{{ isDeviceConnected ? '实时守护中' : '等待设备' }}</text>
        </view>
      </view>
    </view>

    <scroll-view class="page-scroll" scroll-y :show-scrollbar="false">
      <view class="signal-panel">
        <view class="signal-header">
          <view>
            <text class="signal-kicker">HEALTH SIGNAL</text>
            <text class="signal-title">心率概览</text>
          </view>
          <view class="signal-status" :class="{ 'signal-status-live': isDeviceConnected }">
            <view class="signal-status-dot"></view>
            <text>{{ statusLabel }}</text>
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
            <text class="chart-title">今日趋势</text>
            <text class="chart-subtitle">{{ trendModeLabel }}</text>
          </view>
          <view class="chart-detail" @tap="goDetail">
            <text>查看详情</text>
            <app-icon name="right" :size="13" color="#46627D" />
          </view>
        </view>

        <view class="trend-chart">
          <canvas canvas-id="healthTrendCanvas" id="healthTrendCanvas" class="trend-canvas"></canvas>
          <view v-if="!hasTodayTrend" class="trend-empty">
            <text class="trend-empty-text">今日暂无心率记录</text>
            <text class="trend-empty-hint">连接穿戴设备后自动同步</text>
          </view>
        </view>
        <view v-if="hasTodayTrend" class="trend-labels">
          <text>0时</text>
          <text>6时</text>
          <text>12时</text>
          <text>18时</text>
          <text>23时</text>
        </view>

        <view class="metric-row">
          <view class="metric">
            <text class="metric-label">最低</text>
            <text class="metric-value">{{ heartRateData.min || '--' }}</text>
          </view>
          <view class="metric">
            <text class="metric-label">平均</text>
            <text class="metric-value">{{ heartRateData.avg || '--' }}</text>
          </view>
          <view class="metric">
            <text class="metric-label">最高</text>
            <text class="metric-value metric-value-alert">{{ heartRateData.max || '--' }}</text>
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
          <text class="section-note">设备与档案</text>
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

        <view class="resource-grid">
          <view class="resource-card" @tap="goCheckup">
            <view class="resource-card-head">
              <app-icon-tile name="list" tone="violet" />
              <app-icon name="right" :size="14" color="#8291A4" />
            </view>
            <text class="resource-title">体检报告</text>
            <text class="resource-value">{{ latestReport ? latestReport.checkupDate : '待录入' }}</text>
            <text class="resource-desc">
              {{ latestReport ? `${latestAbnormalCount}项指标需关注` : '录入后生成分析' }}
            </text>
          </view>

          <view class="resource-card" @tap="goArchive">
            <view class="resource-card-head">
              <app-icon-tile name="folder-add-filled" tone="blue" />
              <app-icon name="right" :size="14" color="#8291A4" />
            </view>
            <text class="resource-title">健康档案</text>
            <text class="resource-value">{{ healthReports.length }}<text class="resource-unit">份</text></text>
            <text class="resource-desc">按时间归档体检记录</text>
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
const statusLabel = computed(() => isDeviceConnected.value ? '设备在线' : '设备未连接')
const sceneLabel = computed(() => {
  if (!isDeviceConnected.value) return '连接设备后显示实时心率'
  const labels: Record<string, string> = {
    resting: '静息状态',
    exercise: '运动状态',
    sleeping: '睡眠状态'
  }
  return labels[heartRateData.value.scene] || '实时监测'
})
const trendModeLabel = computed(() =>
  isDeviceConnected.value ? '设备同步记录' : '连接设备后显示趋势'
)

const hasTodayTrend = computed(() => heartRateData.value.todayData.length >= 2)

async function drawTrendChart() {
  const data = heartRateData.value.todayData
  if (data.length < 2) return
  const rect = await new Promise<any>(resolve => {
    uni.createSelectorQuery().select('#healthTrendCanvas').boundingClientRect(resolve).exec()
  })
  if (!rect?.width || !rect?.height) return

  const canvas = uni.createCanvasContext('healthTrendCanvas')
  const width = rect.width
  const height = rect.height
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
    const high = Math.max(data[index].value, data[index + 1].value) > 110
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
  background: #F3F7FC;
  color: #192B3D;
}

.health-mast {
  flex: none;
  border-bottom: 1rpx solid #DEE8F1;
  background: #F9FCFF;
}

.mast-content {
  display: flex;
  align-items: center;
  height: 88rpx;
  padding: 0 28rpx;
}

.mast-signal {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  gap: 4rpx;
  width: 52rpx;
  height: 52rpx;
  margin-right: 14rpx;
  border: 1rpx solid #D5E4F2;
  border-radius: 13rpx;
  background: #FFFFFF;
}

.mast-beat {
  width: 5rpx;
  border-radius: 4rpx;
  background: #2E6DD1;
}

.mast-beat-short { height: 15rpx; }
.mast-beat-tall { height: 30rpx; background: #B52832; }

.mast-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.mast-title {
  color: #18334F;
  font-size: 29rpx;
  font-weight: 720;
  line-height: 1.25;
}

.mast-caption {
  margin-top: 2rpx;
  color: #71869B;
  font-size: 14rpx;
  font-weight: 650;
  letter-spacing: 2rpx;
}

.mast-state {
  display: flex;
  flex: none;
  align-items: center;
  gap: 7rpx;
  color: #718295;
  font-size: 18rpx;
  font-weight: 600;
}

.mast-state-dot {
  width: 9rpx;
  height: 9rpx;
  border-radius: 50%;
  background: #A6B1BC;
}

.mast-state-live { color: #287858; }
.mast-state-live .mast-state-dot { background: #23956A; }

.page-scroll {
  min-height: 0;
  height: 0;
  flex: 1;
  box-sizing: border-box;
}

.signal-panel {
  margin: 18rpx 24rpx 0;
  padding: 30rpx 28rpx 26rpx;
  border: 1rpx solid #DCE7F0;
  border-radius: 20rpx;
  background: #F9FCFF;
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
.chart-subtitle,
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
  color: #6E8294;
  font-size: 18rpx;
  font-weight: 800;
  letter-spacing: 2rpx;
}

.signal-title {
  margin-top: 4rpx;
  font-size: 32rpx;
  font-weight: 750;
}

.signal-status {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 8rpx 14rpx;
  border-radius: 18rpx;
  background: #EEF2F5;
  color: #6F7F8E;
  font-size: 20rpx;
  font-weight: 650;
}

.signal-status-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #A8B2BC;
}

.signal-status-live {
  background: #EAF7F1;
  color: #237254;
}

.signal-status-live .signal-status-dot {
  background: #24966B;
}

.reading-row {
  align-items: flex-end;
  justify-content: space-between;
  margin-top: 28rpx;
}

.reading-main {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.reading-value {
  color: #172C40;
  font-size: 72rpx;
  font-weight: 760;
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
  max-width: 260rpx;
  color: #718598;
  text-align: right;
  font-size: 20rpx;
  line-height: 1.45;
}

.chart-header {
  margin-top: 36rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid #E6EDF2;
}

.chart-title {
  font-size: 26rpx;
  font-weight: 700;
}

.chart-subtitle {
  margin-top: 4rpx;
  color: #8797A6;
  font-size: 18rpx;
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
}
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
  margin-top: 22rpx;
  padding-top: 20rpx;
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
  margin: 18rpx 24rpx 0;
  padding: 22rpx 24rpx;
  border: 1rpx solid #ECD9DC;
  border-radius: 18rpx;
  background: #FFF9F9;
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
  margin: 28rpx 24rpx 0;
}

.section-head {
  padding: 0 4rpx 14rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 750;
}

.section-note {
  color: #8797A6;
  font-size: 19rpx;
}

.device-card {
  display: flex;
  align-items: center;
  min-height: 112rpx;
  padding: 22rpx 22rpx;
  border: 1rpx solid #DCE7F0;
  border-radius: 18rpx;
  background: #F9FCFF;
}

.device-card:active,
.resource-card:active {
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

.resource-grid {
  display: flex;
  gap: 14rpx;
  margin-top: 14rpx;
}

.resource-card {
  min-width: 0;
  flex: 1;
  padding: 20rpx 20rpx 22rpx;
  border: 1rpx solid #DCE7F0;
  border-radius: 18rpx;
  background: #FFFFFF;
}

.resource-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.resource-value {
  display: block;
  margin-top: 12rpx;
  color: #213B54;
  font-size: 28rpx;
  font-weight: 740;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.resource-unit {
  margin-left: 3rpx;
  color: #708397;
  font-size: 18rpx;
  font-weight: 600;
}

.bottom-safe {
  height: calc(72rpx + env(safe-area-inset-bottom));
}
</style>
