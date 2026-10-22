import type { CoordinationTask } from '@/api/agent'
import type { RescueCallResponse } from '@/api/rescue'

type RescueState = Pick<RescueCallResponse, 'status' | 'aedCustodyStatus'>
const terminalStatuses = ['COMPLETED', 'NO_RESOURCE', 'EXPIRED', 'USER_CANCELLED', 'SYSTEM_FAILED']

export function createRescueCoordinationPolling(
  fetchTasks: () => Promise<CoordinationTask[]>,
  publish: (tasks: CoordinationTask[]) => void,
  getRescue: () => RescueState | null
) {
  let active = false
  let generation = 0
  let timer: ReturnType<typeof setTimeout> | null = null

  async function refresh(run: number) {
    let again = true
    const initial = getRescue()
    const wasEnded = initial && terminalStatuses.includes(initial.status)
    try {
      const tasks = await fetchTasks()
      if (!active || run !== generation) return
      publish(tasks)
      const rescue = getRescue()
      const ended = rescue && terminalStatuses.includes(rescue.status)
      // A request begun before the terminal transition may predate the last coordination event.
      again = !wasEnded || !ended || rescue.aedCustodyStatus === 'RETURNING' || tasks.some(task => task.status !== 'COMPLETED')
    } catch {
      // A failed supplemental request must remain retryable after rescue polling stops.
    }
    if (!active || run !== generation) return
    if (again) timer = setTimeout(() => { timer = null; void refresh(run) }, 3000)
    else active = false
  }

  return {
    start() {
      if (active) return
      active = true
      void refresh(++generation)
    },
    stop() {
      active = false
      generation++
      if (timer) clearTimeout(timer)
      timer = null
    }
  }
}
