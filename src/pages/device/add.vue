<template>
  <view class="page apple-page motion-page-sheet">
    <view class="scroll-content">
      <view class="form-section">
        <view class="form-label">
          <view class="label-bar"></view>
          <text class="label-text">设备类型</text>
        </view>
        <view class="type-toggle">
          <view
            class="type-option"
            :class="{ 'type-option-active': deviceType === 'fixed' }"
            @tap="deviceType = 'fixed'"
          >
            <app-icon-tile class="type-pictogram" name="fixed-device" tone="blue" size="large" />
            <text class="type-option-label">固定设备</text>
            <text class="type-option-desc">放置在固定位置</text>
            <view v-if="deviceType === 'fixed'" class="type-check">
              <app-icon class="type-check-icon" name="checkmarkempty" :size="14" color="#FFFFFF" />
            </view>
          </view>
          <view
            class="type-option"
            :class="{ 'type-option-active': deviceType === 'mobile' }"
            @tap="deviceType = 'mobile'"
          >
            <app-icon-tile class="type-pictogram" name="mobile-device" tone="green" size="large" />
            <text class="type-option-label">移动设备</text>
            <text class="type-option-desc">车载便携设备</text>
            <view v-if="deviceType === 'mobile'" class="type-check">
              <app-icon class="type-check-icon" name="checkmarkempty" :size="14" color="#FFFFFF" />
            </view>
          </view>
        </view>
      </view>

      <view class="form-section">
        <view class="form-label">
          <view class="label-bar"></view>
          <text class="label-text">设备品类</text>
        </view>
        <view class="category-selector">
          <view
            v-for="cat in categoryOptions"
            :key="cat.value"
            class="category-option"
            :class="{ 'category-option-active': category === cat.value }"
            @tap="category = cat.value"
          >
            <app-icon-tile :name="cat.icon" :tone="cat.tone" />
            <text class="category-label">{{ cat.label }}</text>
          </view>
        </view>
      </view>

      <template v-if="deviceType === 'fixed'">
        <view class="form-section">
          <view class="form-label">
            <view class="label-bar"></view>
            <text class="label-text">设备名称</text>
            <text class="label-required">*</text>
          </view>
          <view class="input-wrap">
            <input
              v-model="fixedForm.name"
              class="form-input"
              placeholder="请输入设备名称"
              placeholder-class="input-placeholder"
            />
          </view>
        </view>

        <view class="form-section">
          <view class="form-label">
            <view class="label-bar"></view>
            <text class="label-text">安装地址</text>
            <text class="label-required">*</text>
          </view>
          <view class="input-wrap">
            <input
              v-model="fixedForm.address"
              class="form-input"
              placeholder="请输入详细地址"
              placeholder-class="input-placeholder"
            />
          </view>
        </view>

        <view class="form-section">
          <view class="form-label">
            <view class="label-bar"></view>
            <text class="label-text">经纬度</text>
          </view>
          <view class="location-picker" @tap="openLocationPicker">
            <view class="location-info">
              <text v-if="fixedForm.longitude" class="location-text">
                {{ fixedForm.longitude.toFixed(4) }}, {{ fixedForm.latitude.toFixed(4) }}
              </text>
              <text v-else class="location-placeholder">点击在地图上选取位置</text>
            </view>
            <view class="location-btn">
              <text class="location-btn-text">选取</text>
            </view>
          </view>
          <view v-if="pickedAddress" class="picked-address">
            <app-icon class="picked-address-icon" name="location-filled" :size="18" color="#1F63D5" />
            <text class="picked-address-text">{{ pickedAddress }}</text>
          </view>
        </view>

        <view class="form-section">
          <view class="form-label">
            <view class="label-bar"></view>
            <text class="label-text">有效期</text>
          </view>
          <view class="input-wrap">
            <picker
              mode="date"
              :value="fixedForm.expireDate"
              @change="onExpireDateChange"
            >
              <view class="picker-display">
                <text v-if="fixedForm.expireDate" class="picker-value">{{ fixedForm.expireDate }}</text>
                <text v-else class="picker-placeholder">请选择有效期</text>
                <app-icon class="picker-arrow" name="right" :size="17" color="#96A1B3" />
              </view>
            </picker>
          </view>
        </view>

        <view class="form-section">
          <view class="form-label">
            <view class="label-bar"></view>
            <text class="label-text">使用说明</text>
          </view>
          <view class="textarea-wrap">
            <textarea
              v-model="fixedForm.instructions"
              class="form-textarea"
              placeholder="请输入设备使用说明"
              placeholder-class="input-placeholder"
              maxlength="500"
              :auto-height="false"
            />
            <text class="textarea-count">{{ fixedForm.instructions.length }}/500</text>
          </view>
        </view>

        <view class="form-section">
          <view class="form-label">
            <view class="label-bar"></view>
            <text class="label-text">设备图片</text>
            <text class="label-hint">（最多3张）</text>
          </view>
          <view class="photo-upload">
            <view
              v-for="(img, idx) in fixedForm.images"
              :key="idx"
              class="photo-item"
            >
              <image class="photo-img" :src="displayImageSource(img)" mode="aspectFill" />
              <view class="photo-delete" @tap="removeImage(idx)">
                <app-icon class="delete-icon" name="closeempty" :size="13" color="#FFFFFF" />
              </view>
            </view>
            <view
              v-if="fixedForm.images.length < 3"
              class="photo-add"
              @tap="addImage"
            >
              <text class="add-icon">+</text>
              <text class="add-label">上传图片</text>
            </view>
          </view>
        </view>
      </template>

      <template v-if="deviceType === 'mobile'">
        <view class="form-section">
          <view class="form-label">
            <view class="label-bar"></view>
            <text class="label-text">设备名称</text>
            <text class="label-required">*</text>
          </view>
          <view class="input-wrap">
            <input
              v-model="mobileForm.name"
              class="form-input"
              placeholder="请输入设备名称"
              placeholder-class="input-placeholder"
            />
          </view>
        </view>

        <view class="form-section">
          <view class="form-label">
            <view class="label-bar"></view>
            <text class="label-text">设备品类</text>
          </view>
          <view class="input-wrap">
            <input
              v-model="mobileForm.deviceCategory"
              class="form-input"
              placeholder="如：便携AED、急救包等"
              placeholder-class="input-placeholder"
            />
          </view>
        </view>

        <view class="form-section">
          <view class="form-label">
            <view class="label-bar"></view>
            <text class="label-text">车辆信息</text>
            <text class="label-required">*</text>
          </view>
          <view class="input-wrap">
            <input
              v-model="mobileForm.vehicleInfo"
              class="form-input"
              placeholder="如：浙A·8K923 白色SUV"
              placeholder-class="input-placeholder"
            />
          </view>
        </view>

        <view class="form-section">
          <view class="form-label">
            <view class="label-bar"></view>
            <text class="label-text">服务范围</text>
          </view>
          <view class="range-selector">
            <view
              v-for="r in rangeOptions"
              :key="r.value"
              class="range-option"
              :class="{ 'range-option-active': mobileForm.serviceRange === r.value }"
              @tap="mobileForm.serviceRange = r.value"
            >
              <text class="range-label">{{ r.label }}</text>
            </view>
          </view>
        </view>

        <view class="form-section">
          <view class="form-label">
            <view class="label-bar"></view>
            <text class="label-text">可用时段</text>
          </view>
          <view class="time-selector">
            <view
              v-for="t in timeOptions"
              :key="t.value"
              class="time-option"
              :class="{ 'time-option-active': mobileForm.serviceTime === t.value }"
              @tap="mobileForm.serviceTime = t.value"
            >
              <text class="time-label">{{ t.label }}</text>
            </view>
          </view>
        </view>

        <view class="form-section">
          <view class="form-label">
            <view class="label-bar"></view>
            <text class="label-text">设备图片</text>
            <text class="label-hint">（可选）</text>
          </view>
          <view class="photo-upload">
            <view
              v-for="(img, idx) in mobileForm.deviceImages"
              :key="'dev-' + idx"
              class="photo-item"
            >
              <image class="photo-img" :src="displayImageSource(img)" mode="aspectFill" />
              <view class="photo-delete" @tap="removeDeviceImage(idx)">
                <app-icon class="delete-icon" name="closeempty" :size="13" color="#FFFFFF" />
              </view>
            </view>
            <view
              v-if="mobileForm.deviceImages.length < 9"
              class="photo-add"
              @tap="addDeviceImage"
            >
              <text class="add-icon">+</text>
              <text class="add-label">上传图片</text>
            </view>
          </view>
        </view>

        <view class="form-section">
          <view class="form-label">
            <view class="label-bar"></view>
            <text class="label-text">车辆图片</text>
            <text class="label-hint">（可选）</text>
          </view>
          <view class="photo-upload">
            <view
              v-for="(img, idx) in mobileForm.vehicleImages"
              :key="'veh-' + idx"
              class="photo-item"
            >
              <image class="photo-img" :src="displayImageSource(img)" mode="aspectFill" />
              <view class="photo-delete" @tap="removeVehicleImage(idx)">
                <app-icon class="delete-icon" name="closeempty" :size="13" color="#FFFFFF" />
              </view>
            </view>
            <view
              v-if="mobileForm.vehicleImages.length < 9"
              class="photo-add"
              @tap="addVehicleImage"
            >
              <text class="add-icon">+</text>
              <text class="add-label">上传图片</text>
            </view>
          </view>
        </view>
      </template>

      <view class="submit-area">
        <view class="submit-btn" :class="{ 'submit-disabled': isSubmitting }" @tap="handleSubmit">
          <text class="submit-text">{{ isSubmitting ? '提交中...' : (editingId ? '保存修改' : '提交录入') }}</text>
        </view>
      </view>

      <view class="bottom-safe"></view>
    </view>

    <assistant-companion context="devices" />
  </view>
</template>

<script setup lang="ts">
import AssistantCompanion from '@/components/AssistantCompanion.vue'
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import { chooseGcj02Location, getCurrentGcj02Location } from '@/utils/location'
import { uploadImage } from '@/api/files'
import { resolveApiUrl } from '@/api/http'
import { userFacingError } from '@/utils/presentation'
import {
  createEmergencyDevice,
  getEmergencyDevice,
  updateEmergencyDevice,
  type SaveEmergencyDeviceRequest
} from '@/api/devices'

const deviceType = ref<'fixed' | 'mobile'>('fixed')

const category = ref('AED')
const categoryOptions: Array<{
  value: string
  label: string
  icon: string
  tone: 'coral' | 'green' | 'amber'
}> = [
  { value: 'AED', label: 'AED', icon: 'aed', tone: 'coral' },
  { value: '急救箱', label: '急救箱', icon: 'rescue-kit', tone: 'green' },
  { value: '急救包', label: '急救包', icon: 'rescue-bag', tone: 'amber' }
]

const fixedForm = ref({
  name: '',
  address: '',
  longitude: 0,
  latitude: 0,
  expireDate: '',
  instructions: '',
  images: [] as string[]
})

const mobileForm = ref({
  name: '',
  deviceCategory: '',
  vehicleInfo: '',
  serviceRange: 5,
  serviceTime: '全天',
  deviceImages: [] as string[],
  vehicleImages: [] as string[]
})

const rangeOptions = [
  { value: 3, label: '3km' },
  { value: 5, label: '5km' },
  { value: 8, label: '8km' },
  { value: 10, label: '10km' }
]

const timeOptions = [
  { value: '全天', label: '全天' },
  { value: '工作日 8:00-18:00', label: '工作日白天' },
  { value: '工作日 9:00-17:00', label: '工作日' },
  { value: '周末', label: '周末' }
]

const isSubmitting = ref(false)
const editingId = ref('')

const pickedAddress = ref('')

function triggerImageUpload(count: number, callback: (url: string) => void) {
  uni.chooseImage({
    count,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      const filePaths = Array.isArray(res.tempFilePaths)
        ? res.tempFilePaths
        : [res.tempFilePaths]
      filePaths.forEach((url: string) => callback(url))
    }
  })
}

function displayImageSource(path: string) {
  return path.startsWith('/api/') ? resolveApiUrl(path) : path
}

async function persistImagePaths(paths: string[], purpose: 'DEVICE_IMAGE' | 'VEHICLE_IMAGE') {
  return Promise.all(paths.map(async (path) => {
    if (path.startsWith('/api/') || /^https?:\/\//.test(path)) return path
    return (await uploadImage(path, purpose)).mediaId
  }))
}

async function openLocationPicker() {
  const existing = fixedForm.value
  try {
    const location = await chooseGcj02Location(existing.longitude || existing.latitude ? {
      longitude: existing.longitude, latitude: existing.latitude, address: pickedAddress.value || existing.address
    } : undefined)
    const form = fixedForm.value
    form.longitude = location.longitude
    form.latitude = location.latitude
    pickedAddress.value = location.address || `${location.longitude.toFixed(5)}, ${location.latitude.toFixed(5)}`
    if (location.address && !form.address) form.address = location.address
  } catch (error) {
    const message = userFacingError(error, '位置选择失败，请重试')
    if (!/取消|cancel/i.test(message)) uni.showToast({ title: message, icon: 'none' })
  }
}

function onExpireDateChange(e: any) {
  fixedForm.value.expireDate = e.detail.value
}

function addImage() {
  triggerImageUpload(3 - fixedForm.value.images.length, (url: string) => {
    if (fixedForm.value.images.length < 3) {
      fixedForm.value.images.push(url)
    }
  })
}

function removeImage(idx: number) {
  fixedForm.value.images.splice(idx, 1)
}

function addDeviceImage() {
  triggerImageUpload(9 - mobileForm.value.deviceImages.length, (url: string) => {
    mobileForm.value.deviceImages.push(url)
  })
}

function removeDeviceImage(idx: number) {
  mobileForm.value.deviceImages.splice(idx, 1)
}

function addVehicleImage() {
  triggerImageUpload(9 - mobileForm.value.vehicleImages.length, (url: string) => {
    mobileForm.value.vehicleImages.push(url)
  })
}

function removeVehicleImage(idx: number) {
  mobileForm.value.vehicleImages.splice(idx, 1)
}

async function loadEditingDevice(id: string) {
  try {
    const device = await getEmergencyDevice(id)
    deviceType.value = device.type === 'FIXED' ? 'fixed' : 'mobile'
    category.value = device.category
    if (device.type === 'FIXED') {
      fixedForm.value = {
        name: device.name,
        address: device.address,
        longitude: device.longitude,
        latitude: device.latitude,
        expireDate: device.expireDate || '',
        instructions: device.instructions || '',
        images: device.imageUrls
      }
      pickedAddress.value = device.address
    } else {
      mobileForm.value = {
        name: device.name,
        deviceCategory: device.category,
        vehicleInfo: device.vehicleInfo || '',
        serviceRange: device.serviceRange || 5,
        serviceTime: device.serviceTime || '全天',
        deviceImages: device.imageUrls,
        vehicleImages: device.vehicleImageUrls
      }
    }
  } catch {
    uni.showToast({ title: '设备信息加载失败', icon: 'none' })
  }
}

onLoad((query) => {
  if (query?.id) {
    editingId.value = String(query.id)
    loadEditingDevice(editingId.value)
  }
})

async function handleSubmit() {
  if (isSubmitting.value) return

  if (deviceType.value === 'fixed') {
    if (!fixedForm.value.name) {
      uni.showToast({ title: '请输入设备名称', icon: 'none' })
      return
    }
    if (!fixedForm.value.address) {
      uni.showToast({ title: '请输入安装地址', icon: 'none' })
      return
    }
    if (!fixedForm.value.longitude || !fixedForm.value.latitude) {
      uni.showToast({ title: '请选择设备位置', icon: 'none' })
      return
    }
  } else {
    if (!mobileForm.value.name) {
      uni.showToast({ title: '请输入设备名称', icon: 'none' })
      return
    }
    if (!mobileForm.value.vehicleInfo) {
      uni.showToast({ title: '请输入车辆信息', icon: 'none' })
      return
    }
  }

  isSubmitting.value = true

  try {
    const location = deviceType.value === 'mobile'
      ? await getCurrentGcj02Location()
      : { longitude: fixedForm.value.longitude, latitude: fixedForm.value.latitude }
    const [imageUrls, vehicleImageUrls] = deviceType.value === 'fixed'
      ? [await persistImagePaths(fixedForm.value.images, 'DEVICE_IMAGE'), []]
      : await Promise.all([
          persistImagePaths(mobileForm.value.deviceImages, 'DEVICE_IMAGE'),
          persistImagePaths(mobileForm.value.vehicleImages, 'VEHICLE_IMAGE')
        ])
    const payload: SaveEmergencyDeviceRequest = deviceType.value === 'fixed'
      ? {
          type: 'FIXED',
          category: category.value,
          name: fixedForm.value.name.trim(),
          address: fixedForm.value.address.trim(),
          longitude: location.longitude,
          latitude: location.latitude,
          serviceTime: '全天',
          expireDate: fixedForm.value.expireDate || undefined,
          owner: '个人录入',
          instructions: fixedForm.value.instructions.trim() || undefined,
          imageUrls,
          vehicleImageUrls
        }
      : {
          type: 'MOBILE',
          category: mobileForm.value.deviceCategory.trim() || category.value,
          name: mobileForm.value.name.trim(),
          address: '移动设备实时位置',
          longitude: location.longitude,
          latitude: location.latitude,
          serviceTime: mobileForm.value.serviceTime,
          owner: '个人录入',
          vehicleInfo: mobileForm.value.vehicleInfo.trim(),
          serviceRange: mobileForm.value.serviceRange,
          imageUrls,
          vehicleImageUrls
        }

    if (editingId.value) {
      await updateEmergencyDevice(editingId.value, payload)
    } else {
      await createEmergencyDevice(payload)
    }
    isSubmitting.value = false
    uni.showToast({
      title: editingId.value ? '设备修改成功' : '已提交平台审核',
      icon: 'success',
      duration: 1200
    })
    setTimeout(() => {
      uni.navigateBack()
    }, 800)
  } catch (error: any) {
    isSubmitting.value = false
    uni.showToast({ title: userFacingError(error, '提交失败，请重试'), icon: 'none' })
  }
}

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

.form-section {
  padding: 28rpx 32rpx 0;
}
.form-label {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
}
.label-bar {
  width: 6rpx;
  height: 28rpx;
  border-radius: 3rpx;
  background: #2E6DD1;
  margin-right: 12rpx;
}
.label-text {
  font-size: 28rpx;
  font-weight: 600;
  color: #20364D;
}
.label-required {
  font-size: 28rpx;
  color: #C93D46;
  margin-left: 4rpx;
}
.label-hint {
  font-size: 22rpx;
  color: #C9CDD4;
  margin-left: 8rpx;
}

.type-toggle {
  display: flex;
  gap: 20rpx;
}
.type-option {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 32rpx 24rpx;
  background: #FFFFFF;
  border-radius: 20rpx;
  border: 1rpx solid #DDE5EF;
  transition: border-color 150ms ease, background-color 150ms ease;
}
.type-option-active {
  border-color: #2E6DD1;
  background: #F0F6FF;
}
.type-pictogram {
  margin-bottom: 16rpx;
}
.type-option-label {
  font-size: 28rpx;
  font-weight: 600;
  color: #20364D;
}
.type-option-active .type-option-label {
  color: #2E6DD1;
}
.type-option-desc {
  font-size: 22rpx;
  color: #86909C;
  margin-top: 4rpx;
}
.type-check {
  position: absolute;
  top: 0;
  right: 0;
  width: 48rpx;
  height: 48rpx;
  border-radius: 0 18rpx 0 18rpx;
  background: #2E6DD1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.type-check-icon {
  font-size: 22rpx;
  color: #FFFFFF;
  font-weight: 700;
}

.category-selector {
  display: flex;
  gap: 16rpx;
}
.category-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  padding: 20rpx 12rpx;
  background: #FFFFFF;
  border-radius: 16rpx;
  border: 1rpx solid #DDE5EF;
  transition: border-color 150ms ease, background-color 150ms ease;
}
.category-option-active {
  border-color: #2E6DD1;
  background: rgba(43, 111, 240, 0.06);
}
.category-label {
  font-size: 26rpx;
  color: #4E5969;
  font-weight: 500;
}
.category-option-active .category-label {
  color: #2E6DD1;
  font-weight: 600;
}

.input-wrap {
  background: #FFFFFF;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}
.form-input {
  width: 100%;
  height: 88rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #20364D;
  box-sizing: border-box;
}
.input-placeholder {
  color: #C9CDD4;
  font-size: 28rpx;
}

.location-picker {
  display: flex;
  align-items: center;
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 0 24rpx;
  height: 88rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}
.location-info {
  flex: 1;
}
.location-text {
  font-size: 28rpx;
  color: #20364D;
  font-weight: 500;
}
.location-placeholder {
  font-size: 28rpx;
  color: #C9CDD4;
}
.location-btn {
  padding: 12rpx 28rpx;
  background: #007AFF;
  border-radius: 24rpx;
  margin-left: 16rpx;
}
.location-btn-text {
  font-size: 24rpx;
  color: #FFFFFF;
  font-weight: 600;
}

.picker-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 88rpx;
  padding: 0 24rpx;
}
.picker-value {
  font-size: 28rpx;
  color: #20364D;
}
.picker-placeholder {
  font-size: 28rpx;
  color: #C9CDD4;
}
.picker-arrow {
  font-size: 32rpx;
  color: #C9CDD4;
}

.textarea-wrap {
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 24rpx;
  position: relative;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}
.form-textarea {
  width: 100%;
  height: 200rpx;
  font-size: 28rpx;
  color: #20364D;
  line-height: 1.6;
}
.textarea-count {
  position: absolute;
  bottom: 16rpx;
  right: 24rpx;
  font-size: 22rpx;
  color: #C9CDD4;
}

.photo-upload {
  display: flex;
  gap: 20rpx;
  flex-wrap: wrap;
}
.photo-item {
  position: relative;
  width: 200rpx;
  height: 200rpx;
  border-radius: 16rpx;
  overflow: hidden;
}
.photo-img {
  width: 100%;
  height: 100%;
}
.photo-delete {
  position: absolute;
  top: 8rpx;
  right: 8rpx;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}
.delete-icon {
  font-size: 22rpx;
  color: #FFFFFF;
}
.photo-add {
  width: 200rpx;
  height: 200rpx;
  border-radius: 16rpx;
  border: 2rpx dashed #C9CDD4;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  background: #FFFFFF;
}
.add-icon {
  font-size: 56rpx;
  color: #C9CDD4;
  line-height: 1;
}
.add-label {
  font-size: 22rpx;
  color: #C9CDD4;
}

.range-selector {
  display: flex;
  gap: 16rpx;
}
.range-option {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20rpx 0;
  background: #FFFFFF;
  border-radius: 16rpx;
  border: 2rpx solid #E5E6EB;
  transition: all 0.3s ease;
}
.range-option-active {
  border-color: #2E6DD1;
  background: rgba(43, 111, 240, 0.06);
}
.range-label {
  font-size: 26rpx;
  color: #4E5969;
  font-weight: 500;
}
.range-option-active .range-label {
  color: #2E6DD1;
  font-weight: 600;
}

.time-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.time-option {
  padding: 16rpx 32rpx;
  background: #FFFFFF;
  border-radius: 32rpx;
  border: 2rpx solid #E5E6EB;
  transition: all 0.3s ease;
}
.time-option-active {
  border-color: #2E6DD1;
  background: rgba(43, 111, 240, 0.06);
}
.time-label {
  font-size: 26rpx;
  color: #4E5969;
  font-weight: 500;
}
.time-option-active .time-label {
  color: #2E6DD1;
  font-weight: 600;
}

.submit-area {
  padding: 48rpx 32rpx;
}
.submit-btn {
  width: 100%;
  height: 96rpx;
  border-radius: 48rpx;
  background: #007AFF;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 32rpx rgba(43, 111, 240, 0.35);
  transition: all 0.3s ease;
}
.submit-btn:active {
  transform: scale(0.98);
}
.submit-disabled {
  opacity: 0.6;
  box-shadow: none;
}
.submit-text {
  font-size: 32rpx;
  font-weight: 700;
  color: #FFFFFF;
  letter-spacing: 2rpx;
}

.bottom-safe {
  height: 60rpx;
}

.picked-address {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 12rpx;
  padding: 12rpx 20rpx;
  background: rgba(43, 111, 240, 0.06);
  border-radius: 12rpx;
}
.picked-address-icon {
  font-size: 24rpx;
}
.picked-address-text {
  font-size: 24rpx;
  color: #2E6DD1;
  font-weight: 500;
}

</style>
