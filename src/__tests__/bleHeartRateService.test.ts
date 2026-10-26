import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

beforeEach(() => {
  vi.resetModules()
  const data = new Map<string, unknown>()
  vi.stubGlobal('uni', {
    getStorageSync: (key: string) => data.get(key),
    setStorageSync: (key: string, value: unknown) => data.set(key, value),
    removeStorageSync: (key: string) => data.delete(key),
    onBluetoothDeviceFound: vi.fn(), offBluetoothDeviceFound: vi.fn(),
    onBLECharacteristicValueChange: vi.fn(), offBLECharacteristicValueChange: vi.fn(),
    stopBluetoothDevicesDiscovery: vi.fn(), notifyBLECharacteristicValueChange: vi.fn(),
    closeBLEConnection: vi.fn(({ complete }) => complete?.()),
    closeBluetoothAdapter: vi.fn()
  })
})
afterEach(() => vi.unstubAllGlobals())

describe('native BLE cleanup', () => {
  it('clears a persisted binding without inventing a local Bluetooth connection', async () => {
    const { bleHeartRateService: service } = await import('../services/bleHeartRateService')
    const clearBinding = vi.fn()
    service.disconnect(clearBinding)
    expect(uni.closeBLEConnection).not.toHaveBeenCalled()
    expect(clearBinding).toHaveBeenCalledOnce()
    expect(service.state).toBe('IDLE')
  })

  it('removes subscriptions and invokes the disconnect completion once', async () => {
    const { bleHeartRateService: service } = await import('../services/bleHeartRateService')
    const discovery = vi.fn(), reading = vi.fn(), clearBinding = vi.fn()
    service.registerDiscovery(discovery)
    service.markConnected('watch-a')
    service.registerHeartRateNotification({ deviceId: 'watch-a', serviceId: '180d', characteristicId: '2a37' }, reading)
    const nativeDiscovery = vi.mocked(uni.onBluetoothDeviceFound).mock.calls[0][0]
    const nativeReading = vi.mocked(uni.onBLECharacteristicValueChange).mock.calls[0][0]
    service.disconnect(clearBinding)
    expect(uni.offBluetoothDeviceFound).toHaveBeenCalledWith(nativeDiscovery)
    expect(uni.offBLECharacteristicValueChange).toHaveBeenCalledWith(nativeReading)
    nativeDiscovery({ devices: [] })
    nativeReading({ deviceId: 'watch-a', serviceId: '180d', characteristicId: '2a37', value: [] })
    expect(discovery).not.toHaveBeenCalled()
    expect(reading).not.toHaveBeenCalled()
    expect(uni.notifyBLECharacteristicValueChange).toHaveBeenCalledWith(expect.objectContaining({ deviceId: 'watch-a', state: false }))
    expect(uni.closeBLEConnection).toHaveBeenCalledOnce()
    expect(clearBinding).toHaveBeenCalledOnce()
  })

  it('stops the old account wearable on logout and does not close it again on repeated logout', async () => {
    const session = await import('../utils/authSession')
    session.saveAccountSession('account-a', new Date(Date.now() + 3600_000).toISOString())
    const { bleHeartRateService: service } = await import('../services/bleHeartRateService')
    const reading = vi.fn()
    service.markConnected('watch-a')
    service.registerHeartRateNotification({ deviceId: 'watch-a', serviceId: '180d', characteristicId: '2a37' }, reading)
    const nativeReading = vi.mocked(uni.onBLECharacteristicValueChange).mock.calls[0][0]
    const packet = { deviceId: 'watch-a', serviceId: '180d', characteristicId: '2a37', value: [] }
    nativeReading(packet)
    expect(reading).toHaveBeenCalledOnce()
    reading.mockClear()
    session.clearAccountSession()
    session.clearAccountSession()
    nativeReading(packet)
    expect(reading).not.toHaveBeenCalled()
    expect(uni.offBLECharacteristicValueChange).toHaveBeenCalledWith(nativeReading)
    expect(uni.closeBLEConnection).toHaveBeenCalledOnce()
    expect(service.state).toBe('IDLE')
  })
})
