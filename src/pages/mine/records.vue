<template>
  <view class="page">
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

    <view v-if="currentTab === 'initiated'" class="record-scroll">
      <view v-if="initiatedRecords.length > 0" class="record-list">
        <view
          v-for="record in initiatedRecords"
          :key="record.id"
          class="record-card"
        >
          <view class="record-header">
            <view class="urgency-badge" :class="'urgency-' + record.urgency">
              <text class="urgency-text">{{ urgencyLabel(record.urgency) }}</text>
            </view>
            <view class="status-badge" :class="'status-' + record.status">
              <text class="status-text">{{ statusLabel(record.status) }}</text>
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
        <app-icon-tile class="empty-icon" name="rescue-notice" tone="coral" size="large" />
        <text class="empty-text">暂无发起的救援记录</text>
        <text class="empty-hint">遇到紧急情况可一键呼救</text>
      </view>
    </view>

    <view v-if="currentTab === 'participated'" class="record-scroll">
      <view v-if="participatedRecords.length > 0" class="record-list">
        <view
          v-for="record in participatedRecords"
          :key="record.id"
          class="record-card"
        >
          <view class="record-header">
            <view class="urgency-badge" :class="'urgency-' + record.urgency">
              <text class="urgency-text">{{ urgencyLabel(record.urgency) }}</text>
            </view>
            <view class="status-badge" :class="'status-' + record.status">
              <text class="status-text">{{ statusLabel(record.status) }}</text>
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
        <app-icon-tile class="empty-icon" name="volunteer" tone="green" size="large" />
        <text class="empty-text">暂无参与的救援记录</text>
        <text class="empty-hint">参与救援可积累志愿时长</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import { listRescueCalls, listResponderTasks, type RescueCallResponse } from '@/api/rescue'
import { demoUsers, getDemoUserId } from '@/utils/demoSession'

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
const currentDemoUser = computed(() => demoUsers.find(user => user.id === getDemoUserId()) || demoUsers[0])
const canParticipate = computed(() => ['VOLUNTEER', 'ADMIN'].includes(currentDemoUser.value.role))

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
    urgency: call.urgency.toLowerCase(),
    status: call.status.toLowerCase(),
    createTime: formatTime(call.createdAt)
  }
}

async function loadRecords() {
  try {
    const [initiated, participated] = await Promise.all([
      listRescueCalls(),
      canParticipate.value ? listResponderTasks() : Promise.resolve(null)
    ])
    initiatedRecords.value = initiated.content.map(toRecord)
    participatedRecords.value = participated?.content
      .filter(call => call.responderUserId === getDemoUserId())
      .map(toRecord) || []
  } catch {
    uni.showToast({ title: '救援记录加载失败', icon: 'none' })
  }
}

function urgencyLabel(urgency: string) {
  const map: Record<string, string> = {
    critical: '紧急',
    high: '高',
    medium: '中',
    low: '低'
  }
  return map[urgency] || urgency
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    pending: '等待中',
    matching: '匹配中',
    accepted: '已接单',
    rescuing: '救援中',
    completed: '已完成',
    cancelled: '已取消'
  }
  return map[status] || status
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
  background: #FFFFFF;
  padding: 0 32rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
  position: sticky;
  top: 0;
  z-index: 10;
}
.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 28rpx 0 20rpx;
  position: relative;
}
.tab-text {
  font-size: 30rpx;
  color: #86909C;
  font-weight: 500;
  transition: all 0.2s ease;
}
.tab-text-active {
  color: #2E6DD1;
  font-weight: 700;
}
.tab-indicator {
  position: absolute;
  bottom: 0;
  width: 48rpx;
  height: 6rpx;
  border-radius: 3rpx;
  background: linear-gradient(90deg, #2E6DD1 0%, #2E6DD1 100%);
}

.record-scroll {
  min-height: calc(100vh - 120rpx);
  box-sizing: border-box;
}
.record-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  padding: 24rpx 32rpx;
}
.record-card {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(43, 111, 240, 0.06);
}

.record-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}
.urgency-badge {
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
}
.urgency-critical {
  background: rgba(245, 63, 63, 0.1);
}
.urgency-high {
  background: rgba(255, 154, 46, 0.1);
}
.urgency-medium {
  background: rgba(43, 111, 240, 0.1);
}
.urgency-low {
  background: rgba(0, 180, 42, 0.1);
}
.urgency-text {
  font-size: 22rpx;
  font-weight: 600;
}
.urgency-critical .urgency-text {
  color: #C93D46;
}
.urgency-high .urgency-text {
  color: #FF9A2E;
}
.urgency-medium .urgency-text {
  color: #2E6DD1;
}
.urgency-low .urgency-text {
  color: #23956A;
}
.status-badge {
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
}
.status-completed {
  background: rgba(0, 180, 42, 0.1);
}
.status-rescuing {
  background: rgba(43, 111, 240, 0.1);
}
.status-pending {
  background: rgba(255, 154, 46, 0.1);
}
.status-cancelled {
  background: rgba(134, 144, 156, 0.1);
}
.status-text {
  font-size: 22rpx;
  font-weight: 600;
}
.status-completed .status-text {
  color: #23956A;
}
.status-rescuing .status-text {
  color: #2E6DD1;
}
.status-pending .status-text {
  color: #FF9A2E;
}
.status-cancelled .status-text {
  color: #86909C;
}

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
