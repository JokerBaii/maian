export type BleLifecycleState =
  | 'IDLE' | 'ADAPTER_OPEN' | 'SCANNING' | 'CONNECTING' | 'CONNECTED' | 'NOTIFYING'

type NotificationTarget = { deviceId: string; serviceId: string; characteristicId: string }

/** App 级 BLE 生命周期：任何时刻只保留一组扫描/心率监听器。 */
class BleHeartRateService {
  private stateValue: BleLifecycleState = 'IDLE'
  private discoveredHandler: ((result: any) => void) | null = null
  private valueHandler: ((result: any) => void) | null = null
  private notificationTarget: NotificationTarget | null = null
  private connectedDeviceId = ''

  get state() { return this.stateValue }

  registerDiscovery(handler: (result: any) => void) {
    this.unregisterDiscovery()
    this.discoveredHandler = handler
    uni.onBluetoothDeviceFound(handler)
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
    this.valueHandler = handler
    uni.onBLECharacteristicValueChange(handler)
    this.stateValue = 'NOTIFYING'
  }

  stopDiscovery() {
    ;(uni as any).stopBluetoothDevicesDiscovery?.({ complete: () => undefined })
    this.unregisterDiscovery()
    if (this.stateValue === 'SCANNING') this.stateValue = 'ADAPTER_OPEN'
  }

  disconnect(deviceId = this.connectedDeviceId, complete?: () => void) {
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
      this.stateValue = 'ADAPTER_OPEN'
      complete?.()
    }
    if (deviceId) {
      uni.closeBLEConnection({ deviceId, complete: finish })
    } else {
      finish()
    }
  }

  /** 彻底释放适配器，用于退出登录或 App 级停止监测。 */
  dispose() {
    this.disconnect(this.connectedDeviceId, () => {
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
