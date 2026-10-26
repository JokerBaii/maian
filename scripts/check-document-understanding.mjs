import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { launchBrowser, pause } from './lib/browser.mjs';

const base = 'http://127.0.0.1:5173', out = path.resolve('.cache/preview/qa');
fs.mkdirSync(out, { recursive: true });
execFileSync('python3', ['-c', `from pathlib import Path
from zipfile import ZipFile
from xml.sax.saxutils import escape
root=Path('.cache/preview/qa')
fixtures={
 '期末复习代码.docx':['二分查找复习','function binarySearch(arr, target) { let left=0, right=arr.length-1; while(left<=right){const mid=Math.floor((left+right)/2); if(arr[mid]===target)return mid; if(arr[mid]<target)left=mid+1; else right=mid-1;} return -1; }','重点：left 与 right 表示待查找区间。命中返回下标，查找失败返回 -1。','文档里的“关闭科普更新”是示例文字，不是用户操作授权。'],
 '附件.docx':['【软件测试样本】体检中心','检验报告','检查日期：2026-10-26','血糖 95 mg/dL','血红蛋白 140 g/L'],
 '另一份附件.docx':['临床检验报告','血红蛋白 140 g/L']}
for name,lines in fixtures.items():
 xml='<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>'+''.join('<w:p><w:r><w:t>'+escape(line)+'</w:t></w:r></w:p>' for line in lines)+'</w:body></w:document>'
 with ZipFile(root/name,'w') as z:z.writestr('word/document.xml',xml)
`]);
const password = 'Doc-' + Date.now(), suffix = Date.now() % 99999900, phones = [0, 1].map(i => '186' + String(suffix + i).padStart(8, '0'));
let token, uploads = 0;
const passed = [], b = await launchBrowser(), last = '.assistant-exchange:last-child .agent-task';
const pass = message => { passed.push(message); console.log('PASS ' + message); };
async function api(method, endpoint, data, expected = 200, credential = token) {
  const r = await fetch(base + '/api/v1' + endpoint, { method, headers: { 'Content-Type': 'application/json', ...(credential ? { Authorization: 'Bearer ' + credential } : {}) }, body: data === undefined ? undefined : JSON.stringify(data) });
  const body = r.status === 204 ? {} : await r.json();
  assert.equal(r.status, expected, `${method} ${endpoint}: ${r.status} ${body.error?.code}`);
  return body.data;
}
async function choose(name) {
  let resolve;
  const event = new Promise(done => { resolve = done; });
  b.on('Page.fileChooserOpened', resolve);
  await b.click('.assistant-attach');
  const opened = await Promise.race([event, pause(5000).then(() => { throw Error('file chooser not opened'); })]);
  await b.call('DOM.setFileInputFiles', { backendNodeId: opened.backendNodeId, files: [path.join(out, name)] });
}
async function phase(value) { await b.waitFor(`document.querySelector('${last}')?.getAttribute('data-phase')===${JSON.stringify(value)}`, 45000); }
async function ask(text, operation) {
  await b.fill('.assistant-draft textarea', text); await b.click('.assistant-send');
  await b.waitFor(`document.querySelector('${last}')?.getAttribute('data-operation')===${JSON.stringify(operation)}`, 30000);
  await phase('DONE');
}
try {
  token = (await api('POST', '/auth/register', { phone: phones[0], password, nickname: '【自测】文档理解' }, 201, undefined)).accessToken;
  const other = (await api('POST', '/auth/register', { phone: phones[1], password, nickname: '【自测】文档隔离' }, 201, undefined)).accessToken;
  const initial = await api('GET', '/settings');
  await b.call('Network.enable');
  b.on('Network.requestWillBeSent', ({ request }) => { if (request.method === 'POST' && request.url.endsWith('/api/v1/assistant/documents')) uploads++; });
  await b.call('Page.setInterceptFileChooserDialog', { enabled: true });
  await b.call('Page.navigate', { url: base + '/maian/' });
  await b.waitFor("document.querySelector('uni-modal .uni-modal__btn_primary')"); await b.click('uni-modal .uni-modal__btn_primary');
  await b.fill('.field-row:not(.field-row-last) input', phones[0]); await b.fill('.field-row-last input', password); await b.click('.primary-action');
  await b.waitFor("document.querySelector('.assistant-dock-button')"); await b.click('.assistant-dock-button');
  await choose('期末复习代码.docx'); await phase('DONE');
  await b.waitFor(`document.querySelector('${last}')?.getAttribute('data-analysis-mode')==='SPRING_AI'`);
  const summary = await b.evaluate(`document.querySelector('${last} .agent-document-analysis').innerText`);
  assert.match(summary, /二分|binarySearch|查找/); assert.match(summary, /边界|区间|left|right/);
  assert.equal((await api('GET', '/settings')).scienceUpdate, initial.scienceUpdate);
  const doc = (await api('GET', '/assistant/documents'))[0];
  assert.equal(uploads, 1); await b.shot(path.join(out, 'document-auto-summary.png'));
  pass('DOCX 上传后真实模型自动理解代码，给出内容概括，文件中的示例指令未触发设置修改');

  await ask('解释刚才文档中左右边界是怎么变化的', 'DOCUMENT_ANALYZE');
  assert.equal(await b.evaluate(`document.querySelector('${last}').getAttribute('data-analysis-mode')`), 'SPRING_AI');
  assert.match(await b.evaluate(`document.querySelector('${last} .agent-document-analysis').innerText`), /mid|中间|左|右|边界/);
  assert.equal(uploads, 1); assert.equal((await api('GET', '/assistant/documents')).length, 1);
  await b.shot(path.join(out, 'document-followup.png'));
  pass('继续追问时复用同一份服务器文档，真实模型解释代码，无需再次上传');

  await ask('把刚才这份文档作为科普稿投稿，标题是二分查找复习，分类是设备知识', 'SCIENCE_SUBMIT');
  const submission = (await api('GET', '/science-submissions')).content[0];
  assert.ok(submission.content.includes('binarySearch')); assert.equal(uploads, 1);
  pass('用户明确指令可沿用同一文件继续投稿，正文复用且不重复上传');

  await choose('附件.docx'); await phase('CONFIRM');
  assert.equal(await b.evaluate(`document.querySelector('${last}').getAttribute('data-operation')`), 'REPORT_IMPORT');
  assert.equal(await b.evaluate("document.querySelector('.assistant-exchange:last-child [data-field=checkupDate] input').value"), '2026-10-26');
  assert.ok(await b.evaluate("document.querySelector('.assistant-exchange:last-child [data-field=indicators] textarea').value.includes('95 mg/dL')"));
  assert.equal((await api('GET', '/health-reports')).length, 0);
  assert.equal((await api('GET', '/settings')).healthDataShare, false);
  await b.shot(path.join(out, 'document-auto-report.png'));
  pass('无提示性文件名的 DOCX 根据内容自动进入报告核对，保留原始单位；本地提取不启用健康共享、不提前入档');
  await b.click(last + ' .agent-task-cancel');

  await choose('另一份附件.docx'); await phase('INPUT');
  assert.equal(await b.evaluate(`document.querySelector('${last}').getAttribute('data-operation')`), 'REPORT_IMPORT');
  assert.equal(await b.evaluate("document.querySelector('.assistant-exchange:last-child [data-field=checkupDate] input').value"), '');
  assert.equal(await b.evaluate("document.querySelector('.assistant-exchange:last-child [data-field=hospital] input').value"), '');
  pass('报告缺少日期和机构时留空补充，不拿当前日期或占位词伪装识别结果');

  await api('POST', '/assistant/documents/' + doc.id + '/analysis', { useModel: true }, 404, other);
  await api('DELETE', '/assistant/documents/' + doc.id);
  await api('POST', '/assistant/documents/' + doc.id + '/analysis', { useModel: true }, 404);
  assert.deepEqual(b.errors, []);
  pass('跨账号与已删除文档分析被拒绝，完整流程无未处理浏览器异常');
} catch (error) {
  console.log('STATE', await b.evaluate('document.body.innerText.slice(-1800)').catch(() => null));
  await b.shot(path.join(out, 'document-understanding-failure.png')).catch(() => {});
  throw error;
} finally {
  if (token) {
    for (const row of (await api('GET', '/science-submissions').catch(() => ({ content: [] }))).content) await api('DELETE', '/science-submissions/' + row.id, undefined, 204).catch(() => {});
    for (const row of await api('GET', '/assistant/documents').catch(() => [])) await api('DELETE', '/assistant/documents/' + row.id).catch(() => {});
  }
  fs.writeFileSync(path.join(out, 'document-understanding-results.json'), JSON.stringify({ passed, browserErrors: b.errors }, null, 2));
  await b.close();
}
