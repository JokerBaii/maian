<template>
  <view class="rescue-review">
    <view class="review-heading"><text>救援复盘</text><button class="review-options-toggle" aria-label="复盘选项" :aria-expanded="optionsVisible" @tap="optionsVisible = !optionsVisible"><app-icon name="more-filled" :size="19" color="#8E8E93" /></button></view>
    <view v-if="optionsVisible" class="review-options">
      <view class="review-option-row"><text>AI 协同整理</text><app-switch v-model="useModel" :disabled="busy" /></view>
      <text class="review-hint">仅使用流程状态和数量，开启后在下次整理时生效。</text>
    </view>
    <text class="review-overview" :class="{ 'review-overview-stale': review?.stale }">{{ overview }}</text>
    <text v-if="errorMessage" class="review-error">{{ errorMessage }}</text>
    <view v-if="ready && !review?.stale && pendingCount" class="review-todos">
      <view v-if="review?.data?.returnPending" class="review-todo"><text>AED 待归还</text><button v-if="isResponder" class="review-link" @tap="$emit('focus-return')">去归还 ›</button><text v-else class="review-hint">等待志愿者确认</text></view>
      <view v-if="review?.data?.feedbackPending" class="review-todo"><text>救援待评价</text><button v-if="!isResponder" class="review-link" @tap="$emit('focus-feedback')">去评价 ›</button><text v-else class="review-hint">等待求救者填写</text></view>
    </view>
    <button v-if="!busy && (!ready || review?.stale)" class="review-generate" @tap="generate">{{ review?.stale ? '更新复盘' : review?.status === 'FAILED' ? '重新整理' : '整理救援记录' }}</button>
    <view v-if="ready && !review?.stale" class="review-footer">
      <button class="review-details-toggle" :aria-expanded="detailsVisible" @tap="detailsVisible = !detailsVisible"><text>{{ detailsVisible ? '收起详情' : '查看详情' }}</text><app-icon :name="detailsVisible ? 'up' : 'down'" :size="11" color="#8E8E93" /></button>
      <button class="review-refresh" :disabled="busy" @tap="generate">更新</button>
    </view>
    <view v-if="detailsVisible && ready && !review?.stale" class="review-details">
      <view class="review-tabs" role="tablist"><button v-for="tab in tabs" :key="tab.key" class="review-tab" :class="{ 'review-tab-active': detailTab === tab.key }" role="tab" :aria-selected="detailTab === tab.key" @tap="detailTab = tab.key">{{ tab.label }}</button></view>
      <view v-if="detailTab === 'events'" class="review-events">
        <view v-if="metrics.length" class="review-metrics"><view v-for="metric in metrics" :key="metric.label" class="review-metric-row"><text>{{ metric.label }}</text><text>{{ formatDuration(metric.seconds) }}</text></view></view>
        <view v-for="(event, index) in review?.data?.timeline" :key="index" class="review-event"><view class="review-event-dot"></view><view><text>{{ event.summary }}</text><text class="review-hint">{{ formatTime(event.occurredAt) }}</text></view></view>
        <text v-if="!review?.data?.timeline.length && !metrics.length" class="review-empty">暂无已记录的救援经过。</text>
      </view>
      <view v-else-if="detailTab === 'process'" class="review-process">
        <view v-for="(step, index) in review?.steps" :key="index" class="review-step"><text class="review-step-number">{{ index + 1 }}</text><view><text class="review-step-title">{{ step.module }}</text><text class="review-hint">{{ step.summary }}</text></view></view>
        <text v-if="!review?.steps.length" class="review-empty">暂无整理过程。</text>
      </view>
      <view v-else class="review-articles">
        <button v-for="article in review?.articles" :key="article.id" class="review-article" @tap="openArticle(article.id)"><text>{{ article.title }}</text><app-icon name="right" :size="12" color="#C7C7CC" /></button>
        <text v-if="!review?.articles.length" class="review-empty">暂无相关学习资料。</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import AppSwitch from '@/components/AppSwitch.vue'
import AppIcon from '@/components/AppIcon.vue'
import { getRescueReview, requestRescueReview, type RescueReview } from '@/api/rescueReview'
import { createRescueReviewPolling } from '@/utils/rescueReviewPolling'
import { userFacingError } from '@/utils/presentation'

const props = defineProps<{ rescueId: string; isResponder: boolean; visible: boolean; sourceSequence: number; feedbackSubmitted: boolean }>()
defineEmits<{ 'focus-return': []; 'focus-feedback': [] }>()
const review = ref<RescueReview | null>(null)
const useModel = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const detailsVisible = ref(false)
const optionsVisible = ref(false)
const detailTab = ref<'events' | 'process' | 'learning'>('events')
const tabs = [{ key: 'events', label: '救援经过' }, { key: 'process', label: '整理过程' }, { key: 'learning', label: '学习资料' }] as const
const ready = computed(() => review.value?.status === 'COMPLETED' && !!review.value.data)
const metrics = computed(() => (review.value?.data?.metrics || []).filter(metric => metric.seconds != null))
const pendingCount = computed(() => Number(!!review.value?.data?.returnPending) + Number(!!review.value?.data?.feedbackPending))
const overview = computed(() => {
  if (review.value?.stale) return '救援记录有更新，请重新整理。'
  if (busy.value) return '正在核对救援经过与待办…'
  if (!ready.value) return review.value?.summary || '整理救援经过，跟进归还与评价。'
  return pendingCount.value ? `还有 ${pendingCount.value} 项待处理` : '本次救援暂无后续待办。'
})
let generation = 0
const busy = computed(() => submitting.value || ['PENDING', 'RUNNING'].includes(review.value?.status || ''))
const polling = createRescueReviewPolling(() => getRescueReview(props.rescueId), value => {
  review.value = value; errorMessage.value = ''
}, error => { errorMessage.value = userFacingError(error, '暂时无法读取复盘记录') })

watch(() => [props.visible, props.rescueId, props.sourceSequence, props.feedbackSubmitted], (value, previous) => {
  generation++
  polling.stop()
  if (value[1] !== previous?.[1]) { review.value = null; detailsVisible.value = false; optionsVisible.value = false; detailTab.value = 'events' }
  if (props.visible && props.rescueId) polling.start()
}, { immediate: true })

async function generate() {
  if (busy.value || !props.visible) return
  submitting.value = true; errorMessage.value = ''
  const run = generation
  polling.stop()
  try {
    const result = await requestRescueReview(props.rescueId, useModel.value)
    if (props.visible && run === generation) { review.value = result; polling.start() }
  } catch (error) {
    if (props.visible && run === generation) errorMessage.value = userFacingError(error, '复盘请求未完成，请重试')
  } finally { submitting.value = false }
}
function formatDuration(seconds: number | null) {
  if (seconds == null) return '未记录'
  if (seconds < 60) return `${seconds}秒`
  return `${Math.floor(seconds / 60)}分${seconds % 60}秒`
}
function formatTime(value: string) { return new Date(value).toLocaleString('zh-CN', { hour12: false }) }
function openArticle(id: string) { uni.navigateTo({ url: `/pages/science/detail?id=${encodeURIComponent(id)}` }) }
onUnmounted(() => { generation++; polling.stop() })
</script>

<style scoped lang="scss">
.rescue-review { color: #1C1C1E; font-size: 24rpx; line-height: 1.65; }
.rescue-review button { margin: 0; padding: 0; border: 0; background: transparent; font-size: inherit; line-height: inherit; color: inherit; }
.rescue-review button::after { border: 0; }
.review-heading, .review-option-row, .review-todo, .review-footer, .review-metric-row, .review-article { display: flex; align-items: center; justify-content: space-between; gap: 16rpx; }
.review-heading { font-size: 28rpx; font-weight: 700; }
.rescue-review .review-options-toggle { display: flex; flex: none; align-items: center; justify-content: center; width: 48rpx; height: 48rpx; margin: -8rpx -8rpx -8rpx 0; }
.review-overview, .review-error { display: block; margin-top: 12rpx; font-size: 24rpx; color: #636366; }
.review-overview-stale { color: #C97818; }
.review-error { color: #B33A43; }
.review-options { margin-top: 16rpx; padding: 16rpx 0; border-top: 1rpx solid rgba(60,60,67,.12); border-bottom: 1rpx solid rgba(60,60,67,.12); }
.review-option-row { min-height: 52rpx; }
.review-hint { display: block; margin-top: 6rpx; color: #8E8E93; font-size: 22rpx; font-weight: 400; line-height: 1.6; }
.review-todos { margin-top: 12rpx; }
.review-todo { min-height: 64rpx; }
.review-todo > text:first-child { flex: 1; }
.review-todo .review-hint { flex: none; margin-top: 0; }
.rescue-review .review-link { flex: none; color: #007AFF; }
.rescue-review .review-generate { display: flex; align-items: center; justify-content: center; width: 100%; min-height: 72rpx; margin-top: 20rpx; padding: 14rpx 20rpx; box-sizing: border-box; border-radius: 14rpx; background: #EDF4FF; color: #007AFF; font-size: 26rpx; line-height: 1.4; }
.review-footer { margin-top: 18rpx; }
.rescue-review .review-details-toggle { display: flex; align-items: center; gap: 8rpx; color: #636366; font-size: 23rpx; }
.rescue-review .review-refresh { color: #007AFF; font-size: 23rpx; }
.review-refresh[disabled] { opacity: .4; }
.review-details { margin-top: 18rpx; border-top: 1rpx solid rgba(60,60,67,.12); }
.review-tabs { display: flex; gap: 28rpx; padding-top: 4rpx; }
.rescue-review .review-tab { position: relative; padding: 15rpx 0; color: #8E8E93; font-size: 23rpx; }
.rescue-review .review-tab-active { color: #1C1C1E; font-weight: 600; }
.review-tab-active::before { content: ''; position: absolute; bottom: 0; left: 0; right: 0; height: 3rpx; background: #007AFF; border-radius: 2rpx; }
.review-metrics { padding: 10rpx 0; border-bottom: 1rpx solid rgba(60,60,67,.1); }
.review-metric-row { padding: 6rpx 0; color: #636366; font-size: 22rpx; }
.review-metric-row > text:last-child { color: #1C1C1E; font-weight: 600; }
.review-event, .review-step { display: flex; align-items: flex-start; gap: 16rpx; padding-top: 20rpx; }
.review-event > view:last-child, .review-step > view { flex: 1; min-width: 0; overflow-wrap: anywhere; }
.review-event-dot { flex: none; width: 10rpx; height: 10rpx; margin-top: 14rpx; border-radius: 50%; background: #AAB8C7; }
.review-step-number { flex: none; width: 32rpx; margin-top: 2rpx; color: #8E8E93; text-align: center; font-size: 22rpx; }
.review-step-title { font-weight: 600; }
.rescue-review .review-article { width: 100%; padding: 20rpx 0; text-align: left; }
.review-article + .review-article { border-top: 1rpx solid rgba(60,60,67,.1); }
.review-article > text { flex: 1; min-width: 0; }
.review-empty { display: block; padding: 20rpx 0 4rpx; color: #8E8E93; font-size: 23rpx; }
</style>
