<template>
  <view class="page">
    <view v-if="user.isVerified" class="verified-section">
      <view class="verified-card">
        <view class="verified-bg"></view>
        <view class="verified-content">
          <view class="verified-icon-wrap">
            <app-icon class="verified-icon" name="checkmarkempty" :size="30" color="#FFFFFF" />
          </view>
          <text class="verified-title">身份信息已校验</text>
          <text class="verified-desc">身份证号码格式与校验位有效</text>
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
        <app-icon class="tip-icon" name="info-filled" :size="18" color="#64728A" />
        <text class="tip-text">仅完成号码校验与脱敏登记，不代表公安实名核验</text>
      </view>
    </view>

    <view v-else class="form-section">
      <view class="form-header">
        <app-icon-tile class="form-header-icon" name="auth-filled" tone="green" />
        <text class="form-header-title">身份信息校验</text>
        <text class="form-header-desc">校验身份证号码格式，信息将脱敏保存</text>
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

      <view class="submit-btn" @tap="handleSubmit">
        <text class="submit-text">校验并保存</text>
      </view>

    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import { getCurrentProfile, verifyIdentity } from '@/api/user'

const user = reactive({
  isVerified: false,
  realName: '',
  idCard: ''
})

const form = reactive({
  realName: '',
  idCard: ''
})

async function handleSubmit() {
  if (!form.realName.trim()) {
    uni.showToast({ title: '请输入真实姓名', icon: 'none' })
    return
  }
  if (!form.idCard.trim() || form.idCard.length < 18) {
    uni.showToast({ title: '请输入正确的身份证号', icon: 'none' })
    return
  }
  uni.showLoading({ title: '提交中...' })
  try {
    const profile = await verifyIdentity(form.realName.trim(), form.idCard.trim())
    user.isVerified = profile.verified
    user.realName = profile.realName || ''
    user.idCard = profile.idCard || ''
    uni.hideLoading()
    uni.showToast({ title: '身份信息已校验', icon: 'success' })
  } catch (error: any) {
    uni.hideLoading()
    uni.showToast({ title: error?.message || '校验失败，请检查信息', icon: 'none' })
  }
}

async function loadProfile() {
  try {
    const profile = await getCurrentProfile()
    user.isVerified = profile.verified
    user.realName = profile.realName || ''
    user.idCard = profile.idCard || ''
  } catch {
    uni.showToast({ title: '身份信息加载失败', icon: 'none' })
  }
}

onShow(loadProfile)
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F3F7FA;
  padding: 24rpx 32rpx;
  box-sizing: border-box;
}

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
  background: linear-gradient(135deg, #23956A 0%, #4DC580 50%, #7BE0A2 100%);
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
  color: #20364D;
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
  margin-bottom: 20rpx;
}
.form-header-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #20364D;
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
  color: #20364D;
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

.submit-btn {
  margin-top: 48rpx;
  background: linear-gradient(135deg, #2E6DD1 0%, #2E6DD1 100%);
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

</style>
