import { afterEach, describe, expect, it, vi } from 'vitest'
import { createAssistantSpeech, type SpeechEngine } from '../composables/useAssistantSpeech'
function setup() {
  let owner='account'
  const engine:SpeechEngine={lang:'',continuous:true,interimResults:false,maxAlternatives:0,onresult:null,onerror:null,onend:null,start:vi.fn(),stop:vi.fn(),abort:vi.fn()}
  const submit=vi.fn(),speech=createAssistantSpeech(submit,()=>engine,()=>owner)
  const result=(text:string,final=true)=>engine.onresult?.({results:[{isFinal:final,0:{transcript:text}}]})
  return {speech,engine,submit,result,changeOwner:()=>{owner='other'}}
}
afterEach(()=>vi.useRealTimers())
describe('assistant speech',()=>{
  it('previews interim speech and submits final text once when recognition ends',()=>{
    const t=setup();t.speech.start();expect(t.engine.lang).toBe('zh-CN')
    t.result('添加',false);expect(t.speech.transcript.value).toBe('添加');expect(t.submit).not.toHaveBeenCalled()
    t.result('添加紧急联系人');t.engine.onend?.();t.engine.onend?.()
    expect(t.submit).toHaveBeenCalledOnce();expect(t.submit).toHaveBeenCalledWith('添加紧急联系人');expect(t.speech.listening.value).toBe(false)
  })
  it('stop waits for a final result instead of submitting incomplete words',()=>{
    const t=setup();t.speech.start();t.result('关闭',false);t.speech.stop();expect(t.submit).not.toHaveBeenCalled()
    t.result('关闭科普更新');t.engine.onend?.();expect(t.submit).toHaveBeenCalledWith('关闭科普更新')
  })
  it('cancel ignores late provider results',()=>{
    const t=setup();t.speech.start();const callback=t.engine.onresult,end=t.engine.onend
    t.speech.cancel();callback?.({results:[{isFinal:true,0:{transcript:'删除设备'}}]});end?.()
    expect(t.submit).not.toHaveBeenCalled();expect(t.engine.abort).toHaveBeenCalledOnce()
  })
  it('aborts on account change and rejects a late result',()=>{
    vi.useFakeTimers();const t=setup();t.speech.start();t.changeOwner();vi.advanceTimersByTime(250)
    expect(t.engine.abort).toHaveBeenCalledOnce();expect(t.speech.listening.value).toBe(false);expect(t.submit).not.toHaveBeenCalled()
  })
  it.each(['not-allowed','network','no-speech'])('keeps %s failures out of the task executor',error=>{
    const t=setup();t.speech.start();t.engine.onerror?.({error});t.engine.onend?.()
    expect(t.speech.error.value).toBeTruthy();expect(t.submit).not.toHaveBeenCalled();expect(t.speech.listening.value).toBe(false)
  })
  it('times out without submitting an unfinished or excessively long command',()=>{
    vi.useFakeTimers();const t=setup();t.speech.start();vi.advanceTimersByTime(50000)
    expect(t.speech.listening.value).toBe(false);expect(t.submit).not.toHaveBeenCalled()
    t.speech.start();t.result('字'.repeat(301));t.engine.onend?.();expect(t.submit).not.toHaveBeenCalled();expect(t.speech.error.value).toContain('太长')
  })
})
