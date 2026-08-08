<template>
  <view class="page apple-page motion-page-list">
    <view class="scroll-content">
      <view class="summary-card">
        <view class="summary-content">
          <view class="summary-top">
            <app-icon-tile name="folder-add-filled" tone="blue" />
            <view class="summary-info">
              <text class="summary-title">健康档案</text>
              <text class="summary-sub">最近一次体检：{{ latestDate }}</text>
            </view>
            <view class="latest-risk" :class="'latest-risk-' + latestRisk">{{ latestRiskLabel }}风险</view>
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

        <view v-else class="record-list">
          <view
            v-for="(record, idx) in archive"
            :key="idx"
            class="record-item"
            @tap="goReport(record)"
            @longpress="confirmDelete(record)"
          >
            <view class="record-date">
              <text class="date-md">{{ record.date.split('-').slice(1).join('/') }}</text>
              <text class="date-year">{{ record.date.split('-')[0] }}</text>
            </view>

            <view class="record-card">
              <view class="record-header">
                <text class="record-hospital">{{ record.hospital }}</text>
                <view class="risk-indicator" :class="'indicator-' + record.riskLevel">
                  <view class="indicator-dot"></view>
                  <text class="indicator-text">{{ riskLevelMap[record.riskLevel] }}</text>
                </view>
              </view>
              <view class="record-body">
                <text :class="record.abnormalCount > 0 ? 'abnormal-has' : 'abnormal-none'">{{ record.abnormalCount }} 项异常指标</text>
              </view>
              <view class="record-footer">
                <text class="record-action">查看报告</text>
                <text class="record-arrow">›</text>
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
import { userFacingError } from '@/utils/presentation'

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
        uni.showToast({ title: userFacingError(error, '删除失败，请重试'), icon: 'none' })
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

.summary-card { margin: 24rpx 24rpx 0; overflow: hidden; border: 1rpx solid rgba(60,60,67,.14); border-radius: 22rpx; background: #FFFFFF; box-shadow: none; }
.summary-content {
  padding: 24rpx;
}
.summary-top {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 20rpx;
}
.summary-info {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.summary-title {
  font-size: 29rpx;
  font-weight: 700;
  color: #20364D;
}
.summary-sub {
  font-size: 20rpx;
  color: #8190A2;
}
.latest-risk { margin-left: auto; padding: 6rpx 10rpx; border-radius: 9rpx; font-size: 18rpx; font-weight: 700; }.latest-risk-low { background: #E9F5F0; color: #197C59; }.latest-risk-medium { background: #FFF3E3; color: #A86B1F; }.latest-risk-high { background: #F9EAEC; color: #B52B36; }
.summary-stats {
  display: flex;
  align-items: center;
  border-top: 1rpx solid #E8EDF2;
  padding: 18rpx 0 0;
}
.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
}
.stat-num {
  font-size: 29rpx;
  font-weight: 800;
  color: #29435D;
  line-height: 1;
}
.stat-low { color: #23956A; }.stat-medium { color: #C98327; }.stat-high { color: #C93D46; }
.stat-label {
  font-size: 22rpx;
  color: #8996A5;
  font-weight: 500;
}
.stat-divider {
  width: 1rpx;
  height: 48rpx;
  background: #E3E9EF;
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

.record-list { overflow: hidden; border: 1rpx solid #E0E7EE; border-radius: 20rpx; background: #FFFFFF; }
.record-item { position: relative; display: grid; grid-template-columns: 82rpx minmax(0,1fr); gap: 18rpx; padding: 22rpx 20rpx; }
.record-item::after { content: ''; position: absolute; right: 20rpx; bottom: 0; left: 20rpx; height: 1rpx; background: #E9EEF3; }.record-item:last-child::after { display: none; }
.record-date { display: flex; flex-direction: column; align-items: flex-start; padding-top: 2rpx; }
.date-year {
  font-size: 20rpx;
  color: #98A5B4;
  font-weight: 500;
}
.date-md {
  order: -1;
  font-size: 27rpx;
  color: #31485F;
  font-weight: 750;
}

.record-card {
  min-width: 0;
  transition: all 0.2s ease;
}
.record-card:active {
  opacity: .7;
}

.record-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}
.record-hospital {
  min-width: 0;
  overflow: hidden;
  font-size: 25rpx;
  font-weight: 680;
  color: #20364D;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  margin-top: 10rpx;
  font-size: 21rpx;
}
.abnormal-has { color: #B74B53; }.abnormal-none { color: #23825F; }
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
.record-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4rpx;
  margin-top: 12rpx;
}
.record-action {
  font-size: 21rpx;
  color: #2E6DD1;
  font-weight: 500;
}
.record-arrow {
  font-size: 28rpx;
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
  background: #007AFF;
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
