import { reactive } from 'vue'
import { TASK_DEFINITIONS, select, referenceKey, referenceFor, rescueEligible, type TaskRun } from './assistant/catalog'
import { prepareTaskFields, executeTask } from './assistant/operations'
import { prepareTaskFiles } from './assistant/files'
import { request, ApiRequestError } from '@/api/http'
import { getAccountAccessToken } from '@/utils/authSession'
import { uploadAgentDocument, type AgentFile } from '@/api/agentDocuments'
import { uploadImage } from '@/api/files'
import type { AssistantTask, AssistantContext } from '@/api/agent'
import { getCurrentGcj02Location, chooseGcj02Location, type AppLocation } from '@/utils/location'

export type { TaskRun, TaskField } from './assistant/catalog'
const content = (data: any): any[] => Array.isArray(data) ? data : data?.content || []
function moduleName(path: string) {
  if (path.includes('/assistant/documents')) return '文档存储'
  return ({ settings: '设置', 'emergency-contacts': '联系人', 'emergency-devices': '设备管理', 'health-reports': '健康档案', 'science-submissions': '科普投稿', 'rescue-calls': '救援服务', 'rescue-resources': '附近急救资源', 'rescue-reviews': '救援复盘', 'coordination-tasks': '资源协同', 'health-monitoring': '健康监测', admin: '账户管理', profile: '个人资料' } as Record<string,string>)[path.split('/')[3]] || '业务服务'
}
export function createAssistantTaskRuntime(dependencies = { request, identity: getAccountAccessToken, upload: uploadAgentDocument, uploadImage }, location = { current: getCurrentGcj02Location, choose: chooseGcj02Location }) {
  const owners = new WeakMap<TaskRun, string>()
  const targets = new WeakMap<TaskRun, any[]>()
  const valid = (run: TaskRun) => {
    if (run.phase === 'CANCELED' || !owners.get(run) || dependencies.identity() !== owners.get(run)) throw new Error('账户或会话已变化，任务已停止')
  }
  async function api(run: TaskRun, path: string, method: UniNamespace.RequestOptions['method'] | 'PATCH' = 'GET', data?: any, timeout = 20000) {
    valid(run)
    run.steps.push(`${moduleName(path)}：${method === 'GET' ? '读取最新数据' : '提交操作'}`)
    let response: any
    if (method !== 'GET' && !path.endsWith('/recognition') && !path.endsWith('/nearby')) run.mutationStarted = true
    try { response = await dependencies.request(path, { method: method as UniNamespace.RequestOptions['method'], data, timeout }) }
    catch (error) {
      if (method !== 'GET' && !path.endsWith('/recognition') && error instanceof ApiRequestError && (error.code === 'NETWORK_ERROR' || (error.statusCode || 0) >= 500)) (error as ApiRequestError & { uncertain: boolean }).uncertain = true
      throw error
    }
    valid(run)
    return response
  }
  function create(task: AssistantTask, context: AssistantContext = {}): TaskRun {
    const run = reactive<TaskRun>({ operation: task.operation, parameters: { ...task.parameters }, title: TASK_DEFINITIONS[task.operation]?.title || '不支持的操作', phase: 'QUEUED', fields: [], steps: [], summary: '', items: [], links: [] })
    if (task.operation === 'RESCUE_CREATE') {
      // A model is never a source of real-world location or a medical urgency assessment.
      delete run.parameters.latitude; delete run.parameters.longitude; delete run.parameters.address
      run.parameters.urgency = 'HIGH'
      run.rescue = {}
    }
    run.context = { ...context }
    run.locationRequired = !!TASK_DEFINITIONS[task.operation]?.locationRequired
    owners.set(run, dependencies.identity())
    return run
  }
  function cancel(run: TaskRun) { if (!['DONE', 'UNCERTAIN'].includes(run.phase)) run.phase = 'CANCELED'; owners.delete(run) }
  function restore(saved: TaskRun): TaskRun {
    if (!TASK_DEFINITIONS[saved.operation] || !saved.parameters || Object.values(saved.parameters).some(value => typeof value !== 'string') || !Array.isArray(saved.fields) || !Array.isArray(saved.items) || !Array.isArray(saved.links) || !Array.isArray(saved.steps)) throw new Error('任务记录已失效')
    const run = create(saved, saved.context)
    Object.assign(run, saved, { target: undefined, restored: true })
    if (saved.phase === 'RUNNING') {
      run.phase = saved.mutationStarted ? 'UNCERTAIN' : 'ERROR'
      run.error = saved.mutationStarted ? '中断前已提交操作，结果尚未确认。请查看业务记录，避免重复办理。' : '任务已恢复，点击继续即可接着办理。'
    } else if (saved.phase === 'CONFIRM') {
      run.phase = 'INPUT'; run.summary = '任务已恢复，继续时会核对最新状态。'
    }
    if (!['DONE','CANCELED','UNCERTAIN'].includes(run.phase)) {
      run.location = undefined
      if (run.rescue) { run.rescue = {}; run.items = []; delete run.parameters.latitude; delete run.parameters.longitude; delete run.parameters.address }
    }
    return run
  }
  function patch(run: TaskRun, parameters: Record<string,string>) {
    valid(run)
    if (!['INPUT','FILE','CONFIRM','ERROR'].includes(run.phase)) throw new Error('这个任务当前不能补充')
    const allowed = new Set(run.fields.map(f => f.key))
    if (Object.keys(parameters).some(key => !allowed.has(key) || ['latitude','longitude','urgency'].includes(key))) throw new Error('只能补充当前任务允许的字段')
    if (parameters.key && parameters.key !== run.parameters.key) delete run.parameters.value
    Object.assign(run.parameters, parameters)
    run.steps.push('已接收本次补充，沿用当前任务的信息和文件')
    run.phase = 'INPUT'
  }
  async function prepare(run: TaskRun, confirmed = false, file?: AgentFile, image = false, locationMode?: 'current' | 'choose') {
    if (['DONE', 'UNCERTAIN', 'CANCELED', 'RUNNING'].includes(run.phase)) return
    let uploadingFile = false
    try {
      valid(run)
      if (!TASK_DEFINITIONS[run.operation]) throw new Error('当前版本还不能执行这个操作')
      if (run.restored) {
        // Re-fetch owner-visible candidates instead of trusting cached options or target state.
        run.fields = run.fields.filter(field => field.key !== 'id')
        run.restored = false; confirmed = false
      }
      run.error = undefined; run.phase = 'RUNNING'
      const p = run.parameters
      let op = run.operation
      const env = environment(run)
      if (op === 'DEVICE_CREATE') {
        p.type ||= 'FIXED'; p.serviceTime ||= '全天'
        if (!run.location) {
          run.summary = '正在获取设备登记位置…'
          try { run.location = await env.currentLocation() } catch { valid(run); run.phase = 'INPUT'; run.error = '未取得设备位置，请允许定位后重试，或在地图上选点。'; run.fields = TASK_DEFINITIONS.DEVICE_CREATE.fields!.map(f => ({...f})); return }
          p.address ||= run.location.address || `当前位置（${run.location.latitude.toFixed(5)}, ${run.location.longitude.toFixed(5)}）`
        }
      }
      if (op === 'RESCUE_CREATE') {
        const refreshed = !run.rescue?.location || !!locationMode || Date.now() - (run.rescue.locatedAt || 0) > 60000
        if (refreshed) {
          if (!await prepareRescue(run, locationMode === 'choose')) { run.phase = 'INPUT'; return }
          confirmed = false
        }
      }
      const upload = async <T>(send: () => Promise<T>): Promise<T> => {
        uploadingFile = true; run.mutationStarted = true
        const result = await send()
        uploadingFile = false; valid(run)
        return result
      }
      if (!await prepareTaskFiles(run, { api: env.api, upload: value => upload(() => dependencies.upload(value)), uploadImage: path => upload(() => dependencies.uploadImage(path, 'HEALTH_REPORT')) }, file, image)) return
      op = run.operation
      const definition = TASK_DEFINITIONS[op]
      if (file) confirmed = false
      if (p.name && /^(这台|这台设备|这个|当前|当前设备|刚才的|它|这篇文章|这份报告|这次救援|当前救援|这条提醒|紧急联系人|联系人|我的联系人|这个联系人|我的设备|设备|我的AED|文章|当前文章|提醒|稿件|报告)$/.test(p.name)) delete p.name
      const source = op === 'REMINDER_CREATE' && p.kind === 'DEVICE_REVIEW' ? '/api/v1/emergency-devices/mine' : op === 'REMINDER_CREATE' && p.kind === 'SCIENCE_REVIEW' ? '/api/v1/science-submissions' : definition.source
      if (source) {
        if (!run.fields.some(f => f.key === 'id')) {
          let candidates: any[]
          if (source === 'rescue-targets') {
            const profile = await api(run, '/api/v1/profile')
            const own = content(await api(run, '/api/v1/rescue-calls?size=100'))
            const assigned = profile.role === 'VOLUNTEER' ? content(await api(run, '/api/v1/rescue-calls/responder-tasks?size=100')).map(item => ({...item,_responder:true})) : []
            if (profile.role === 'VOLUNTEER' && run.context?.rescueId && !own.some(item => item.id === run.context!.rescueId) && !assigned.some(item => item.id === run.context!.rescueId)) assigned.push({ ...await api(run, '/api/v1/rescue-calls/responder-tasks/' + encodeURIComponent(run.context.rescueId)), _responder:true })
            candidates = [...new Map([...assigned, ...own].map(item => [item.id, item])).values()]
          } else candidates = content(await api(run, source + (source.includes('?') ? '&' : '?') + 'size=100'))
          candidates = candidates.filter(item => rescueEligible(op, item))
          const options = candidates.map((item: any) => ({ value: item.id, label: item.name || item.title || item.nickname || item.hospital || item.deviceName || `${item.address || '救援'} · ${item.createdAt?.slice(0,16) || item.id.slice(0,8)}` }))
          if (!options.length) throw new Error('当前没有符合状态和权限的可操作记录')
          const selector = select('id', '操作对象', options)
          run.fields.push(selector)
          if (!options.some(o => o.value === p.id)) delete p.id
          const key = op === 'REMINDER_CREATE' ? p.kind === 'DEVICE_REVIEW' ? 'deviceId' : 'submissionId' : referenceKey(op), contextId = key ? run.context?.[key] : undefined
          if (!p.id && p.name) {
            const matched = options.filter(o => o.label === p.name)
            if (matched.length === 1) p.id = matched[0].value
          }
          if (!p.id && !p.name && contextId) {
            if (!options.some(o => o.value === contextId)) throw new Error('当前对象已不可操作，请核对记录状态和账号权限')
            p.id = contextId
          }
          if (!p.id && !p.name && options.length === 1) p.id = options[0].value
          if (p.id) {
            selector.autoResolved = true
            run.target = candidates.find(item => item.id === p.id)
            run.context = { ...run.context, ...referenceFor(op,p.id) }
            run.steps.push(`已核对真实记录，选中：${options.find(o => o.value === p.id)!.label}`)
          }
          // Keep the authoritative candidates for conversational selection and later supplementation.
          targets.set(run, candidates)
        }
        run.target = targets.get(run)?.find(item => item.id === p.id)
        if (run.target) run.context = { ...run.context, ...referenceFor(op,p.id) }
      }
      await prepareTaskFields(run, env)
      const missing = run.fields.filter(f => !f.optional && !p[f.key]?.trim())
      if (missing.length) { run.phase = 'INPUT'; run.summary = '补充这些信息后，我会继续执行。'; return }
      for (const f of run.fields) if (f.options && !f.options.some(o => o.value === p[f.key])) throw new Error(`请重新选择${f.label}`)
      if (definition.confirm && !confirmed) { run.phase = 'CONFIRM'; run.summary = op === 'RESCUE_CREATE' ? '已获取位置，确认后立即发起站内呼救。' : op === 'REPORT_IMPORT' ? '请核对从原始报告整理出的信息，确认后保存到健康档案。' : `请确认${run.title}${run.target ? `：${run.target.name || run.target.title || run.target.hospital || run.target.address || '所选记录'}` : ''}。`; return }
      run.summary = '正在执行…'
      await executeTask(run, env)
      valid(run)
      run.phase = 'DONE'; run.mutationStarted = false; run.steps.push('业务接口已确认成功')
      if ((definition.createsRecord || definition.confirm || definition.refresh) && typeof uni !== 'undefined') uni.$emit('maian:agent-executed', { operation: op })
    } catch (error) {
      if (!owners.has(run) || dependencies.identity() !== owners.get(run)) { run.phase = 'CANCELED'; return }
      const uncertain = TASK_DEFINITIONS[run.operation]?.createsRecord && error instanceof ApiRequestError && ((error as ApiRequestError & { uncertain?: boolean }).uncertain || (uploadingFile && error.code === 'NETWORK_ERROR'))
      run.phase = uncertain ? 'UNCERTAIN' : 'ERROR'
      run.error = uncertain ? '网络中断，暂时无法确认是否已保存。请先查看业务记录，避免重复提交。' : error instanceof Error ? error.message : '操作未完成'
    } finally {
      // #ifdef H5
      if (file?.path.startsWith('blob:')) URL.revokeObjectURL(file.path)
      // #endif
    }
  }
  async function prepareRescue(run: TaskRun, manual: boolean) {
    run.rescue = {}; run.items = []
    delete run.parameters.latitude; delete run.parameters.longitude
    run.summary = manual ? '请在地图上选择现场位置…' : '正在获取当前位置…'
    run.steps.push(manual ? '打开地图选取现场位置' : '请求系统定位当前位置')
    let timer: ReturnType<typeof setTimeout> | undefined
    let position: AppLocation
    try {
      const pending = manual ? location.choose() : location.current()
      position = await (manual ? pending : Promise.race([pending, new Promise<never>((_, reject) => {
        timer = setTimeout(() => reject(new Error('定位超时')), 10000)
      })]))
      valid(run)
      if (!Number.isFinite(position.latitude) || !Number.isFinite(position.longitude) || Math.abs(position.latitude) > 90 || Math.abs(position.longitude) > 180) throw new Error('位置无效')
    } catch {
      valid(run)
      run.rescue.locationError = manual ? '未选择位置，请重新定位或在地图上选点。' : '未获取到位置，请允许定位后重试，或在地图上选择现场。'
      run.steps.push('未取得真实位置，未创建呼救')
      return false
    } finally { clearTimeout(timer) }
    run.rescue.location = position; run.rescue.locatedAt = Date.now()
    run.parameters.latitude = String(position.latitude); run.parameters.longitude = String(position.longitude)
    run.parameters.address = position.address || `当前位置（${position.latitude.toFixed(5)}, ${position.longitude.toFixed(5)}）`
    run.summary = '已获取位置，正在查找附近可用AED…'
    run.steps.push('已取得真实位置，查询附近可调度AED')
    try {
      const devices = await api(run, '/api/v1/rescue-resources/nearby', 'POST', { latitude: position.latitude, longitude: position.longitude }, 8000)
      run.items = devices.map((device: any) => ({ title: device.name, detail: `${device.address} · 直线距离${device.distanceMeters}米 · 预计取送${Math.max(1, Math.ceil(device.estimatedArrivalSeconds / 60))}分钟`, navigation: { latitude: device.latitude, longitude: device.longitude, name: device.name, address: device.address, mode: 'walk' as const } }))
      run.steps.push(devices.length ? `找到${devices.length}个可用候选，尚未占用设备` : '当前搜索范围内未找到可用AED，仍可发起呼救')
      if (!devices.length) run.rescue.resourceError = '当前范围内暂无可用AED，仍可发起呼救，由系统继续匹配。'
    } catch {
      valid(run)
      run.rescue.resourceError = '附近AED暂未查询成功，仍可直接确认呼救。'
    }
    return true
  }
  function environment(run: TaskRun) {
    return { api: (path: string, method?: UniNamespace.RequestOptions['method'] | 'PATCH', data?: any) => api(run,path,method,data), currentLocation: async () => {
      const point = await location.current(); valid(run)
      if (!Number.isFinite(point.latitude) || !Number.isFinite(point.longitude) || Math.abs(point.latitude)>90 || Math.abs(point.longitude)>180) throw new Error('定位无效，请重新获取真实位置')
      return point
    } }
  }
  async function locateRescue(run: TaskRun, manual = false) {
    if (run.operation === 'RESCUE_CREATE') return prepare(run, false, undefined, false, manual ? 'choose' : 'current')
    if (!run.locationRequired || ['DONE','CANCELED','UNCERTAIN','RUNNING'].includes(run.phase)) return
    try {
      valid(run); run.phase = 'RUNNING'
      const point = manual ? await location.choose() : await location.current()
      valid(run)
      if (!Number.isFinite(point.latitude) || !Number.isFinite(point.longitude)) throw new Error('位置无效')
      run.location = point
      run.parameters.address = point.address || `当前位置（${point.latitude.toFixed(5)}, ${point.longitude.toFixed(5)}）`
      run.phase = 'INPUT'; await prepare(run)
    } catch {
      if (!owners.has(run) || dependencies.identity() !== owners.get(run)) { run.phase = 'CANCELED'; return }
      run.phase = 'INPUT'; run.error = '未取得位置，请重试定位或在地图上选点。'
    }
  }
  function reuseDocument(from: TaskRun, to: TaskRun) {
    valid(from); valid(to)
    if (!from.document || !['SCIENCE_SUBMIT','REPORT_IMPORT'].includes(to.operation)) return
    to.document = from.document; to.fileName = from.document.name
    if (to.operation === 'SCIENCE_SUBMIT') to.parameters.content = from.document.text || ''
    to.steps.push('复用本次会话中已经上传的本人文档，无需再次选择或上传')
  }
  return { create, restore, prepare, cancel, patch, locateRescue, reuseDocument }
}
