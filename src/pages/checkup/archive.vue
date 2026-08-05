<template>
  <view class="page">
    <view class="scroll-content">
      <view class="summary-card">
        <view class="summary-bg"></view>
        <view class="summary-content">
          <view class="summary-top">
            <app-icon-tile name="folder-add-filled" tone="violet" />
            <view class="summary-info">
              <text class="summary-title">我的健康档案</text>
              <text class="summary-sub">持续记录，守护健康</text>
            </view>
          </view>
          <view class="summary-stats">
            <view class="stat-item">
              <text class="stat-num">{{ archive.length }}</text>
              <text class="stat-label">体检报告</text>
            </view>
            <view class="stat-divider"></view>
            <view class="stat-item">
              <text class="stat-num">{{ latestDate }}</text>
              <text class="stat-label">最近体检</text>
            </view>
            <view class="stat-divider"></view>
            <view class="stat-item">
              <text class="stat-num" :class="'stat-' + latestRisk">{{ latestRiskLabel }}</text>
              <text class="stat-label">最新风险</text>
            </view>
          </view>
        </view>
      </view>

      <view class="timeline-section">
        <view class="section-header">
          <view class="section-title-wrap">
            <text class="section-title">体检记录</text>
          </view>
        </view>

        <view v-if="loading" class="archive-state">正在加载健康档案…</view>
        <view v-else-if="!archive.length" class="archive-state">
          <text class="archive-state-title">暂无体检报告</text>
          <text class="archive-state-desc">点击右下角按钮上传报告原图，识别后即可生成分析</text>
        </view>

        <view v-else class="timeline">
          <view
            v-for="(record, idx) in archive"
            :key="idx"
            class="timeline-item"
            @tap="goReport(record)"
            @longpress="confirmDelete(record)"
          >
            <view class="timeline-left">
              <view class="timeline-dot-wrap">
                <view class="timeline-dot" :class="'dot-' + record.riskLevel"></view>
                <view v-if="idx < archive.length - 1" class="timeline-line"></view>
              </view>
              <view class="timeline-date">
                <text class="date-year">{{ record.date.split('-')[0] }}</text>
                <text class="date-md">{{ record.date.split('-').slice(1).join('/') }}</text>
              </view>
            </view>

            <view class="record-card">
              <view class="record-header">
                <view class="record-hospital-wrap">
                  <app-icon-tile name="hospital" tone="blue" size="small" />
                  <text class="record-hospital">{{ record.hospital }}</text>
                </view>
                <view class="risk-indicator" :class="'indicator-' + record.riskLevel">
                  <view class="indicator-dot"></view>
                  <text class="indicator-text">{{ riskLevelMap[record.riskLevel] }}</text>
                </view>
              </view>
              <view class="record-body">
                <view class="record-stat-row">
                  <view class="record-stat">
                    <text class="record-stat-label">异常指标</text>
                    <view class="abnormal-badge" :class="record.abnormalCount > 0 ? 'badge-has' : 'badge-none'">
                      <text class="abnormal-badge-text">{{ record.abnormalCount }}</text>
                    </view>
                  </view>
                  <view class="record-stat">
                    <text class="record-stat-label">体检日期</text>
                    <text class="record-stat-value">{{ record.date }}</text>
                  </view>
                </view>
              </view>
              <view class="record-footer">
                <text class="record-hint">长按删除</text>
                <text class="record-action">查看详细报告</text>
                <text class="record-arrow">></text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="bottom-safe"></view>
    </view>

    <view class="fab-btn" @tap="goUpload">
      <view class="fab-icon-wrap">
        <text class="fab-icon">+</text>
      </view>
      <text class="fab-text">上传新报告</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIconTile from '@/components/AppIconTile.vue'
import { listHealthReports, deleteHealthReport } from '@/api/reports'

interface ArchiveRecord {
  id: string
  date: string
  hospital: string
  riskLevel: string
  abnormalCount: number
}

const archive = ref<ArchiveRecord[]>([])
const loading = ref(true)

async function loadArchive() {
  try {
    const reports = await listHealthReports()
    archive.value = reports.map(report => ({
      id: report.id,
      date: report.checkupDate,
      hospital: report.hospital,
      riskLevel: report.riskLevel.toLowerCase(),
      abnormalCount: report.indicators.filter(indicator => indicator.abnormal).length
    }))
  } catch {
    uni.showToast({ title: '健康档案加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

onShow(loadArchive)

/** 长按删除报告。后端按当前用户校验归属，只能删自己的记录。 */
function confirmDelete(record: ArchiveRecord) {
  uni.showModal({
    title: '删除这份报告',
    content: `${record.hospital} · ${record.date}\n删除后无法恢复。`,
    confirmText: '删除',
    confirmColor: '#C93D46',
    success: async (result) => {
      if (!result.confirm) return
      try {
        await deleteHealthReport(record.id)
        archive.value = archive.value.filter(item => item.id !== record.id)
        uni.showToast({ title: '已删除', icon: 'success' })
      } catch (error: any) {
        uni.showToast({ title: error?.message || '删除失败，请重试', icon: 'none' })
      }
    }
  })
}

const latestDate = computed(() => {
  if (archive.value.length > 0) {
    const d = archive.value[0].date
    return d.split('-').slice(1).join('/')
  }
  return '--'
})

const latestRisk = computed(() => {
  if (archive.value.length > 0) {
    return archive.value[0].riskLevel
  }
  return 'low'
})

const latestRiskLabel = computed(() => {
  const map: Record<string, string> = { low: '低', medium: '中', high: '高' }
  return map[latestRisk.value] || '低'
})

const riskLevelMap: Record<string, string> = {
  low: '低风险',
  medium: '中风险',
  high: '高风险'
}

function goReport(record: any) {
  uni.navigateTo({ url: '/pages/checkup/report?id=' + record.id })
}

function goUpload() {
  uni.navigateTo({ url: '/pages/checkup/upload' })
}

</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F3F7FA;
}

.scroll-content {
  min-height: 100vh;
  box-sizing: border-box;
  padding-bottom: 160rpx;
}

.summary-card {
  position: relative;
  margin: 24rpx 24rpx 0;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 40rpx rgba(43, 111, 240, 0.12);
}
.summary-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #2E6DD1 0%, #1A4FD0 40%, #0D3AAF 100%);
}
.summary-content {
  position: relative;
  padding: 32rpx;
  z-index: 2;
}
.summary-top {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 24rpx;
}
.summary-info {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.summary-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #FFFFFF;
}
.summary-sub {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.7);
}
.summary-stats {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 16rpx;
  padding: 20rpx 0;
  backdrop-filter: blur(4px);
}
.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
}
.stat-num {
  font-size: 36rpx;
  font-weight: 800;
  color: #FFFFFF;
  line-height: 1;
}
.stat-low { color: #4DC580; }
.stat-medium { color: #FFCF8B; }
.stat-high { color: #FF7D7D; }
.stat-label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
}
.stat-divider {
  width: 1rpx;
  height: 48rpx;
  background: rgba(255, 255, 255, 0.15);
}

.timeline-section {
  margin: 24rpx 24rpx 0;
}
.section-header {
  margin-bottom: 20rpx;
}
.section-title-wrap {
  display: flex;
  align-items: center;
}
.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #20364D;
}

.timeline {
  padding-left: 4rpx;
}
.timeline-item {
  display: flex;
  gap: 20rpx;
  padding-bottom: 24rpx;
}
.timeline-item:last-child {
  padding-bottom: 0;
}

.timeline-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 80rpx;
  flex-shrink: 0;
}
.timeline-dot-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}
.timeline-dot {
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  border: 4rpx solid #FFFFFF;
  z-index: 2;
  flex-shrink: 0;
}
.dot-low {
  background: #23956A;
  box-shadow: 0 0 8rpx rgba(0, 180, 42, 0.3), 0 0 0 2rpx #23956A;
}
.dot-medium {
  background: #FF9A2E;
  box-shadow: 0 0 8rpx rgba(255, 154, 46, 0.3), 0 0 0 2rpx #FF9A2E;
}
.dot-high {
  background: #C93D46;
  box-shadow: 0 0 8rpx rgba(245, 63, 63, 0.3), 0 0 0 2rpx #C93D46;
}
.timeline-line {
  width: 2rpx;
  flex: 1;
  background: #E5E6EB;
  margin-top: 8rpx;
  min-height: 80rpx;
}
.timeline-date {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2rpx;
  margin-top: 8rpx;
}
.date-year {
  font-size: 20rpx;
  color: #C9CDD4;
  font-weight: 500;
}
.date-md {
  font-size: 26rpx;
  color: #4E5969;
  font-weight: 700;
}

.record-card {
  flex: 1;
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 20rpx;
  box-shadow: 0 4rpx 20rpx rgba(43, 111, 240, 0.06);
  transition: all 0.2s ease;
}
.record-card:active {
  transform: scale(0.98);
  box-shadow: 0 2rpx 12rpx rgba(43, 111, 240, 0.1);
}

.record-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}
.record-hospital-wrap {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.record-hospital {
  font-size: 26rpx;
  font-weight: 600;
  color: #20364D;
}
.risk-indicator {
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 4rpx 14rpx;
  border-radius: 12rpx;
}
.indicator-low {
  background: rgba(0, 180, 42, 0.08);
}
.indicator-medium {
  background: rgba(255, 154, 46, 0.08);
}
.indicator-high {
  background: rgba(245, 63, 63, 0.08);
}
.indicator-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
}
.indicator-low .indicator-dot { background: #23956A; }
.indicator-medium .indicator-dot { background: #FF9A2E; }
.indicator-high .indicator-dot { background: #C93D46; }
.indicator-text {
  font-size: 22rpx;
  font-weight: 600;
}
.indicator-low .indicator-text { color: #23956A; }
.indicator-medium .indicator-text { color: #FF9A2E; }
.indicator-high .indicator-text { color: #C93D46; }

.record-body {
  margin-bottom: 12rpx;
}
.record-stat-row {
  display: flex;
  align-items: center;
  gap: 24rpx;
}
.record-stat {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.record-stat-label {
  font-size: 22rpx;
  color: #86909C;
  font-weight: 500;
}
.record-stat-value {
  font-size: 23rpx;
  color: #4E5969;
  font-weight: 600;
}
.archive-state {
  margin-top: 20rpx;
  padding: 60rpx 32rpx;
  border-radius: 20rpx;
  background: #FFFFFF;
  text-align: center;
  color: #86909C;
  font-size: 25rpx;
}
.archive-state-title {
  display: block;
  color: #1C2B45;
  font-size: 29rpx;
  font-weight: 700;
}
.archive-state-desc {
  display: block;
  margin-top: 12rpx;
  font-size: 23rpx;
  line-height: 1.6;
}
.abnormal-badge {
  min-width: 36rpx;
  height: 36rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 10rpx;
}
.badge-has {
  background: rgba(245, 63, 63, 0.1);
}
.badge-none {
  background: rgba(0, 180, 42, 0.1);
}
.abnormal-badge-text {
  font-size: 22rpx;
  font-weight: 700;
}
.badge-has .abnormal-badge-text { color: #C93D46; }
.badge-none .abnormal-badge-text { color: #23956A; }

.record-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4rpx;
  padding-top: 12rpx;
  border-top: 1rpx solid #F2F3F5;
}
.record-hint {
  margin-right: auto;
  color: #B6BFCC;
  font-size: 21rpx;
}
.record-action {
  font-size: 24rpx;
  color: #2E6DD1;
  font-weight: 500;
}
.record-arrow {
  font-size: 24rpx;
  color: #2E6DD1;
}

.fab-btn {
  position: fixed;
  right: 32rpx;
  bottom: 120rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 16rpx 28rpx;
  background: linear-gradient(135deg, #2E6DD1 0%, #2E6DD1 100%);
  border-radius: 48rpx;
  box-shadow: 0 8rpx 32rpx rgba(43, 111, 240, 0.4);
  z-index: 100;
  transition: all 0.2s ease;
}
.fab-btn:active {
  transform: scale(0.95);
  box-shadow: 0 4rpx 16rpx rgba(43, 111, 240, 0.3);
}
.fab-icon-wrap {
  width: 40rpx;
  height: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.fab-icon {
  font-size: 36rpx;
  color: #FFFFFF;
  font-weight: 300;
  line-height: 1;
}
.fab-text {
  font-size: 26rpx;
  color: #FFFFFF;
  font-weight: 600;
}

.bottom-safe {
  height: 60rpx;
}
</style>
