import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { launchBrowser, pause } from './lib/browser.mjs';

const base = 'http://127.0.0.1:5173', out = path.resolve('.cache/preview/qa');
const phone = '188' + String(Date.now() % 100000000).padStart(8, '0'), password = 'Consent-' + Date.now();
let token, uploads = 0, settingsWrites = 0, recognized = 0;
const passed = [], interceptionErrors = [];
async function api(method, endpoint, data) {
  const response = await fetch(base + '/api/v1' + endpoint, { method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) }, body: data === undefined ? undefined : JSON.stringify(data) });
  const body = response.status === 204 ? {} : await response.json();
  assert.ok(response.ok, `${method} ${endpoint}: ${response.status}`);
  return body.data;
}
const b = await launchBrowser(), last = '.assistant-exchange:last-child .agent-task';
const pass = message => { passed.push(message); console.log('PASS ' + message); };
try {
  token = (await api('POST', '/auth/register', { phone, password, nickname: '【自测】报告授权' })).accessToken;
  assert.equal((await api('GET', '/settings')).healthDataShare, false);
  await b.call('Network.enable');
  b.on('Network.requestWillBeSent', ({ request }) => {
    if (request.method === 'POST' && request.url.includes('/api/v1/media?')) uploads++;
    if (request.method === 'PUT' && request.url.endsWith('/api/v1/settings')) settingsWrites++;
  });
  // Exercise the actual consent guard. Only the authorized OCR provider result is simulated.
  await b.call('Fetch.enable', { patterns: [{ urlPattern: '*/api/v1/health-reports/recognition', requestStage: 'Request' }] });
  b.on('Fetch.requestPaused', async event => {
    try {
      if (!(await api('GET', '/settings')).healthDataShare) {
        await b.call('Fetch.continueRequest', { requestId: event.requestId });
        return;
      }
      recognized++;
      const data = { hospital: '【自测】体检中心', checkupDate: '2026-10-26', indicators: [{ name: '血糖', value: '5.0', unit: 'mmol/L', abnormal: false }], rawLines: [] };
      await b.call('Fetch.fulfillRequest', { requestId: event.requestId, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'application/json' }], body: Buffer.from(JSON.stringify({ success: true, data })).toString('base64') });
    } catch (error) { interceptionErrors.push(error.message); await b.call('Fetch.continueRequest', { requestId: event.requestId }).catch(() => {}); }
  });
  await b.call('Page.navigate', { url: base + '/maian/' });
  await b.waitFor("document.querySelector('uni-modal .uni-modal__btn_primary')");
  await b.click('uni-modal .uni-modal__btn_primary');
  await b.fill('.field-row:not(.field-row-last) input', phone);
  await b.fill('.field-row-last input', password);
  await b.click('.primary-action');
  await b.waitFor("document.querySelector('.assistant-dock-button')");
  await b.evaluate("uni.navigateTo({url:'/pages/mine/settings?section=privacy'})");
  await b.waitFor("document.querySelector('#settings-privacy .settings-item-last .app-switch')?.getAttribute('aria-checked')==='false'");
  await b.click('.assistant-dock-button');
  const image = fs.readFileSync('src/static/icons/tab-home.png').toString('base64');
  const paste = () => b.evaluate(`(()=>{const data=new DataTransfer(),bytes=Uint8Array.from(atob('${image}'),c=>c.charCodeAt(0));data.items.add(new File([bytes],'检查单.png',{type:'image/png'}));document.querySelector('.assistant-draft textarea').dispatchEvent(new ClipboardEvent('paste',{clipboardData:data,bubbles:true,cancelable:true}))})()`);
  await paste();
  await b.waitFor("document.querySelector('uni-modal')?.innerText.includes('百度智能云 OCR')");
  await b.click('uni-modal .uni-modal__btn_default');
  await b.waitFor(`document.querySelector('${last}')?.getAttribute('data-phase')==='INPUT'`);
  assert.equal((await api('GET', '/settings')).healthDataShare, false);
  assert.equal(recognized, 0);
  assert.equal(uploads, 1);
  assert.equal(settingsWrites, 0);
  assert.equal((await api('GET', '/health-reports')).length, 0);
  assert.ok((await b.evaluate(`document.querySelector('${last}').innerText`)).includes('手动填写'));
  assert.equal(await b.evaluate(`!!document.querySelector('${last} .agent-task-error')`), false);
  pass('首次识别说明真实用途；拒绝授权后不启用共享、不识别、不入档，保留图片与手动录入');

  await b.click(last + ' .agent-task-primary');
  await b.waitFor("document.querySelector('uni-modal')?.innerText.includes('百度智能云 OCR')");
  await b.click('uni-modal .uni-modal__btn_primary');
  await b.waitFor(`document.querySelector('${last}')?.getAttribute('data-phase')==='CONFIRM'`);
  assert.equal((await api('GET', '/settings')).healthDataShare, true);
  await b.waitFor("document.querySelector('#settings-privacy .settings-item-last .app-switch')?.getAttribute('aria-checked')==='true'");
  assert.equal(uploads, 1);
  assert.equal(settingsWrites, 1);
  assert.equal(recognized, 1);
  assert.equal((await api('GET', '/health-reports')).length, 0);
  assert.equal(await b.evaluate("document.querySelector('[data-field=indicators] textarea').value.includes('5.0 mmol/L')"), true);
  await b.shot(path.join(out, 'report-consent-review.png'));
  pass('就地授权后沿用同一张图片进入核对；不重复上传，也不跳过保存确认（OCR结果模拟）');

  await b.click('.assistant-overlay [aria-label="清空查找记录"]');
  await paste();
  await b.waitFor(`document.querySelector('${last}')?.getAttribute('data-phase')==='CONFIRM'`);
  assert.equal(settingsWrites, 1);
  assert.equal(recognized, 2);
  assert.equal(await b.evaluate(`!!(${b.element('uni-modal .uni-modal')})`), false);
  assert.equal((await api('GET', '/health-reports')).length, 0);
  assert.deepEqual(interceptionErrors, []);
  assert.deepEqual(b.errors, []);
  pass('已有授权时直接识别新图片，不再次弹窗；全程无未处理浏览器异常');
} catch (error) {
  console.log('STATE', await b.evaluate('document.body.innerText.slice(-1400)').catch(() => null));
  await b.shot(path.join(out, 'report-consent-failure.png')).catch(() => {});
  throw error;
} finally {
  fs.mkdirSync(out, { recursive: true });
  fs.writeFileSync(path.join(out, 'report-consent-results.json'), JSON.stringify({ passed, browserErrors: b.errors, interceptionErrors }, null, 2));
  await b.close();
}
