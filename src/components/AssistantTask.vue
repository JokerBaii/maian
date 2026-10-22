<template>
  <view class="agent-task" :data-operation="task.operation" :data-phase="task.phase">
    <view class="agent-task-heading"><text>{{ title }}</text><text class="agent-task-status" :class="{ 'agent-task-success': task.phase === 'DONE' }">{{ status }}</text></view>
    <text v-if="task.fileName" class="agent-task-file">{{ task.fileName }}</text>
    <text v-if="task.summary && !['INPUT','FILE','ERROR','UNCERTAIN','CANCELED'].includes(task.phase) && !(task.rescue && task.phase === 'CONFIRM')" class="agent-task-summary">{{ task.summary }}</text>
    <text v-if="task.error" class="agent-task-error">{{ task.error }}</text>
    <view v-if="task.locationRequired && !['DONE','CANCELED','UNCERTAIN'].includes(task.phase)" class="agent-rescue-location">
      <view class="agent-rescue-location-heading"><app-icon name="location-filled" :size="16" color="#007AFF" /><text>{{ task.location ? '已获取设备位置' : '获取设备位置' }}</text></view>
      <text v-if="task.location" class="agent-rescue-address">{{ task.parameters.address }}</text>
      <view class="agent-rescue-location-actions"><button :disabled="busy" @tap="$emit('locate', false)">重新定位</button><button :disabled="busy" @tap="$emit('locate', true)">地图选点</button></view>
    </view>
    <view v-if="task.rescue && !['DONE','CANCELED','UNCERTAIN'].includes(task.phase)" class="agent-rescue-location">
      <view class="agent-rescue-location-heading"><app-icon name="location-filled" :size="16" color="#007AFF" /><text>{{ task.rescue.location ? '已获取现场位置' : '定位现场' }}</text></view>
      <text class="agent-rescue-address">{{ task.rescue.locationError || (task.rescue.location ? task.parameters.address : '正在获取当前位置…') }}</text>
      <view class="agent-rescue-location-actions"><button :disabled="busy" @tap="$emit('locate', false)">重新定位</button><button :disabled="busy" @tap="$emit('locate', true)">地图选点</button><button @tap="callEmergency">拨打120</button></view>
      <text v-if="task.rescue.resourceError" class="agent-task-note agent-rescue-resource-note">{{ task.rescue.resourceError }}</text>
    </view>
    <view v-if="task.phase === 'FILE' || (task.phase === 'ERROR' && ['DOCUMENT_UPLOAD','DOCUMENT_AUTO','REPORT_IMPORT','SCIENCE_SUBMIT'].includes(task.operation))" class="agent-task-file-actions">
      <button class="agent-task-file-button" :disabled="busy || choosing" @tap="choose(false)">{{ task.document ? '重新选择文档' : '选择文档' }}</button>
      <button v-if="task.operation === 'REPORT_IMPORT'" class="agent-task-file-button" :disabled="busy || choosing" @tap="choose(true)">选择检查单图片</button>
      <text class="agent-task-note">{{ task.operation === 'REPORT_IMPORT' ? '支持文档或 JPG、PNG 检查单；图片识别需已开通 OCR。' : '支持 PDF、DOCX、TXT、MD、CSV，最大15MB。' }}</text>
    </view>
    <view v-if="task.operation !== 'RESCUE_CREATE' && ['INPUT','FILE','CONFIRM','ERROR'].includes(task.phase) && visibleFields.length" class="agent-task-fields">
      <view v-for="field in visibleFields" :key="field.key" class="agent-task-field" :class="{ 'agent-task-field-multiline': field.type === 'textarea' }">
        <text class="agent-task-label">{{ fieldLabel(field) }}<text v-if="field.optional" class="agent-task-note">（选填）</text></text>
        <picker v-if="optionsFor(field)" :range="optionsFor(field)" range-key="label" :disabled="busy" @change="setOption(field, $event)"><view class="agent-task-select"><text :class="{ 'agent-task-placeholder': !task.parameters[field.key] }">{{ optionsFor(field)?.find(o => o.value === task.parameters[field.key])?.label || '请选择' }}</text><app-icon name="right" :size="12" color="#C7C7CC" /></view></picker>
        <textarea v-else-if="field.type === 'textarea'" v-model="task.parameters[field.key]" :disabled="busy" :maxlength="field.key === 'content' ? 200000 : 4000" auto-height :placeholder="placeholder(field)" placeholder-class="agent-task-placeholder" :aria-label="fieldLabel(field)" :data-field="field.key" />
        <input v-else v-model="task.parameters[field.key]" :type="field.key === 'phone' ? 'number' : field.type === 'number' ? 'digit' : 'text'" :disabled="busy" :maxlength="field.key === 'phone' ? 11 : 200" :placeholder="placeholder(field)" placeholder-class="agent-task-placeholder" :aria-label="fieldLabel(field)" :data-field="field.key" />
      </view>
    </view>
    <text v-if="task.rescue && task.items.length" class="agent-rescue-resource-note agent-task-note">附近可用AED · 预览未占用，取送时间为估算</text>
    <view v-if="task.items.length" class="agent-task-results"><view v-for="(item,index) in task.items" :key="index" class="agent-task-item"><view><text>{{ item.title }}</text><text>{{ item.detail }}</text></view><button v-if="item.documentId" class="agent-task-download" @tap="download(item.documentId)">下载</button><button v-if="item.navigation" class="agent-task-navigation" @tap="openMapNavigation(item.navigation)">导航</button></view></view>
    <view class="agent-task-actions">
      <button v-for="link in task.links" :key="link.url" class="agent-task-link" @tap="$emit('open',link)">{{ link.title }} ↗</button>
      <button v-if="['INPUT','FILE','CONFIRM','ERROR','QUEUED'].includes(task.phase)" class="agent-task-cancel" :disabled="busy" @tap="$emit('cancel')">取消</button>
      <button v-if="(!task.rescue || task.rescue.location) && (['INPUT','CONFIRM','ERROR'].includes(task.phase) || (task.phase === 'QUEUED' && task.restored) || (task.phase === 'FILE' && task.fields.length))" class="agent-task-primary" :class="{ 'agent-task-destructive': destructive }" :disabled="busy || (task.phase !== 'ERROR' && !fieldsComplete)" @tap="$emit('continue', task.phase === 'CONFIRM')">{{ actionLabel }}</button>
    </view>
    <button v-if="executionSteps.length" class="agent-task-trace-toggle" @tap="showSteps = !showSteps"><text>{{ showSteps ? '收起过程' : '执行过程' }}</text><app-icon :name="showSteps ? 'up' : 'down'" :size="10" color="#8E8E93" /></button>
    <view v-if="showSteps" class="agent-task-steps"><text v-for="(step,index) in executionSteps" :key="index">{{ index + 1 }}. {{ step }}</text></view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { TaskField, TaskRun } from '@/utils/assistantTasks'
import { chooseAgentFile, type AgentFile } from '@/api/agentDocuments'
import { issueMediaDownload } from '@/api/files'
import { resolveApiUrl } from '@/api/http'
import type { AssistantAction } from '@/api/agent'
import AppIcon from '@/components/AppIcon.vue'
import { openMapNavigation } from '@/utils/location'
const props = defineProps<{ task: TaskRun; busy: boolean; planningSteps?: string[] }>()
const emit = defineEmits<{ (event: 'continue', confirmed: boolean, file?: AgentFile, image?: boolean): void; (event: 'locate', manual: boolean): void; (event: 'cancel'): void; (event: 'open', action: AssistantAction): void }>()
const choosing = ref(false), showSteps = ref(false)
const title = computed(() => props.task.operation === 'CONTACT_SAVE' ? '添加联系人' : props.task.title)
const visibleFields = computed(() => props.task.fields.filter(field => !field.autoResolved))
const fieldsComplete = computed(() => props.task.fields.every(field => field.optional || !!props.task.parameters[field.key]?.trim()))
const missingCount = computed(() => props.task.fields.filter(field => !field.optional && !props.task.parameters[field.key]?.trim()).length)
const status = computed(() => props.task.rescue && props.task.phase === 'INPUT' ? '待定位' : props.task.phase === 'INPUT' && missingCount.value ? `还需 ${missingCount.value} 项` : ({ QUEUED: '等待前项', RUNNING: '执行中', INPUT: '待保存', FILE: '待选文件', CONFIRM: '待确认', DONE: '已完成', ERROR: '未完成', UNCERTAIN: '待核实', CANCELED: '已取消' })[props.task.phase])
function callEmergency() { uni.makePhoneCall({ phoneNumber: '120' }) }
const destructive = computed(() => props.task.phase === 'CONFIRM' && (props.task.operation.endsWith('_DELETE') || ['RESCUE_CREATE','RESCUE_CANCEL'].includes(props.task.operation)))
const actionLabel = computed(() => {
  if (props.task.phase === 'ERROR') return '重试'
  if (props.task.phase === 'CONFIRM') return props.task.operation.endsWith('_DELETE') ? '确认删除' : props.task.operation === 'REPORT_IMPORT' ? '保存报告' : props.task.operation === 'RESCUE_CREATE' ? '确认呼救' : '确认执行'
  return ({ CONTACT_SAVE: '保存联系人', SETTINGS_UPDATE: '保存设置', SCIENCE_SUBMIT: '提交审核', DEVICE_CREATE: '登记设备', REPORT_IMPORT: '核对报告' } as Record<string,string>)[props.task.operation] || '继续'
})
const executionSteps = computed(() => [...(props.planningSteps || []), ...props.task.steps])
const fieldLabel = (field: TaskField) => ({ checkupDate: '检查日期', indicators: '检查指标', rating: '评分', value: '设置为', id: '操作对象' } as Record<string,string>)[field.key] || field.label
const placeholder = (field: TaskField) => field.key === 'name' ? props.task.operation.startsWith('DEVICE_') ? '请输入设备名称' : '请输入姓名' : ({ dueAt:'例如明天上午8点', serviceTime:'全天或08:00–18:00', phone: '请输入手机号', relation: '家人、朋友等', hospital: '请输入检查机构', checkupDate: 'YYYY-MM-DD', indicators: '每行填写：名称 数值 单位', content: '输入或上传稿件正文', title: props.task.operation === 'REMINDER_CREATE' ? '请输入提醒内容' : '请输入标题', address: '请输入详细地址', rating: '1–5', comment: '写下你的评价', reviewNote: '填写审核说明', value: '请输入新值' } as Record<string,string>)[field.key] || '请输入'
function optionsFor(field: TaskField) {
  if (field.options) return field.options
  if (field.key === 'value' && ['rescuePush','healthAlert','scienceUpdate','locationShare','healthDataShare'].includes(props.task.parameters.key)) return [{ value: 'true', label: '开启' }, { value: 'false', label: '关闭' }]
}
function setOption(field: TaskField, event: any) {
  props.task.parameters[field.key] = optionsFor(field)?.[Number(event.detail.value)]?.value || ''
  if (field.key === 'key') props.task.parameters.value = ''
}
async function choose(image: boolean) {
  if (choosing.value || props.busy) return
  choosing.value = true
  try { const file = await chooseAgentFile(image, props.task.operation === 'DOCUMENT_AUTO'); if (file) emit('continue', false, file, image || /\.(png|jpe?g)$/i.test(file.name)) }
  catch (error) { uni.showToast({ title: error instanceof Error ? error.message : '文件选择失败', icon: 'none' }) }
  finally { choosing.value = false }
}
async function download(id: string) {
  try {
    const download = await issueMediaDownload(id)
    // #ifdef H5
    const anchor = document.createElement('a'); anchor.href = resolveApiUrl(download.url); anchor.download = ''; anchor.click()
    // #endif
    // #ifndef H5
    uni.downloadFile({ url: resolveApiUrl(download.url), success: result => uni.openDocument({ filePath: result.tempFilePath, showMenu: true }) })
    // #endif
  } catch { uni.showToast({ title: '文件暂时无法下载', icon: 'none' }) }
}
</script>

<style scoped>
.agent-task { margin-top: 12px; color: var(--care-ink,#1C1C1E); }
.agent-task-heading { display: flex; justify-content: space-between; gap: 12px; align-items: baseline; margin: 0 2px; font-size: 14px; font-weight: 600; }
.agent-task-status { flex: none; color: #8E8E93; font-size: 10px; font-weight: 400; }.agent-task-success { color: #248A5A; }
.agent-task-summary,.agent-task-file,.agent-task-error { display: block; margin: 8px 2px 0; font-size: 12px; line-height: 1.7; overflow-wrap: anywhere; }.agent-task-file { color: #636366; font-size: 11px; }.agent-task-error { color: #B54A45; }
.agent-task-fields { margin-top: 12px; overflow: hidden; border-radius: var(--care-group-radius,20rpx); background: var(--care-paper,#FFF); }
.agent-task-field { position: relative; display: flex; align-items: center; gap: 12px; min-height: 46px; box-sizing: border-box; padding: 0 14px; }
.agent-task-field:not(:last-child)::after { content: ''; position: absolute; right: 0; bottom: 0; left: 14px; height: 1px; background: var(--care-separator,rgba(60,60,67,.16)); transform: scaleY(.5); }
.agent-task-field:focus-within { background: #FAFAFC; }
.agent-task-label { flex: 0 0 64px; color: #636366; font-size: 12px; line-height: 1.5; }
.agent-task-field input { flex: 1; min-width: 0; width: 0; height: 46px; padding: 0; border: 0; border-radius: 0; background: transparent; color: #1C1C1E; text-align: right; font-size: 13px; }
.agent-task-field picker { flex: 1; min-width: 0; }
.agent-task-select { display: flex; align-items: center; justify-content: flex-end; gap: 8px; min-height: 46px; font-size: 13px; }
.agent-task-select > text { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.agent-task-placeholder { color: #AEAEB2; font-size: 12px; }
.agent-task-field-multiline { display: block; padding: 12px 14px; }
.agent-task-field-multiline > .agent-task-label { display: block; margin-bottom: 8px; }
.agent-task-field textarea { width: 100%; min-height: 72px; max-height: 160px; box-sizing: border-box; padding: 0; border: 0; background: transparent; color: #1C1C1E; font-size: 13px; line-height: 1.7; }
.agent-task-actions,.agent-task-file-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; margin-top: 10px; }.agent-task-actions { justify-content: flex-end; }.agent-task-actions:empty { display: none; }
.agent-task button { margin: 0; padding: 0; border: 0; border-radius: 0; background: transparent; font-family: inherit; font-size: 12px; line-height: 1.6; }.agent-task button::after { border: 0; }
.agent-task .agent-task-primary { display: flex; align-items: center; justify-content: center; min-height: 38px; padding: 0 18px; border-radius: 8px; background: var(--care-action,#007AFF); color: #FFF; font-weight: 500; }
.agent-task .agent-task-primary[disabled] { background: #E5E5EA; color: #AEAEB2; }
.agent-task .agent-task-destructive { background: #FF3B30; }
.agent-task .agent-task-cancel { min-height: 38px; padding: 0 6px; color: #8E8E93; }
.agent-task .agent-task-file-button { min-height: 38px; padding: 8px 12px; box-sizing: border-box; border-radius: 8px; color: #007AFF; background: #FFF; }
.agent-task button[disabled]:not(.agent-task-primary) { opacity: .45; }
.agent-task .agent-task-link { margin-right: auto; color: #007AFF; }.agent-task-download { color: #007AFF; }.agent-task-note { color: #8E8E93; font-size: 10px; }.agent-task-file-actions > .agent-task-note { flex-basis: 100%; }
.agent-task .agent-task-trace-toggle { display: inline-flex; align-items: center; gap: 5px; min-height: 28px; margin-top: 5px; color: #8E8E93; font-size: 10px; }.agent-task-steps > text { display: block; margin-top: 6px; color: #8E8E93; font-size: 10px; }
.agent-rescue-location { margin-top: 10px; padding: 10px 12px; border-radius: 10px; background: #FFF; }
.agent-rescue-location-heading { display: flex; align-items: center; gap: 5px; font-size: 12px; font-weight: 500; }
.agent-rescue-address { display: block; margin-top: 4px; color: #636366; font-size: 11px; overflow-wrap: anywhere; }
.agent-rescue-location-actions { display: flex; gap: 16px; margin-top: 6px; }
.agent-rescue-location-actions button,.agent-task-navigation { color: #007AFF; font-size: 11px; }
.agent-rescue-resource-note { display: block; margin-top: 8px; }
.agent-task-results { margin-top: 10px; padding: 0 14px; border-radius: var(--care-group-radius,20rpx); background: #FFF; }
.agent-task-item { display: flex; align-items: center; gap: 10px; padding: 10px 0; border-bottom: 1px solid #F2F2F7; }.agent-task-item:last-child { border: 0; }.agent-task-item > view { flex: 1; min-width: 0; }.agent-task-item text { display: block; font-size: 12px; overflow-wrap: anywhere; }.agent-task-item text + text { color: #8E8E93; font-size: 10px; margin-top: 3px; }
</style>
