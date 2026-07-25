<template>
  <view class="page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-content">
        <view class="nav-back" @tap="goBack">
          <text class="back-arrow">&#x2190;</text>
        </view>
        <text class="nav-title">设备管理</text>
        <view class="nav-add" @tap="goAdd">
          <text class="add-icon">+</text>
        </view>
      </view>
    </view>

    <scroll-view
      class="scroll-content"
      scroll-y
      :style="{ paddingTop: (statusBarHeight + 44) + 'px' }"
    >
      <!-- Tab 栏 -->
      <view class="tab-bar">
        <view
          class="tab-item"
          :class="{ 'tab-active': activeTab === 'fixed' }"
          @tap="activeTab = 'fixed'"
        >
          <text class="tab-text">我的固定设备</text>
          <view v-if="activeTab === 'fixed'" class="tab-indicator"></view>
        </view>
        <view
          class="tab-item"
          :class="{ 'tab-active': activeTab === 'mobile' }"
          @tap="activeTab = 'mobile'"
        >
          <text class="tab-text">我的移动设备</text>
          <view v-if="activeTab === 'mobile'" class="tab-indicator"></view>
        </view>
      </view>

      <!-- 固定设备列表 -->
      <view v-if="activeTab === 'fixed'" class="device-list">
        <view v-if="fixedDevices.length > 0">
          <view
            v-for="device in fixedDevices"
            :key="device.id"
            class="device-card"
          >
            <view class="card-header">
              <view class="card-header-left">
                <view class="device-icon-wrap device-icon-fixed">
                  <text class="device-icon-text">{{ device.category === 'AED' ? 'AED' : '急救' }}</text>
                </view>
                <view class="card-header-info">
                  <text class="card-name">{{ device.name }}</text>
                  <view class="card-meta-row">
                    <view class="card-category-tag">
                      <text class="category-tag-text">{{ device.category }}</text>
                    </view>
                    <view class="card-type-tag card-type-fixed">
                      <text class="type-tag-text">固定</text>
                    </view>
                  </view>
                </view>
              </view>
              <view class="card-status-toggle" @tap="toggleDeviceStatus(device)">
                <view
                  class="status-switch"
                  :class="{ 'status-switch-on': device.status === 'available' }"
                >
                  <view class="status-switch-thumb"></view>
                </view>
                <text class="status-switch-label">{{ device.status === 'available' ? '在线' : '离线' }}</text>
              </view>
            </view>

            <view class="card-body">
              <view class="card-info-item">
                <text class="info-icon">📍</text>
                <text class="info-text">{{ device.address }}</text>
              </view>
              <view class="card-info-item">
                <text class="info-icon">📅</text>
                <text class="info-text">有效期至 {{ device.expireDate }}</text>
              </view>
              <view class="card-info-item">
                <text class="info-icon">🏢</text>
                <text class="info-text">{{ device.owner }}</text>
              </view>
            </view>

            <view class="card-footer">
              <view class="card-status-badge" :class="'badge-' + (device.status === 'available' ? 'online' : 'offline')">
                <view class="badge-dot"></view>
                <text class="badge-text">{{ device.status === 'available' ? '可用' : '维护中' }}</text>
              </view>
              <view class="card-actions">
                <view class="card-btn card-btn-detail" @tap="viewDetail(device)">
                  <text class="card-btn-text">详情</text>
                </view>
                <view class="card-btn card-btn-edit" @tap="editDevice(device)">
                  <text class="card-btn-text card-btn-text-edit">编辑</text>
                </view>
              </view>
            </view>
          </view>
        </view>

        <!-- 空状态 -->
        <view v-else class="empty-state">
          <view class="empty-icon-wrap">
            <text class="empty-icon">📦</text>
          </view>
          <text class="empty-title">暂无固定设备</text>
          <text class="empty-desc">您还没有录入固定急救设备</text>
          <view class="empty-btn" @tap="goAdd">
            <text class="empty-btn-text">录入设备</text>
          </view>
        </view>
      </view>

      <!-- 移动设备列表 -->
      <view v-if="activeTab === 'mobile'" class="device-list">
        <view v-if="mobileDevices.length > 0">
          <view
            v-for="device in mobileDevices"
            :key="device.id"
            class="device-card"
          >
            <view class="card-header">
              <view class="card-header-left">
                <view class="device-icon-wrap device-icon-mobile">
                  <text class="device-icon-text">{{ device.category === 'AED' ? 'AED' : '急救' }}</text>
                </view>
                <view class="card-header-info">
                  <text class="card-name">{{ device.name }}</text>
                  <view class="card-meta-row">
                    <view class="card-category-tag">
                      <text class="category-tag-text">{{ device.category }}</text>
                    </view>
                    <view class="card-type-tag card-type-mobile">
                      <text class="type-tag-text">移动</text>
                    </view>
                  </view>
                </view>
              </view>
              <view class="card-status-toggle" @tap="toggleMobileStatus(device)">
                <view
                  class="status-switch"
                  :class="{ 'status-switch-on': device.online }"
                >
                  <view class="status-switch-thumb"></view>
                </view>
                <text class="status-switch-label">{{ device.online ? '在线' : '离线' }}</text>
              </view>
            </view>

            <view class="card-body">
              <view class="card-info-item">
                <text class="info-icon">🚗</text>
                <text class="info-text">{{ device.vehicleInfo }}</text>
              </view>
              <view class="card-info-item">
                <text class="info-icon">📍</text>
                <text class="info-text">{{ device.address }}</text>
              </view>
              <view class="card-info-item">
                <text class="info-icon">⏰</text>
                <text class="info-text">{{ device.serviceTime }}</text>
              </view>
              <view class="card-info-item">
                <text class="info-icon">📏</text>
                <text class="info-text">服务范围 {{ device.serviceRange }}km</text>
              </view>
            </view>

            <view class="card-footer">
              <view class="card-status-badge" :class="'badge-' + (device.online ? 'online' : 'offline')">
                <view class="badge-dot"></view>
                <text class="badge-text">{{ device.online ? '在线' : '离线' }}</text>
              </view>
              <view class="card-actions">
                <view class="card-btn card-btn-detail" @tap="viewDetail(device)">
                  <text class="card-btn-text">详情</text>
                </view>
                <view class="card-btn card-btn-edit" @tap="editDevice(device)">
                  <text class="card-btn-text card-btn-text-edit">编辑</text>
                </view>
              </view>
            </view>
          </view>
        </view>

        <!-- 空状态 -->
        <view v-else class="empty-state">
          <view class="empty-icon-wrap">
            <text class="empty-icon">🚗</text>
          </view>
          <text class="empty-title">暂无移动设备</text>
          <text class="empty-desc">您还没有录入移动急救设备</text>
          <view class="empty-btn" @tap="goAdd">
            <text class="empty-btn-text">录入设备</text>
          </view>
        </view>
      </view>

      <!-- 底部安全区 -->
      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { mockFixedDevices, mockMobileDevices } from '@/mock/data'

// 系统信息
const statusBarHeight = ref(0)
const systemInfo = uni.getSystemInfoSync()
statusBarHeight.value = systemInfo.statusBarHeight || 20

// Tab
const activeTab = ref<'fixed' | 'mobile'>('fixed')

// 设备列表 - 使用mock数据（深拷贝以便切换状态）
const fixedDevices = ref(mockFixedDevices.map(d => ({ ...d })))
const mobileDevices = ref(mockMobileDevices.map(d => ({ ...d })))

// 切换固定设备状态
function toggleDeviceStatus(device: any) {
  device.status = device.status === 'available' ? 'maintenance' : 'available'
  uni.showToast({
    title: device.status === 'available' ? '设备已上线' : '设备已离线',
    icon: 'none'
  })
}

// 切换移动设备状态
function toggleMobileStatus(device: any) {
  device.online = !device.online
  device.status = device.online ? 'online' : 'offline'
  uni.showToast({
    title: device.online ? '设备已上线' : '设备已离线',
    icon: 'none'
  })
}

// 查看详情
function viewDetail(device: any) {
  uni.showToast({
    title: '查看设备详情',
    icon: 'none'
  })
}

// 编辑设备
function editDevice(device: any) {
  uni.navigateTo({
    url: '/pages/device/add?id=' + device.id
  })
}

// 跳转添加
function goAdd() {
  uni.navigateTo({
    url: '/pages/device/add'
  })
}

// 返回
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
.nav-add {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}
.add-icon {
  font-size: 36rpx;
  color: #FFFFFF;
  font-weight: 600;
}

/* 滚动内容 */
.scroll-content {
  min-height: 100vh;
  box-sizing: border-box;
}

/* Tab 栏 */
.tab-bar {
  display: flex;
  background: #FFFFFF;
  padding: 0 32rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}
.tab-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 28rpx 32rpx;
  margin-right: 48rpx;
}
.tab-text {
  font-size: 30rpx;
  color: #86909C;
  font-weight: 500;
  transition: all 0.3s ease;
}
.tab-active .tab-text {
  color: #2B6FF0;
  font-weight: 700;
}
.tab-indicator {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 48rpx;
  height: 6rpx;
  border-radius: 3rpx;
  background: #2B6FF0;
}

/* 设备列表 */
.device-list {
  padding: 24rpx 24rpx 0;
}

/* 设备卡片 */
.device-card {
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
}

/* 卡片头部 */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-header-left {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}
.device-icon-wrap {
  width: 80rpx;
  height: 80rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  flex-shrink: 0;
}
.device-icon-fixed {
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
}
.device-icon-mobile {
  background: linear-gradient(135deg, #00B42A 0%, #4DC580 100%);
}
.device-icon-text {
  font-size: 24rpx;
  font-weight: 700;
  color: #FFFFFF;
}
.card-header-info {
  flex: 1;
  min-width: 0;
}
.card-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1D2129;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-meta-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 8rpx;
}
.card-category-tag {
  padding: 2rpx 12rpx;
  background: #F2F3F5;
  border-radius: 6rpx;
}
.category-tag-text {
  font-size: 20rpx;
  color: #4E5969;
}
.card-type-tag {
  padding: 2rpx 12rpx;
  border-radius: 6rpx;
}
.card-type-fixed {
  background: rgba(43, 111, 240, 0.1);
}
.card-type-mobile {
  background: rgba(0, 180, 42, 0.1);
}
.type-tag-text {
  font-size: 20rpx;
  font-weight: 600;
}
.card-type-fixed .type-tag-text {
  color: #2B6FF0;
}
.card-type-mobile .type-tag-text {
  color: #00B42A;
}

/* 状态开关 */
.card-status-toggle {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  margin-left: 16rpx;
}
.status-switch {
  width: 84rpx;
  height: 48rpx;
  border-radius: 24rpx;
  background: #E5E6EB;
  position: relative;
  transition: all 0.3s ease;
  padding: 4rpx;
}
.status-switch-on {
  background: #2B6FF0;
}
.status-switch-thumb {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: #FFFFFF;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}
.status-switch-on .status-switch-thumb {
  transform: translateX(36rpx);
}
.status-switch-label {
  font-size: 20rpx;
  color: #86909C;
}

/* 卡片内容 */
.card-body {
  padding: 20rpx 0;
  margin-top: 20rpx;
  border-top: 1rpx solid #F2F3F5;
}
.card-info-item {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-bottom: 12rpx;
}
.card-info-item:last-child {
  margin-bottom: 0;
}
.info-icon {
  font-size: 24rpx;
  flex-shrink: 0;
}
.info-text {
  font-size: 24rpx;
  color: #4E5969;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

/* 卡片底部 */
.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #F2F3F5;
}
.card-status-badge {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
}
.badge-online {
  background: rgba(0, 180, 42, 0.08);
}
.badge-offline {
  background: rgba(201, 205, 212, 0.15);
}
.badge-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
}
.badge-online .badge-dot {
  background: #00B42A;
  box-shadow: 0 0 8rpx rgba(0, 180, 42, 0.4);
}
.badge-offline .badge-dot {
  background: #C9CDD4;
}
.badge-text {
  font-size: 22rpx;
  font-weight: 500;
}
.badge-online .badge-text {
  color: #00B42A;
}
.badge-offline .badge-text {
  color: #C9CDD4;
}
.card-actions {
  display: flex;
  gap: 12rpx;
}
.card-btn {
  padding: 10rpx 28rpx;
  border-radius: 24rpx;
  transition: all 0.3s ease;
}
.card-btn:active {
  transform: scale(0.95);
}
.card-btn-detail {
  background: #F2F3F5;
}
.card-btn-edit {
  background: rgba(43, 111, 240, 0.08);
}
.card-btn-text {
  font-size: 24rpx;
  color: #4E5969;
  font-weight: 500;
}
.card-btn-text-edit {
  color: #2B6FF0;
  font-weight: 600;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
}
.empty-icon-wrap {
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  background: #F2F3F5;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32rpx;
}
.empty-icon {
  font-size: 64rpx;
}
.empty-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1D2129;
  margin-bottom: 12rpx;
}
.empty-desc {
  font-size: 26rpx;
  color: #86909C;
  margin-bottom: 40rpx;
}
.empty-btn {
  padding: 20rpx 64rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  border-radius: 40rpx;
  box-shadow: 0 6rpx 24rpx rgba(43, 111, 240, 0.3);
}
.empty-btn:active {
  transform: scale(0.97);
}
.empty-btn-text {
  font-size: 28rpx;
  color: #FFFFFF;
  font-weight: 600;
}

/* 底部安全区 */
.bottom-safe {
  height: 60rpx;
}
</style>
