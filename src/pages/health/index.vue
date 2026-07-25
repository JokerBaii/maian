<template>
  <view class="page">
    <scroll-view class="page-scroll" scroll-y>
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
            <app-icon name="heart-filled" :size="24" color="#B52832" />
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
          <view class="trend-normal-band"></view>
          <view
            v-for="(item, index) in trendBars"
            :key="index"
            class="trend-column"
          >
            <view
              class="trend-bar"
              :style="{ height: item.height + 'rpx', backgroundColor: item.color }"
            ></view>
          </view>
        </view>
        <view class="trend-labels">
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
            <text class="alert-label">{{ isDeviceConnected ? '最近预警' : '示例预警' }}</text>
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
          <text class="section-note">集中管理</text>
        </view>
        <view class="resource-list">
          <view class="resource-row" @tap="goBind">
            <view class="resource-icon">
              <app-icon name="heart-filled" :size="20" color="#235FAE" />
            </view>
            <view class="resource-copy">
              <text class="resource-title">穿戴设备</text>
              <text class="resource-desc">
                {{ isDeviceConnected ? heartRateData.wearable.name : '未连接，连接后同步实测数据' }}
              </text>
            </view>
            <view class="resource-action">
              <text>{{ isDeviceConnected ? '已连接' : '连接' }}</text>
              <app-icon name="right" :size="14" color="#8291A4" />
            </view>
          </view>

          <view class="resource-row" @tap="goCheckup">
            <view class="resource-icon">
              <app-icon name="list" :size="20" color="#235FAE" />
            </view>
            <view class="resource-copy">
              <text class="resource-title">体检报告</text>
              <text class="resource-desc">
                {{ latestReport ? `${latestReport.checkupDate} · ${latestAbnormalCount}项异常` : '暂无报告，点击录入' }}
              </text>
            </view>
            <app-icon name="right" :size="14" color="#8291A4" />
          </view>

          <view class="resource-row resource-row-last" @tap="goArchive">
            <view class="resource-icon">
              <app-icon name="folder-add-filled" :size="20" color="#235FAE" />
            </view>
            <view class="resource-copy">
              <text class="resource-title">健康档案</text>
              <text class="resource-desc">{{ healthReports.length }}份体检记录</text>
            </view>
            <app-icon name="right" :size="14" color="#8291A4" />
          </view>
        </view>
      </view>

      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { useHealthMonitoring } from '@/composables/useHealthMonitoring'
import { listHealthReports, type HealthReportResponse } from '@/api/reports'

const { monitoring: heartRateData, loadMonitoring } = useHealthMonitoring()
const healthReports = ref<HealthReportResponse[]>([])

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
  isDeviceConnected.value ? '设备实测 · 每 30 分钟' : '示例趋势 · 不代表个人数据'
)
const trendBars = computed(() => {
  const data = heartRateData.value.todayData.filter((_, index) => index % 2 === 0)
  const values = data.map(item => item.value)
  const maximum = Math.max(...values, 1)
  const minimum = Math.min(...values, maximum)
  const range = maximum - minimum || 1
  return data.map(item => ({
    height: ((item.value - minimum) / range) * 88 + 18,
    color: item.value > 110
      ? '#D54A55'
      : item.value > 95
        ? '#D88735'
        : item.value < 60
          ? '#5E83C7'
          : '#24966B'
  }))
})

onMounted(async () => {
  const [monitoring, reports] = await Promise.allSettled([
    loadMonitoring(),
    listHealthReports()
  ])
  if (monitoring.status !== 'fulfilled') {
    uni.showToast({ title: '健康数据加载失败', icon: 'none' })
  }
  if (reports.status === 'fulfilled') {
    healthReports.value = reports.value
  }
})

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
  min-height: 100vh;
  background: #F1F5F8;
  color: #192B3D;
}

.page-scroll {
  height: 100vh;
  box-sizing: border-box;
}

.signal-panel {
  margin: 24rpx 24rpx 0;
  padding: 30rpx 28rpx 26rpx;
  border: 1rpx solid #D9E4EC;
  border-radius: 22rpx;
  background: #FFFFFF;
}

.signal-header,
.reading-row,
.chart-header,
.metric-row,
.section-head,
.resource-row,
.alert-strip,
.alert-signal,
.alert-meta,
.resource-action {
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
  align-items: baseline;
  gap: 8rpx;
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
  display: flex;
  align-items: flex-end;
  gap: 7rpx;
  height: 142rpx;
  margin-top: 18rpx;
  overflow: hidden;
}

.trend-normal-band {
  position: absolute;
  right: 0;
  bottom: 30rpx;
  left: 0;
  height: 58rpx;
  border-top: 1rpx solid #DDEBE5;
  border-bottom: 1rpx solid #DDEBE5;
  background: #F3F9F6;
}

.trend-column {
  position: relative;
  z-index: 1;
  display: flex;
  flex: 1;
  align-items: flex-end;
  justify-content: center;
}

.trend-bar {
  width: 100%;
  max-width: 14rpx;
  min-height: 8rpx;
  border-radius: 7rpx;
}

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
  margin: 32rpx 24rpx 0;
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

.resource-list {
  padding: 0 24rpx;
  border: 1rpx solid #D9E4EC;
  border-radius: 20rpx;
  background: #FFFFFF;
}

.resource-row {
  min-height: 116rpx;
  border-bottom: 1rpx solid #E7EDF2;
}

.resource-row-last {
  border-bottom: 0;
}

.resource-icon {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 58rpx;
  height: 58rpx;
  border-radius: 15rpx;
  background: #ECF3FA;
}

.resource-copy {
  min-width: 0;
  flex: 1;
  margin-left: 18rpx;
}

.resource-title {
  font-size: 25rpx;
  font-weight: 700;
}

.resource-desc {
  margin-top: 4rpx;
  overflow: hidden;
  color: #7D8E9E;
  font-size: 19rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-action {
  gap: 3rpx;
  margin-left: 12rpx;
  color: #46627D;
  font-size: 19rpx;
  font-weight: 650;
}

.bottom-safe {
  height: calc(140rpx + env(safe-area-inset-bottom));
}
</style>
