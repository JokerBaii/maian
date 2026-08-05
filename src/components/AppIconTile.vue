<template>
  <view
    class="app-icon-tile"
    :class="[`app-icon-tile--${tone}`, `app-icon-tile--${size}`]"
    aria-hidden="true"
  >
    <image class="app-icon-tile__glyph" :src="glyphSource" mode="aspectFit" />
    <view
      v-if="status"
      class="app-icon-tile__status"
      :class="`app-icon-tile__status--${status}`"
    ></view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

type IconTone = 'blue' | 'violet' | 'cyan' | 'green' | 'amber' | 'coral' | 'slate'
type IconStatus = 'online' | 'offline'
type IconSize = 'small' | 'standard' | 'large'

const glyphFiles: Record<string, string> = {
  'map-filled': 'map',
  'plus-filled': 'plus',
  'folder-add-filled': 'archive',
  'videocam-filled': 'video',
  'heart-filled': 'heart',
  wearable: 'wearable',
  'cloud-filled': 'sleep',
  'fire-filled': 'exercise',
  list: 'report',
  'notification-filled': 'alert',
  'rescue-notice': 'rescue-notice',
  'health-alert': 'health-alert',
  'science-update': 'science-update',
  'phone-filled': 'phone',
  'auth-filled': 'id',
  'settings-filled': 'settings',
  'location-filled': 'location',
  bars: 'data',
  'arrow-up': 'limit-up',
  'arrow-down': 'limit-down',
  'trash-filled': 'trash',
  'info-filled': 'info',
  hospital: 'hospital',
  check: 'check',
  'fixed-device': 'fixed-device',
  'mobile-device': 'mobile-device',
  aed: 'aed',
  'rescue-kit': 'rescue-kit',
  camera: 'camera',
  volunteer: 'volunteer',
  author: 'author',
  'user-filled': 'author'
}

const props = withDefaults(defineProps<{
  name: string
  tone?: IconTone
  status?: IconStatus
  size?: IconSize
}>(), {
  tone: 'blue',
  status: undefined,
  size: 'standard'
})

const glyphSource = computed(() =>
  `/static/icons/entry/${glyphFiles[props.name] || 'info'}.png`
)
</script>

<style lang="scss" scoped>
.app-icon-tile {
  position: relative;
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  border-radius: 50%;
}

.app-icon-tile--small {
  width: 34rpx;
  height: 34rpx;
}

.app-icon-tile--standard {
  width: 56rpx;
  height: 56rpx;
}

.app-icon-tile--large {
  width: 88rpx;
  height: 88rpx;
}

.app-icon-tile--blue { background: #2D8FEA; }
.app-icon-tile--violet { background: #7167E8; }
.app-icon-tile--cyan { background: #10B7AA; }
.app-icon-tile--green { background: #25B96B; }
.app-icon-tile--amber { background: #F2AE00; }
.app-icon-tile--coral { background: #F04452; }
.app-icon-tile--slate { background: #65758D; }

.app-icon-tile__glyph {
  width: 34rpx;
  height: 34rpx;
}

.app-icon-tile--small .app-icon-tile__glyph {
  width: 20rpx;
  height: 20rpx;
}

.app-icon-tile--large .app-icon-tile__glyph {
  width: 52rpx;
  height: 52rpx;
}

.app-icon-tile__status {
  position: absolute;
  right: -2rpx;
  bottom: -2rpx;
  width: 10rpx;
  height: 10rpx;
  border: 4rpx solid #F9FCFF;
  border-radius: 50%;
}

.app-icon-tile__status--online { background: #23956A; }
.app-icon-tile__status--offline { background: #A6B1BC; }
</style>
