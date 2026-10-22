import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawn } from 'node:child_process';

export const pause = ms => new Promise(resolve => setTimeout(resolve, ms));

export async function launchBrowser({ width = 390, height = 844 } = {}) {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'maian-browser-'));
  const chrome = spawn(process.env.CHROME_BINARY || '/opt/google/chrome/chrome', [
    '--headless=new', '--no-sandbox', '--disable-gpu', '--no-first-run',
    '--remote-debugging-port=0', '--user-data-dir=' + directory, 'about:blank'
  ], { stdio: ['ignore', 'ignore', 'pipe'] });
  let launchError = '';
  chrome.on('error', error => { launchError = error.message; });
  chrome.stderr.on('data', data => { launchError = (launchError + data).slice(-2000); });
  try {
    const portFile = path.join(directory, 'DevToolsActivePort');
    for (let i = 0; i < 100 && !fs.existsSync(portFile); i++) {
      if (chrome.exitCode !== null) throw new Error('Chrome exited: ' + launchError);
      await pause(100);
    }
    const port = fs.readFileSync(portFile, 'utf8').split('\n')[0];
    const pages = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json();
    const ws = new WebSocket(pages.find(page => page.type === 'page').webSocketDebuggerUrl);
    await new Promise((resolve, reject) => {
      ws.addEventListener('open', resolve, { once: true });
      ws.addEventListener('error', reject, { once: true });
    });
    let sequence = 0;
    const pending = new Map();
    const handlers = new Map();
    const errors = [];
    ws.addEventListener('message', event => {
      const message = JSON.parse(event.data);
      if (message.method === 'Runtime.exceptionThrown') errors.push(message.params.exceptionDetails.exception?.description || message.params.exceptionDetails.text);
      if (message.method && handlers.has(message.method)) handlers.get(message.method)(message.params);
      const item = pending.get(message.id);
      if (!item) return;
      pending.delete(message.id); clearTimeout(item.timer);
      if (message.error) item.reject(new Error(JSON.stringify(message.error)));
      else item.resolve(message.result);
    });
    function call(method, params = {}) {
      return new Promise((resolve, reject) => {
        const id = ++sequence;
        const timer = setTimeout(() => { pending.delete(id); reject(new Error('Browser command timed out: ' + method)); }, 15000);
        pending.set(id, { resolve, reject, timer });
        ws.send(JSON.stringify({ id, method, params }));
      });
    }
    async function evaluate(expression) {
      const result = await call('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
      if (result.exceptionDetails) throw new Error(result.exceptionDetails.exception?.description || result.exceptionDetails.text);
      return result.result.value;
    }
    async function waitFor(expression, timeout = 15000) {
      const until = Date.now() + timeout;
      while (Date.now() < until) {
        try { if (await evaluate(expression)) return; }
        catch (error) { if (!/context|Cannot find|Cannot read properties of null/.test(error.message)) throw error; }
        await pause(100);
      }
      throw new Error('Timed out waiting for: ' + expression);
    }
    const element = selector => `Array.from(document.querySelectorAll(${JSON.stringify(selector)})).find(e=>{const r=e.getBoundingClientRect();return r.width>0&&r.height>0&&r.bottom>0&&r.top<innerHeight})`;
    async function click(selector) {
      await evaluate(`document.activeElement?.blur();(${element(selector)})?.scrollIntoView({block:'nearest'})`);
      await pause(120);
      await waitFor(`(()=>{const e=${element(selector)};if(!e)return false;const r=e.getBoundingClientRect();const hit=document.elementFromPoint(r.left+r.width/2,r.top+r.height/2);return hit===e||e.contains(hit)})()`);
      const target = await evaluate(`(()=>{const e=${element(selector)};const r=e.getBoundingClientRect();return {x:r.left+r.width/2,y:r.top+r.height/2}})()`);
      await call('Input.dispatchMouseEvent', { type: 'mousePressed', ...target, button: 'left', clickCount: 1 });
      await call('Input.dispatchMouseEvent', { type: 'mouseReleased', ...target, button: 'left', clickCount: 1 });
    }
    async function fill(selector, text) {
      await evaluate(`(()=>{const e=${element(selector)};if(!e)throw Error('Missing input');e.focus();e.value='';e.dispatchEvent(new Event('input',{bubbles:true}))})()`);
      await call('Input.insertText', { text });
    }
    async function shot(file) {
      fs.mkdirSync(path.dirname(file), { recursive: true });
      const result = await call('Page.captureScreenshot', { format: 'png' });
      fs.writeFileSync(file, Buffer.from(result.data, 'base64'));
    }
    async function close() {
      ws.close();
      chrome.kill('SIGTERM');
      for (let i = 0; i < 30 && chrome.exitCode === null; i++) await pause(100);
      if (chrome.exitCode === null) chrome.kill('SIGKILL');
      for (const item of pending.values()) clearTimeout(item.timer);
      fs.rmSync(directory, { recursive: true, force: true });
    }
    await call('Runtime.enable'); await call('Page.enable');
    await call('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: width < 768 });
    await call('Emulation.setGeolocationOverride', { latitude: 30.28, longitude: 120.15, accuracy: 15 });
    return { call, evaluate, waitFor, click, fill, shot, close, element, errors, on: (method, handler) => handlers.set(method, handler) };
  } catch (error) {
    chrome.kill('SIGTERM');
    throw error;
  }
}
