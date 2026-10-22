<template>
  <view class="assistant-page"><assistant-panel full-page @close="goBack" /></view>
</template>

<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import AssistantPanel from '@/components/AssistantPanel.vue'
import { assistantSession } from '@/composables/useAssistantSession'

onShow(() => assistantSession.syncAccount())
function goBack() {
  if (getCurrentPages().length > 1) uni.navigateBack()
  else uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style scoped>
.assistant-page { height: calc(100vh - var(--window-top,0px)); height: calc(100dvh - var(--window-top,0px)); }
</style>
