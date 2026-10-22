import { SETTINGS, TASK_DEFINITIONS, field, select, referenceFor, type TaskField, type TaskRun, type TaskEnvironment } from './catalog'
import { parseReportIndicators } from './files'
import type { AssistantAction } from '@/api/agent'
import { rescueStatusLabel } from '@/utils/presentation'
import { toServiceWindows } from '@/utils/deviceSchedule'
import { openMapNavigation } from '@/utils/location'

const page = (title: string, url: string): AssistantAction => ({ kind: 'PAGE', title, description: '查看实际业务记录', url })
const content = (data: any): any[] => Array.isArray(data) ? data : data?.content || []
const stateLabel = (value: string) => ({ USER: '普通用户', VOLUNTEER: '志愿者', ADMIN: '管理员', PENDING_REVIEW: '待审核', APPROVED: '已通过', REJECTED: '未通过', AVAILABLE: '可用', RESERVED: '救援占用', DISABLED: '暂停使用', COMPLETED: '已完成', RUNNING: '处理中', PENDING: '待处理', FAILED: '失败', ATTENTION: '需关注', USER_CANCELLED: '已取消', normal: '正常', high: '偏高', low: '偏低' } as Record<string,string>)[value] || value
const editableFieldLabels: Record<string, string> = { newName: '名称', phone: '手机号', relation: '关系', address: '设备地址', serviceTime: '服务时间' }

export async function prepareTaskFields(run: TaskRun, env: TaskEnvironment) {
  const p = run.parameters, op = run.operation
  if (run.operation === 'REMINDER_CREATE') {
    p.kind ||= 'TIME'
    if (!['TIME','DEVICE_REVIEW','SCIENCE_REVIEW'].includes(p.kind)) throw new Error('不支持这个提醒类型')
    const fields: TaskField[] = [{key:'title',label:'提醒内容'}, ...(p.kind === 'TIME' ? [{key:'dueAt',label:'提醒时间'}] : [])]
    for (const field of fields) if (!run.fields.some(f=>f.key===field.key)) run.fields.push(field)
  }
  if (['CONTACT_UPDATE','DEVICE_UPDATE'].includes(run.operation)) {
    if (!run.target) return
    const keys = run.operation === 'CONTACT_UPDATE' ? ['newName','phone','relation'] : ['newName','address','serviceTime']
    if (!keys.some(key => p[key]?.trim()) && !p.key) {
      run.fields = [...run.fields.filter(f => f.key === 'id'), {key:'key',label:'修改内容',type:'select',options:keys.map(value => ({ value,label:editableFieldLabels[value] }))}, {key:'value',label:'新值'}]
    } else if (p.key) {
      if (!keys.includes(p.key)) throw new Error('不支持修改这个字段')
      if (!p.value?.trim()) { if (!run.fields.some(f => f.key === 'value')) run.fields.push({key:'value',label:'新值'}) }
      else { p[p.key] = p.value; delete p.key; delete p.value }
    }
    if (!p.key && keys.some(key => p[key]?.trim())) run.fields = [...run.fields.filter(f => f.key === 'id'), ...keys.map(key => ({key,label:editableFieldLabels[key],optional:true}))]
  }
  if (run.operation === 'DEVICE_LOCATION') {
    if (run.target?.type !== 'MOBILE') throw new Error('只有移动设备支持上报当前位置')
    if (!run.location) {
      run.summary = '正在获取设备当前位置…'; run.location = await env.currentLocation()
      p.address = run.location.address || `当前位置（${run.location.latitude.toFixed(5)}, ${run.location.longitude.toFixed(5)}）`
      run.steps.push('已从系统取得真实位置，等待确认更新移动设备')
    }
  }
  if (['RESCUE_REPLACE','RESCUE_RESOURCE_FAILURE'].includes(run.operation) && !p.expectedDeviceId) {
    const task = await env.api(`/api/v1/rescue-calls/responder-tasks/${encodeURIComponent(p.id)}`)
    if (!task.matchedAed?.deviceId) throw new Error('当前任务没有关联AED')
    p.expectedDeviceId = task.matchedAed.deviceId
  }
  if (run.operation === 'SCIENCE_INTERACTION' && p.liked === undefined && p.collected === undefined) {
    run.fields = [...run.fields.filter(f => f.key === 'id'), { key:'key', label:'文章操作',type:'select',options:[{value:'collected',label:'收藏'},{value:'liked',label:'点赞'}] }, {key:'value',label:'设置为',type:'select',options:[{value:'true',label:'开启'},{value:'false',label:'取消'}]}]
    if (p.key && p.value && ['liked','collected'].includes(p.key)) p[p.key] = p.value
  }
  if (op === 'SETTINGS_UPDATE') {
    const explicit = Object.keys(SETTINGS).filter(k => p[k] !== undefined)
    if (!explicit.length && !p.key) run.fields = [select('key', '要修改的设置', Object.entries(SETTINGS).map(([value, label]) => ({ value, label }))), field('value', '新值（开关填 true/false，阈值填数字）')]
    else if (!explicit.length && p.key) run.fields = [field('value', '新值（开关填 true/false，阈值填数字）')]
  }
  for (const f of TASK_DEFINITIONS[op].fields || []) if (!run.fields.some(existing => existing.key === f.key)) run.fields.push({ ...f })
  if (op === 'SETTINGS_UPDATE') { const changed = Object.keys(SETTINGS).filter(key => p[key] !== undefined); if (changed.length === 1) run.context = { ...run.context, settingKey: changed[0] } }
}

export async function executeTask(run: TaskRun, env: TaskEnvironment) {
  const p = run.parameters, op = run.operation, id = encodeURIComponent(p.id || '')
  const { api } = env
  const definition = TASK_DEFINITIONS[op]
  if (definition.list) {
    const { path, title, destination } = definition.list
    const records = content(await api(path + (path.includes('?') ? '&' : '?') + 'size=100'))
    run.items = records.slice(0, 30).map((i: any) => ({ title: i.name || i.title || i.hospital || i.deviceName || i.nickname || i.address || '记录', detail: [i.phone, i.relation, i.status ? stateLabel(i.status) : '', i.summary, i.checkupDate, i.role ? stateLabel(i.role) : ''].filter(Boolean).join(' · '), documentId: op === 'DOCUMENT_LIST' ? i.id : undefined, reference: referenceFor(op, i.id) }))
    run.summary = `${title}：找到 ${records.length} 条记录${records.length > 30 ? '，这里显示前30条' : ''}。`
    if (destination) run.links = [page('打开完整记录', destination)]
    return
  }
  if (definition.rescueAction) {
    const payload = ['RESCUE_REPLACE','RESCUE_RESOURCE_FAILURE'].includes(op) ? { expectedDeviceId:p.expectedDeviceId } : undefined
    const saved = await api(`/api/v1/rescue-calls/${id}/${definition.rescueAction}`, 'POST', payload)
    run.summary = `${run.title}已由救援服务确认。当前状态：${rescueStatusLabel(saved.status)}`
    run.links = [page('查看救援进展', `/pages/rescue/detail?id=${id}${run.target?._responder ? "&mode=responder" : ""}`)]
    return
  }
  switch (op) {
    case 'SETTINGS_READ': {
      const value = await api('/api/v1/settings')
      run.items = Object.entries(SETTINGS).map(([key, title]) => ({ title, detail: typeof value[key] === 'boolean' ? value[key] ? '已开启' : '已关闭' : `${value[key]} BPM` })); run.summary = '这是你当前的设置。'; break
    }
    case 'SETTINGS_UPDATE': {
      const changes: Record<string, string> = {}
      for (const key of Object.keys(SETTINGS)) if (p[key] !== undefined) changes[key] = p[key]
      if (p.key) { if (!SETTINGS[p.key]) throw new Error('不支持修改该设置'); changes[p.key] = p.value }
      if (!Object.keys(changes).length) throw new Error('请说明要修改的设置')
      const current = await api('/api/v1/settings')
      delete current.updatedAt
      for (const [key, value] of Object.entries(changes)) {
        if (key.endsWith('HeartRate')) { if (!/^\d+$/.test(value)) throw new Error('心率阈值必须为整数'); current[key] = Number(value) }
        else { if (!['true', 'false'].includes(value)) throw new Error('开关值必须为true或false'); current[key] = value === 'true' }
      }
      const saved = await api('/api/v1/settings', 'PUT', current)
      run.summary = Object.keys(changes).map(key => `${SETTINGS[key]}已${typeof saved[key] === 'boolean' ? saved[key] ? '开启' : '关闭' : `改为 ${saved[key]} BPM`}`).join('；')
      run.links = [page('查看设置', '/pages/mine/settings')]; break
    }
    case 'CONTACT_SAVE': { const saved = await api('/api/v1/emergency-contacts', 'POST', { name: p.name, phone: p.phone, relation: p.relation }); run.output = {contactId:saved.id}; run.context = {...run.context,...run.output}; run.summary = `已保存联系人 ${p.name}。`; run.links = [page('查看联系人', '/pages/mine/contacts')]; break }
    case 'CONTACT_DELETE': await api(`/api/v1/emergency-contacts/${id}`, 'DELETE'); run.summary = '所选联系人已删除。'; break
    case 'DEVICE_CREATE': { const saved = await api('/api/v1/emergency-devices', 'POST', { name: p.name, address: p.address, type: p.type, category: p.category || 'AED', latitude: run.location!.latitude, longitude: run.location!.longitude, serviceWindows: toServiceWindows(p.serviceTime), imageMediaIds: [], vehicleImageMediaIds: [] }); run.output = {deviceId:saved.id}; run.context = {...run.context,...run.output}; run.summary = '设备已登记并提交审核。'; run.links = [page('查看我的设备', '/pages/device/manage')]; break }
    case 'DEVICE_ENABLE': case 'DEVICE_DISABLE': case 'DEVICE_FAULT': case 'DEVICE_DELETE': {
      const suffix = { DEVICE_ENABLE: '/enable', DEVICE_DISABLE: '/disable', DEVICE_FAULT: '/fault', DEVICE_DELETE: '' }[op]
      await api(`/api/v1/emergency-devices/${id}${suffix}`, op === 'DEVICE_DELETE' ? 'DELETE' : 'POST'); run.summary = `${run.title}已完成。`; run.links = [page('查看设备与协同结果', '/pages/device/manage')]; break
    }
    case 'DOCUMENT_UPLOAD': run.summary = `${run.document?.name || '文档'} 已上传保存。${run.document?.notice || ''}`; run.items = [{ title: run.document!.name, detail: `${run.document!.sizeBytes} 字节 · 仅本人可访问`, documentId: run.document!.id }]; break
    case 'DOCUMENT_DELETE': await api(`/api/v1/assistant/documents/${id}`, 'DELETE'); run.summary = '所选文档已删除。'; break
    case 'REPORT_DELETE': await api(`/api/v1/health-reports/${id}`, 'DELETE'); run.summary = '所选体检报告已删除。'; break
    case 'REPORT_IMPORT': {
      const indicators = parseReportIndicators(p.indicators)
      const report = await api('/api/v1/health-reports', 'POST', { hospital: p.hospital, checkupDate: p.checkupDate, sourceMediaId: run.mediaId, indicators })
      run.output = {reportId:report.id}; run.context = {...run.context,...run.output}
      run.summary = '体检报告已保存到健康档案。'; run.links = [page('查看报告', '/pages/checkup/report?id=' + encodeURIComponent(report.id))]; break
    }
    case 'SCIENCE_SUBMIT': {
      if (p.title.length > 50 || p.content.length > 2000) throw new Error('投稿标题最多50字、正文最多2000字，请在这里调整后提交')
      const result = await api('/api/v1/science-submissions', 'POST', { title: p.title, category: p.category, content: p.content })
      run.output = {submissionId:result.id}; run.context = {...run.context,...run.output}
      run.summary = `《${result.title}》已提交审核，通过后才会发布。`; run.links = [page('查看我的投稿', '/pages/science/submissions')]; break
    }
    case 'SCIENCE_REVIEW': case 'DEVICE_REVIEW': await api(`/api/v1/${op === 'SCIENCE_REVIEW' ? 'science-submissions' : 'emergency-devices'}/${id}/review`, 'PATCH', { approved: p.approved === 'true', reviewNote: p.reviewNote }); run.summary = '审核结果已保存。'; break
    case 'RESCUE_REVIEW': await api(`/api/v1/rescue-reviews/${id}`, 'POST', { useModel: false }); run.summary = '复盘任务已提交，后台正在整理真实救援记录。'; run.links = [page('查看复盘进展', '/pages/rescue/detail?id=' + id)]; break
    case 'RESCUE_FEEDBACK': await api(`/api/v1/rescue-calls/${id}/feedback`, 'POST', { rating: numeric(p.rating), comment: p.comment }); run.summary = '救援评价已提交。'; break
    case 'RESCUE_CANCEL': await api(`/api/v1/rescue-calls/${id}/cancellation`, 'POST'); run.summary = '所选呼救已取消。'; break
    case 'RESCUE_CREATE': {
      const position = run.rescue?.location
      if (!position) throw new Error('请先定位或在地图上选择现场位置')
      const call = await api('/api/v1/rescue-calls', 'POST', { address: p.address, latitude: position.latitude, longitude: position.longitude, urgency: 'HIGH', description: p.description, symptoms: ['需要紧急救助'], clientRequestId: `assistant-${Date.now()}-${Math.random().toString(16).slice(2)}` })
      run.output = {rescueId:call.id}; run.context = {...run.context,...run.output}
      run.items = []; run.summary = '呼救已创建，系统正在匹配资源。'; run.links = [page('查看呼救进展', '/pages/rescue/detail?id=' + encodeURIComponent(call.id))]; break
    }
    case 'COORDINATION_RETRY': await api(`/api/v1/coordination-tasks/${id}/retry`, 'POST'); run.summary = '协同重试已提交。'; break
    case 'HEALTH_STATUS': { const value = await api('/api/v1/health-monitoring'); run.summary = `当前记录心率 ${value.current} BPM，今日均值 ${value.avg} BPM。`; run.items = [{ title: '记录状态', detail: stateLabel(value.status) }, { title: '今日预警', detail: `${value.alerts.length} 条` }]; break }
    case 'PROFILE_READ': { const value = await api('/api/v1/profile'); run.summary = `${value.nickname} · ${value.phone}`; run.items = [{ title: '角色', detail: stateLabel(value.role) }, { title: '身份状态', detail: value.verified ? '已校验' : '未校验' }]; break }
    case 'ADMIN_ROLE': await api(`/api/v1/admin/users/${id}/role`, 'PATCH', { role: p.role }); run.summary = '账户角色已更新。'; break
    case 'VOLUNTEER_AVAILABILITY': {
      if (!['true','false'].includes(p.available)) throw new Error('请说明开始接单或暂停接单')
      if (p.available === 'false') await api('/api/v1/rescue-calls/responder-presence','DELETE')
      else { const point = await env.currentLocation(); await api('/api/v1/rescue-calls/responder-presence','PUT',{latitude:point.latitude,longitude:point.longitude,available:true}) }
      run.summary=p.available==='true'?'已更新当前位置并设为可响应。':'已暂停接收新的救援，不影响已接任务。';return
    }
    case 'REMINDER_CREATE': {
      const reminder=await api('/api/v1/assistant/reminders','POST',{title:p.title,kind:p.kind||'TIME',dueAt:p.dueAt,targetId:p.kind==='TIME'?undefined:p.id})
      run.summary=reminder.dueAt ? `已设置站内提醒：${new Date(reminder.dueAt).toLocaleString('zh-CN',{timeZone:'Asia/Shanghai',hour12:false})}。` : '已保存跟进，审核状态变化后将在应用内提醒。'
      run.output={reminderId:reminder.id};run.context={...run.context,...run.output};return
    }
    case 'REMINDER_LIST': {
      const reminders=await api('/api/v1/assistant/reminders')
      run.items=reminders.map((r:any)=>({title:r.title,detail:r.result || (r.dueAt ? new Date(r.dueAt).toLocaleString('zh-CN',{timeZone:'Asia/Shanghai',hour12:false}) : '等待审核状态变化'),reference:{reminderId:r.id}}))
      run.summary=`共有${reminders.length}条未结束提醒，可直接说“取消第几条提醒”。`;return
    }
    case 'REMINDER_DISMISS': await api('/api/v1/assistant/reminders/'+id,'DELETE');run.summary='这条提醒已结束。';return
    case 'CONTACT_UPDATE': {
      const current = (await api('/api/v1/emergency-contacts')).find((item: any) => item.id === p.id)
      if (!current) throw new Error('联系人已不存在，请重新选择')
      await api('/api/v1/emergency-contacts/' + id,'PUT',{name:p.newName || current.name,phone:p.phone || current.phone,relation:p.relation || current.relation})
      run.summary='联系人已更新，未提及的信息保持原值。'; return
    }
    case 'CONTACT_CALL': {
      const current = (await api('/api/v1/emergency-contacts')).find((item: any) => item.id === p.id)
      if (!current || !/^1[3-9]\d{9}$/.test(current.phone)) throw new Error('未找到有效联系人号码')
      await new Promise<void>((resolve,reject) => uni.makePhoneCall({phoneNumber:current.phone,success:()=>resolve(),fail:reject}))
      run.summary='已打开系统拨号，请在电话界面完成通话。'; return
    }
    case 'DEVICE_UPDATE': {
      const current = await api('/api/v1/emergency-devices/mine/' + id)
      const payload = Object.fromEntries(['name','address','type','category','longitude','latitude','ownerPhone','serviceWindows','expireDate','owner','vehicleInfo','serviceRange','instructions','imageMediaIds','vehicleImageMediaIds'].map(key => [key,current[key]]))
      if (p.newName) payload.name=p.newName
      if (p.address) payload.address=p.address
      if (p.serviceTime) payload.serviceWindows=toServiceWindows(p.serviceTime)
      await api('/api/v1/emergency-devices/' + id,'PUT',payload)
      run.summary='设备资料已更新，未提及的资料保持原值。'; return
    }
    case 'DEVICE_LOCATION':
      if (!run.location) throw new Error('尚未取得真实位置')
      await api('/api/v1/emergency-devices/' + id + '/location','PATCH',{latitude:run.location.latitude,longitude:run.location.longitude,address:p.address})
      run.summary='已更新移动设备的位置。'; return
    case 'DEVICE_NEARBY': {
      const point = run.location || await env.currentLocation()
      run.location = point
      const devices = await api('/api/v1/rescue-resources/nearby','POST',{latitude:point.latitude,longitude:point.longitude})
      run.items = devices.map((device: any) => ({title:device.name,detail:`${device.address} · 直线距离${device.distanceMeters}米 · 预计取送${Math.max(1,Math.ceil(device.estimatedArrivalSeconds/60))}分钟`,reference:{deviceId:device.id},navigation:{name:device.name,address:device.address,latitude:device.latitude,longitude:device.longitude}}))
      run.summary=devices.length ? `已自动定位，找到${devices.length}个附近可用AED，按预计取送时间排序。` : '当前范围内暂无可用AED。'
      return
    }
    case 'DEVICE_NAVIGATE': {
      const device = await api('/api/v1/emergency-devices/' + id)
      openMapNavigation({latitude:device.latitude,longitude:device.longitude,name:device.name,address:device.address})
      run.summary='已打开设备导航。'; return
    }
    case 'RESCUE_PROGRESS': {
      const detail = await api(run.target?._responder ? `/api/v1/rescue-calls/responder-tasks/${id}` : `/api/v1/rescue-calls/${id}`)
      run.summary=`救援状态：${rescueStatusLabel(detail.status)}。`
      run.items=[{title:'现场位置',detail:detail.address || '接单后可见'},{title:'匹配设备',detail:detail.matchedAed?.name || '尚未匹配'},{title:'预计取送',detail:detail.matchedAed ? `${Math.ceil(detail.matchedAed.estimatedArrivalSeconds/60)}分钟（估算）` : '等待资源'}]
      run.links=[page('打开救援详情',`/pages/rescue/detail?id=${id}${run.target?._responder ? "&mode=responder" : ""}`)];return
    }
    case 'RESCUE_NAVIGATE': {
      const detail = await api(run.target?._responder ? `/api/v1/rescue-calls/responder-tasks/${id}` : `/api/v1/rescue-calls/${id}`)
      const target = p.destination==='SCENE' ? {latitude:detail.latitude,longitude:detail.longitude,name:'求救现场',address:detail.address} : detail.matchedAed
      if (!target || !Number.isFinite(target.latitude) || !Number.isFinite(target.longitude)) throw new Error('当前阶段暂无可导航的位置，请先接单并确认任务')
      openMapNavigation(target);run.summary='已使用当前救援的真实位置打开导航。';return
    }
    case 'RESCUE_OFFERS': {
      const value=await api('/api/v1/rescue-calls/responder-tasks?size=100')
      const offers=value.content.filter((item: any)=>item.status==='MATCHING')
      run.items=offers.map((item:any)=>({title:`待接救援 · ${item.createdAt.slice(0,16)}`,detail:item.distanceToRequesterMeters ? `距现场约${item.distanceToRequesterMeters}米` : '等待响应',reference:{rescueId:item.id}}))
      run.summary=offers.length ? `当前可接任务${offers.length}个。` : '当前没有可接任务，可说“开始接单”更新位置并设为可响应。';return
    }
    case 'SCIENCE_DELETE': await api('/api/v1/science-submissions/' + id,'DELETE');run.summary='稿件已删除。';return
    case 'SCIENCE_INTERACTION': {
      const current=await api('/api/v1/science-articles/' + id + '/interaction')
      const payload={liked:current.liked,collected:current.collected}
      for (const key of ['liked','collected'] as const) if (p[key]!==undefined) {if(!['true','false'].includes(p[key]))throw new Error('请选择开启或取消');payload[key]=p[key]==='true'}
      await api('/api/v1/science-articles/' + id + '/interaction','PUT',payload)
      run.summary='文章操作已保存。';return
    }
    case 'SCIENCE_READ': {
      const article=await api('/api/v1/science-content/' + id)
      run.summary=article.summary || article.title;run.links=[page('阅读完整文章','/pages/science/detail?id='+id)];return
    }
    case 'WEARABLE_READ': {
      const device=await api('/api/v1/wearable-device');run.summary=device.name ? `${device.name} · ${device.connected?'已记录连接':'未连接'} · 电量${device.battery}%` : '尚未绑定穿戴设备。';return
    }
    case 'WEARABLE_UNBIND': await api('/api/v1/wearable-device','DELETE');run.summary='已解除本账号的穿戴设备绑定记录。';return
    default: throw new Error('当前版本还不能执行这个操作')
  }
}
function numeric(value: string) { const result = Number(value); if (!value?.trim() || !Number.isFinite(result)) throw new Error('请填写有效数字'); return result }
