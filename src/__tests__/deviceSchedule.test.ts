import { describe, expect, it } from 'vitest'
import { toServiceWindows } from '../utils/deviceSchedule'

describe('device service schedule conversion', () => {
  it('turns all-day service into seven structured windows', () => {
    const windows = toServiceWindows('全天')
    expect(windows).toHaveLength(7)
    expect(windows[0]).toMatchObject({ dayOfWeek: 'MONDAY', opensAt: '00:00', closesAt: '00:00' })
  })

  it('limits workday schedules to Monday through Friday', () => {
    const windows = toServiceWindows('工作日 09:00-18:00')
    expect(windows.map(item => item.dayOfWeek)).toEqual([
      'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'
    ])
  })

  it('rejects unparseable free text instead of dispatching by mistake', () => {
    expect(() => toServiceWindows('大概白天')).toThrow('服务时段格式无效')
  })

  it('supports the weekend option shown by device registration and timed weekend service', () => {
    expect(toServiceWindows('周末')).toEqual([
      { dayOfWeek: 'SATURDAY', opensAt: '00:00', closesAt: '00:00' },
      { dayOfWeek: 'SUNDAY', opensAt: '00:00', closesAt: '00:00' }
    ])
    expect(toServiceWindows('周末 9:00-17:00')).toEqual([
      { dayOfWeek: 'SATURDAY', opensAt: '09:00', closesAt: '17:00' },
      { dayOfWeek: 'SUNDAY', opensAt: '09:00', closesAt: '17:00' }
    ])
  })
})
