import { reactive, ref, watch } from 'vue'
import { askAssistant, type AssistantResult, type AssistantContext } from '@/api/agent'
import { ordinalIndex } from '@/utils/assistantContext'
import { getAccountAccessToken } from '@/utils/authSession'
import { userFacingError } from '@/utils/presentation'
import { createAssistantTaskRuntime, type TaskRun } from '@/utils/assistantTasks'
import type { AgentFile } from '@/api/agentDocuments'
import { assistantPersistence, type AssistantPersistence } from '@/utils/assistantPersistence'
import { TASK_DEFINITIONS } from '@/utils/assistant/catalog'

export interface AssistantEntry { id: number; question: string; result?: AssistantResult; error?: string; tasks?: TaskRun[] }

export function createAssistantSession(dependencies = { ask: askAssistant, identity: getAccountAccessToken }, runtime = createAssistantTaskRuntime(), persistence?: AssistantPersistence) {
  const entries = ref<AssistantEntry[]>([])
  const draft = ref('')
  const useModel = ref(true)
  const busy = ref(false)
  const locationPicking = ref(false)
  const sceneContext = ref<AssistantContext>({})
  const recoveryNotice = ref('')
  let account = ''
  let generation = 0
  let sequence = 0
  let hydrating = false

  function checkpoint() {
    if (hydrating || !account || dependencies.identity() !== account) return
    persistence?.save(account, {
      entries: entries.value.map(entry => ({ ...entry, tasks: entry.tasks?.map(task => ({ ...task, target: undefined, steps: task.steps.slice(-40) })) })),
      draft: draft.value, useModel: useModel.value, scene: sceneContext.value
    })
  }
  // Save edits and the pre-request mutation marker synchronously, including before page unload.
  if (persistence) watch([entries, draft, useModel, sceneContext], checkpoint, { deep: true, flush: 'sync' })

  function clear() {
    hydrating = true
    for (const entry of entries.value) for (const task of entry.tasks || []) runtime.cancel(task)
    generation++; entries.value = []; draft.value = ''; busy.value = false; locationPicking.value = false
    recoveryNotice.value = ''; persistence?.clear(); hydrating = false
  }
  function syncAccount() {
    const current = dependencies.identity()
    if (current !== account) {
      const saved = !account && current ? persistence?.load(current) : undefined
      clear(); hydrating = true; sceneContext.value = {}; account = current
      try {
        if (saved) {
          entries.value = saved.entries.slice(-8).map(entry => ({ ...entry, tasks: entry.tasks?.map(task => runtime.restore(task)), ...(!entry.result && !entry.error ? { error: '上次请求已中断，可以重新发送。' } : {}) }))
          sequence = Math.max(sequence, ...entries.value.map(entry => entry.id))
          draft.value = saved.draft || ''; useModel.value = saved.useModel !== false; sceneContext.value = saved.scene || {}
          if (entries.value.length || draft.value) recoveryNotice.value = '已恢复上次进度，可以接着说'
        }
      } catch { entries.value = []; draft.value = ''; persistence?.clear() }
      finally { hydrating = false; checkpoint() }
    }
    return !!current
  }
  function pendingTask(resumePrevious = false) {
    const latest = entries.value.at(-1)
    for (const entry of resumePrevious ? [...entries.value].reverse() : latest ? [latest] : []) {
      const task = entry.tasks?.find(task => task.phase !== 'DONE')
      if (task) return ['INPUT','FILE','CONFIRM','ERROR','QUEUED'].includes(task.phase) ? { entry, task } : undefined
    }
  }
  function conversationContext(scene: AssistantContext, text: string) {
    const documentTask = [...entries.value].reverse().flatMap(entry => entry.tasks || []).find(task => (task.document || task.context?.documentId) && task.phase !== 'CANCELED')
    const documentId = documentTask?.document?.id || documentTask?.context?.documentId
    const context: AssistantContext = { ...(documentId ? { documentId } : {}), ...scene }
    const ordinal = ordinalIndex(text)
    if (ordinal === undefined && Object.entries(scene).some(([key,value]) => key !== 'page' && !!value)) return context
    if (ordinal !== undefined || /这|刚才|它|再打开|再关闭/.test(text)) {
      for (const entry of [...entries.value].reverse()) {
        const task = entry.tasks?.at(-1)
        if (!task) continue
        const reference = ordinal !== undefined ? task.items[ordinal]?.reference : task.items.length === 1 ? task.items[0].reference : task.context
        if (reference) {
          if (ordinal !== undefined) return { ...(scene.page ? {page:scene.page} : {}), ...reference }
          for (const [key,value] of Object.entries(reference)) if (key !== 'page' && !(context as any)[key]) (context as any)[key] = value
          break
        }
      }
    }
    return context
  }
  function movePending(target: { entry: AssistantEntry; task: TaskRun }, entry: AssistantEntry) {
    const index = target.entry.tasks!.indexOf(target.task)
    if (target.task.phase === 'QUEUED') inheritCompleted(target.entry, target.task)
    entry.tasks = target.entry.tasks!.splice(index)
  }
  async function submit(question = draft.value, scene: AssistantContext = {}) {
    if (!syncAccount() || busy.value || !question.trim()) return
    recoveryNotice.value = ''
    const text = question.trim().slice(0, 300)
    const run = ++generation
    const allowModel = useModel.value
    const pending = pendingTask(/^继续刚才(?:的任务)?[。！!\s]*$/.test(text))
    const context = conversationContext(scene, text)
    draft.value = ''; busy.value = true
    const entry = reactive<AssistantEntry>({ id: ++sequence, question: text })
    entries.value = [...entries.value.slice(-7), entry]
    try {
      const ordinal = ordinalIndex(text)
      if (ordinal !== undefined) {
        const options = pending?.task.fields.find(field => field.key === 'id')?.options
        const records = [...entries.value].reverse().flatMap(item => item === entry ? [] : item.tasks || []).find(task => task.items.some(item => item.reference))?.items
        const choices = options || records
        if (choices && ordinal >= choices.length) throw new Error(`当前列表没有第${ordinal + 1}项，请重新选择`)
      }
      if (pending && /^(取消|取消任务|取消当前任务|算了|不办了|停止办理)[。！!\s]*$/.test(text)) {
        cancelTask(pending.entry, pending.task, true)
        entry.result = { reply: '已取消当前任务。', executionMode: 'DIRECT', actions: [], steps: ['已停止当前尚未完成的操作'] }
        movePending(pending, entry); return
      }
      if (pending && /^(确认|确认执行|确认呼救|确认删除|保存|继续|继续刚才|继续刚才的任务|确定|可以|好)[。！!\s]*$/.test(text)) {
        entry.result = { reply: '继续办理当前任务。', executionMode: 'DIRECT', actions: [], steps: ['已收到你对当前任务的明确指令'] }
        movePending(pending, entry)
        await runtime.prepare(pending.task, pending.task.phase === 'CONFIRM')
        await advance(entry); return
      }
      if (pending) {
        const field = pending.task.fields.find(f => f.key === 'id')
        const index = ordinalIndex(text)
        const selected = index !== undefined && /^第.+(?:个|台|篇|份|条|项)?[。！!\s]*$/.test(text) ? field?.options?.[index] : field?.options?.find(option => option.label === text)
        if (selected) {
          entry.result = { reply: '已选中，继续办理。', executionMode: 'DIRECT', actions: [], steps: ['按真实列表选择了操作对象'] }
          runtime.patch(pending.task, { id: selected.value }); movePending(pending, entry)
          await runtime.prepare(pending.task); await advance(entry); return
        }
      }
      const continuation = pending ? { operation: pending.task.operation, editableKeys: pending.task.fields.map(f => f.key).filter(key => key !== 'id' && !['latitude','longitude','urgency'].includes(key)), missingKeys: pending.task.fields.filter(f => !f.optional && !pending.task.parameters[f.key]?.trim()).map(f => f.key) } : undefined
      const history = entries.value.filter(item => item !== entry && item.result?.conversation && !item.tasks?.length && !item.result.actions.length && !item.error).slice(-3)
        .map(item => ({ question: item.question.slice(0,300), reply: item.result!.reply.slice(0,1000) }))
      const result = history.length && allowModel
        ? await dependencies.ask(text, allowModel, Object.keys(context).length ? context : undefined, continuation, history)
        : await dependencies.ask(text, allowModel, Object.keys(context).length ? context : undefined, continuation)
      if (dependencies.identity() !== account) { syncAccount(); return }
      if (generation === run) {
        entry.result = result
        if (result.continuation) {
          if (!pending || result.tasks?.length !== 1 || result.tasks[0].operation !== pending.task.operation) throw new Error('补充信息未能对应当前任务，请重新说明')
          runtime.patch(pending.task, result.tasks[0].parameters); movePending(pending, entry)
        } else {
          entry.tasks = (result.tasks || []).map(task => {
            if (task.operation === 'RESCUE_CREATE') task = { ...task, parameters: { ...task.parameters, description: task.parameters.description || text } }
            if (['DOCUMENT_AUTO','DOCUMENT_ANALYZE'].includes(task.operation)) task = { ...task, parameters: { ...task.parameters, instruction: text } }
            return runtime.create(task, context)
          })
          const emergency = entry.tasks.find(task => task.operation === 'RESCUE_CREATE')
          if (emergency) entry.tasks = [emergency, ...entry.tasks.filter(task => task !== emergency)]
          if (/刚才|刚上传|这份|这个文件|上个文件|文档|文件/.test(text) && !/另一|重新上传/.test(text)) {
            const previous = [...entries.value].reverse().flatMap(item => item === entry ? [] : item.tasks || []).find(task => task.document && task.phase !== 'CANCELED')
            if (previous) for (const task of entry.tasks) runtime.reuseDocument(previous,task)
          }
        }
        if (result.continuation) { entry.tasks![0].useModel = useModel.value; await runtime.prepare(entry.tasks![0]) }
        await advance(entry)
      }
    } catch (error) {
      if (dependencies.identity() !== account) { syncAccount(); return }
      if (generation === run) entry.error = userFacingError(error, '暂时没找到，稍后再试试')
    } finally { if (generation === run) busy.value = false }
  }
  function inheritCompleted(entry: AssistantEntry, task: TaskRun) {
    let output: AssistantContext | undefined, document: TaskRun | undefined
    for (const previous of entry.tasks?.slice(0, entry.tasks.indexOf(task)) || []) {
      if (previous.phase !== 'DONE') break
      if (previous.output) output = previous.output
      if (previous.document) document = previous
    }
    if (output && !task.parameters.id && (!task.parameters.name || /^(它|这个|这台|刚才的)$/.test(task.parameters.name))) task.context = { ...task.context, ...output }
    if (document && !task.document) runtime.reuseDocument(document, task)
  }
  async function advance(entry: AssistantEntry) {
    for (const task of entry.tasks || []) {
      if (task.phase === 'DONE') continue
      if (task.phase === 'QUEUED') {
        inheritCompleted(entry, task)
        task.useModel = useModel.value
        await runtime.prepare(task)
      }
      if ((task.phase as TaskRun['phase']) !== 'DONE' || TASK_DEFINITIONS[task.operation]?.navigation) break
    }
  }
  async function continueTask(entry: AssistantEntry, task: TaskRun, confirmed = false, file?: AgentFile, image = false) {
    if (!syncAccount() || busy.value || !entries.value.includes(entry) || !entry.tasks?.includes(task)) return
    if (entry.tasks.slice(0, entry.tasks.indexOf(task)).some(previous => previous.phase !== 'DONE')) return
    recoveryNotice.value = ''
    const run = generation
    busy.value = true
    try {
      if (task.phase === 'QUEUED') inheritCompleted(entry, task)
      task.useModel = useModel.value
      await runtime.prepare(task, confirmed, file, image)
      if (run === generation) await advance(entry)
    }
    finally { if (run === generation) busy.value = false }
  }
  async function attachFile(operation: string, file: AgentFile, image = false, context: AssistantContext = {}) {
    if (!syncAccount() || busy.value) return
    const pending = pendingTask()
    const reuse = pending && pending.task.phase === 'FILE'
    const entry = reactive<AssistantEntry>({ id: ++sequence, question: file.name, result: { reply: '收到文件，开始办理。', executionMode: 'DIRECT', actions: [], steps: ['已收到你选择的文件'] }, tasks: reuse ? [] : [runtime.create({ operation, parameters: {} }, context)] })
    if (reuse) movePending(pending, entry)
    entries.value = [...entries.value.slice(-7), entry]
    await continueTask(entry, entry.tasks![0], false, file, image)
  }
  async function locateTask(entry: AssistantEntry, task: TaskRun, manual = false) {
    if (!syncAccount() || busy.value || !entries.value.includes(entry) || !entry.tasks?.includes(task) || !task.locationRequired) return
    recoveryNotice.value = ''
    const run = generation
    busy.value = true
    locationPicking.value = manual
    try { await runtime.locate(task, manual) }
    finally { if (run === generation) { locationPicking.value = false; busy.value = false } }
  }
  function cancelTask(entry: AssistantEntry, task: TaskRun, fromInstruction = false) {
    if (busy.value && !fromInstruction) return
    const index = entry.tasks?.indexOf(task) ?? -1
    for (const pending of entry.tasks?.slice(index) || []) runtime.cancel(pending)
  }
  return { entries, draft, useModel, busy, locationPicking, sceneContext, recoveryNotice, clear, syncAccount, submit, continueTask, locateTask, attachFile, cancelTask }
}

export const assistantSession = createAssistantSession(undefined, undefined, assistantPersistence)
