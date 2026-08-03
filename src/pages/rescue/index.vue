<template>
  <view class="page">
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-content">
        <view class="nav-back" @tap="goBack"><app-icon name="left" :size="24" color="#FFFFFF" /></view>
        <text class="nav-title">紧急呼救</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <scroll-view scroll-y class="page-scroll" :style="{ paddingTop: (statusBarHeight + 44) + 'px' }">
      <view class="sos-panel">
        <text class="sos-kicker">ONE-TAP EMERGENCY</text>
        <text class="sos-title">一键发出救援请求</text>
        <text class="sos-desc">自动携带位置并匹配预计最快到达的 AED</text>

        <view class="sos-button-wrap" @tap="triggerSOS">
          <view class="sos-pulse"></view>
          <view class="sos-button" :class="{ active: isSubmitting }">
            <text class="sos-word">SOS</text>
            <text class="sos-action-text">{{ sosButtonText }}</text>
          </view>
        </view>

        <view class="location-chip" @tap="refreshLocation">
          <app-icon name="location-filled" :size="16" :color="rescueCoordinates ? '#1D7B58' : '#8A6A28'" />
          <text>{{ locationText }}</text>
          <text class="refresh-text">刷新</text>
        </view>

        <view class="dispatch-flow">
          <view v-for="(step, index) in flowSteps" :key="step.label" class="flow-step">
            <view class="flow-icon"><app-icon :name="step.icon" :size="16" color="#2E6DD1" /></view>
            <text>{{ step.label }}</text>
            <view v-if="index < flowSteps.length - 1" class="flow-line"></view>
          </view>
        </view>
      </view>

      <view class="emergency-actions">
        <view class="call-120" @tap="callEmergency">
          <app-icon name="phone-filled" :size="19" color="#A9212B" />
          <view class="call-copy">
            <text class="call-title">直接拨打 120</text>
            <text class="call-desc">危及生命时优先联系专业急救</text>
          </view>
          <app-icon name="right" :size="16" color="#A9212B" />
        </view>
      </view>

      <view class="details-card">
        <view class="details-head" @tap="detailsExpanded = !detailsExpanded">
          <view>
            <text class="details-title">补充现场信息</text>
            <text class="details-hint">选填，不影响一键呼救</text>
          </view>
          <app-icon :name="detailsExpanded ? 'up' : 'down'" :size="17" color="#708197" />
        </view>

        <view v-if="detailsExpanded" class="details-content">
          <text class="field-label">现场情况</text>
          <view class="symptom-tags">
            <view v-for="tag in symptomTags" :key="tag" class="symptom-tag" :class="{ selected: selectedSymptoms.includes(tag) }" @tap="toggleSymptom(tag)">
              <text>{{ tag }}</text>
            </view>
          </view>

          <text class="field-label field-label-spaced">紧急程度</text>
          <view class="urgency-selector">
            <view v-for="item in urgencyOptions" :key="item.value" class="urgency-option" :class="{ selected: selectedUrgency === item.value }" @tap="selectedUrgency = item.value">
              <view class="urgency-dot" :class="'dot-' + item.value"></view>
              <text>{{ item.label }}</text>
            </view>
          </view>

          <textarea v-model="description" class="desc-input" placeholder="补充伤者状态、现场入口等信息" maxlength="300" />
          <view class="photo-row">
            <image v-for="(img, index) in photoList" :key="img" class="photo" :src="img" mode="aspectFill" @tap="removePhoto(index)" />
            <view v-if="photoList.length < 3" class="photo-add" @tap="addPhoto">
              <app-icon name="camera-filled" :size="22" color="#708197" /><text>现场照片</text>
            </view>
          </view>
        </view>
      </view>

      <view class="safety-note">平台协同不能替代 120，呼救后请保持电话畅通</view>
      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { uploadImage } from '@/api/files'
import { ApiRequestError } from '@/api/http'
import { createRescueCall, type RescueUrgency } from '@/api/rescue'
import { getCurrentGcj02Location } from '@/utils/location'

const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)
const isSubmitting = ref(false)
const isLocating = ref(false)
const detailsExpanded = ref(false)
const selectedUrgency = ref('critical')
const selectedSymptoms = ref<string[]>([])
const description = ref('')
const photoList = ref<string[]>([])
const locationText = ref('正在获取当前位置')
const rescueCoordinates = ref<{ latitude: number; longitude: number } | null>(null)
const flowSteps = [
  { label: '获取定位', icon: 'location-filled' },
  { label: '智能匹配', icon: 'map-filled' },
  { label: '实时驰援', icon: 'navigate-filled' }
]
const urgencyOptions = [
  { value: 'critical', label: '危急' },
  { value: 'high', label: '紧急' },
  { value: 'medium', label: '一般' }
]
const symptomTags = ['意识不清', '呼吸异常', '胸痛心悸', '外伤出血', '跌倒骨折', '其他情况']

const sosButtonText = computed(() => {
  if (isLocating.value) return '正在定位'
  if (isSubmitting.value) return '正在呼救'
  return '立即呼救'
})

async function refreshLocation() {
  if (isLocating.value) return false
  isLocating.value = true
  locationText.value = '正在获取当前位置'
  try {
    const result = await getCurrentGcj02Location()
    rescueCoordinates.value = { latitude: result.latitude, longitude: result.longitude }
    locationText.value = `定位已就绪 · ${result.latitude.toFixed(4)}, ${result.longitude.toFixed(4)}`
    return true
  } catch {
    rescueCoordinates.value = null
    locationText.value = '定位失败，点击重新获取'
    return false
  } finally {
    isLocating.value = false
  }
}

function toggleSymptom(tag: string) {
  const index = selectedSymptoms.value.indexOf(tag)
  if (index >= 0) selectedSymptoms.value.splice(index, 1)
  else selectedSymptoms.value.push(tag)
}

function addPhoto() {
  uni.chooseImage({
    count: 3 - photoList.value.length,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: result => photoList.value.push(...result.tempFilePaths)
  })
}

function removePhoto(index: number) {
  photoList.value.splice(index, 1)
}

async function triggerSOS() {
  if (isSubmitting.value || isLocating.value) return
  if (!rescueCoordinates.value && !(await refreshLocation())) {
    uni.showModal({
      title: '暂时无法获取位置',
      content: '请开启定位权限后重试；情况危急时请直接拨打 120。',
      confirmText: '拨打 120',
      cancelText: '稍后重试',
      success: result => result.confirm && callEmergency()
    })
    return
  }
  await submitRescue()
}

async function submitRescue() {
  const coordinates = rescueCoordinates.value
  if (!coordinates || isSubmitting.value) return
  isSubmitting.value = true
  try {
    const uploadedImages = await Promise.all(photoList.value.map(uploadImage))
    const rescueCall = await createRescueCall({
      urgency: selectedUrgency.value.toUpperCase() as RescueUrgency,
      latitude: coordinates.latitude,
      longitude: coordinates.longitude,
      address: locationText.value,
      description: description.value.trim() || '一键呼救，请尽快联系确认现场情况',
      symptoms: selectedSymptoms.value.length ? selectedSymptoms.value : ['需要紧急救助'],
      imageUrls: uploadedImages.map(image => image.url),
      clientRequestId: `${Date.now()}-${Math.random().toString(36).slice(2, 12)}`
    })
    uni.navigateTo({ url: `/pages/rescue/detail?id=${encodeURIComponent(rescueCall.id)}` })
  } catch (error) {
    const message = error instanceof ApiRequestError ? error.message : '救援请求发送失败，请重试'
    uni.showModal({ title: '发送失败', content: `${message}\n如情况危急，请立即拨打 120。`, showCancel: false })
  } finally {
    isSubmitting.value = false
  }
}

function callEmergency() {
  uni.makePhoneCall({ phoneNumber: '120' })
}

function goBack() {
  uni.navigateBack()
}

onMounted(refreshLocation)
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #F3F7FA; color: #20364D; }
.nav-bar { position: fixed; z-index: 20; top: 0; left: 0; right: 0; background: #A9212B; }
.nav-bar-content { display: flex; align-items: center; justify-content: space-between; height: 88rpx; padding: 0 24rpx; }
.nav-back, .nav-placeholder { width: 60rpx; height: 60rpx; display: flex; align-items: center; justify-content: center; }
.nav-title { color: #FFFFFF; font-size: 33rpx; font-weight: 700; }
.page-scroll { height: 100vh; box-sizing: border-box; }
.sos-panel { margin: 24rpx; padding: 34rpx 28rpx 26rpx; border-radius: 24rpx; background: #FFFFFF; text-align: center; border: 1rpx solid #E0E7EF; }
.sos-kicker { display: block; color: #A9212B; font-size: 19rpx; font-weight: 700; letter-spacing: 3rpx; }
.sos-title { display: block; margin-top: 9rpx; color: #1A3048; font-size: 36rpx; font-weight: 750; }
.sos-desc { display: block; margin-top: 9rpx; color: #708197; font-size: 23rpx; }
.sos-button-wrap { position: relative; display: flex; align-items: center; justify-content: center; width: 250rpx; height: 250rpx; margin: 24rpx auto 18rpx; }
.sos-pulse { position: absolute; width: 224rpx; height: 224rpx; border: 2rpx solid rgba(169, 33, 43, .25); border-radius: 50%; animation: pulse 2s ease-out infinite; }
.sos-button { z-index: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; width: 190rpx; height: 190rpx; border-radius: 50%; background: #A9212B; box-shadow: 0 12rpx 26rpx rgba(132, 25, 34, .2); }
.sos-button.active { opacity: .78; transform: scale(.96); }
.sos-word { color: #FFFFFF; font-size: 58rpx; font-weight: 800; letter-spacing: 5rpx; line-height: 1; }
.sos-action-text { margin-top: 10rpx; color: #FFFFFF; font-size: 23rpx; font-weight: 650; }
@keyframes pulse { from { transform: scale(.85); opacity: 1; } to { transform: scale(1.18); opacity: 0; } }
.location-chip { display: inline-flex; align-items: center; gap: 8rpx; max-width: 100%; padding: 11rpx 16rpx; border-radius: 10rpx; background: #F3F6F9; color: #546579; font-size: 21rpx; }
.refresh-text { margin-left: 7rpx; color: #2E6DD1; font-weight: 650; }
.dispatch-flow { display: flex; justify-content: center; margin-top: 25rpx; padding-top: 22rpx; border-top: 1rpx solid #EEF2F6; }
.flow-step { position: relative; display: flex; flex: 1; flex-direction: column; align-items: center; gap: 8rpx; color: #627387; font-size: 20rpx; }
.flow-icon { z-index: 1; display: flex; align-items: center; justify-content: center; width: 48rpx; height: 48rpx; border-radius: 50%; background: #EAF1FD; }
.flow-line { position: absolute; top: 23rpx; left: calc(50% + 24rpx); width: calc(100% - 48rpx); height: 2rpx; background: #D8E3F1; }
.emergency-actions { margin: 0 24rpx 18rpx; }
.call-120 { display: flex; align-items: center; gap: 16rpx; padding: 20rpx 22rpx; border: 1rpx solid #ECD7D9; border-radius: 18rpx; background: #FFF7F7; }
.call-copy { display: flex; flex: 1; flex-direction: column; gap: 3rpx; text-align: left; }
.call-title { color: #8E2029; font-size: 25rpx; font-weight: 700; }
.call-desc { color: #8C7478; font-size: 20rpx; }
.details-card { margin: 0 24rpx; border: 1rpx solid #E0E7EF; border-radius: 20rpx; background: #FFFFFF; }
.details-head { display: flex; align-items: center; justify-content: space-between; padding: 22rpx 24rpx; }
.details-head > view { display: flex; flex-direction: column; gap: 4rpx; }
.details-title { font-size: 27rpx; font-weight: 700; }
.details-hint { color: #8694A6; font-size: 20rpx; }
.details-content { padding: 2rpx 24rpx 24rpx; border-top: 1rpx solid #EEF2F6; }
.field-label { display: block; margin: 22rpx 0 14rpx; font-size: 23rpx; font-weight: 650; }
.field-label-spaced { margin-top: 26rpx; }
.symptom-tags { display: flex; flex-wrap: wrap; gap: 10rpx; }
.symptom-tag { padding: 11rpx 18rpx; border: 1rpx solid #DCE3EB; border-radius: 9rpx; background: #F7F9FB; color: #56677B; font-size: 22rpx; }
.symptom-tag.selected { border-color: #AFC8EC; background: #EAF1FD; color: #245FAF; font-weight: 650; }
.urgency-selector { display: flex; gap: 10rpx; }
.urgency-option { display: flex; flex: 1; align-items: center; justify-content: center; gap: 8rpx; padding: 14rpx 0; border: 1rpx solid #DCE3EB; border-radius: 9rpx; color: #617185; font-size: 22rpx; }
.urgency-option.selected { border-color: #AFC8EC; background: #F0F5FC; color: #244F88; font-weight: 650; }
.urgency-dot { width: 10rpx; height: 10rpx; border-radius: 50%; }.dot-critical { background: #A9212B; }.dot-high { background: #CF8525; }.dot-medium { background: #2E6DD1; }
.desc-input { box-sizing: border-box; width: 100%; height: 130rpx; margin-top: 22rpx; padding: 17rpx; border: 1rpx solid #DCE3EB; border-radius: 10rpx; background: #F8FAFC; color: #30455C; font-size: 23rpx; }
.photo-row { display: flex; gap: 12rpx; margin-top: 14rpx; }.photo, .photo-add { width: 122rpx; height: 94rpx; border-radius: 9rpx; }.photo-add { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6rpx; border: 1rpx dashed #BCC8D5; color: #708197; font-size: 18rpx; }
.safety-note { margin: 22rpx 24rpx 0; color: #8390A1; text-align: center; font-size: 20rpx; }.bottom-safe { height: 70rpx; }
</style>
