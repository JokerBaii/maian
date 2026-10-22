import { describe, expect, it, vi } from 'vitest'
import { createAssistantTaskRuntime } from '../utils/assistantTasks'
import { ApiRequestError } from '../api/http'

function runtime(request = vi.fn(), identity = () => 'owner') {
  return createAssistantTaskRuntime({ request, identity, upload: vi.fn(), uploadImage: vi.fn() })
}
describe('global assistant task execution', () => {
  it('locates and searches eligible AEDs before a single rescue confirmation, then sends a valid call', async () => {
    const current = vi.fn().mockResolvedValue({ latitude: 30.27, longitude: 120.14, address: '现场东门' })
    const request = vi.fn().mockResolvedValueOnce([{ name: '附近AED', address: '体育馆', latitude: 30.271, longitude: 120.141, distanceMeters: 100, estimatedArrivalSeconds: 120 }]).mockResolvedValueOnce({ id: 'call-id' })
    const agent = createAssistantTaskRuntime({ request, identity: () => 'owner', upload: vi.fn(), uploadImage: vi.fn() }, { current, choose: vi.fn() })
    const task = agent.create({ operation: 'RESCUE_CREATE', parameters: { description: '有人晕倒了，需要急救', latitude: '1', longitude: '2', address: '模型猜测地址', urgency: 'CRITICAL' } })
    await agent.prepare(task, true)
    expect(task.phase).toBe('CONFIRM'); expect(current).toHaveBeenCalledOnce()
    expect(task.fields.map(f => f.key)).not.toContain('latitude')
    expect(task.fields.map(f => f.key)).not.toContain('longitude')
    expect(task.items[0].navigation?.name).toBe('附近AED')
    expect(request).toHaveBeenCalledTimes(1)
    expect(request.mock.calls[0][1].data).toEqual({ latitude: 30.27, longitude: 120.14 })
    await agent.prepare(task, true)
    expect(current).toHaveBeenCalledOnce(); expect(task.phase).toBe('DONE')
    expect(request.mock.calls[1][1].data).toMatchObject({ latitude: 30.27, longitude: 120.14, address: '现场东门', urgency: 'HIGH', symptoms: ['需要紧急救助'] })
  })
  it('offers map selection after denied location and never substitutes a default location', async () => {
    const current = vi.fn().mockRejectedValue(new Error('permission denied'))
    const choose = vi.fn().mockResolvedValue({ latitude: 31, longitude: 121, address: '选择的现场' })
    const request = vi.fn().mockResolvedValue([])
    const agent = createAssistantTaskRuntime({ request, identity: () => 'owner', upload: vi.fn(), uploadImage: vi.fn() }, { current, choose })
    const task = agent.create({ operation: 'RESCUE_CREATE', parameters: {} })
    await agent.prepare(task)
    expect(task.phase).toBe('INPUT'); expect(task.rescue?.location).toBeUndefined(); expect(request).not.toHaveBeenCalled()
    await agent.locateRescue(task, true)
    expect(task.phase).toBe('CONFIRM'); expect(task.parameters.address).toBe('选择的现场')
    expect(choose).toHaveBeenCalledOnce(); expect(request).toHaveBeenCalledTimes(1)
  })
  it('still permits rescue confirmation when nearby resource lookup fails', async () => {
    const request = vi.fn().mockRejectedValueOnce(new ApiRequestError('断网', 'NETWORK_ERROR')).mockResolvedValueOnce({ id: 'call-id' })
    const agent = createAssistantTaskRuntime({ request, identity: () => 'owner', upload: vi.fn(), uploadImage: vi.fn() }, { current: vi.fn().mockResolvedValue({ latitude: 30, longitude: 120 }), choose: vi.fn() })
    const task = agent.create({ operation: 'RESCUE_CREATE', parameters: {} })
    await agent.prepare(task); expect(task.phase).toBe('CONFIRM'); expect(task.rescue?.resourceError).toBeTruthy()
    await agent.prepare(task, true); expect(task.phase).toBe('DONE')
  })
  it('requires reconfirmation if the location is stale and refreshed', async () => {
    const current = vi.fn().mockResolvedValueOnce({ latitude: 30, longitude: 120 }).mockResolvedValueOnce({ latitude: 31, longitude: 121 })
    const request = vi.fn().mockResolvedValue([])
    const agent = createAssistantTaskRuntime({ request, identity: () => 'owner', upload: vi.fn(), uploadImage: vi.fn() }, { current, choose: vi.fn() })
    const task = agent.create({ operation: 'RESCUE_CREATE', parameters: {} })
    await agent.prepare(task)
    task.rescue!.locatedAt = Date.now() - 61000
    await agent.prepare(task, true)
    expect(task.phase).toBe('CONFIRM'); expect(task.rescue?.location?.latitude).toBe(31)
    expect(request.mock.calls.every(call => call[0] === '/api/v1/rescue-resources/nearby')).toBe(true)
  })
  it('drops a location response after the account changes', async () => {
    let owner = 'a'
    const request = vi.fn(), current = vi.fn().mockImplementation(async () => { owner = 'b'; return { latitude: 30, longitude: 120 } })
    const agent = createAssistantTaskRuntime({ request, identity: () => owner, upload: vi.fn(), uploadImage: vi.fn() }, { current, choose: vi.fn() })
    const task = agent.create({ operation: 'RESCUE_CREATE', parameters: {} })
    await agent.prepare(task)
    expect(task.phase).toBe('CANCELED'); expect(request).not.toHaveBeenCalled()
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
