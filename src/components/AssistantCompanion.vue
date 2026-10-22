<template>
  <view v-if="active" class="assistant-companion">
    <view class="assistant-dock" :class="{ 'assistant-dock-dragging': dragging, 'assistant-dock-left': leftSide }" :style="{ left: position.x + 'px', top: position.y + 'px' }">
      <view v-if="hintVisible" class="assistant-dock-hint"><button @tap="open">有事找脉脉 <text>↗</text></button><button class="assistant-hint-dismiss" aria-label="收起助手提示" @tap.stop="dismissHint">×</button></view>
      <button class="assistant-dock-button" aria-label="打开脉脉助手" :aria-expanded="expanded" @tap.stop="tap" @touchstart.stop="touchStart" @touchmove.stop.prevent="touchMove" @touchend.stop="touchEnd" @touchcancel="touchEnd" @mousedown="mouseStart">
        <assistant-mascot :size="70" :awake="hintVisible || dragging" /><text class="assistant-dock-label">{{ reminderCount ? `${reminderCount}条提醒` : '脉脉' }}</text>
      </button>
    </view>
    <view v-if="expanded" class="assistant-overlay" @keydown.esc="close">
      <view class="assistant-sheet" :style="windowStyle" role="dialog" aria-label="脉脉助手" aria-modal="false" @tap.stop><assistant-panel :context="context" :subject="subject" compact @close="close" @navigate="close" /></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { onHide, onResize, onShow } from '@dcloudio/uni-app'
import AssistantMascot from '@/components/AssistantMascot.vue'
import AssistantPanel from '@/components/AssistantPanel.vue'
import { assistantSession } from '@/composables/useAssistantSession'
import type { AssistantContext } from '@/api/agent'
import { assistantReminderNotice } from '@/composables/useAssistantReminders'
const reminderCount = assistantReminderNotice.count
import { assistantDockBounds, assistantWindowBounds, clampAssistantDock, readAssistantDock, restoreAssistantDock, snapAssistantDock, type DockPosition } from '@/utils/assistantDock'

const props = withDefaults(defineProps<{ context?: string; subject?: AssistantContext; tabPage?: boolean; hidden?: boolean }>(), { context: 'home', tabPage: false, hidden: false })
const shown = ref(true)
const signedIn = ref(false)
const active = computed(() => shown.value && signedIn.value && !props.hidden)
const expanded = ref(false)
const hintVisible = ref(false)
const dragging = ref(false)
const position = ref<DockPosition>({ x: 0, y: 0 })
const viewport = ref({ width: 390, height: 800, safeTop: 0, safeBottom: 0 })
const windowStyle = computed(() => {
  const view = viewport.value
  const window = assistantWindowBounds(view.width, view.height, position.value, props.tabPage, view.safeTop, view.safeBottom)
  return { left: window.x + 'px', top: window.y + 'px', width: window.width + 'px', height: window.height + 'px' }
})
let bounds = assistantDockBounds(390, 800, props.tabPage)
const leftSide = computed(() => position.value.x < (bounds.left + bounds.right) / 2)
let start: { x: number; y: number; origin: DockPosition } | null = null
let suppressTapUntil = 0
let hintTimer: ReturnType<typeof setTimeout> | undefined
let resumeAfterPicker = false
const DOCK_KEY = 'maian:assistant-dock:v1'
const HINT_KEY = 'maian:assistant-intro:v1'

function layout() {
  const info = uni.getSystemInfoSync()
  let width = info.windowWidth, height = info.windowHeight
  // #ifdef H5
  width = window.innerWidth; height = window.visualViewport?.height || window.innerHeight
  // #endif
  bounds = assistantDockBounds(width, height, props.tabPage, info.safeAreaInsets?.top || 0, info.safeAreaInsets?.bottom || 0)
  viewport.value = { width, height, safeTop: info.safeAreaInsets?.top || 0, safeBottom: info.safeAreaInsets?.bottom || 0 }
  position.value = restoreAssistantDock(readAssistantDock(uni.getStorageSync(DOCK_KEY)), bounds)
}
function show() {
  shown.value = true
  signedIn.value = assistantSession.syncAccount()
  void assistantReminderNotice.refresh()
  layout()
  if (resumeAfterPicker) { expanded.value = true; resumeAfterPicker = false }
  if (props.context === 'home' && !uni.getStorageSync(HINT_KEY)) {
    hintVisible.value = true
    clearTimeout(hintTimer)
    hintTimer = setTimeout(dismissHint, 6500)
  }
}
function dismissHint() { hintVisible.value = false; clearTimeout(hintTimer); uni.setStorageSync(HINT_KEY, true) }
function open() { if (!assistantSession.syncAccount()) return; dismissHint(); expanded.value = true }
function close() { expanded.value = false; uni.hideKeyboard() }
function tap() { if (Date.now() >= suppressTapUntil) expanded.value ? close() : open() }
function begin(x: number, y: number) { start = { x, y, origin: { ...position.value } }; dragging.value = false }
function move(x: number, y: number) {
  if (!start) return
  if (Math.hypot(x - start.x, y - start.y) < 8 && !dragging.value) return
  dragging.value = true; hintVisible.value = false
  if (expanded.value) close()
  position.value = clampAssistantDock({ x: start.origin.x + x - start.x, y: start.origin.y + y - start.y }, bounds)
}
function end() {
  if (dragging.value) {
    suppressTapUntil = Date.now() + 400
    const saved = snapAssistantDock(position.value, bounds)
    uni.setStorageSync(DOCK_KEY, saved)
    position.value = restoreAssistantDock(saved, bounds)
  }
  start = null; dragging.value = false
}
function touchStart(event: any) { const point = event.touches?.[0]; if (point) begin(point.clientX, point.clientY) }
function touchMove(event: any) { const point = event.touches?.[0]; if (point) move(point.clientX, point.clientY) }
function touchEnd() { end() }
// #ifdef H5
function mouseMove(event: MouseEvent) { move(event.clientX, event.clientY) }
function mouseEnd() { end(); window.removeEventListener('mousemove', mouseMove); window.removeEventListener('mouseup', mouseEnd) }
// #endif
function mouseStart(event: any) {
  // #ifdef H5
  // uni-app normalizes mouse events without the native `button` property.
  if (typeof event.button === 'number' && event.button !== 0) return
  event.preventDefault()
  begin(event.clientX, event.clientY + (uni.getSystemInfoSync().windowTop || 0))
  window.addEventListener('mousemove', mouseMove)
  window.addEventListener('mouseup', mouseEnd)
  // #endif
}
onMounted(() => {
  show()
  // #ifdef H5
  window.addEventListener('resize', layout)
  window.visualViewport?.addEventListener('resize', layout)
  // #endif
})
onShow(show)
onResize(layout)
onHide(() => { resumeAfterPicker = expanded.value && assistantSession.locationPicking.value; shown.value = false; expanded.value = false; clearTimeout(hintTimer); start = null })
onUnmounted(() => {
  clearTimeout(hintTimer)
  // #ifdef H5
  window.removeEventListener('resize', layout); window.visualViewport?.removeEventListener('resize', layout); window.removeEventListener('mousemove', mouseMove); window.removeEventListener('mouseup', mouseEnd)
  // #endif
})
</script>

<style scoped>
.assistant-dock { position: fixed; z-index: 88; width: 72px; height: 96px; transition: left .18s ease, top .18s ease; touch-action: none; }
.assistant-dock-dragging { transition: none; }
.assistant-dock-button { display: flex; flex-direction: column; align-items: center; width: 72px; height: 96px; margin: 0; padding: 0; overflow: visible; background: transparent; border: 0; line-height: 1; touch-action: none; }
.assistant-dock-button::after,.assistant-dock-hint button::after { border: 0; }.assistant-dock-button:focus-visible { outline: 2px solid #75A5D2; border-radius: 22px; }
.assistant-dock-label { position: relative; margin-top: -1px; padding: 3px 10px; border: 1px solid #E2EBF3; border-radius: 10px; background: #FFFFFFEB; color: #7289A3; font-size: 10px; line-height: 1.3; box-shadow: 0 3px 8px #34577408; }
.assistant-dock-hint { position: absolute; display: flex; align-items: center; gap: 4px; right: 77px; top: 20px; padding: 5px 4px 5px 13px; white-space: nowrap; border: 1px solid #E2EBF3; border-radius: 15px 15px 3px 15px; background: #FFFFFFF5; box-shadow: 0 5px 20px #3457740D; }
.assistant-dock-hint button { margin: 0; padding: 3px 0; border: 0; background: transparent; color: #67829E; font-size: 11px; line-height: 1.8; }.assistant-dock-hint button > text { margin-left: 5px; color: #9BB5C6; }.assistant-dock-hint .assistant-hint-dismiss { padding: 0 5px; font-size: 15px; color: #B4BFCC; }.assistant-dock-left .assistant-dock-hint { right: auto; left: 77px; border-radius: 15px 15px 15px 3px; }
.assistant-overlay { position: fixed; z-index: 1200; inset: 0; pointer-events: none; }
.assistant-sheet { position: absolute; display: flex; flex-direction: column; overflow: hidden; box-sizing: border-box; border: 1px solid #E2E5EB; border-radius: 16px; background: #FFFFFF; pointer-events: auto; box-shadow: 0 8px 32px #24374B26, 0 2px 6px #24374B0A; animation: assistant-sheet-in .16s ease-out; }
.assistant-sheet :deep(.assistant-panel) { flex: 1; height: 0; }
@keyframes assistant-sheet-in { from { opacity: 0; transform: translateY(6px) scale(.98); } to { opacity: 1; transform: none; } }
@media (prefers-reduced-motion: reduce) { .assistant-sheet { animation: none; }.assistant-dock { transition: none; } }
</style>
