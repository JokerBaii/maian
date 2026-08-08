<template>
  <view class="page apple-page emergency-page motion-page-sheet">
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-content">
        <view class="nav-back" @tap="goBack"><app-icon name="left" :size="24" color="#FFFFFF" /></view>
        <text class="nav-title">紧急呼救</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <view v-if="isSubmitting" class="matching-layer" :style="{ paddingTop: (statusBarHeight + 44) + 'px' }">
      <text class="matching-sos">SOS</text>
      <view class="matching-loader"><view></view><view></view><view></view></view>
      <text class="matching-title">正在联系附近救援人员</text>
      <text class="matching-desc">你的呼救和位置已送达</text>
      <view class="matching-steps">
        <view class="matching-step done"><view></view><text>呼救送达</text></view>
        <view class="matching-step active"><view></view><text>附近响应</text></view>
        <view class="matching-step"><view></view><text>AED 协同</text></view>
      </view>
    </view>

    <scroll-view v-else scroll-y class="page-scroll" :style="{ paddingTop: (statusBarHeight + 44) + 'px' }">
      <view class="rescue-stage motion-enter">
        <view class="stage-status">
          <view class="readiness-chip" :class="{ locating: isLocating }">
            <view class="readiness-dot"></view>
            <text>{{ isLocating ? '正在确认位置' : '位置已确认' }}</text>
          </view>
        </view>

        <text class="sos-word">SOS</text>
        <text class="stage-title">需要紧急帮助</text>
        <text class="stage-desc">向附近救援人员发送你的位置</text>

        <view class="location-chip" @tap="refreshLocation">
          <view class="location-main">
            <app-icon name="location-filled" :size="17" color="#FFFFFF" />
            <view class="location-copy">
              <text class="location-label">呼救位置</text>
              <text class="location-value">{{ locationText }}</text>
            </view>
          </view>
          <text class="refresh-text">更新</text>
        </view>

        <view class="sos-primary" @tap="triggerSOS">
          <text>{{ sosButtonText }}</text>
        </view>
        <view class="call-120" @tap="callEmergency">
          <app-icon name="phone-filled" :size="18" color="#FFFFFF" />
          <text>拨打 120</text>
        </view>
      </view>

      <view class="details-card motion-enter motion-enter-delay-2">
        <view class="details-head" @tap="detailsExpanded = !detailsExpanded">
          <view>
            <text class="details-title">补充现场信息</text>
            <text class="details-hint">选填</text>
          </view>
          <app-icon :name="detailsExpanded ? 'up' : 'down'" :size="17" color="#FFFFFF" />
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
              <app-icon name="camera-filled" :size="22" color="#FFFFFF" /><text>现场照片</text>
            </view>
          </view>
        </view>
      </view>

      <view class="safety-note">呼救后请保持电话畅通</view>
      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { uploadImage } from '@/api/files'
import { attachRescueMedia, createRescueCall, type RescueUrgency } from '@/api/rescue'
import { chooseGcj02Location, getCurrentGcj02Location, FIXED_LOCATION, FIXED_LOCATION_ADDRESS, isDemoMode } from '@/utils/location'
import { createClientRequestId } from '@/utils/requestId'
import { userFacingError } from '@/utils/presentation'

const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)
const isSubmitting = ref(false)
const isLocating = ref(false)
const detailsExpanded = ref(false)
const selectedUrgency = ref('critical')
const selectedSymptoms = ref<string[]>([])
const description = ref('')
const photoList = ref<string[]>([])
const locationText = ref(isDemoMode ? FIXED_LOCATION_ADDRESS : '正在获取真实位置')
const rescueCoordinates = ref<{ latitude: number; longitude: number } | null>(isDemoMode ? {
  latitude: FIXED_LOCATION.latitude,
  longitude: FIXED_LOCATION.longitude
} : null)
const sosRequestId = ref(createClientRequestId())
const urgencyOptions = [
  { value: 'critical', label: '危急' },
  { value: 'high', label: '紧急' },
  { value: 'medium', label: '一般' }
]
const symptomTags = ['意识不清', '呼吸异常', '胸痛心悸', '外伤出血', '跌倒骨折', '其他情况']

const sosButtonText = computed(() => {
  if (isLocating.value) return '正在定位'
  if (isSubmitting.value) return '正在呼救'
  return '发出紧急呼救'
})

async function refreshLocation(allowManualFallback = true) {
  if (isLocating.value) return false
  isLocating.value = true
  try {
    let result
    try {
      result = await getCurrentGcj02Location()
    } catch (error) {
      if (!allowManualFallback) throw error
      result = await chooseGcj02Location()
    }
    rescueCoordinates.value = { latitude: result.latitude, longitude: result.longitude }
    locationText.value = result.address
      || `${result.latitude.toFixed(5)}, ${result.longitude.toFixed(5)}`
    return true
  } catch {
    if (allowManualFallback) uni.showToast({ title: '未能获取位置，请检查定位权限', icon: 'none' })
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
  await submitRescue()
}

async function submitRescue() {
  const coordinates = rescueCoordinates.value
  if (!coordinates || isSubmitting.value) return
  const matchingStartedAt = Date.now()
  isSubmitting.value = true
  try {
    const rescueCall = await createRescueCall({
      urgency: selectedUrgency.value.toUpperCase() as RescueUrgency,
      latitude: coordinates.latitude,
      longitude: coordinates.longitude,
      address: locationText.value,
      description: description.value.trim() || '一键呼救，请尽快联系确认现场情况',
      symptoms: selectedSymptoms.value.length ? selectedSymptoms.value : ['需要紧急救助'],
      clientRequestId: sosRequestId.value
    })
    await new Promise(resolve => setTimeout(resolve, Math.max(0, 1600 - (Date.now() - matchingStartedAt))))
    uni.navigateTo({ url: `/pages/rescue/detail?id=${encodeURIComponent(rescueCall.id)}` })
    void uploadRescueAttachments(rescueCall.id, [...photoList.value])
  } catch (error) {
    const message = userFacingError(error, '救援请求发送失败，请重试')
    uni.showModal({ title: '发送失败', content: `${message}\n如情况危急，请立即拨打 120。`, showCancel: false })
  } finally {
    isSubmitting.value = false
  }
}

async function uploadRescueAttachments(rescueCallId: string, paths: string[]) {
  let failedCount = 0
  for (const path of paths) {
    try {
      const media = await uploadImage(path, 'RESCUE_ATTACHMENT')
      await attachRescueMedia(rescueCallId, media.mediaId)
    } catch {
      failedCount += 1
    }
  }
  if (failedCount) {
    uni.showToast({ title: '呼救已发出，部分现场照片未能上传', icon: 'none' })
  }
}

function callEmergency() {
  uni.makePhoneCall({ phoneNumber: '120' })
}

function goBack() {
  uni.navigateBack()
}

onMounted(() => refreshLocation(false))
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; background: #B3212D; color: #FFFFFF; }
.nav-bar { position: fixed; z-index: 20; top: 0; left: 0; right: 0; background: #A9212B; }
.nav-bar-content { display: flex; align-items: center; justify-content: space-between; height: 88rpx; padding: 0 24rpx; }
.nav-back, .nav-placeholder { width: 60rpx; height: 60rpx; display: flex; align-items: center; justify-content: center; }
.nav-title { color: #FFFFFF; font-size: 33rpx; font-weight: 700; }
.page-scroll { height: 100vh; box-sizing: border-box; background: #B3212D; }
.rescue-stage { min-height: 710rpx; padding: 36rpx 38rpx 28rpx; color: #FFFFFF; text-align: center; }
.stage-status { position: relative; z-index: 2; display: flex; justify-content: flex-end; }
.stage-title { display: block; margin-top: 12rpx; color: #FFFFFF; font-size: 38rpx; font-weight: 760; letter-spacing: -.5rpx; }
.stage-desc { display: block; margin-top: 7rpx; color: rgba(255,255,255,.72); font-size: 21rpx; }
.readiness-chip { display: flex; align-items: center; gap: 7rpx; padding: 6rpx 0; color: rgba(255,255,255,.82); font-size: 18rpx; font-weight: 650; }
.readiness-chip.locating { color: rgba(255,255,255,.68); }
.readiness-dot { width: 9rpx; height: 9rpx; border-radius: 50%; background: currentColor; box-shadow: 0 0 0 0 rgba(35,149,106,.24); animation: readinessPulse 2s ease-out infinite; }
.sos-word { display: block; margin-top: 66rpx; color: #FFFFFF; font-size: 118rpx; font-weight: 820; letter-spacing: 9rpx; line-height: .9; }
@keyframes readinessPulse { 0% { box-shadow: 0 0 0 0 rgba(35,149,106,.25); } 70%,100% { box-shadow: 0 0 0 8rpx rgba(35,149,106,0); } }
.location-chip { display: flex; align-items: center; justify-content: space-between; gap: 14rpx; margin-top: 60rpx; padding: 17rpx 0; border-top: 1rpx solid rgba(255,255,255,.20); border-bottom: 1rpx solid rgba(255,255,255,.20); text-align: left; }
.location-main { display: flex; min-width: 0; flex: 1; align-items: center; gap: 11rpx; }
.location-copy { min-width: 0; flex: 1; }
.location-label, .location-value { display: block; }
.location-label { color: rgba(255,255,255,.58); font-size: 17rpx; }
.location-value { margin-top: 2rpx; overflow: hidden; color: #FFFFFF; font-size: 21rpx; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.refresh-text { flex: none; color: #FFFFFF; font-size: 19rpx; font-weight: 700; }
.sos-primary { display: flex; height: 88rpx; align-items: center; justify-content: center; margin-top: 30rpx; border-radius: 20rpx; background: #FFFFFF; color: #A91F2A; font-size: 27rpx; font-weight: 760; box-shadow: 0 12rpx 30rpx rgba(92,10,18,.20); }
.call-120 { display: flex; height: 72rpx; align-items: center; justify-content: center; gap: 10rpx; margin-top: 12rpx; border: 1rpx solid rgba(255,255,255,.45); border-radius: 18rpx; color: #FFFFFF; font-size: 23rpx; font-weight: 680; }
.matching-layer { position: fixed; z-index: 18; inset: 0; box-sizing: border-box; display: flex; flex-direction: column; align-items: center; background: #B3212D; color: #FFFFFF; }
.matching-sos { margin-top: 170rpx; font-size: 112rpx; font-weight: 820; letter-spacing: 9rpx; line-height: 1; }
.matching-loader { display: flex; gap: 12rpx; margin-top: 40rpx; }.matching-loader view { width: 12rpx; height: 12rpx; border-radius: 50%; background: #FFFFFF; animation: matchingDot 1.2s ease-in-out infinite; }.matching-loader view:nth-child(2) { animation-delay: .16s; }.matching-loader view:nth-child(3) { animation-delay: .32s; }
.matching-title { margin-top: 38rpx; font-size: 31rpx; font-weight: 740; }.matching-desc { margin-top: 10rpx; color: rgba(255,255,255,.70); font-size: 21rpx; }
.matching-steps { display: flex; width: 560rpx; max-width: calc(100vw - 80rpx); justify-content: space-between; margin-top: 54rpx; }
.matching-step { position: relative; display: flex; flex: 1; flex-direction: column; align-items: center; gap: 10rpx; color: rgba(255,255,255,.42); font-size: 18rpx; }
.matching-step:not(:last-child)::after { content: ''; position: absolute; top: 7rpx; left: calc(50% + 14rpx); width: calc(100% - 28rpx); height: 2rpx; background: rgba(255,255,255,.20); }
.matching-step view { z-index: 1; width: 15rpx; height: 15rpx; border-radius: 50%; background: rgba(255,255,255,.28); }
.matching-step.done,.matching-step.active { color: #FFFFFF; }.matching-step.done view { background: #FFFFFF; }.matching-step.active view { background: #FFFFFF; box-shadow: 0 0 0 8rpx rgba(255,255,255,.12); }
@keyframes matchingDot { 0%,65%,100% { opacity: .32; transform: translateY(0); } 32% { opacity: 1; transform: translateY(-8rpx); } }
.details-card { margin: 0 38rpx; border-top: 1rpx solid rgba(255,255,255,.22); color: #FFFFFF; }
.details-head { display: flex; align-items: center; justify-content: space-between; padding: 22rpx 24rpx; }
.details-head > view { display: flex; flex-direction: column; gap: 4rpx; }
.details-title { font-size: 27rpx; font-weight: 700; }
.details-hint { color: rgba(255,255,255,.56); font-size: 20rpx; }
.details-content { margin: 0 0 24rpx; padding: 2rpx 20rpx 22rpx; border-radius: 18rpx; background: rgba(104,8,16,.18); animation: detailsReveal 220ms cubic-bezier(.2,.72,.2,1) both; }
.field-label { display: block; margin: 22rpx 0 14rpx; font-size: 23rpx; font-weight: 650; }
.field-label-spaced { margin-top: 26rpx; }
.symptom-tags { display: flex; flex-wrap: wrap; gap: 10rpx; }
.symptom-tag { padding: 11rpx 18rpx; border: 1rpx solid rgba(255,255,255,.34); border-radius: 9rpx; color: rgba(255,255,255,.82); font-size: 22rpx; }
.symptom-tag.selected { border-color: #FFFFFF; background: #FFFFFF; color: #A91F2A; font-weight: 650; }
.urgency-selector { display: flex; gap: 10rpx; }
.urgency-option { display: flex; flex: 1; align-items: center; justify-content: center; gap: 8rpx; padding: 14rpx 0; border: 1rpx solid rgba(255,255,255,.34); border-radius: 9rpx; color: rgba(255,255,255,.82); font-size: 22rpx; }
.urgency-option.selected { border-color: #FFFFFF; background: #FFFFFF; color: #A91F2A; font-weight: 650; }
.urgency-dot { width: 10rpx; height: 10rpx; border-radius: 50%; }.dot-critical { background: #A9212B; }.dot-high { background: #CF8525; }.dot-medium { background: #2E6DD1; }
.desc-input { box-sizing: border-box; width: 100%; height: 130rpx; margin-top: 22rpx; padding: 17rpx; border: 1rpx solid rgba(255,255,255,.30); border-radius: 10rpx; background: rgba(255,255,255,.10); color: #FFFFFF; font-size: 23rpx; }
.photo-row { display: flex; gap: 12rpx; margin-top: 14rpx; }.photo, .photo-add { width: 122rpx; height: 94rpx; border-radius: 9rpx; }.photo-add { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6rpx; border: 1rpx dashed rgba(255,255,255,.40); color: rgba(255,255,255,.76); font-size: 18rpx; }
.safety-note { margin: 20rpx 24rpx 0; color: rgba(255,255,255,.54); text-align: center; font-size: 19rpx; }.bottom-safe { height: 70rpx; }
@keyframes detailsReveal { from { opacity: 0; transform: translateY(-8rpx); } to { opacity: 1; transform: translateY(0); } }

@media (max-width: 360px) {
  .rescue-stage { padding-right: 24rpx; padding-left: 24rpx; }
  .sos-word { font-size: 104rpx; }
}
</style>
