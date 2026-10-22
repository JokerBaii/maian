import { afterEach, describe, expect, it, vi } from 'vitest'
import { createAssistantSession } from '../composables/useAssistantSession'
import { createAssistantTaskRuntime } from '../utils/assistantTasks'
import { assistantPersistence, ASSISTANT_SESSION_KEY, type AssistantPersistence, type AssistantSnapshot } from '../utils/assistantPersistence'
import { clearAccountSession } from '../utils/authSession'

function setup() {
  let owner = 'account-a', saved: AssistantSnapshot | undefined
  const storage: AssistantPersistence = { load: () => saved && structuredClone(saved), save: (_, data) => { saved = JSON.parse(JSON.stringify(data)) }, clear: () => { saved = undefined } }
  const request = vi.fn().mockResolvedValue({ id: 'created' }), upload = vi.fn()
  const ask = vi.fn().mockResolvedValue({ reply: '', executionMode: 'DIRECT', actions: [], steps: [], tasks: [{ operation: 'CONTACT_SAVE', parameters: {} }] })
  const session = () => createAssistantSession({ ask, identity: () => owner }, createAssistantTaskRuntime({ request, upload, uploadImage: vi.fn(), identity: () => owner }, { current: vi.fn(), choose: vi.fn() }), storage)
  return { session, request, upload, ask, saved: () => saved, setOwner: (next: string) => { owner = next } }
}
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks() })
describe('assistant recovery', () => {
  it('restores form edits, draft and model choice, then saves the original task once', async () => {
    const test = setup(), first = test.session()
    await first.submit('添加联系人'); const task = first.entries.value[0].tasks![0]
    task.parameters.name = '妈妈'; task.parameters.relation = '母亲'; first.draft.value = '待补手机号'; first.useModel.value = false
    const restored = test.session(); restored.syncAccount()
    expect(restored.draft.value).toBe('待补手机号'); expect(restored.useModel.value).toBe(false)
    const next = restored.entries.value[0].tasks![0]
    expect(next.parameters).toEqual({ name: '妈妈', relation: '母亲' }); expect(restored.recoveryNotice.value).toBeTruthy()
    next.parameters.phone = '13800000000'
    await restored.continueTask(restored.entries.value[0], next)
    expect(test.request).toHaveBeenCalledOnce(); expect(next.phase).toBe('DONE')
  })
  it('keeps uploaded documents and resumes without another upload', async () => {
    const test = setup(); test.ask.mockResolvedValue({ reply:'',executionMode:'DIRECT',actions:[],steps:[],tasks:[{operation:'REPORT_IMPORT',parameters:{}}] })
    const first = test.session(); await first.submit('导入报告')
    const task = first.entries.value[0].tasks![0]
    task.document = { id:'doc',name:'报告.txt',sizeBytes:12,contentType:'text/plain',createdAt:'now',text:'医院\n2026-10-22\n血糖 5 mmol/L' }
    task.parameters = { hospital:'医院',checkupDate:'2026-10-22',indicators:'血糖 5 mmol/L' }; task.phase='CONFIRM'
    const restored = test.session(); restored.syncAccount(); const next = restored.entries.value[0].tasks![0]
    await restored.continueTask(restored.entries.value[0], next, true)
    expect(next.document?.id).toBe('doc'); expect(next.phase).toBe('CONFIRM'); expect(test.upload).not.toHaveBeenCalled(); expect(test.request).not.toHaveBeenCalled()
  })
  it('checkpoints before a write and never repeats an interrupted submission', async () => {
    const test = setup(); test.request.mockImplementation(() => new Promise(() => {}))
    test.ask.mockResolvedValue({reply:'',executionMode:'DIRECT',actions:[],steps:[],tasks:[{operation:'CONTACT_SAVE',parameters:{name:'妈妈',phone:'13800000000',relation:'母亲'}}]})
    const first = test.session(); void first.submit('保存联系人')
    await vi.waitFor(() => expect(test.request).toHaveBeenCalledOnce())
    expect(test.saved()!.entries[0].tasks![0].mutationStarted).toBe(true)
    const restored = test.session(); restored.syncAccount(); const task = restored.entries.value[0].tasks![0]
    expect(task.phase).toBe('UNCERTAIN')
    await restored.continueTask(restored.entries.value[0], task, true)
    expect(test.request).toHaveBeenCalledOnce()
  })
  it('refreshes actual target ownership before a restored destructive action', async () => {
    const test = setup(); test.ask.mockResolvedValue({reply:'',executionMode:'DIRECT',actions:[],steps:[],tasks:[{operation:'CONTACT_DELETE',parameters:{id:'old'}}]})
    test.request.mockResolvedValue([{id:'old',name:'妈妈'}])
    const first = test.session(); await first.submit('删除联系人')
    const restored = test.session(); restored.syncAccount()
    test.request.mockResolvedValue([{id:'different',name:'其他人'}])
    const task=restored.entries.value[0].tasks![0]; await restored.continueTask(restored.entries.value[0],task,true)
    expect(task.phase).toBe('ERROR'); expect(test.request.mock.calls.every(call=>call[1].method==='GET')).toBe(true)
  })
  it('does not run a queued write before its previous task finishes', async () => {
    const test=setup(); test.ask.mockResolvedValue({reply:'',executionMode:'DIRECT',actions:[],steps:[],tasks:[{operation:'CONTACT_SAVE',parameters:{}},{operation:'SETTINGS_UPDATE',parameters:{scienceUpdate:'false'}}]})
    const first=test.session();await first.submit('两个任务')
    const restored=test.session();restored.syncAccount();const entry=restored.entries.value[0]
    await restored.continueTask(entry,entry.tasks![1]);expect(test.request).not.toHaveBeenCalled()
  })
  it('can repeat an interrupted read without treating it as a completed write', async () => {
    const test=setup();test.ask.mockResolvedValue({reply:'',executionMode:'DIRECT',actions:[],steps:[],tasks:[{operation:'SETTINGS_READ',parameters:{}}]})
    test.request.mockImplementationOnce(()=>new Promise(()=>{}))
    const first=test.session();void first.submit('查看设置');await vi.waitFor(()=>expect(test.request).toHaveBeenCalledOnce())
    const restored=test.session();restored.syncAccount();const entry=restored.entries.value[0],task=entry.tasks![0]
    expect(task.phase).toBe('ERROR');test.request.mockResolvedValue({scienceUpdate:false})
    await restored.continueTask(entry,task);expect(task.phase).toBe('DONE');expect(test.request).toHaveBeenCalledTimes(2)
  })
  it('short confirmation cannot skip an uncertain write to run the next queued task', async () => {
    const test=setup();test.ask.mockResolvedValueOnce({reply:'',executionMode:'DIRECT',actions:[],steps:[],tasks:[{operation:'CONTACT_SAVE',parameters:{name:'家人',phone:'13800000000',relation:'家人'}},{operation:'SETTINGS_UPDATE',parameters:{scienceUpdate:'false'}}]}).mockResolvedValue({reply:'请说明指令',executionMode:'DIRECT',actions:[],steps:[]})
    test.request.mockImplementation(()=>new Promise(()=>{}))
    void test.session().submit('保存后改设置');await vi.waitFor(()=>expect(test.request).toHaveBeenCalledOnce())
    const restored=test.session();restored.syncAccount();const queued=restored.entries.value[0].tasks![1]
    await restored.submit('确认')
    expect(queued.phase).toBe('QUEUED');expect(test.request).toHaveBeenCalledOnce()
  })
  it('never restores an old incident location as a current GPS fix', () => {
    const runtime=createAssistantTaskRuntime({request:vi.fn(),upload:vi.fn(),uploadImage:vi.fn(),identity:()=> 'a'},{current:vi.fn(),choose:vi.fn()})
    const task=runtime.create({operation:'RESCUE_CREATE',parameters:{description:'需要帮助'}})
    task.phase='CONFIRM';task.rescue={location:{latitude:22,longitude:113},locatedAt:Date.now()};task.parameters.latitude='22';task.parameters.longitude='113';task.parameters.address='旧位置'
    const restored=runtime.restore(JSON.parse(JSON.stringify(task)))
    expect(restored.phase).toBe('INPUT');expect(restored.rescue?.location).toBeUndefined();expect(restored.parameters.latitude).toBeUndefined();expect(restored.parameters.address).toBeUndefined()
  })
  it('clear and account switching remove history and checkpoints', async () => {
    const test=setup(),first=test.session();await first.submit('联系人');first.clear();expect(test.saved()).toBeUndefined()
    await first.submit('联系人');test.setOwner('account-b');first.syncAccount()
    expect(first.entries.value).toEqual([]);expect(test.saved()?.entries).toEqual([])
  })
  it('expires and isolates local snapshots without storing an access token', () => {
    const data=new Map<string,string>();vi.stubGlobal('uni',{getStorageSync:(key:string)=>data.get(key),setStorageSync:(key:string,value:string)=>data.set(key,value),removeStorageSync:(key:string)=>data.delete(key)})
    const snapshot={entries:[],draft:'草稿',useModel:false,scene:{}}
    assistantPersistence.save('secret-token',snapshot)
    expect(data.get(ASSISTANT_SESSION_KEY)).not.toContain('secret-token');expect(assistantPersistence.load('secret-token')?.draft).toBe('草稿')
    expect(assistantPersistence.load('other-account')).toBeUndefined()
    assistantPersistence.save('secret-token',snapshot);vi.spyOn(Date,'now').mockReturnValue(Date.now()+86400001)
    expect(assistantPersistence.load('secret-token')).toBeUndefined()
    assistantPersistence.save('secret-token',snapshot);clearAccountSession();expect(data.has(ASSISTANT_SESSION_KEY)).toBe(false)
  })
})
