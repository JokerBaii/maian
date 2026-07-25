<template>
  <view class="page">
    <scroll-view class="scroll-content" scroll-y>
      <view class="page-actions">
        <view class="page-add-action" @tap="goAdd">
          <app-icon name="plus" :size="18" color="#FFFFFF" />
          <text>录入设备</text>
        </view>
      </view>
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
                  :class="{ 'status-switch-on': device.status === 'AVAILABLE' }"
                >
                  <view class="status-switch-thumb"></view>
                </view>
                <text class="status-switch-label">{{ device.status === 'AVAILABLE' ? '在线' : '离线' }}</text>
              </view>
            </view>

            <view class="card-body">
              <view class="card-info-item">
                <app-icon class="info-icon" name="location-filled" :size="16" color="#8994A8" />
                <text class="info-text">{{ device.address }}</text>
              </view>
              <view class="card-info-item">
                <app-icon class="info-icon" name="calendar" :size="16" color="#8994A8" />
                <text class="info-text">
                  {{ device.expireDate ? `有效期至 ${device.expireDate}` : '未设置有效期' }}
                </text>
              </view>
              <view class="card-info-item">
                <app-icon class="info-icon" name="shop-filled" :size="16" color="#8994A8" />
                <text class="info-text">{{ device.owner || '未登记所有方' }}</text>
              </view>
              <view v-if="deviceImages(device).length" class="card-image-strip">
                <image
                  v-for="(imageUrl, index) in deviceImages(device)"
                  :key="imageUrl"
                  class="card-image"
                  :src="resolveApiUrl(imageUrl)"
                  mode="aspectFill"
                  @tap="previewDeviceImage(device, index)"
                />
              </view>
            </view>

            <view class="card-footer">
              <view class="card-status-badge" :class="'badge-' + (device.status === 'AVAILABLE' ? 'online' : 'offline')">
                <view class="badge-dot"></view>
                <text class="badge-text">{{ device.status === 'AVAILABLE' ? '可用' : '维护中' }}</text>
              </view>
              <view class="card-actions">
                <view class="card-btn card-btn-detail" @tap="viewDetail(device)">
                  <text class="card-btn-text">详情</text>
                </view>
                <view class="card-btn card-btn-edit" @tap="editDevice(device)">
                  <text class="card-btn-text card-btn-text-edit">编辑</text>
                </view>
                <view class="card-btn card-btn-delete" @tap="confirmDelete(device)">
                  <text class="card-btn-text card-btn-text-delete">删除</text>
                </view>
              </view>
            </view>
          </view>
        </view>

        <view v-else class="empty-state">
          <view class="empty-icon-wrap">
            <app-icon class="empty-icon" name="cart" :size="44" color="#8994A8" />
          </view>
          <text class="empty-title">暂无固定设备</text>
          <text class="empty-desc">您还没有录入固定急救设备</text>
          <view class="empty-btn" @tap="goAdd">
            <text class="empty-btn-text">录入设备</text>
          </view>
        </view>
      </view>

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
                <app-icon class="info-icon" name="navigate-filled" :size="16" color="#8994A8" />
                <text class="info-text">{{ device.vehicleInfo || '未填写车辆信息' }}</text>
              </view>
              <view class="card-info-item">
                <app-icon class="info-icon" name="location-filled" :size="16" color="#8994A8" />
                <text class="info-text">{{ device.address }}</text>
              </view>
              <view class="card-info-item">
                <app-icon class="info-icon" name="calendar-filled" :size="16" color="#8994A8" />
                <text class="info-text">{{ device.serviceTime }}</text>
              </view>
              <view class="card-info-item">
                <app-icon class="info-icon" name="map-pin-ellipse" :size="16" color="#8994A8" />
                <text class="info-text">
                  {{ device.serviceRange ? `服务范围 ${device.serviceRange}km` : '服务范围未设置' }}
                </text>
              </view>
              <view v-if="deviceImages(device).length" class="card-image-strip">
                <image
                  v-for="(imageUrl, index) in deviceImages(device)"
                  :key="imageUrl"
                  class="card-image"
                  :src="resolveApiUrl(imageUrl)"
                  mode="aspectFill"
                  @tap="previewDeviceImage(device, index)"
                />
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
                <view class="card-btn card-btn-delete" @tap="confirmDelete(device)">
                  <text class="card-btn-text card-btn-text-delete">删除</text>
                </view>
              </view>
            </view>
          </view>
        </view>

        <view v-else class="empty-state">
          <view class="empty-icon-wrap">
            <app-icon class="empty-icon" name="navigate" :size="44" color="#8994A8" />
          </view>
          <text class="empty-title">暂无移动设备</text>
          <text class="empty-desc">您还没有录入移动急救设备</text>
          <view class="empty-btn" @tap="goAdd">
            <text class="empty-btn-text">录入设备</text>
          </view>
        </view>
      </view>

      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIcon from '@/components/AppIcon.vue'
import { resolveApiUrl } from '@/api/http'
import {
  listEmergencyDevices,
  deleteEmergencyDevice,
  updateEmergencyDeviceStatus,
  type EmergencyDeviceResponse
} from '@/api/devices'

const activeTab = ref<'fixed' | 'mobile'>('fixed')

type DeviceView = EmergencyDeviceResponse & {
  online: boolean
}

const fixedDevices = ref<DeviceView[]>([])
const mobileDevices = ref<DeviceView[]>([])

function toView(device: EmergencyDeviceResponse): DeviceView {
  return {
    ...device,
    online: device.status === 'AVAILABLE'
  }
}

function deviceImages(device: DeviceView) {
  return [...device.imageUrls, ...device.vehicleImageUrls]
}

function previewDeviceImage(device: DeviceView, index: number) {
  const urls = deviceImages(device).map(resolveApiUrl)
  uni.previewImage({ current: urls[index], urls })
}

async function loadDevices() {
  try {
    const page = await listEmergencyDevices()
    const devices = page.content.map(toView)
    fixedDevices.value = devices.filter(device => device.type === 'FIXED')
    mobileDevices.value = devices.filter(device => device.type === 'MOBILE')
  } catch {
    uni.showToast({ title: '设备数据加载失败', icon: 'none' })
  }
}

async function toggleDeviceStatus(device: DeviceView) {
  const nextStatus = device.status === 'AVAILABLE' ? 'MAINTENANCE' : 'AVAILABLE'
  try {
    const updated = await updateEmergencyDeviceStatus(device.id, nextStatus)
    Object.assign(device, toView(updated))
    uni.showToast({ title: nextStatus === 'AVAILABLE' ? '设备已上线' : '设备已维护', icon: 'none' })
  } catch {
    uni.showToast({ title: '状态更新失败', icon: 'none' })
  }
}

async function toggleMobileStatus(device: DeviceView) {
  const nextStatus = device.status === 'AVAILABLE' ? 'OFFLINE' : 'AVAILABLE'
  try {
    const updated = await updateEmergencyDeviceStatus(device.id, nextStatus)
    Object.assign(device, toView(updated))
    uni.showToast({ title: nextStatus === 'AVAILABLE' ? '设备已上线' : '设备已离线', icon: 'none' })
  } catch {
    uni.showToast({ title: '状态更新失败', icon: 'none' })
  }
}

function viewDetail(device: DeviceView) {
  uni.showModal({
    title: device.name,
    content: `${device.address}\n服务时间：${device.serviceTime || '未设置'}\n联系电话：${device.ownerPhone || '未设置'}`,
    showCancel: false,
    confirmText: '知道了'
  })
}

function editDevice(device: DeviceView) {
  uni.navigateTo({
    url: '/pages/device/add?id=' + device.id
  })
}

function goAdd() {
  uni.navigateTo({
    url: '/pages/device/add'
  })
}

function confirmDelete(device: DeviceView) {
  uni.showModal({
    title: '删除设备',
    content: `确定删除“${device.name}”吗？删除后地图中将不再展示。`,
    confirmColor: '#D64B4B',
    success: async (result) => {
      if (!result.confirm) return
      try {
        await deleteEmergencyDevice(device.id)
        await loadDevices()
        uni.showToast({ title: '设备已删除', icon: 'success' })
      } catch {
        uni.showToast({ title: '设备删除失败', icon: 'none' })
      }
    }
  })
}

onShow(loadDevices)

</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F0F4FA;
}

.scroll-content {
  min-height: 100vh;
  box-sizing: border-box;
}

.page-actions {
  display: flex;
  justify-content: flex-end;
  padding: 24rpx 32rpx 12rpx;
}
.page-add-action {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  min-height: 68rpx;
  padding: 0 24rpx;
  border-radius: 18rpx;
  background: #2B6FF0;
  box-shadow: 0 8rpx 20rpx rgba(43, 111, 240, 0.2);
  font-size: 26rpx;
  font-weight: 600;
  color: #FFFFFF;
}

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

.device-list {
  padding: 24rpx 24rpx 0;
}

.device-card {
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);
}

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
.card-image-strip {
  display: flex;
  gap: 12rpx;
  margin-top: 18rpx;
  overflow: hidden;
}
.card-image {
  width: 128rpx;
  height: 96rpx;
  flex: none;
  border-radius: 14rpx;
  background: #EDF2F8;
}

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
.card-btn-delete {
  background: rgba(214, 75, 75, 0.08);
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
.card-btn-text-delete {
  color: #C44242;
  font-weight: 600;
}

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

.bottom-safe {
  height: 60rpx;
}
</style>
