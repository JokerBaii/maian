<template>
  <view class="app-nav" :style="{ paddingTop: statusBarHeight + 'px' }">
    <view class="app-nav-content">
      <view class="app-nav-side app-nav-leading" @tap="emit('back')">
        <app-icon v-if="back" name="back" :size="22" color="#007AFF" />
      </view>
      <text class="app-nav-title">{{ title }}</text>
      <view class="app-nav-side app-nav-trailing" @tap="emit('action')">
        <app-icon v-if="actionIcon" :name="actionIcon" :size="21" color="#007AFF" />
        <text v-else-if="actionText" class="app-nav-action-text">{{ actionText }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import AppIcon from '@/components/AppIcon.vue'

withDefaults(defineProps<{
  title: string
  back?: boolean
  actionIcon?: string
  actionText?: string
}>(), {
  back: false,
  actionIcon: '',
  actionText: ''
})

const emit = defineEmits<{ back: []; action: [] }>()
const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 20
</script>

<style scoped>
.app-nav {
  position: fixed;
  z-index: 1000;
  top: 0;
  right: 0;
  left: 0;
  border-bottom: 1rpx solid rgba(60, 60, 67, .12);
  background: rgba(249, 249, 251, .94);
  backdrop-filter: saturate(160%) blur(24rpx);
  -webkit-backdrop-filter: saturate(160%) blur(24rpx);
}
.app-nav-content { position: relative; display: flex; height: 88rpx; align-items: center; justify-content: center; }
.app-nav-title { max-width: 66%; overflow: hidden; color: #1C1C1E; font-size: 32rpx; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.app-nav-side { position: absolute; top: 0; display: flex; min-width: 88rpx; height: 88rpx; align-items: center; }
.app-nav-leading { left: 12rpx; justify-content: flex-start; }
.app-nav-trailing { right: 18rpx; justify-content: flex-end; }
.app-nav-action-text { color: #007AFF; font-size: 24rpx; }
.app-nav-side:active { opacity: .45; }
</style>
