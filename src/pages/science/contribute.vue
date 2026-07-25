<template>
  <view class="page">
    <scroll-view class="scroll-content" scroll-y>
      <view class="tips-card">
        <view class="tips-header">
          <view class="tips-icon-wrap">
            <app-icon class="tips-icon" name="compose" :size="22" color="#1F63D5" />
          </view>
          <text class="tips-title">投稿须知</text>
        </view>
        <view class="tips-list">
          <view class="tips-item">
            <view class="tips-dot"></view>
            <text class="tips-text">内容需与急救、健康、医疗相关，确保科学准确</text>
          </view>
          <view class="tips-item">
            <view class="tips-dot"></view>
            <text class="tips-text">请勿发布虚假信息或未经证实的偏方</text>
          </view>
          <view class="tips-item">
            <view class="tips-dot"></view>
            <text class="tips-text">原创内容优先，转载需注明来源</text>
          </view>
          <view class="tips-item">
            <view class="tips-dot"></view>
            <text class="tips-text">投稿审核通过后将在科普频道展示</text>
          </view>
        </view>
      </view>

      <view class="form-card">
        <view class="form-section">
          <view class="form-label-row">
            <view class="form-label-bar"></view>
            <text class="form-label">标题</text>
            <text class="form-required">*</text>
          </view>
          <view class="input-wrap">
            <input
              class="form-input"
              v-model="formData.title"
              placeholder="请输入文章标题"
              placeholder-class="input-placeholder"
              maxlength="50"
            />
            <text class="input-count">{{ formData.title.length }}/50</text>
          </view>
        </view>

        <view class="form-section">
          <view class="form-label-row">
            <view class="form-label-bar"></view>
            <text class="form-label">分类</text>
            <text class="form-required">*</text>
          </view>
          <view class="category-list">
            <view
              v-for="cat in categories"
              :key="cat.key"
              class="category-item"
              :class="{ 'category-active': formData.category === cat.key }"
              @tap="formData.category = cat.key"
            >
              <text class="category-item-text" :class="{ 'category-item-text-active': formData.category === cat.key }">{{ cat.label }}</text>
            </view>
          </view>
        </view>

        <view class="form-section">
          <view class="form-label-row">
            <view class="form-label-bar"></view>
            <text class="form-label">内容</text>
            <text class="form-required">*</text>
          </view>
          <view class="textarea-wrap">
            <textarea
              class="form-textarea"
              v-model="formData.content"
              placeholder="请输入文章内容，支持分段描述..."
              placeholder-class="input-placeholder"
              maxlength="2000"
              :auto-height="false"
            />
            <view class="textarea-footer">
              <text class="textarea-count">{{ formData.content.length }}/2000</text>
            </view>
          </view>
        </view>

        <view class="form-section">
          <view class="form-label-row">
            <view class="form-label-bar"></view>
            <text class="form-label">封面图</text>
          </view>
          <view class="upload-area">
            <view v-if="!formData.coverImage" class="upload-slot" @tap="chooseCoverImage">
              <view class="upload-icon-wrap">
                <text class="upload-icon">+</text>
              </view>
              <text class="upload-text">上传封面</text>
              <text class="upload-hint">建议尺寸 750x420</text>
            </view>
            <view v-else class="upload-preview" @tap="chooseCoverImage">
              <image class="preview-image" :src="formData.coverImage" mode="aspectFill" />
              <view class="preview-remove" @tap.stop="removeCoverImage">
                <app-icon class="remove-icon" name="closeempty" :size="14" color="#FFFFFF" />
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="submit-area">
        <view
          class="submit-btn"
          :class="{ 'submit-btn-disabled': !canSubmit }"
          @tap="handleSubmit"
        >
          <text class="submit-btn-text" :class="{ 'submit-btn-text-disabled': !canSubmit }">提交投稿</text>
        </view>
      </view>

      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { createScienceSubmission } from '@/api/science'
import { uploadImage } from '@/api/files'

const categories = [
  { key: 'device', label: '设备使用' },
  { key: 'emergency', label: '突发急症' },
  { key: 'health', label: '健康管理' },
  { key: 'exercise', label: '运动养生' }
]

const formData = ref({
  title: '',
  category: '',
  content: '',
  coverImage: ''
})

const canSubmit = computed(() => {
  return formData.value.title.trim() !== '' &&
    formData.value.category !== '' &&
    formData.value.content.trim() !== ''
})

function chooseCoverImage() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      formData.value.coverImage = res.tempFilePaths[0]
    }
  })
}

function removeCoverImage() {
  formData.value.coverImage = ''
}

async function handleSubmit() {
  if (!canSubmit.value) {
    uni.showToast({
      title: '请填写必填项',
      icon: 'none',
      duration: 1500
    })
    return
  }

  uni.showLoading({
    title: '提交中...'
  })

  try {
    const cover = formData.value.coverImage
      ? await uploadImage(formData.value.coverImage)
      : null
    await createScienceSubmission({
      title: formData.value.title.trim(),
      category: formData.value.category,
      content: formData.value.content.trim(),
      coverImageUrl: cover?.url
    })
    uni.hideLoading()
    uni.showToast({
      title: '投稿成功，等待审核',
      icon: 'success',
      duration: 1500
    })
    formData.value = {
      title: '',
      category: '',
      content: '',
      coverImage: ''
    }
  } catch (error: any) {
    uni.hideLoading()
    uni.showToast({ title: error?.message || '投稿失败，请重试', icon: 'none' })
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

.tips-card {
  margin: 24rpx 24rpx 0;
  background: linear-gradient(135deg, #E8F0FE 0%, #D4E4FC 100%);
  border-radius: 24rpx;
  padding: 28rpx 24rpx;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.08);
}
.tips-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 20rpx;
}
.tips-icon-wrap {
  width: 48rpx;
  height: 48rpx;
  border-radius: 12rpx;
  background: rgba(43, 111, 240, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
}
.tips-icon {
  font-size: 28rpx;
}
.tips-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #2B6FF0;
}
.tips-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}
.tips-item {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
}
.tips-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #2B6FF0;
  margin-top: 12rpx;
  flex-shrink: 0;
}
.tips-text {
  font-size: 24rpx;
  color: #4E5969;
  line-height: 1.6;
}

.form-card {
  margin: 24rpx 24rpx 0;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 8rpx 28rpx 28rpx;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.06);
}

.form-section {
  padding: 24rpx 0;
  border-bottom: 1rpx solid #F2F3F5;
}
.form-section:last-child {
  border-bottom: none;
}
.form-label-row {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}
.form-label-bar {
  width: 6rpx;
  height: 24rpx;
  border-radius: 3rpx;
  background: #2B6FF0;
  margin-right: 10rpx;
}
.form-label {
  font-size: 28rpx;
  font-weight: 600;
  color: #1D2129;
}
.form-required {
  font-size: 28rpx;
  color: #F53F3F;
  margin-left: 4rpx;
}

.input-wrap {
  display: flex;
  align-items: center;
  background: #F7F8FA;
  border-radius: 16rpx;
  padding: 0 24rpx;
  height: 88rpx;
  border: 2rpx solid transparent;
  transition: all 0.3s ease;
}
.input-wrap:focus-within {
  border-color: #2B6FF0;
  background: #FFFFFF;
  box-shadow: 0 0 0 4rpx rgba(43, 111, 240, 0.08);
}
.form-input {
  flex: 1;
  font-size: 28rpx;
  color: #1D2129;
  height: 88rpx;
}
.input-placeholder {
  color: #C9CDD4;
  font-size: 28rpx;
}
.input-count {
  font-size: 22rpx;
  color: #C9CDD4;
  margin-left: 12rpx;
  flex-shrink: 0;
}

.category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.category-item {
  padding: 16rpx 32rpx;
  border-radius: 16rpx;
  background: #F7F8FA;
  border: 2rpx solid transparent;
  transition: all 0.3s ease;
}
.category-item:active {
  transform: scale(0.96);
}
.category-active {
  background: rgba(43, 111, 240, 0.06);
  border-color: #2B6FF0;
  box-shadow: 0 2rpx 12rpx rgba(43, 111, 240, 0.12);
}
.category-item-text {
  font-size: 26rpx;
  color: #4E5969;
  font-weight: 500;
  transition: color 0.3s ease;
}
.category-item-text-active {
  color: #2B6FF0;
  font-weight: 700;
}

.textarea-wrap {
  background: #F7F8FA;
  border-radius: 16rpx;
  padding: 24rpx;
  border: 2rpx solid transparent;
  transition: all 0.3s ease;
}
.textarea-wrap:focus-within {
  border-color: #2B6FF0;
  background: #FFFFFF;
  box-shadow: 0 0 0 4rpx rgba(43, 111, 240, 0.08);
}
.form-textarea {
  width: 100%;
  min-height: 280rpx;
  font-size: 28rpx;
  color: #1D2129;
  line-height: 1.7;
  box-sizing: border-box;
}
.textarea-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 12rpx;
}
.textarea-count {
  font-size: 22rpx;
  color: #C9CDD4;
}

.upload-area {
  margin-top: 4rpx;
}
.upload-slot {
  width: 320rpx;
  height: 220rpx;
  border-radius: 16rpx;
  border: 2rpx dashed #C9CDD4;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  transition: all 0.3s ease;
  background: #FAFBFC;
}
.upload-slot:active {
  border-color: #2B6FF0;
  background: rgba(43, 111, 240, 0.02);
}
.upload-icon-wrap {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: #E8F0FE;
  display: flex;
  align-items: center;
  justify-content: center;
}
.upload-icon {
  font-size: 36rpx;
  color: #2B6FF0;
  font-weight: 700;
}
.upload-text {
  font-size: 24rpx;
  color: #86909C;
  font-weight: 500;
}
.upload-hint {
  font-size: 20rpx;
  color: #C9CDD4;
}
.upload-preview {
  width: 320rpx;
  height: 220rpx;
  border-radius: 16rpx;
  overflow: hidden;
  position: relative;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.08);
}
.preview-image {
  width: 100%;
  height: 100%;
}
.preview-remove {
  position: absolute;
  top: 12rpx;
  right: 12rpx;
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}
.remove-icon {
  font-size: 24rpx;
  color: #FFFFFF;
  font-weight: 700;
}

.submit-area {
  padding: 40rpx 24rpx 0;
}
.submit-btn {
  height: 96rpx;
  border-radius: 48rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 32rpx rgba(43, 111, 240, 0.3);
  transition: all 0.2s ease;
}
.submit-btn:active {
  transform: scale(0.97);
}
.submit-btn-disabled {
  background: linear-gradient(135deg, #C9CDD4 0%, #D5D8DE 100%);
  box-shadow: none;
}
.submit-btn-disabled:active {
  transform: none;
}
.submit-btn-text {
  font-size: 32rpx;
  font-weight: 700;
  color: #FFFFFF;
}
.submit-btn-text-disabled {
  color: rgba(255, 255, 255, 0.6);
}

.bottom-safe {
  height: 60rpx;
}
</style>
