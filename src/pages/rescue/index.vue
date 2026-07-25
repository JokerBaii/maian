<template>
  <view class="page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-content">
        <view class="nav-back" @tap="goBack">
          <text class="back-arrow">&#x2190;</text>
        </view>
        <text class="nav-title">紧急呼救</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <scroll-view
      class="scroll-content"
      scroll-y
      :style="{ paddingTop: (statusBarHeight + 44) + 'px' }"
    >
      <!-- SOS 主按钮区域 -->
      <view class="sos-hero">
        <view class="sos-ring-container" @tap="triggerSOS">
          <view class="sos-pulse sos-pulse-1"></view>
          <view class="sos-pulse sos-pulse-2"></view>
          <view class="sos-pulse sos-pulse-3"></view>
          <view class="sos-main-btn" :class="{ 'sos-activated': isActivated }">
            <text class="sos-main-text">SOS</text>
            <text class="sos-main-label">{{ isActivated ? '已发送' : '一键呼救' }}</text>
          </view>
        </view>
        <text class="sos-location">
          <text class="location-icon">📍</text>
          {{ locationText }}
        </text>
      </view>

      <!-- 紧急等级选择 -->
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

      <!-- 症状标签 -->
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

      <!-- 详细描述 -->
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

      <!-- 照片上传 -->
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
              <text class="delete-icon">&#x2715;</text>
            </view>
          </view>
          <view class="photo-add" @tap="addPhoto">
            <text class="add-icon">+</text>
            <text class="add-label">上传照片</text>
          </view>
        </view>
      </view>

      <!-- 提交按钮 -->
      <view class="submit-area">
        <view class="submit-btn" :class="{ 'submit-disabled': isSubmitting }" @tap="submitRescue">
          <text class="submit-text">{{ submitBtnText }}</text>
        </view>
      </view>

      <!-- 匹配动画遮罩 -->
      <view v-if="isMatching" class="matching-overlay">
        <view class="matching-card">
          <view class="matching-animation">
            <view class="matching-ring matching-ring-1"></view>
            <view class="matching-ring matching-ring-2"></view>
            <view class="matching-ring matching-ring-3"></view>
            <view class="matching-center">
              <text class="matching-icon">🔍</text>
            </view>
          </view>
          <text class="matching-title">正在匹配附近救援资源</text>
          <text class="matching-desc">已通知周边志愿者和设备持有者</text>
          <view class="matching-progress">
            <view class="progress-bar">
              <view class="progress-fill" :style="{ width: matchProgress + '%' }"></view>
            </view>
            <text class="progress-text">{{ matchProgress }}%</text>
          </view>
        </view>
      </view>

      <!-- 底部安全区 -->
      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

// 系统信息
const statusBarHeight = ref(0)
const systemInfo = uni.getSystemInfoSync()
statusBarHeight.value = systemInfo.statusBarHeight || 20

// 状态
const isActivated = ref(false)
const isSubmitting = ref(false)
const isMatching = ref(false)
const matchProgress = ref(0)

// 表单数据
const selectedUrgency = ref('critical')
const selectedSymptoms = ref<string[]>([])
const description = ref('')
const photoList = ref<string[]>([])

// 位置
const locationText = ref('杭州市西湖区文三路')

// 紧急等级选项
const urgencyOptions = [
  { value: 'critical', label: '危急' },
  { value: 'high', label: '紧急' },
  { value: 'medium', label: '一般' }
]

// 症状标签
const symptomTags = ['心脏骤停', '呼吸困难', '外伤出血', '晕厥', '骨折', '其他']

// 提交按钮文字
const submitBtnText = computed(() => {
  if (isSubmitting.value) return '正在发送...'
  if (isMatching.value) return '匹配中...'
  return '发送救援请求'
})

// 切换症状标签
function toggleSymptom(tag: string) {
  const idx = selectedSymptoms.value.indexOf(tag)
  if (idx > -1) {
    selectedSymptoms.value.splice(idx, 1)
  } else {
    selectedSymptoms.value.push(tag)
  }
}

// 添加照片 - H5环境下使用复用的file input
let h5FileInput: HTMLInputElement | null = null

onMounted(() => {
  const isH5 = typeof window !== 'undefined' && typeof document !== 'undefined'
  if (isH5) {
    h5FileInput = document.createElement('input')
    h5FileInput.type = 'file'
    h5FileInput.accept = 'image/*'
    h5FileInput.multiple = true
    h5FileInput.style.display = 'none'
    h5FileInput.onchange = (e: Event) => {
      const target = e.target as HTMLInputElement
      const files = target.files
      if (!files || files.length === 0) return
      for (let i = 0; i < files.length; i++) {
        const file = files[i]
        const reader = new FileReader()
        reader.onload = (ev) => {
          const result = ev.target?.result as string
          if (result) {
            photoList.value.push(result)
          }
        }
        reader.readAsDataURL(file)
      }
    }
    document.body.appendChild(h5FileInput)
  }
})

onUnmounted(() => {
  if (h5FileInput && h5FileInput.parentNode) {
    h5FileInput.parentNode.removeChild(h5FileInput)
    h5FileInput = null
  }
})

function addPhoto() {
  const isH5 = typeof window !== 'undefined' && typeof document !== 'undefined'
  if (isH5 && h5FileInput) {
    h5FileInput.value = ''
    h5FileInput.click()
    return
  }
  // 小程序/APP环境
  uni.chooseImage({
    count: 9,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      photoList.value.push(...res.tempFilePaths)
    }
  })
}

// 删除照片
function removePhoto(idx: number) {
  photoList.value.splice(idx, 1)
}

// 触发SOS
function triggerSOS() {
  if (isActivated.value) return
  isActivated.value = true
  selectedUrgency.value = 'critical'
  if (!selectedSymptoms.value.length) {
    selectedSymptoms.value = ['心脏骤停']
  }
  setTimeout(() => {
    isActivated.value = false
  }, 3000)
}

// 提交救援
function submitRescue() {
  if (isSubmitting.value || isMatching.value) return

  isSubmitting.value = true

  setTimeout(() => {
    isSubmitting.value = false
    isMatching.value = true
    matchProgress.value = 0

    // 模拟匹配进度
    const timer = setInterval(() => {
      matchProgress.value += Math.floor(Math.random() * 15) + 5
      if (matchProgress.value >= 100) {
        matchProgress.value = 100
        clearInterval(timer)
        setTimeout(() => {
          isMatching.value = false
          uni.navigateTo({
            url: '/pages/rescue/detail?id=R001'
          })
        }, 800)
      }
    }, 400)
  }, 1500)
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
  background: linear-gradient(135deg, #F53F3F 0%, #E02020 100%);
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

/* 滚动内容 */
.scroll-content {
  min-height: 100vh;
  box-sizing: border-box;
}

/* SOS 主区域 */
.sos-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 0 32rpx;
  background: linear-gradient(180deg, #F53F3F 0%, rgba(245, 63, 63, 0.15) 70%, transparent 100%);
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
  border: 4rpx solid rgba(245, 63, 63, 0.3);
  animation: rescuePulse 2s ease-out infinite;
}
.sos-pulse-1 {
  width: 280rpx;
  height: 280rpx;
  animation-delay: 0s;
}
.sos-pulse-2 {
  width: 360rpx;
  height: 360rpx;
  animation-delay: 0.4s;
}
.sos-pulse-3 {
  width: 440rpx;
  height: 440rpx;
  animation-delay: 0.8s;
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
  background: linear-gradient(135deg, #F53F3F 0%, #CB2634 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12rpx 48rpx rgba(245, 63, 63, 0.5);
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
  color: rgba(255, 255, 255, 0.85);
  background: rgba(255, 255, 255, 0.15);
  padding: 8rpx 24rpx;
  border-radius: 24rpx;
}
.location-icon {
  font-size: 24rpx;
}

/* 表单区域 */
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
  background: #F53F3F;
  margin-right: 12rpx;
}
.label-text {
  font-size: 28rpx;
  font-weight: 600;
  color: #1D2129;
}
.label-hint {
  font-size: 22rpx;
  color: #C9CDD4;
  margin-left: 8rpx;
}

/* 紧急等级 */
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
  border-radius: 16rpx;
  background: #FFFFFF;
  border: 2rpx solid #E5E6EB;
  transition: all 0.3s ease;
}
.urgency-option.urgency-active {
  border-color: transparent;
}
.urgency-critical.urgency-active {
  background: linear-gradient(135deg, rgba(245, 63, 63, 0.1) 0%, rgba(245, 63, 63, 0.05) 100%);
  border-color: #F53F3F;
}
.urgency-high.urgency-active {
  background: linear-gradient(135deg, rgba(255, 154, 46, 0.1) 0%, rgba(255, 154, 46, 0.05) 100%);
  border-color: #FF9A2E;
}
.urgency-medium.urgency-active {
  background: linear-gradient(135deg, rgba(43, 111, 240, 0.1) 0%, rgba(43, 111, 240, 0.05) 100%);
  border-color: #2B6FF0;
}
.urgency-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
}
.urgency-critical .urgency-dot {
  background: #F53F3F;
}
.urgency-high .urgency-dot {
  background: #FF9A2E;
}
.urgency-medium .urgency-dot {
  background: #2B6FF0;
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
  color: #F53F3F;
}
.urgency-high.urgency-active .urgency-label {
  color: #FF9A2E;
}
.urgency-medium.urgency-active .urgency-label {
  color: #2B6FF0;
}

/* 症状标签 */
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
  transition: all 0.3s ease;
}
.symptom-tag-active {
  background: linear-gradient(135deg, #F53F3F 0%, #E02020 100%);
  border-color: transparent;
}
.symptom-tag-text {
  font-size: 26rpx;
  color: #4E5969;
  font-weight: 500;
}
.symptom-tag-active .symptom-tag-text {
  color: #FFFFFF;
  font-weight: 600;
}

/* 描述输入 */
.desc-input-wrap {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 24rpx;
  position: relative;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}
.desc-input {
  width: 100%;
  height: 200rpx;
  font-size: 28rpx;
  color: #1D2129;
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

/* 照片上传 */
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

/* 提交按钮 */
.submit-area {
  padding: 48rpx 32rpx;
}
.submit-btn {
  width: 100%;
  height: 96rpx;
  border-radius: 48rpx;
  background: linear-gradient(135deg, #F53F3F 0%, #E02020 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 32rpx rgba(245, 63, 63, 0.35);
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

/* 匹配动画遮罩 */
.matching-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(8px);
}
.matching-card {
  width: 560rpx;
  background: #FFFFFF;
  border-radius: 32rpx;
  padding: 64rpx 48rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.matching-animation {
  position: relative;
  width: 200rpx;
  height: 200rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.matching-ring {
  position: absolute;
  border-radius: 50%;
  border: 4rpx solid rgba(43, 111, 240, 0.3);
  animation: matchPulse 2s ease-out infinite;
}
.matching-ring-1 {
  width: 120rpx;
  height: 120rpx;
  animation-delay: 0s;
}
.matching-ring-2 {
  width: 160rpx;
  height: 160rpx;
  animation-delay: 0.3s;
}
.matching-ring-3 {
  width: 200rpx;
  height: 200rpx;
  animation-delay: 0.6s;
}
@keyframes matchPulse {
  0% {
    transform: scale(0.8);
    opacity: 1;
  }
  100% {
    transform: scale(1.5);
    opacity: 0;
  }
}
.matching-center {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}
.matching-icon {
  font-size: 36rpx;
}
.matching-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1D2129;
  margin-top: 32rpx;
}
.matching-desc {
  font-size: 24rpx;
  color: #86909C;
  margin-top: 12rpx;
}
.matching-progress {
  width: 100%;
  margin-top: 32rpx;
}
.progress-bar {
  width: 100%;
  height: 12rpx;
  border-radius: 6rpx;
  background: #F2F3F5;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: 6rpx;
  background: linear-gradient(90deg, #2B6FF0 0%, #5B8DEF 100%);
  transition: width 0.3s ease;
}
.progress-text {
  font-size: 24rpx;
  color: #2B6FF0;
  font-weight: 600;
  margin-top: 12rpx;
  text-align: center;
  display: block;
}

/* 底部安全区 */
.bottom-safe {
  height: 60rpx;
}
</style>
