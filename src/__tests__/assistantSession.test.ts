import { describe, expect, it, vi } from 'vitest'
import { createAssistantSession } from '../composables/useAssistantSession'
import type { AssistantResult } from '../api/agent'

const answer: AssistantResult = { reply: '找到啦', executionMode: 'RULES', actions: [], steps: ['检索完成'] }
function deferred() {
  let resolve!: (value: AssistantResult) => void
  return { promise: new Promise<AssistantResult>(done => { resolve = done }), resolve: (value: AssistantResult) => resolve(value) }
}

describe('assistant session', () => {
  it('opens the emergency page from the model decision without preparing or submitting a rescue', async () => {
    const description = '有人晕倒了，在东门 & 服务台旁'
    const ask = vi.fn().mockResolvedValue({ ...answer, executionMode:'SPRING_AI', tasks:[{operation:'RESCUE_CREATE', parameters:{description, latitude:'1', longitude:'2'}}] })
    const navigate = vi.fn().mockResolvedValue(undefined)
    const runtime = (await import('../utils/assistantTasks')).createAssistantTaskRuntime({request:vi.fn(),identity:()=> 'a',upload:vi.fn(),uploadImage:vi.fn()},undefined,navigate)
    const session = createAssistantSession({ask,identity:()=> 'a'},runtime)
    await session.submit(description)
    expect(ask).toHaveBeenCalledOnce()
    expect(navigate).toHaveBeenCalledWith({url:'/pages/rescue/index?description='+encodeURIComponent(description)})
    expect(session.entries.value[0].tasks![0].phase).toBe('DONE')
  })
  it('executes a model-selected page jump without presenting an extra confirmation', async () => {
    const ask = vi.fn().mockResolvedValue({...answer,tasks:[{operation:'OPEN_PAGE',parameters:{url:'/pages/mine/settings?section=privacy',title:'隐私设置'}}]})
    const navigate = vi.fn().mockResolvedValue(undefined)
    const runtime = (await import('../utils/assistantTasks')).createAssistantTaskRuntime({request:vi.fn(),identity:()=> 'a',upload:vi.fn(),uploadImage:vi.fn()},undefined,navigate)
    const session = createAssistantSession({ask,identity:()=> 'a'},runtime)
    await session.submit('带我去管理数据共享的地方')
    expect(navigate).toHaveBeenCalledOnce();expect(session.entries.value[0].tasks![0].phase).toBe('DONE')
  })
  it('does not infer an emergency from words when the model chooses to discuss the scenario', async () => {
    const ask=vi.fn().mockResolvedValue({...answer,conversation:true}),navigate=vi.fn()
    const session=createAssistantSession({ask,identity:()=> 'a'})
    await session.submit('有人晕倒这一情境在演练里怎么展示')
    expect(ask).toHaveBeenCalledOnce();expect(navigate).not.toHaveBeenCalled()
  })
  it('prevents duplicate requests and respects the session model preference', async () => {
    const pending = deferred()
    const ask = vi.fn().mockReturnValueOnce(pending.promise).mockResolvedValue(answer)
    const session = createAssistantSession({ ask, identity: () => 'account-a' })
    session.syncAccount(); session.useModel.value = true; session.draft.value = ' 隐私设置 '
    const first = session.submit()
    await session.submit('duplicate')
    expect(ask).toHaveBeenCalledTimes(1)
    expect(ask).toHaveBeenCalledWith('隐私设置', true, undefined, undefined)
    expect(session.useModel.value).toBe(true)
    pending.resolve(answer); await first
    session.useModel.value = false
    await session.submit('推荐文章')
    expect(ask).toHaveBeenLastCalledWith('推荐文章', false, undefined, undefined)
    expect(session.entries.value).toHaveLength(2)
  })
  it('discards an old response and private history after an account change', async () => {
    let identity = 'account-a'
    const pending = deferred()
    const session = createAssistantSession({ ask: () => pending.promise, identity: () => identity })
    const first = session.submit('account-a query')
    identity = 'account-b'
    session.syncAccount()
    pending.resolve(answer); await first
    expect(session.entries.value).toHaveLength(0)
    expect(session.busy.value).toBe(false)
    expect(session.draft.value).toBe('')
  })
  it('does not reinsert a response if the conversation is cleared while waiting', async () => {
    const pending = deferred()
    const session = createAssistantSession({ ask: () => pending.promise, identity: () => 'a' })
    const request = session.submit('query')
    session.clear()
    pending.resolve(answer); await request
    expect(session.entries.value).toHaveLength(0)
    expect(session.busy.value).toBe(false)
  })
  it('allows a retry after a failed request and limits in-memory history', async () => {
    const ask = vi.fn().mockRejectedValueOnce(new Error('offline')).mockResolvedValue(answer)
    const session = createAssistantSession({ ask, identity: () => 'a' })
    await session.submit('privacy')
    expect(session.entries.value[0].error).toBeTruthy()
    expect(session.busy.value).toBe(false)
    for (let i = 0; i < 10; i++) await session.submit('query-' + i)
    expect(session.entries.value).toHaveLength(8)
    expect(session.entries.value[7].result).toEqual(answer)
  })
})
