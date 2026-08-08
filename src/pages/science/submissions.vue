<template>
  <view class="page apple-page motion-page-list">
    <view v-if="loading" class="state-card">正在加载我的投稿…</view>
    <view v-else-if="!submissions.length" class="state-card">
      <text class="state-title">还没有投稿</text>
      <text class="state-desc">投稿通过审核后会在科普频道展示</text>
      <view class="state-action" @tap="goContribute">
        <text class="state-action-text">去投稿</text>
      </view>
    </view>
    <template v-else>
      <view class="summary">
        <view v-for="item in statusSummary" :key="item.key" class="summary-item">
          <text class="summary-value" :class="'summary-' + item.key">{{ item.count }}</text>
          <text class="summary-label">{{ item.label }}</text>
        </view>
      </view>

      <view class="submission-list">
        <view v-for="item in submissions" :key="item.id" class="submission-card">
          <view class="submission-head">
            <text class="category">{{ categoryLabel(item.category) }}</text>
            <text class="status" :class="'status-' + item.status.toLowerCase()">
              {{ statusLabel(item.status) }}
            </text>
          </view>
          <text class="title">{{ item.title }}</text>
          <text class="content">{{ item.content }}</text>
          <image
            v-if="item.coverImageUrl"
            class="cover"
            :src="resolveApiUrl(item.coverImageUrl)"
            mode="aspectFill"
          />
          <view v-if="item.reviewNote" class="review-note">
            <text class="review-note-label">审核意见</text>
            <text class="review-note-text">{{ item.reviewNote }}</text>
          </view>
          <view class="submission-foot">
            <text class="time">{{ formatTime(item.submittedAt) }}</text>
            <text class="delete-action" @tap="confirmDelete(item)">删除</text>
          </view>
        </view>
      </view>
    </template>
    <view class="bottom-safe"></view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { resolveApiUrl } from '@/api/http'
import { submissionStatusLabel, userFacingError } from '@/utils/presentation'
import {
  listScienceSubmissions,
  deleteScienceSubmission,
  type ScienceSubmissionResponse
} from '@/api/science'

const loading = ref(true)
const submissions = ref<ScienceSubmissionResponse[]>([])

const statusSummary = computed(() => {
  const counters = { pending: 0, approved: 0, rejected: 0 }
  submissions.value.forEach((item) => {
    if (item.status === 'PENDING') counters.pending += 1
    else if (item.status === 'APPROVED') counters.approved += 1
    else counters.rejected += 1
  })
  return [
    { key: 'pending', label: '待审核', count: counters.pending },
    { key: 'approved', label: '已通过', count: counters.approved },
    { key: 'rejected', label: '已驳回', count: counters.rejected }
  ]
})

async function loadSubmissions() {
  try {
    const page = await listScienceSubmissions()
    submissions.value = page.content
  } catch (error: any) {
    uni.showToast({ title: userFacingError(error, '投稿加载失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

onShow(loadSubmissions)

function categoryLabel(category: string) {
  return ({
    device: '设备使用',
    emergency: '突发急症',
    health: '健康管理',
    exercise: '运动养生'
  } as Record<string, string>)[category] || category
}

const statusLabel = submissionStatusLabel

function formatTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function confirmDelete(item: ScienceSubmissionResponse) {
  uni.showModal({
    title: '删除这篇投稿',
    content: `${item.title}\n删除后无法恢复。`,
    confirmText: '删除',
    confirmColor: '#C93D46',
    success: async (result) => {
      if (!result.confirm) return
      try {
        await deleteScienceSubmission(item.id)
        submissions.value = submissions.value.filter(row => row.id !== item.id)
        uni.showToast({ title: '已删除', icon: 'success' })
      } catch (error: any) {
        uni.showToast({ title: userFacingError(error, '删除失败，请重试'), icon: 'none' })
      }
    }
  })
}

function goContribute() {
  uni.navigateTo({ url: '/pages/science/contribute' })
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; box-sizing: border-box; padding: 20rpx 24rpx 40rpx; background: #F3F7FA; }
.state-card,
.submission-card { padding: 28rpx; border: 1rpx solid #E1E9F0; border-radius: 18rpx; background: #FFFFFF; }
.state-card { color: #718197; text-align: center; font-size: 25rpx; }
.state-title { display: block; color: #40536B; font-size: 28rpx; font-weight: 700; }
.state-desc { display: block; margin-top: 8rpx; color: #8A98AA; font-size: 23rpx; }
.state-action { display: inline-block; margin-top: 22rpx; padding: 14rpx 32rpx; border: 1rpx solid #2E6DD1; border-radius: 999rpx; }
.state-action-text { color: #2E6DD1; font-size: 24rpx; font-weight: 650; }

.summary { display: flex; margin-bottom: 18rpx; padding: 24rpx 0; border: 1rpx solid #E1E9F0; border-radius: 18rpx; background: #FFFFFF; }
.summary-item { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 6rpx; }
.summary-value { font-size: 34rpx; font-weight: 800; }
.summary-pending { color: #B4680C; }
.summary-approved { color: #1C7A57; }
.summary-rejected { color: #A33D45; }
.summary-label { color: #8A98AA; font-size: 21rpx; }

.submission-card { margin-bottom: 18rpx; }
.submission-head { display: flex; align-items: center; justify-content: space-between; }
.category { padding: 7rpx 13rpx; border-radius: 10rpx; background: #EAF2FC; color: #2E6DD1; font-size: 21rpx; }
.status { padding: 5rpx 14rpx; border-radius: 999rpx; font-size: 21rpx; font-weight: 650; }
.status-pending { background: rgba(217, 123, 18, 0.12); color: #B4680C; }
.status-approved { background: rgba(28, 122, 87, 0.1); color: #1C7A57; }
.status-rejected { background: rgba(163, 61, 69, 0.1); color: #A33D45; }
.title { display: block; margin-top: 18rpx; color: #20364D; font-size: 30rpx; font-weight: 750; }
.content { display: -webkit-box; margin-top: 12rpx; overflow: hidden; color: #607187; font-size: 24rpx; line-height: 1.7; -webkit-box-orient: vertical; -webkit-line-clamp: 3; }
.cover { width: 100%; height: 240rpx; margin-top: 18rpx; border-radius: 14rpx; }
.review-note { margin-top: 18rpx; padding: 16rpx 18rpx; border-radius: 12rpx; background: #F5F8FB; }
.review-note-label { display: block; color: #8A98AA; font-size: 20rpx; }
.review-note-text { display: block; margin-top: 6rpx; color: #4E5969; font-size: 23rpx; line-height: 1.6; }
.submission-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 20rpx; padding-top: 16rpx; border-top: 1rpx solid #F2F5F8; }
.time { color: #96A2B2; font-size: 21rpx; }
.delete-action { color: #A33D45; font-size: 23rpx; font-weight: 600; }
.bottom-safe { height: calc(32rpx + env(safe-area-inset-bottom)); }
</style>
