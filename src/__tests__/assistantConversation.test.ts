import { describe, expect, it, vi } from 'vitest'
import { createAssistantSession } from '../composables/useAssistantSession'
import { createAssistantTaskRuntime } from '../utils/assistantTasks'
import type { AssistantResult } from '../api/agent'

const result=(operation:string,parameters:Record<string,string>={},continuation=false):AssistantResult=>({reply:'办理',executionMode:'RULES',actions:[],steps:[],tasks:[{operation,parameters}],continuation})
function setup(ask=vi.fn(),request=vi.fn(),upload=vi.fn()) {
  const runtime=createAssistantTaskRuntime({request,identity:()=> 'account',upload,uploadImage:vi.fn()},{current:vi.fn(),choose:vi.fn()})
  return {session:createAssistantSession({ask,identity:()=> 'account'},runtime),request,ask,upload}
}
describe('assistant conversation workflow',()=>{
  it('passes only recent ordinary dialogue to the model without leaking earlier task fields',async()=>{
    const ask=vi.fn().mockResolvedValueOnce(result('CONTACT_SAVE',{name:'私密姓名'}))
      .mockResolvedValue({reply:'你好，我是脉脉',executionMode:'SPRING_AI',actions:[],steps:[],conversation:true})
    const {session}=setup(ask)
    await session.submit('添加联系人'); await session.submit('Hello'); await session.submit('你能做什么？')
    expect(ask).toHaveBeenLastCalledWith('你能做什么？',true,undefined,undefined,[{question:'Hello',reply:'你好，我是脉脉'}])
    expect(JSON.stringify(ask.mock.calls.at(-1))).not.toContain('私密姓名')
  })
  it('merges spoken fields into one task and saves once when complete',async()=>{
    const ask=vi.fn().mockResolvedValueOnce(result('CONTACT_SAVE')).mockResolvedValueOnce(result('CONTACT_SAVE',{name:'妈妈',relation:'母亲'},true)).mockResolvedValueOnce(result('CONTACT_SAVE',{phone:'13800000000'},true))
    const {session,request}=setup(ask,vi.fn().mockResolvedValue({id:'contact'}))
    await session.submit('添加紧急联系人');const original=session.entries.value[0].tasks![0]
    await session.submit('我妈妈');expect(session.entries.value.at(-1)!.tasks![0]).toBe(original)
    expect(original.phase).toBe('INPUT');expect(request).not.toHaveBeenCalled()
    await session.submit('手机号13800000000')
    expect(original.phase).toBe('DONE');expect(request).toHaveBeenCalledOnce()
    expect(request.mock.calls[0][1].data).toEqual({name:'妈妈',relation:'母亲',phone:'13800000000'})
  })
  it('accepts confirmation and cancellation locally without another model call',async()=>{
    const ask=vi.fn().mockResolvedValue(result('CONTACT_DELETE'))
    const request=vi.fn().mockResolvedValueOnce([{id:'own',name:'家人'}]).mockResolvedValue(true)
    const {session}=setup(ask,request)
    await session.submit('删除联系人');expect(session.entries.value[0].tasks![0].phase).toBe('CONFIRM')
    await session.submit('确认');expect(ask).toHaveBeenCalledOnce();expect(request.mock.calls[1][0]).toBe('/api/v1/emergency-contacts/own')
    await session.submit('再删除联系人');await session.submit('取消')
    expect(session.entries.value.at(-1)!.tasks![0].phase).toBe('CANCELED')
    expect(request.mock.calls.filter(c=>c[1].method==='DELETE')).toHaveLength(1)
  })
  it('reuses the pending upload and retains its file during later supplementation',async()=>{
    const ask=vi.fn().mockResolvedValueOnce(result('REPORT_IMPORT')).mockResolvedValueOnce(result('REPORT_IMPORT',{hospital:'测试体检中心'},true))
    const request=vi.fn().mockResolvedValue({id:'report'})
    const upload=vi.fn().mockResolvedValue({id:'doc',name:'报告.txt',sizeBytes:100,text:'检查日期：2026-10-20\n血压 120/80 mmHg'})
    const {session}=setup(ask,request,upload)
    await session.submit('导入体检报告');const original=session.entries.value[0].tasks![0]
    await session.attachFile('DOCUMENT_AUTO',{path:'fixture',name:'报告.txt',size:100})
    expect(session.entries.value.at(-1)!.tasks![0]).toBe(original);expect(original.phase).toBe('INPUT')
    await session.submit('机构是测试体检中心');expect(original.phase).toBe('CONFIRM')
    await session.submit('确认');expect(original.phase).toBe('DONE');expect(upload).toHaveBeenCalledOnce();expect(request).toHaveBeenCalledOnce()
  })
  it('does not execute an older hidden confirmation after an unrelated query',async()=>{
    const ask=vi.fn().mockResolvedValueOnce(result('CONTACT_DELETE')).mockResolvedValue({reply:'找到入口',executionMode:'RULES',actions:[],steps:[]})
    const request=vi.fn().mockResolvedValue([{id:'own',name:'家人'}])
    const {session}=setup(ask,request)
    await session.submit('删除联系人');await session.submit('隐私设置在哪里');await session.submit('确认')
    expect(request).toHaveBeenCalledOnce();expect(session.entries.value[0].tasks![0].phase).toBe('CONFIRM')
  })
  it('uses the ordinal from the visible result list and still validates the real target',async()=>{
    const ask=vi.fn().mockResolvedValueOnce(result('DEVICE_LIST')).mockResolvedValueOnce(result('DEVICE_DISABLE'))
    const devices=[{id:'first',name:'东门'},{id:'second',name:'西门'}]
    const {session}=setup(ask,vi.fn().mockResolvedValue(devices))
    await session.submit('查看我的设备');await session.submit('停用第二台设备')
    expect(ask.mock.calls[1][2]).toEqual({deviceId:'second'})
    expect(session.entries.value.at(-1)!.tasks![0].parameters.id).toBe('second')
    expect(session.entries.value.at(-1)!.tasks![0].phase).toBe('CONFIRM')
  })
  it('keeps the current page object ahead of unrelated previous results',async()=>{
    const ask=vi.fn().mockResolvedValueOnce(result('DEVICE_LIST')).mockResolvedValue({reply:'收到',executionMode:'RULES',actions:[],steps:[]})
    const {session}=setup(ask,vi.fn().mockResolvedValue([{id:'device',name:'设备'}]))
    await session.submit('查看我的设备')
    await session.submit('收藏这个',{page:'pages/science/detail',articleId:'article'})
    expect(ask.mock.calls[1][2]).toEqual({page:'pages/science/detail',articleId:'article'})
  })
  it('rejects out-of-range list references before planning or executing anything',async()=>{
    const ask=vi.fn().mockResolvedValue(result('DEVICE_LIST'))
    const request=vi.fn().mockResolvedValue([{id:'device',name:'设备'}]),{session}=setup(ask,request)
    await session.submit('查看我的设备');await session.submit('停用第99台设备')
    expect(ask).toHaveBeenCalledOnce();expect(request).toHaveBeenCalledOnce()
    expect(session.entries.value.at(-1)!.error).toContain('没有第99项')
  })
  it('passes the uploaded file to the next planned operation without another file selection',async()=>{
    const answer=result('DOCUMENT_UPLOAD');answer.tasks!.push({operation:'REPORT_IMPORT',parameters:{}})
    const upload=vi.fn().mockResolvedValue({id:'doc',name:'报告.txt',text:'测试体检中心\n检查日期：2026-10-20\n血压 120/80 mmHg',sizeBytes:100})
    const {session,request}=setup(vi.fn().mockResolvedValue(answer),vi.fn().mockResolvedValue({id:'report'}),upload)
    await session.submit('上传并整理报告');await session.attachFile('DOCUMENT_AUTO',{path:'fixture',name:'报告.txt',size:100})
    const tasks=session.entries.value.at(-1)!.tasks!
    expect(tasks[0].phase).toBe('DONE');expect(tasks[1].phase).toBe('CONFIRM');expect(tasks[1].document?.id).toBe('doc')
    await session.submit('确认');expect(upload).toHaveBeenCalledOnce();expect(request).toHaveBeenCalledOnce()
  })

  it('follows the newly created record in a queued workflow instead of the old page object',async()=>{
    const answer=result('DEVICE_CREATE',{name:'新设备'});answer.tasks!.push({operation:'REMINDER_CREATE',parameters:{kind:'DEVICE_REVIEW',title:'审核跟进'}})
    const request=vi.fn().mockImplementation(async(path,options)=>{
      if(path==='/api/v1/emergency-devices'&&options.method==='POST')return {id:'new'}
      if(path.startsWith('/api/v1/emergency-devices/mine'))return [{id:'old',name:'旧设备'},{id:'new',name:'新设备'}]
      if(path==='/api/v1/assistant/reminders'){expect(options.data.targetId).toBe('new');return {id:'reminder'}}
      throw new Error('Unexpected request')
    })
    const runtime=createAssistantTaskRuntime({request,identity:()=> 'account',upload:vi.fn(),uploadImage:vi.fn()},{current:vi.fn().mockResolvedValue({latitude:30,longitude:120,address:'现场'}),choose:vi.fn()})
    const session=createAssistantSession({ask:vi.fn().mockResolvedValue(answer),identity:()=> 'account'},runtime)
    await session.submit('登记新设备并跟进它的审核',{page:'pages/device/add',deviceId:'old'});await session.submit('确认')
    expect(session.entries.value.at(-1)!.tasks!.map(task=>task.phase)).toEqual(['DONE','DONE'])
  })

})
