import { onAccountSessionChange } from '@/utils/authSession'

export type BleLifecycleState =
  | 'IDLE' | 'ADAPTER_OPEN' | 'SCANNING' | 'CONNECTING' | 'CONNECTED' | 'NOTIFYING'

type NotificationTarget = { deviceId: string; serviceId: string; characteristicId: string }


class BleHeartRateService {
  private stateValue: BleLifecycleState = 'IDLE'
  private discoveredHandler: ((result: any) => void) | null = null
  private valueHandler: ((result: any) => void) | null = null
  private notificationTarget: NotificationTarget | null = null
  private connectedDeviceId = ''

  get state() { return this.stateValue }

  registerDiscovery(handler: (result: any) => void) {
    this.unregisterDiscovery()
    const activeHandler = (result: any) => {
      if (this.discoveredHandler === activeHandler) handler(result)
    }
    this.discoveredHandler = activeHandler
    uni.onBluetoothDeviceFound(activeHandler)
  }

  markAdapterOpen() { this.stateValue = 'ADAPTER_OPEN' }
  markScanning() { this.stateValue = 'SCANNING' }
  markConnecting() { this.stateValue = 'CONNECTING' }

  markConnected(deviceId: string) {
    this.connectedDeviceId = deviceId
    this.stateValue = 'CONNECTED'
  }

  registerHeartRateNotification(
    target: NotificationTarget,
    handler: (result: any) => void
  ) {
    this.unregisterHeartRateListener()
    this.notificationTarget = target
    const activeHandler = (result: any) => {
      if (this.valueHandler === activeHandler) handler(result)
    }
    this.valueHandler = activeHandler
    uni.onBLECharacteristicValueChange(activeHandler)
    this.stateValue = 'NOTIFYING'
  }

  stopDiscovery() {
    if (this.stateValue === 'SCANNING') {
      ;(uni as any).stopBluetoothDevicesDiscovery?.({ complete: () => undefined })
    }
    this.unregisterDiscovery()
    if (this.stateValue === 'SCANNING') this.stateValue = 'ADAPTER_OPEN'
  }

  disconnect(complete?: () => void) {
    const deviceId = this.connectedDeviceId
    const adapterWasOpen = this.stateValue !== 'IDLE'
    this.stopDiscovery()
    const target = this.notificationTarget
    if (target) {
      ;(uni as any).notifyBLECharacteristicValueChange?.({
        ...target,
        state: false,
        complete: () => undefined
      })
    }
    this.unregisterHeartRateListener()
    this.notificationTarget = null
    const finish = () => {
      this.connectedDeviceId = ''
      this.stateValue = adapterWasOpen ? 'ADAPTER_OPEN' : 'IDLE'
      complete?.()
    }
    if (deviceId) {
      uni.closeBLEConnection({ deviceId, complete: finish })
    } else {
      finish()
    }
  }


  dispose() {
    this.disconnect(() => {
      ;(uni as any).closeBluetoothAdapter?.({ complete: () => undefined })
      this.stateValue = 'IDLE'
    })
  }

  private unregisterDiscovery() {
    if (!this.discoveredHandler) return
    ;(uni as any).offBluetoothDeviceFound?.(this.discoveredHandler)
    this.discoveredHandler = null
  }

  private unregisterHeartRateListener() {
    if (!this.valueHandler) return
    ;(uni as any).offBLECharacteristicValueChange?.(this.valueHandler)
    this.valueHandler = null
  }
}

export const bleHeartRateService = new BleHeartRateService()
onAccountSessionChange(() => {
  if (bleHeartRateService.state !== 'IDLE') bleHeartRateService.dispose()
})
