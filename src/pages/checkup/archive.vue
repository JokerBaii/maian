<template>
  <view class="page apple-page motion-page-list">
    <scroll-view class="page-scroll" scroll-y :show-scrollbar="false">
      <view class="large-title-row">
        <view>
          <text class="large-title">健康档案</text>
          <text class="large-subtitle">体检结果按时间集中整理</text>
        </view>
        <view class="add-button app-motion-control" @tap="goUpload">
          <app-icon name="plus" :size="19" color="#FFFFFF" />
        </view>
      </view>

      <view v-if="loading" class="archive-state">正在载入健康档案…</view>
      <view v-else-if="!archive.length" class="empty-state">
        <view class="empty-symbol"><app-icon name="list" :size="28" color="#3478D4" /></view>
        <text class="empty-title">从一份体检报告开始</text>
        <text class="empty-copy">上传报告原图，核对识别结果后保存到时间线</text>
        <view class="empty-action" @tap="goUpload">添加报告</view>
      </view>

      <template v-else>
        <view class="latest-section">
          <text class="eyebrow">最近一次</text>
          <view class="latest-card app-motion-control" @tap="goReport(latestRecord!)">
            <view class="latest-top">
              <view class="latest-date">
                <text class="latest-day">{{ formatMonthDay(latestRecord!.date) }}</text>
                <text class="latest-year">{{ latestRecord!.date.slice(0, 4) }}</text>
              </view>
              <view class="risk-pill" :class="`risk-${latestRecord!.riskLevel}`">
                {{ riskLevelMap[latestRecord!.riskLevel] }}
              </view>
            </view>
            <text class="latest-hospital">{{ latestRecord!.hospital }}</text>
            <view class="latest-summary">
              <text>{{ latestRecord!.indicatorCount }} 项指标</text>
              <view class="summary-separator"></view>
              <text :class="latestRecord!.abnormalCount ? 'summary-attention' : 'summary-normal'">
                {{ latestRecord!.abnormalCount ? `${latestRecord!.abnormalCount} 项需关注` : '未见异常标记' }}
              </text>
            </view>
            <view class="latest-footer">
              <text>查看完整报告</text>
              <app-icon name="right" :size="14" color="#3478D4" />
            </view>
          </view>
        </view>

        <view class="overview-strip">
          <view class="overview-item">
            <text class="overview-value">{{ archive.length }}</text>
            <text class="overview-label">报告</text>
          </view>
          <view class="overview-divider"></view>
          <view class="overview-item">
            <text class="overview-value">{{ totalIndicators }}</text>
            <text class="overview-label">已归档指标</text>
          </view>
          <view class="overview-divider"></view>
          <view class="overview-item">
            <text class="overview-value">{{ attentionReports }}</text>
            <text class="overview-label">需关注报告</text>
          </view>
        </view>

        <view class="history-section">
          <view class="section-heading">
            <text class="section-title">报告记录</text>
            <text class="section-caption">长按可删除</text>
          </view>
          <view class="record-group">
            <view
              v-for="record in archive"
              :key="record.id"
              class="record-row app-motion-control"
              @tap="goReport(record)"
              @longpress="confirmDelete(record)"
            >
              <view class="timeline-date">
                <text class="timeline-month">{{ formatMonthDay(record.date) }}</text>
                <text class="timeline-year">{{ record.date.slice(0, 4) }}</text>
              </view>
              <view class="timeline-rail">
                <view class="timeline-node" :class="`node-${record.riskLevel}`"></view>
                <view class="timeline-line"></view>
              </view>
              <view class="record-copy">
                <text class="record-hospital">{{ record.hospital }}</text>
                <text class="record-meta">
                  {{ record.indicatorCount }} 项指标 · {{ record.abnormalCount ? `${record.abnormalCount} 项需关注` : '结果平稳' }}
                </text>
              </view>
              <app-icon name="right" :size="14" color="#B0B7C2" />
            </view>
          </view>
        </view>
      </template>

      <view class="bottom-safe"></view>
    </scroll-view>
    <assistant-companion context="health" />
  </view>
</template>

<script setup lang="ts">
import { useAssistantRefresh } from '@/composables/useAssistantRefresh'
import AssistantCompanion from '@/components/AssistantCompanion.vue'
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIcon from '@/components/AppIcon.vue'
import { listHealthReports, deleteHealthReport } from '@/api/reports'
import { userFacingError } from '@/utils/presentation'

interface ArchiveRecord {
  id: string
  date: string
  hospital: string
  riskLevel: 'low' | 'medium' | 'high'
  abnormalCount: number
  indicatorCount: number
}

const archive = ref<ArchiveRecord[]>([])
const loading = ref(true)
const latestRecord = computed(() => archive.value[0] || null)
const totalIndicators = computed(() => archive.value.reduce((sum, item) => sum + item.indicatorCount, 0))
const attentionReports = computed(() => archive.value.filter(item => item.abnormalCount > 0).length)

const riskLevelMap: Record<string, string> = {
  low: '低风险',
  medium: '中风险',
  high: '高风险'
}

async function loadArchive() {
  loading.value = true
  try {
    const reports = await listHealthReports()
    archive.value = reports.map(report => ({
      id: report.id,
      date: report.checkupDate,
      hospital: report.hospital,
      riskLevel: report.riskLevel.toLowerCase() as ArchiveRecord['riskLevel'],
      abnormalCount: report.indicators.filter(indicator => indicator.abnormal).length,
      indicatorCount: report.indicators.length
    })).sort((left, right) => right.date.localeCompare(left.date))
  } catch {
    uni.showToast({ title: '健康档案加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

onShow(loadArchive)
useAssistantRefresh(loadArchive)

function formatMonthDay(date: string) {
  const [, month = '', day = ''] = date.split('-')
  return `${month}/${day}`
}

function confirmDelete(record: ArchiveRecord) {
  uni.showModal({
    title: '删除这份报告？',
    content: `${record.hospital} · ${record.date}\n删除后无法恢复。`,
    confirmText: '删除',
    confirmColor: '#C93D46',
    success: async result => {
      if (!result.confirm) return
      try {
        await deleteHealthReport(record.id)
        archive.value = archive.value.filter(item => item.id !== record.id)
        uni.showToast({ title: '已删除', icon: 'success' })
      } catch (error: any) {
        uni.showToast({ title: userFacingError(error, '删除失败，请重试'), icon: 'none' })
      }
    }
  })
}

function goReport(record: ArchiveRecord) {
  uni.navigateTo({ url: '/pages/checkup/report?id=' + record.id })
}

function goUpload() {
  uni.navigateTo({ url: '/pages/checkup/upload' })
}
</script>

<style lang="scss" scoped>
.page {
  height: calc(100vh - var(--window-top, 0px) - var(--window-bottom, 0px));
  height: calc(100dvh - var(--window-top, 0px) - var(--window-bottom, 0px));
  overflow: hidden;
  background: #F2F2F7;
  color: #172B42;
}
.page-scroll { height: 100%; box-sizing: border-box; }
.large-title-row { display: flex; align-items: flex-end; justify-content: space-between; padding: 36rpx 30rpx 26rpx; }
.large-title, .large-subtitle { display: block; }
.large-title { font-size: 48rpx; font-weight: 760; letter-spacing: -1rpx; }
.large-subtitle { margin-top: 7rpx; color: #7B8794; font-size: 23rpx; }
.add-button { display: flex; width: 64rpx; height: 64rpx; align-items: center; justify-content: center; border-radius: 50%; background: #1677E8; box-shadow: 0 8rpx 20rpx rgba(22,119,232,.22); }
.add-button:active { transform: scale(.92); opacity: .72; }
.latest-section, .history-section { margin: 0 24rpx; }
.eyebrow { display: block; margin: 0 8rpx 12rpx; color: #7C8796; font-size: 22rpx; font-weight: 600; }
.latest-card { padding: 28rpx; border: 1rpx solid rgba(60,60,67,.12); border-radius: 25rpx; background: #FFF; box-shadow: 0 10rpx 28rpx rgba(32,54,77,.055); }
.latest-card:active, .record-row:active { transform: scale(.99); background: #F8F8FA; }
.latest-top { display: flex; align-items: flex-start; justify-content: space-between; }
.latest-date { display: flex; align-items: baseline; gap: 10rpx; }
.latest-day { font-size: 42rpx; font-weight: 760; letter-spacing: -1rpx; }
.latest-year { color: #8A94A2; font-size: 22rpx; }
.risk-pill { padding: 8rpx 15rpx; border-radius: 999rpx; font-size: 21rpx; font-weight: 650; }
.risk-low { color: #167B56; background: #E9F6F0; }.risk-medium { color: #A8610A; background: #FFF3E3; }.risk-high { color: #B83443; background: #FDECEF; }
.latest-hospital { display: block; margin-top: 25rpx; font-size: 29rpx; font-weight: 680; line-height: 1.35; }
.latest-summary { display: flex; align-items: center; gap: 14rpx; margin-top: 12rpx; color: #758192; font-size: 23rpx; }
.summary-separator { width: 1rpx; height: 20rpx; background: #D9DEE5; }
.summary-normal { color: #167B56; }.summary-attention { color: #B83443; }
.latest-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 27rpx; padding-top: 22rpx; border-top: 1rpx solid #E9EBEF; color: #3478D4; font-size: 24rpx; font-weight: 600; }
.overview-strip { display: flex; align-items: stretch; margin: 24rpx; padding: 24rpx 8rpx; border-radius: 22rpx; background: rgba(255,255,255,.78); }
.overview-item { flex: 1; text-align: center; }
.overview-value, .overview-label { display: block; }
.overview-value { font-size: 31rpx; font-weight: 720; }.overview-label { margin-top: 5rpx; color: #838C98; font-size: 20rpx; }
.overview-divider { width: 1rpx; background: #E1E4E8; }
.section-heading { display: flex; align-items: baseline; justify-content: space-between; padding: 10rpx 8rpx 14rpx; }
.section-title { font-size: 31rpx; font-weight: 720; }.section-caption { color: #9299A4; font-size: 20rpx; }
.record-group { overflow: hidden; border: 1rpx solid rgba(60,60,67,.11); border-radius: 24rpx; background: #FFF; }
.record-row { position: relative; display: grid; min-height: 116rpx; grid-template-columns: 72rpx 22rpx minmax(0,1fr) 24rpx; align-items: center; gap: 12rpx; padding: 18rpx 22rpx; }
.record-row::after { content: ''; position: absolute; right: 22rpx; bottom: 0; left: 128rpx; height: 1rpx; background: #E9EBEF; }.record-row:last-child::after { display: none; }
.timeline-date { text-align: right; }.timeline-month, .timeline-year { display: block; }.timeline-month { font-size: 25rpx; font-weight: 700; }.timeline-year { margin-top: 3rpx; color: #929BA8; font-size: 19rpx; }
.timeline-rail { position: relative; align-self: stretch; display: flex; justify-content: center; }
.timeline-node { z-index: 1; width: 13rpx; height: 13rpx; margin-top: 31rpx; border: 3rpx solid #FFF; border-radius: 50%; box-shadow: 0 0 0 1rpx currentColor; background: currentColor; }.node-low { color: #2F9B72; }.node-medium { color: #D58A2B; }.node-high { color: #C94A58; }
.timeline-line { position: absolute; top: 46rpx; bottom: -40rpx; width: 1rpx; background: #DCE1E7; }.record-row:last-child .timeline-line { display: none; }
.record-copy { min-width: 0; }.record-hospital, .record-meta { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.record-hospital { font-size: 26rpx; font-weight: 650; }.record-meta { margin-top: 8rpx; color: #818B99; font-size: 21rpx; }
.archive-state, .empty-state { margin: 20rpx 24rpx; padding: 90rpx 34rpx; border-radius: 26rpx; background: #FFF; text-align: center; color: #7B8794; font-size: 24rpx; }
.empty-symbol { display: flex; width: 72rpx; height: 72rpx; align-items: center; justify-content: center; margin: 0 auto 22rpx; border-radius: 22rpx; background: #EDF5FF; }.empty-title, .empty-copy { display: block; }.empty-title { color: #172B42; font-size: 30rpx; font-weight: 700; }.empty-copy { max-width: 480rpx; margin: 12rpx auto 0; font-size: 23rpx; line-height: 1.6; }.empty-action { display: inline-flex; margin-top: 30rpx; padding: 16rpx 28rpx; border-radius: 16rpx; background: #1677E8; color: #FFF; font-size: 24rpx; font-weight: 650; }
.bottom-safe { height: calc(70rpx + env(safe-area-inset-bottom)); }
</style>
