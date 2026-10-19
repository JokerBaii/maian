<template>
  <view class="page apple-page">
    <view class="intro">
      <text class="eyebrow">脉安助手</text>
      <text class="title">帮你找到下一步</text>
      <text class="description">找设置、找功能，或挑几篇想看的科普文章。</text>
    </view>
    <view class="input-card">
      <textarea v-model="message" class="message-input" placeholder="例如：在哪里修改心率预警阈值？" :maxlength="300" :auto-height="true" />
      <view class="input-footer"><text>{{ message.length }}/300</text><button class="submit" :disabled="busy || !message.trim()" @tap="submit">{{ busy ? '正在查找…' : '帮我找找' }}</button></view>
    </view>
    <view class="suggestions">
      <text v-for="suggestion in suggestions" :key="suggestion" class="suggestion" @tap="chooseSuggestion(suggestion)">{{ suggestion }}</text>
    </view>
    <view class="model-option">
      <view class="option-copy"><text>允许 AI 理解本次输入</text><text class="option-description">开启后，本次输入将发往模型服务。关闭时使用站内检索。</text></view>
      <app-switch v-model="useModel" />
    </view>
    <text v-if="errorMessage" class="error">{{ errorMessage }}</text>
    <view v-if="result" class="result">
      <view class="result-heading"><text>找到的内容</text><text class="result-mode">{{ result.executionMode === 'SPRING_AI' ? 'AI 工具检索' : result.executionMode === 'RULE_FALLBACK' ? '已切换站内检索' : '站内检索' }}</text></view>
      <text class="reply">{{ result.reply }}</text>
      <view v-for="action in result.actions" :key="action.url" class="action-card" @tap="openAction(action)">
        <view class="action-copy"><text class="action-kind">{{ action.kind === 'ARTICLE' ? '已审核科普' : '功能入口' }}</text><text class="action-title">{{ action.title }}</text><text class="action-description">{{ action.description }}</text></view>
        <text class="chevron">›</text>
      </view>
      <view v-if="result.steps.length" class="trace">
        <text class="trace-toggle" @tap="traceVisible = !traceVisible">{{ traceVisible ? '收起查找过程 −' : '查看查找过程 +' }}</text>
        <text v-for="(step, index) in traceVisible ? result.steps : []" :key="index" class="trace-step">{{ index + 1 }}. {{ step }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AppSwitch from '@/components/AppSwitch.vue'
import { askAssistant, type AssistantAction, type AssistantResult } from '@/api/agent'
import { isAssistantDestination } from '@/utils/assistantNavigation'
import { userFacingError } from '@/utils/presentation'

const message = ref('')
const useModel = ref(false)
const busy = ref(false)
const traceVisible = ref(false)
const errorMessage = ref('')
const result = ref<AssistantResult | null>(null)
const suggestions = ['隐私设置在哪里', '修改心率预警阈值', '推荐 AED 文章', '管理我的设备', '查看资源协同进展']
async function submit() {
  if (busy.value || !message.value.trim()) return
  busy.value = true; errorMessage.value = ''; result.value = null; traceVisible.value = false
  try { result.value = await askAssistant(message.value.trim(), useModel.value) }
  catch (error) { errorMessage.value = userFacingError(error, '暂时无法查找，请稍后重试') }
  finally { busy.value = false }
}
function chooseSuggestion(value: string) { if (busy.value) return; message.value = value; void submit() }
function openAction(action: AssistantAction) {
  if (!isAssistantDestination(action.url)) {
    uni.showToast({ title: '该入口暂不可用', icon: 'none' }); return
  }
  uni.navigateTo({ url: action.url })
}
</script>

<style scoped lang="scss">
.page { min-height: 100vh; box-sizing: border-box; padding: 36rpx 30rpx calc(50rpx + env(safe-area-inset-bottom)); background: #f2f2f7; color: #1c1c1e; }
.intro { padding: 10rpx 4rpx 32rpx; }
.eyebrow { display: block; color: #2865b6; font-size: 24rpx; font-weight: 600; }
.title { display: block; margin-top: 12rpx; font-size: 42rpx; font-weight: 750; }
.description { display: block; margin-top: 14rpx; color: #636366; font-size: 26rpx; line-height: 1.6; }
.input-card { padding: 28rpx; background: #fff; border: 1rpx solid rgba(60,60,67,.1); border-radius: 24rpx; }
.message-input { width: 100%; min-height: 140rpx; font-size: 29rpx; line-height: 1.7; }
.input-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 18rpx; color: #8e8e93; font-size: 22rpx; }
.submit { margin: 0; padding: 0 28rpx; border-radius: 16rpx; color: #fff; background: #2865b6; font-size: 26rpx; line-height: 76rpx; }
.submit[disabled] { color: #8e8e93; background: #eceef2; }
.submit::after { border: 0; }
.suggestions { display: flex; flex-wrap: wrap; gap: 14rpx; margin-top: 22rpx; }
.suggestion { padding: 16rpx 20rpx; border: 1rpx solid #dce2eb; background: #fff; border-radius: 30rpx; font-size: 24rpx; color: #46566c; }
.model-option { display: flex; align-items: center; gap: 20rpx; padding: 30rpx 4rpx; }
.option-copy { flex: 1; font-size: 25rpx; }
.option-description { display: block; margin-top: 8rpx; font-size: 22rpx; line-height: 1.6; color: #8e8e93; }
.error { display: block; padding: 20rpx 0; color: #b33a43; font-size: 26rpx; }
.result { margin-top: 16rpx; }
.result-heading { display: flex; justify-content: space-between; align-items: center; font-weight: 650; font-size: 30rpx; }
.result-mode { font-size: 21rpx; color: #8e8e93; font-weight: 400; }
.reply { display: block; margin: 18rpx 0 24rpx; font-size: 25rpx; line-height: 1.7; color: #636366; }
.action-card { display: flex; align-items: center; gap: 20rpx; padding: 28rpx; margin-bottom: 16rpx; background: #fff; border-radius: 22rpx; }
.action-copy { flex: 1; min-width: 0; }
.action-kind { display: block; color: #2865b6; font-size: 21rpx; }
.action-title { display: block; margin-top: 10rpx; font-size: 29rpx; font-weight: 600; }
.action-description { display: block; margin-top: 10rpx; font-size: 24rpx; color: #636366; line-height: 1.6; }
.chevron { font-size: 38rpx; color: #a7afbb; }
.trace { padding: 16rpx 4rpx; font-size: 23rpx; }
.trace-toggle { color: #2865b6; }
.trace-step { display: block; margin-top: 16rpx; line-height: 1.6; color: #636366; }
</style>
