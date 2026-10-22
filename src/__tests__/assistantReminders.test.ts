import { describe,expect,it,vi } from 'vitest'
import { createReminderNotice } from '../composables/useAssistantReminders'

describe('account-isolated reminder notice',()=>{
  it('drops a late poll from the previous account',async()=>{
    let owner='first',resolve!:(data:any)=>void
    const request=vi.fn().mockImplementationOnce(()=>new Promise(done=>{resolve=done})).mockResolvedValueOnce([])
    const notice=createReminderNotice({request,identity:()=>owner})
    const first=notice.refresh(true);owner='second';await notice.refresh(true)
    resolve([{status:'READY'}]);await first;expect(notice.count.value).toBe(0)
  })
  it('counts only ready reminders and clears them at logout',async()=>{
    let owner='first';const request=vi.fn().mockResolvedValue([{status:'WAITING'},{status:'READY'}])
    const notice=createReminderNotice({request,identity:()=>owner});await notice.refresh(true);expect(notice.count.value).toBe(1)
    owner='';await notice.refresh();expect(notice.count.value).toBe(0);expect(request).toHaveBeenCalledOnce()
  })
})
