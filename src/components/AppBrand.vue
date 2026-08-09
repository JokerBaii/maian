<template>
  <view class="app-brand" :class="`app-brand-${size}`">
    <view class="app-brand-mark" aria-hidden="true">
      <view class="brand-stem brand-stem-short"></view>
      <view class="brand-stem brand-stem-tall"></view>
      <view class="brand-stem brand-stem-short"></view>
    </view>
    <view class="app-brand-copy">
      <text class="app-brand-name">脉安驰援</text>
      <text class="app-brand-caption">MAIAN RESPONSE</text>
    </view>
  </view>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ size?: 'compact' | 'large' }>(), { size: 'compact' })
</script>

<style lang="scss" scoped>
.app-brand { display: inline-flex; align-items: center; gap: 14rpx; }
.app-brand-mark {
  display: flex;
  width: 52rpx;
  height: 52rpx;
  align-items: center;
  justify-content: center;
  gap: 4rpx;
  border: 1rpx solid rgba(46, 109, 209, .2);
  border-radius: 13rpx;
  background: rgba(255, 255, 255, .88);
  box-shadow: 0 5rpx 18rpx rgba(23, 61, 102, .08);
  backdrop-filter: blur(12px);
}
.brand-stem { width: 5rpx; border-radius: 4rpx; background: #2E6DD1; }
.brand-stem-short { height: 16rpx; }
.brand-stem-tall { height: 31rpx; background: #B52832; }
.app-brand-copy { display: flex; flex-direction: column; }
.app-brand-name { color: #18334F; font-size: 29rpx; font-weight: 720; letter-spacing: 2rpx; }
.app-brand-caption { margin-top: 2rpx; color: #6A8198; font-size: 15rpx; font-weight: 650; letter-spacing: 2rpx; }
.app-brand-large { gap: 18rpx; }
.app-brand-large .app-brand-mark { width: 64rpx; height: 64rpx; border-radius: 16rpx; }
.app-brand-large .app-brand-name { font-size: 34rpx; }
.app-brand-large .app-brand-caption { font-size: 16rpx; }
</style>
