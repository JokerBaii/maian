<template>
  <view class="page">
    <!-- 已认证状态 -->
    <view v-if="user.isVerified" class="verified-section">
      <view class="verified-card">
        <view class="verified-bg"></view>
        <view class="verified-content">
          <view class="verified-icon-wrap">
            <text class="verified-icon">&#x2713;</text>
          </view>
          <text class="verified-title">实名认证已完成</text>
          <text class="verified-desc">您的身份信息已通过验证</text>
          <view class="verified-info">
            <view class="info-row">
              <text class="info-label">姓名</text>
              <text class="info-value">{{ user.realName }}</text>
            </view>
            <view class="info-divider"></view>
            <view class="info-row">
              <text class="info-label">身份证号</text>
              <text class="info-value">{{ user.idCard }}</text>
            </view>
          </view>
        </view>
      </view>
      <view class="tip-card">
        <text class="tip-icon">&#x2139;</text>
        <text class="tip-text">实名认证信息一经提交不可修改，如需变更请联系客服</text>
      </view>
    </view>

    <!-- 未认证状态 -->
    <view v-else class="form-section">
      <view class="form-header">
        <view class="form-header-icon">
          <text class="form-header-icon-text">🛡️</text>
        </view>
        <text class="form-header-title">实名认证</text>
        <text class="form-header-desc">完成实名认证后可使用救援服务</text>
      </view>

      <view class="form-card">
        <view class="form-item">
          <text class="form-label">真实姓名</text>
          <input
            class="form-input"
            v-model="form.realName"
            placeholder="请输入真实姓名"
            placeholder-class="form-placeholder"
          />
        </view>
        <view class="form-divider"></view>
        <view class="form-item">
          <text class="form-label">身份证号</text>
          <input
            class="form-input"
            v-model="form.idCard"
            placeholder="请输入18位身份证号"
            placeholder-class="form-placeholder"
            maxlength="18"
          />
        </view>
      </view>

      <view class="upload-section">
        <text class="upload-title">身份证照片</text>
        <view class="upload-row">
          <view class="upload-item" @tap="uploadCard('front')">
            <view class="upload-box" :class="{ 'upload-box-done': frontUploaded }">
              <view v-if="!frontUploaded" class="upload-placeholder">
                <text class="upload-plus">+</text>
                <text class="upload-label">人像面</text>
              </view>
              <view v-else class="upload-done">
                <text class="upload-done-icon">&#x2713;</text>
                <text class="upload-done-text">已上传</text>
              </view>
            </view>
          </view>
          <view class="upload-item" @tap="uploadCard('back')">
            <view class="upload-box" :class="{ 'upload-box-done': backUploaded }">
              <view v-if="!backUploaded" class="upload-placeholder">
                <text class="upload-plus">+</text>
                <text class="upload-label">国徽面</text>
              </view>
              <view v-else class="upload-done">
                <text class="upload-done-icon">&#x2713;</text>
                <text class="upload-done-text">已上传</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="submit-btn" @tap="handleSubmit">
        <text class="submit-text">提交认证</text>
      </view>

      <view class="agreement-row">
        <text class="agreement-text">提交即表示同意</text>
        <text class="agreement-link">《用户协议》</text>
        <text class="agreement-text">和</text>
        <text class="agreement-link">《隐私政策》</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { mockUser } from '@/mock/data'

const user = computed(() => mockUser)

const form = reactive({
  realName: '',
  idCard: ''
})

const frontUploaded = ref(false)
const backUploaded = ref(false)

function uploadCard(side: string) {
  uni.chooseImage({
    count: 1,
    success: () => {
      if (side === 'front') {
        frontUploaded.value = true
      } else {
        backUploaded.value = true
      }
      uni.showToast({ title: '上传成功', icon: 'success' })
    }
  })
}

function handleSubmit() {
  if (!form.realName.trim()) {
    uni.showToast({ title: '请输入真实姓名', icon: 'none' })
    return
  }
  if (!form.idCard.trim() || form.idCard.length < 18) {
    uni.showToast({ title: '请输入正确的身份证号', icon: 'none' })
    return
  }
  if (!frontUploaded.value || !backUploaded.value) {
    uni.showToast({ title: '请上传身份证照片', icon: 'none' })
    return
  }
  uni.showLoading({ title: '提交中...' })
  setTimeout(() => {
    uni.hideLoading()
    uni.showToast({ title: '提交成功，等待审核', icon: 'none' })
  }, 1500)
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F0F4FA;
  padding: 24rpx 32rpx;
  box-sizing: border-box;
}

/* 已认证状态 */
.verified-section {
  padding-top: 40rpx;
}
.verified-card {
  position: relative;
  background: #FFFFFF;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 32rpx rgba(0, 180, 42, 0.1);
}
.verified-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 200rpx;
  background: linear-gradient(135deg, #00B42A 0%, #4DC580 50%, #7BE0A2 100%);
}
.verified-content {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48rpx 32rpx 40rpx;
}
.verified-icon-wrap {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  border: 4rpx solid rgba(255, 255, 255, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
}
.verified-icon {
  font-size: 64rpx;
  color: #FFFFFF;
  font-weight: 800;
}
.verified-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #FFFFFF;
  margin-bottom: 8rpx;
}
.verified-desc {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 36rpx;
}
.verified-info {
  width: 100%;
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 28rpx 32rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}
.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8rpx 0;
}
.info-label {
  font-size: 28rpx;
  color: #86909C;
  font-weight: 500;
}
.info-value {
  font-size: 28rpx;
  color: #1D2129;
  font-weight: 600;
}
.info-divider {
  height: 1rpx;
  background: #F2F3F5;
  margin: 12rpx 0;
}

.tip-card {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  margin-top: 24rpx;
  padding: 20rpx 24rpx;
  background: #FFF7E8;
  border-radius: 16rpx;
  border: 1rpx solid #FFE4BA;
}
.tip-icon {
  font-size: 28rpx;
  color: #FF9A2E;
  font-weight: 700;
  flex-shrink: 0;
  margin-top: 2rpx;
}
.tip-text {
  font-size: 24rpx;
  color: #7D6B4E;
  line-height: 1.6;
}

/* 未认证表单 */
.form-section {
  padding-top: 16rpx;
}
.form-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40rpx 0 32rpx;
}
.form-header-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(43, 111, 240, 0.25);
  margin-bottom: 20rpx;
}
.form-header-icon-text {
  font-size: 48rpx;
  color: #FFFFFF;
}
.form-header-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #1D2129;
  margin-bottom: 8rpx;
}
.form-header-desc {
  font-size: 26rpx;
  color: #86909C;
}

.form-card {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 8rpx 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(43, 111, 240, 0.06);
}
.form-item {
  padding: 24rpx 0;
}
.form-label {
  font-size: 26rpx;
  color: #86909C;
  font-weight: 500;
  margin-bottom: 12rpx;
  display: block;
}
.form-input {
  font-size: 30rpx;
  color: #1D2129;
  font-weight: 500;
  width: 100%;
}
.form-placeholder {
  color: #C9CDD4;
  font-size: 30rpx;
}
.form-divider {
  height: 1rpx;
  background: #F2F3F5;
}

/* 上传区域 */
.upload-section {
  margin-top: 32rpx;
}
.upload-title {
  font-size: 28rpx;
  color: #1D2129;
  font-weight: 600;
  margin-bottom: 20rpx;
  display: block;
}
.upload-row {
  display: flex;
  gap: 24rpx;
}
.upload-item {
  flex: 1;
}
.upload-box {
  height: 240rpx;
  border-radius: 20rpx;
  border: 2rpx dashed #C9CDD4;
  background: #FAFBFC;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}
.upload-box-done {
  border-color: #00B42A;
  border-style: solid;
  background: rgba(0, 180, 42, 0.04);
}
.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}
.upload-plus {
  font-size: 56rpx;
  color: #C9CDD4;
  font-weight: 300;
  line-height: 1;
}
.upload-label {
  font-size: 24rpx;
  color: #86909C;
}
.upload-done {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}
.upload-done-icon {
  font-size: 48rpx;
  color: #00B42A;
  font-weight: 700;
}
.upload-done-text {
  font-size: 24rpx;
  color: #00B42A;
  font-weight: 500;
}

/* 提交按钮 */
.submit-btn {
  margin-top: 48rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  border-radius: 48rpx;
  padding: 28rpx 0;
  text-align: center;
  box-shadow: 0 8rpx 32rpx rgba(43, 111, 240, 0.3);
}
.submit-btn:active {
  opacity: 0.85;
}
.submit-text {
  font-size: 32rpx;
  color: #FFFFFF;
  font-weight: 700;
  letter-spacing: 2rpx;
}

/* 协议 */
.agreement-row {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 24rpx;
  flex-wrap: wrap;
}
.agreement-text {
  font-size: 22rpx;
  color: #86909C;
}
.agreement-link {
  font-size: 22rpx;
  color: #2B6FF0;
  font-weight: 500;
}
</style>
