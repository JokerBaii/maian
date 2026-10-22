import type { AgentDocument, AgentFile } from '@/api/agentDocuments'
import { TASK_DEFINITIONS, type TaskEnvironment, type TaskRun } from './catalog'

interface FileEnvironment extends Pick<TaskEnvironment, 'api'> {
  upload: (file: AgentFile) => Promise<AgentDocument>
  uploadImage: (path: string) => Promise<{ mediaId: string }>
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
      run.document = undefined; run.mediaId = undefined
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
      const text = run.document.text || ''
      if (op === 'DOCUMENT_AUTO') {
        const extracted: Record<string,string> = {}; fillReport(extracted, text)
        if (extracted.checkupDate && extracted.indicators) {
          op = run.operation = 'REPORT_IMPORT'; run.title = TASK_DEFINITIONS[op].title; run.autoRouted = true
          Object.assign(p, extracted); run.steps.push('识别到报告日期和指标，整理为报告核对任务；尚未写入健康档案')
        } else { op = run.operation = 'DOCUMENT_UPLOAD'; run.title = TASK_DEFINITIONS[op].title }
      }
    }
  }
  if (run.document) {
    if (op === 'SCIENCE_SUBMIT') { p.title ||= (file?.name || run.document.name).replace(/\.[^.]+$/, '').slice(0,50); p.content ||= run.document.text || '' }
    if (op === 'REPORT_IMPORT') fillReport(p,run.document.text || '')
  }
  if (run.mediaId && !p.indicators) {
    run.fields = TASK_DEFINITIONS.REPORT_IMPORT.fields!.map(f => ({ ...f }))
    const recognized = await env.api('/api/v1/health-reports/recognition', 'POST', { mediaId: run.mediaId })
    p.hospital = recognized.hospital; p.checkupDate = recognized.checkupDate
    p.indicators = recognized.indicators.map((i: any) => `${i.name} ${i.value} ${i.unit || '-'} ${i.referenceRange || '-'} ${i.abnormal ? '异常' : ''}`).join('\n')
  }
  if (['DOCUMENT_UPLOAD', 'DOCUMENT_AUTO', 'SCIENCE_SUBMIT', 'REPORT_IMPORT'].includes(op) && !run.document && !run.mediaId && !p.content && !p.indicators) {
    run.phase = 'FILE'; run.summary = '选择文件后继续办理，也可以补充下面的业务信息。'
    if (!['DOCUMENT_UPLOAD','DOCUMENT_AUTO'].includes(op)) run.fields = TASK_DEFINITIONS[op].fields!.map(f => ({ ...f }))
    return false
  }
  return true
}

function fillReport(parameters: Record<string, string>, text: string) {
  const lines = text.split(/\r?\n/).map(l => l.trim()).filter(Boolean)
  const date = text.match(/(20\d{2})[-/.年](\d{1,2})[-/.月](\d{1,2})/)
  if (date) parameters.checkupDate ||= `${date[1]}-${date[2].padStart(2, '0')}-${date[3].padStart(2, '0')}`
  parameters.hospital ||= lines.find(l => /医院|体检中心|健康管理中心/.test(l)) || ''
  // Extract literal values only. Reference ranges and abnormal flags are never invented.
  parameters.indicators ||= lines.filter(l => /^(血压|空腹血糖|血糖|总胆固醇|低密度脂蛋白|高密度脂蛋白|甘油三酯|血红蛋白|静息心率|心率|体质指数|BMI)\s*[:：\s]\s*\d/.test(l)).map(l => l.replace(/[:：]/g, ' ').trim()).join('\n')
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
