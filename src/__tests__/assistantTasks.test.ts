import { describe, expect, it, vi } from 'vitest'
import { createAssistantTaskRuntime } from '../utils/assistantTasks'
import { ApiRequestError } from '../api/http'

function runtime(request = vi.fn(), identity = () => 'owner') {
  return createAssistantTaskRuntime({ request, identity, upload: vi.fn(), uploadImage: vi.fn() })
}
describe('global assistant task execution', () => {
  it('opens the emergency page once without waiting for GPS, querying AEDs or creating a call', async () => {
    const request = vi.fn(), current = vi.fn(), navigate = vi.fn().mockResolvedValue(undefined)
    const agent = createAssistantTaskRuntime({request,identity:()=> 'owner',upload:vi.fn(),uploadImage:vi.fn()}, {current,choose:vi.fn()}, navigate)
    const description = '有人晕倒了，需要急救'
    const task = agent.create({operation:'RESCUE_CREATE',parameters:{description,latitude:'1',longitude:'2',address:'猜测地址'}})
    await agent.prepare(task); await agent.prepare(task)
    expect(task.phase).toBe('DONE'); expect(navigate).toHaveBeenCalledOnce()
    expect(navigate).toHaveBeenCalledWith({url:'/pages/rescue/index?description='+encodeURIComponent(description)})
    expect(request).not.toHaveBeenCalled(); expect(current).not.toHaveBeenCalled()
    expect(task.parameters).toEqual({description})
  })
  it('retries a failed page navigation without retrying any rescue write', async () => {
    const request=vi.fn(), navigate=vi.fn().mockRejectedValueOnce(new Error('页面打开失败')).mockResolvedValue(undefined)
    const agent=createAssistantTaskRuntime({request,identity:()=> 'owner',upload:vi.fn(),uploadImage:vi.fn()}, {current:vi.fn(),choose:vi.fn()}, navigate)
    const task=agent.create({operation:'RESCUE_CREATE',parameters:{description:'需要帮助'}})
    await agent.prepare(task); expect(task.phase).toBe('ERROR')
    await agent.prepare(task); expect(task.phase).toBe('DONE'); expect(request).not.toHaveBeenCalled()
  })
  it('rejects a model-provided external page instead of navigating', async () => {
    const request=vi.fn(), agent=runtime(request)
    const task=agent.create({operation:'OPEN_PAGE',parameters:{url:'https://example.com'}})
    await agent.prepare(task); expect(task.phase).toBe('ERROR'); expect(request).not.toHaveBeenCalled()
  })
  it('drops a navigation result after the account changes', async () => {
    let owner='a'
    const navigate=vi.fn().mockImplementation(async()=> {owner='b'})
    const agent=createAssistantTaskRuntime({request:vi.fn(),identity:()=> owner,upload:vi.fn(),uploadImage:vi.fn()}, {current:vi.fn(),choose:vi.fn()}, navigate)
    const task=agent.create({operation:'RESCUE_CREATE',parameters:{}})
    await agent.prepare(task); expect(task.phase).toBe('CANCELED')
  })
  it('merges only requested settings with the latest server values', async () => {
    const original = { rescuePush: true, healthAlert: true, scienceUpdate: true, locationShare: false, healthDataShare: false, maxHeartRate: 130, minHeartRate: 50, updatedAt: 'now' }
    const request = vi.fn().mockResolvedValueOnce({ ...original }).mockImplementationOnce(async (_path, options) => options.data)
    const agent = runtime(request), task = agent.create({ operation: 'SETTINGS_UPDATE', parameters: { scienceUpdate: 'false', maxHeartRate: '140', injected: 'true' } })
    await agent.prepare(task)
    expect(task.phase).toBe('DONE')
    expect(request.mock.calls[1][1].data).toEqual({ rescuePush: true, healthAlert: true, scienceUpdate: false, locationShare: false, healthDataShare: false, maxHeartRate: 140, minHeartRate: 50 })
  })
  it('requires an account-visible target and confirmation before deleting', async () => {
    const request = vi.fn().mockResolvedValueOnce([{ id: 'own', name: '家人' }]).mockResolvedValue(true)
    const agent = runtime(request), task = agent.create({ operation: 'CONTACT_DELETE', parameters: { id: 'another-users-id', name: '家人' } })
    await agent.prepare(task)
    expect(task.parameters.id).toBe('own'); expect(task.phase).toBe('CONFIRM'); expect(request).toHaveBeenCalledTimes(1)
    await agent.prepare(task, true)
    expect(request.mock.calls[1][0]).toBe('/api/v1/emergency-contacts/own'); expect(task.phase).toBe('DONE')
  })
  it('stops before a mutation if the account changes while reading', async () => {
    let owner = 'a'
    const request = vi.fn().mockImplementation(async () => { owner = 'b'; return { scienceUpdate: true } })
    const agent = runtime(request, () => owner), task = agent.create({ operation: 'SETTINGS_UPDATE', parameters: { scienceUpdate: 'false' } })
    await agent.prepare(task)
    expect(task.phase).toBe('CANCELED'); expect(request).toHaveBeenCalledTimes(1)
  })
  it('does not retry a create whose response was lost after a possible commit', async () => {
    const request = vi.fn().mockRejectedValue(new ApiRequestError('连接中断', 'NETWORK_ERROR'))
    const agent = runtime(request), task = agent.create({ operation: 'CONTACT_SAVE', parameters: { name: '小明', phone: '13800000000', relation: '家人' } })
    await agent.prepare(task); expect(task.phase).toBe('UNCERTAIN')
    await agent.prepare(task); expect(request).toHaveBeenCalledTimes(1)
  })
  it('allows retrying a failed read and rejects arbitrary operations', async () => {
    const request = vi.fn().mockRejectedValueOnce(new ApiRequestError('断网', 'NETWORK_ERROR')).mockResolvedValue([])
    const agent = runtime(request), task = agent.create({ operation: 'CONTACT_LIST', parameters: {} })
    await agent.prepare(task); expect(task.phase).toBe('ERROR')
    await agent.prepare(task); expect(task.phase).toBe('DONE')
    const invalid = agent.create({ operation: 'EXECUTE_URL', parameters: { url: '/admin' } })
    await agent.prepare(invalid); expect(invalid.phase).toBe('ERROR'); expect(request).toHaveBeenCalledTimes(2)
  })
  it('collects missing values without executing and cancels pending tasks', async () => {
    const request = vi.fn(), agent = runtime(request), task = agent.create({ operation: 'CONTACT_SAVE', parameters: { name: '家人' } })
    await agent.prepare(task); expect(task.phase).toBe('INPUT'); expect(request).not.toHaveBeenCalled()
    agent.cancel(task); await agent.prepare(task); expect(task.phase).toBe('CANCELED'); expect(request).not.toHaveBeenCalled()
  })
})
