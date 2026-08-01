<template>
  <view class="page">
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
        <view class="nav-bar-content">
          <view class="nav-back" @tap="goBack">
          <app-icon name="left" :size="24" color="#FFFFFF" />
        </view>
        <text class="nav-title">紧急呼救</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <view
      class="scroll-content"
      :style="{ paddingTop: (statusBarHeight + 44) + 'px' }"
    >
      <view class="sos-hero">
        <view class="sos-ring-container" @tap="triggerSOS">
          <view class="sos-pulse sos-pulse-1"></view>
          <view class="sos-pulse sos-pulse-2"></view>
          <view class="sos-pulse sos-pulse-3"></view>
          <view class="sos-main-btn" :class="{ 'sos-activated': isActivated }">
            <text class="sos-main-text">SOS</text>
            <text class="sos-main-label">{{ isActivated ? '已选择危急' : '危急模式' }}</text>
          </view>
        </view>
        <text class="sos-location" @tap="refreshLocation">
          <app-icon class="location-icon" name="location-filled" :size="18" color="#1F63D5" />
          {{ locationText }}
        </text>
        <text class="sos-guidance">确认症状和位置后再发送，减少误触</text>
      </view>

      <view class="form-section">
        <view class="form-label">
          <view class="label-bar"></view>
          <text class="label-text">紧急等级</text>
        </view>
        <view class="urgency-selector">
          <view
            v-for="item in urgencyOptions"
            :key="item.value"
            class="urgency-option"
            :class="[
              'urgency-' + item.value,
              { 'urgency-active': selectedUrgency === item.value }
            ]"
            @tap="selectedUrgency = item.value"
          >
            <view class="urgency-dot"></view>
            <text class="urgency-label">{{ item.label }}</text>
          </view>
        </view>
      </view>

      <view class="form-section">
        <view class="form-label">
          <view class="label-bar"></view>
          <text class="label-text">症状描述</text>
        </view>
        <view class="symptom-tags">
          <view
            v-for="tag in symptomTags"
            :key="tag"
            class="symptom-tag"
            :class="{ 'symptom-tag-active': selectedSymptoms.includes(tag) }"
            @tap="toggleSymptom(tag)"
          >
            <text class="symptom-tag-text">{{ tag }}</text>
          </view>
        </view>
      </view>

      <view class="form-section">
        <view class="form-label">
          <view class="label-bar"></view>
          <text class="label-text">详细说明</text>
        </view>
        <view class="desc-input-wrap">
          <textarea
            v-model="description"
            class="desc-input"
            placeholder="请描述当前紧急情况，包括伤者状态、具体需求等..."
            placeholder-class="desc-placeholder"
            maxlength="500"
            :auto-height="false"
          />
          <text class="desc-count">{{ description.length }}/500</text>
        </view>
      </view>

      <view class="form-section">
        <view class="form-label">
          <view class="label-bar"></view>
          <text class="label-text">现场照片</text>
          <text class="label-hint">（可选）</text>
        </view>
        <view class="photo-upload">
          <view
            v-for="(img, idx) in photoList"
            :key="idx"
            class="photo-item"
          >
            <image class="photo-img" :src="img" mode="aspectFill" />
            <view class="photo-delete" @tap="removePhoto(idx)">
              <app-icon name="closeempty" :size="18" color="#FFFFFF" />
            </view>
          </view>
          <view v-if="photoList.length < 9" class="photo-add" @tap="addPhoto">
            <app-icon name="camera-filled" :size="28" color="#77849A" />
            <text class="add-label">上传照片</text>
          </view>
        </view>
      </view>

      <view class="submit-area">
        <view class="submit-btn" :class="{ 'submit-disabled': submitDisabled }" @tap="submitRescue">
          <text class="submit-text">{{ submitBtnText }}</text>
        </view>
      </view>

      <view class="bottom-safe"></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { uploadImage } from '@/api/files'
import { ApiRequestError } from '@/api/http'
import { createRescueCall, type RescueUrgency } from '@/api/rescue'
import { getCurrentGcj02Location } from '@/utils/location'

const statusBarHeight = ref(0)
const systemInfo = uni.getSystemInfoSync()
statusBarHeight.value = systemInfo.statusBarHeight || 20

const isActivated = ref(false)
const isSubmitting = ref(false)

const selectedUrgency = ref('critical')
const selectedSymptoms = ref<string[]>([])
const description = ref('')
const photoList = ref<string[]>([])
const clientRequestId = `${Date.now()}-${Math.random().toString(36).slice(2, 12)}`

const locationText = ref('正在获取当前位置')
const rescueCoordinates = ref<{ latitude: number; longitude: number } | null>(null)

function refreshLocation() {
  getCurrentGcj02Location()
    .then((result) => {
      rescueCoordinates.value = {
        latitude: result.latitude,
        longitude: result.longitude
      }
      locationText.value = `当前位置 · ${result.latitude.toFixed(4)}, ${result.longitude.toFixed(4)}`
    })
    .catch(() => {
      rescueCoordinates.value = null
      locationText.value = '定位失败，点击重新获取'
    })
}

onMounted(refreshLocation)

const urgencyOptions = [
  { value: 'critical', label: '危急' },
  { value: 'high', label: '紧急' },
  { value: 'medium', label: '一般' }
]

const symptomTags = ['心脏骤停', '呼吸困难', '外伤出血', '晕厥', '骨折', '其他']

const submitBtnText = computed(() => {
  if (isSubmitting.value) return '正在发送...'
  return '发送救援请求'
})
const submitDisabled = computed(() => (
  isSubmitting.value || selectedSymptoms.value.length === 0 || !rescueCoordinates.value
))

function toggleSymptom(tag: string) {
  const idx = selectedSymptoms.value.indexOf(tag)
  if (idx > -1) {
    selectedSymptoms.value.splice(idx, 1)
  } else {
    selectedSymptoms.value.push(tag)
  }
}

function addPhoto() {
  uni.chooseImage({
    count: 9 - photoList.value.length,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      photoList.value.push(...res.tempFilePaths)
    }
  })
}

function removePhoto(idx: number) {
  photoList.value.splice(idx, 1)
}

function triggerSOS() {
  if (isActivated.value) return
  isActivated.value = true
  selectedUrgency.value = 'critical'
  setTimeout(() => {
    isActivated.value = false
  }, 600)
}

async function submitRescue() {
  if (isSubmitting.value) return
  if (!selectedSymptoms.value.length) {
    uni.showToast({ title: '请至少选择一项症状', icon: 'none' })
    return
  }
  const coordinates = rescueCoordinates.value
  if (!coordinates) {
    uni.showModal({
      title: '无法发送位置',
      content: '请先授权定位；如情况危急，请立即拨打 120。',
      showCancel: false,
      confirmText: '知道了'
    })
    return
  }

  isSubmitting.value = true

  try {
    const uploadedImages = await Promise.all(photoList.value.map(uploadImage))
    const rescueCall = await createRescueCall({
      urgency: selectedUrgency.value.toUpperCase() as RescueUrgency,
      latitude: coordinates.latitude,
      longitude: coordinates.longitude,
      address: locationText.value,
      description: description.value.trim() || undefined,
      symptoms: selectedSymptoms.value,
      imageUrls: uploadedImages.map((image) => image.url),
      clientRequestId
    })
    uni.navigateTo({
      url: `/pages/rescue/detail?id=${encodeURIComponent(rescueCall.id)}`
    })
  } catch (error) {
    const message = error instanceof ApiRequestError
      ? error.message
      : '救援请求发送失败，请重试'
    uni.showModal({
      title: '发送失败',
      content: `${message}\n如情况危急，请立即拨打 120。`,
      showCancel: false,
      confirmText: '知道了'
    })
  } finally {
    isSubmitting.value = false
  }
}

function goBack() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F4F7FB;
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background: #A9212B;
  border-bottom: 1rpx solid rgba(255, 255, 255, 0.16);
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

.scroll-content {
  min-height: 100vh;
  box-sizing: border-box;
}

.sos-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 0 32rpx;
  background: #F4F7FB;
  border-bottom: 1rpx solid #E1E7F0;
}
.sos-ring-container {
  position: relative;
  width: 280rpx;
  height: 280rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.sos-pulse {
  position: absolute;
  border-radius: 50%;
  border: 2rpx solid rgba(169, 33, 43, 0.22);
  animation: rescuePulse 2.4s ease-out infinite;
}
.sos-pulse-1 {
  width: 280rpx;
  height: 280rpx;
  animation-delay: 0s;
}
.sos-pulse-2 {
  display: none;
}
.sos-pulse-3 {
  display: none;
}
@keyframes rescuePulse {
  0% {
    transform: scale(0.85);
    opacity: 1;
  }
  100% {
    transform: scale(1.3);
    opacity: 0;
  }
}
.sos-main-btn {
  width: 200rpx;
  height: 200rpx;
  border-radius: 50%;
  background: #A9212B;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(96, 20, 27, 0.18);
  z-index: 3;
  transition: all 0.3s ease;
}
.sos-activated {
  transform: scale(0.9);
  box-shadow: 0 4rpx 24rpx rgba(245, 63, 63, 0.8);
}
.sos-main-text {
  font-size: 64rpx;
  font-weight: 800;
  color: #FFFFFF;
  letter-spacing: 6rpx;
  line-height: 1;
}
.sos-main-label {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.9);
  margin-top: 8rpx;
}
.sos-location {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 24rpx;
  font-size: 24rpx;
  color: #33415C;
  background: #E9EEF6;
  padding: 8rpx 24rpx;
  border-radius: 24rpx;
}
.sos-guidance {
  margin-top: 12rpx;
  color: #77849A;
  font-size: 22rpx;
}
.location-icon {
  font-size: 24rpx;
}

.form-section {
  padding: 32rpx 32rpx 0;
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
  background: #1F63D5;
  margin-right: 12rpx;
}
.label-text {
  font-size: 28rpx;
  font-weight: 600;
  color: #20364D;
}
.label-hint {
  font-size: 22rpx;
  color: #C9CDD4;
  margin-left: 8rpx;
}

.urgency-selector {
  display: flex;
  gap: 20rpx;
}
.urgency-option {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  padding: 20rpx 0;
  border-radius: 12rpx;
  background: #FFFFFF;
  border: 2rpx solid #E5E6EB;
  transition: border-color 150ms ease, background 150ms ease;
}
.urgency-option.urgency-active {
  border-color: transparent;
}
.urgency-critical.urgency-active {
  background: #FBEAEC;
  border-color: #A9212B;
}
.urgency-high.urgency-active {
  background: #FFF4DE;
  border-color: #A86708;
}
.urgency-medium.urgency-active {
  background: #EAF1FD;
  border-color: #1F63D5;
}
.urgency-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
}
.urgency-critical .urgency-dot {
  background: #C93D46;
}
.urgency-high .urgency-dot {
  background: #FF9A2E;
}
.urgency-medium .urgency-dot {
  background: #2E6DD1;
}
.urgency-label {
  font-size: 28rpx;
  color: #4E5969;
  font-weight: 500;
}
.urgency-active .urgency-label {
  font-weight: 700;
}
.urgency-critical.urgency-active .urgency-label {
  color: #C93D46;
}
.urgency-high.urgency-active .urgency-label {
  color: #FF9A2E;
}
.urgency-medium.urgency-active .urgency-label {
  color: #2E6DD1;
}

.symptom-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.symptom-tag {
  padding: 14rpx 28rpx;
  border-radius: 32rpx;
  background: #FFFFFF;
  border: 2rpx solid #E5E6EB;
  transition: border-color 150ms ease, background 150ms ease;
}
.symptom-tag-active {
  background: #EAF1FD;
  border-color: #1F63D5;
}
.symptom-tag-text {
  font-size: 26rpx;
  color: #4E5969;
  font-weight: 500;
}
.symptom-tag-active .symptom-tag-text {
  color: #174D9F;
  font-weight: 600;
}

.desc-input-wrap {
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 24rpx;
  position: relative;
  border: 1rpx solid #DCE3ED;
}
.desc-input {
  width: 100%;
  height: 200rpx;
  font-size: 28rpx;
  color: #20364D;
  line-height: 1.6;
}
.desc-placeholder {
  color: #C9CDD4;
  font-size: 28rpx;
}
.desc-count {
  position: absolute;
  bottom: 16rpx;
  right: 24rpx;
  font-size: 22rpx;
  color: #C9CDD4;
}

.photo-upload {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
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

.submit-area {
  padding: 48rpx 32rpx;
}
.submit-btn {
  width: 100%;
  height: 96rpx;
  border-radius: 48rpx;
  background: #A9212B;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 3rpx 10rpx rgba(96, 20, 27, 0.16);
  transition: transform 150ms ease, opacity 150ms ease;
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
</style>
