import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { launchBrowser, pause } from './lib/browser.mjs';

const root = path.resolve(import.meta.dirname, '..');
const output = path.join(root, '.cache/preview/qa');
const base = 'http://127.0.0.1:5173';
const password = fs.readFileSync(path.join(root, '.cache/preview/password'), 'utf8');
const results = [];
const testPhone = '197' + String(Date.now() % 100000000).padStart(8, '0');
const registration = await fetch(base + '/api/v1/auth/register', {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({phone:testPhone,password,nickname:'【自测】悬浮助手'})});
assert.equal(registration.status,201);
const browser = await launchBrowser();
const visible = selector => `!!(${browser.element(selector)})`;
const screenshot = async name => { await pause(350); await browser.shot(path.join(output, name + '.png')); };
const pass = name => { results.push(name); console.log('PASS ' + name); };
async function route(url, method = 'navigateTo') {
  await browser.evaluate(`new Promise((resolve,reject)=>uni.${method}({url:${JSON.stringify(url)},success:()=>resolve(true),fail:reject}))`);
  await pause(250);
}
async function ask(message) {
  await browser.fill('.assistant-overlay .assistant-draft textarea', message);
  await browser.click('.assistant-overlay .assistant-send');
  await browser.waitFor("!document.querySelector('.assistant-overlay .assistant-searching')");
}
async function settingsSnapshot() {
  return await browser.evaluate(`fetch('/api/v1/settings',{headers:{Authorization:'Bearer '+uni.getStorageSync('maian_access_token')}}).then(r=>r.json()).then(r=>r.data)`);
}
try {
  await browser.call('Page.navigate', { url: base + '/maian/' });
  await browser.waitFor(visible('.primary-action'));
  await browser.waitFor(visible('uni-modal .uni-modal__btn_primary'));
  await browser.click('uni-modal .uni-modal__btn_primary');
  await browser.fill('.field-row:not(.field-row-last) input', testPhone);
  await browser.fill('.field-row-last input', password);
  await browser.click('.primary-action');
  await pause(700);
  await browser.waitFor(visible('.assistant-dock-button'));
  assert.match(await browser.evaluate('location.hash'), /pages\/index\/index/);
  await screenshot('assistant-home');
  pass('真实表单登录与首页悬浮形象');

  await browser.click('.assistant-hint-dismiss');
  await browser.call('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-reduced-motion', value: 'no-preference' }] });
  // Measure rendered movement after the greeting disappears, rather than only checking CSS names.
  const motion = await browser.evaluate(`new Promise(resolve=>{
    const dock=document.querySelector('.assistant-dock');
    const character=dock.querySelector('.mascot-character'),arm=dock.querySelector('.mascot-arm-right');
    const samples=[],start=performance.now();
    const timer=setInterval(()=>{
      const body=new DOMMatrixReadOnly(getComputedStyle(character).transform);
      const hand=new DOMMatrixReadOnly(getComputedStyle(arm).transform);
      samples.push({y:body.m42,angle:Math.atan2(hand.b,hand.a)*180/Math.PI,dockTop:dock.getBoundingClientRect().top});
      if(performance.now()-start>=6200){clearInterval(timer);resolve(samples)}
    },80);
  })`);
  const range = key => Math.max(...motion.map(sample => sample[key])) - Math.min(...motion.map(sample => sample[key]));
  assert.ok(range('y') >= 6, 'Idle float is too subtle after dismissing the greeting');
  assert.ok(range('angle') >= 50, 'Idle hand stopped waving after dismissing the greeting');
  assert.ok(range('dockTop') < 1, 'Animation moved the dock hit target');
  await browser.call('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-reduced-motion', value: 'reduce' }] });
  await pause(150);
  assert.equal(await browser.evaluate("document.querySelector('.assistant-dock .mascot').getAnimations({subtree:true}).filter(a=>a.playState==='running').length"), 0);
  await browser.call('Emulation.setEmulatedMedia', { features: [{ name: 'prefers-reduced-motion', value: 'no-preference' }] });
  await browser.waitFor("document.querySelector('.assistant-dock .mascot-character').getAnimations().some(a=>a.playState==='running')");
  pass('提示收起后持续浮动和周期挥手，点击区域稳定，减少动态偏好生效并可恢复');

  const before = await settingsSnapshot();
  await browser.click('.assistant-dock-button');
  await browser.waitFor(visible('.assistant-welcome'));
  await screenshot('assistant-welcome');
  // This suite checks navigation deterministically; the global-agent suite exercises the live model.
  await browser.click('.assistant-model-choice');
  await browser.click('.assistant-privacy .app-switch');
  await browser.click('.assistant-model-choice');
  await ask('隐私设置在哪里');
  await browser.waitFor("document.querySelector('.assistant-overlay .assistant-result-title')?.innerText==='隐私设置'");
  await browser.click('.assistant-trace-toggle');
  await browser.waitFor(visible('.assistant-trace'));
  await screenshot('assistant-settings-result');
  await browser.click('.assistant-result-card');
  await browser.waitFor(visible('#settings-privacy.assistant-highlight'));
  assert.equal(await browser.evaluate(visible('.assistant-overlay')), false);
  assert.deepEqual(await settingsSnapshot(), before);
  pass('助手查找隐私设置、展示真实过程、定位目标且不修改设置');

  await browser.click('.assistant-dock-button');
  await browser.waitFor(visible('.assistant-overlay'));
  assert.ok((await browser.evaluate("document.querySelector('.assistant-overlay').innerText")).includes('隐私设置在哪里'));
  await ask('心率阈值设置在哪里');
  await browser.waitFor("document.querySelectorAll('.assistant-overlay .assistant-result-title').length===2");
  await browser.evaluate("[...document.querySelectorAll('.assistant-overlay .assistant-result-card')].find(e=>e.innerText.includes('心率预警阈值')).scrollIntoView({block:'center'})");
  // Select the result associated with this question rather than the earlier privacy card.
  await browser.evaluate("[...document.querySelectorAll('.assistant-overlay .assistant-result-card')].find(e=>e.innerText.includes('心率预警阈值')).click()");
  await browser.waitFor(visible('#settings-thresholds.assistant-highlight'));
  pass('跨页面保留查找记录与心率阈值定位');

  await route('/pages/index/index', 'switchTab');
  await browser.waitFor(visible('.assistant-dock-button'));
  let point = await browser.evaluate(`(()=>{const r=(${browser.element('.assistant-dock-button')}).getBoundingClientRect();return {x:r.x+35,y:r.y+35}})()`);
  await browser.call('Input.dispatchMouseEvent', { type: 'mousePressed', ...point, button: 'left', clickCount: 1 });
  await browser.call('Input.dispatchMouseEvent', { type: 'mouseMoved', x: 40, y: point.y - 85, button: 'left', buttons: 1 });
  await browser.call('Input.dispatchMouseEvent', { type: 'mouseReleased', x: 40, y: point.y - 85, button: 'left', clickCount: 1 });
  await pause(500);
  assert.equal(await browser.evaluate(visible('.assistant-overlay')), false);
  assert.equal(Math.round(await browser.evaluate(`(${browser.element('.assistant-dock')}).getBoundingClientRect().left`)), 12);
  await browser.call('Emulation.setTouchEmulationEnabled', { enabled: true, maxTouchPoints: 1 });
  point = await browser.evaluate(`(()=>{const r=(${browser.element('.assistant-dock-button')}).getBoundingClientRect();return {x:r.x+35,y:r.y+35}})()`);
  await browser.call('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [{ ...point, id: 0 }] });
  await browser.call('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: [{ x: 345, y: point.y - 35, id: 0 }] });
  await browser.call('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
  await pause(500);
  assert.equal(await browser.evaluate(visible('.assistant-overlay')), false);
  assert.equal(Math.round(await browser.evaluate(`(${browser.element('.assistant-dock')}).getBoundingClientRect().left`)), 306);
  await browser.call('Emulation.setTouchEmulationEnabled', { enabled: false });
  await browser.click('.assistant-dock-button');
  await ask('推荐 AED 文章');
  await browser.waitFor(visible('.assistant-overlay .assistant-result-card[data-kind="ARTICLE"]'));
  assert.equal(await browser.evaluate("document.querySelector('.assistant-overlay').innerText.includes('【自测】AED 功能说明')"), false);
  await screenshot('assistant-articles');
  await browser.click('.assistant-overlay .assistant-result-card[data-kind="ARTICLE"]');
  await browser.waitFor("location.hash.includes('pages/science/detail')");
  await browser.waitFor("document.body.innerText.includes('【演示】')");
  pass('鼠标与触摸拖动吸边不误触，文章推荐打开真实内容');

  await route('/pages/index/index', 'switchTab');
  await browser.click('.assistant-dock-button');
  await ask('打开火星传送门');
  await browser.waitFor("document.querySelector('.assistant-overlay').innerText.includes('这次没能理解你的意思')");
  await browser.fill('.assistant-overlay .assistant-draft textarea', '   ');
  assert.equal(await browser.evaluate("document.querySelector('.assistant-overlay .assistant-send').getAttribute('disabled')!==null"), true);
  pass('无匹配结果提示与空白输入拦截');

  let injectFailure = true;
  browser.on('Fetch.requestPaused', request => {
    const work = injectFailure
      ? browser.call('Fetch.fulfillRequest', { requestId: request.requestId, responseCode: 503, responseHeaders: [{ name: 'Content-Type', value: 'application/json' }], body: Buffer.from(JSON.stringify({ success: false, error: { code: 'SERVICE_UNAVAILABLE', message: '服务暂时不可用', details: [] } })).toString('base64') })
      : browser.call('Fetch.continueRequest', { requestId: request.requestId });
    injectFailure = false;
    work.catch(error => browser.errors.push(error.message));
  });
  await browser.call('Fetch.enable', { patterns: [{ urlPattern: '*api/v1/assistant', requestStage: 'Request' }] });
  await ask('通知设置');
  await browser.waitFor(visible('.assistant-retry'));
  await browser.call('Fetch.disable');
  await browser.click('.assistant-retry');
  await browser.waitFor("[...document.querySelectorAll('.assistant-overlay .assistant-result-title')].some(e=>e.innerText==='通知设置')");
  pass('接口失败后展示错误并可真实重试成功');

  await browser.click('[aria-label="展开完整助手"]');
  await browser.waitFor("location.hash.includes('pages/assistant/index')");
  await browser.waitFor(visible('.assistant-page .assistant-panel'));
  assert.ok((await browser.evaluate("document.querySelector('.assistant-page').innerText")).includes('通知设置'));
  await screenshot('assistant-full-page');
  await browser.click('.assistant-page [aria-label="关闭助手"]');
  await browser.waitFor(visible('.assistant-dock-button'));
  pass('完整助手入口、历史保留与关闭返回');

  for (const page of ['health', 'mine', 'map', 'index']) {
    await route('/pages/' + page + '/index', 'switchTab');
    await browser.waitFor(visible('.assistant-dock-button'));
    assert.equal(await browser.evaluate("[...document.querySelectorAll('.assistant-dock-button')].filter(e=>e.getBoundingClientRect().width>0).length"), 1);
  }
  await route('/pages/device/manage');
  await browser.waitFor(visible('.empty-state'));
  await browser.waitFor(visible('.assistant-dock-button'));
  assert.equal(await browser.evaluate(visible('.coordination-entry')), false);
  await browser.click('.tab-item:nth-child(2)');
  assert.equal(await browser.evaluate("[...document.querySelectorAll('.assistant-dock-button')].filter(e=>e.getBoundingClientRect().width>0).length"), 1);
  pass('设备空列表无协同横幅，固定与移动分类均保留一个脉脉入口');

  await route('/pages/rescue/index');
  await browser.waitFor(visible('.sos-primary'));
  assert.equal(await browser.evaluate(visible('.assistant-dock-button')), false);
  pass('四个主页切换仅显示一个形象，呼救页不遮挡关键操作');

  await route('/pages/index/index', 'switchTab');
  await browser.call('Emulation.setDeviceMetricsOverride', { width: 1280, height: 900, deviceScaleFactor: 1, mobile: false });
  await pause(300);
  const dock = await browser.evaluate(`(${browser.element('.assistant-dock')}).getBoundingClientRect().toJSON()`);
  assert.ok(dock.left >= 425 && dock.right <= 855, 'Desktop character escaped the phone preview');
  await browser.click('.assistant-dock-button');
  await screenshot('assistant-desktop');
  const sheet = await browser.evaluate("document.querySelector('.assistant-sheet').getBoundingClientRect().toJSON()");
  assert.equal(Math.round(sheet.width), 320);
  assert.ok(sheet.height<=360,'Floating conversation grew beyond its compact limit');
  assert.equal(await browser.evaluate("getComputedStyle(document.querySelector('.assistant-overlay')).backdropFilter"),'none');
  assert.equal(await browser.evaluate("getComputedStyle(document.querySelector('.assistant-overlay')).pointerEvents"),'none');
  await browser.call('Emulation.setDeviceMetricsOverride', { width: 320, height: 568, deviceScaleFactor: 1, mobile: true });
  await pause(300);
  const send = await browser.evaluate("document.querySelector('.assistant-overlay .assistant-send').getBoundingClientRect().toJSON()");
  assert.ok(send.top > 0 && send.bottom < 568, 'Small-screen composer is outside viewport');
  await screenshot('assistant-small-screen');
  pass('桌面手机预览边界与小屏输入栏可见性');

  const markdown='**健康与安全**\n- 查看心率、健康监测数据、体检报告\n- 管理穿戴设备、个人资料\n\n**急救相关**\n- 查找附近AED\n\n### 办理步骤\n1. 说出需求\n2. 核对信息\n\n| 功能 | 状态 |\n| --- | --- |\n| 设置 | 可办理 |';
  browser.on('Fetch.requestPaused', request => {
    void browser.call('Fetch.fulfillRequest',{requestId:request.requestId,responseCode:200,responseHeaders:[{name:'Content-Type',value:'application/json'}],body:Buffer.from(JSON.stringify({success:true,data:{reply:markdown,executionMode:'SPRING_AI',actions:[],tasks:[],steps:[],conversation:true}})).toString('base64')}).catch(error=>browser.errors.push(error.message));
  });
  await browser.call('Fetch.enable',{patterns:[{urlPattern:'*api/v1/assistant',requestStage:'Request'}]});
  await ask('介绍一下功能');await browser.call('Fetch.disable');
  const reply="document.querySelector('.assistant-overlay .assistant-exchange:last-child .assistant-answer-copy')";
  await browser.waitFor(`${reply}?.querySelectorAll('strong').length===2`);
  assert.equal(await browser.evaluate(`${reply}.querySelectorAll('ul').length`),2);
  assert.equal(await browser.evaluate(`${reply}.querySelectorAll('li').length`),5);
  assert.equal(await browser.evaluate(`${reply}.querySelectorAll('table').length`),1);
  assert.ok(!(await browser.evaluate(`${reply}.innerText`)).includes('**'));
  assert.ok(await browser.evaluate(`${reply}.scrollWidth<=${reply}.clientWidth+1`),'Markdown overflows the compact panel');
  await screenshot('assistant-markdown-small');
  await browser.call('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:true});await pause(200);
  assert.ok(await browser.evaluate(`${reply}.scrollWidth<=${reply}.clientWidth+1`));
  await screenshot('assistant-markdown-reply');
  pass('Markdown回复正确呈现加粗、标题、列表和表格，小屏与手机宽度不横向溢出（固定回复测试渲染）');


  await browser.click('.assistant-overlay [aria-label="关闭助手"]');
  await browser.call('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true });
  await route('/pages/mine/settings');
  await browser.evaluate("[...document.querySelectorAll('.logout-action')].find(e=>e.getBoundingClientRect().width>0)?.scrollIntoView({block:'center'})");
  await browser.click('.logout-action');
  await browser.waitFor(visible('uni-modal .uni-modal__btn_primary'));
  await browser.click('uni-modal .uni-modal__btn_primary');
  await browser.waitFor(visible('.primary-action'));
  await browser.fill('.field-row:not(.field-row-last) input', '19900000001');
  await browser.fill('.field-row-last input', password);
  await browser.click('.primary-action');
  await browser.waitFor("location.hash.includes('pages/index/index')");
  await route('/pages/device/manage');
  await browser.waitFor("[...document.querySelectorAll('.device-card')].filter(e=>e.getBoundingClientRect().width>0).length>=2");
  assert.equal(await browser.evaluate("[...document.querySelectorAll('.assistant-dock-button')].filter(e=>e.getBoundingClientRect().width>0).length"), 1);
  assert.equal(await browser.evaluate(visible('.coordination-entry')), false);
  await screenshot('assistant-devices');
  await browser.click('.device-card');
  await browser.waitFor(visible('.app-sheet-panel'));
  assert.equal(await browser.evaluate(visible('.assistant-dock-button')), false);
  await browser.click('.app-sheet-action');
  await browser.waitFor("location.hash.includes('pages/agent/index')");
  await browser.waitFor(visible('.assistant-dock-button'));
  pass('多设备列表只显示一个脉脉，操作菜单打开时隐藏，协同记录仍可访问');

  assert.deepEqual(browser.errors, []);
  pass('全程无未处理的浏览器脚本异常');
} catch (error) {
  console.log('FAILURE STATE', await browser.evaluate("({tail:document.querySelector('.assistant-overlay')?.innerText.slice(-1800),retry:document.querySelectorAll('.assistant-retry').length,scroll:[...document.querySelectorAll('.assistant-conversation')].map(e=>({rect:e.getBoundingClientRect().toJSON(),html:e.innerHTML.slice(0,200)}))})").catch(()=>null));
  await screenshot('assistant-failure').catch(() => {});
  throw error;
} finally {
  fs.mkdirSync(output, { recursive: true });
  fs.writeFileSync(path.join(output, 'assistant-ui-results.json'), JSON.stringify({ passed: results, browserErrors: browser.errors }, null, 2));
  await browser.close();
}
