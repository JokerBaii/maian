<template>
  <view class="page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-content">
        <text class="nav-title">我的</text>
        <view class="nav-right">
          <view class="nav-icon-btn" @tap="goSettings">
            <text class="nav-icon-text">&#x2699;</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 可滚动内容区 -->
    <scroll-view
      class="scroll-content"
      scroll-y
      :style="{ paddingTop: (statusBarHeight + 44) + 'px' }"
    >
      <!-- 个人信息卡片 -->
      <view class="profile-section">
        <view class="profile-bg"></view>
        <view class="profile-card">
          <view class="profile-avatar-wrap">
            <view class="avatar-ring">
              <view class="avatar-placeholder">
                <text class="avatar-text">{{ user.nickname.charAt(0) }}</text>
              </view>
            </view>
          </view>
          <view class="profile-info">
            <view class="name-row">
              <text class="nickname">{{ user.nickname }}</text>
              <view class="role-badge">
                <text class="role-badge-text">急救志愿者</text>
              </view>
            </view>
            <text class="phone">{{ user.phone }}</text>
            <view class="verify-row">
              <view class="verify-badge">
                <text class="verify-icon">&#x2713;</text>
                <text class="verify-text">已认证</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 统计数据 -->
      <view class="stats-row">
        <view class="stat-item" @tap="goRecords">
          <text class="stat-value">2</text>
          <text class="stat-label">救援次数</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item" @tap="goDevices">
          <text class="stat-value">1</text>
          <text class="stat-label">我的设备</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item">
          <text class="stat-value">0</text>
          <text class="stat-label">科普投稿</text>
        </view>
      </view>

      <!-- 菜单列表 -->
      <!-- 急救服务 -->
      <view class="menu-section">
        <text class="menu-section-title">急救服务</text>
        <view class="menu-list">
          <view class="menu-item" @tap="goDevices">
            <view class="menu-icon-wrap menu-icon-blue">
              <text class="menu-icon-text">&#x231A;</text>
            </view>
            <text class="menu-label">我的设备</text>
            <text class="menu-arrow">&#x2038;</text>
          </view>
          <view class="menu-item" @tap="goRecords">
            <view class="menu-icon-wrap menu-icon-red">
              <text class="menu-icon-text">&#x26A1;</text>
            </view>
            <text class="menu-label">救援记录</text>
            <text class="menu-arrow">&#x2038;</text>
          </view>
          <view class="menu-item menu-item-last" @tap="goContacts">
            <view class="menu-icon-wrap menu-icon-orange">
              <text class="menu-icon-text">&#x260E;</text>
            </view>
            <text class="menu-label">紧急联系人</text>
            <text class="menu-arrow">&#x2038;</text>
          </view>
        </view>
      </view>

      <!-- 健康管理 -->
      <view class="menu-section">
        <text class="menu-section-title">健康管理</text>
        <view class="menu-list">
          <view class="menu-item" @tap="goArchive">
            <view class="menu-icon-wrap menu-icon-green">
              <text class="menu-icon-text">📁</text>
            </view>
            <text class="menu-label">健康档案</text>
            <text class="menu-arrow">&#x2038;</text>
          </view>
          <view class="menu-item menu-item-last" @tap="goCheckup">
            <view class="menu-icon-wrap menu-icon-purple">
              <text class="menu-icon-text">📋</text>
            </view>
            <text class="menu-label">体检报告</text>
            <text class="menu-arrow">&#x2038;</text>
          </view>
        </view>
      </view>

      <!-- 其他 -->
      <view class="menu-section">
        <text class="menu-section-title">其他</text>
        <view class="menu-list">
          <view class="menu-item" @tap="goAuth">
            <view class="menu-icon-wrap menu-icon-cyan">
              <text class="menu-icon-text">🛡️</text>
            </view>
            <text class="menu-label">实名认证</text>
            <view class="menu-extra">
              <text class="menu-extra-text" v-if="user.isVerified">已认证</text>
            </view>
            <text class="menu-arrow">&#x2038;</text>
          </view>
          <view class="menu-item menu-item-last" @tap="goSettings">
            <view class="menu-icon-wrap menu-icon-grey">
              <text class="menu-icon-text">&#x2699;</text>
            </view>
            <text class="menu-label">设置</text>
            <text class="menu-arrow">&#x2038;</text>
          </view>
        </view>
      </view>

      <!-- 退出登录 -->
      <view class="logout-btn" @tap="handleLogout">
        <text class="logout-text">退出登录</text>
      </view>

      <!-- 底部安全区 -->
      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { mockUser } from '@/mock/data'

const user = computed(() => mockUser)

const statusBarHeight = ref(0)
const systemInfo = uni.getSystemInfoSync()
statusBarHeight.value = systemInfo.statusBarHeight || 20

function goDevices() {
  uni.navigateTo({ url: '/pages/mine/devices' })
}
function goRecords() {
  uni.navigateTo({ url: '/pages/mine/records' })
}
function goContacts() {
  uni.navigateTo({ url: '/pages/mine/contacts' })
}
function goAuth() {
  uni.navigateTo({ url: '/pages/mine/auth' })
}
function goSettings() {
  uni.navigateTo({ url: '/pages/mine/settings' })
}
function goArchive() {
  uni.navigateTo({ url: '/pages/checkup/archive' })
}
function goCheckup() {
  uni.navigateTo({ url: '/pages/checkup/upload' })
}
function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定要退出登录吗？',
    confirmColor: '#2B6FF0',
    success: (res) => {
      if (res.confirm) {
        uni.showToast({ title: '已退出登录', icon: 'none' })
      }
    }
  })
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
.nav-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #FFFFFF;
}
.nav-right {
  display: flex;
  align-items: center;
}
.nav-icon-btn {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
}
.nav-icon-text {
  font-size: 36rpx;
  color: #FFFFFF;
}

/* 滚动内容 */
.scroll-content {
  min-height: 100vh;
  box-sizing: border-box;
}

/* 个人信息区 */
.profile-section {
  position: relative;
  padding: 24rpx 32rpx 0;
  overflow: hidden;
}
.profile-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 360rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 50%, #7DA8F7 100%);
  border-radius: 0 0 48rpx 48rpx;
}
.profile-card {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  padding: 36rpx 32rpx;
}
.profile-avatar-wrap {
  margin-right: 28rpx;
}
.avatar-ring {
  width: 128rpx;
  height: 128rpx;
  border-radius: 50%;
  padding: 6rpx;
  background: rgba(255, 255, 255, 0.3);
  border: 2rpx solid rgba(255, 255, 255, 0.5);
}
.avatar-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: linear-gradient(135deg, #FFFFFF 0%, #E8F0FE 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-text {
  font-size: 52rpx;
  font-weight: 800;
  color: #2B6FF0;
}
.profile-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.name-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.nickname {
  font-size: 36rpx;
  font-weight: 700;
  color: #FFFFFF;
}
.role-badge {
  background: rgba(255, 255, 255, 0.25);
  border-radius: 8rpx;
  padding: 4rpx 14rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.4);
}
.role-badge-text {
  font-size: 20rpx;
  color: #FFFFFF;
  font-weight: 500;
}
.phone {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
}
.verify-row {
  margin-top: 4rpx;
}
.verify-badge {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  background: rgba(0, 180, 42, 0.3);
  border-radius: 8rpx;
  padding: 4rpx 14rpx;
  border: 1rpx solid rgba(0, 180, 42, 0.4);
}
.verify-icon {
  font-size: 20rpx;
  color: #4DC580;
  font-weight: 700;
}
.verify-text {
  font-size: 20rpx;
  color: #4DC580;
  font-weight: 600;
}

/* 统计数据 */
.stats-row {
  display: flex;
  align-items: center;
  margin: 24rpx 32rpx 0;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 32rpx 0;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.08);
}
.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}
.stat-value {
  font-size: 44rpx;
  font-weight: 800;
  color: #1D2129;
  line-height: 1;
}
.stat-label {
  font-size: 24rpx;
  color: #86909C;
  font-weight: 500;
}
.stat-divider {
  width: 1rpx;
  height: 60rpx;
  background: #E5E6EB;
}

/* 菜单分区 */
.menu-section {
  margin-top: 32rpx;
  padding: 0 32rpx;
}
.menu-section-title {
  font-size: 26rpx;
  color: #86909C;
  font-weight: 600;
  margin-bottom: 16rpx;
  padding-left: 8rpx;
}
.menu-list {
  background: #FFFFFF;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(43, 111, 240, 0.06);
}
.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx 24rpx;
  border-bottom: 1rpx solid #F2F3F5;
  position: relative;
}
.menu-item:active {
  background: #F7F8FA;
}
.menu-item-last {
  border-bottom: none;
}
.menu-icon-wrap {
  width: 64rpx;
  height: 64rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  flex-shrink: 0;
}
.menu-icon-blue {
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  box-shadow: 0 4rpx 12rpx rgba(43, 111, 240, 0.2);
}
.menu-icon-red {
  background: linear-gradient(135deg, #F53F3F 0%, #FF7D7D 100%);
  box-shadow: 0 4rpx 12rpx rgba(245, 63, 63, 0.2);
}
.menu-icon-orange {
  background: linear-gradient(135deg, #FF9A2E 0%, #FFCF8B 100%);
  box-shadow: 0 4rpx 12rpx rgba(255, 154, 46, 0.2);
}
.menu-icon-green {
  background: linear-gradient(135deg, #00B42A 0%, #4DC580 100%);
  box-shadow: 0 4rpx 12rpx rgba(0, 180, 42, 0.2);
}
.menu-icon-purple {
  background: linear-gradient(135deg, #722ED1 0%, #B37FEB 100%);
  box-shadow: 0 4rpx 12rpx rgba(114, 46, 209, 0.2);
}
.menu-icon-cyan {
  background: linear-gradient(135deg, #0FC6C2 0%, #5CE0DB 100%);
  box-shadow: 0 4rpx 12rpx rgba(15, 198, 194, 0.2);
}
.menu-icon-grey {
  background: linear-gradient(135deg, #86909C 0%, #C9CDD4 100%);
  box-shadow: 0 4rpx 12rpx rgba(134, 144, 156, 0.2);
}
.menu-icon-text {
  font-size: 32rpx;
  color: #FFFFFF;
}
.menu-label {
  flex: 1;
  font-size: 30rpx;
  color: #1D2129;
  font-weight: 500;
}
.menu-extra {
  margin-right: 8rpx;
}
.menu-extra-text {
  font-size: 24rpx;
  color: #00B42A;
  font-weight: 500;
}
.menu-arrow {
  font-size: 28rpx;
  color: #C9CDD4;
  font-weight: 300;
}

/* 退出登录 */
.logout-btn {
  margin: 48rpx 32rpx 0;
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 28rpx 0;
  text-align: center;
  box-shadow: 0 4rpx 16rpx rgba(43, 111, 240, 0.06);
}
.logout-btn:active {
  background: #F7F8FA;
}
.logout-text {
  font-size: 30rpx;
  color: #F53F3F;
  font-weight: 600;
}

/* 底部安全区 */
.bottom-safe {
  height: 200rpx;
}
</style>
