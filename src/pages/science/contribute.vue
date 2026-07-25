<template>
  <view class="page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-content">
        <view class="nav-back" @tap="goBack">
          <text class="back-arrow">&#x2190;</text>
        </view>
        <text class="nav-title">内容投稿</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <scroll-view
      class="scroll-content"
      scroll-y
      :style="{ paddingTop: (statusBarHeight + 44) + 'px' }"
    >
      <!-- 投稿须知卡片 -->
      <view class="tips-card">
        <view class="tips-header">
          <view class="tips-icon-wrap">
            <text class="tips-icon">📝</text>
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

      <!-- 表单区域 -->
      <view class="form-card">
        <!-- 标题输入 -->
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

        <!-- 分类选择 -->
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

        <!-- 内容输入 -->
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

        <!-- 封面图上传 -->
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
                <text class="remove-icon">&#x2717;</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 提交按钮 -->
      <view class="submit-area">
        <view
          class="submit-btn"
          :class="{ 'submit-btn-disabled': !canSubmit }"
          @tap="handleSubmit"
        >
          <text class="submit-btn-text" :class="{ 'submit-btn-text-disabled': !canSubmit }">提交投稿</text>
        </view>
      </view>

      <!-- 底部安全区 -->
      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

// 系统信息
const statusBarHeight = ref(0)
const systemInfo = uni.getSystemInfoSync()
statusBarHeight.value = systemInfo.statusBarHeight || 20

// 分类选项
const categories = [
  { key: 'device', label: '设备使用' },
  { key: 'emergency', label: '突发急症' },
  { key: 'health', label: '健康管理' },
  { key: 'exercise', label: '运动养生' }
]

// 表单数据
const formData = ref({
  title: '',
  category: '',
  content: '',
  coverImage: ''
})

// 是否可提交
const canSubmit = computed(() => {
  return formData.value.title.trim() !== '' &&
    formData.value.category !== '' &&
    formData.value.content.trim() !== ''
})

// 选择封面图
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

// 移除封面图
function removeCoverImage() {
  formData.value.coverImage = ''
}

// 提交
function handleSubmit() {
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

  setTimeout(() => {
    uni.hideLoading()
    uni.showToast({
      title: '投稿成功，等待审核',
      icon: 'success',
      duration: 2000
    })

    // 重置表单
    setTimeout(() => {
      formData.value = {
        title: '',
        category: '',
        content: '',
        coverImage: ''
      }
    }, 2000)
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
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
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

/* 投稿须知卡片 */
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

/* 表单卡片 */
.form-card {
  margin: 24rpx 24rpx 0;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 8rpx 28rpx 28rpx;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.06);
}

/* 表单区块 */
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

/* 输入框 */
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

/* 分类选择 */
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

/* 文本域 */
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

/* 封面图上传 */
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

/* 提交按钮 */
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

/* 底部安全区 */
.bottom-safe {
  height: 60rpx;
}
</style>
