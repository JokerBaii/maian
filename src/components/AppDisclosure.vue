<template>
  <view class="app-disclosure">
    <button class="app-disclosure-heading" :aria-expanded="expanded" @tap="expanded = !expanded"><text>{{ title }}</text><app-icon :name="expanded ? 'up' : 'down'" :size="12" color="#8E8E93" /></button>
    <view v-if="expanded" class="app-disclosure-content"><slot /></view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
defineProps<{ title: string }>()
const expanded = ref(false)
</script>

<style scoped lang="scss">
.app-disclosure { min-width: 0; }
.app-disclosure-heading { display: flex; align-items: center; justify-content: space-between; gap: 20rpx; width: 100%; margin: 0; padding: 0; border: 0; background: transparent; color: var(--care-ink, #1C1C1E); text-align: left; font-size: 26rpx; font-weight: 500; line-height: 1.65; }
.app-disclosure-heading::after { border: 0; }
.app-disclosure-content { margin-top: 20rpx; }
</style>
