<template>
  <view class="page">
    <!-- Tab 切换 -->
    <view class="tab-bar">
      <view
        class="tab-item"
        :class="{ 'tab-active': currentTab === 'fixed' }"
        @tap="currentTab = 'fixed'"
      >
        <text class="tab-text" :class="{ 'tab-text-active': currentTab === 'fixed' }">固定设备</text>
        <view v-if="currentTab === 'fixed'" class="tab-indicator"></view>
      </view>
      <view
        class="tab-item"
        :class="{ 'tab-active': currentTab === 'mobile' }"
        @tap="currentTab = 'mobile'"
      >
        <text class="tab-text" :class="{ 'tab-text-active': currentTab === 'mobile' }">移动设备</text>
        <view v-if="currentTab === 'mobile'" class="tab-indicator"></view>
      </view>
    </view>

    <!-- 固定设备列表 -->
    <scroll-view v-if="currentTab === 'fixed'" class="device-scroll" scroll-y>
      <view class="device-list">
        <view
          v-for="device in fixedDevices"
          :key="device.id"
          class="device-card"
        >
          <view class="device-card-top" :class="device.status === 'available' ? 'device-top-available' : 'device-top-maintenance'">
            <view class="device-type-tag">
              <text class="device-type-text">{{ device.category }}</text>
            </view>
            <view class="device-status-tag" :class="device.status === 'available' ? 'status-tag-available' : 'status-tag-maintenance'">
              <view class="status-tag-dot"></view>
              <text class="status-tag-text">{{ device.status === 'available' ? '可用' : '维护中' }}</text>
            </view>
          </view>
          <view class="device-card-body">
            <text class="device-name">{{ device.name }}</text>
            <text class="device-address">{{ device.address }}</text>
            <view class="device-meta-row">
              <text class="device-meta-item">有效期至 {{ device.expireDate }}</text>
            </view>
            <view class="device-card-footer">
              <text class="device-owner">管理方: {{ device.owner }}</text>
              <view class="online-toggle" @tap="toggleDevice(device)">
                <view class="toggle-track" :class="{ 'toggle-on': device.status === 'available' }">
                  <view class="toggle-thumb"></view>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 移动设备列表 -->
    <scroll-view v-if="currentTab === 'mobile'" class="device-scroll" scroll-y>
      <view class="device-list">
        <view
          v-for="device in mobileDevices"
          :key="device.id"
          class="device-card"
        >
          <view class="device-card-top" :class="device.online ? 'device-top-online' : 'device-top-offline'">
            <view class="device-type-tag">
              <text class="device-type-text">{{ device.category }}</text>
            </view>
            <view class="device-status-tag" :class="device.online ? 'status-tag-online' : 'status-tag-offline'">
              <view class="status-tag-dot"></view>
              <text class="status-tag-text">{{ device.online ? '在线' : '离线' }}</text>
            </view>
          </view>
          <view class="device-card-body">
            <text class="device-name">{{ device.name }}</text>
            <text class="device-address">{{ device.address }}</text>
            <view class="device-meta-row">
              <text class="device-meta-item">{{ device.vehicleInfo }}</text>
              <text class="device-meta-item">服务范围: {{ device.serviceRange }}km</text>
            </view>
            <view class="device-card-footer">
              <text class="device-owner">{{ device.serviceTime }}</text>
              <view class="online-toggle" @tap="toggleMobile(device)">
                <view class="toggle-track" :class="{ 'toggle-on': device.online }">
                  <view class="toggle-thumb"></view>
                </view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 录入新设备按钮 -->
    <view class="add-device-btn" @tap="goAddDevice">
      <text class="add-device-icon">+</text>
      <text class="add-device-text">录入新设备</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { mockFixedDevices, mockMobileDevices } from '@/mock/data'

const currentTab = ref<'fixed' | 'mobile'>('fixed')

const fixedDevices = ref(mockFixedDevices.map(d => ({ ...d })))
const mobileDevices = ref(mockMobileDevices.map(d => ({ ...d })))

function toggleDevice(device: any) {
  device.status = device.status === 'available' ? 'maintenance' : 'available'
  uni.showToast({
    title: device.status === 'available' ? '已上线' : '已下线',
    icon: 'none'
  })
}

function toggleMobile(device: any) {
  device.online = !device.online
  uni.showToast({
    title: device.online ? '已上线' : '已离线',
    icon: 'none'
  })
}

function goAddDevice() {
  uni.navigateTo({ url: '/pages/device/add' })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F0F4FA;
  padding-bottom: 160rpx;
}

/* Tab 切换 */
.tab-bar {
  display: flex;
  background: #FFFFFF;
  padding: 0 32rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
  position: sticky;
  top: 0;
  z-index: 10;
}
.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 28rpx 0 20rpx;
  position: relative;
}
.tab-text {
  font-size: 30rpx;
  color: #86909C;
  font-weight: 500;
  transition: all 0.2s ease;
}
.tab-text-active {
  color: #2B6FF0;
  font-weight: 700;
}
.tab-indicator {
  position: absolute;
  bottom: 0;
  width: 48rpx;
  height: 6rpx;
  border-radius: 3rpx;
  background: linear-gradient(90deg, #2B6FF0 0%, #5B8DEF 100%);
}

/* 设备列表 */
.device-scroll {
  height: calc(100vh - 120rpx - 160rpx);
  box-sizing: border-box;
}
.device-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  padding: 24rpx 32rpx;
}
.device-card {
  background: #FFFFFF;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(43, 111, 240, 0.06);
}
.device-card-top {
  height: 100rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24rpx;
}
.device-top-available {
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
}
.device-top-maintenance {
  background: linear-gradient(135deg, #FF9A2E 0%, #FFCF8B 100%);
}
.device-top-online {
  background: linear-gradient(135deg, #00B42A 0%, #4DC580 100%);
}
.device-top-offline {
  background: linear-gradient(135deg, #86909C 0%, #C9CDD4 100%);
}
.device-type-tag {
  background: rgba(255, 255, 255, 0.25);
  border-radius: 8rpx;
  padding: 4rpx 16rpx;
}
.device-type-text {
  font-size: 24rpx;
  color: #FFFFFF;
  font-weight: 600;
}
.device-status-tag {
  display: flex;
  align-items: center;
  gap: 8rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 8rpx;
  padding: 4rpx 14rpx;
}
.status-tag-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #FFFFFF;
}
.status-tag-available .status-tag-dot {
  box-shadow: 0 0 8rpx rgba(255, 255, 255, 0.6);
}
.status-tag-text {
  font-size: 22rpx;
  color: #FFFFFF;
  font-weight: 500;
}

/* 设备卡片内容 */
.device-card-body {
  padding: 20rpx 24rpx 24rpx;
}
.device-name {
  font-size: 28rpx;
  color: #1D2129;
  font-weight: 600;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: normal;
}
.device-address {
  font-size: 24rpx;
  color: #86909C;
  margin-top: 8rpx;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: normal;
  line-height: 1.5;
}
.device-meta-row {
  display: flex;
  gap: 24rpx;
  margin-top: 12rpx;
  flex-wrap: wrap;
}
.device-meta-item {
  font-size: 22rpx;
  color: #4E5969;
  background: #F7F8FA;
  border-radius: 8rpx;
  padding: 4rpx 12rpx;
}
.device-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #F2F3F5;
}
.device-owner {
  font-size: 22rpx;
  color: #86909C;
  flex: 1;
}

/* 在线开关 */
.online-toggle {
  flex-shrink: 0;
}
.toggle-track {
  width: 88rpx;
  height: 48rpx;
  border-radius: 24rpx;
  background: #E5E6EB;
  position: relative;
  transition: background 0.3s ease;
  padding: 4rpx;
  box-sizing: border-box;
}
.toggle-on {
  background: #2B6FF0;
}
.toggle-thumb {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: #FFFFFF;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.15);
  transition: transform 0.3s ease;
  transform: translateX(0);
}
.toggle-on .toggle-thumb {
  transform: translateX(40rpx);
}

/* 录入新设备按钮 */
.add-device-btn {
  position: fixed;
  bottom: 60rpx;
  left: 32rpx;
  right: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  border-radius: 48rpx;
  padding: 28rpx 0;
  box-shadow: 0 8rpx 32rpx rgba(43, 111, 240, 0.3);
  z-index: 10;
}
.add-device-btn:active {
  opacity: 0.85;
}
.add-device-icon {
  font-size: 40rpx;
  color: #FFFFFF;
  font-weight: 300;
  line-height: 1;
}
.add-device-text {
  font-size: 30rpx;
  color: #FFFFFF;
  font-weight: 600;
}
</style>
