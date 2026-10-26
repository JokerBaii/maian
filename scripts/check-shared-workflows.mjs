import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { launchBrowser, pause } from './lib/browser.mjs';

const base = 'http://127.0.0.1:5173', out = path.resolve('.cache/preview/qa');
const phone = '191' + String(Date.now() % 100000000).padStart(8, '0'), password = 'Shared-' + Date.now();
const registered = await fetch(base + '/api/v1/auth/register', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ phone, password, nickname: '【自测】公共流程' }) });
assert.ok(registered.ok);
const token = (await registered.json()).data.accessToken;
const b = await launchBrowser(), passed = [];
const pass = message => { passed.push(message); console.log('PASS ' + message); };
async function route(url) {
  await b.evaluate(`new Promise((resolve,reject)=>uni.navigateTo({url:${JSON.stringify(url)},success:resolve,fail:reject}))`);
  await pause(300);
}
try {
  await b.call('Page.navigate', { url: base + '/maian/' });
  await b.waitFor("document.querySelector('uni-modal .uni-modal__btn_primary')");
  await b.click('uni-modal .uni-modal__btn_primary');
  await b.fill('.field-row:not(.field-row-last) input', phone);
  await b.fill('.field-row-last input', password);
  await b.click('.primary-action');
  await b.waitFor("document.querySelector('.assistant-dock-button')");
  await route('/pages/device/add');
  await b.fill('.input-wrap input', '【自测】保留设备草稿');
  await b.click('.location-picker');
  await b.waitFor("location.hash.includes('pages/location/pick')&&document.querySelector('.leaflet-tile-loaded')");
  assert.equal(await b.evaluate("document.querySelector('.location-picker-confirm').hasAttribute('disabled')"), true);
  await b.click('#location-picker-map');
  await b.waitFor("document.querySelector('.location-picker-confirm')?.hasAttribute('disabled')===false");
  await b.click('.location-picker-confirm');
  await b.waitFor("location.hash.includes('pages/device/add')&&document.querySelector('.picked-address')");
  assert.equal(await b.evaluate("document.querySelector('.input-wrap input').value"), '【自测】保留设备草稿');
  const selected = await b.evaluate("document.querySelector('.picked-address').innerText");
  pass('设备页使用公共选点页，初始中心不可直接提交，明确选点后回填且保留设备草稿');

  await b.click('.location-picker');
  await b.waitFor("location.hash.includes('pages/location/pick')&&document.querySelector('.leaflet-tile-loaded')");
  await b.waitFor("document.querySelector('.location-picker-confirm')?.hasAttribute('disabled')===false");
  await b.click('#location-picker-map');
  await b.evaluate('uni.navigateBack()');
  await b.waitFor("location.hash.includes('pages/device/add')&&document.querySelector('.picked-address')");
  assert.equal(await b.evaluate("document.querySelector('.picked-address').innerText"), selected);
  assert.equal(await b.evaluate("document.querySelector('.input-wrap input').value"), '【自测】保留设备草稿');
  pass('再次选点带入现有坐标，取消不会覆盖设备位置或清空草稿');

  // Keep the map selection usable even when optional POI/address services are unavailable.
  await b.call('Network.enable');
  await b.call('Network.setBlockedURLs', { urls: ['*webapi.amap.com/*'] });
  await b.call('Page.reload');
  await b.waitFor("document.querySelector('.location-btn')");
  await b.click('.location-picker');
  await b.waitFor("location.hash.includes('pages/location/pick')&&document.querySelector('.leaflet-tile-loaded')");
  await b.fill('.location-search input', '杭州地点');
  await b.click('.location-search uni-button');
  await b.waitFor("document.querySelector('.location-search uni-button')?.innerText==='搜索'", 12000);
  await b.click('#location-picker-map');
  await b.click('.location-picker-confirm');
  await b.waitFor("location.hash.includes('pages/device/add')");
  pass('高德搜索不可用时，底图选点和确认仍可继续');
  await b.call('Network.setBlockedURLs', { urls: [] });

  const wearableUrl = base + '/api/v1/wearable-device';
  const headers = { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token };
  const saved = await fetch(wearableUrl, { method: 'PUT', headers, body: JSON.stringify({ deviceIdentifier: 'persisted-watch', name: '【自测】已登记手环', type: 'bluetooth', connected: false, battery: 80 }) });
  assert.ok(saved.ok);
  let deletes = 0;
  b.on('Network.requestWillBeSent', ({ request }) => { if (request.method === 'DELETE' && request.url.endsWith('/api/v1/wearable-device')) deletes++; });
  await route('/pages/health/bind');
  await b.waitFor("document.querySelector('.bound-card')");
  assert.ok((await b.evaluate("document.querySelector('.connected-badge').innerText")).includes('未连接'));
  await b.evaluate('window.__nativeCloseCalls=0;uni.closeBLEConnection=()=>{window.__nativeCloseCalls++}');
  await b.click('.disconnect-btn');
  await b.waitFor("document.querySelector('uni-modal .uni-modal__btn_primary')");
  await b.click('uni-modal .uni-modal__btn_primary');
  await b.waitFor("document.querySelector('.empty-bound')");
  for (let i = 0; i < 30 && !deletes; i++) await pause(100);
  assert.equal(deletes, 1);
  assert.equal(await b.evaluate('window.__nativeCloseCalls'), 0);
  assert.equal((await (await fetch(wearableUrl, { headers })).json()).data.type, 'none');
  pass('已登记设备显示未连接；H5 解绑只提交一次，且不把历史标识当成本机蓝牙连接');

  await b.waitFor("document.querySelector('.scan-btn')");
  await b.evaluate('window.__nativeScanCalls=0;uni.openBluetoothAdapter=()=>{window.__nativeScanCalls++}');
  await b.click('.scan-btn');
  await b.waitFor("document.querySelector('uni-modal .uni-modal__btn_primary')");
  assert.equal(await b.evaluate('window.__nativeScanCalls'), 0);
  assert.ok((await b.evaluate("document.querySelector('uni-modal').innerText")).includes('手机客户端'));
  await b.click('uni-modal .uni-modal__btn_primary');
  pass('H5 扫描只说明客户端入口，不同时调用原生蓝牙或出现 iOS 专属误提示');

  await b.call('Emulation.setDeviceMetricsOverride', { width: 320, height: 568, deviceScaleFactor: 1, mobile: true });
  assert.ok(await b.evaluate('document.documentElement.scrollWidth<=innerWidth+1'));
  assert.deepEqual(b.errors, []);
  pass('公共流程在小屏无横向溢出且无未处理浏览器异常');
} catch (error) {
  console.log('STATE', await b.evaluate('document.body.innerText.slice(-1200)').catch(() => null));
  await b.shot(path.join(out, 'shared-workflows-failure.png')).catch(() => {});
  throw error;
} finally {
  fs.mkdirSync(out, { recursive: true });
  fs.writeFileSync(path.join(out, 'shared-workflows-results.json'), JSON.stringify({ passed, browserErrors: b.errors }, null, 2));
  await b.close();
}
