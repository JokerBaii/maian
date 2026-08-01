<template>
  <view class="page">
    <view class="page-scroll">
      <view v-if="loading" class="state-card">
        <view class="loading-ring"></view>
        <text class="state-title">正在加载健康分析</text>
      </view>

      <view v-else-if="!report" class="state-card">
        <app-icon-tile class="state-icon" name="info-filled" tone="slate" size="large" />
        <text class="state-title">{{ errorMessage || '暂无体检报告' }}</text>
        <view class="state-button" @tap="goUpload">录入报告</view>
      </view>

      <template v-else>
        <view class="risk-card" :class="`risk-${report.riskLevel.toLowerCase()}`">
          <view class="risk-top">
            <view>
              <text class="risk-kicker">HEALTH ANALYSIS</text>
              <text class="risk-title">{{ riskLabel }}</text>
            </view>
            <view class="source-chip">
              <view class="source-dot"></view>
              <text>{{ report.analysisSource === 'SPRING_AI' ? 'Spring AI' : '规则分析' }}</text>
            </view>
          </view>
          <text class="risk-summary">{{ report.summary }}</text>
          <view class="risk-stats">
            <view class="risk-stat">
              <text class="stat-value">{{ abnormalIndicators.length }}</text>
              <text class="stat-label">异常指标</text>
            </view>
            <view class="stat-line"></view>
            <view class="risk-stat">
              <text class="stat-value">{{ report.indicators.length }}</text>
              <text class="stat-label">检测项目</text>
            </view>
            <view class="stat-line"></view>
            <view class="risk-stat">
              <text class="stat-value">{{ normalCount }}</text>
              <text class="stat-label">正常项目</text>
            </view>
          </view>
        </view>

        <view class="report-card">
          <view class="section-head">
            <view>
              <text class="section-kicker">REPORT INFO</text>
              <text class="section-title">报告信息</text>
            </view>
            <text class="report-date">{{ report.checkupDate }}</text>
          </view>
          <view class="hospital-row">
            <app-icon-tile name="hospital" tone="blue" />
            <text>{{ report.hospital }}</text>
          </view>
          <image
            v-if="report.sourceImageUrl"
            class="source-image"
            :src="resolveApiUrl(report.sourceImageUrl)"
            mode="aspectFill"
            @tap="previewSourceImage"
          />
        </view>

        <view class="report-card">
          <view class="section-head">
            <view>
              <text class="section-kicker">INDICATORS</text>
              <text class="section-title">体检指标</text>
            </view>
            <text class="section-meta">{{ report.indicators.length }} 项</text>
          </view>
          <view class="indicator-list">
            <view
              v-for="indicator in report.indicators"
              :key="`${indicator.name}-${indicator.value}`"
              class="indicator-row"
              :class="{ abnormal: indicator.abnormal }"
            >
              <view class="indicator-main">
                <text class="indicator-name">{{ indicator.name }}</text>
                <text v-if="indicator.referenceRange" class="indicator-range">
                  参考 {{ indicator.referenceRange }}
                </text>
              </view>
              <view class="indicator-value-wrap">
                <text class="indicator-value">{{ indicator.value }}</text>
                <text v-if="indicator.unit" class="indicator-unit">{{ indicator.unit }}</text>
              </view>
              <view class="indicator-status" :class="{ abnormal: indicator.abnormal }">
                <app-icon-tile
                  :name="indicator.abnormal ? 'health-alert' : 'check'"
                  :tone="indicator.abnormal ? 'coral' : 'green'"
                  size="small"
                />
                <text>{{ indicator.abnormal ? '异常' : '正常' }}</text>
              </view>
            </view>
          </view>
        </view>

        <view class="report-card">
          <view class="section-head">
            <view>
              <text class="section-kicker">RECOMMENDATIONS</text>
              <text class="section-title">健康建议</text>
            </view>
          </view>
          <view class="recommendation-list">
            <view
              v-for="(recommendation, index) in report.recommendations"
              :key="index"
              class="recommendation"
            >
              <text class="recommendation-index">{{ index + 1 }}</text>
              <text class="recommendation-text">{{ recommendation }}</text>
            </view>
          </view>
        </view>

        <view class="disclaimer-card">
          <app-icon name="info-filled" :size="19" color="#2A67CB" />
          <text>{{ report.disclaimer }}</text>
        </view>

        <view class="bottom-actions">
          <view class="secondary-button" @tap="handleShare">复制摘要</view>
          <view class="primary-button" @tap="goArchive">查看健康档案</view>
        </view>
        <view class="bottom-space"></view>
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import { resolveApiUrl } from '@/api/http'
import {
  getHealthReport,
  listHealthReports,
  type HealthReportResponse
} from '@/api/reports'

const report = ref<HealthReportResponse | null>(null)
const loading = ref(true)
const errorMessage = ref('')

const abnormalIndicators = computed(() => report.value?.indicators.filter(item => item.abnormal) || [])
const normalCount = computed(() => (report.value?.indicators.length || 0) - abnormalIndicators.value.length)
const riskLabel = computed(() => {
  const labels = { LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险' }
  return labels[report.value?.riskLevel || 'LOW']
})

onLoad(async (query) => {
  const id = typeof query?.id === 'string' ? query.id : ''
  try {
    if (id) {
      report.value = await getHealthReport(id)
    } else {
      const reports = await listHealthReports()
      report.value = reports[0] || null
    }
  } catch (error: any) {
    errorMessage.value = error?.message || '报告加载失败'
  } finally {
    loading.value = false
  }
})

function previewSourceImage() {
  if (!report.value?.sourceImageUrl) return
  const url = resolveApiUrl(report.value.sourceImageUrl)
  uni.previewImage({ current: url, urls: [url] })
}

function handleShare() {
  if (!report.value) return
  uni.setClipboardData({
    data: `脉安驰援健康分析：${riskLabel.value}，发现 ${abnormalIndicators.value.length} 项异常指标。${report.value.summary}`,
    success: () => uni.showToast({ title: '报告摘要已复制', icon: 'none' })
  })
}

function goArchive() {
  uni.navigateTo({ url: '/pages/checkup/archive' })
}

function goUpload() {
  uni.redirectTo({ url: '/pages/checkup/upload' })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F1F5FA;
  color: #18253A;
}

.page-scroll {
  min-height: 100vh;
}

.state-card {
  min-height: 72vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.loading-ring {
  width: 54rpx;
  height: 54rpx;
  border: 6rpx solid #DDE7F5;
  border-top-color: #2B70E3;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.state-icon {
  width: 88rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 25rpx;
  background: #FFFFFF;
  box-shadow: 0 10rpx 30rpx rgba(37, 62, 101, 0.09);
}

.state-title {
  margin-top: 25rpx;
  font-size: 28rpx;
  font-weight: 700;
}

.state-button {
  margin-top: 24rpx;
  padding: 17rpx 30rpx;
  border-radius: 16rpx;
  background: #2B70E3;
  color: #FFFFFF;
  font-size: 24rpx;
  font-weight: 700;
}

.risk-card,
.report-card,
.disclaimer-card {
  margin: 24rpx 24rpx 0;
  border-radius: 26rpx;
}

.risk-card {
  padding: 32rpx 30rpx 28rpx;
  overflow: hidden;
  background:
    radial-gradient(circle at 91% 8%, rgba(255, 255, 255, 0.18), transparent 34%),
    linear-gradient(135deg, #186F57 0%, #27A477 100%);
  color: #FFFFFF;
  box-shadow: 0 16rpx 42rpx rgba(28, 128, 93, 0.22);
}

.risk-medium {
  background:
    radial-gradient(circle at 91% 8%, rgba(255, 255, 255, 0.18), transparent 34%),
    linear-gradient(135deg, #B85F17 0%, #E28D34 100%);
  box-shadow: 0 16rpx 42rpx rgba(195, 105, 28, 0.22);
}

.risk-high {
  background:
    radial-gradient(circle at 91% 8%, rgba(255, 255, 255, 0.18), transparent 34%),
    linear-gradient(135deg, #A92836 0%, #D84B56 100%);
  box-shadow: 0 16rpx 42rpx rgba(184, 48, 61, 0.22);
}

.risk-top,
.section-head,
.hospital-row,
.indicator-row,
.recommendation,
.bottom-actions {
  display: flex;
  align-items: center;
}

.risk-top,
.section-head {
  justify-content: space-between;
}

.risk-kicker,
.section-kicker {
  display: block;
  font-size: 19rpx;
  font-weight: 800;
  letter-spacing: 3rpx;
}

.risk-kicker {
  color: rgba(255, 255, 255, 0.64);
}

.risk-title {
  display: block;
  margin-top: 6rpx;
  font-size: 40rpx;
  font-weight: 850;
}

.source-chip {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 9rpx 14rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.2);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.12);
  font-size: 20rpx;
}

.source-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #D4FFE9;
}

.risk-summary {
  display: block;
  margin-top: 22rpx;
  color: rgba(255, 255, 255, 0.86);
  font-size: 25rpx;
  line-height: 1.65;
}

.risk-stats {
  display: flex;
  align-items: center;
  margin-top: 27rpx;
  padding: 19rpx 0;
  border-radius: 18rpx;
  background: rgba(255, 255, 255, 0.11);
}

.risk-stat {
  flex: 1;
  text-align: center;
}

.stat-value,
.stat-label {
  display: block;
}

.stat-value {
  font-size: 31rpx;
  font-weight: 850;
}

.stat-label {
  margin-top: 5rpx;
  color: rgba(255, 255, 255, 0.7);
  font-size: 19rpx;
}

.stat-line {
  width: 1rpx;
  height: 42rpx;
  background: rgba(255, 255, 255, 0.2);
}

.report-card {
  padding: 28rpx;
  background: #FFFFFF;
  box-shadow: 0 8rpx 26rpx rgba(38, 63, 103, 0.06);
}

.section-kicker {
  color: #8190A7;
}

.section-title {
  display: block;
  margin-top: 5rpx;
  font-size: 31rpx;
  font-weight: 800;
}

.section-meta,
.report-date {
  color: #77869D;
  font-size: 22rpx;
}

.hospital-row {
  gap: 15rpx;
  margin-top: 24rpx;
  font-size: 25rpx;
  font-weight: 700;
}

.source-image {
  width: 100%;
  height: 260rpx;
  margin-top: 21rpx;
  border-radius: 18rpx;
  background: #EDF2F8;
}

.indicator-list {
  margin-top: 18rpx;
}

.indicator-row {
  gap: 14rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #E8EDF4;
}

.indicator-row:last-child {
  border-bottom: 0;
}

.indicator-main {
  min-width: 0;
  flex: 1;
}

.indicator-name,
.indicator-range {
  display: block;
}

.indicator-name {
  font-size: 25rpx;
  font-weight: 750;
}

.indicator-range {
  margin-top: 5rpx;
  color: #94A0B2;
  font-size: 19rpx;
}

.indicator-value-wrap {
  flex: none;
  text-align: right;
}

.indicator-value {
  font-size: 28rpx;
  font-weight: 850;
}

.indicator-unit {
  margin-left: 5rpx;
  color: #7C899D;
  font-size: 18rpx;
}

.indicator-status {
  flex: none;
  display: flex;
  align-items: center;
  gap: 7rpx;
  padding: 5rpx 10rpx 5rpx 6rpx;
  border-radius: 10rpx;
  background: #EAF8F2;
  color: #19845A;
  font-size: 19rpx;
  font-weight: 700;
}

.indicator-status.abnormal {
  background: #FFF0F1;
  color: #CF3F4A;
}

.recommendation-list {
  margin-top: 22rpx;
}

.recommendation {
  align-items: flex-start;
  gap: 16rpx;
  padding: 18rpx;
  border-radius: 16rpx;
  background: #F5F8FD;
}

.recommendation + .recommendation {
  margin-top: 13rpx;
}

.recommendation-index {
  flex: none;
  width: 42rpx;
  height: 42rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 13rpx;
  background: #DFEAFC;
  color: #1F63D5;
  font-size: 20rpx;
  font-weight: 800;
}

.recommendation-text {
  flex: 1;
  font-size: 24rpx;
  line-height: 1.65;
}

.disclaimer-card {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
  padding: 22rpx;
  border: 1rpx solid #D9E5F7;
  background: #EEF5FF;
  color: #65758D;
  font-size: 21rpx;
  line-height: 1.6;
}

.bottom-actions {
  gap: 16rpx;
  margin: 28rpx 24rpx 0;
}

.secondary-button,
.primary-button {
  height: 86rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 20rpx;
  font-size: 25rpx;
  font-weight: 800;
}

.secondary-button {
  width: 220rpx;
  border: 1rpx solid #C9D5E6;
  background: #FFFFFF;
  color: #47617F;
}

.primary-button {
  flex: 1;
  background: linear-gradient(135deg, #245FC6 0%, #377BE9 100%);
  color: #FFFFFF;
  box-shadow: 0 10rpx 25rpx rgba(37, 101, 207, 0.22);
}

.bottom-space {
  height: calc(42rpx + env(safe-area-inset-bottom));
}
</style>
