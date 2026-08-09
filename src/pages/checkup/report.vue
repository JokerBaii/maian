<template>
  <view class="page apple-page motion-page-focus">
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
              <text class="risk-kicker">健康评估</text>
              <text class="risk-title">{{ riskLabel }}</text>
            </view>
          </view>
          <text class="risk-summary">{{ report.summary }}</text>
          <view class="risk-overview">
            <text>{{ report.indicators.length }} 项指标</text>
            <view class="overview-dot"></view>
            <text :class="{ 'overview-alert': abnormalIndicators.length > 0 }">
              {{ abnormalIndicators.length > 0 ? `${abnormalIndicators.length} 项需关注` : '未发现异常' }}
            </text>
          </view>
        </view>

        <view class="report-section">
          <view class="section-head">
            <text class="section-title">报告信息</text>
            <text class="report-date">{{ report.checkupDate }}</text>
          </view>
          <view class="hospital-row">
            <app-icon-tile name="hospital" tone="blue" />
            <text>{{ report.hospital }}</text>
          </view>
          <image
            v-if="sourceImageUrl"
            class="source-image"
            :src="resolveApiUrl(sourceImageUrl)"
            mode="aspectFill"
            @tap="previewSourceImage"
          />
        </view>

        <view class="report-section">
          <view class="section-head">
            <text class="section-title">体检指标</text>
            <text class="section-meta">{{ report.indicators.length }} 项</text>
          </view>
          <view class="indicator-list" :class="{ 'indicator-list-expanded': indicatorsExpanded }">
            <view
              v-for="indicator in visibleIndicators"
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
          <view
            v-if="report.indicators.length > indicatorPreviewLimit"
            class="indicator-expand"
            @tap="toggleIndicators"
          >
            <text>{{ indicatorsExpanded ? '收起全部指标' : `查看全部 ${report.indicators.length} 项` }}</text>
            <app-icon
              :name="indicatorsExpanded ? 'top' : 'bottom'"
              :size="14"
              color="#007AFF"
            />
          </view>
        </view>

        <view class="report-section">
          <view class="section-head">
            <text class="section-title">建议</text>
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
          <view class="report-action report-action-secondary" @tap="handleShare">复制摘要</view>
          <view class="report-action report-action-primary" @tap="goArchive">查看健康档案</view>
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
import { issueMediaDownload } from '@/api/files'
import { userFacingError } from '@/utils/presentation'
import {
  getHealthReport,
  listHealthReports,
  type HealthReportResponse
} from '@/api/reports'

const report = ref<HealthReportResponse | null>(null)
const loading = ref(true)
const errorMessage = ref('')
const sourceImageUrl = ref('')
const indicatorsExpanded = ref(false)
const indicatorPreviewLimit = 5

const abnormalIndicators = computed(() => report.value?.indicators.filter(item => item.abnormal) || [])
const priorityIndicators = computed(() => {
  const indicators = report.value?.indicators || []
  const preferredNames = ['血压', '空腹血糖', '总胆固醇', '低密度脂蛋白', '血红蛋白', '体重指数']
  return [...indicators]
    .sort((left, right) => {
      if (left.abnormal !== right.abnormal) return left.abnormal ? -1 : 1
      const leftPriority = preferredNames.indexOf(left.name)
      const rightPriority = preferredNames.indexOf(right.name)
      if (leftPriority === -1 && rightPriority === -1) return 0
      if (leftPriority === -1) return 1
      if (rightPriority === -1) return -1
      return leftPriority - rightPriority
    })
    .slice(0, indicatorPreviewLimit)
})
const visibleIndicators = computed(() =>
  indicatorsExpanded.value ? (report.value?.indicators || []) : priorityIndicators.value
)
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
    if (report.value?.sourceMediaId) {
      sourceImageUrl.value = (await issueMediaDownload(report.value.sourceMediaId)).url
    }
  } catch (error: any) {
    errorMessage.value = userFacingError(error, '报告加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
})

function previewSourceImage() {
  if (!sourceImageUrl.value) return
  const url = resolveApiUrl(sourceImageUrl.value)
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

function toggleIndicators() {
  indicatorsExpanded.value = !indicatorsExpanded.value
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #FFFFFF;
  color: #1C1C1E;
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
.report-section,
.disclaimer-card {
  margin: 0;
  border-radius: 0;
}

.risk-card {
  padding: 38rpx 28rpx 34rpx;
  border: 0;
  border-bottom: 16rpx solid #F2F2F7;
  background: #FFFFFF;
  color: #1C1C1E;
  box-shadow: none;
}

.risk-low .risk-title { color: #248A5A; }
.risk-medium .risk-title { color: #B7670B; }
.risk-high .risk-title { color: #C72C38; }

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
  color: #8E8E93;
}

.risk-title {
  display: block;
  margin-top: 6rpx;
  font-size: 44rpx;
  font-weight: 850;
}

.risk-summary {
  display: block;
  margin-top: 22rpx;
  color: #48484A;
  font-size: 25rpx;
  line-height: 1.65;
}

.risk-overview {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 22rpx;
  color: #636366;
  font-size: 21rpx;
}

.overview-dot {
  width: 6rpx;
  height: 6rpx;
  border-radius: 50%;
  background: #AEAEB2;
}

.overview-alert { color: #C72C38; font-weight: 650; }

.report-section {
  padding: 30rpx 28rpx;
  border: 0;
  border-bottom: 16rpx solid #F2F2F7;
  background: #FFFFFF;
  box-shadow: none;
  border-radius: 0 !important;
}

.section-kicker {
  color: #8190A7;
}

.section-title {
  display: block;
  font-size: 30rpx;
  font-weight: 720;
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

.indicator-list-expanded {
  animation: indicatorListReveal 220ms cubic-bezier(.2, .72, .2, 1) both;
}

@keyframes indicatorListReveal {
  from { opacity: .72; transform: translateY(-6rpx); }
  to { opacity: 1; transform: none; }
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
  color: #19845A;
  font-size: 19rpx;
  font-weight: 700;
}

.indicator-status.abnormal {
  color: #CF3F4A;
}

.indicator-expand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  min-height: 76rpx;
  margin-top: 4rpx;
  border-top: 1rpx solid rgba(60, 60, 67, .12);
  color: #007AFF;
  font-size: 23rpx;
  font-weight: 600;
  transition: opacity 150ms ease;
}

.indicator-expand:active { opacity: .48; }

.recommendation-list {
  margin-top: 14rpx;
}

.recommendation {
  align-items: flex-start;
  gap: 16rpx;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #E8EDF4;
}

.recommendation + .recommendation {
  margin-top: 0;
}
.recommendation:last-child { border-bottom: 0; }

.recommendation-index {
  flex: none;
  width: 42rpx;
  height: 42rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #EAF2FF;
  color: #1768D5;
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
  margin: 0;
  padding: 24rpx 28rpx;
  border: 0;
  border-bottom: 16rpx solid #F2F2F7;
  background: #F5F9FF;
  color: #65758D;
  font-size: 21rpx;
  line-height: 1.6;
}

.bottom-actions {
  display: flex;
  flex-direction: column;
  gap: 0;
  margin: 0;
  padding: 0 28rpx;
  background: #FFFFFF;
}

.report-action {
  width: 100%;
  height: 90rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0;
  background: #FFFFFF;
  color: #007AFF;
  font-size: 25rpx;
  font-weight: 600;
}

.report-action-secondary {
  order: 2;
  border: 0;
  border-top: 1rpx solid rgba(60, 60, 67, .14);
  background: #FFFFFF !important;
  color: #007AFF !important;
  border-radius: 0 !important;
}

.report-action-primary {
  flex: none;
  background: #FFFFFF !important;
  color: #007AFF !important;
  border-radius: 0 !important;
  box-shadow: none;
}

.bottom-space {
  height: calc(42rpx + env(safe-area-inset-bottom));
}
</style>
