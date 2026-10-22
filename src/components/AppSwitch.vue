<template>
  <view
    class="app-switch"
    :class="{ 'app-switch-on': modelValue, 'app-switch-disabled': disabled }"
    role="switch"
    :aria-checked="modelValue ? 'true' : 'false'"
    @tap.stop="toggle"
  >
    <view class="app-switch-thumb"></view>
  </view>
</template>

<script setup lang="ts">
const props = withDefaults(defineProps<{
  modelValue: boolean
  disabled?: boolean
}>(), {
  disabled: false
})

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void
}>()

function toggle() {
  if (props.disabled) return
  emit('update:modelValue', !props.modelValue)
  uni.vibrateShort({ type: 'light', fail: () => undefined })
}
</script>

<style lang="scss" scoped>
.app-switch {
  position: relative;
  width: 88rpx;
  height: 52rpx;
  flex: none;
  box-sizing: border-box;
  padding: 4rpx;
  border-radius: 999rpx;
  background: #E9E9EA;
  transition: background-color 200ms ease, opacity 160ms ease;
}

.app-switch-on { background: #34C759; }
.app-switch-disabled { opacity: .42; }

.app-switch-thumb {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: #FFFFFF;
  box-shadow: 0 2rpx 5rpx rgba(0, 0, 0, .18), 0 1rpx 2rpx rgba(0, 0, 0, .10);
  transform: translateX(0);
  transition: width 120ms ease, transform 210ms cubic-bezier(.32, .72, .2, 1);
}

.app-switch-on .app-switch-thumb { transform: translateX(36rpx); }
.app-switch:active .app-switch-thumb { width: 48rpx; }
.app-switch-on:active .app-switch-thumb { transform: translateX(32rpx); }

@media (prefers-reduced-motion: reduce) {
  .app-switch,
  .app-switch-thumb { transition-duration: .01ms; }
}
</style>
