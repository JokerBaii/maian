<template>
  <view class="page apple-page motion-page-list">
    <view class="tab-bar">
      <view
        class="tab-item"
        :class="{ 'tab-active': currentTab === 'initiated' }"
        @tap="currentTab = 'initiated'"
      >
        <text class="tab-text" :class="{ 'tab-text-active': currentTab === 'initiated' }">我发起的</text>
        <view v-if="currentTab === 'initiated'" class="tab-indicator"></view>
      </view>
      <view
        v-if="canParticipate"
        class="tab-item"
        :class="{ 'tab-active': currentTab === 'participated' }"
        @tap="currentTab = 'participated'"
      >
        <text class="tab-text" :class="{ 'tab-text-active': currentTab === 'participated' }">我参与的</text>
        <view v-if="currentTab === 'participated'" class="tab-indicator"></view>
      </view>
    </view>

    <view :key="currentTab" class="record-scroll">
      <view v-if="currentRecords.length > 0" class="record-list">
        <view
          v-for="record in currentRecords"
          :key="record.id"
          class="record-card"
          @tap="goDetail(record.id)"
        >
          <view class="record-header">
            <text class="urgency-text" :class="'urgency-' + record.urgency.toLowerCase()">{{ urgencyLabel(record.urgency) }}</text>
            <view class="status-copy" :class="'status-' + statusTone(record.status)">
              <view class="status-dot"></view>
              <text class="status-text">{{ statusLabel(record.status) }}</text>
              <text class="record-chevron">›</text>
            </view>
          </view>
          <view class="record-body">
            <text class="record-desc">{{ record.description }}</text>
            <view class="record-info-row">
              <app-icon class="record-info-icon" name="location-filled" :size="14" color="#8994A8" />
              <text class="record-info-text">{{ record.address }}</text>
            </view>
            <view class="record-info-row">
              <app-icon class="record-info-icon" name="calendar" :size="14" color="#8994A8" />
              <text class="record-info-text">{{ record.createTime }}</text>
            </view>
          </view>
        </view>
      </view>
      <view v-else class="empty-state">
        <app-icon-tile
          class="empty-icon"
          :name="currentTab === 'initiated' ? 'rescue-notice' : 'volunteer'"
          :tone="currentTab === 'initiated' ? 'coral' : 'green'"
          size="large"
        />
        <text class="empty-text">暂无{{ currentTab === 'initiated' ? '发起' : '参与' }}的救援记录</text>
        <text class="empty-hint">{{ currentTab === 'initiated' ? '遇到紧急情况可一键呼救' : '参与救援可积累志愿时长' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import {
  listRescueCalls,
  listResponderTasks,
  type RescueCallResponse,
  type ResponderTaskResponse
} from '@/api/rescue'
import { getCurrentProfile } from '@/api/user'
import { rescueStatusLabel, rescueUrgencyLabel } from '@/utils/presentation'

const currentTab = ref<'initiated' | 'participated'>('initiated')

interface RescueRecordView {
  id: string
  address: string
  description: string
  urgency: string
  status: string
  createTime: string
}

const initiatedRecords = ref<RescueRecordView[]>([])
const participatedRecords = ref<RescueRecordView[]>([])
const currentRecords = computed(() => currentTab.value === 'initiated' ? initiatedRecords.value : participatedRecords.value)
const currentRole = ref('USER')
const canParticipate = computed(() => ['VOLUNTEER', 'ADMIN'].includes(currentRole.value))

function formatTime(value: string) {
  const date = new Date(value)
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function toRecord(call: RescueCallResponse): RescueRecordView {
  return {
    id: call.id,
    address: call.address,
    description: call.description || call.symptoms.join('、') || '紧急救援请求',
    urgency: call.urgency,
    status: call.status,
    createTime: formatTime(call.createdAt)
  }
}

function responderToRecord(call: ResponderTaskResponse): RescueRecordView {
  return {
    id: call.id,
    address: call.address || '接单后可查看精确位置',
    description: call.description || call.symptoms.join('、') || '紧急救援请求',
    urgency: call.urgency,
    status: call.status,
    createTime: formatTime(call.createdAt)
  }
}

function goDetail(rescueId: string) {
  const mode = currentTab.value === 'participated' ? '&mode=responder' : ''
  uni.navigateTo({ url: `/pages/rescue/detail?id=${encodeURIComponent(rescueId)}${mode}` })
}

async function loadRecords() {
  try {
    const profile = await getCurrentProfile()
    currentRole.value = profile.role
    const [initiated, participated] = await Promise.all([
      listRescueCalls(),
      canParticipate.value ? listResponderTasks() : Promise.resolve(null)
    ])
    initiatedRecords.value = initiated.content.map(toRecord)
    participatedRecords.value = participated?.content
      .filter(call => call.detailAvailable)
      .map(responderToRecord) || []
  } catch {
    uni.showToast({ title: '救援记录加载失败', icon: 'none' })
  }
}

const urgencyLabel = rescueUrgencyLabel
const statusLabel = rescueStatusLabel

function statusTone(status: string) {
  if (status === 'COMPLETED') return 'completed'
  if (['EN_ROUTE_TO_AED', 'EN_ROUTE_TO_REQUESTER', 'ARRIVED', 'RESCUING'].includes(status)) return 'active'
  if (['PENDING', 'MATCHING', 'PENDING_CONFIRMATION'].includes(status)) return 'waiting'
  if (['NO_RESOURCE', 'EXPIRED', 'SYSTEM_FAILED'].includes(status)) return 'attention'
  return 'ended'
}

onShow(loadRecords)
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F3F7FA;
}

.tab-bar {
  display: flex;
  gap: 4rpx;
  margin: 18rpx 24rpx 0;
  padding: 4rpx;
  border-radius: 16rpx;
  background: #E8E8ED;
  position: sticky;
  top: 14rpx;
  z-index: 10;
}
.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 58rpx;
  border-radius: 12rpx;
  position: relative;
}
.tab-active { background: #FFFFFF; box-shadow: 0 1rpx 4rpx rgba(0, 0, 0, .10); }
.tab-text {
  font-size: 24rpx;
  color: #86909C;
  font-weight: 500;
  transition: all 0.2s ease;
}
.tab-text-active {
  color: #2E6DD1;
  font-weight: 700;
}
.tab-indicator {
  display: none;
}

.record-scroll {
  min-height: calc(100vh - 120rpx);
  box-sizing: border-box;
  animation: recordSegmentChange 300ms cubic-bezier(.2, .72, .2, 1) both;
}
@keyframes recordSegmentChange {
  from { opacity: 0; transform: translateY(8rpx); }
  to { opacity: 1; transform: translateY(0); }
}
.record-list {
  margin: 24rpx;
  overflow: hidden;
  border: 1rpx solid #E0E7EE;
  border-radius: 22rpx;
  background: #FFFFFF;
  box-shadow: none;
}
.record-card {
  position: relative;
  background: #FFFFFF;
  padding: 26rpx 24rpx;
}
.record-card::after {
  content: '';
  position: absolute;
  right: 24rpx;
  bottom: 0;
  left: 24rpx;
  height: 1rpx;
  background: #E8EDF2;
}
.record-card:last-child::after {
  display: none;
}
.record-card:active {
  background: #F8FAFC;
}

.record-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}
.urgency-text {
  font-size: 22rpx;
  font-weight: 650;
}
.urgency-critical { color: #C93D46; }
.urgency-high { color: #D17709; }
.urgency-medium { color: #2E6DD1; }
.urgency-low { color: #23956A; }
.status-copy { display: flex; align-items: center; gap: 7rpx; }
.status-dot { width: 9rpx; height: 9rpx; border-radius: 50%; background: currentColor; }
.status-text {
  font-size: 22rpx;
  font-weight: 500;
}
.status-completed { color: #23956A; }
.status-active { color: #2E6DD1; }
.status-waiting { color: #B66A10; }
.status-attention { color: #B52F3A; }
.status-ended { color: #86909C; }
.record-chevron { margin-left: 2rpx; color: #C7C7CC; font-size: 32rpx; line-height: 1; }

.record-body {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}
.record-desc {
  font-size: 28rpx;
  color: #20364D;
  font-weight: 600;
  line-height: 1.5;
}
.record-info-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.record-info-icon {
  font-size: 24rpx;
  flex-shrink: 0;
}
.record-info-text {
  font-size: 24rpx;
  color: #86909C;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 0;
}
.empty-icon {
  margin-bottom: 24rpx;
}
.empty-text {
  font-size: 30rpx;
  color: #86909C;
  font-weight: 500;
  margin-bottom: 8rpx;
}
.empty-hint {
  font-size: 24rpx;
  color: #C9CDD4;
}
</style>
