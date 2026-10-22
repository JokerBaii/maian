<template>
  <rich-text class="assistant-reply" :nodes="formatted" selectable />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { assistantReplyNodes } from '@/utils/assistantReply'
const props = defineProps<{ text: string }>()
const formatted = computed(() => assistantReplyNodes(props.text))
</script>

<style scoped>
.assistant-reply { display: block; min-width: 0; max-width: 100%; font-size: inherit; line-height: inherit; overflow-wrap: anywhere; user-select: text; }
</style>
