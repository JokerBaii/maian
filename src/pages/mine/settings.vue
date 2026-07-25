<template>
  <view class="page">
    <scroll-view class="settings-scroll" scroll-y>
      <!-- 通知设置 -->
      <view class="settings-section">
        <text class="section-title">通知设置</text>
        <view class="settings-list">
          <view class="settings-item">
            <view class="settings-item-left">
              <view class="settings-icon-wrap settings-icon-red">
                <text class="settings-icon-text">🚨</text>
              </view>
              <text class="settings-label">呼救推送</text>
            </view>
            <view class="toggle-wrap" @tap="settings.rescuePush = !settings.rescuePush">
              <view class="toggle-track" :class="{ 'toggle-on': settings.rescuePush }">
                <view class="toggle-thumb"></view>
              </view>
            </view>
          </view>
          <view class="settings-item">
            <view class="settings-item-left">
              <view class="settings-icon-wrap settings-icon-orange">
                <text class="settings-icon-text">&#x26A0;</text>
              </view>
              <text class="settings-label">健康预警</text>
            </view>
            <view class="toggle-wrap" @tap="settings.healthAlert = !settings.healthAlert">
              <view class="toggle-track" :class="{ 'toggle-on': settings.healthAlert }">
                <view class="toggle-thumb"></view>
              </view>
            </view>
          </view>
          <view class="settings-item settings-item-last">
            <view class="settings-item-left">
              <view class="settings-icon-wrap settings-icon-blue">
                <text class="settings-icon-text">📢</text>
              </view>
              <text class="settings-label">科普更新</text>
            </view>
            <view class="toggle-wrap" @tap="settings.scienceUpdate = !settings.scienceUpdate">
              <view class="toggle-track" :class="{ 'toggle-on': settings.scienceUpdate }">
                <view class="toggle-thumb"></view>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 隐私设置 -->
      <view class="settings-section">
        <text class="section-title">隐私设置</text>
        <view class="settings-list">
          <view class="settings-item">
            <view class="settings-item-left">
              <view class="settings-icon-wrap settings-icon-green">
                <text class="settings-icon-text">📍</text>
              </view>
              <text class="settings-label">位置共享</text>
            </view>
            <view class="toggle-wrap" @tap="settings.locationShare = !settings.locationShare">
              <view class="toggle-track" :class="{ 'toggle-on': settings.locationShare }">
                <view class="toggle-thumb"></view>
              </view>
            </view>
          </view>
          <view class="settings-item settings-item-last">
            <view class="settings-item-left">
              <view class="settings-icon-wrap settings-icon-purple">
                <text class="settings-icon-text">📊</text>
              </view>
              <text class="settings-label">健康数据共享</text>
            </view>
            <view class="toggle-wrap" @tap="settings.healthDataShare = !settings.healthDataShare">
              <view class="toggle-track" :class="{ 'toggle-on': settings.healthDataShare }">
                <view class="toggle-thumb"></view>
              </view>
            </view>
          </view>
        </view>
      </view>

      <!-- 心率预警阈值 -->
      <view class="settings-section">
        <text class="section-title">心率预警阈值</text>
        <view class="settings-list">
          <view class="settings-item">
            <view class="settings-item-left">
              <view class="settings-icon-wrap settings-icon-red">
                <text class="settings-icon-text">&#x2191;</text>
              </view>
              <text class="settings-label">最高心率</text>
            </view>
            <view class="threshold-wrap" @tap="editThreshold('max')">
              <text class="threshold-value">{{ settings.maxHeartRate }}</text>
              <text class="threshold-unit">BPM</text>
              <text class="threshold-arrow">&#x2038;</text>
            </view>
          </view>
          <view class="settings-item settings-item-last">
            <view class="settings-item-left">
              <view class="settings-icon-wrap settings-icon-blue">
                <text class="settings-icon-text">&#x2193;</text>
              </view>
              <text class="settings-label">最低心率</text>
            </view>
            <view class="threshold-wrap" @tap="editThreshold('min')">
              <text class="threshold-value">{{ settings.minHeartRate }}</text>
              <text class="threshold-unit">BPM</text>
              <text class="threshold-arrow">&#x2038;</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 其他 -->
      <view class="settings-section">
        <text class="section-title">其他</text>
        <view class="settings-list">
          <view class="settings-item" @tap="clearCache">
            <view class="settings-item-left">
              <view class="settings-icon-wrap settings-icon-grey">
                <text class="settings-icon-text">🗑️</text>
              </view>
              <text class="settings-label">清除缓存</text>
            </view>
            <text class="settings-extra">12.3MB</text>
          </view>
          <view class="settings-item" @tap="goAbout">
            <view class="settings-item-left">
              <view class="settings-icon-wrap settings-icon-cyan">
                <text class="settings-icon-text">&#x2139;</text>
              </view>
              <text class="settings-label">关于我们</text>
            </view>
            <text class="settings-arrow">&#x2038;</text>
          </view>
          <view class="settings-item" @tap="goAgreement">
            <view class="settings-item-left">
              <view class="settings-icon-wrap settings-icon-orange">
                <text class="settings-icon-text">📝</text>
              </view>
              <text class="settings-label">用户协议</text>
            </view>
            <text class="settings-arrow">&#x2038;</text>
          </view>
          <view class="settings-item settings-item-last" @tap="goPrivacy">
            <view class="settings-item-left">
              <view class="settings-icon-wrap settings-icon-purple">
                <text class="settings-icon-text">🔒</text>
              </view>
              <text class="settings-label">隐私政策</text>
            </view>
            <text class="settings-arrow">&#x2038;</text>
          </view>
        </view>
      </view>

      <!-- 版本信息 -->
      <view class="version-info">
        <text class="version-text">脉安驰援 v1.0.0</text>
      </view>

      <view class="bottom-safe"></view>
    </scroll-view>

    <!-- 心率阈值编辑弹窗 -->
    <view v-if="thresholdPopupVisible" class="popup-mask" @tap="thresholdPopupVisible = false">
      <view class="popup-content" @tap.stop>
        <view class="popup-header">
          <text class="popup-title">{{ editingMax ? '设置最高心率' : '设置最低心率' }}</text>
          <view class="popup-close" @tap="thresholdPopupVisible = false">
            <text class="popup-close-text">&#x2715;</text>
          </view>
        </view>
        <view class="popup-form">
          <input
            class="popup-input"
            v-model="thresholdInput"
            type="number"
            :placeholder="editingMax ? '请输入最高心率阈值' : '请输入最低心率阈值'"
            placeholder-class="popup-input-placeholder"
          />
          <text class="popup-hint">{{ editingMax ? '当心率超过此值时将触发预警' : '当心率低于此值时将触发预警' }}</text>
        </view>
        <view class="popup-submit" @tap="saveThreshold">
          <text class="popup-submit-text">确定</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'

const settings = reactive({
  rescuePush: true,
  healthAlert: true,
  scienceUpdate: false,
  locationShare: true,
  healthDataShare: false,
  maxHeartRate: 120,
  minHeartRate: 50
})

const thresholdPopupVisible = ref(false)
const editingMax = ref(true)
const thresholdInput = ref('')

function editThreshold(type: string) {
  editingMax.value = type === 'max'
  thresholdInput.value = type === 'max' ? String(settings.maxHeartRate) : String(settings.minHeartRate)
  thresholdPopupVisible.value = true
}

function saveThreshold() {
  const val = parseInt(thresholdInput.value)
  if (isNaN(val) || val <= 0) {
    uni.showToast({ title: '请输入有效数值', icon: 'none' })
    return
  }
  if (editingMax.value) {
    settings.maxHeartRate = val
  } else {
    settings.minHeartRate = val
  }
  thresholdPopupVisible.value = false
  uni.showToast({ title: '设置成功', icon: 'success' })
}

function clearCache() {
  uni.showModal({
    title: '清除缓存',
    content: '确定清除所有缓存数据吗？',
    confirmColor: '#2B6FF0',
    success: (res) => {
      if (res.confirm) {
        uni.showToast({ title: '缓存已清除', icon: 'success' })
      }
    }
  })
}

function goAbout() {
  uni.showToast({ title: '脉安驰援 v1.0.0', icon: 'none' })
}
function goAgreement() {
  uni.showToast({ title: '用户协议', icon: 'none' })
}
function goPrivacy() {
  uni.showToast({ title: '隐私政策', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F0F4FA;
}

.settings-scroll {
  height: 100vh;
  box-sizing: border-box;
}

/* 分区 */
.settings-section {
  padding: 28rpx 32rpx 0;
}
.section-title {
  font-size: 26rpx;
  color: #86909C;
  font-weight: 600;
  margin-bottom: 16rpx;
  padding-left: 8rpx;
}
.settings-list {
  background: #FFFFFF;
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(43, 111, 240, 0.06);
}
.settings-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 26rpx 24rpx;
  border-bottom: 1rpx solid #F2F3F5;
}
.settings-item:active {
  background: #F7F8FA;
}
.settings-item-last {
  border-bottom: none;
}
.settings-item-left {
  display: flex;
  align-items: center;
  flex: 1;
}
.settings-icon-wrap {
  width: 56rpx;
  height: 56rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  flex-shrink: 0;
}
.settings-icon-red {
  background: linear-gradient(135deg, #F53F3F 0%, #FF7D7D 100%);
  box-shadow: 0 4rpx 12rpx rgba(245, 63, 63, 0.15);
}
.settings-icon-orange {
  background: linear-gradient(135deg, #FF9A2E 0%, #FFCF8B 100%);
  box-shadow: 0 4rpx 12rpx rgba(255, 154, 46, 0.15);
}
.settings-icon-blue {
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  box-shadow: 0 4rpx 12rpx rgba(43, 111, 240, 0.15);
}
.settings-icon-green {
  background: linear-gradient(135deg, #00B42A 0%, #4DC580 100%);
  box-shadow: 0 4rpx 12rpx rgba(0, 180, 42, 0.15);
}
.settings-icon-purple {
  background: linear-gradient(135deg, #722ED1 0%, #B37FEB 100%);
  box-shadow: 0 4rpx 12rpx rgba(114, 46, 209, 0.15);
}
.settings-icon-cyan {
  background: linear-gradient(135deg, #0FC6C2 0%, #5CE0DB 100%);
  box-shadow: 0 4rpx 12rpx rgba(15, 198, 194, 0.15);
}
.settings-icon-grey {
  background: linear-gradient(135deg, #86909C 0%, #C9CDD4 100%);
  box-shadow: 0 4rpx 12rpx rgba(134, 144, 156, 0.15);
}
.settings-icon-text {
  font-size: 28rpx;
  color: #FFFFFF;
}
.settings-label {
  font-size: 28rpx;
  color: #1D2129;
  font-weight: 500;
}

/* 开关 */
.toggle-wrap {
  flex-shrink: 0;
}
.toggle-track {
  width: 88rpx;
  height: 48rpx;
  border-radius: 24rpx;
  background: #E5E6EB;
  position: relative;
  transition: background 0.3s ease;
  padding: 4rpx;
  box-sizing: border-box;
}
.toggle-on {
  background: #2B6FF0;
}
.toggle-thumb {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: #FFFFFF;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.15);
  transition: transform 0.3s ease;
  transform: translateX(0);
}
.toggle-on .toggle-thumb {
  transform: translateX(40rpx);
}

/* 阈值设置 */
.threshold-wrap {
  display: flex;
  align-items: center;
  gap: 6rpx;
  flex-shrink: 0;
}
.threshold-value {
  font-size: 28rpx;
  color: #2B6FF0;
  font-weight: 700;
}
.threshold-unit {
  font-size: 22rpx;
  color: #86909C;
}
.threshold-arrow {
  font-size: 24rpx;
  color: #C9CDD4;
  margin-left: 4rpx;
}

/* 右侧额外信息 */
.settings-extra {
  font-size: 24rpx;
  color: #86909C;
  margin-right: 8rpx;
}
.settings-arrow {
  font-size: 24rpx;
  color: #C9CDD4;
}

/* 版本信息 */
.version-info {
  display: flex;
  justify-content: center;
  padding: 60rpx 0 20rpx;
}
.version-text {
  font-size: 24rpx;
  color: #C9CDD4;
}

/* 底部安全区 */
.bottom-safe {
  height: 80rpx;
}

/* 弹窗 */
.popup-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
}
.popup-content {
  width: 600rpx;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 36rpx 32rpx;
  box-sizing: border-box;
}
.popup-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28rpx;
}
.popup-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1D2129;
}
.popup-close {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: #F2F3F5;
  display: flex;
  align-items: center;
  justify-content: center;
}
.popup-close-text {
  font-size: 24rpx;
  color: #86909C;
}
.popup-form {
  margin-bottom: 28rpx;
}
.popup-input {
  width: 100%;
  height: 88rpx;
  background: #F7F8FA;
  border-radius: 16rpx;
  padding: 0 24rpx;
  font-size: 30rpx;
  color: #1D2129;
  font-weight: 600;
  box-sizing: border-box;
}
.popup-input-placeholder {
  color: #C9CDD4;
  font-size: 28rpx;
  font-weight: 400;
}
.popup-hint {
  font-size: 22rpx;
  color: #86909C;
  margin-top: 12rpx;
  display: block;
}
.popup-submit {
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  border-radius: 48rpx;
  padding: 24rpx 0;
  text-align: center;
  box-shadow: 0 8rpx 32rpx rgba(43, 111, 240, 0.3);
}
.popup-submit:active {
  opacity: 0.85;
}
.popup-submit-text {
  font-size: 30rpx;
  color: #FFFFFF;
  font-weight: 700;
}
</style>
