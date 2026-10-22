<template>
  <view class="page apple-page motion-page-list">
    <view class="queue-heading">
      <text class="queue-count">{{ loading ? '正在同步' : `${submissions.length} 篇待确认` }}</text>
    </view>
    <view v-if="loading" class="state-card">正在加载内容…</view>
    <view v-else-if="!submissions.length" class="state-card">
      <text class="state-title">内容都已处理</text>
      <text class="state-desc">新投稿会出现在这里</text>
    </view>
    <view v-else class="review-list">
      <app-list-row
        v-for="item in submissions"
        :key="item.id"
        class="review-row"
        :class="{ 'review-row-busy': reviewingId === item.id }"
        :icon="categoryIcon(item.category)"
        :tone="categoryTone(item.category)"
        :title="item.title"
        :meta="categoryLabel(item.category)"
        :trailing="formatTime(item.submittedAt)"
        :thumbnail="item.coverImageUrl || ''"
        compact
        @tap="openReviewActions(item)"
      />
    </view>
    <app-action-sheet
      v-model:visible="sheetVisible"
      :title="selectedSubmission ? `《${selectedSubmission.title}》` : ''"
      :message="selectedSubmission ? selectedSubmission.content.slice(0, 58) : ''"
      :actions="reviewActions"
      @select="handleReviewAction"
    />
    <view class="bottom-safe"></view>
    <assistant-companion context="science" :subject="{ submissionId: selectedSubmission?.id }" :hidden="sheetVisible" />
  </view>
</template>

<script setup lang="ts">
import { useAssistantRefresh } from '@/composables/useAssistantRefresh'
import AssistantCompanion from '@/components/AssistantCompanion.vue'
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppActionSheet, { type AppActionSheetItem } from '@/components/AppActionSheet.vue'
import AppListRow from '@/components/AppListRow.vue'
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

function categoryIcon(category: string) {
  return ({ device: 'fixed-device', emergency: 'rescue-notice', health: 'health-alert', exercise: 'volunteer' } as Record<string, string>)[category] || 'science-update'
}

function categoryTone(category: string): 'blue' | 'coral' | 'green' | 'cyan' {
  return ({ device: 'blue', emergency: 'coral', health: 'cyan', exercise: 'green' } as const)[category as 'device' | 'emergency' | 'health' | 'exercise'] || 'blue'
}

function formatTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : `${date.getMonth() + 1}月${date.getDate()}日`
}

const reviewingId = ref('')
const sheetVisible = ref(false)
const selectedSubmission = ref<ScienceSubmissionResponse | null>(null)
const reviewActions: AppActionSheetItem[] = [
  { label: '确认发布' },
  { label: '退回投稿者修改', destructive: true }
]

function openReviewActions(item: ScienceSubmissionResponse) {
  if (reviewingId.value) return
  selectedSubmission.value = item
  sheetVisible.value = true
}

function handleReviewAction(index: number) {
  if (!selectedSubmission.value) return
  review(selectedSubmission.value, index === 0)
}

async function review(item: ScienceSubmissionResponse, approved: boolean) {
  if (reviewingId.value) return
  reviewingId.value = item.id
  try {
    await reviewScienceSubmission(item.id, approved, approved ? undefined : '请修改内容后重新提交')
    submissions.value = submissions.value.filter(candidate => candidate.id !== item.id)
    selectedSubmission.value = null
    uni.showToast({ title: approved ? '内容已发布' : '已退回投稿者', icon: 'success' })
  } catch (error: any) {
    uni.showToast({ title: userFacingError(error, '处理失败，请稍后重试'), icon: 'none' })
  } finally {
    reviewingId.value = ''
  }
}

onShow(loadSubmissions)
useAssistantRefresh(loadSubmissions)
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; box-sizing: border-box; padding: 20rpx 0 40rpx; background: #F2F2F7; color: #1C1C1E; }
.queue-heading { padding: 4rpx 28rpx 14rpx; }
.queue-count { display: block; font-size: 34rpx; font-weight: 750; letter-spacing: -.03em; }
.state-card { margin: 0 24rpx; padding: 72rpx 30rpx; border-radius: 20rpx; background: #FFFFFF; color: #8E8E93; text-align: center; font-size: 24rpx; }
.state-title,
.state-desc { display: block; }
.state-title { color: #1C1C1E; font-size: 29rpx; font-weight: 650; }
.state-desc { margin-top: 10rpx; font-size: 22rpx; }
.review-list { padding-left: 24rpx; background: #FFFFFF; }
.review-row-busy { opacity: .48; pointer-events: none; }
.bottom-safe { height: calc(32rpx + env(safe-area-inset-bottom)); }
</style>
