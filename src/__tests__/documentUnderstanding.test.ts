import { describe, expect, it, vi } from 'vitest'
import { createAssistantSession } from '../composables/useAssistantSession'
import { createAssistantTaskRuntime } from '../utils/assistantTasks'
import { ApiRequestError } from '../api/http'

const document = { id: 'file', name: '期末复习代码.docx', text: 'binarySearch(left,right) 使用中间位置缩小边界', sizeBytes: 200 }
const analysis = { kind: 'DOCUMENT', summary: '这份代码介绍二分查找，重点是边界更新。', executionMode: 'SPRING_AI', truncated: false }
function setup(request = vi.fn().mockResolvedValue(analysis)) {
  const upload = vi.fn().mockResolvedValue(document)
  const runtime = createAssistantTaskRuntime({ request, upload, uploadImage: vi.fn(), identity: () => 'owner' })
  return { runtime, request, upload }
}

describe('document understanding workflow', () => {
  it('reads an ordinary document immediately after upload and keeps the original available', async () => {
    const { runtime, upload, request } = setup(), task = runtime.create({ operation: 'DOCUMENT_AUTO', parameters: {} })
    await runtime.prepare(task, false, { path: 'fixture', name: document.name, size: 200 })
    expect(task.phase).toBe('DONE')
    expect(task.operation).toBe('DOCUMENT_UPLOAD')
    expect(task.analysis?.summary).toContain('二分查找')
    expect(task.context?.documentId).toBe('file')
    expect(upload).toHaveBeenCalledOnce()
    expect(request).toHaveBeenCalledWith('/api/v1/assistant/documents/file/analysis', expect.objectContaining({ data: { useModel: true } }))
  })

  it('uses the same owned file for follow-up questions, including the current model preference', async () => {
    const request = vi.fn().mockImplementation(async path => path.endsWith('/analysis') ? analysis : [document])
    const { runtime, upload } = setup(request)
    const ask = vi.fn().mockResolvedValue({ reply: '我来阅读', executionMode: 'SPRING_AI', actions: [], steps: [], tasks: [{ operation: 'DOCUMENT_ANALYZE', parameters: { instruction: '解释左边界变化' } }] })
    const session = createAssistantSession({ ask, identity: () => 'owner' }, runtime)
    session.useModel.value = false
    await session.attachFile('DOCUMENT_AUTO', { path: 'fixture', name: document.name, size: 200 })
    await session.submit('解释刚才文档的左边界变化')
    expect(ask.mock.calls[0][2]).toMatchObject({ documentId: 'file' })
    expect(session.entries.value.at(-1)!.tasks![0].phase).toBe('DONE')
    expect(upload).toHaveBeenCalledOnce()
    expect(request.mock.calls.at(-1)![1].data).toEqual({ instruction: '解释刚才文档的左边界变化', useModel: false })
  })

  it('auto-routes an anonymously named report without guessing the missing date', async () => {
    const { runtime } = setup(vi.fn().mockResolvedValue({ kind: 'REPORT', summary: '检查报告', report: { hospital: '体检中心', checkupDate: null, indicators: [{ name: '血糖', value: '95', unit: 'mg/dL', abnormal: false }] } }))
    const task = runtime.create({ operation: 'DOCUMENT_AUTO', parameters: {} })
    await runtime.prepare(task, false, { path: 'fixture', name: '附件.docx', size: 200 })
    expect(task.operation).toBe('REPORT_IMPORT')
    expect(task.phase).toBe('INPUT')
    expect(task.parameters.checkupDate).toBe('')
    expect(task.parameters.indicators).toContain('95 mg/dL')
  })

  it('preserves the user goal when a planned upload continues after file selection', async () => {
    const { runtime, request } = setup()
    const ask = vi.fn().mockResolvedValue({ reply: '选择文件后继续', executionMode: 'SPRING_AI', actions: [], steps: [], tasks: [{ operation: 'DOCUMENT_AUTO', parameters: {} }] })
    const session = createAssistantSession({ ask, identity: () => 'owner' }, runtime)
    await session.submit('上传文档并按里面的代码整理复习提纲')
    const task = session.entries.value.at(-1)!.tasks![0]
    expect(task.phase).toBe('FILE')
    await session.continueTask(session.entries.value.at(-1)!, task, false, { path: 'fixture', name: document.name, size: 200 })
    expect(request).toHaveBeenCalledWith('/api/v1/assistant/documents/file/analysis', expect.objectContaining({ data: { instruction: '上传文档并按里面的代码整理复习提纲', useModel: true } }))
  })

  it('retries a failed analysis without another upload or treating it as an uncertain write', async () => {
    const { runtime, upload } = setup(vi.fn().mockRejectedValueOnce(new ApiRequestError('网络中断', 'NETWORK_ERROR')).mockResolvedValueOnce(analysis))
    const task = runtime.create({ operation: 'DOCUMENT_AUTO', parameters: {} })
    await runtime.prepare(task, false, { path: 'fixture', name: document.name, size: 200 })
    expect(task.phase).toBe('ERROR')
    await runtime.prepare(task)
    expect(task.phase).toBe('DONE')
    expect(upload).toHaveBeenCalledOnce()
  })
})
