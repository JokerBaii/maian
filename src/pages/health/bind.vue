<template>
  <view class="page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-content">
        <view class="nav-back" @tap="goBack">
          <text class="back-arrow">&#x2190;</text>
        </view>
        <text class="nav-title">设备绑定</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <scroll-view
      class="scroll-content"
      scroll-y
      :style="{ paddingTop: (statusBarHeight + 44) + 'px' }"
    >
      <!-- 已绑定设备 -->
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
            <view class="device-avatar">
              <text class="device-avatar-icon">&#x231A;</text>
            </view>
            <view class="bound-detail">
              <text class="bound-name">{{ boundDevice.name }}</text>
              <view class="bound-meta">
                <text class="bound-type">手环</text>
                <text class="bound-divider">|</text>
                <view class="battery-mini">
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

      <!-- 未绑定提示 -->
      <view v-if="!boundDevice" class="empty-bound">
        <view class="empty-icon">
          <text class="empty-icon-text">&#x231A;</text>
        </view>
        <text class="empty-title">暂无绑定设备</text>
        <text class="empty-desc">请扫描并绑定可穿戴设备以开始健康监测</text>
      </view>

      <!-- 扫描按钮 -->
      <view class="scan-section">
        <view class="scan-btn" :class="{ 'scanning': isScanning }" @tap="toggleScan">
          <view v-if="isScanning" class="scan-animation">
            <view class="scan-ring scan-ring-1"></view>
            <view class="scan-ring scan-ring-2"></view>
            <view class="scan-ring scan-ring-3"></view>
          </view>
          <view class="scan-icon-wrap">
            <text class="scan-icon-text">{{ isScanning ? '' : '🔍' }}</text>
            <view v-if="isScanning" class="scan-spinner"></view>
          </view>
          <text class="scan-label">{{ isScanning ? '扫描中...' : '扫描设备' }}</text>
        </view>
      </view>

      <!-- 发现的设备列表 -->
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
              <view class="device-item-avatar" :style="{ background: device.avatarBg }">
                <text class="device-item-icon">{{ device.icon }}</text>
              </view>
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

      <!-- 绑定成功动画 -->
      <view v-if="showBindSuccess" class="success-overlay">
        <view class="success-card">
          <view class="success-icon-wrap">
            <view class="success-check">
              <text class="check-mark">&#x2713;</text>
            </view>
          </view>
          <text class="success-title">绑定成功</text>
          <text class="success-desc">已成功连接{{ boundDeviceName }}</text>
          <text class="success-hint">现在可以开始监测心率了</text>
        </view>
      </view>

      <!-- 底部安全区 -->
      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { mockUser, mockBLEDevices } from '@/mock/data'

// 系统信息
const statusBarHeight = ref(0)
const systemInfo = uni.getSystemInfoSync()
statusBarHeight.value = systemInfo.statusBarHeight || 20

// 已绑定设备
const boundDevice = ref<{ name: string; type: string; connected: boolean; battery: number } | null>({
  name: mockUser.bindDevice.name,
  type: mockUser.bindDevice.type,
  connected: mockUser.bindDevice.connected,
  battery: mockUser.bindDevice.battery
})

// 扫描状态
const isScanning = ref(false)
const showBindSuccess = ref(false)
const boundDeviceName = ref('')

// 发现设备列表
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

// mock设备数据 - 从mockBLEDevices生成
const mockDevices: Omit<DiscoveredDevice, 'binding' | 'bound'>[] = mockBLEDevices.map(d => {
  const iconMap: Record<string, string> = {
    '华为手环 Band 8': 'H',
    '小米手环 7': 'M',
    'Apple Watch S9': 'A',
    'OPPO Watch 3': 'O',
    '华为Watch GT4': 'G',
    '荣耀手环 7': 'R'
  }
  const bgMap: Record<string, string> = {
    band: 'linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%)',
    watch: 'linear-gradient(135deg, #1D2129 0%, #4E5969 100%)'
  }
  return {
    id: d.id,
    name: d.name,
    icon: iconMap[d.name] || d.name.charAt(0),
    avatarBg: bgMap[d.type] || 'linear-gradient(135deg, #00B42A 0%, #4DC580 100%)',
    signal: d.signal
  }
})

function toggleScan() {
  if (isScanning.value) {
    isScanning.value = false
    return
  }
  isScanning.value = true
  discoveredDevices.length = 0

  // 模拟逐个发现设备
  mockDevices.forEach((device, idx) => {
    setTimeout(() => {
      if (!isScanning.value) return
      discoveredDevices.push({
        ...device,
        binding: false,
        bound: false
      })
      // 最后一个设备发现后停止扫描
      if (idx === mockDevices.length - 1) {
        setTimeout(() => {
          isScanning.value = false
        }, 500)
      }
    }, (idx + 1) * 800)
  })
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

  setTimeout(() => {
    device.binding = false
    device.bound = true
    boundDeviceName.value = device.name
    boundDevice.value = {
      name: device.name,
      type: 'band',
      connected: true,
      battery: 85
    }

    // 显示绑定成功动画
    showBindSuccess.value = true
    setTimeout(() => {
      showBindSuccess.value = false
    }, 2000)
  }, 1500)
}

function handleDisconnect() {
  uni.showModal({
    title: '断开设备',
    content: '确定要断开当前设备连接吗？',
    confirmColor: '#F53F3F',
    success: (res) => {
      if (res.confirm) {
        boundDevice.value = null
      }
    }
  })
}

function goBack() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F0F4FA;
}

/* 导航栏 */
.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
}
.nav-bar-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 88rpx;
  padding: 0 32rpx;
}
.nav-back {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.back-arrow {
  font-size: 40rpx;
  color: #FFFFFF;
  font-weight: 600;
}
.nav-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #FFFFFF;
}
.nav-placeholder {
  width: 64rpx;
}

/* 滚动内容 */
.scroll-content {
  min-height: 100vh;
  box-sizing: border-box;
}

/* 通用卡片 */
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
  background: #2B6FF0;
  margin-right: 12rpx;
}
.card-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1D2129;
}

/* 已绑定设备 */
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
  background: #00B42A;
  box-shadow: 0 0 8rpx rgba(0, 180, 42, 0.4);
}
.connected-text {
  font-size: 22rpx;
  color: #00B42A;
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
.device-avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(43, 111, 240, 0.2);
}
.device-avatar-icon {
  font-size: 44rpx;
  color: #FFFFFF;
}
.bound-detail {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.bound-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1D2129;
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
  background: #00B42A;
}
.battery-text-mini {
  font-size: 20rpx;
  color: #86909C;
}
.disconnect-btn {
  padding: 12rpx 28rpx;
  border-radius: 24rpx;
  border: 2rpx solid #F53F3F;
  background: rgba(245, 63, 63, 0.04);
}
.disconnect-text {
  font-size: 24rpx;
  color: #F53F3F;
  font-weight: 600;
}

/* 未绑定提示 */
.empty-bound {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 64rpx 32rpx 32rpx;
}
.empty-icon {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: rgba(43, 111, 240, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20rpx;
}
.empty-icon-text {
  font-size: 56rpx;
  opacity: 0.5;
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

/* 扫描按钮 */
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
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
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

/* 设备列表 */
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
.device-item-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
}
.device-item-icon {
  font-size: 32rpx;
  color: #FFFFFF;
  font-weight: 800;
}
.device-item-info {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.device-item-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1D2129;
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
  background: #00B42A;
}
.signal-text {
  font-size: 20rpx;
  color: #86909C;
  margin-left: 6rpx;
}
.bind-btn {
  padding: 10rpx 28rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
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
  color: #00B42A;
}

/* 绑定成功动画 */
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
  background: linear-gradient(135deg, #00B42A 0%, #4DC580 100%);
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
  color: #1D2129;
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

/* 底部安全区 */
.bottom-safe {
  height: 60rpx;
}
</style>
