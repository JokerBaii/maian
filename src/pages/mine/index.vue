<template>
  <view class="page apple-page">
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-content">
        <text class="nav-title">我的</text>
        <view class="nav-right">
          <view class="nav-icon-btn" @tap="goSettings">
            <app-icon class="nav-icon-text" name="settings-filled" :size="21" color="#2C2C2E" />
          </view>
        </view>
      </view>
    </view>

    <scroll-view
      class="scroll-content"
      scroll-y
      :show-scrollbar="false"
      :style="{ paddingTop: (statusBarHeight + 44) + 'px' }"
    >
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
                <text class="role-badge-text">{{ roleLabel }}</text>
              </view>
            </view>
            <text class="phone">{{ user.phone }}</text>
            <view class="verify-row">
              <view v-if="user.isVerified" class="verify-badge">
                <app-icon class="verify-icon" name="checkmarkempty" :size="12" color="#FFFFFF" />
                <text class="verify-text">已校验</text>
              </view>
              <view v-else class="verify-badge verify-badge-unverified">
                <text class="verify-text">未校验</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="stats-row">
        <view class="stat-item" @tap="goRecords">
          <text class="stat-value">{{ rescueCount }}</text>
          <text class="stat-label">救援次数</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item" @tap="goDevices">
          <text class="stat-value">{{ deviceCount }}</text>
          <text class="stat-label">我的设备</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item" @tap="goSubmissions">
          <text class="stat-value">{{ contributionCount }}</text>
          <text class="stat-label">科普投稿</text>
        </view>
      </view>

      <view class="menu-section">
        <text class="menu-section-title">急救服务</text>
        <view class="menu-list">
          <view class="menu-item" @tap="goDevices">
            <app-icon-tile class="menu-icon" name="wearable" tone="coral" />
            <text class="menu-label">我的设备</text>
            <app-icon class="menu-arrow" name="right" :size="14" color="#A7B0C0" />
          </view>
          <view class="menu-item" @tap="goRecords">
            <app-icon-tile class="menu-icon" name="notification-filled" tone="coral" />
            <text class="menu-label">救援记录</text>
            <app-icon class="menu-arrow" name="right" :size="14" color="#A7B0C0" />
          </view>
          <view v-if="user.role === 'VOLUNTEER'" class="menu-item" @tap="goRescueTasks">
            <app-icon-tile class="menu-icon" name="volunteer" tone="green" />
            <text class="menu-label">救援任务</text>
            <app-icon class="menu-arrow" name="right" :size="14" color="#A7B0C0" />
          </view>
          <view v-if="user.role === 'ADMIN'" class="menu-item" @tap="goDeviceReview">
            <app-icon-tile class="menu-icon" name="check" tone="blue" />
            <text class="menu-label">设备审核</text>
            <app-icon class="menu-arrow" name="right" :size="14" color="#A7B0C0" />
          </view>
          <view v-if="user.role === 'ADMIN'" class="menu-item" @tap="goScienceReview">
            <app-icon-tile class="menu-icon" name="science-update" tone="cyan" />
            <text class="menu-label">投稿审核</text>
            <app-icon class="menu-arrow" name="right" :size="14" color="#A7B0C0" />
          </view>
          <view class="menu-item menu-item-last" @tap="goContacts">
            <app-icon-tile class="menu-icon" name="phone-filled" tone="green" />
            <text class="menu-label">紧急联系人</text>
            <app-icon class="menu-arrow" name="right" :size="14" color="#A7B0C0" />
          </view>
        </view>
      </view>

      <view class="menu-section">
        <text class="menu-section-title">健康管理</text>
        <view class="menu-list">
          <view class="menu-item" @tap="goArchive">
            <app-icon-tile class="menu-icon" name="folder-add-filled" tone="blue" />
            <text class="menu-label">健康档案</text>
            <app-icon class="menu-arrow" name="right" :size="14" color="#A7B0C0" />
          </view>
          <view class="menu-item menu-item-last" @tap="goCheckup">
            <app-icon-tile class="menu-icon" name="list" tone="violet" />
            <text class="menu-label">体检报告</text>
            <app-icon class="menu-arrow" name="right" :size="14" color="#A7B0C0" />
          </view>
        </view>
      </view>

      <view class="menu-section">
        <text class="menu-section-title">其他</text>
        <view class="menu-list">
          <view class="menu-item" @tap="goDemoMode">
            <app-icon-tile class="menu-icon" name="user-filled" tone="blue" />
            <text class="menu-label">演示身份切换</text>
            <view class="menu-extra"><text class="menu-extra-text">比赛演示</text></view>
            <app-icon class="menu-arrow" name="right" :size="14" color="#A7B0C0" />
          </view>
          <view class="menu-item" @tap="goAuth">
            <app-icon-tile class="menu-icon" name="auth-filled" tone="green" />
            <text class="menu-label">身份信息</text>
            <view class="menu-extra">
              <text class="menu-extra-text" v-if="user.isVerified">已校验</text>
            </view>
            <app-icon class="menu-arrow" name="right" :size="14" color="#A7B0C0" />
          </view>
          <view class="menu-item menu-item-last" @tap="goSettings">
            <app-icon-tile class="menu-icon" name="settings-filled" tone="slate" />
            <text class="menu-label">设置</text>
            <app-icon class="menu-arrow" name="right" :size="14" color="#A7B0C0" />
          </view>
        </view>
      </view>

      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, reactive } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import { listMyEmergencyDevices } from '@/api/devices'
import { listRescueCalls } from '@/api/rescue'
import { getCurrentProfile } from '@/api/user'
import { getScienceSubmissionCount } from '@/api/science'

const user = reactive({
  nickname: '用户',
  phone: '',
  role: 'USER',
  isVerified: false
})
const roleLabel = computed(() => ({
  USER: '普通用户',
  VOLUNTEER: '急救志愿者',
  ADMIN: '平台审核员'
} as Record<string, string>)[user.role] || '当前用户')
const rescueCount = ref(0)
const deviceCount = ref(0)
const contributionCount = ref(0)

const statusBarHeight = ref(0)
const systemInfo = uni.getSystemInfoSync()
statusBarHeight.value = systemInfo.statusBarHeight || 20

function goDevices() {
  uni.navigateTo({ url: '/pages/device/manage' })
}
function goSubmissions() {
  uni.navigateTo({ url: '/pages/science/submissions' })
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
function goDemoMode() {
  uni.navigateTo({ url: '/pages/mine/demo' })
}
function goRescueTasks() {
  uni.navigateTo({ url: '/pages/rescue/tasks' })
}
function goDeviceReview() {
  uni.navigateTo({ url: '/pages/device/review' })
}
function goScienceReview() {
  uni.navigateTo({ url: '/pages/science/review' })
}
function goArchive() {
  uni.navigateTo({ url: '/pages/checkup/archive' })
}
function goCheckup() {
  uni.navigateTo({ url: '/pages/checkup/upload' })
}
async function loadStats() {
  const [rescues, devices, profile, submissions] = await Promise.allSettled([
    listRescueCalls(),
    listMyEmergencyDevices(),
    getCurrentProfile(),
    getScienceSubmissionCount()
  ])
  if (rescues.status === 'fulfilled') rescueCount.value = rescues.value.totalElements
  if (devices.status === 'fulfilled') deviceCount.value = devices.value.totalElements
  if (profile.status === 'fulfilled') {
    user.nickname = profile.value.nickname
    user.phone = profile.value.phone
    user.role = profile.value.role
    user.isVerified = profile.value.verified
  }
  if (submissions.status === 'fulfilled') contributionCount.value = submissions.value.count
}

onShow(loadStats)
</script>

<style lang="scss" scoped>
.page {
  height: calc(100vh - var(--window-top, 0px) - var(--window-bottom, 0px));
  overflow: hidden;
  background: #F2F2F7;
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  border-bottom: 1rpx solid rgba(60, 60, 67, .12);
  background: rgba(249, 249, 251, .94);
  backdrop-filter: blur(24rpx);
}
.nav-bar-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 88rpx;
  padding: 0 32rpx;
}
.nav-title {
  font-size: 34rpx;
  font-weight: 650;
  color: #1C1C1E;
}
.nav-right {
  display: flex;
  align-items: center;
}
.nav-icon-btn {
  width: 64rpx;
  height: 64rpx;
  border-radius: 18rpx;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
}
.nav-icon-text {
  font-size: 36rpx;
  color: #FFFFFF;
}

.scroll-content {
  height: 100%;
  box-sizing: border-box;
}

.profile-section {
  position: relative;
  padding: 24rpx 24rpx 0;
  overflow: hidden;
}
.profile-bg {
  display: none;
}
.profile-card {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  padding: 30rpx 26rpx;
  border: 1rpx solid rgba(60, 60, 67, .12);
  border-radius: 24rpx;
  background: #FFFFFF;
}
.profile-avatar-wrap {
  margin-right: 28rpx;
}
.avatar-ring {
  width: 104rpx;
  height: 104rpx;
  border-radius: 50%;
  padding: 0;
  background: #E8F1FD;
}
.avatar-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: #E8F1FD;
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-text {
  font-size: 44rpx;
  font-weight: 800;
  color: #2E6DD1;
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
  color: #1C1C1E;
}
.role-badge {
  background: #EEF4FC;
  border-radius: 8rpx;
  padding: 4rpx 14rpx;
  border: 0;
}
.role-badge-text {
  font-size: 20rpx;
  color: #2E6DD1;
  font-weight: 500;
}
.phone {
  font-size: 26rpx;
  color: #8E8E93;
}
.verify-row {
  margin-top: 4rpx;
}
.verify-badge {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  background: #E8F6EF;
  border-radius: 8rpx;
  padding: 4rpx 14rpx;
  border: 0;
}

.verify-badge-unverified {
  background: rgba(169, 180, 194, 0.22);
  border-color: rgba(169, 180, 194, 0.4);
}

.verify-badge-unverified .verify-text {
  color: #68758A;
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

.stats-row {
  display: flex;
  align-items: center;
  margin: 16rpx 24rpx 0;
  background: #FFFFFF;
  border: 1rpx solid rgba(60, 60, 67, .12);
  border-radius: 22rpx;
  padding: 26rpx 0;
  box-shadow: none;
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
  color: #20364D;
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

.menu-section {
  margin-top: 28rpx;
  padding: 0 24rpx;
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
  border: 1rpx solid rgba(60, 60, 67, .12);
  border-radius: 22rpx;
  overflow: hidden;
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
.menu-icon {
  margin-right: 18rpx;
}
.menu-label {
  flex: 1;
  font-size: 30rpx;
  color: #20364D;
  font-weight: 500;
}
.menu-extra {
  margin-right: 8rpx;
}
.menu-extra-text {
  font-size: 24rpx;
  color: #23956A;
  font-weight: 500;
}
.menu-arrow {
  font-size: 28rpx;
  color: #C9CDD4;
  font-weight: 300;
}

.bottom-safe {
  height: 200rpx;
}
</style>
