<template>
  <view class="page">
    <view class="scroll-content">
      <view class="page-actions">
        <view class="device-summary">
          <text class="summary-number">{{ fixedDevices.length + mobileDevices.length }}</text>
          <text class="summary-label">台设备已登记</text>
        </view>
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
          <text class="tab-text">固定设备</text>
          <text class="tab-count">{{ fixedDevices.length }}</text>
          <view v-if="activeTab === 'fixed'" class="tab-indicator"></view>
        </view>
        <view
          class="tab-item"
          :class="{ 'tab-active': activeTab === 'mobile' }"
          @tap="activeTab = 'mobile'"
        >
          <text class="tab-text">移动设备</text>
          <text class="tab-count">{{ mobileDevices.length }}</text>
          <view v-if="activeTab === 'mobile'" class="tab-indicator"></view>
        </view>
      </view>

      <view class="device-list">
        <view v-if="currentDevices.length > 0">
          <view
            v-for="device in currentDevices"
            :key="device.id"
            class="device-card"
            :class="'device-card-' + statusTone(device)"
          >
            <view class="device-status-rail"></view>
            <view class="card-header">
              <view class="card-header-info">
                <text class="card-name">{{ device.name }}</text>
                <view class="card-meta-row">
                  <text class="category-tag-text">{{ device.category }}</text>
                  <text class="card-meta-separator">·</text>
                  <text class="type-tag-text">{{ device.type === 'FIXED' ? '固定设备' : '移动设备' }}</text>
                </view>
              </view>
              <view
                v-if="canToggleDevice(device)"
                class="card-status-action"
                :class="{ 'card-status-action-online': device.online }"
                @tap.stop="toggleCurrentStatus(device)"
              >
                <view class="status-dot"></view>
                <text class="status-action-label">{{ deviceStatusLabel(device) }}</text>
              </view>
              <view v-else class="review-status" :class="'review-' + device.status.toLowerCase()">
                {{ deviceStatusLabel(device) }}
              </view>
            </view>

            <view class="card-body">
              <view class="card-info-item">
                <app-icon class="info-icon" name="location-filled" :size="16" color="#8994A8" />
                <text class="info-text">{{ device.address }}</text>
              </view>
              <view class="card-facts">
                <view v-if="device.type === 'FIXED'" class="card-fact">
                  <app-icon name="calendar" :size="14" color="#718197" />
                  <text>{{ device.expireDate ? `有效至 ${device.expireDate}` : '有效期未登记' }}</text>
                </view>
                <view v-if="device.type === 'FIXED'" class="card-fact">
                  <app-icon name="shop-filled" :size="14" color="#718197" />
                  <text>{{ device.owner || '所有方未登记' }}</text>
                </view>
                <view v-if="device.type === 'MOBILE'" class="card-fact">
                  <app-icon name="navigate-filled" :size="14" color="#718197" />
                  <text>{{ device.vehicleInfo || '携带信息未填写' }}</text>
                </view>
                <view v-if="device.type === 'MOBILE'" class="card-fact">
                  <app-icon name="map-pin-ellipse" :size="14" color="#718197" />
                  <text>{{ device.serviceRange ? `${device.serviceRange}km 服务范围` : '范围未设置' }}</text>
                </view>
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
              <text class="footer-service">{{ device.serviceTime || '服务时段未设置' }}</text>
              <view class="card-actions">
                <view
                  v-if="device.type === 'MOBILE'"
                  class="card-btn card-btn-detail"
                  @tap="reportMobileLocation(device)"
                >
                  <text class="card-btn-text">上报位置</text>
                </view>
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
          <app-icon-tile
            class="empty-icon"
            :name="activeTab === 'fixed' ? 'fixed-device' : 'mobile-device'"
            :tone="activeTab === 'fixed' ? 'blue' : 'green'"
            size="large"
          />
          <text class="empty-title">{{ activeTab === 'fixed' ? '暂无固定设备' : '暂无移动设备' }}</text>
          <text class="empty-desc">还没有录入这类急救设备</text>
          <view class="empty-btn" @tap="goAdd">
            <text class="empty-btn-text">录入设备</text>
          </view>
        </view>
      </view>

      <view class="bottom-safe"></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import { getCurrentGcj02Location } from '@/utils/location'
import { resolveApiUrl } from '@/api/http'
import {
  listMyEmergencyDevices,
  deleteEmergencyDevice,
  updateEmergencyDeviceStatus,
  updateEmergencyDeviceLocation,
  type EmergencyDeviceResponse
} from '@/api/devices'

const activeTab = ref<'fixed' | 'mobile'>('fixed')

type DeviceView = EmergencyDeviceResponse & {
  online: boolean
}

const fixedDevices = ref<DeviceView[]>([])
const mobileDevices = ref<DeviceView[]>([])
const currentDevices = computed(() => (
  activeTab.value === 'fixed' ? fixedDevices.value : mobileDevices.value
))
let syncingLocation = false

function toView(device: EmergencyDeviceResponse): DeviceView {
  return {
    ...device,
    online: device.status === 'AVAILABLE'
  }
}

function deviceImages(device: DeviceView) {
  return [...device.imageUrls, ...device.vehicleImageUrls]
}

function canToggleDevice(device: DeviceView) {
  return ['AVAILABLE', 'MAINTENANCE', 'OFFLINE'].includes(device.status)
}

function deviceStatusLabel(device: DeviceView) {
  const labels: Record<string, string> = {
    PENDING_REVIEW: '待平台审核',
    AVAILABLE: device.type === 'FIXED' ? '可用' : '在线',
    RESERVED: '救援占用中',
    MAINTENANCE: '维护中',
    OFFLINE: '离线',
    EXPIRED: '已过期',
    REJECTED: '审核未通过'
  }
  return labels[device.status] || device.status
}

function statusTone(device: DeviceView) {
  if (device.status === 'AVAILABLE') return 'online'
  if (device.status === 'PENDING_REVIEW') return 'pending'
  if (device.status === 'REJECTED' || device.status === 'EXPIRED') return 'rejected'
  return 'offline'
}

function previewDeviceImage(device: DeviceView, index: number) {
  const urls = deviceImages(device).map(resolveApiUrl)
  uni.previewImage({ current: urls[index], urls })
}

async function loadDevices() {
  try {
    const page = await listMyEmergencyDevices()
    const devices = page.content.map(toView)
    fixedDevices.value = devices.filter(device => device.type === 'FIXED')
    mobileDevices.value = devices.filter(device => device.type === 'MOBILE')
  } catch {
    uni.showToast({ title: '设备数据加载失败', icon: 'none' })
  }
}

async function toggleDeviceStatus(device: DeviceView) {
  if (!canToggleDevice(device)) return
  if (device.status === 'RESERVED') {
    uni.showToast({ title: '设备正在执行救援', icon: 'none' })
    return
  }
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
  if (!canToggleDevice(device)) return
  if (device.status === 'RESERVED') {
    uni.showToast({ title: '设备正在执行救援', icon: 'none' })
    return
  }
  const nextStatus = device.status === 'AVAILABLE' ? 'OFFLINE' : 'AVAILABLE'
  try {
    if (nextStatus === 'AVAILABLE') {
      const location = await getCurrentLocation()
      await updateEmergencyDeviceLocation(device.id, {
        ...location,
        address: `实时位置 · ${location.latitude.toFixed(5)}, ${location.longitude.toFixed(5)}`
      })
    }
    const updated = await updateEmergencyDeviceStatus(device.id, nextStatus)
    Object.assign(device, toView(updated))
    uni.showToast({ title: nextStatus === 'AVAILABLE' ? '设备已上线' : '设备已离线', icon: 'none' })
  } catch {
    uni.showToast({ title: '状态更新失败', icon: 'none' })
  }
}

function toggleCurrentStatus(device: DeviceView) {
  return device.type === 'FIXED'
    ? toggleDeviceStatus(device)
    : toggleMobileStatus(device)
}

function getCurrentLocation(): Promise<{ longitude: number; latitude: number }> {
  return getCurrentGcj02Location()
}

/**
 * 上报单台移动设备的当前位置。
 *
 * 只在用户点击时对指定设备生效：批量把同一个位置写到所有移动设备会让它们
 * 落在同一坐标，调度距离全部变成 0，失去调度意义。
 */
async function reportMobileLocation(device: DeviceView) {
  if (syncingLocation) return
  syncingLocation = true
  try {
    const location = await getCurrentLocation()
    const address = `实时位置 · ${location.latitude.toFixed(5)}, ${location.longitude.toFixed(5)}`
    const updated = await updateEmergencyDeviceLocation(device.id, { ...location, address })
    const target = mobileDevices.value.find(item => item.id === updated.id)
    if (target) Object.assign(target, toView(updated))
    uni.showToast({ title: '位置已上报', icon: 'success' })
  } catch (error: any) {
    uni.showToast({ title: error?.message || '位置上报失败', icon: 'none' })
  } finally {
    syncingLocation = false
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
  background: #F4F7FA;
}

.review-status {
  padding: 8rpx 14rpx;
  border-radius: 10rpx;
  color: #B66A10;
  background: #FFF4E5;
  font-size: 22rpx;
  font-weight: 600;
}
.badge-pending { color: #B66A10; background: #FFF4E5; }
.badge-rejected { color: #C93D46; background: #FFF0F0; }

.scroll-content {
  min-height: 100vh;
  box-sizing: border-box;
}

.page-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 24rpx 16rpx;
}
.device-summary {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
}
.summary-number {
  color: #173A67;
  font-size: 38rpx;
  font-weight: 750;
  line-height: 1;
}
.summary-label {
  color: #718197;
  font-size: 23rpx;
}
.page-add-action {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  min-height: 62rpx;
  padding: 0 22rpx;
  border-radius: 12rpx;
  background: #2E6DD1;
  font-size: 24rpx;
  font-weight: 600;
  color: #FFFFFF;
}

.tab-bar {
  display: flex;
  gap: 6rpx;
  margin: 0 24rpx;
  padding: 6rpx;
  border-radius: 16rpx;
  background: #E8EEF5;
}
.tab-item {
  position: relative;
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  padding: 16rpx 20rpx;
  border-radius: 12rpx;
}
.tab-active {
  background: #FFFFFF;
}
.tab-text {
  font-size: 25rpx;
  color: #718197;
  font-weight: 500;
}
.tab-active .tab-text {
  color: #2E6DD1;
  font-weight: 700;
}
.tab-count {
  min-width: 30rpx;
  padding: 2rpx 8rpx;
  border-radius: 12rpx;
  background: #D8E1EB;
  color: #607187;
  text-align: center;
  font-size: 19rpx;
}
.tab-active .tab-count {
  background: #E6EFFD;
  color: #2E6DD1;
}
.tab-indicator {
  position: absolute;
  bottom: 4rpx;
  left: 50%;
  transform: translateX(-50%);
  width: 30rpx;
  height: 4rpx;
  border-radius: 3rpx;
  background: #2E6DD1;
}

.device-list {
  margin: 18rpx 24rpx 0;
}

.device-card {
  position: relative;
  margin-bottom: 16rpx;
  overflow: hidden;
  padding: 22rpx 22rpx 18rpx 30rpx;
  border: 1rpx solid #DDE6EE;
  border-radius: 18rpx;
  background: #FFFFFF;
}
.device-status-rail {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 7rpx;
  background: #AAB6C5;
}
.device-card-online .device-status-rail { background: #24986B; }
.device-card-pending .device-status-rail { background: #D58B2D; }
.device-card-rejected .device-status-rail { background: #C94D55; }
.device-card-offline .device-status-rail { background: #9AA6B5; }

.card-header-info {
  flex: 1;
  min-width: 0;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}
.card-name {
  font-size: 29rpx;
  font-weight: 700;
  color: #20364D;
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
.category-tag-text {
  font-size: 21rpx;
  color: #607187;
}
.card-meta-separator {
  color: #B5C0CD;
  font-size: 20rpx;
}
.type-tag-text {
  font-size: 21rpx;
  color: #607187;
}

.card-status-action {
  display: inline-flex;
  align-items: center;
  gap: 9rpx;
  margin-left: 16rpx;
  padding: 10rpx 15rpx;
  border: 1rpx solid #DCE3EB;
  border-radius: 10rpx;
  background: #F3F5F7;
}
.status-dot {
  width: 11rpx;
  height: 11rpx;
  border-radius: 50%;
  background: #9AA6B5;
}
.status-action-label {
  color: #68788C;
  font-size: 21rpx;
  font-weight: 600;
  line-height: 1;
}
.card-status-action-online {
  border-color: #CBE9DD;
  background: #EAF7F1;
}
.card-status-action-online .status-dot {
  background: #24986B;
}
.card-status-action-online .status-action-label {
  color: #1D7B58;
}

.card-body {
  padding: 14rpx 0 0;
  margin-top: 14rpx;
  border-top: 1rpx solid #F2F3F5;
}
.card-info-item {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-bottom: 10rpx;
}
.card-info-item:last-child {
  margin-bottom: 0;
}
.info-icon {
  font-size: 24rpx;
  flex-shrink: 0;
}
.info-text {
  font-size: 23rpx;
  color: #4E5969;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.card-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx;
}
.card-fact {
  display: flex;
  align-items: center;
  gap: 7rpx;
  max-width: 100%;
  padding: 8rpx 11rpx;
  border-radius: 9rpx;
  background: #F4F7FA;
  color: #607187;
  font-size: 21rpx;
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
  margin-top: 14rpx;
  padding-top: 14rpx;
  border-top: 1rpx solid #F2F3F5;
}
.footer-service {
  min-width: 0;
  overflow: hidden;
  color: #7B899C;
  font-size: 21rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.badge-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
}
.badge-online .badge-dot {
  background: #23956A;
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
  color: #23956A;
}
.badge-offline .badge-text {
  color: #C9CDD4;
}
.card-actions {
  display: flex;
  flex: none;
  gap: 8rpx;
  margin-left: 12rpx;
}
.card-btn {
  padding: 8rpx 12rpx;
  border-radius: 9rpx;
  background: #F3F6F9;
  transition: opacity 150ms ease;
}
.card-btn:active {
  opacity: 0.55;
}
.card-btn-text {
  font-size: 21rpx;
  color: #4E5969;
  font-weight: 500;
}
.card-btn-text-edit {
  color: #2E6DD1;
  font-weight: 600;
}
.card-btn-edit { background: #EEF4FD; }
.card-btn-delete { background: #FFF1F1; }
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
.empty-icon {
  margin-bottom: 32rpx;
}
.empty-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #20364D;
  margin-bottom: 12rpx;
}
.empty-desc {
  font-size: 26rpx;
  color: #86909C;
  margin-bottom: 40rpx;
}
.empty-btn {
  padding: 20rpx 64rpx;
  background: linear-gradient(135deg, #2E6DD1 0%, #2E6DD1 100%);
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
