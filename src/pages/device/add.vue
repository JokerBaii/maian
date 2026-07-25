<template>
  <view class="page">
    <scroll-view class="scroll-content" scroll-y>
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
            <view class="type-option-icon type-icon-fixed">
              <app-icon class="type-icon-text" name="cart-filled" :size="28" color="#1F63D5" />
            </view>
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
            <view class="type-option-icon type-icon-mobile">
              <app-icon class="type-icon-text" name="navigate-filled" :size="28" color="#158F63" />
            </view>
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
            <text class="category-emoji">{{ cat.icon }}</text>
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
    </scroll-view>

    <!-- #ifdef H5 -->
    <view v-if="showLocationPicker" class="location-picker-overlay">
      <view class="lp-header" :style="{ paddingTop: statusBarHeight + 'px' }">
        <view class="lp-header-bar">
          <view class="lp-back" @tap="cancelLocationPicker">
            <app-icon class="lp-back-icon" name="back" :size="22" color="#1D2A3E" />
          </view>
          <text class="lp-title">选取位置</text>
          <view class="lp-confirm" @tap="confirmLocationPicker">
            <text class="lp-confirm-text">确定</text>
          </view>
        </view>
        <view class="lp-search-bar">
          <app-icon class="lp-search-icon" name="search" :size="18" color="#56627A" />
          <input
            class="lp-search-input"
            v-model="poiSearchText"
            placeholder="搜索地点名称或关键字"
            placeholder-class="lp-search-placeholder"
            confirm-type="search"
            @confirm="searchPOI"
          />
          <view v-if="poiSearchText" class="lp-search-clear" @tap="clearPOISearch">
            <app-icon class="lp-clear-icon" name="closeempty" :size="16" color="#6E7A90" />
          </view>
        </view>
      </view>

      <view v-if="poiResults.length > 0" class="poi-results">
        <scroll-view scroll-y class="poi-list">
          <view
            v-for="(poi, idx) in poiResults"
            :key="idx"
            class="poi-item"
            :class="{ 'poi-item-active': selectedPoiIdx === idx }"
            @tap="selectPOI(poi, idx)"
          >
            <view class="poi-item-left">
              <text class="poi-item-name">{{ poi.name }}</text>
              <text class="poi-item-addr">{{ poi.address || poi.name }}</text>
            </view>
            <view v-if="selectedPoiIdx === idx" class="poi-item-check">
              <app-icon class="poi-check-icon" name="checkmarkempty" :size="15" color="#FFFFFF" />
            </view>
          </view>
        </scroll-view>
      </view>

      <view id="lp-map-container" class="lp-map-container"></view>

      <view class="lp-center-marker">
        <view class="lp-marker-pin"></view>
        <view class="lp-marker-shadow"></view>
      </view>

      <view class="lp-bottom-info">
        <view class="lp-info-content">
          <text v-if="pickerAddress" class="lp-info-addr">{{ pickerAddress }}</text>
          <text v-else class="lp-info-addr lp-info-placeholder">点击地图或搜索选取位置</text>
          <text v-if="pickerLng" class="lp-info-coord">{{ pickerLng.toFixed(6) }}, {{ pickerLat.toFixed(6) }}</text>
        </view>
      </view>
    </view>
    <!-- #endif -->
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AppIcon from '@/components/AppIcon.vue'
// #ifdef H5
import { loadAMap } from '@/common/amap'
// #endif
import { uploadImage } from '@/api/files'
import { resolveApiUrl } from '@/api/http'
import {
  createEmergencyDevice,
  getEmergencyDevice,
  updateEmergencyDevice,
  type SaveEmergencyDeviceRequest
} from '@/api/devices'

const statusBarHeight = ref(0)
const systemInfo = uni.getSystemInfoSync()
statusBarHeight.value = systemInfo.statusBarHeight || 20

const deviceType = ref<'fixed' | 'mobile'>('fixed')

const category = ref('AED')
const categoryOptions = [
  { value: 'AED', label: 'AED', icon: 'AED' },
  { value: '急救箱', label: '急救箱', icon: '急救' },
  { value: '急救包', label: '急救包', icon: '急救' }
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

// #ifdef H5
const showLocationPicker = ref(false)
let pickerMapInstance: any = null
const pickerLng = ref(0)
const pickerLat = ref(0)
const pickerAddress = ref('')
const poiSearchText = ref('')
const poiResults = ref<any[]>([])
const selectedPoiIdx = ref(-1)
// #endif

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

async function persistImagePaths(paths: string[]) {
  return Promise.all(paths.map(async (path) => {
    if (path.startsWith('/api/') || /^https?:\/\//.test(path)) return path
    return (await uploadImage(path)).url
  }))
}

function openLocationPicker() {
  // #ifdef H5
  showLocationPicker.value = true
  poiResults.value = []
  selectedPoiIdx.value = -1
  poiSearchText.value = ''

  setTimeout(() => {
    initPickerMap()
  }, 300)
  // #endif
  // #ifndef H5
  uni.chooseLocation({
    latitude: fixedForm.value.latitude || undefined,
    longitude: fixedForm.value.longitude || undefined,
    success: (location) => {
      fixedForm.value.latitude = location.latitude
      fixedForm.value.longitude = location.longitude
      fixedForm.value.address = location.address || location.name || fixedForm.value.address
      pickedAddress.value = fixedForm.value.address
    }
  })
  // #endif
}

// #ifdef H5
function cancelLocationPicker() {
  showLocationPicker.value = false
  if (pickerMapInstance) {
    pickerMapInstance.destroy()
    pickerMapInstance = null
  }
  poiResults.value = []
}

function confirmLocationPicker() {
  if (pickerLng.value) {
    fixedForm.value.longitude = pickerLng.value
    fixedForm.value.latitude = pickerLat.value
    pickedAddress.value = pickerAddress.value
    if (pickerAddress.value && !fixedForm.value.address) {
      fixedForm.value.address = pickerAddress.value
    }
  }
  cancelLocationPicker()
}

async function initPickerMap() {
  const loaded = await loadAMap()
  if (!loaded) {
    const useLocation = (longitude: number, latitude: number) => {
      fixedForm.value.longitude = longitude
      fixedForm.value.latitude = latitude
      pickedAddress.value = fixedForm.value.address || '当前位置'
      uni.showToast({ title: '已使用当前位置', icon: 'none' })
      cancelLocationPicker()
    }
    uni.getLocation({
      type: 'gcj02',
      isHighAccuracy: true,
      success: (location) => {
        useLocation(location.longitude, location.latitude)
      },
      fail: () => {
        uni.showToast({ title: '请授权定位后重试', icon: 'none' })
        cancelLocationPicker()
      }
    })
    return
  }
  const AMap = (window as any).AMap

  const center = fixedForm.value.longitude
    ? [fixedForm.value.longitude, fixedForm.value.latitude]
    : [120.15, 30.28]

  pickerMapInstance = new AMap.Map('lp-map-container', {
    zoom: 15,
    center,
    mapStyle: 'amap://styles/whitesmoke',
    resizeEnable: true
  })

  pickerMapInstance.on('click', (e: any) => {
    pickerLng.value = e.lnglat.getLng()
    pickerLat.value = e.lnglat.getLat()
    selectedPoiIdx.value = -1
    reverseGeocode(pickerLng.value, pickerLat.value)
  })

  if (fixedForm.value.longitude) {
    pickerLng.value = fixedForm.value.longitude
    pickerLat.value = fixedForm.value.latitude
    pickerAddress.value = pickedAddress.value || ''
  }
}

async function reverseGeocode(lng: number, lat: number) {
  const AMap = (window as any).AMap
  try {
    await new Promise((resolve) => {
      AMap.plugin(['AMap.Geocoder'], () => resolve(true))
    })
    const geocoder = new AMap.Geocoder({ city: '杭州' })
    geocoder.getAddress([lng, lat], (status: string, result: any) => {
      if (status === 'complete' && result.info === 'OK') {
        pickerAddress.value = result.regeocode.formattedAddress || ''
      } else {
        pickerAddress.value = `${lng.toFixed(4)}, ${lat.toFixed(4)}`
      }
    })
  } catch (e) {
    pickerAddress.value = `${lng.toFixed(4)}, ${lat.toFixed(4)}`
  }
}

async function searchPOI() {
  if (!poiSearchText.value) return
  const AMap = (window as any).AMap

  try {
    await new Promise((resolve) => {
      AMap.plugin(['AMap.PlaceSearch'], () => resolve(true))
    })

    const placeSearch = new AMap.PlaceSearch({
      city: '杭州',
      citylimit: true,
      pageSize: 10,
      extensions: 'all'
    })

    placeSearch.search(poiSearchText.value, (status: string, result: any) => {
      if (status === 'complete' && result.poiList) {
        poiResults.value = result.poiList.pois.map((poi: any) => ({
          name: poi.name,
          address: poi.address || poi.name,
          longitude: poi.location.lng,
          latitude: poi.location.lat
        }))
      } else {
        poiResults.value = []
        uni.showToast({ title: '未找到相关地点', icon: 'none' })
      }
    })
  } catch (e) {
    uni.showToast({ title: '搜索失败', icon: 'none' })
  }
}

function selectPOI(poi: any, idx: number) {
  selectedPoiIdx.value = idx
  pickerLng.value = poi.longitude
  pickerLat.value = poi.latitude
  pickerAddress.value = poi.address || poi.name

  if (pickerMapInstance) {
    pickerMapInstance.setCenter([poi.longitude, poi.latitude])
  }

  poiResults.value = []
  poiSearchText.value = ''
}

function clearPOISearch() {
  poiSearchText.value = ''
  poiResults.value = []
  selectedPoiIdx.value = -1
}
// #endif

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

function getCurrentLocation() {
  return new Promise<{ longitude: number; latitude: number }>((resolve, reject) => {
    uni.getLocation({
      type: 'gcj02',
      isHighAccuracy: true,
      success: ({ longitude, latitude }) => resolve({ longitude, latitude }),
      fail: () => reject(new Error('无法获取当前位置，请授权定位后重试'))
    })
  })
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
      ? await getCurrentLocation()
      : { longitude: fixedForm.value.longitude, latitude: fixedForm.value.latitude }
    const [imageUrls, vehicleImageUrls] = deviceType.value === 'fixed'
      ? [await persistImagePaths(fixedForm.value.images), []]
      : await Promise.all([
          persistImagePaths(mobileForm.value.deviceImages),
          persistImagePaths(mobileForm.value.vehicleImages)
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
      title: editingId.value ? '设备修改成功' : '设备录入成功',
      icon: 'success',
      duration: 1200
    })
    setTimeout(() => {
      uni.navigateBack()
    }, 800)
  } catch (error: any) {
    isSubmitting.value = false
    uni.showToast({ title: error?.message || '提交失败，请重试', icon: 'none' })
  }
}

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
  background: #2B6FF0;
  margin-right: 12rpx;
}
.label-text {
  font-size: 28rpx;
  font-weight: 600;
  color: #1D2129;
}
.label-required {
  font-size: 28rpx;
  color: #F53F3F;
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
  border: 2rpx solid #E5E6EB;
  transition: all 0.3s ease;
}
.type-option-active {
  border-color: #2B6FF0;
  background: linear-gradient(135deg, rgba(43, 111, 240, 0.06) 0%, rgba(43, 111, 240, 0.02) 100%);
}
.type-option-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16rpx;
}
.type-icon-fixed {
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
}
.type-icon-mobile {
  background: linear-gradient(135deg, #00B42A 0%, #4DC580 100%);
}
.type-icon-text {
  font-size: 36rpx;
}
.type-option-label {
  font-size: 28rpx;
  font-weight: 600;
  color: #1D2129;
}
.type-option-active .type-option-label {
  color: #2B6FF0;
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
  background: #2B6FF0;
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
  padding: 24rpx 16rpx;
  background: #FFFFFF;
  border-radius: 16rpx;
  border: 2rpx solid #E5E6EB;
  transition: all 0.3s ease;
}
.category-option-active {
  border-color: #2B6FF0;
  background: rgba(43, 111, 240, 0.06);
}
.category-emoji {
  font-size: 32rpx;
  font-weight: 700;
  color: #2B6FF0;
  margin-bottom: 8rpx;
}
.category-option-active .category-emoji {
  color: #2B6FF0;
}
.category-label {
  font-size: 26rpx;
  color: #4E5969;
  font-weight: 500;
}
.category-option-active .category-label {
  color: #2B6FF0;
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
  color: #1D2129;
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
  color: #1D2129;
  font-weight: 500;
}
.location-placeholder {
  font-size: 28rpx;
  color: #C9CDD4;
}
.location-btn {
  padding: 12rpx 28rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
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
  color: #1D2129;
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
  color: #1D2129;
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
  border-color: #2B6FF0;
  background: rgba(43, 111, 240, 0.06);
}
.range-label {
  font-size: 26rpx;
  color: #4E5969;
  font-weight: 500;
}
.range-option-active .range-label {
  color: #2B6FF0;
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
  border-color: #2B6FF0;
  background: rgba(43, 111, 240, 0.06);
}
.time-label {
  font-size: 26rpx;
  color: #4E5969;
  font-weight: 500;
}
.time-option-active .time-label {
  color: #2B6FF0;
  font-weight: 600;
}

.submit-area {
  padding: 48rpx 32rpx;
}
.submit-btn {
  width: 100%;
  height: 96rpx;
  border-radius: 48rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
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
  color: #2B6FF0;
  font-weight: 500;
}

/* #ifdef H5 */
.location-picker-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  background: #F5F7FA;
  display: flex;
  flex-direction: column;
}
.lp-header {
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  padding-bottom: 16rpx;
  z-index: 10;
}
.lp-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 88rpx;
  padding: 0 24rpx;
}
.lp-back {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.lp-back-icon {
  font-size: 40rpx;
  color: #FFFFFF;
  font-weight: 600;
}
.lp-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #FFFFFF;
}
.lp-confirm {
  padding: 12rpx 28rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 28rpx;
}
.lp-confirm-text {
  font-size: 26rpx;
  color: #FFFFFF;
  font-weight: 600;
}
.lp-search-bar {
  display: flex;
  align-items: center;
  margin: 0 24rpx;
  height: 72rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 36rpx;
  padding: 0 24rpx;
}
.lp-search-icon {
  font-size: 28rpx;
  margin-right: 12rpx;
}
.lp-search-input {
  flex: 1;
  height: 72rpx;
  font-size: 28rpx;
  color: #FFFFFF;
}
.lp-search-placeholder {
  color: rgba(255, 255, 255, 0.6);
  font-size: 28rpx;
}
.lp-search-clear {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
}
.lp-clear-icon {
  font-size: 18rpx;
  color: #FFFFFF;
}

.poi-results {
  position: absolute;
  top: 200rpx;
  left: 0;
  right: 0;
  max-height: 400rpx;
  z-index: 20;
  background: #FFFFFF;
  margin: 0 24rpx;
  border-radius: 16rpx;
  box-shadow: 0 4rpx 24rpx rgba(0, 0, 0, 0.12);
  overflow: hidden;
}
.poi-list {
  max-height: 400rpx;
}
.poi-item {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  border-bottom: 1rpx solid #F2F3F5;
}
.poi-item:last-child {
  border-bottom: none;
}
.poi-item-active {
  background: rgba(43, 111, 240, 0.06);
}
.poi-item-left {
  flex: 1;
  min-width: 0;
}
.poi-item-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1D2129;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.poi-item-addr {
  font-size: 22rpx;
  color: #86909C;
  margin-top: 4rpx;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.poi-item-check {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: #2B6FF0;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 16rpx;
}
.poi-check-icon {
  font-size: 22rpx;
  color: #FFFFFF;
  font-weight: 700;
}

.lp-map-container {
  flex: 1;
  width: 100%;
}

.lp-center-marker {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -100%);
  z-index: 15;
  pointer-events: none;
}
.lp-marker-pin {
  width: 32px;
  height: 40px;
  position: relative;
}
.lp-marker-pin::before {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 24px;
  border-radius: 50% 50% 50% 0;
  background: #F53F3F;
  transform: translateX(-50%) rotate(-45deg);
  box-shadow: 0 2px 6px rgba(245, 63, 63, 0.4);
}
.lp-marker-pin::after {
  content: '';
  position: absolute;
  top: 4px;
  left: 50%;
  transform: translateX(-50%);
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #FFFFFF;
  z-index: 1;
}
.lp-marker-shadow {
  width: 16px;
  height: 6px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.2);
  margin: 4px auto 0;
}

.lp-bottom-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 15;
  background: #FFFFFF;
  border-radius: 24rpx 24rpx 0 0;
  padding: 24rpx 32rpx 40rpx;
  box-shadow: 0 -4rpx 24rpx rgba(0, 0, 0, 0.08);
}
.lp-info-content {
  display: flex;
  flex-direction: column;
}
.lp-info-addr {
  font-size: 28rpx;
  font-weight: 600;
  color: #1D2129;
  line-height: 1.5;
}
.lp-info-placeholder {
  color: #C9CDD4;
  font-weight: 400;
}
.lp-info-coord {
  font-size: 22rpx;
  color: #86909C;
  margin-top: 6rpx;
}
/* #endif */
</style>
