import { ref } from 'vue'
import { request } from '@/api/http'
import { getAccountAccessToken } from '@/utils/authSession'

export interface AssistantReminder { id:string; title:string; kind:string; status:'WAITING'|'READY'; result?:string; dueAt?:string }
export function createReminderNotice(dependencies = { request, identity:getAccountAccessToken }) {
  const count = ref(0)
  let owner = '', revision = 0, lastRead = 0
  async function refresh(force = false) {
    const identity=dependencies.identity()
    if(identity!==owner){owner=identity;revision++;lastRead=0;count.value=0}
    if(!identity || (!force && Date.now()-lastRead<15000))return
    const run=++revision
    lastRead=Date.now()
    try {
      const reminders=await dependencies.request<AssistantReminder[]>('/api/v1/assistant/reminders')
      if(run===revision && identity===dependencies.identity())count.value=reminders.filter(r=>r.status==='READY').length
    } catch { /* A failed reminder poll must not interrupt any business operation. */ }
  }
  return { count, refresh }
}
export const assistantReminderNotice=createReminderNotice()
let timer:ReturnType<typeof setInterval>|undefined, registered=false
export function startAssistantReminders(){
  if(!timer)timer=setInterval(()=>void assistantReminderNotice.refresh(),30000)
  if(!registered){registered=true;uni.$on('maian:agent-executed',()=>void assistantReminderNotice.refresh(true))}
  void assistantReminderNotice.refresh()
}
export function stopAssistantReminders(){clearInterval(timer);timer=undefined}
