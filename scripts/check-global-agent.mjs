import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { launchBrowser, pause } from './lib/browser.mjs';

const base='http://127.0.0.1:5173', output=path.resolve('.cache/preview/qa');
fs.mkdirSync(output,{recursive:true});
const password='GlobalAgent-'+Date.now(), phone='196'+String(Date.now()%100000000).padStart(8,'0');
let token;
async function api(method,url,data){
 const r=await fetch(base+'/api/v1'+url,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:data===undefined?undefined:JSON.stringify(data)});
 const body=r.status===204?{}:await r.json();
 assert.ok(r.ok,`${method} ${url}: ${r.status} ${JSON.stringify(body.error)}`);return body.data;
}
token=(await api('POST','/auth/register',{phone,password,nickname:'【自测】全局Agent'})).accessToken;
const initial=await api('GET','/settings');delete initial.updatedAt;
await api('PUT','/settings',{...initial,scienceUpdate:true});
const b=await launchBrowser();const passed=[];
const pass=name=>{passed.push(name);console.log('PASS '+name)};
const visible=selector=>`!!(${b.element(selector)})`;
const last="[...document.querySelectorAll('.assistant-overlay .assistant-exchange')].at(-1)";
async function route(url){await b.evaluate(`new Promise((resolve,reject)=>uni.navigateTo({url:${JSON.stringify(url)},success:resolve,fail:reject}))`);await pause(300)}
async function ask(text,operation,phase){
 const selector=(Array.isArray(operation)?operation:[operation]).map(value=>`.agent-task[data-operation="${value}"]`).join(',');
 await b.fill('.assistant-overlay .assistant-draft textarea',text);await b.click('.assistant-overlay .assistant-send');
 await b.waitFor(`${last}?.querySelector(${JSON.stringify(selector)})?.getAttribute('data-phase')===${JSON.stringify(phase)}`,45000);
 await b.waitFor("!document.querySelector('.assistant-overlay .assistant-panel-identity .mascot-thinking')",30000);
}
async function chooseFile(file){
 await pause(450);
 let opened;const event=new Promise(resolve=>{opened=resolve});
 b.on('Page.fileChooserOpened',opened);
 await b.click('.assistant-overlay .assistant-exchange:last-child .agent-task-file-button');
 const chooser=await Promise.race([event,pause(3000).then(()=>{throw Error('file chooser did not open')})]);
 await b.call('DOM.setFileInputFiles',{backendNodeId:chooser.backendNodeId,files:[file]});
}
async function waitPhase(phase){await b.waitFor(`${last}?.querySelector('.agent-task')?.getAttribute('data-phase')===${JSON.stringify(phase)}`,30000)}
try {
 await b.call('Page.setInterceptFileChooserDialog',{enabled:true});
 await b.call('Page.navigate',{url:base+'/maian/'});
 await b.waitFor(visible('uni-modal .uni-modal__btn_primary'));await b.click('uni-modal .uni-modal__btn_primary');
 await b.fill('.field-row:not(.field-row-last) input',phone);await b.fill('.field-row-last input',password);await b.click('.primary-action');
 await b.waitFor(visible('.assistant-dock-button'));
 await b.click('.assistant-dock-button');
 async function chat(text){
  await b.fill('.assistant-overlay .assistant-draft textarea',text);await b.click('.assistant-overlay .assistant-send');
  await b.waitFor("JSON.parse(uni.getStorageSync('maian:assistant-session:v1')).value.entries.at(-1)?.result?.conversation===true",30000);
  assert.equal(await b.evaluate(`${last}?.querySelectorAll('.agent-task,.assistant-result-card').length`),0);
  const textReply=await b.evaluate(`${last}?.querySelector('.assistant-answer-copy')?.innerText`);
  assert.ok(textReply?.trim());assert.ok(!/暂未找到匹配内容|这次没能理解你的意思/.test(textReply));
  return textReply;
 }
 await chat('Hello');await b.shot(path.join(output,'global-agent-natural-greeting.png'));
 assert.equal(await b.evaluate("document.querySelectorAll('.assistant-result-mode').length"),0);
 pass('真实模型自然回应Hello，没有强制搜索、任务卡或内部模式标签');
 assert.ok((await chat('你刚才说你叫什么？')).includes('脉脉'));
 assert.ok(/设置|设备|文档|联系|救援/.test(await chat('那你能帮我做哪些事？')));
 pass('连续追问保留最近普通对话上下文，解释自身能力不创建业务任务');
 await chat('谢谢，先不用操作');
 assert.equal((await api('GET','/settings')).scienceUpdate,true);
 pass('感谢与闲聊自然回应，不产生设置写入或虚构完成结果');
 await b.click('.assistant-overlay [aria-label="关闭助手"]');
 await route('/pages/mine/settings');await b.click('.assistant-dock-button');
 const before=await api('GET','/settings');assert.equal(before.scienceUpdate,true);
 await ask('把科普更新关掉，并把最高心率阈值设为135','SETTINGS_UPDATE','DONE');
 const changed=await api('GET','/settings');assert.equal(changed.scienceUpdate,false);assert.equal(changed.maxHeartRate,135);assert.equal(changed.rescuePush,before.rescuePush);
 const mode=await b.evaluate(`${last}?.querySelector('.assistant-answer')?.getAttribute('data-execution-mode')`);assert.equal(mode,'SPRING_AI','The live model was not used');
 await b.click('.assistant-overlay [aria-label="关闭助手"]');
 await b.waitFor("document.querySelector('#settings-notifications .settings-item-last .app-switch')?.getAttribute('aria-checked')==='false'");
 assert.ok((await b.evaluate('document.body.innerText')).includes('135'));
 pass('真实DeepSeek指令修改开关和阈值，设置页自动同步，无关设置保持不变');
 await b.click('.assistant-dock-button');
 await ask('把最低心率阈值改成200','SETTINGS_UPDATE','ERROR');assert.equal((await api('GET','/settings')).minHeartRate,before.minHeartRate);
 pass('非法阈值由业务接口拒绝，未写入健康设置');

 await ask('添加紧急联系人，姓名测试家人，手机号13800000000，关系是同学','CONTACT_SAVE','DONE');
 await b.shot(path.join(output,'global-agent-contact-created.png'));
 assert.ok((await api('GET','/emergency-contacts')).some(c=>c.name==='测试家人'));
 await ask('删除联系人测试家人','CONTACT_DELETE','CONFIRM');assert.equal((await api('GET','/emergency-contacts')).length,1);
 await b.click('.assistant-overlay .assistant-exchange:last-child .agent-task-primary');await waitPhase('DONE');assert.equal((await api('GET','/emergency-contacts')).length,0);
 pass('一次自然语言指令直接新增联系人，不要求重复填表或保存确认；删除保留一次确认');

 const note=path.join(output,'global-agent-note.txt');fs.writeFileSync(note,'【自测】全局Agent通用附件。此文件仅测试上传和读取。');
 await ask('我要上传一个文档',['DOCUMENT_AUTO','DOCUMENT_UPLOAD'],'FILE');await chooseFile(note);await waitPhase('DONE');
 assert.ok((await api('GET','/assistant/documents')).some(d=>d.name==='global-agent-note.txt'));
 await b.shot(path.join(output,'global-agent-document.png'));pass('输入上传指令后选择真实文件，上传读取并保存到本人文档');

 const article=path.join(output,'global-agent-science.txt');fs.writeFileSync(article,'【自测】验证文档投稿链路，内容不包含医学指导。');
 await ask('帮我上传文档投稿，标题是全局Agent测试稿，分类是设备知识，正文从我接下来选的文件读取','SCIENCE_SUBMIT','FILE');
 await chooseFile(article);await waitPhase('DONE');
 const submissions=(await api('GET','/science-submissions')).content;assert.equal(submissions[0].status,'PENDING');assert.ok(submissions[0].content.includes('验证文档投稿链路'));
 pass('文档内容进入科普投稿，真实保存为待审核状态');

 const report=path.join(output,'global-agent-health.txt');fs.writeFileSync(report,'【自测】测试体检中心\n检查日期：2026-10-20\n血压 120/80 mmHg\n空腹血糖 5.0 mmol/L\n');
 await ask('导入一份体检报告到健康档案','REPORT_IMPORT','FILE');await chooseFile(report);await waitPhase('CONFIRM');
 assert.equal((await api('GET','/health-reports')).length,0);
 await b.shot(path.join(output,'global-agent-report-review.png'));
 await b.click('.assistant-overlay .assistant-exchange:last-child .agent-task-primary');await waitPhase('DONE');
 const reports=await api('GET','/health-reports');assert.equal(reports.length,1);assert.equal(reports[0].checkupDate,'2026-10-20');assert.equal(reports[0].indicators[0].value,'120/80');
 pass('体检文档提取原始日期机构和指标，核对后真实保存到健康档案');

 await ask('查看待审核的设备','DEVICE_REVIEW_LIST','ERROR');
 assert.ok((await b.evaluate(`${last}?.innerText`)).includes('权限')||(await b.evaluate(`${last}?.innerText`)).includes('无权'));
 pass('普通用户的管理操作仍由真实权限校验拦截');
 assert.deepEqual(b.errors,[]);pass('执行过程无未处理浏览器异常');
} catch(error){
 console.log('STATE',await b.evaluate("document.querySelector('.assistant-overlay')?.innerText.slice(-2600)").catch(()=>null));
 await b.shot(path.join(output,'global-agent-failure.png')).catch(()=>{});throw error;
} finally {
 // Keep the demo accounts and their workflows untouched.
 for(const item of (await api('GET','/science-submissions')).content)await api('DELETE','/science-submissions/'+item.id);
 for(const item of await api('GET','/health-reports'))await api('DELETE','/health-reports/'+item.id);
 for(const item of await api('GET','/assistant/documents'))await api('DELETE','/assistant/documents/'+item.id);
 for(const item of await api('GET','/emergency-contacts'))await api('DELETE','/emergency-contacts/'+item.id);
 fs.writeFileSync(path.join(output,'global-agent-results.json'),JSON.stringify({passed,browserErrors:b.errors},null,2));
 await b.close();
}
