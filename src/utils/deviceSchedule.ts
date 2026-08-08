export type WeekDay = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export interface DeviceServiceWindow {
  dayOfWeek: WeekDay
  opensAt: string
  closesAt: string
}

const ALL_DAYS: WeekDay[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

export function toServiceWindows(serviceTime?: string): DeviceServiceWindow[] {
  const value = serviceTime?.trim() || '全天'
  const days = value.includes('工作日') ? ALL_DAYS.slice(0, 5) : ALL_DAYS
  if (value.includes('全天')) {
    return days.map(dayOfWeek => ({ dayOfWeek, opensAt: '00:00', closesAt: '00:00' }))
  }
  const match = value.match(/(\d{1,2}:\d{2})\s*[-–至]\s*(\d{1,2}:\d{2})/)
  if (!match) throw new Error('服务时段格式无效')
  const normalize = (time: string) => time.padStart(5, '0')
  return days.map(dayOfWeek => ({
    dayOfWeek,
    opensAt: normalize(match[1]),
    closesAt: normalize(match[2])
  }))
}
