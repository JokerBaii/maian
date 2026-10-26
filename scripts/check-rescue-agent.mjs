import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { launchBrowser, pause } from './lib/browser.mjs';

const base = 'http://127.0.0.1:5173', output = path.resolve('.cache/preview/qa');
fs.mkdirSync(output, { recursive: true });
const phone = '195' + String(Date.now() % 100000000).padStart(8, '0'), password = 'RescueAgent-' + Date.now();
let token, deviceId, callId;
const results = [];
async function api(method, url, data, access = token) {
  const response = await fetch(base + '/api/v1' + url, { method, headers: { 'Content-Type': 'application/json', ...(access ? { Authorization: 'Bearer ' + access } : {}) }, body: data === undefined ? undefined : JSON.stringify(data) });
  const body = response.status === 204 ? {} : await response.json();
  assert.ok(response.ok, `${method} ${url}: ${response.status} ${JSON.stringify(body.error)}`);
  return body.data;
}
const b = await launchBrowser();
const pass = name => { results.push(name); console.log('PASS ' + name); };
async function ask(message = '有个人晕倒了，需要急救') {
  await b.fill('.assistant-overlay .assistant-draft textarea', message);
  await b.click('.assistant-overlay .assistant-send');
}
try {
  token = (await api('POST', '/auth/register', { phone, password, nickname: '【自测】急救Agent' })).accessToken;
  await b.call('Browser.grantPermissions', { origin: base, permissions: ['geolocation'] });
  await b.call('Emulation.setGeolocationOverride', { latitude: 22.34, longitude: 113.47, accuracy: 15 });
  await b.call('Page.navigate', { url: base + '/maian/' });
  await b.waitFor("document.querySelector('uni-modal .uni-modal__btn_primary')");
  await b.click('uni-modal .uni-modal__btn_primary');
  await b.fill('.field-row:not(.field-row-last) input', phone); await b.fill('.field-row-last input', password); await b.click('.primary-action');
  await b.waitFor("document.querySelector('.assistant-dock-button')");
  const position = await b.evaluate("import('/maian/src/utils/location.ts').then(m=>m.getCurrentGcj02Location())");
  deviceId = (await api('POST', '/emergency-devices', { name: '【自测】急救Agent专用AED', address: '【自测】定位测试现场', type: 'FIXED', category: 'AED', latitude: position.latitude + .00001, longitude: position.longitude, serviceWindows: ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'].map(dayOfWeek => ({ dayOfWeek, opensAt: '00:00', closesAt: '00:00' })), imageMediaIds: [], vehicleImageMediaIds: [] })).id;
  const admin = (await api('POST', '/auth/login', { phone: '19900000003', password: fs.readFileSync('.cache/preview/password', 'utf8').trim() })).accessToken;
  await api('PATCH', `/emergency-devices/${deviceId}/review`, { approved: true, reviewNote: '本地急救Agent回归' }, admin);
  await b.click('.assistant-dock-button');
  await ask();
  await b.waitFor("location.hash.includes('pages/rescue/index')&&document.querySelector('.sos-primary')?.innerText==='发出紧急呼救'",25000);
  const planningMode = await b.evaluate("import('/maian/src/composables/useAssistantSession.ts').then(m=>m.assistantSession.entries.value.at(-1).result.executionMode)");
  assert.equal(planningMode,'SPRING_AI');
  assert.equal(await b.evaluate("document.querySelector('.stage-desc').innerText"),'有个人晕倒了，需要急救');
  assert.equal((await api('GET','/rescue-calls')).content.length,0);
  assert.equal((await api('GET','/emergency-devices/mine/'+deviceId)).status,'AVAILABLE');
  await b.shot(path.join(output,'rescue-agent-emergency-page.png'));
  pass('真实模型识别求助后直接进入急救页，自动定位并带入描述，不显示助手确认表单、不提前占用设备');
  await b.click('.sos-primary');
  await b.waitFor("location.hash.includes('pages/rescue/detail?id=')",20000);
  const call = (await api('GET', '/rescue-calls')).content[0]; callId = call.id;
  assert.ok(Math.abs(call.latitude - position.latitude) < .000001 && Math.abs(call.longitude - position.longitude) < .000001);
  assert.deepEqual(call.symptoms, ['需要紧急救助']); assert.equal(call.matchedAed.deviceId, deviceId);
  await b.shot(path.join(output, 'rescue-agent-created.png'));
  await api('POST', '/rescue-calls/' + callId + '/cancellation'); callId = undefined;
  pass('只点击一次急救页发送按钮，使用现场定位创建呼救并由原调度器匹配专用测试AED');

  await b.evaluate(`new Promise((resolve,reject)=>uni.navigateTo({url:'/pages/rescue/detail?id=${call.id}',success:resolve,fail:reject}))`);
  await b.waitFor("document.querySelector('.rescue-review')&&document.querySelector('.detail-card')");
  assert.equal(await b.evaluate("document.querySelector('.review-options')!==null"),false);
  await b.click('.review-options-toggle');
  for(const width of [320,390,768]){
    await b.call('Emulation.setDeviceMetricsOverride',{width,height:844,deviceScaleFactor:1,mobile:true});await pause(200);
    const alignment=await b.evaluate(`(()=>{
      const box=s=>document.querySelector(s).getBoundingClientRect().toJSON();
      const row=document.querySelector('.review-option-row'),label=row.children[0].getBoundingClientRect(),toggle=row.querySelector('.app-switch').getBoundingClientRect();
      return {review:box('.rescue-review'),detail:box('.detail-card'),quick:box('.quick-actions'),heading:box('.review-heading'),title:box('.detail-card .panel-title'),button:box('.review-generate'),row:box('.review-option-row'),centers:Math.abs(label.top+label.height/2-toggle.top-toggle.height/2)};
    })()`);
    for(const neighbor of [alignment.detail,alignment.quick]){
      assert.ok(Math.abs(alignment.review.left-neighbor.left)<1,'Review left edge is inset');
      assert.ok(Math.abs(alignment.review.right-neighbor.right)<1,'Review right edge is inset');
    }
    assert.ok(Math.abs(alignment.heading.left-alignment.title.left)<1,'Review title has duplicate padding');
    assert.ok(Math.abs(alignment.button.left-alignment.heading.left)<1&&Math.abs(alignment.button.right-alignment.row.right)<1,'Review button does not align with content');
    assert.ok(alignment.centers<1,'Switch is not centered with its label');
    await b.evaluate("document.querySelector('.rescue-review').scrollIntoView({block:'center'})");await pause(150);
    await b.shot(path.join(output,'rescue-review-alignment-'+width+'.png'));
  }
  await b.call('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:true});
  await b.click('.review-options-toggle');
  await b.click('.review-generate');
  await b.waitFor("document.querySelector('.review-footer')",25000);
  assert.equal(await b.evaluate("document.querySelector('.review-details')!==null||document.querySelector('.review-options')!==null"),false);
  assert.ok((await b.evaluate("document.querySelector('.review-overview').innerText")).includes('暂无后续待办'));
  assert.ok(await b.evaluate("document.querySelector('.rescue-review').getBoundingClientRect().height<160"),'Completed review still occupies too much vertical space');
  await b.evaluate("document.querySelector('.rescue-review').scrollIntoView({block:'center'})");await pause(200);await b.shot(path.join(output,'rescue-review-summary.png'));
  await b.click('.review-details-toggle');await b.waitFor("document.querySelector('.review-events')");
  const actualReview=await api('GET','/rescue-reviews/'+call.id);
  assert.equal(await b.evaluate("document.querySelectorAll('.review-metric-row').length"),actualReview.data.metrics.filter(metric=>metric.seconds!==null).length);
  assert.ok(!(await b.evaluate("document.querySelector('.rescue-review').innerText")).includes('未记录'));
  assert.equal(await b.evaluate("document.querySelectorAll('.review-event').length"),actualReview.data.timeline.length);
  await b.click('.review-tab:nth-child(2)');await b.waitFor("document.querySelector('.review-process')");
  assert.equal(await b.evaluate("document.querySelector('.review-events')!==null"),false);
  assert.equal(await b.evaluate("document.querySelectorAll('.review-step').length"),actualReview.steps.length);
  await b.shot(path.join(output,'rescue-review-process-tab.png'));
  await b.click('.review-tab:nth-child(3)');await b.waitFor("document.querySelector('.review-articles')");
  assert.equal(await b.evaluate("document.querySelector('.review-process')!==null"),false);
  assert.equal(await b.evaluate("document.querySelectorAll('.review-article').length"),actualReview.articles.length);
  await b.click('.review-details-toggle');assert.equal(await b.evaluate("document.querySelector('.review-details')!==null"),false);
  pass('真实复盘默认只显示结论和待办，事件/过程/学习分开查看，缺失时效不占位');
  await b.evaluate("new Promise((resolve,reject)=>uni.switchTab({url:'/pages/index/index',success:resolve,fail:reject}))");await b.waitFor("document.querySelector('.assistant-dock-button')");await b.click('.assistant-dock-button');
  pass('救援详情复盘与相邻卡片边缘、标题、按钮及开关在320/390/768宽度下对齐');


  await b.click('.assistant-overlay [aria-label="清空查找记录"]');
  await ask('故事里主角晕倒了，我是在讨论剧情，不需要打开急救页');
  await b.waitFor("import('/maian/src/composables/useAssistantSession.ts').then(m=>!m.assistantSession.busy.value)",25000);
  assert.ok(!(await b.evaluate('location.hash')).includes('pages/rescue/index'));
  assert.equal((await api('GET','/rescue-calls')).content.length,1);
  pass('模型结合语境讨论剧情时不会因为晕倒关键词跳转或创建呼救');
  assert.deepEqual(b.errors, []); pass('无未处理浏览器异常');
} catch (error) {
  console.log('STATE', await b.evaluate("document.body.innerText.slice(-1600)").catch(() => null));
  await b.shot(path.join(output, 'rescue-agent-failure.png')).catch(() => {});
  throw error;
} finally {
  if (callId) await api('POST', '/rescue-calls/' + callId + '/cancellation').catch(() => {});
  if (deviceId) await api('POST', '/emergency-devices/' + deviceId + '/disable').catch(() => {});
  fs.writeFileSync(path.join(output, 'rescue-agent-results.json'), JSON.stringify({ passed: results, browserErrors: b.errors }, null, 2));
  await b.close();
}
