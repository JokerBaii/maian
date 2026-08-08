<template>
  <view class="page apple-page motion-page-sheet">
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

      <view v-if="boundDevice" class="card telemetry-card">
        <view class="telemetry-heading">
          <view>
            <text class="telemetry-kicker">今日监测</text>
            <text class="telemetry-title">生命信号持续同步</text>
          </view>
          <view class="telemetry-live">
            <view class="telemetry-live-dot"></view>
            <text>{{ boundDevice.connected ? '同步中' : '已暂停' }}</text>
          </view>
        </view>

        <view class="pulse-reading">
          <view class="pulse-wave" aria-hidden="true">
            <view class="pulse-line"></view>
          </view>
          <view class="pulse-value-wrap">
            <text class="pulse-value">{{ heartRateData.current || '--' }}</text>
            <text class="pulse-unit">BPM</text>
          </view>
          <text class="pulse-scene">{{ currentSceneLabel }}</text>
        </view>

        <view class="telemetry-metrics">
          <view class="telemetry-metric">
            <text class="telemetry-value">{{ todaySampleCount }}</text>
            <text class="telemetry-label">今日采样</text>
          </view>
          <view class="telemetry-divider"></view>
          <view class="telemetry-metric">
            <text class="telemetry-value">{{ todayRange }}</text>
            <text class="telemetry-label">心率区间</text>
          </view>
          <view class="telemetry-divider"></view>
          <view class="telemetry-metric">
            <text class="telemetry-value">{{ heartRateData.avg || '--' }}</text>
            <text class="telemetry-label">今日平均</text>
          </view>
        </view>

        <view class="sync-row">
          <text>最近同步</text>
          <text class="sync-value">{{ lastSyncLabel }}</text>
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
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import { useHealthMonitoring } from '@/composables/useHealthMonitoring'
import {
  createHeartRateReading,
  deleteWearableDevice,
  getWearableDevice,
  saveWearableDevice
} from '@/api/monitoring'
import { bleHeartRateService } from '@/services/bleHeartRateService'
import { parseHeartRateMeasurement } from '@/utils/heartRateMeasurement'

const { monitoring: heartRateData, loadMonitoring, updateWearable } = useHealthMonitoring()

const boundDevice = ref<{
  name: string
  type: string
  connected: boolean
  battery: number | null
  lastSeenAt: string | null
} | null>(null)
const connectedDeviceId = ref('')
let scanTimer: ReturnType<typeof setTimeout> | null = null
let lastReadingUploadAt = 0

const isScanning = ref(false)
const showBindSuccess = ref(false)
const boundDeviceName = ref('')

const todaySampleCount = computed(() => heartRateData.value.todayData.length)
const todayRange = computed(() => {
  if (!heartRateData.value.todayData.length) return '--'
  return `${heartRateData.value.min}–${heartRateData.value.max}`
})
const currentSceneLabel = computed(() => {
  const labels: Record<string, string> = {
    resting: '静息',
    exercise: '运动',
    sleeping: '睡眠'
  }
  return labels[heartRateData.value.scene] || '实时'
})
const lastSyncLabel = computed(() => {
  const value = boundDevice.value?.lastSeenAt
  if (!value) return '等待首次同步'
  const timestamp = new Date(value).getTime()
  if (!Number.isFinite(timestamp)) return '已同步'
  const elapsedMinutes = Math.max(0, Math.floor((Date.now() - timestamp) / 60000))
  if (elapsedMinutes < 1) return '刚刚'
  if (elapsedMinutes < 60) return `${elapsedMinutes} 分钟前`
  const date = new Date(timestamp)
  return `${date.getMonth() + 1}月${date.getDate()}日 ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
})

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
  const [deviceResult] = await Promise.allSettled([
    getWearableDevice(),
    loadMonitoring(true)
  ])
  if (deviceResult.status === 'fulfilled') {
    const device = deviceResult.value
    if (device.type !== 'none') {
      connectedDeviceId.value = device.deviceIdentifier || ''
      boundDevice.value = {
        name: device.name,
        type: device.type,
        connected: device.connected,
        battery: device.battery,
        lastSeenAt: device.lastSeenAt || null
      }
      updateWearable(device)
    }
  } else {
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
    title: '请使用 iOS 客户端连接',
    content: '网页端无法直接访问低功耗蓝牙。请在 iPhone 上打开脉安驰援并靠近手环完成连接。',
    confirmText: '我知道了',
    showCancel: false
  })
  // #endif
}

// #ifdef APP-PLUS
function startBluetoothScan() {
  discoveredDevices.length = 0
  uni.openBluetoothAdapter({
    success: async () => {
      bleHeartRateService.markAdapterOpen()
      bleHeartRateService.registerDiscovery(handleBluetoothDevicesFound)
      uni.startBluetoothDevicesDiscovery({
        allowDuplicatesKey: false,
        interval: 500,
        success: () => {
          isScanning.value = true
          bleHeartRateService.markScanning()
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
      avatarBg: '#007AFF',
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
          bleHeartRateService.registerHeartRateNotification({
            deviceId,
            serviceId: service.uuid,
            characteristicId: characteristic.uuid
          }, handleHeartRateValue)
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
  const bpm = parseHeartRateMeasurement(result.value)
  if (bpm == null) return
  const now = Date.now()
  if (now - lastReadingUploadAt < 15000) return
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
  bleHeartRateService.stopDiscovery()
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
  bleHeartRateService.markConnecting()
  uni.createBLEConnection({
    deviceId: device.id,
    timeout: 10000,
    success: async () => {
      device.binding = false
      device.bound = true
      connectedDeviceId.value = device.id
      bleHeartRateService.markConnected(device.id)
      boundDeviceName.value = device.name
      boundDevice.value = {
        name: device.name,
        type: 'bluetooth',
        connected: true,
        battery: null,
        lastSeenAt: new Date().toISOString()
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
            uni.showToast({ title: '设备已断开，请稍后确认解绑状态', icon: 'none' })
          }
        }
        // #ifdef APP-PLUS
        if (connectedDeviceId.value) {
          bleHeartRateService.disconnect(connectedDeviceId.value, clearDevice)
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

onUnmounted(() => {
  stopScan()
  // 心率通知由 App 级单例继续接收；扫描监听会随页面释放。
})

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

.telemetry-card {
  border: 1rpx solid rgba(60, 60, 67, .14);
}

.telemetry-heading,
.pulse-reading,
.telemetry-metrics,
.sync-row {
  display: flex;
  align-items: center;
}

.telemetry-heading,
.sync-row {
  justify-content: space-between;
}

.telemetry-kicker,
.telemetry-title {
  display: block;
}

.telemetry-kicker {
  color: #8E8E93;
  font-size: 19rpx;
  font-weight: 750;
  letter-spacing: 2rpx;
}

.telemetry-title {
  margin-top: 4rpx;
  color: #20364D;
  font-size: 29rpx;
  font-weight: 780;
}

.telemetry-live {
  display: flex;
  align-items: center;
  gap: 8rpx;
  color: #23865F;
  font-size: 21rpx;
  font-weight: 700;
}

.telemetry-live-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #28A474;
  box-shadow: 0 0 0 7rpx rgba(40, 164, 116, .1);
  animation: telemetryPulse 2.2s ease-in-out infinite;
}

.pulse-reading {
  min-height: 108rpx;
  margin-top: 24rpx;
  padding: 0 20rpx;
  border-radius: 18rpx;
  background: #F6F8FB;
}

.pulse-wave {
  position: relative;
  width: 92rpx;
  height: 44rpx;
  overflow: hidden;
}

.pulse-line {
  position: absolute;
  top: 20rpx;
  left: 0;
  width: 92rpx;
  height: 2rpx;
  background: #D9E2EA;
}

.pulse-line::after {
  content: '';
  position: absolute;
  top: -18rpx;
  left: 16rpx;
  width: 52rpx;
  height: 36rpx;
  border-right: 4rpx solid #C93D46;
  border-bottom: 4rpx solid #C93D46;
  transform: skewX(-28deg) rotate(-42deg);
  animation: telemetryBeat 1.65s cubic-bezier(.2, .72, .2, 1) infinite;
}

.pulse-value-wrap {
  display: flex;
  align-items: baseline;
  gap: 7rpx;
  margin-left: 18rpx;
}

.pulse-value {
  color: #1C1C1E;
  font-size: 48rpx;
  font-weight: 820;
  line-height: 1;
}

.pulse-unit,
.pulse-scene {
  color: #8E8E93;
  font-size: 19rpx;
}

.pulse-scene {
  margin-left: auto;
  padding: 7rpx 12rpx;
  border-radius: 10rpx;
  background: #FFFFFF;
}

.telemetry-metrics {
  margin-top: 22rpx;
}

.telemetry-metric {
  min-width: 0;
  flex: 1;
  text-align: center;
}

.telemetry-value,
.telemetry-label {
  display: block;
}

.telemetry-value {
  color: #20364D;
  font-size: 27rpx;
  font-weight: 780;
}

.telemetry-label {
  margin-top: 4rpx;
  color: #8E8E93;
  font-size: 19rpx;
}

.telemetry-divider {
  width: 1rpx;
  height: 42rpx;
  background: rgba(60, 60, 67, .14);
}

.sync-row {
  margin-top: 22rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid rgba(60, 60, 67, .12);
  color: #8E8E93;
  font-size: 20rpx;
}

.sync-value {
  color: #636366;
  font-weight: 650;
}

@keyframes telemetryPulse {
  0%, 100% { opacity: .65; transform: scale(.92); }
  50% { opacity: 1; transform: scale(1.08); }
}

@keyframes telemetryBeat {
  0%, 42%, 100% { opacity: .38; transform: skewX(-28deg) rotate(-42deg) scale(.92); }
  48% { opacity: 1; transform: skewX(-28deg) rotate(-42deg) scale(1.08); }
  58% { opacity: .58; transform: skewX(-28deg) rotate(-42deg) scale(.98); }
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
  padding: 26rpx 32rpx 38rpx;
}
.scan-btn {
  position: relative;
  width: 100%;
  min-height: 88rpx;
  border-radius: 18rpx;
  background: #007AFF;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  box-shadow: none;
  transition: opacity 150ms ease, transform 150ms ease;
  z-index: 2;
}
.scan-btn.scanning {
  opacity: .72;
}
.scan-icon-wrap {
  width: 44rpx;
  height: 44rpx;
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
  background: #007AFF;
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
  background: #248A5A;
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
