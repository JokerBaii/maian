<template>
  <view class="app-avatar" :style="avatarStyle">
    <image v-if="src" class="app-avatar-image" :src="src" mode="aspectFill" />
    <text v-else class="app-avatar-fallback">{{ fallback }}</text>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  src?: string
  name?: string
  size?: number
}>(), {
  src: '',
  name: '',
  size: 44
})

const fallback = computed(() => props.name.trim().charAt(0) || '人')
const avatarStyle = computed(() => ({ width: `${props.size}px`, height: `${props.size}px` }))
</script>

<style scoped>
.app-avatar {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 50%;
  background: rgba(118, 118, 128, .12);
}
.app-avatar-image { width: 100%; height: 100%; }
.app-avatar-fallback { color: #636366; font-size: 42%; font-weight: 650; }
</style>
