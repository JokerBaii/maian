<template>
  <view class="page apple-page motion-page-list">
    <view v-if="loading" class="state-card">正在加载待审核投稿…</view>
    <view v-else-if="!submissions.length" class="state-card">
      <text class="state-title">暂无待审核投稿</text>
      <text class="state-desc">新投稿提交后会进入这里</text>
    </view>
    <view v-else class="submission-list">
      <view v-for="item in submissions" :key="item.id" class="submission-card">
        <view class="submission-head">
          <text class="category">{{ categoryLabel(item.category) }}</text>
          <text class="time">{{ formatTime(item.submittedAt) }}</text>
        </view>
        <text class="title">{{ item.title }}</text>
        <text class="content">{{ item.content }}</text>
        <image v-if="item.coverImageUrl" class="cover" :src="item.coverImageUrl" mode="aspectFill" />
        <view class="actions" :class="{ 'actions-busy': reviewingId === item.id }">
          <view class="reject" @tap="review(item, false)">驳回</view>
          <view class="approve" @tap="review(item, true)">审核通过</view>
        </view>
      </view>
    </view>
    <view class="bottom-safe"></view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  listPendingScienceSubmissions,
  reviewScienceSubmission,
  type ScienceSubmissionResponse
} from '@/api/science'
import { userFacingError } from '@/utils/presentation'

const loading = ref(false)
const submissions = ref<ScienceSubmissionResponse[]>([])

async function loadSubmissions() {
  loading.value = true
  try {
    const page = await listPendingScienceSubmissions()
    submissions.value = page.content
  } catch (error: any) {
    uni.showToast({ title: userFacingError(error, '投稿加载失败'), icon: 'none' })
  } finally {
    loading.value = false
  }
}

function categoryLabel(category: string) {
  return ({ device: '设备使用', emergency: '突发急症', health: '健康管理', exercise: '运动养生' } as Record<string, string>)[category] || category
}

function formatTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : `${date.getMonth() + 1}月${date.getDate()}日`
}

const reviewingId = ref('')

function review(item: ScienceSubmissionResponse, approved: boolean) {
  if (reviewingId.value) return
  reviewingId.value = item.id
  uni.showModal({
    title: approved ? '通过投稿' : '驳回投稿',
    content: approved ? `确认通过《${item.title}》？` : `确认驳回《${item.title}》？`,
    editable: !approved,
    placeholderText: approved ? undefined : '可填写驳回原因',
    confirmText: approved ? '通过' : '驳回',
    confirmColor: approved ? '#23956A' : '#C93D46',
    success: async result => {
      if (!result.confirm) {
        reviewingId.value = ''
        return
      }
      try {
        await reviewScienceSubmission(item.id, approved, result.content?.trim() || undefined)
        submissions.value = submissions.value.filter(candidate => candidate.id !== item.id)
        uni.showToast({ title: approved ? '审核已通过' : '投稿已驳回', icon: 'success' })
      } catch (error: any) {
        uni.showToast({ title: userFacingError(error, '审核失败，请稍后重试'), icon: 'none' })
      } finally {
        reviewingId.value = ''
      }
    },
    fail: () => {
      reviewingId.value = ''
    }
  })
}

onShow(loadSubmissions)
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; box-sizing: border-box; padding: 20rpx 24rpx 40rpx; background: #F3F7FA; }
.state-card,
.submission-card { padding: 28rpx; border: 1rpx solid #E1E9F0; border-radius: 18rpx; background: #FFFFFF; }
.submission-card { margin-bottom: 18rpx; }
.state-card { color: #718197; text-align: center; font-size: 25rpx; }
.state-title,
.state-desc { display: block; }
.state-title { color: #40536B; font-size: 28rpx; font-weight: 700; }
.state-desc { margin-top: 8rpx; color: #8A98AA; font-size: 23rpx; }
.submission-head { display: flex; align-items: center; justify-content: space-between; }
.category { padding: 7rpx 13rpx; border-radius: 10rpx; background: #EAF2FC; color: #2E6DD1; font-size: 21rpx; }
.time { color: #96A2B2; font-size: 21rpx; }
.title { display: block; margin-top: 18rpx; color: #20364D; font-size: 30rpx; font-weight: 750; }
.content { display: -webkit-box; margin-top: 12rpx; overflow: hidden; color: #607187; font-size: 24rpx; line-height: 1.7; -webkit-box-orient: vertical; -webkit-line-clamp: 5; }
.cover { width: 100%; height: 240rpx; margin-top: 18rpx; border-radius: 14rpx; }
.actions { display: flex; gap: 16rpx; margin-top: 24rpx; }
.actions-busy { opacity: 0.55; }
.reject,
.approve { flex: 1; padding: 18rpx; border-radius: 14rpx; text-align: center; font-size: 25rpx; font-weight: 650; }
.reject { background: #F7EDEF; color: #A33D45; }
.approve { background: #E9F5F0; color: #197A57; }
.bottom-safe { height: calc(32rpx + env(safe-area-inset-bottom)); }
</style>
