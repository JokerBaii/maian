<template>
  <uni-icons
    :type="name"
    :size="size"
    :color="color"
    aria-hidden="true"
  />
</template>

<script setup lang="ts">
import UniIcons from '@dcloudio/uni-ui/lib/uni-icons/uni-icons.vue'

withDefaults(defineProps<{
  name: string
  size?: number | string
  color?: string
}>(), {
  size: 20,
  color: '#56627A'
})
</script>

