import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {launchBrowser,pause} from './lib/browser.mjs';
const base='http://127.0.0.1:5173',out=path.resolve('.cache/preview/qa');fs.mkdirSync(out,{recursive:true});
const suffix=Date.now()%99999900,password='AgentFlow-'+Date.now(),phones=[0,1].map(i=>'194'+String(suffix+i).padStart(8,'0'));
let user,volunteer,admin,callId;const deviceIds=[],submissionIds=[],reminderIds=[],passed=[];
async function api(token,method,url,data){const r=await fetch(base+'/api/v1'+url,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:data===undefined?undefined:JSON.stringify(data)});const body=r.status===204?{}:await r.json();assert.ok(r.ok,`${method} ${url}: ${r.status} ${JSON.stringify(body.error)}`);return body.data}
const b=await launchBrowser(),v=await launchBrowser();
const last='.assistant-overlay .assistant-exchange:last-child .agent-task';
const pass=name=>{passed.push(name);console.log('PASS '+name)};
async function login(browser,phone,pw){await browser.call('Browser.grantPermissions',{origin:base,permissions:['geolocation']});await browser.call('Emulation.setGeolocationOverride',{latitude:22.34,longitude:113.47,accuracy:15});await browser.call('Page.navigate',{url:base+'/maian/'});await browser.waitFor("document.querySelector('uni-modal .uni-modal__btn_primary')");await browser.click('uni-modal .uni-modal__btn_primary');await browser.fill('.field-row:not(.field-row-last) input',phone);await browser.fill('.field-row-last input',pw);await browser.click('.primary-action');await browser.waitFor("document.querySelector('.assistant-dock-button')")}
async function open(browser){if(!await browser.evaluate("!!document.querySelector('.assistant-overlay')"))await browser.click('.assistant-dock-button')}
async function ask(browser,text,op,phase){await open(browser);await browser.fill('.assistant-overlay .assistant-draft textarea',text);await browser.click('.assistant-overlay .assistant-send');await browser.waitFor(`document.querySelector('${last}')?.getAttribute('data-operation')===${JSON.stringify(op)}&&document.querySelector('${last}')?.getAttribute('data-phase')===${JSON.stringify(phase)}`,45000);await browser.waitFor("!document.querySelector('.assistant-overlay .mascot-thinking')",30000);await pause(150)}
async function route(browser,url,tab=false){if(await browser.evaluate("!!document.querySelector('.assistant-overlay')"))await browser.click('.assistant-overlay [aria-label="关闭助手"]');await browser.evaluate(`new Promise((resolve,reject)=>uni.${tab?'switchTab':'navigateTo'}({url:${JSON.stringify(url)},success:resolve,fail:reject}))`);await pause(300)}
async function shot(browser,name){await browser.shot(path.join(out,'agent-flow-'+name+'.png'))}
try{
 user=(await api(null,'POST','/auth/register',{phone:phones[0],password,nickname:'【自测】全流程用户'})).accessToken;
 const registered=await api(null,'POST','/auth/register',{phone:phones[1],password,nickname:'【自测】全流程志愿者'});
 admin=(await api(null,'POST','/auth/login',{phone:'19900000003',password:fs.readFileSync('.cache/preview/password','utf8').trim()})).accessToken;
 const vp=await api(registered.accessToken,'GET','/profile');await api(admin,'PATCH','/admin/users/'+vp.id+'/role',{role:'VOLUNTEER'});volunteer=(await api(null,'POST','/auth/login',{phone:phones[1],password})).accessToken;
 await login(b,phones[0],password);
 await ask(b,'添加一个紧急联系人','CONTACT_SAVE','INPUT');await ask(b,'她叫林芳，是我妈妈','CONTACT_SAVE','INPUT');await ask(b,'手机号13800000000','CONTACT_SAVE','DONE');
 const contacts=await api(user,'GET','/emergency-contacts');assert.equal(contacts.length,1);assert.equal(contacts[0].phone,'13800000000');
 pass('真实模型起任务，连续口述姓名关系和手机号，沿用同一任务自动保存一次');
 await ask(b,'删除紧急联系人','CONTACT_DELETE','CONFIRM');await ask(b,'取消','CONTACT_DELETE','CANCELED');assert.equal((await api(user,'GET','/emergency-contacts')).length,1);
 await ask(b,'删除紧急联系人','CONTACT_DELETE','CONFIRM');await ask(b,'确认','CONTACT_DELETE','DONE');assert.equal((await api(user,'GET','/emergency-contacts')).length,0);
 pass('文字取消保持业务记录，文字确认执行真实删除');
 await ask(b,'登记一个固定AED设备，名称是【自测】全局办理AED，服务时间全天','DEVICE_CREATE','DONE');
 const device=(await api(user,'GET','/emergency-devices/mine')).content[0];deviceIds.push(device.id);assert.equal(device.status,'PENDING_REVIEW');assert.equal(device.serviceWindows.length,7);assert.ok(device.latitude>30&&device.latitude<31);assert.ok(device.longitude>120&&device.longitude<121);
 await api(admin,'PATCH','/emergency-devices/'+device.id+'/review',{approved:true,reviewNote:'本地回归'});
 await route(b,'/pages/device/add?id='+device.id);await ask(b,'把这台设备停用','DEVICE_DISABLE','CONFIRM');await ask(b,'确认','DEVICE_DISABLE','DONE');assert.equal((await api(user,'GET','/emergency-devices/mine/'+device.id)).status,'DISABLED');
 pass('设备登记自动定位并补齐合法服务时段，在编辑页说“这台”自动绑定真实设备');
 await api(user,'POST','/emergency-devices/'+device.id+'/enable');
 await route(b,'/pages/index/index',true);await open(b);await b.call('Page.setInterceptFileChooserDialog',{enabled:true});let chooserResolve;const chooser=new Promise(resolve=>{chooserResolve=resolve});b.on('Page.fileChooserOpened',chooserResolve);await b.click('.assistant-attach');
 const file=path.join(out,'agent-flow-report.txt');fs.writeFileSync(file,'【自测】体检中心\n检查日期：2026-10-20\n血压 120/80 mmHg\n');const picked=await Promise.race([chooser,pause(4000).then(()=>{throw Error('direct file picker did not open')})]);await b.call('DOM.setFileInputFiles',{backendNodeId:picked.backendNodeId,files:[file]});
 await b.waitFor(`document.querySelector('${last}')?.getAttribute('data-operation')==='REPORT_IMPORT'&&document.querySelector('${last}')?.getAttribute('data-phase')==='CONFIRM'`);
 assert.equal((await api(user,'GET','/health-reports')).length,0);await shot(b,'auto-report');await ask(b,'确认','REPORT_IMPORT','DONE');assert.equal((await api(user,'GET','/health-reports')).length,1);
 pass('加号直接打开文件选择器，自动识别报告用途，核对后真实入档');
 const docCount=(await api(user,'GET','/assistant/documents')).length;
 await ask(b,'把刚才这份文档作为科普稿投稿，标题是全局办理测试稿，分类是设备知识','SCIENCE_SUBMIT','DONE');
 const submission=(await api(user,'GET','/science-submissions')).content[0];submissionIds.push(submission.id);assert.ok(submission.content.includes('血压 120/80'));assert.equal((await api(user,'GET','/assistant/documents')).length,docCount);
 pass('“刚才这份文档”复用本人已上传文件完成投稿，没有再次选文件或上传');
 await api(admin,'PATCH','/science-submissions/'+submission.id+'/review',{approved:true,reviewNote:'临时自测，稍后删除'});const articleId='submission-'+submission.id;
 await api(user,'PUT','/science-articles/'+articleId+'/interaction',{liked:true,collected:false});await route(b,'/pages/science/detail?id='+articleId);await ask(b,'收藏这篇文章','SCIENCE_INTERACTION','DONE');
 assert.deepEqual((({liked,collected})=>({liked,collected}))(await api(user,'GET','/science-articles/'+articleId+'/interaction')),{liked:true,collected:true});await b.waitFor("document.querySelectorAll('.action-icon-active').length===2");await shot(b,'article-context');
 pass('当前文章指代、真实收藏与页面即时同步，保留原点赞值');
 await ask(b,'十分钟后提醒我归还AED','REMINDER_CREATE','DONE');let reminders=await api(user,'GET','/assistant/reminders');reminderIds.push(...reminders.map(r=>r.id));assert.equal(reminders.length,1);assert.ok(new Date(reminders[0].dueAt).getTime()>Date.now());
 await b.click('.assistant-overlay [aria-label="清空查找记录"]');assert.equal((await api(user,'GET','/assistant/reminders')).length,1);
 await ask(b,'查看我的提醒','REMINDER_LIST','DONE');await ask(b,'取消第一条提醒','REMINDER_DISMISS','DONE');assert.equal((await api(user,'GET','/assistant/reminders')).length,0);
 pass('定时站内提醒持久保存，清空对话后仍可查询，按序号结束真实提醒');
 const watchDevice=await api(user,'POST','/emergency-devices',{name:'【自测】审核跟进AED',address:'【自测】跟进位置',type:'FIXED',category:'AED',latitude:device.latitude+1,longitude:device.longitude,serviceWindows:device.serviceWindows,imageMediaIds:[],vehicleImageMediaIds:[]});deviceIds.push(watchDevice.id);
 await route(b,'/pages/device/add?id='+watchDevice.id);await ask(b,'这台设备审核后提醒我','REMINDER_CREATE','DONE');const watched=(await api(user,'GET','/assistant/reminders'))[0];reminderIds.push(watched.id);assert.equal(watched.status,'WAITING');assert.equal(watched.targetId,watchDevice.id);
 await b.click('.assistant-overlay [aria-label="关闭助手"]');await api(admin,'PATCH','/emergency-devices/'+watchDevice.id+'/review',{approved:true,reviewNote:'自测触发跟进'});
 for(let i=0;i<40;i++){if((await api(user,'GET','/assistant/reminders')).some(r=>r.id===watched.id&&r.status==='READY'))break;await pause(500)}
 assert.equal((await api(user,'GET','/assistant/reminders'))[0].status,'READY');await b.waitFor("document.querySelector('.assistant-dock-label')?.innerText.includes('条提醒')",40000);await shot(b,'reminder-notice');
 pass('退出对话后后台仍跟进审核状态，真实变更后显示站内提醒标记');
 await route(b,'/pages/index/index',true);if(!await b.evaluate("!!document.querySelector('.assistant-overlay')"))await b.click('.assistant-dock-button');
 await b.fill('.assistant-overlay .assistant-draft textarea','有个人晕倒了，需要急救');await b.click('.assistant-overlay .assistant-send');
 await b.waitFor("location.hash.includes('pages/rescue/index')&&document.querySelector('.sos-primary')?.innerText==='发出紧急呼救'",25000);
 await b.click('.sos-primary');await b.waitFor("location.hash.includes('pages/rescue/detail?id=')",20000);
 const call=(await api(user,'GET','/rescue-calls')).content[0];callId=call.id;assert.ok(call.matchedAed);
 await login(v,phones[1],password);await ask(v,'开始接单','VOLUNTEER_AVAILABILITY','DONE');await route(v,'/pages/rescue/detail?id='+callId+'&mode=responder');await ask(v,'接下这个任务','RESCUE_ACCEPT','DONE');
 for(const [text,op] of [['我已到达AED取用点','RESCUE_AED_ARRIVAL'],['确认取到AED','RESCUE_AED_PICKUP'],['我已到达现场','RESCUE_ARRIVAL'],['开始施救','RESCUE_START'],['提交完成','RESCUE_COMPLETE']]){await ask(v,text,op,'DONE')}
 await route(b,'/pages/rescue/detail?id='+callId);await ask(b,'确认救援完成','RESCUE_CONFIRM_COMPLETION','CONFIRM');await ask(b,'确认','RESCUE_CONFIRM_COMPLETION','DONE');
 await ask(v,'归还AED','RESCUE_RETURN','DONE');const finished=await api(user,'GET','/rescue-calls/'+callId);assert.equal(finished.status,'COMPLETED');assert.equal(finished.aedCustodyStatus,'RETURNED');await shot(v,'rescue-return');
 pass('真实双账号接单、取用点、取用、到场、施救、提交完成、求救者确认和归还全部由Agent办理');
 await ask(v,'暂停接单','VOLUNTEER_AVAILABILITY','DONE');
 assert.deepEqual(b.errors,[]);assert.deepEqual(v.errors,[]);pass('全流程无未处理浏览器异常');
}catch(error){console.log('USER STATE',await b.evaluate("document.querySelector('.assistant-overlay')?.innerText.slice(-1800)").catch(()=>null));console.log('VOLUNTEER STATE',await v.evaluate("document.querySelector('.assistant-overlay')?.innerText.slice(-1800)").catch(()=>null));await shot(b,'failure').catch(()=>{});throw error}
finally{
 if(callId){const call=await api(user,'GET','/rescue-calls/'+callId).catch(()=>null);if(call&&call.status==='PENDING_CONFIRMATION'){await api(user,'POST','/rescue-calls/'+callId+'/completion-confirmation').catch(()=>{});await api(volunteer,'POST','/rescue-calls/'+callId+'/aed-return').catch(()=>{});}if(call&&['MATCHING','PENDING'].includes(call.status))await api(user,'POST','/rescue-calls/'+callId+'/cancellation').catch(()=>{});}
 if(user){for(const item of (await api(user,'GET','/science-submissions').catch(()=>({content:[]}))).content)await api(user,'DELETE','/science-submissions/'+item.id).catch(()=>{});for(const item of await api(user,'GET','/emergency-contacts').catch(()=>[]))await api(user,'DELETE','/emergency-contacts/'+item.id).catch(()=>{});for(const item of await api(user,'GET','/assistant/documents').catch(()=>[]))await api(user,'DELETE','/assistant/documents/'+item.id).catch(()=>{});for(const item of await api(user,'GET','/health-reports').catch(()=>[]))await api(user,'DELETE','/health-reports/'+item.id).catch(()=>{});for(const item of await api(user,'GET','/assistant/reminders').catch(()=>[]))await api(user,'DELETE','/assistant/reminders/'+item.id).catch(()=>{});for(const id of deviceIds)await api(user,'POST','/emergency-devices/'+id+'/disable').catch(()=>{});}
 if(volunteer)await api(volunteer,'DELETE','/rescue-calls/responder-presence').catch(()=>{});
 fs.writeFileSync(path.join(out,'agent-workflow-results.json'),JSON.stringify({passed,browserErrors:[...b.errors,...v.errors]},null,2));await b.close();await v.close();
}
