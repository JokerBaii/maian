import type { AgentDocument, AgentFile, DocumentAnalysis } from '@/api/agentDocuments'
import { ApiRequestError } from '@/api/http'
import { TASK_DEFINITIONS, type TaskEnvironment, type TaskRun } from './catalog'

interface FileEnvironment extends Pick<TaskEnvironment, 'api'> {
  upload: (file: AgentFile) => Promise<AgentDocument>
  uploadImage: (path: string) => Promise<{ mediaId: string }>
  authorizeReport: () => Promise<boolean>
}

/** Uploads are fenced by the runtime; extracted text never becomes an instruction. */
export async function prepareTaskFiles(run: TaskRun, env: FileEnvironment, file?: AgentFile, image = false) {
  const p = run.parameters
  let op = run.operation
  if (file) {
    if (file.size > 15 * 1024 * 1024) throw new Error('请选择不超过15MB的文件')
    // A new attachment replaces the old source. Retrying without a file keeps edits.
    const replaceAttachment = () => {
      if (run.document || run.mediaId) {
        const keys = op === 'REPORT_IMPORT' ? ['hospital', 'checkupDate', 'indicators'] : op === 'SCIENCE_SUBMIT' ? ['title', 'content'] : []
        for (const key of keys) delete p[key]
      }
      run.document = undefined; run.mediaId = undefined; run.analysis = undefined
      run.fileName = file.name; run.items = []; run.links = []
    }
    run.steps.push('开始上传你选择的文件')
    if (image) {
      if (!['REPORT_IMPORT','DOCUMENT_AUTO'].includes(op)) throw new Error('检查单图片请选择体检报告任务')
      if (op === 'DOCUMENT_AUTO') { op = run.operation = 'REPORT_IMPORT'; run.title = TASK_DEFINITIONS[op].title; run.autoRouted = true }
      // The existing recognition endpoint enforces the account's health-processing consent.
      const uploaded = await env.uploadImage(file.path)
      replaceAttachment(); run.mediaId = uploaded.mediaId
    } else {
      const uploaded = await env.upload(file)
      replaceAttachment(); run.document = uploaded
      run.steps.push('文件已保存，已读取可提取的文档文字')
    }
  }
  if (run.document) {
    run.context = { ...run.context, documentId: run.document.id }
    run.output = { ...run.output, documentId: run.document.id }
    if (op === 'SCIENCE_SUBMIT') { p.title ||= (file?.name || run.document.name).replace(/\.[^.]+$/, '').slice(0,50); p.content ||= run.document.text || '' }
    if ((op === 'DOCUMENT_AUTO' || (op === 'REPORT_IMPORT' && !p.indicators?.trim())) && !run.analysis) {
      run.summary = '正在阅读文件，识别内容与用途…'
      const analysis: DocumentAnalysis = await env.api('/api/v1/assistant/documents/' + encodeURIComponent(run.document.id) + '/analysis', 'POST', { instruction: p.instruction, useModel: run.useModel !== false })
      run.analysis = analysis
      run.steps.push(analysis.kind === 'REPORT' ? '根据文档内容识别为检查报告，已整理原始字段' : '已读取文档并生成阅读结果')
      if (op === 'DOCUMENT_AUTO') {
        op = run.operation = analysis.kind === 'REPORT' ? 'REPORT_IMPORT' : 'DOCUMENT_UPLOAD'
        run.title = TASK_DEFINITIONS[op].title
        run.autoRouted = true
      }
      const report = analysis.report
      if (op === 'REPORT_IMPORT' && report) {
        p.hospital ||= report.hospital || ''
        p.checkupDate ||= report.checkupDate || ''
        p.indicators ||= report.indicators.map(i => `${i.name} ${i.value} ${i.unit || '-'} ${i.referenceRange || '-'} ${i.abnormal ? '异常' : ''}`).join('\n')
      }
    }
  }
  if (run.mediaId && !p.indicators?.trim()) {
    run.fields = TASK_DEFINITIONS.REPORT_IMPORT.fields!.map(f => ({ ...f }))
    const recognize = () => env.api('/api/v1/health-reports/recognition', 'POST', { mediaId: run.mediaId })
    let recognized
    try { recognized = await recognize() }
    catch (error) {
      if (!(error instanceof ApiRequestError) || error.code !== 'FORBIDDEN' || error.statusCode !== 403) throw error
      if (!await env.authorizeReport()) {
        run.phase = 'INPUT'
        run.summary = '尚未进行文字识别。可手动填写报告，或点击“识别这张图片”重新授权。'
        return false
      }
      run.steps.push('已取得健康数据处理授权，继续识别已上传的图片')
      recognized = await recognize()
    }
    p.hospital = recognized.hospital || ''; p.checkupDate = recognized.checkupDate || ''
    p.indicators = recognized.indicators.map((i: any) => `${i.name} ${i.value} ${i.unit || '-'} ${i.referenceRange || '-'} ${i.abnormal ? '异常' : ''}`).join('\n')
  }
  if (['DOCUMENT_UPLOAD', 'DOCUMENT_AUTO', 'SCIENCE_SUBMIT', 'REPORT_IMPORT'].includes(op) && !run.document && !run.mediaId && !p.content && !p.indicators) {
    run.phase = 'FILE'; run.summary = '选择文件后继续办理，也可以补充下面的业务信息。'
    if (!['DOCUMENT_UPLOAD','DOCUMENT_AUTO'].includes(op)) run.fields = TASK_DEFINITIONS[op].fields!.map(f => ({ ...f }))
    return false
  }
  return true
}

export function parseReportIndicators(indicators: string) {
  return indicators.split('\n').filter(Boolean).map(line => {
    const parts = line.replace(/[↑↓]/g, ' ').trim().split(/\s+/)
    if (parts.length < 2) throw new Error('指标请按“名称 数值 单位”逐行填写')
    // Flags belong after the unit; an L inside mmol/L, U/L or the unit L is data.
    const isFlag = (part: string) => ['H', 'L', '异常'].includes(part)
    const metadata = parts.slice(3)
    const abnormal = /[↑↓]/.test(line) || metadata.some(isFlag)
    const referenceRange = metadata.filter(part => !isFlag(part))[0]
    return { name: parts[0], value: parts[1], unit: parts[2] === '-' ? undefined : parts[2], referenceRange: referenceRange === '-' ? undefined : referenceRange, abnormal }
  })
}
