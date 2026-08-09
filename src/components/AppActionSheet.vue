<template>
  <transition name="app-sheet">
    <view v-if="visible" class="app-sheet-host" @touchmove.stop.prevent>
      <view class="app-sheet-mask" @tap="close" />
      <view class="app-sheet-panel" role="dialog" aria-modal="true" @tap.stop>
        <view class="app-sheet-grabber" />
        <view class="app-sheet-group">
          <view v-if="title || message" class="app-sheet-context">
            <text v-if="title" class="app-sheet-title">{{ title }}</text>
            <text v-if="message" class="app-sheet-message">{{ message }}</text>
          </view>
          <view
            v-for="(action, index) in actions"
            :key="`${index}-${action.label}`"
            class="app-sheet-action app-motion-control"
            :class="{
              'app-sheet-action-destructive': action.destructive,
              'app-sheet-action-disabled': action.disabled
            }"
            @tap="select(index)"
          >
            <text>{{ action.label }}</text>
          </view>
        </view>
        <view class="app-sheet-group app-sheet-cancel app-motion-control" @tap="close">
          <text>{{ cancelText }}</text>
        </view>
      </view>
    </view>
  </transition>
</template>

<script setup lang="ts">
export interface AppActionSheetItem {
  label: string
  destructive?: boolean
  disabled?: boolean
}

const props = withDefaults(defineProps<{
  visible: boolean
  title?: string
  message?: string
  actions: AppActionSheetItem[]
  cancelText?: string
}>(), {
  title: '',
  message: '',
  cancelText: '取消'
})

const emit = defineEmits<{
  (event: 'update:visible', value: boolean): void
  (event: 'select', index: number): void
}>()

function close() {
  emit('update:visible', false)
}

function select(index: number) {
  if (props.actions[index]?.disabled) return
  emit('select', index)
  close()
  uni.vibrateShort({ type: 'light', fail: () => undefined })
}
</script>

<style lang="scss" scoped>
.app-sheet-host {
  position: fixed;
  inset: 0;
  z-index: 1400;
}

.app-sheet-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, .28);
}

.app-sheet-panel {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  padding: 10rpx 16rpx calc(16rpx + env(safe-area-inset-bottom));
}

.app-sheet-grabber {
  width: 68rpx;
  height: 8rpx;
  margin: 0 auto 12rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, .72);
}

.app-sheet-group {
  overflow: hidden;
  border-radius: 22rpx;
  background: rgba(250, 250, 250, .96);
  box-shadow: 0 10rpx 36rpx rgba(0, 0, 0, .12);
  backdrop-filter: saturate(180%) blur(30rpx);
  -webkit-backdrop-filter: saturate(180%) blur(30rpx);
}

.app-sheet-context {
  padding: 22rpx 28rpx 18rpx;
  text-align: center;
}

.app-sheet-title,
.app-sheet-message { display: block; }
.app-sheet-title { color: #3A3A3C; font-size: 24rpx; font-weight: 650; line-height: 1.4; }
.app-sheet-message { margin-top: 5rpx; color: #8E8E93; font-size: 21rpx; line-height: 1.45; }

.app-sheet-action,
.app-sheet-cancel {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 104rpx;
  box-sizing: border-box;
  color: #007AFF;
  font-size: 31rpx;
  font-weight: 500;
}

.app-sheet-action { position: relative; }
.app-sheet-action::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  height: 1rpx;
  background: rgba(60, 60, 67, .20);
}
.app-sheet-context + .app-sheet-action::before { display: block; }
.app-sheet-action:first-child::before { display: none; }
.app-sheet-action:active,
.app-sheet-cancel:active { background: rgba(118, 118, 128, .14); }
.app-sheet-action-destructive { color: #FF3B30; }
.app-sheet-action-disabled { color: #AEAEB2; }
.app-sheet-cancel { margin-top: 14rpx; font-weight: 650; }

.app-sheet-enter-active,
.app-sheet-leave-active { transition: opacity 180ms ease; }
.app-sheet-enter-active .app-sheet-panel,
.app-sheet-leave-active .app-sheet-panel { transition: transform 220ms cubic-bezier(.32, .72, .2, 1); }
.app-sheet-enter-from,
.app-sheet-leave-to { opacity: 0; }
.app-sheet-enter-from .app-sheet-panel,
.app-sheet-leave-to .app-sheet-panel { transform: translate3d(0, 100%, 0); }

/* #ifdef H5 */
@media (min-width: 768px) {
  .app-sheet-host { right: auto; left: 50%; width: 430px; transform: translateX(-50%); }
}
/* #endif */

@media (prefers-reduced-motion: reduce) {
  .app-sheet-enter-active,
  .app-sheet-leave-active,
  .app-sheet-enter-active .app-sheet-panel,
  .app-sheet-leave-active .app-sheet-panel { transition-duration: .01ms; }
}
</style>
