import { ref } from 'vue'
import { getAccountAccessToken, onAccountSessionChange } from '@/utils/authSession'
import {
  getHealthMonitoring,
  type HealthMonitoringResponse,
  type WearableDevice
} from '@/api/monitoring'

function emptyMonitoring(): HealthMonitoringResponse {
  return {
    current: 0,
    min: 0,
    max: 0,
    avg: 0,
    status: 'normal',
    scene: 'resting',
    todayData: [],
    hourData: [],
    weekData: [],
    monthData: [],
    alerts: [],
    wearable: {
      name: '未绑定设备',
      type: 'none',
      connected: false,
      battery: 0
    },
    minHeartRate: 50,
    maxHeartRate: 120
  }
}

const monitoring = ref(emptyMonitoring())

let loadingPromise: Promise<HealthMonitoringResponse> | null = null
let generation = 0
onAccountSessionChange(() => {
  generation++
  loadingPromise = null
  monitoring.value = emptyMonitoring()
})

export function useHealthMonitoring() {
  async function loadMonitoring(force = false) {
    getAccountAccessToken()
    if (!force && loadingPromise) return loadingPromise
    const current = ++generation
    const pending = getHealthMonitoring()
      .then(result => {
        if (current === generation) monitoring.value = result
        return result
      })
      .finally(() => {
        if (loadingPromise === pending) loadingPromise = null
      })
    loadingPromise = pending
    return pending
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
