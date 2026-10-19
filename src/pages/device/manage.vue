<template>
  <view class="page apple-page motion-page-list">
    <view class="scroll-content">
      <view class="page-actions">
        <view class="device-summary">
          <text class="summary-number">{{ fixedDevices.length + mobileDevices.length }}</text>
          <text class="summary-label">台设备已登记</text>
        </view>
        <view class="page-add-action" @tap="goAdd">
          <app-icon name="plus" :size="18" color="#007AFF" />
          <text>添加</text>
        </view>
      </view>
      <view class="coordination-entry" @tap="openCoordination">
        <view><text class="coordination-entry-title">资源协同</text><text class="coordination-entry-description">查看设备变化后的检查与处理结果</text></view>
        <app-icon name="right" :size="16" color="#007AFF" />
      </view>
      <view class="tab-bar">
        <view
          class="tab-item"
          :class="{ 'tab-active': activeTab === 'fixed' }"
          @tap="activeTab = 'fixed'"
        >
          <text class="tab-text">固定设备</text>
          <text class="tab-count">{{ fixedDevices.length }}</text>
        </view>
        <view
          class="tab-item"
          :class="{ 'tab-active': activeTab === 'mobile' }"
          @tap="activeTab = 'mobile'"
        >
          <text class="tab-text">移动设备</text>
          <text class="tab-count">{{ mobileDevices.length }}</text>
        </view>
      </view>

      <view class="device-list">
        <view v-if="currentDevices.length > 0">
          <app-list-row
            v-for="device in currentDevices"
            :key="device.id"
            class="device-card"
            :class="'device-card-' + statusTone(device)"
            :icon="device.type === 'FIXED' ? 'fixed-device' : 'mobile-device'"
            :tone="device.type === 'FIXED' ? 'blue' : 'green'"
            :title="device.name"
            :summary="device.address"
            :meta="`${device.category} · ${device.serviceTime || '服务时段未设置'}`"
            :show-chevron="false"
            @tap="openDeviceActions(device)"
          >
            <template #accessory>
              <view class="card-trailing">
                <view v-if="canToggleDevice(device)" class="switch-wrap" @tap.stop>
                  <text class="switch-label" :class="{ 'switch-label-online': device.online }">{{ deviceStatusLabel(device) }}</text>
                  <app-switch :model-value="device.online" @update:model-value="toggleCurrentStatus(device)" />
                </view>
                <view v-else class="review-status" :class="'review-' + device.status.toLowerCase()">
                  {{ deviceStatusLabel(device) }}
                </view>
                <app-icon name="right" :size="15" color="#C7C7CC" />
              </view>
            </template>
          </app-list-row>
        </view>

        <view v-else class="empty-state">
          <app-icon-tile
            class="empty-icon"
            :name="activeTab === 'fixed' ? 'fixed-device' : 'mobile-device'"
            :tone="activeTab === 'fixed' ? 'blue' : 'green'"
            size="large"
          />
          <text class="empty-title">{{ activeTab === 'fixed' ? '暂无固定设备' : '暂无移动设备' }}</text>
          <text class="empty-desc">还没有添加这类急救设备</text>
          <view class="empty-btn" @tap="goAdd">
            <text class="empty-btn-text">添加设备</text>
          </view>
        </view>
      </view>

      <view class="bottom-safe"></view>
    </view>
    <app-action-sheet
      v-model:visible="sheetVisible"
      :title="selectedDevice?.name || ''"
      :message="selectedDevice ? `${selectedDevice.address} · ${selectedDevice.serviceTime || '服务时段未设置'}` : ''"
      :actions="deviceActions"
      @select="handleDeviceAction"
    />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import AppSwitch from '@/components/AppSwitch.vue'
import AppActionSheet, { type AppActionSheetItem } from '@/components/AppActionSheet.vue'
import AppListRow from '@/components/AppListRow.vue'
import { getCurrentGcj02Location } from '@/utils/location'
import { deviceStatusLabel as presentDeviceStatus, userFacingError } from '@/utils/presentation'
import {
  listMyEmergencyDevices,
  deleteEmergencyDevice,
  enableEmergencyDevice,
  disableEmergencyDevice,
  reportEmergencyDeviceFault,
  updateEmergencyDeviceLocation,
  type EmergencyDeviceResponse
} from '@/api/devices'

const activeTab = ref<'fixed' | 'mobile'>('fixed')
const sheetVisible = ref(false)
const selectedDevice = ref<DeviceView | null>(null)

type DeviceView = EmergencyDeviceResponse & {
  online: boolean
}

const fixedDevices = ref<DeviceView[]>([])
const mobileDevices = ref<DeviceView[]>([])
const currentDevices = computed(() => (
  activeTab.value === 'fixed' ? fixedDevices.value : mobileDevices.value
))
let syncingLocation = false
const deviceActions = computed<AppActionSheetItem[]>(() => {
  if (!selectedDevice.value) return []
  const actions: AppActionSheetItem[] = []
  if (selectedDevice.value.type === 'MOBILE') actions.push({ label: '更新当前位置' })
  actions.push({ label: '查看协同记录' })
  if (['AVAILABLE', 'RESERVED'].includes(selectedDevice.value.status)) actions.push({ label: '报告设备故障', destructive: true })
  if (selectedDevice.value.status === 'DISABLED' && selectedDevice.value.hasActiveReservation) actions.push({ label: '确认修复并恢复' })
  if (!selectedDevice.value.hasActiveReservation) {
    actions.push({ label: '编辑设备' })
    actions.push({ label: '删除设备', destructive: true })
  }
  return actions
})

function toView(device: EmergencyDeviceResponse): DeviceView {
  return {
    ...device,
    online: device.type === 'MOBILE'
      ? device.mobilePresenceStatus === 'ONLINE'
      : device.status === 'AVAILABLE'
  }
}

function canToggleDevice(device: DeviceView) {
  return !device.hasActiveReservation && ['AVAILABLE', 'DISABLED'].includes(device.status)
}

function deviceStatusLabel(device: DeviceView) {
  return presentDeviceStatus(device)
}

function statusTone(device: DeviceView) {
  if (device.type === 'MOBILE' && device.mobilePresenceStatus !== 'ONLINE') return 'offline'
  if (device.status === 'AVAILABLE') return 'online'
  if (device.status === 'PENDING_REVIEW') return 'pending'
  if (device.status === 'REJECTED' || device.status === 'EXPIRED') return 'rejected'
  return 'offline'
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
  const nextStatus = device.status === 'AVAILABLE' ? 'DISABLED' : 'AVAILABLE'
  try {
    const updated = nextStatus === 'AVAILABLE'
      ? await enableEmergencyDevice(device.id)
      : await disableEmergencyDevice(device.id)
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
  const nextStatus = device.status === 'AVAILABLE' ? 'DISABLED' : 'AVAILABLE'
  try {
    if (nextStatus === 'AVAILABLE') {
      const location = await getCurrentLocation()
      await updateEmergencyDeviceLocation(device.id, {
        ...location,
        address: `实时位置 · ${location.latitude.toFixed(5)}, ${location.longitude.toFixed(5)}`
      })
    }
    const updated = nextStatus === 'AVAILABLE'
      ? await enableEmergencyDevice(device.id)
      : await disableEmergencyDevice(device.id)
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
    uni.showToast({ title: userFacingError(error, '位置更新失败，请稍后重试'), icon: 'none' })
  } finally {
    syncingLocation = false
  }
}

function editDevice(device: DeviceView) {
  uni.navigateTo({
    url: '/pages/device/add?id=' + device.id
  })
}

function openDeviceActions(device: DeviceView) {
  selectedDevice.value = device
  sheetVisible.value = true
}

function handleDeviceAction(index: number) {
  const device = selectedDevice.value
  if (!device) return
  const action = deviceActions.value[index]?.label
  if (action === '更新当前位置') return reportMobileLocation(device)
  if (action === '查看协同记录') return openCoordination()
  if (action === '报告设备故障') return confirmFault(device)
  if (action === '确认修复并恢复') return confirmRestore(device)
  if (action === '编辑设备') return editDevice(device)
  if (action === '删除设备') confirmDelete(device)
}

function openCoordination() { uni.navigateTo({ url: '/pages/agent/index' }) }

function confirmFault(device: DeviceView) {
  uni.showModal({
    title: '报告设备故障',
    content: `确认“${device.name}”无法正常使用？系统将停用设备并检查关联救援；已有志愿者响应的任务需人工核实。`,
    confirmText: '确认上报',
    success: async result => {
      if (!result.confirm) return
      try {
        Object.assign(device, toView(await reportEmergencyDeviceFault(device.id)))
        uni.showToast({ title: '已上报，正在检查关联任务', icon: 'none' })
        openCoordination()
      } catch (error) { uni.showToast({ title: userFacingError(error, '故障上报失败'), icon: 'none' }) }
    }
  })
}

function confirmRestore(device: DeviceView) {
  uni.showModal({
    title: '确认设备已修复',
    content: '请与相关志愿者核实设备已可正常取用。恢复后保留原救援的设备预留与保管关系。',
    confirmText: '确认恢复',
    success: async result => {
      if (!result.confirm) return
      try {
        Object.assign(device, toView(await enableEmergencyDevice(device.id)))
        uni.showToast({ title: '已恢复原救援设备安排', icon: 'none' })
      } catch (error) { uni.showToast({ title: userFacingError(error, '恢复失败'), icon: 'none' }) }
    }
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
.coordination-entry { margin: 8rpx 28rpx 24rpx; padding: 24rpx; display: flex; align-items: center; justify-content: space-between; gap: 20rpx; border-radius: 20rpx; background: #edf4ff; }
.coordination-entry-title { display: block; font-size: 28rpx; font-weight: 600; color: #2865b6; }
.coordination-entry-description { display: block; margin-top: 8rpx; color: #636366; font-size: 23rpx; }
.page {
  min-height: 100vh;
  background: #F2F2F7;
}

.review-status {
  color: #B66A10;
  font-size: 21rpx;
  font-weight: 500;
}
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
  color: #1C1C1E;
  font-size: 34rpx;
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
  gap: 7rpx;
  min-height: 62rpx;
  padding: 0 4rpx 0 18rpx;
  font-size: 24rpx;
  font-weight: 600;
  color: #007AFF;
}
.page-add-action:active { opacity: .45; }

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
.device-list {
  margin: 18rpx 24rpx 0;
  overflow: hidden;
  border: 1rpx solid rgba(60, 60, 67, .14);
  border-radius: 20rpx;
  background: #FFFFFF;
  box-shadow: none;
}

.device-card {
  position: relative;
  margin-left: 20rpx;
  overflow: visible;
  padding: 20rpx 22rpx 20rpx 0;
  border: 0;
  border-radius: 0;
  background: #FFFFFF;
}
.device-card:active { background: #F8FAFC; }
.card-trailing {
  display: flex;
  flex: none;
  align-items: center;
  gap: 12rpx;
}
.switch-wrap { display: flex; flex-direction: column; align-items: flex-end; gap: 7rpx; }
.switch-label { color: #8E8E93; font-size: 19rpx; }
.switch-label-online { color: #24865F; }

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 82rpx 24rpx;
}
.empty-icon {
  margin-bottom: 20rpx;
}
.empty-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #20364D;
  margin-bottom: 12rpx;
}
.empty-desc {
  font-size: 22rpx;
  color: #86909C;
  margin-bottom: 26rpx;
}
.empty-btn {
  padding: 14rpx 34rpx;
  background: #2E6DD1;
  border-radius: 14rpx;
  box-shadow: none;
}
.empty-btn:active {
  transform: scale(0.97);
}
.empty-btn-text {
  font-size: 23rpx;
  color: #FFFFFF;
  font-weight: 600;
}

.bottom-safe {
  height: 60rpx;
}
</style>
