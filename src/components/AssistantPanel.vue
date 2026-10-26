<template>
  <view class="assistant-panel" :class="{ 'assistant-panel-full': fullPage, 'assistant-panel-compact': compact }">
    <view class="assistant-panel-header">
      <view class="assistant-panel-identity"><assistant-mascot :size="compact ? 30 : 40" awake :thinking="busy" /><view><text class="assistant-panel-name">脉脉</text><text class="assistant-panel-subtitle">你的全局办事助手</text></view></view>
      <view class="assistant-panel-controls">
        <button v-if="!fullPage" class="assistant-icon-button" aria-label="展开完整助手" @tap="expand"><app-icon name="scan" :size="18" color="#7A899C" /></button>
        <button class="assistant-icon-button" aria-label="清空查找记录" :disabled="busy || !entries.length" @tap="clear"><app-icon name="trash" :size="19" color="#7A899C" /></button>
        <button class="assistant-icon-button" aria-label="关闭助手" @tap="$emit('close')"><app-icon name="closeempty" :size="22" color="#56677D" /></button>
      </view>
    </view>
    <view v-if="recoveryNotice" class="assistant-recovery-notice"><text>{{ recoveryNotice }}</text></view>
    <button v-if="reminderCount" class="assistant-reminder-notice" @tap="submit('查看我的提醒')">有 {{ reminderCount }} 条站内提醒，点这里查看</button>
    <scroll-view class="assistant-conversation" scroll-y :show-scrollbar="false" :scroll-top="scrollTop" :scroll-with-animation="true">
      <view class="assistant-conversation-content">
      <view v-if="!entries.length" class="assistant-welcome">
        <view class="assistant-welcome-art"><view class="assistant-orbit assistant-orbit-one"></view><view class="assistant-orbit assistant-orbit-two"></view><assistant-mascot :size="105" awake /><text class="assistant-spark assistant-spark-one">✦</text><text class="assistant-spark assistant-spark-two">✧</text></view>
        <text class="assistant-welcome-title">嗨，今天想做点什么？</text>
        <text class="assistant-welcome-copy">一句话改设置、管设备，<text>也能上传文档，交给脉脉办理。</text></text>
        <view class="assistant-starters">
          <button v-for="suggestion in starters" :key="suggestion.message" class="assistant-starter" @tap="submit(suggestion.message)">
            <app-icon-tile :name="suggestion.icon" :tone="suggestion.tone" />
            <text class="assistant-starter-title">{{ suggestion.title }}</text>
            <text class="assistant-starter-caption">{{ suggestion.caption }}</text>
            <app-icon name="right" :size="13" color="#A7B0C0" />
          </button>
        </view>
      </view>
      <view v-for="(entry, index) in entries" :id="'assistant-entry-' + entry.id" :key="entry.id" class="assistant-exchange">
        <view class="assistant-question"><text>{{ entry.question }}</text></view>
        <view class="assistant-answer" :data-execution-mode="entry.result?.executionMode">
          <view class="assistant-answer-byline"><view class="assistant-avatar-dot"></view><text>脉脉</text></view>
          <view v-if="!entry.result && !entry.error" class="assistant-searching"><view></view><view></view><view></view><text>正在思考…</text></view>
          <template v-if="entry.result">
            <assistant-reply v-if="!entry.tasks?.length" class="assistant-answer-copy" :text="entry.result.actions.length ? '找到啦，点下面就能打开。' : entry.result.reply" />
            <assistant-task v-for="(task,taskIndex) in entry.tasks" :key="taskIndex" :task="task" :busy="busy" :planning-steps="entry.result.steps" @continue="(confirmed,file,image) => continueTask(entry,task,confirmed,file,image)" @locate="manual => locateTask(entry,task,manual)" @cancel="cancelTask(entry,task)" @open="openAction" />
            <view v-if="entry.result.actions.length" class="assistant-results">
              <button v-for="action in entry.result.actions" :key="action.url" class="assistant-result-card" :data-kind="action.kind" @tap="openAction(action)">
                <app-icon-tile :name="action.kind === 'ARTICLE' ? 'science-update' : 'settings-filled'" :tone="action.kind === 'ARTICLE' ? 'cyan' : 'blue'" />
                <view class="assistant-result-copy"><text class="assistant-result-title">{{ action.title }}</text><text v-if="action.description" class="assistant-result-description">{{ action.description }}</text></view>
                <app-icon name="right" :size="13" color="#A7B0C0" />
              </button>
            </view>
            <button v-if="entry.result.steps.length && !entry.tasks?.length" class="assistant-trace-toggle" @tap="traceEntry = traceEntry === entry.id ? null : entry.id"><app-icon name="smallcircle" :size="11" color="#95A3B5" /><text>{{ traceEntry === entry.id ? '收起过程' : '处理过程' }}</text><app-icon :name="traceEntry === entry.id ? 'up' : 'down'" :size="11" color="#95A3B5" /></button>
            <view v-if="traceEntry === entry.id" class="assistant-trace"><view v-for="(step, stepIndex) in entry.result.steps" :key="stepIndex"><text>{{ stepIndex + 1 }}</text><text>{{ step }}</text></view></view>
          </template>
          <view v-if="entry.error" class="assistant-request-error"><text>{{ entry.error }}</text><button class="assistant-retry" :disabled="busy" @tap="submit(entry.question)">再试一次</button></view>
        </view>
        <view v-if="index === entries.length - 1" :id="'assistant-end-' + entry.id" class="assistant-scroll-end"></view>
      </view>
      </view>
    </scroll-view>
    <view class="assistant-compose">
      <scroll-view v-if="entries.length" scroll-x :show-scrollbar="false" class="assistant-followups"><view class="assistant-followup-list"><button v-for="suggestion in followups" :key="suggestion" :disabled="busy" @tap="submit(suggestion)">{{ suggestion }}<text>↗</text></button></view></scroll-view>
      <view v-if="privacyVisible" class="assistant-privacy"><view class="assistant-privacy-copy"><text>使用 AI 理解指令</text><text>开启后，指令、任务上下文及你要求分析的文档文字会发送至模型；健康报告按健康数据授权处理。</text></view><app-switch v-model="useModel" :disabled="busy" /></view>
      <view v-if="speech.listening.value || speech.error.value" class="assistant-speech-status" aria-live="polite"><text>{{ speech.error.value || speech.transcript.value || '正在听，说完自动发送…' }}</text><button v-if="speech.listening.value" class="assistant-speech-cancel" @tap="speech.cancel">取消</button></view>
      <view class="assistant-compose-row"><button class="assistant-attach" aria-label="上传文档交给脉脉" :disabled="busy" @tap="attach"><app-icon name="plus" :size="22" color="#8E8E93" /></button><button v-if="speech.available" class="assistant-voice" :class="{ 'assistant-voice-active': speech.listening.value }" :aria-label="speech.listening.value ? '结束说话并发送' : '语音输入，说完发送'" :disabled="busy" @tap="toggleSpeech"><app-icon :name="speech.listening.value ? 'smallcircle-filled' : 'mic'" :size="20" :color="speech.listening.value ? '#007AFF' : '#8E8E93'" /></button><textarea v-model="draft" class="assistant-draft" placeholder="想办什么事，告诉脉脉…" :maxlength="300" :show-confirm-bar="false" confirm-type="send" @confirm="submit()" /><button class="assistant-send" aria-label="发送给脉脉" :disabled="busy || !draft.trim()" @tap="submit()"><app-icon name="arrow-up" :size="22" :color="busy || !draft.trim() ? '#91A8C7' : '#FFFFFF'" /></button></view>
      <view class="assistant-compose-footnote"><text>{{ speech.listening.value ? '由浏览器语音服务识别' : waitingHint }}</text><button class="assistant-model-choice" :class="{ 'assistant-model-on': useModel }" @tap="privacyVisible = !privacyVisible"><view></view><text>{{ useModel ? 'AI 理解已开启' : '站内指令模式' }}</text><app-icon :name="privacyVisible ? 'up' : 'down'" :size="10" color="#8C9AAF" /></button></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import AssistantMascot from '@/components/AssistantMascot.vue'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import AppSwitch from '@/components/AppSwitch.vue'
import AssistantTask from '@/components/AssistantTask.vue'
import AssistantReply from '@/components/AssistantReply.vue'
import { createAssistantSpeech } from '@/composables/useAssistantSpeech'
import { chooseAgentFile } from '@/api/agentDocuments'
import { assistantSession } from '@/composables/useAssistantSession'
import { isAssistantDestination } from '@/utils/assistantNavigation'
import type { AssistantAction, AssistantContext } from '@/api/agent'
import { readAssistantContext } from '@/utils/assistantContext'
import { assistantReminderNotice } from '@/composables/useAssistantReminders'
const reminderCount = assistantReminderNotice.count

const props = withDefaults(defineProps<{ context?: string; subject?: AssistantContext; fullPage?: boolean; compact?: boolean }>(), { context: 'home', fullPage: false, compact: false })
const emit = defineEmits<{ (event: 'close'): void; (event: 'navigate'): void }>()
const { entries, draft, useModel, busy, recoveryNotice, clear: clearSession, syncAccount, submit: submitRequest, continueTask, locateTask, attachFile, cancelTask } = assistantSession
const waitingHint = computed(() => entries.value.at(-1)?.tasks?.some(task => task.phase === 'INPUT') ? '直接说缺少的信息，我会接着办理' : entries.value.at(-1)?.tasks?.some(task => task.phase === 'CONFIRM') ? '输入“确认”或“取消”，也可点按钮' : '直接说指令，也能上传或拖入文件')
function sceneContext() {
  const current = readAssistantContext(props.subject)
  return props.fullPage && current.page === assistantSession.sceneContext.value.page ? {...assistantSession.sceneContext.value,...current} : current
}
const speech = createAssistantSpeech(text => {
  if (busy.value) { draft.value = text; return }
  if (draft.value.trim()) { draft.value = (draft.value + text).slice(0, 300); return }
  void submit(text)
})
function clear() { speech.cancel(); clearSession() }
function toggleSpeech() { if (speech.listening.value) speech.stop(); else speech.start() }
function submit(question?: string) { speech.cancel(); return submitRequest(question ?? draft.value, sceneContext()) }
watch(busy, value => { if (value) speech.cancel() })
const stopHiddenSpeech = () => { if (document.hidden) speech.cancel() }
syncAccount()
const traceEntry = ref<number | null>(null)
const privacyVisible = ref(false)
const scrollTop = ref(0)
const instance = getCurrentInstance()
let scrollFrame = 0
let nativeFileRoot: HTMLElement | undefined
const starters = computed(() => {
  const choices = [
    { title: '上传文档', caption: '选择文件后直接办理', message: '上传一个文档', icon: 'folder-add-filled', tone: 'slate' as const },
    { title: '学点急救', caption: 'AED 使用知识', message: '推荐 AED 文章', icon: 'science-update', tone: 'cyan' as const },
    { title: '管理设备', caption: '查询我的急救设备', message: '查看我的设备', icon: 'aed', tone: 'coral' as const },
    { title: '修改设置', caption: '直接调整开关与阈值', message: '修改我的设置', icon: 'settings-filled', tone: 'blue' as const }
  ]
  if (props.context === 'health') choices[0] = { ...choices[0], title: '导入报告', caption: '上传文档整理入档', message: '上传体检报告' }
  if (props.context === 'devices') return [choices[2], choices[3], choices[0], choices[1]]
  return choices
})
const followups = ['关闭科普更新', '推荐 AED 文章', '查看我的设备']
async function scrollToLatest(animate = true) {
  await nextTick()
  // #ifdef H5
  const root = instance?.proxy?.$el as HTMLElement | undefined
  const scroller = root?.querySelector('.assistant-conversation .uni-scroll-view-scrollbar-hidden') as HTMLElement | null
  const latest = entries.value[entries.value.length - 1]
  const interactive = latest?.tasks?.some(task => ['INPUT','FILE','CONFIRM','ERROR'].includes(task.phase))
  // Let auto-height fields settle before measuring the scroll extent.
  cancelAnimationFrame(scrollFrame)
  scrollFrame = requestAnimationFrame(() => {
    scrollFrame = requestAnimationFrame(() => {
      if (!scroller) return
      let top = latest ? scroller.scrollHeight : 0
      const task = root?.querySelector('.assistant-exchange:last-child .agent-task') as HTMLElement | null
      if (interactive && task && task.offsetHeight + 12 <= scroller.clientHeight) {
        top = scroller.scrollTop + task.getBoundingClientRect().top - scroller.getBoundingClientRect().top - 6
      }
      scroller.scrollTo({ top, behavior: animate && !interactive && !window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'smooth' : 'auto' })
    })
  })
  // #endif
  // #ifndef H5
  uni.createSelectorQuery().in(instance?.proxy).select('.assistant-conversation-content').boundingClientRect((rect: any) => {
    if (rect) scrollTop.value = entries.value.length ? rect.height : 0
  }).exec()
  // #endif
}
watch(() => [entries.value.length, busy.value], () => scrollToLatest())
const resizeConversation = () => { void scrollToLatest(false) }
onMounted(() => {
  if (!props.fullPage) assistantSession.sceneContext.value = sceneContext()
  void scrollToLatest(false)
  // #ifdef H5
  // uni-app's normalized view events omit DataTransfer and ClipboardData.
  nativeFileRoot = instance?.proxy?.$el as HTMLElement | undefined
  nativeFileRoot?.addEventListener('dragover', dragFile)
  nativeFileRoot?.addEventListener('drop', dropFile)
  nativeFileRoot?.addEventListener('paste', pasteFile)
  document.addEventListener('visibilitychange', stopHiddenSpeech)
  window.addEventListener('resize', resizeConversation)
  window.visualViewport?.addEventListener('resize', resizeConversation)
  // #endif
})
onUnmounted(() => {
  speech.cancel()
  // #ifdef H5
  nativeFileRoot?.removeEventListener('dragover', dragFile)
  nativeFileRoot?.removeEventListener('drop', dropFile)
  nativeFileRoot?.removeEventListener('paste', pasteFile)
  cancelAnimationFrame(scrollFrame)
  document.removeEventListener('visibilitychange', stopHiddenSpeech)
  window.removeEventListener('resize', resizeConversation)
  window.visualViewport?.removeEventListener('resize', resizeConversation)
  // #endif
})
async function receiveFile(file: import('@/api/agentDocuments').AgentFile) {
  const image = /\.(png|jpe?g)$/i.test(file.name)
  const scene = sceneContext()
  const operation = scene.page === 'pages/science/contribute' ? 'SCIENCE_SUBMIT' : scene.page?.startsWith('pages/checkup/') ? 'REPORT_IMPORT' : 'DOCUMENT_AUTO'
  await attachFile(operation, file, image, scene)
}
async function attach() {
  try {
    const file = await chooseAgentFile(false, true)
    if (!file) return
    await receiveFile(file)
  }
  catch (error) { uni.showToast({ title: error instanceof Error ? error.message : '文件选择失败', icon: 'none' }) }
}
function dragFile(event: DragEvent) {
  if (event.dataTransfer?.types.includes('Files')) event.preventDefault()
}
function dropFile(event: any) {
  // #ifdef H5
  const files = event.dataTransfer?.files
  if (!files?.length || busy.value) return
  event.preventDefault()
  if (files.length !== 1) { uni.showToast({ title: '请一次办理一个文件', icon: 'none' }); return }
  const file = files[0]
  void receiveFile({ path: URL.createObjectURL(file), name: file.name, size: file.size })
  // #endif
}
function pasteFile(event: any) {
  // #ifdef H5
  const file = event.clipboardData?.files?.[0]
  if (!file || busy.value) return
  event.preventDefault()
  void receiveFile({ path: URL.createObjectURL(file), name: file.name || '检查单.png', size: file.size })
  // #endif
}
function expand() { uni.navigateTo({ url: '/pages/assistant/index', success: () => emit('navigate') }) }
function openAction(action: AssistantAction) {
  if (!isAssistantDestination(action.url)) { uni.showToast({ title: '这个入口暂时打不开', icon: 'none' }); return }
  uni.navigateTo({ url: action.url, success: () => emit('navigate') })
}
</script>

<style scoped lang="scss">
.assistant-panel { display: flex; flex-direction: column; height: 100%; min-height: 0; overflow: hidden; color: var(--care-ink, #1C1C1E); background: var(--care-canvas, #F2F2F7); font-size: 14px; line-height: 1.6; }
.assistant-panel button { font-family: inherit; line-height: inherit; margin: 0; border: 0; }.assistant-panel button::after { border: 0; }
.assistant-panel-header { display: flex; flex-shrink: 0; align-items: center; justify-content: space-between; gap: 12px; padding: 10px 18px; border-bottom: 1px solid #E7EDF4; background: #FFFFFF; }
.assistant-panel-identity { display: flex; align-items: center; gap: 10px; }.assistant-panel-name { display: block; font-size: 20px; font-weight: 700; letter-spacing: 1px; }.assistant-panel-subtitle { display: block; margin-top: 1px; color: #99A5B4; font-size: 11px; }
.assistant-panel-controls { display: flex; gap: 5px; }.assistant-icon-button { display: flex; justify-content: center; align-items: center; width: 34px; height: 34px; padding: 0; background: transparent; }.assistant-icon-button[disabled] { opacity: .3; }
.assistant-conversation { flex: 1; height: 0; min-height: 0; }
.assistant-reminder-notice { flex: none; padding: 5px 12px; border-radius: 0; background: #FFF; color: #007AFF; text-align: left; font-size: 11px; }
.assistant-welcome { padding: 16px 22px 22px; text-align: center; }.assistant-welcome-art { position: relative; display: flex; align-items: center; justify-content: center; height: 128px; width: 200px; margin: 0 auto 5px; }.assistant-orbit { position: absolute; width: 150px; height: 95px; background: #E9F3F7; border-radius: 50%; transform: rotate(-20deg); }.assistant-orbit-two { width: 168px; height: 115px; border: 1px solid #E6EFF4; background: transparent; transform: rotate(20deg); }.assistant-spark { position: absolute; color: #8BBDB6; }.assistant-spark-one { right: 20px; top: 25px; font-size: 22px; }.assistant-spark-two { left: 17px; bottom: 28px; color: #ADC9E0; font-size: 25px; }
.assistant-welcome-title { display: block; margin-top: 5px; font-size: 21px; font-weight: 650; letter-spacing: -.5px; }.assistant-welcome-copy { display: block; margin-top: 9px; color: var(--care-tertiary, #8E8E93); font-size: 13px; }.assistant-welcome-copy > text { display: block; }
.assistant-starters { overflow: hidden; margin-top: 23px; border-radius: var(--care-group-radius, 20rpx); background: var(--care-paper, #FFFFFF); }
.assistant-starter { position: relative; display: flex; align-items: center; gap: 12px; width: 100%; min-height: 54px; box-sizing: border-box; padding: 11px 14px; border-radius: 0; background: transparent; text-align: left; color: var(--care-ink, #1C1C1E); }
.assistant-starter:not(:last-child)::before { content: ''; position: absolute; right: 0; bottom: 0; left: calc(14px + 56rpx + 12px); height: 1px; background: var(--care-separator, rgba(60, 60, 67, .16)); transform: scaleY(.5); }
.assistant-starter-title { flex-shrink: 0; font-size: 15px; font-weight: 400; }
.assistant-starter-caption { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; text-align: right; color: var(--care-tertiary, #8E8E93); font-size: 11px; }
.assistant-starter:active { background: #EDEDF2; }
.assistant-starter:focus-visible { outline: 2px solid var(--care-action, #007AFF); outline-offset: -2px; }
.assistant-exchange { padding: 22px 18px 0; }.assistant-question { display: flex; justify-content: flex-end; margin-bottom: 21px; }.assistant-question > text { max-width: 85%; padding: 11px 15px; border-radius: 17px 17px 4px 17px; background: #E7EFFC; color: #41669F; overflow-wrap: anywhere; font-size: 13px; }
.assistant-answer-byline { display: flex; align-items: center; gap: 6px; color: #8796A9; font-size: 11px; }.assistant-avatar-dot { width: 6px; height: 6px; border-radius: 50%; background: #71BAA7; }.assistant-answer-copy { display: block; margin: 10px 0 13px; font-size: 13px; line-height: 1.8; }
.assistant-results { overflow: hidden; border-radius: var(--care-group-radius, 20rpx); background: var(--care-paper, #FFFFFF); }
.assistant-result-card { position: relative; display: flex; align-items: center; gap: 12px; width: 100%; min-height: 60px; box-sizing: border-box; padding: 12px 14px; border-radius: 0; text-align: left; background: transparent; }
.assistant-result-card:not(:last-child)::before { content: ''; position: absolute; left: calc(14px + 56rpx + 12px); right: 0; bottom: 0; height: 1px; background: var(--care-separator, rgba(60, 60, 67, .16)); transform: scaleY(.5); }
.assistant-result-card:active { background: #EDEDF2; }
.assistant-result-card:focus-visible { outline: 2px solid var(--care-action, #007AFF); outline-offset: -2px; }
.assistant-result-copy { flex: 1; min-width: 0; }.assistant-result-title { display: block; font-size: 14px; font-weight: 500; line-height: 1.5; overflow-wrap: anywhere; color: var(--care-ink, #1C1C1E); }.assistant-result-description { display: -webkit-box; margin-top: 3px; overflow: hidden; -webkit-box-orient: vertical; -webkit-line-clamp: 2; font-size: 11px; line-height: 1.5; color: var(--care-secondary, #636366); }
.assistant-trace-toggle { display: flex; align-items: center; gap: 5px; margin-top: 13px !important; padding: 4px 0; color: #98A5B6; background: transparent; font-size: 10px; }.assistant-trace { padding: 5px 0 0 5px; }.assistant-trace > view { display: flex; gap: 8px; padding: 5px 0; font-size: 11px; color: #8897A9; }.assistant-trace > view > text:first-child { color: #ADC0D1; }
.assistant-searching { display: flex; align-items: center; gap: 4px; height: 42px; color: #8D9BAD; font-size: 12px; }.assistant-searching > view { width: 4px; height: 4px; border-radius: 50%; background: #8FB8CB; animation: searching-dot .9s ease-in-out infinite alternate; }.assistant-searching > view:nth-child(2) { animation-delay: .2s; }.assistant-searching > view:nth-child(3) { animation-delay: .4s; }.assistant-searching > text { margin-left: 6px; }
.assistant-request-error { margin-top: 10px; color: #A96C65; font-size: 12px; }.assistant-request-error button { padding: 8px 0; background: transparent; text-align: left; color: #537FB9; font-size: 12px; }.assistant-scroll-end { height: 22px; }
.assistant-compose { flex-shrink: 0; padding: 12px 16px max(12px,env(safe-area-inset-bottom)); border-top: 1px solid #E7EDF4; background: #FFFFFF; }.assistant-compose-row { display: flex; align-items: center; gap: 10px; padding: 10px; border-radius: var(--care-control-radius, 14rpx); background: var(--care-fill, rgba(118, 118, 128, .10)); }.assistant-draft { flex: 1; min-width: 0; width: 0; height: 20px; padding: 0; font-size: 13px; line-height: 20px; color: var(--care-ink, #1C1C1E); }.assistant-send { display: flex; justify-content: center; align-items: center; flex-shrink: 0; width: 35px; height: 35px; padding: 0; border-radius: var(--care-control-radius, 14rpx); background: var(--care-action, #007AFF); }.assistant-send[disabled] { opacity: 1; background: #E8E8ED; }
.assistant-compose-footnote { display: flex; justify-content: space-between; align-items: center; gap: 5px; margin-top: 9px; color: #ACB5C2; font-size: 9px; }.assistant-model-choice { display: flex; align-items: center; gap: 4px; padding: 0; color: #9DA9B9; background: transparent; font-size: 10px; }.assistant-model-choice > view { width: 4px; height: 4px; border-radius: 50%; background: #B9C4D2; }.assistant-model-on { color: #498F7C; }.assistant-model-on > view { background: #70B7A1; }
.assistant-voice { display: flex; align-items: center; justify-content: center; flex: none; width: 22px; height: 30px; padding: 0; background: transparent; }.assistant-voice[disabled] { opacity: .4; }.assistant-voice-active { background: #E7EFFC; border-radius: 8px; }
.assistant-speech-status,.assistant-recovery-notice { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 4px 10px; color: #636366; font-size: 10px; }.assistant-speech-status > text { flex: 1; min-width: 0; overflow-wrap: anywhere; }.assistant-speech-status button { flex: none; padding: 0; font-size: 10px; color: #007AFF; background: transparent; }.assistant-recovery-notice { flex: none; background: #FFF; }
.assistant-attach { display: flex; align-items: center; justify-content: center; flex: none; width: 28px; height: 35px; padding: 0; background: transparent; }.assistant-attach[disabled] { opacity: .4; }
.assistant-privacy { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 0 2px 13px; }.assistant-privacy-copy { flex: 1; min-width: 0; font-size: 12px; }.assistant-privacy-copy > text { display: block; }.assistant-privacy-copy > text + text { margin-top: 2px; font-size: 10px; color: #9AA7B7; }
.assistant-followups { margin-bottom: 7px; white-space: nowrap; }.assistant-followup-list { display: inline-flex; gap: 18px; }.assistant-followups button { display: inline-flex; align-items: center; gap: 5px; min-height: 24px; padding: 3px 0; border-radius: 0; background: transparent; color: #8E8E93; font-size: 11px; }.assistant-followups button > text { color: #C7C7CC; }.assistant-followups button[disabled] { opacity: .5; }
@keyframes searching-dot { to { opacity: .3; transform: translateY(-3px); } }
@media (prefers-reduced-motion: reduce) { .assistant-searching > view { animation: none; } }
@media (max-height: 650px) { .assistant-welcome-art { height: 96px; }.assistant-welcome-art :deep(.mascot) { font-size: 76px !important; }.assistant-welcome { padding-top: 5px; }.assistant-starters { margin-top: 15px; }.assistant-starter { min-height: 48px; padding-top: 8px; padding-bottom: 8px; }.assistant-welcome-copy > text { display: inline; } }
.assistant-panel-compact .assistant-panel-header { padding: 6px 10px; gap: 6px; }
.assistant-panel-compact .assistant-panel-identity { gap: 7px; }
.assistant-panel-compact .assistant-panel-name { font-size: 15px; letter-spacing: 0; }
.assistant-panel-compact .assistant-panel-subtitle { display: none; }
.assistant-panel-compact .assistant-panel-controls { gap: 0; }
.assistant-panel-compact .assistant-icon-button { width: 30px; height: 32px; }
.assistant-panel-compact .assistant-welcome { padding: 12px; }
.assistant-panel-compact .assistant-welcome-art { display: none; }
.assistant-panel-compact .assistant-welcome-title { margin-top: 0; font-size: 16px; }
.assistant-panel-compact .assistant-welcome-copy { margin-top: 4px; font-size: 11px; }
.assistant-panel-compact .assistant-welcome-copy > text { display: inline; }
.assistant-panel-compact .assistant-starters { margin-top: 12px; }
.assistant-panel-compact .assistant-starter { min-height: 42px; padding: 6px 10px; gap: 9px; }
.assistant-panel-compact .assistant-starter-title { font-size: 13px; }
.assistant-panel-compact .assistant-starter-caption { font-size: 10px; }
.assistant-panel-compact .assistant-exchange { padding: 12px 12px 0; }
.assistant-panel-compact .assistant-question { margin-bottom: 10px; }
.assistant-panel-compact .assistant-question > text { padding: 7px 10px; font-size: 12px; }
.assistant-panel-compact .assistant-followups { display: none; }
.assistant-panel-compact .assistant-compose { padding: 8px 10px max(8px,env(safe-area-inset-bottom)); }
.assistant-panel-compact .assistant-compose-row { padding: 5px 7px; gap: 7px; }
.assistant-panel-compact .assistant-draft { font-size: 12px; }
.assistant-panel-compact .assistant-send { width: 30px; height: 30px; }
.assistant-panel-compact .assistant-attach { width: 22px; height: 30px; }
.assistant-panel-compact .assistant-compose-footnote { margin-top: 5px; font-size: 8px; }
.assistant-panel-compact .assistant-model-choice { font-size: 9px; }
.assistant-panel-compact .assistant-scroll-end { height: 6px; }
.assistant-panel-compact :deep(.agent-task) { margin-top: 8px; }
.assistant-panel-compact :deep(.agent-task-heading) { font-size: 13px; }
.assistant-panel-compact :deep(.agent-task-fields) { margin-top: 8px; }
.assistant-panel-compact :deep(.agent-task-field) { min-height: 40px; }
.assistant-panel-compact :deep(.agent-task-field input),.assistant-panel-compact :deep(.agent-task-select) { height: 40px; min-height: 40px; }
.assistant-panel-compact :deep(.agent-task-actions) { margin-top: 8px; }
.assistant-panel-compact :deep(.agent-task-primary),.assistant-panel-compact :deep(.agent-task-cancel) { min-height: 34px; }
.assistant-panel-compact :deep(.agent-task-trace-toggle) { min-height: 24px; }
</style>
