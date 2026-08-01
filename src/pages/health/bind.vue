<template>
  <view class="page">
    <view class="scroll-content">
      <view v-if="boundDevice" class="card bound-card">
        <view class="card-header">
          <view class="card-title-wrap">
            <view class="card-title-bar"></view>
            <text class="card-title">当前设备</text>
          </view>
          <view class="connected-badge">
            <view class="connected-dot"></view>
            <text class="connected-text">已连接</text>
          </view>
          </view>
          <view class="bound-info">
          <view class="bound-left">
            <app-icon-tile name="wearable" tone="coral" status="online" />
            <view class="bound-detail">
              <text class="bound-name">{{ boundDevice.name }}</text>
              <view class="bound-meta">
                <text class="bound-type">蓝牙设备</text>
                <text class="bound-divider">|</text>
                <view v-if="boundDevice.battery !== null" class="battery-mini">
                  <view class="battery-body-mini">
                    <view class="battery-level-mini" :style="{ width: boundDevice.battery + '%' }"></view>
                  </view>
                  <text class="battery-text-mini">{{ boundDevice.battery }}%</text>
                </view>
              </view>
            </view>
          </view>
          <view class="disconnect-btn" @tap="handleDisconnect">
            <text class="disconnect-text">断开</text>
          </view>
        </view>
      </view>

      <view v-if="!boundDevice" class="empty-bound">
        <app-icon-tile
          class="empty-icon"
          name="wearable"
          tone="slate"
          status="offline"
          size="large"
        />
        <text class="empty-title">暂无绑定设备</text>
        <text class="empty-desc">请扫描并绑定可穿戴设备以开始健康监测</text>
      </view>

      <view class="scan-section">
        <view class="scan-btn" :class="{ 'scanning': isScanning }" @tap="toggleScan">
          <view v-if="isScanning" class="scan-animation">
            <view class="scan-ring scan-ring-1"></view>
            <view class="scan-ring scan-ring-2"></view>
            <view class="scan-ring scan-ring-3"></view>
          </view>
          <view class="scan-icon-wrap">
            <app-icon
              v-if="!isScanning"
              class="scan-icon-text"
              name="search"
              :size="22"
              color="#FFFFFF"
            />
            <view v-if="isScanning" class="scan-spinner"></view>
          </view>
          <text class="scan-label">{{ isScanning ? '扫描中...' : '扫描设备' }}</text>
        </view>
      </view>

      <view v-if="discoveredDevices.length" class="card devices-card">
        <view class="card-header">
          <view class="card-title-wrap">
            <view class="card-title-bar"></view>
            <text class="card-title">发现设备</text>
          </view>
          <text class="device-count">{{ discoveredDevices.length }}个设备</text>
        </view>
        <view class="device-list">
          <view
            v-for="device in discoveredDevices"
            :key="device.id"
            class="device-item"
          >
            <view class="device-item-left">
              <app-icon-tile name="wearable" tone="coral" />
              <view class="device-item-info">
                <text class="device-item-name">{{ device.name }}</text>
                <view class="signal-bars">
                  <view
                    v-for="i in 4"
                    :key="i"
                    class="signal-bar"
                    :class="{ 'signal-active': i <= device.signal }"
                  ></view>
                  <text class="signal-text">{{ signalLabel(device.signal) }}</text>
                </view>
              </view>
            </view>
            <view
              class="bind-btn"
              :class="device.binding ? 'bind-btn-loading' : (device.bound ? 'bind-btn-bound' : '')"
              @tap="handleBind(device)"
            >
              <text class="bind-btn-text">{{ bindBtnLabel(device) }}</text>
            </view>
          </view>
        </view>
      </view>

      <view v-if="showBindSuccess" class="success-overlay">
        <view class="success-card">
          <view class="success-icon-wrap">
            <view class="success-check">
              <app-icon class="check-mark" name="checkmarkempty" :size="30" color="#FFFFFF" />
            </view>
          </view>
          <text class="success-title">绑定成功</text>
          <text class="success-desc">已成功连接{{ boundDeviceName }}</text>
          <text class="success-hint">现在可以开始监测心率了</text>
        </view>
      </view>

      <view class="bottom-safe"></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import { useHealthMonitoring } from '@/composables/useHealthMonitoring'
import {
  createHeartRateReading,
  deleteWearableDevice,
  getWearableDevice,
  saveWearableDevice
} from '@/api/monitoring'

const { updateWearable } = useHealthMonitoring()

const boundDevice = ref<{
  name: string
  type: string
  connected: boolean
  battery: number | null
} | null>(null)
const connectedDeviceId = ref('')
let scanTimer: ReturnType<typeof setTimeout> | null = null
let lastReadingUploadAt = 0

const isScanning = ref(false)
const showBindSuccess = ref(false)
const boundDeviceName = ref('')

interface DiscoveredDevice {
  id: string
  name: string
  icon: string
  avatarBg: string
  signal: number
  binding: boolean
  bound: boolean
}

const discoveredDevices = reactive<DiscoveredDevice[]>([])

onMounted(async () => {
  try {
    const device = await getWearableDevice()
    if (device.type !== 'none') {
      connectedDeviceId.value = device.deviceIdentifier || ''
      boundDevice.value = {
        name: device.name,
        type: device.type,
        connected: device.connected,
        battery: device.battery
      }
      updateWearable(device)
    }
  } catch {
    uni.showToast({ title: '绑定设备信息加载失败', icon: 'none' })
  }
})

function toggleScan() {
  if (isScanning.value) {
    stopScan()
    return
  }

  // #ifdef APP-PLUS
  startBluetoothScan()
  // #endif
  // #ifndef APP-PLUS
  uni.showModal({
    title: '请使用移动端',
    content: '浏览器无法稳定访问低功耗蓝牙设备，请在 iOS App 中完成扫描和绑定。',
    showCancel: false,
    confirmText: '知道了'
  })
  // #endif
}

// #ifdef APP-PLUS
function startBluetoothScan() {
  discoveredDevices.length = 0
  uni.openBluetoothAdapter({
    success: async () => {
      uni.onBluetoothDeviceFound(handleBluetoothDevicesFound)
      uni.startBluetoothDevicesDiscovery({
        allowDuplicatesKey: false,
        interval: 500,
        success: () => {
          isScanning.value = true
          scanTimer = setTimeout(stopScan, 10000)
        },
        fail: showBluetoothError
      })
    },
    fail: showBluetoothError
  })
}

function handleBluetoothDevicesFound(result: any) {
  const devices = Array.isArray(result.devices) ? result.devices : []
  devices.forEach((rawDevice: any) => {
    const name = (rawDevice.name || rawDevice.localName || '').trim()
    if (!name || !rawDevice.deviceId) return

    const existing = discoveredDevices.find(device => device.id === rawDevice.deviceId)
    const signal = signalLevel(Number(rawDevice.RSSI))
    if (existing) {
      existing.signal = signal
      return
    }

    discoveredDevices.push({
      id: rawDevice.deviceId,
      name,
      icon: name.charAt(0).toUpperCase(),
      avatarBg: 'linear-gradient(135deg, #2E6DD1 0%, #2E6DD1 100%)',
      signal,
      binding: false,
      bound: false
    })
  })
}

function signalLevel(rssi: number) {
  if (rssi >= -55) return 4
  if (rssi >= -70) return 3
  if (rssi >= -85) return 2
  return 1
}

function showBluetoothError(error: any) {
  stopScan()
  const message = error?.errCode === 10001
    ? '请先开启手机蓝牙'
    : '蓝牙扫描失败，请检查系统权限'
  uni.showToast({ title: message, icon: 'none' })
}

function containsBluetoothUuid(value: string, shortUuid: string) {
  return value.toLowerCase().replace(/-/g, '').includes(shortUuid.toLowerCase())
}

function startHeartRateNotifications(deviceId: string) {
  uni.getBLEDeviceServices({
    deviceId,
    success: (serviceResult: any) => {
      const service = (serviceResult.services || []).find((item: any) => (
        containsBluetoothUuid(item.uuid || '', '180d')
      ))
      if (!service) return
      uni.getBLEDeviceCharacteristics({
        deviceId,
        serviceId: service.uuid,
        success: (characteristicResult: any) => {
          const characteristic = (characteristicResult.characteristics || []).find((item: any) => (
            containsBluetoothUuid(item.uuid || '', '2a37')
            && (item.properties?.notify || item.properties?.indicate)
          ))
          if (!characteristic) return
          uni.onBLECharacteristicValueChange(handleHeartRateValue)
          uni.notifyBLECharacteristicValueChange({
            deviceId,
            serviceId: service.uuid,
            characteristicId: characteristic.uuid,
            state: true
          })
        }
      })
    }
  })
}

function handleHeartRateValue(result: any) {
  if (!result?.value) return
  const view = new DataView(result.value)
  if (view.byteLength < 2) return
  const usesSixteenBits = (view.getUint8(0) & 0x01) === 0x01
  const bpm = usesSixteenBits && view.byteLength >= 3
    ? view.getUint16(1, true)
    : view.getUint8(1)
  const now = Date.now()
  if (bpm < 25 || bpm > 250 || now - lastReadingUploadAt < 15000) return
  lastReadingUploadAt = now
  createHeartRateReading({
    bpm,
    scene: 'resting',
    recordedAt: new Date(now).toISOString()
  }).catch(() => {
    lastReadingUploadAt = 0
  })
}
// #endif

function stopScan() {
  if (scanTimer) {
    clearTimeout(scanTimer)
    scanTimer = null
  }
  isScanning.value = false
  // #ifdef APP-PLUS
  uni.stopBluetoothDevicesDiscovery({})
  // #endif
}

function signalLabel(level: number) {
  if (level >= 4) return '信号强'
  if (level >= 3) return '信号良'
  if (level >= 2) return '信号中'
  return '信号弱'
}

function bindBtnLabel(device: DiscoveredDevice) {
  if (device.binding) return '连接中...'
  if (device.bound) return '已绑定'
  return '绑定'
}

function handleBind(device: DiscoveredDevice) {
  if (device.bound || device.binding) return
  device.binding = true

  // #ifdef APP-PLUS
  stopScan()
  uni.createBLEConnection({
    deviceId: device.id,
    timeout: 10000,
    success: async () => {
      device.binding = false
      device.bound = true
      connectedDeviceId.value = device.id
      boundDeviceName.value = device.name
      boundDevice.value = {
        name: device.name,
        type: 'bluetooth',
        connected: true,
        battery: null
      }
      updateWearable({
        name: device.name,
        type: 'bluetooth',
        connected: true,
        battery: 0
      })
      try {
        const saved = await saveWearableDevice({
          deviceIdentifier: device.id,
          name: device.name,
          type: 'bluetooth',
          connected: true,
          battery: 0
        })
        updateWearable(saved)
        // #ifdef APP-PLUS
        startHeartRateNotifications(device.id)
        // #endif
      } catch {
        uni.showToast({ title: '设备已连接，但同步绑定信息失败', icon: 'none' })
      }
      showBindSuccess.value = true
      setTimeout(() => {
        showBindSuccess.value = false
      }, 1800)
    },
    fail: () => {
      device.binding = false
      uni.showToast({ title: '连接失败，请靠近设备后重试', icon: 'none' })
    }
  })
  // #endif
  // #ifndef APP-PLUS
  device.binding = false
  uni.showToast({ title: '请在 iOS App 中绑定设备', icon: 'none' })
  // #endif
}

function handleDisconnect() {
  uni.showModal({
    title: '断开设备',
    content: '确定要断开当前设备连接吗？',
    confirmColor: '#C93D46',
    success: (res) => {
      if (res.confirm) {
        const clearDevice = async () => {
          discoveredDevices.forEach(device => {
            device.bound = false
            device.binding = false
          })
          connectedDeviceId.value = ''
          boundDevice.value = null
          updateWearable({
            name: '未绑定设备',
            type: 'none',
            connected: false,
            battery: 0
          })
          try {
            await deleteWearableDevice()
          } catch {
            uni.showToast({ title: '设备已断开，服务端解绑失败', icon: 'none' })
          }
        }
        // #ifdef APP-PLUS
        if (connectedDeviceId.value) {
          uni.closeBLEConnection({
            deviceId: connectedDeviceId.value,
            complete: clearDevice
          })
        } else {
          clearDevice()
        }
        // #endif
        // #ifndef APP-PLUS
        clearDevice()
        // #endif
      }
    }
  })
}

onUnmounted(stopScan)

</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F3F7FA;
}

.scroll-content {
  min-height: 100vh;
  box-sizing: border-box;
}

.card {
  margin: 24rpx 24rpx 0;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.06);
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}
.card-title-wrap {
  display: flex;
  align-items: center;
}
.card-title-bar {
  width: 6rpx;
  height: 28rpx;
  border-radius: 3rpx;
  background: #2E6DD1;
  margin-right: 12rpx;
}
.card-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #20364D;
}

.connected-badge {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  background: rgba(0, 180, 42, 0.08);
}
.connected-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #23956A;
  box-shadow: 0 0 8rpx rgba(0, 180, 42, 0.4);
}
.connected-text {
  font-size: 22rpx;
  color: #23956A;
  font-weight: 600;
}
.bound-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.bound-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.bound-detail {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.bound-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #20364D;
}
.bound-meta {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.bound-type {
  font-size: 22rpx;
  color: #86909C;
}
.bound-divider {
  font-size: 22rpx;
  color: #E5E6EB;
}
.battery-mini {
  display: flex;
  align-items: center;
  gap: 6rpx;
}
.battery-body-mini {
  width: 36rpx;
  height: 16rpx;
  border-radius: 3rpx;
  border: 2rpx solid #86909C;
  padding: 2rpx;
  box-sizing: border-box;
}
.battery-level-mini {
  height: 100%;
  border-radius: 1rpx;
  background: #23956A;
}
.battery-text-mini {
  font-size: 20rpx;
  color: #86909C;
}
.disconnect-btn {
  padding: 12rpx 28rpx;
  border-radius: 24rpx;
  border: 2rpx solid #C93D46;
  background: rgba(245, 63, 63, 0.04);
}
.disconnect-text {
  font-size: 24rpx;
  color: #C93D46;
  font-weight: 600;
}

.empty-bound {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 64rpx 32rpx 32rpx;
}
.empty-icon {
  margin-bottom: 20rpx;
}
.empty-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #4E5969;
  margin-bottom: 8rpx;
}
.empty-desc {
  font-size: 24rpx;
  color: #C9CDD4;
  text-align: center;
}

.scan-section {
  display: flex;
  justify-content: center;
  padding: 40rpx 0;
}
.scan-btn {
  position: relative;
  width: 240rpx;
  height: 240rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #2E6DD1 0%, #2E6DD1 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  box-shadow: 0 8rpx 40rpx rgba(43, 111, 240, 0.35);
  transition: all 0.3s ease;
  z-index: 2;
}
.scan-btn.scanning {
  box-shadow: 0 8rpx 48rpx rgba(43, 111, 240, 0.5);
}
.scan-animation {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}
.scan-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  border: 3rpx solid rgba(43, 111, 240, 0.25);
  animation: scanPulse 2s ease-out infinite;
}
.scan-ring-1 {
  width: 280rpx;
  height: 280rpx;
  animation-delay: 0s;
}
.scan-ring-2 {
  width: 340rpx;
  height: 340rpx;
  animation-delay: 0.4s;
}
.scan-ring-3 {
  width: 400rpx;
  height: 400rpx;
  animation-delay: 0.8s;
}
@keyframes scanPulse {
  0% {
    transform: translate(-50%, -50%) scale(0.85);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -50%) scale(1.2);
    opacity: 0;
  }
}
.scan-icon-wrap {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.scan-icon-text {
  font-size: 48rpx;
  color: #FFFFFF;
}
.scan-spinner {
  width: 48rpx;
  height: 48rpx;
  border: 4rpx solid rgba(255, 255, 255, 0.3);
  border-top-color: #FFFFFF;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.scan-label {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
  letter-spacing: 2rpx;
}

.device-count {
  font-size: 22rpx;
  color: #86909C;
}
.device-list {
  display: flex;
  flex-direction: column;
}
.device-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #F2F3F5;
}
.device-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.device-item-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.device-item-info {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.device-item-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #20364D;
}
.signal-bars {
  display: flex;
  align-items: flex-end;
  gap: 4rpx;
}
.signal-bar {
  width: 8rpx;
  border-radius: 2rpx;
  background: #E5E6EB;
}
.signal-bar:nth-child(1) { height: 10rpx; }
.signal-bar:nth-child(2) { height: 16rpx; }
.signal-bar:nth-child(3) { height: 22rpx; }
.signal-bar:nth-child(4) { height: 28rpx; }
.signal-active {
  background: #23956A;
}
.signal-text {
  font-size: 20rpx;
  color: #86909C;
  margin-left: 6rpx;
}
.bind-btn {
  padding: 10rpx 28rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #2E6DD1 0%, #2E6DD1 100%);
  box-shadow: 0 4rpx 12rpx rgba(43, 111, 240, 0.2);
  transition: all 0.3s ease;
}
.bind-btn-loading {
  background: #E5E6EB;
  box-shadow: none;
}
.bind-btn-bound {
  background: rgba(0, 180, 42, 0.08);
  box-shadow: none;
}
.bind-btn-text {
  font-size: 24rpx;
  font-weight: 600;
  color: #FFFFFF;
}
.bind-btn-loading .bind-btn-text {
  color: #86909C;
}
.bind-btn-bound .bind-btn-text {
  color: #23956A;
}

.success-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(6px);
  animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
.success-card {
  width: 520rpx;
  background: #FFFFFF;
  border-radius: 32rpx;
  padding: 64rpx 48rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  animation: slideUp 0.4s ease;
}
@keyframes slideUp {
  from {
    transform: translateY(60rpx);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}
.success-icon-wrap {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #23956A 0%, #4DC580 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 32rpx rgba(0, 180, 42, 0.3);
  margin-bottom: 32rpx;
  animation: successPop 0.5s ease 0.2s both;
}
@keyframes successPop {
  0% { transform: scale(0); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}
.success-check {
  display: flex;
  align-items: center;
  justify-content: center;
}
.check-mark {
  font-size: 56rpx;
  color: #FFFFFF;
  font-weight: 700;
}
.success-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #20364D;
  margin-bottom: 12rpx;
}
.success-desc {
  font-size: 26rpx;
  color: #4E5969;
  text-align: center;
  margin-bottom: 8rpx;
}
.success-hint {
  font-size: 22rpx;
  color: #C9CDD4;
}

.bottom-safe {
  height: 60rpx;
}
</style>
