import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiRequestError } from '../api/http'
import { createAssistantTaskRuntime } from '../utils/assistantTasks'

const report = (hospital: string, value: string) => ({ hospital, checkupDate: '2026-10-22', indicators: [{ name: '血糖', value, unit: 'mmol/L', abnormal: false }] })
const file = (name: string) => ({ path: name, name, size: 100 })
const document = (name: string, text: string) => ({ id: name, name, text, sizeBytes: 100, contentType: 'text/plain', createdAt: '2026-10-22' })
const dependencies = (request = vi.fn()) => ({ request, identity: () => 'owner', upload: vi.fn(), uploadImage: vi.fn() })
afterEach(() => vi.unstubAllGlobals())

describe('assistant report import', () => {
  it('restores a pending consent dialog as retryable when the image upload is already confirmed', async () => {
    let answer!: (result: { confirm: boolean }) => void
    const showModal = vi.fn(({ success }) => { answer = success })
    vi.stubGlobal('uni', { showModal })
    const request = vi.fn().mockImplementation(async path => {
      if (path === '/api/v1/settings') return { healthDataShare: false }
      throw new ApiRequestError('未授权', 'FORBIDDEN', [], 403)
    })
    const deps = dependencies(request)
    deps.uploadImage.mockResolvedValue({ mediaId: 'private-image' })
    const runtime = createAssistantTaskRuntime(deps), task = runtime.create({ operation: 'REPORT_IMPORT', parameters: {} })
    const pending = runtime.prepare(task, false, file('report.png'), true)
    await vi.waitFor(() => expect(showModal).toHaveBeenCalledOnce())
    const restored = runtime.restore(JSON.parse(JSON.stringify(task)))
    expect(restored.phase).toBe('ERROR')
    expect(restored.mediaId).toBe('private-image')
    answer({ confirm: false })
    await pending
  })

  it('obtains explicit consent after a denied OCR attempt and reuses the uploaded image for review', async () => {
    const settings = { healthDataShare: false, scienceUpdate: false, updatedAt: 'now' }
    let granted = false
    const request = vi.fn().mockImplementation(async (path, options) => {
      if (path === '/api/v1/settings' && options?.method === 'PUT') { granted = true; return { ...settings, healthDataShare: true } }
      if (path === '/api/v1/settings') return settings
      if (path.endsWith('/recognition')) {
        if (!granted) throw new ApiRequestError('未授权将健康报告发送至百度智能云 OCR', 'FORBIDDEN', [], 403)
        return report('体检中心', '4.5')
      }
      throw new Error('报告尚不能保存')
    })
    const showModal = vi.fn(({ success }) => success({ confirm: true }))
    vi.stubGlobal('uni', { showModal })
    const deps = dependencies(request)
    deps.uploadImage.mockResolvedValue({ mediaId: 'private-image' })
    const runtime = createAssistantTaskRuntime(deps), task = runtime.create({ operation: 'DOCUMENT_AUTO', parameters: {} })
    await runtime.prepare(task, false, file('report.png'), true)
    expect(task.phase).toBe('CONFIRM')
    expect(task.operation).toBe('REPORT_IMPORT')
    expect(task.error).toBeUndefined()
    expect(deps.uploadImage).toHaveBeenCalledOnce()
    expect(showModal).toHaveBeenCalledOnce()
    expect(request.mock.calls.filter(([path]) => path.endsWith('/recognition'))).toHaveLength(2)
    expect(request.mock.calls.some(([path]) => path === '/api/v1/health-reports')).toBe(false)
  })

  it('keeps manual entry and the same image when external processing is declined', async () => {
    const request = vi.fn().mockImplementation(async path => {
      if (path === '/api/v1/settings') return { healthDataShare: false }
      throw new ApiRequestError('未授权', 'FORBIDDEN', [], 403)
    })
    vi.stubGlobal('uni', { showModal: vi.fn(({ success }) => success({ confirm: false })) })
    const deps = dependencies(request)
    deps.uploadImage.mockResolvedValue({ mediaId: 'private-image' })
    const runtime = createAssistantTaskRuntime(deps), task = runtime.create({ operation: 'REPORT_IMPORT', parameters: {} })
    await runtime.prepare(task, false, file('report.png'), true)
    expect(task.phase).toBe('INPUT')
    expect(task.mediaId).toBe('private-image')
    expect(task.summary).toContain('手动填写')
    expect(task.error).toBeUndefined()
    expect(request.mock.calls.filter(([path]) => path.endsWith('/recognition'))).toHaveLength(1)
    expect(request.mock.calls.some(([, options]) => options?.method === 'PUT')).toBe(false)
  })

  it('requires a new review if recognition is retried after clearing the previous indicators', async () => {
    const deps = dependencies(vi.fn().mockResolvedValue(report('体检中心', '5.0')))
    const runtime = createAssistantTaskRuntime(deps), task = runtime.create({ operation: 'REPORT_IMPORT', parameters: { hospital: '旧机构', checkupDate: '2026-10-22', indicators: ' ' } })
    task.mediaId = 'private-image'
    task.phase = 'CONFIRM'
    await runtime.prepare(task, true)
    expect(task.phase).toBe('CONFIRM')
    expect(task.parameters.hospital).toBe('体检中心')
    expect(deps.request).toHaveBeenCalledOnce()
    expect(deps.request.mock.calls[0][0]).toBe('/api/v1/health-reports/recognition')
  })

  it.each([
    ['血糖 4.5 mmol/L 3.9-6.1', 'mmol/L', '3.9-6.1', false],
    ['酶活性 20 U/L 0-40', 'U/L', '0-40', false],
    ['血红蛋白 140 g/L 120-160', 'g/L', '120-160', false],
    ['肺活量 3.5 L -', 'L', undefined, false],
    ['血糖 7.1 mmol/L 3.9-6.1 H', 'mmol/L', '3.9-6.1', true],
    ['血糖 3.0 mmol/L L', 'mmol/L', undefined, true],
    ['血糖 7.1↑ mmol/L 3.9-6.1', 'mmol/L', '3.9-6.1', true],
    ['血糖 3.0↓ mmol/L -', 'mmol/L', undefined, true],
    ['血糖 7.1 mmol/L - 异常', 'mmol/L', undefined, true],
  ])('preserves units and explicit flags in %s', async (line, unit, referenceRange, abnormal) => {
    const request = vi.fn().mockResolvedValue({ id: 'report' })
    const runtime = createAssistantTaskRuntime(dependencies(request))
    const task = runtime.create({ operation: 'REPORT_IMPORT', parameters: { hospital: '体检中心', checkupDate: '2026-10-22', indicators: line as string } })
    await runtime.prepare(task)
    expect(task.phase).toBe('CONFIRM'); expect(request).not.toHaveBeenCalled()
    await runtime.prepare(task, true)
    expect(task.phase).toBe('DONE')
    expect(request.mock.calls[0][1].data.indicators[0]).toMatchObject({ unit, referenceRange, abnormal })
  })

  it('recognizes a replacement image and saves only its new data after reconfirmation', async () => {
    const deps = dependencies()
    deps.uploadImage.mockResolvedValueOnce({ mediaId: 'old' }).mockResolvedValueOnce({ mediaId: 'new' })
    deps.request.mockResolvedValueOnce(report('旧医院', '4.5'))
      .mockRejectedValueOnce(new ApiRequestError('请重新核对报告', 'VALIDATION_FAILED', [], 400))
      .mockResolvedValueOnce(report('新医院', '5.0')).mockResolvedValueOnce({ id: 'saved' })
    const runtime = createAssistantTaskRuntime(deps)
    const task = runtime.create({ operation: 'REPORT_IMPORT', parameters: {} })
    await runtime.prepare(task, false, file('old.png'), true)
    await runtime.prepare(task, true)
    expect(task.phase).toBe('ERROR')
    await runtime.prepare(task, true, file('new.png'), true)
    expect(task.phase).toBe('CONFIRM')
    expect(deps.request.mock.calls[2]).toMatchObject(['/api/v1/health-reports/recognition', { data: { mediaId: 'new' } }])
    expect(deps.request).toHaveBeenCalledTimes(3)
    await runtime.prepare(task, true)
    expect(task.phase).toBe('DONE')
    expect(deps.request.mock.calls[3][1].data).toMatchObject({ hospital: '新医院', sourceMediaId: 'new', indicators: [{ value: '5.0', unit: 'mmol/L', abnormal: false }] })
  })

  it('drops the previous image source and extracted fields when replaced with a document', async () => {
    const deps = dependencies()
    deps.uploadImage.mockResolvedValue({ mediaId: 'image' })
    deps.upload.mockResolvedValue(document('new.txt', '新医院\n2026-10-21\n血糖 5.0 mmol/L'))
    deps.request.mockResolvedValueOnce(report('旧医院', '4.5'))
      .mockRejectedValueOnce(new ApiRequestError('请重新核对报告', 'VALIDATION_FAILED', [], 400))
      .mockResolvedValueOnce({ kind: 'REPORT', summary: '报告', report: { ...report('新医院','5.0'), checkupDate:'2026-10-21' } }).mockResolvedValueOnce({ id: 'saved' })
    const runtime = createAssistantTaskRuntime(deps)
    const task = runtime.create({ operation: 'REPORT_IMPORT', parameters: {} })
    await runtime.prepare(task, false, file('old.png'), true)
    await runtime.prepare(task, true)
    await runtime.prepare(task, false, file('new.txt'))
    expect(task.phase).toBe('CONFIRM'); expect(task.mediaId).toBeUndefined()
    expect(task.parameters).toMatchObject({ hospital: '新医院', checkupDate: '2026-10-21' });expect(task.parameters.indicators).toContain('血糖 5.0 mmol/L')
    await runtime.prepare(task, true)
    expect(deps.request.mock.calls[3][1].data).toMatchObject({ hospital: '新医院', sourceMediaId: undefined, indicators: [{ value: '5.0' }] })
  })

  it('drops a previous document when switching to an image, including on a later retry', async () => {
    const deps = dependencies()
    deps.upload.mockResolvedValue(document('old.txt', '旧医院\n2026-10-01\n血糖 4.5 mmol/L'))
    deps.uploadImage.mockResolvedValue({ mediaId: 'new-image' })
    deps.request.mockResolvedValueOnce({kind:'REPORT',summary:'报告',report:report('旧医院','4.5')}).mockRejectedValueOnce(new ApiRequestError('请重新核对报告', 'VALIDATION_FAILED', [], 400))
      .mockRejectedValueOnce(new ApiRequestError('识别连接中断', 'NETWORK_ERROR'))
      .mockResolvedValueOnce(report('新医院', '5.0')).mockResolvedValueOnce({ id: 'saved' })
    const runtime = createAssistantTaskRuntime(deps)
    const task = runtime.create({ operation: 'REPORT_IMPORT', parameters: {} })
    await runtime.prepare(task, false, file('old.txt'))
    await runtime.prepare(task, true)
    await runtime.prepare(task, false, file('new.png'), true)
    expect(task.phase).toBe('ERROR'); expect(task.document).toBeUndefined()
    expect(task.parameters.indicators).toBeUndefined()
    await runtime.prepare(task)
    expect(task.phase).toBe('CONFIRM')
    task.parameters.hospital = '用户核对后的医院'
    await runtime.prepare(task, true)
    expect(task.phase).toBe('DONE'); expect(deps.uploadImage).toHaveBeenCalledOnce()
    expect(deps.request.mock.calls[4][1].data).toMatchObject({ hospital: '用户核对后的医院', sourceMediaId: 'new-image', indicators: [{ value: '5.0' }] })
  })

  it('keeps the old source if uploading its replacement fails', async () => {
    const deps = dependencies()
    deps.uploadImage.mockResolvedValueOnce({ mediaId: 'old' })
      .mockRejectedValueOnce(new ApiRequestError('文件无效', 'VALIDATION_FAILED', [], 400))
    deps.request.mockResolvedValueOnce(report('旧医院', '4.5'))
    const runtime = createAssistantTaskRuntime(deps)
    const task = runtime.create({ operation: 'REPORT_IMPORT', parameters: {} })
    await runtime.prepare(task, false, file('old.png'), true)
    task.phase = 'ERROR'
    await runtime.prepare(task, false, file('new.png'), true)
    expect(task.phase).toBe('ERROR'); expect(task.fileName).toBe('old.png')
    expect(task.mediaId).toBe('old'); expect(task.parameters.hospital).toBe('旧医院')
  })

  it('replaces an oversized science draft with the new document while retaining its category', async () => {
    const deps = dependencies()
    deps.upload.mockResolvedValueOnce(document('old.txt', '文'.repeat(2001)))
      .mockResolvedValueOnce(document('new.txt', '新的投稿内容'))
    deps.request.mockResolvedValue({ id: 'submission', title: 'new' })
    const runtime = createAssistantTaskRuntime(deps)
    const task = runtime.create({ operation: 'SCIENCE_SUBMIT', parameters: { category: 'device' } })
    await runtime.prepare(task, false, file('old.txt'))
    expect(task.phase).toBe('ERROR')
    await runtime.prepare(task, false, file('new.txt'))
    expect(task.phase).toBe('DONE')
    expect(deps.request.mock.calls[0][1].data).toEqual({ title: 'new', category: 'device', content: '新的投稿内容' })
  })
})
