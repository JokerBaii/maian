<template>
  <view class="app-list-row app-motion-control" :class="{ 'app-list-row-compact': compact }">
    <app-icon-tile v-if="icon" class="app-list-icon" :name="icon" :tone="tone" />
    <view class="app-list-copy">
      <view class="app-list-title-line">
        <text class="app-list-title">{{ title }}</text>
        <text v-if="trailing" class="app-list-trailing">{{ trailing }}</text>
      </view>
      <text v-if="summary" class="app-list-summary">{{ summary }}</text>
      <text v-if="meta" class="app-list-meta">{{ meta }}</text>
    </view>
    <image v-if="thumbnail" class="app-list-thumbnail" :src="thumbnail" mode="aspectFill" />
    <slot name="accessory">
      <text v-if="showChevron" class="app-list-chevron">›</text>
    </slot>
  </view>
</template>

<script setup lang="ts">
import AppIconTile from '@/components/AppIconTile.vue'

withDefaults(defineProps<{
  icon?: string
  tone?: 'blue' | 'green' | 'coral' | 'cyan' | 'violet' | 'slate'
  title: string
  summary?: string
  meta?: string
  trailing?: string
  thumbnail?: string
  showChevron?: boolean
  compact?: boolean
}>(), {
  icon: '',
  tone: 'blue',
  summary: '',
  meta: '',
  trailing: '',
  thumbnail: '',
  showChevron: true,
  compact: false
})
</script>

<style lang="scss" scoped>
.app-list-row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 18rpx;
  min-height: 126rpx;
  padding: 20rpx 22rpx 20rpx 0;
  box-sizing: border-box;
}
.app-list-row::after {
  content: '';
  position: absolute;
  right: 0;
  bottom: 0;
  left: 78rpx;
  height: 1rpx;
  background: rgba(60, 60, 67, .14);
}
.app-list-row:last-child::after { display: none; }
.app-list-row:active { background: rgba(118, 118, 128, .08); }
.app-list-icon { flex: none; }
.app-list-copy { flex: 1; min-width: 0; }
.app-list-title-line { display: flex; align-items: baseline; gap: 12rpx; }
.app-list-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: #1C1C1E;
  font-size: 29rpx;
  font-weight: 650;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.app-list-trailing { flex: none; color: #8E8E93; font-size: 22rpx; }
.app-list-summary {
  display: -webkit-box;
  margin-top: 6rpx;
  overflow: hidden;
  color: #636366;
  font-size: 24rpx;
  line-height: 1.4;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.app-list-meta { display: block; margin-top: 5rpx; overflow: hidden; color: #8E8E93; font-size: 22rpx; text-overflow: ellipsis; white-space: nowrap; }
.app-list-thumbnail { width: 82rpx; height: 82rpx; flex: none; border-radius: 12rpx; }
.app-list-chevron { flex: none; color: #C7C7CC; font-size: 34rpx; line-height: 1; }
.app-list-row-compact { min-height: 104rpx; padding-top: 14rpx; padding-bottom: 14rpx; }
.app-list-row-compact .app-list-title { font-size: 28rpx; }
.app-list-row-compact .app-list-meta { margin-top: 2rpx; }
</style>
