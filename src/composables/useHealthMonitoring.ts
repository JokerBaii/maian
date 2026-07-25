import { ref } from 'vue'
import {
  getHealthMonitoring,
  type HealthMonitoringResponse,
  type WearableDevice
} from '@/api/monitoring'

const monitoring = ref<HealthMonitoringResponse>({
  current: 0,
  min: 0,
  max: 0,
  avg: 0,
  status: 'normal',
  scene: 'resting',
  todayData: [],
  weekData: [],
  monthData: [],
  alerts: [],
  wearable: {
    name: '未绑定设备',
    type: 'none',
    connected: false,
    battery: 0
  }
})

let loadingPromise: Promise<HealthMonitoringResponse> | null = null

export function useHealthMonitoring() {
  async function loadMonitoring(force = false) {
    if (!force && loadingPromise) return loadingPromise
    loadingPromise = getHealthMonitoring()
      .then(result => {
        monitoring.value = result
        return result
      })
      .finally(() => {
        loadingPromise = null
      })
    return loadingPromise
  }

  function updateWearable(wearable: WearableDevice) {
    monitoring.value = {
      ...monitoring.value,
      wearable
    }
  }

  return {
    monitoring,
    loadMonitoring,
    updateWearable
  }
}
