import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {launchBrowser,pause} from './lib/browser.mjs';

const base='http://127.0.0.1:5173',out=path.resolve('.cache/preview/qa');
const phone='188'+String(Date.now()%100000000).padStart(8,'0'),password='Density-'+Date.now();
let token,deviceId,reportId;
const passed=[],b=await launchBrowser();
const pass=name=>{passed.push(name);console.log('PASS '+name)};
async function api(method,url,data,access=token){
 const response=await fetch(base+'/api/v1'+url,{method,headers:{'Content-Type':'application/json',...(access?{Authorization:'Bearer '+access}:{})},body:data===undefined?undefined:JSON.stringify(data)});
 const body=response.status===204?{}:await response.json();assert.ok(response.ok,`${method} ${url}: ${response.status}`);return body.data;
}
async function route(url,tab=false){await b.evaluate(`new Promise((resolve,reject)=>uni.${tab?'switchTab':'navigateTo'}({url:${JSON.stringify(url)},success:resolve,fail:reject}))`);await pause(250)}
async function shot(name){await b.shot(path.join(out,'density-'+name+'.png'))}
try{
 token=(await api('POST','/auth/register',{phone,password,nickname:'【自测】页面信息层级'})).accessToken;
 await b.call('Page.navigate',{url:base+'/maian/'});await b.waitFor("document.querySelector('uni-modal .uni-modal__btn_primary')");await b.click('uni-modal .uni-modal__btn_primary');
 await b.fill('.field-row:not(.field-row-last) input',phone);await b.fill('.field-row-last input',password);await b.click('.primary-action');await b.waitFor("document.querySelector('.assistant-dock-button')");

 await route('/pages/health/index',true);await b.waitFor("document.querySelector('.trend-empty-action')");
 assert.equal(await b.evaluate("document.querySelector('.metric-row')!==null"),false);await shot('health-empty');
 await b.click('.trend-empty-action');await b.waitFor("location.hash.includes('pages/health/bind')");
 pass('健康首页无数据时只保留一个连接入口，不显示三组空统计，连接入口可用');

 await route('/pages/health/detail');await b.waitFor("document.querySelector('.sample-note')");
 assert.equal(await b.evaluate("document.querySelectorAll('.chart-section,.stats-strip,.scene-statistics,.alert-card').length"),0);await shot('heart-empty');
 await route('/pages/health/alert');await b.waitFor("document.querySelector('.empty-state')");assert.equal(await b.evaluate("document.querySelector('.summary-card')!==null"),false);await shot('alerts-empty');
 pass('心率详情和预警空页不堆空图表、零值卡片或重复的最近预警');

 await route('/pages/device/manage');await b.waitFor("document.querySelector('.empty-title')");
 assert.equal(await b.evaluate("document.querySelector('.empty-desc')!==null"),false);await shot('devices-empty');
 await route('/pages/science/contribute');await b.waitFor("document.querySelector('.contribution-guide')");
 assert.equal(await b.evaluate("document.querySelector('.tips-list')!==null"),false);
 await b.fill('.contribution-input input','【自测】保留表单草稿');await b.click('.contribution-guide .app-disclosure-heading');
 await b.waitFor("document.querySelectorAll('.tips-item').length===4");await b.click('.contribution-guide .app-disclosure-heading');
 assert.equal(await b.evaluate("document.querySelector('.contribution-input input').value"),'【自测】保留表单草稿');await shot('contribution');
 pass('设备空页删除重复提示，投稿须知默认折叠且展开收起保留表单草稿');

 const settings=await api('GET','/settings');await api('PUT','/settings',{...settings,healthDataShare:true});
 await api('PUT','/wearable-device',{deviceIdentifier:'density-'+Date.now(),name:'【自测】模拟输入',type:'watch',connected:true,battery:80});
 for(const minutes of [21,11,1])await api('POST','/heart-rate-readings',{bpm:80,scene:'resting',recordedAt:new Date(Date.now()-minutes*60000).toISOString()});
 await route('/pages/health/detail');await b.waitFor("document.querySelector('.stats-strip')&&document.querySelector('.scene-statistics')");
 assert.equal(await b.evaluate("document.querySelector('.scene-row')!==null"),false);
 await b.waitFor(`(()=>{const canvases=[...document.querySelectorAll('.chart-canvas canvas')].filter(c=>c.getBoundingClientRect().width>0);return canvases.some(c=>{const pixels=c.getContext('2d').getImageData(0,0,c.width,c.height).data;return pixels.some((v,i)=>i%4===3&&v>0)})})()`);
 await b.click('.scene-statistics .app-disclosure-heading');await b.waitFor("document.querySelectorAll('.scene-row').length===1");
 assert.ok((await b.evaluate("document.querySelector('.scene-row').innerText")).includes('80'));
 await b.click('.scene-statistics .app-disclosure-heading');assert.equal(await b.evaluate("document.querySelector('.scene-row')!==null"),false);await shot('heart-records');
 pass('真实心率数据保留趋势与简明统计，场景按需展开，只展示有记录的场景');

 const form=new FormData();form.append('file',new Blob([fs.readFileSync('src/static/icons/tab-home.png')],{type:'image/png'}),'自测检查单.png');
 const uploaded=await fetch(base+'/api/v1/media?purpose=HEALTH_REPORT',{method:'POST',headers:{Authorization:'Bearer '+token},body:form});assert.ok(uploaded.ok);const media=(await uploaded.json()).data;
 const report=await api('POST','/health-reports',{hospital:'【自测】报告排版',checkupDate:new Date().toISOString().slice(0,10),sourceMediaId:media.mediaId,indicators:[{name:'血压',value:'120/80',unit:'mmHg',abnormal:false}]});reportId=report.id;
 await route('/pages/checkup/report?id='+report.id);await b.waitFor("document.querySelector('.source-disclosure')&&document.querySelector('.indicator-row')");
 assert.equal(await b.evaluate("document.querySelector('.source-image')!==null"),false);assert.ok(await b.evaluate("!!document.querySelector('.disclaimer-card')"));
 await b.click('.source-disclosure .app-disclosure-heading');await b.waitFor("document.querySelector('.source-image')");await b.click('.source-disclosure .app-disclosure-heading');await shot('report');
 pass('真实报告优先显示结论和指标，原图按需展开，报告免责声明保留');

 deviceId=(await api('POST','/emergency-devices',{name:'【自测】协同信息层级',address:'【自测】设备位置',type:'FIXED',category:'AED',latitude:22.34,longitude:113.47,serviceWindows:['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'].map(dayOfWeek=>({dayOfWeek,opensAt:'00:00',closesAt:'00:00'})),imageMediaIds:[],vehicleImageMediaIds:[]})).id;
 const admin=(await api('POST','/auth/login',{phone:'19900000003',password:fs.readFileSync('.cache/preview/password','utf8').trim()})).accessToken;
 await api('PATCH','/emergency-devices/'+deviceId+'/review',{approved:true,reviewNote:'本机排版自测'},admin);
 await route('/pages/agent/index');await b.waitFor("document.querySelector('.coordination-card')");
 assert.equal(await b.evaluate("document.querySelector('.task-trigger')!==null||document.querySelector('.description')!==null||document.querySelector('.task-process')!==null"),false);
 await b.click('.task-footer');await b.waitFor("document.querySelector('.task-process')");
 assert.equal(await b.evaluate("document.querySelector('.execution-mode')!==null"),false);await b.click('.task-footer');await shot('coordination');
 pass('真实协同记录显示设备、结果和时间，过程按需展开，删除内部模式与重复说明');

 for(const url of ['/pages/health/detail','/pages/science/contribute','/pages/checkup/report?id='+report.id,'/pages/agent/index']){
  await route(url);await b.call('Emulation.setDeviceMetricsOverride',{width:320,height:568,deviceScaleFactor:1,mobile:true});await pause(150);
  assert.ok(await b.evaluate("document.documentElement.scrollWidth<=innerWidth+1"),'Page overflows at 320px: '+url);
 }
 assert.deepEqual(b.errors,[]);pass('上述页面在320px小屏无横向溢出且无未处理浏览器异常');
}catch(error){console.log('STATE',await b.evaluate("document.body.innerText.slice(-1600)").catch(()=>null));await shot('failure').catch(()=>{});throw error}
finally{
 if(reportId)await api('DELETE','/health-reports/'+reportId).catch(()=>{});
 if(deviceId)await api('POST','/emergency-devices/'+deviceId+'/disable').catch(()=>{});
 if(token)await api('DELETE','/wearable-device').catch(()=>{});
 fs.mkdirSync(out,{recursive:true});fs.writeFileSync(path.join(out,'page-density-results.json'),JSON.stringify({passed,browserErrors:b.errors},null,2));await b.close();
}
