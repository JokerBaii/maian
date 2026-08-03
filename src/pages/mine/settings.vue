<template>
  <view class="page">
    <view class="settings-scroll">
      <view class="settings-section">
        <text class="section-title">通知设置</text>
        <view class="settings-list">
          <view class="settings-item">
            <view class="settings-item-left">
              <app-icon-tile class="settings-icon" name="rescue-notice" tone="coral" />
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
              <app-icon-tile class="settings-icon" name="health-alert" tone="coral" />
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
              <app-icon-tile class="settings-icon" name="science-update" tone="cyan" />
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

      <view class="settings-section">
        <text class="section-title">隐私设置</text>
        <view class="settings-list">
          <view class="settings-item">
            <view class="settings-item-left">
              <app-icon-tile class="settings-icon" name="location-filled" tone="blue" />
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
              <app-icon-tile class="settings-icon" name="bars" tone="cyan" />
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

      <view class="settings-section">
        <text class="section-title">心率预警阈值</text>
        <view class="settings-list">
          <view class="settings-item">
            <view class="settings-item-left">
              <app-icon-tile class="settings-icon" name="arrow-up" tone="coral" />
              <text class="settings-label">最高心率</text>
            </view>
            <view class="threshold-wrap" @tap="editThreshold('max')">
              <text class="threshold-value">{{ settings.maxHeartRate }}</text>
              <text class="threshold-unit">BPM</text>
              <app-icon name="right" :size="14" color="#A8B2C1" />
            </view>
          </view>
          <view class="settings-item settings-item-last">
            <view class="settings-item-left">
              <app-icon-tile class="settings-icon" name="arrow-down" tone="blue" />
              <text class="settings-label">最低心率</text>
            </view>
            <view class="threshold-wrap" @tap="editThreshold('min')">
              <text class="threshold-value">{{ settings.minHeartRate }}</text>
              <text class="threshold-unit">BPM</text>
              <app-icon name="right" :size="14" color="#A8B2C1" />
            </view>
          </view>
        </view>
      </view>

      <view class="settings-section">
        <text class="section-title">其他</text>
        <view class="settings-list">
          <view class="settings-item" @tap="clearCache">
            <view class="settings-item-left">
              <app-icon-tile class="settings-icon" name="trash-filled" tone="slate" />
              <text class="settings-label">清除缓存</text>
            </view>
            <text class="settings-extra">{{ cacheSizeText }}</text>
          </view>
          <view class="settings-item" @tap="goAbout">
            <view class="settings-item-left">
              <app-icon-tile class="settings-icon" name="info-filled" tone="blue" />
              <text class="settings-label">关于我们</text>
            </view>
            <app-icon name="right" :size="14" color="#A8B2C1" />
          </view>
          <view class="settings-item" @tap="goLegal('privacy')">
            <view class="settings-item-left">
              <app-icon-tile class="settings-icon" name="auth-filled" tone="blue" />
              <text class="settings-label">隐私政策</text>
            </view>
            <app-icon name="right" :size="14" color="#A8B2C1" />
          </view>
          <view class="settings-item" @tap="goLegal('terms')">
            <view class="settings-item-left">
              <app-icon-tile class="settings-icon" name="list" tone="violet" />
              <text class="settings-label">用户协议</text>
            </view>
            <app-icon name="right" :size="14" color="#A8B2C1" />
          </view>
          <view class="settings-item settings-item-last" @tap="goLegal('medical')">
            <view class="settings-item-left">
              <app-icon-tile class="settings-icon" name="health-alert" tone="coral" />
              <text class="settings-label">医疗免责声明</text>
            </view>
            <app-icon name="right" :size="14" color="#A8B2C1" />
          </view>
        </view>
      </view>

      <view class="version-info">
        <text class="version-text">脉安驰援 v1.0.0</text>
      </view>

      <view class="bottom-safe"></view>
    </view>

    <view v-if="thresholdPopupVisible" class="popup-mask" @tap="thresholdPopupVisible = false">
      <view class="popup-content" @tap.stop>
        <view class="popup-header">
          <text class="popup-title">{{ editingMax ? '设置最高心率' : '设置最低心率' }}</text>
          <view class="popup-close" @tap="thresholdPopupVisible = false">
            <app-icon name="closeempty" :size="16" color="#748198" />
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
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import { getUserSettings, updateUserSettings } from '@/api/user'

const SETTINGS_STORAGE_KEY = 'maian:user-settings'
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
const cacheSizeKb = ref(0)
const settingsReady = ref(false)
let saveTimer: ReturnType<typeof setTimeout> | null = null
const cacheSizeText = computed(() => (
  cacheSizeKb.value >= 1024
    ? `${(cacheSizeKb.value / 1024).toFixed(1)}MB`
    : `${cacheSizeKb.value}KB`
))

function applySettings(value: typeof settings) {
  settings.rescuePush = value.rescuePush
  settings.healthAlert = value.healthAlert
  settings.scienceUpdate = value.scienceUpdate
  settings.locationShare = value.locationShare
  settings.healthDataShare = value.healthDataShare
  settings.maxHeartRate = value.maxHeartRate
  settings.minHeartRate = value.minHeartRate
}

onMounted(async () => {
  const saved = uni.getStorageSync(SETTINGS_STORAGE_KEY)
  if (saved && typeof saved === 'object') {
    Object.assign(settings, saved)
  }
  try {
    const remote = await getUserSettings()
    applySettings(remote)
  } catch {
    uni.showToast({ title: '设置同步失败，已使用本机设置', icon: 'none' })
  } finally {
    settingsReady.value = true
  }
  cacheSizeKb.value = uni.getStorageInfoSync().currentSize || 0
})

watch(settings, (value) => {
  uni.setStorageSync(SETTINGS_STORAGE_KEY, { ...value })
  if (!settingsReady.value) return
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(async () => {
    try {
      const remote = await updateUserSettings({ ...settings })
      applySettings(remote)
    } catch (error: any) {
      uni.showToast({ title: error?.message || '设置保存失败', icon: 'none' })
    }
  }, 350)
}, { deep: true })

onUnmounted(() => {
  if (saveTimer) clearTimeout(saveTimer)
})

function editThreshold(type: string) {
  editingMax.value = type === 'max'
  thresholdInput.value = type === 'max' ? String(settings.maxHeartRate) : String(settings.minHeartRate)
  thresholdPopupVisible.value = true
}

function saveThreshold() {
  const val = parseInt(thresholdInput.value)
  const validRange = editingMax.value
    ? val >= 80 && val <= 240 && val > settings.minHeartRate
    : val >= 30 && val <= 120 && val < settings.maxHeartRate
  if (!validRange) {
    uni.showToast({
      title: editingMax.value ? '最高心率需为 80–240' : '最低心率需为 30–120',
      icon: 'none'
    })
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
    confirmColor: '#2E6DD1',
    success: (res) => {
      if (res.confirm) {
        try {
          uni.clearStorageSync()
          cacheSizeKb.value = 0
          uni.showToast({ title: '缓存已清除', icon: 'success' })
        } catch {
          uni.showToast({ title: '缓存清理失败', icon: 'none' })
        }
      }
    }
  })
}

function goAbout() {
  uni.showModal({
    title: '脉安驰援 v1.0.0',
    content: '共享急救设备、紧急救援协同与健康管理应用。',
    showCancel: false,
    confirmText: '知道了'
  })
}

function goLegal(type: 'privacy' | 'terms' | 'medical') {
  uni.navigateTo({ url: `/pages/legal/index?type=${type}` })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F3F7FA;
}

.settings-scroll {
  min-height: 100vh;
  box-sizing: border-box;
}

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
  border: 1rpx solid var(--network-line);
  border-radius: var(--network-radius-section);
  overflow: hidden;
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
.settings-icon {
  margin-right: 18rpx;
}
.settings-label {
  font-size: 28rpx;
  color: #20364D;
  font-weight: 500;
}

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
  background: #2E6DD1;
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

.threshold-wrap {
  display: flex;
  align-items: center;
  gap: 6rpx;
  flex-shrink: 0;
}
.threshold-value {
  font-size: 28rpx;
  color: #2E6DD1;
  font-weight: 700;
}
.threshold-unit {
  font-size: 22rpx;
  color: #86909C;
}
.settings-extra {
  font-size: 24rpx;
  color: #86909C;
  margin-right: 8rpx;
}
.version-info {
  display: flex;
  justify-content: center;
  padding: 60rpx 0 20rpx;
}
.version-text {
  font-size: 24rpx;
  color: #C9CDD4;
}

.bottom-safe {
  height: 80rpx;
}

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
  color: #20364D;
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
  color: #20364D;
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
  background: linear-gradient(135deg, #2E6DD1 0%, #2E6DD1 100%);
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
