<template>
  <view class="home-page">
    <scroll-view class="home-scroll" scroll-y :show-scrollbar="false">
      <view class="hero" :style="{ paddingTop: statusBarHeight + 'px' }">
      <image
        class="hero-art"
        src="/static/illustrations/rescue-network-hero-light-v1.webp"
        mode="aspectFill"
      />
      <view class="hero-wash"></view>

      <view class="hero-nav">
        <view class="brand-lockup">
          <view class="brand-pulse" aria-hidden="true">
            <view class="pulse-stem pulse-stem-short"></view>
            <view class="pulse-stem pulse-stem-tall"></view>
            <view class="pulse-stem pulse-stem-short"></view>
          </view>
          <view class="brand-copy">
            <text class="brand-name">脉安驰援</text>
            <text class="brand-caption">MAIAN RESPONSE</text>
          </view>
        </view>
      </view>

      <view class="hero-copy">
        <text class="hero-title">关键时刻，</text>
        <text class="hero-title hero-title-accent">更快驰援</text>
      </view>

      <view class="hero-status">
        <view class="location-line">
          <app-icon name="location-filled" :size="15" color="#2E6DD1" />
          <text>{{ locationLabel }}</text>
        </view>
        <view class="status-divider"></view>
        <view class="network-line">
          <view class="network-dot"></view>
          <text>{{ networkSummary }}</text>
        </view>
      </view>
    </view>

    <view class="action-deck">
      <view class="sos-action" @tap="openSOS">
        <view class="sos-mark"><text>SOS</text></view>
        <text class="sos-title">紧急呼救</text>
        <text class="sos-hint">立即匹配</text>
        <app-icon name="right" :size="20" color="#FFFFFF" />
      </view>

      <view class="quick-grid">
        <view class="quick-action" @tap="openMap">
          <app-icon-tile name="map-filled" tone="blue" />
          <text>急救地图</text>
        </view>
        <view class="quick-action" @tap="openDevice">
          <app-icon-tile name="plus-filled" tone="green" />
          <text>共享设备</text>
        </view>
        <view class="quick-action" @tap="openCheckup">
          <app-icon-tile name="list" tone="violet" />
          <text>体检报告</text>
        </view>
        <view class="quick-action" @tap="openScience">
          <app-icon-tile name="videocam-filled" tone="cyan" />
          <text>急救课堂</text>
        </view>
      </view>

      <view class="pulse-panel" @tap="openMap">
        <view class="pulse-heading">
          <text>急救网络脉冲</text>
          <view class="pulse-live">
            <view class="network-dot"></view>
            <text>实时</text>
          </view>
        </view>
        <view class="waveform" aria-hidden="true">
          <view
            v-for="(height, index) in waveform"
            :key="index"
            class="wave-bar"
            :class="{ 'wave-bar-hot': index >= 9 && index <= 13 }"
            :style="{ height: height + 'rpx' }"
          ></view>
          <view class="wave-sweep"></view>
        </view>
      </view>
    </view>

    <view class="home-section">
      <view class="section-heading">
        <text class="section-title">今日健康</text>
        <view class="section-link" @tap="openHealth">
          <text>查看详情</text>
          <app-icon name="right" :size="14" color="#2E6DD1" />
        </view>
      </view>

      <view class="health-summary" @tap="openHealth">
        <app-icon-tile class="health-mark" name="heart-filled" tone="coral" />
        <view class="heart-reading">
          <view class="heart-number-line">
            <text class="heart-number">{{ heartValue }}</text>
            <text class="heart-unit">BPM</text>
          </view>
          <text class="heart-state">{{ heartState }}</text>
        </view>
        <view v-if="healthBars.length" class="health-bars" aria-hidden="true">
          <view
            v-for="(height, index) in healthBars"
            :key="index"
            class="health-bar"
            :style="{ height: height + 'rpx' }"
          ></view>
        </view>
        <app-icon v-else name="right" :size="18" color="#8DA0B2" />
      </view>
    </view>

    <view class="home-section classroom-section">
      <view class="section-heading">
        <view>
          <text class="section-title">急救课堂</text>
          <text class="section-subtitle">每天掌握一个救命要点</text>
        </view>
        <view class="section-link" @tap="openScience">
          <text>全部内容</text>
          <app-icon name="right" :size="14" color="#2E6DD1" />
        </view>
      </view>

      <view class="classroom-panel">
        <view v-for="article in homeArticles.slice(0, 2)" :key="article.id" class="classroom-row" @tap="openArticle(article.id)">
          <image class="classroom-cover" :src="article.cover" mode="aspectFill" />
          <view class="classroom-copy">
            <text class="classroom-tag tag-article">知识文章 · {{ article.categoryLabel }}</text>
            <text class="classroom-title">{{ article.title }}</text>
            <text class="classroom-meta">{{ article.author }} · {{ article.viewCount }} 次阅读</text>
          </view>
          <app-icon name="right" :size="15" color="#9AA8B6" />
        </view>
        <view v-for="video in homeVideos" :key="video.id" class="classroom-row" @tap="playVideo(video.id)">
          <view class="classroom-video-cover">
            <image class="classroom-cover" :src="video.poster" mode="aspectFill" />
            <view class="row-play"><app-icon name="videocam-filled" :size="14" color="#FFFFFF" /></view>
          </view>
          <view class="classroom-copy">
            <text class="classroom-tag tag-video">视频课堂 · {{ video.duration }}</text>
            <text class="classroom-title">{{ video.title }}</text>
            <text class="classroom-meta">{{ video.source }}</text>
          </view>
          <app-icon name="right" :size="15" color="#9AA8B6" />
        </view>
      </view>

      <view class="quiz-shortcut" @tap="openQuiz">
        <view class="quiz-shortcut-mark">15题</view>
        <view class="quiz-shortcut-copy">
          <text>急救知识自测</text>
          <text>单独板块 · 即时答案解析</text>
        </view>
        <app-icon name="right" :size="16" color="#2E6DD1" />
      </view>
    </view>

    <view class="safety-note">
      <view class="safety-line"></view>
      <text>危及生命时，请立即拨打 120</text>
    </view>
      <view class="bottom-space"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import { useHealthMonitoring } from '@/composables/useHealthMonitoring'
import { listEmergencyDevices, type EmergencyDeviceResponse } from '@/api/devices'
import { FIXED_LOCATION, FIXED_LOCATION_SHORT_NAME } from '@/utils/location'
import { scienceArticles, officialFirstAidVideos } from '@/data/editorial'

const statusBarHeight = ref(uni.getSystemInfoSync().statusBarHeight || 20)
const waveform = [12, 18, 16, 24, 20, 30, 22, 18, 28, 42, 16, 58, 20, 36, 24, 18, 22, 30, 20, 16, 24, 18, 14]
const { monitoring: healthMonitoring, loadMonitoring } = useHealthMonitoring()
const homeArticles = scienceArticles.slice(0, 3)
const homeVideos = officialFirstAidVideos.slice(0, 2)

const deviceSnapshot = ref<EmergencyDeviceResponse[]>([])
const currentCoordinates = ref<{ longitude: number; latitude: number } | null>({
  longitude: FIXED_LOCATION.longitude,
  latitude: FIXED_LOCATION.latitude
})
const devicesLoaded = ref(false)

const availableDeviceCount = computed(() => (
  deviceSnapshot.value.filter(device => device.status === 'AVAILABLE').length
))

const locationLabel = FIXED_LOCATION_SHORT_NAME

const networkSummary = computed(() => (
  devicesLoaded.value ? `${availableDeviceCount.value} 台设备可用` : '正在同步设备'
))

const heartValue = computed(() => (
  healthMonitoring.value.current > 0 ? healthMonitoring.value.current : '--'
))

const heartState = computed(() => {
  if (!healthMonitoring.value.current) return '暂无心率记录'
  if (healthMonitoring.value.status === 'warning') return '最近心率偏高'
  if (healthMonitoring.value.status === 'danger') return '最近心率偏低'
  return '最近心率正常'
})

const healthBars = computed(() => {
  const values = healthMonitoring.value.todayData
    .filter((_, index) => index % 2 === 0)
    .map(point => point.value)
  if (!values.length) return []
  const min = Math.min(...values)
  const range = Math.max(...values) - min || 1
  return values.map(value => Math.round(14 + ((value - min) / range) * 34))
})

async function loadHomeData() {
  const [devicesResult] = await Promise.allSettled([
    listEmergencyDevices(),
    loadMonitoring()
  ])

  if (devicesResult.status === 'fulfilled') {
    devicesLoaded.value = true
    deviceSnapshot.value = devicesResult.value.content
  } else {
    devicesLoaded.value = false
    deviceSnapshot.value = []
  }
}

function openSOS() {
  uni.navigateTo({ url: '/pages/rescue/index' })
}

function openMap() {
  uni.switchTab({ url: '/pages/map/index' })
}

function openHealth() {
  uni.switchTab({ url: '/pages/health/index' })
}

function openDevice() {
  uni.navigateTo({ url: '/pages/device/add' })
}

function openCheckup() {
  uni.navigateTo({ url: '/pages/checkup/upload' })
}

function openScience() {
  uni.navigateTo({ url: '/pages/science/index' })
}

function openArticle(id: string) {
  uni.navigateTo({ url: `/pages/science/detail?id=${id}` })
}

function playVideo(id: string) {
  uni.navigateTo({ url: `/pages/science/video?id=${id}` })
}

function openQuiz() {
  uni.navigateTo({ url: '/pages/science/quiz' })
}

onShow(loadHomeData)
</script>

<style lang="scss" scoped>
.home-page {
  height: calc(100vh - var(--window-top, 0px) - var(--window-bottom, 0px));
  overflow: hidden;
  background: #F3F7FC;
  color: #16283C;
}

.home-scroll {
  height: 100%;
  box-sizing: border-box;
}

.hero {
  position: relative;
  min-height: 620rpx;
  overflow: hidden;
  background: #ECF5FC;
}

.hero-art,
.hero-wash {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.hero-wash {
  background: linear-gradient(
    180deg,
    rgba(248, 252, 255, 0.98) 0%,
    rgba(248, 252, 255, 0.78) 38%,
    rgba(238, 247, 253, 0.18) 70%,
    rgba(238, 247, 253, 0.64) 100%
  );
}

.hero-nav,
.hero-copy,
.hero-status {
  position: relative;
  z-index: 2;
}

.hero-nav {
  display: flex;
  align-items: center;
  height: 92rpx;
  padding: 0 32rpx;
}

.brand-lockup {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.brand-pulse {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4rpx;
  width: 52rpx;
  height: 52rpx;
  border: 1rpx solid rgba(46, 109, 209, 0.2);
  border-radius: 13rpx;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(12px);
}

.pulse-stem {
  width: 5rpx;
  border-radius: 4rpx;
  background: #2E6DD1;
}

.pulse-stem-short { height: 16rpx; }
.pulse-stem-tall { height: 31rpx; background: #B52832; }

.brand-copy {
  display: flex;
  flex-direction: column;
}

.brand-name {
  color: #18334F;
  font-size: 29rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
}

.brand-caption {
  margin-top: 2rpx;
  color: #6A8198;
  font-size: 15rpx;
  font-weight: 600;
  letter-spacing: 2rpx;
}

.hero-copy {
  width: 64%;
  padding: 44rpx 32rpx 0;
}

.hero-title {
  display: block;
  color: #152D46;
  font-size: 54rpx;
  font-weight: 750;
  line-height: 1.13;
  letter-spacing: -2rpx;
}

.hero-title-accent {
  margin-top: 4rpx;
  color: #2E6DD1;
}

.hero-status {
  position: absolute;
  right: 24rpx;
  bottom: 24rpx;
  left: 24rpx;
  display: flex;
  align-items: center;
  padding: 19rpx 22rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.8);
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 8rpx 24rpx rgba(45, 92, 132, 0.08);
  backdrop-filter: blur(16px);
}

.location-line,
.network-line {
  display: flex;
  align-items: center;
  gap: 8rpx;
  color: #38536D;
  font-size: 21rpx;
  font-weight: 600;
}

.status-divider {
  width: 1rpx;
  height: 28rpx;
  margin: 0 20rpx;
  background: #D7E2EC;
}

.network-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #23956A;
}

.action-deck {
  padding: 24rpx;
}

.sos-action {
  display: flex;
  align-items: center;
  min-height: 104rpx;
  padding: 14rpx 22rpx;
  border-radius: 18rpx;
  background: #B52832;
  box-shadow: 0 8rpx 20rpx rgba(181, 40, 50, 0.16);
}

.sos-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64rpx;
  height: 64rpx;
  margin-right: 18rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.28);
  border-radius: 50%;
  color: #FFFFFF;
  font-size: 21rpx;
  font-weight: 800;
  letter-spacing: 1rpx;
}

.sos-title {
  flex: 1;
  color: #FFFFFF;
  font-size: 29rpx;
  font-weight: 700;
}

.sos-hint {
  margin-right: 8rpx;
  color: rgba(255, 255, 255, 0.72);
  font-size: 19rpx;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  margin-top: 16rpx;
  overflow: hidden;
  border: 1rpx solid #DCE7F0;
  border-radius: 18rpx;
  background: #FFFFFF;
}

.quick-action {
  display: flex;
  align-items: center;
  gap: 14rpx;
  min-height: 92rpx;
  padding: 12rpx 22rpx;
  color: #263F57;
  font-size: 24rpx;
  font-weight: 650;
}

.quick-action:nth-child(odd) { border-right: 1rpx solid #E2EAF1; }
.quick-action:nth-child(n + 3) { border-top: 1rpx solid #E2EAF1; }

.pulse-panel {
  margin-top: 16rpx;
  padding: 18rpx 22rpx 14rpx;
  border: 1rpx solid #DCE7F0;
  border-radius: 18rpx;
  background: #F9FCFF;
}

.pulse-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #29435C;
  font-size: 22rpx;
  font-weight: 650;
}

.pulse-live {
  display: flex;
  align-items: center;
  gap: 7rpx;
  color: #367A5C;
  font-size: 17rpx;
  font-weight: 500;
}

.waveform {
  position: relative;
  display: flex;
  align-items: center;
  height: 62rpx;
  gap: 7rpx;
  margin-top: 8rpx;
  overflow: hidden;
}

.waveform::before {
  position: absolute;
  right: 0;
  left: 0;
  height: 1rpx;
  background: #D8E5F0;
  content: '';
}

.wave-bar {
  flex: 1;
  min-width: 3rpx;
  border-radius: 4rpx;
  background: #82B6E8;
}

.wave-bar-hot { background: #2E6DD1; }

.wave-sweep {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 55%;
  width: 72rpx;
  background: linear-gradient(90deg, transparent, rgba(46, 109, 209, 0.12), transparent);
  animation: sweep 2.8s linear infinite;
}

@keyframes sweep {
  from { transform: translateX(-420rpx); }
  to { transform: translateX(360rpx); }
}

.home-section {
  padding: 20rpx 24rpx 0;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.section-title {
  color: #1B334B;
  font-size: 30rpx;
  font-weight: 720;
}

.section-link {
  display: flex;
  align-items: center;
  gap: 3rpx;
  color: #2E6DD1;
  font-size: 20rpx;
}

.health-summary {
  display: flex;
  align-items: center;
  min-height: 128rpx;
  padding: 20rpx 22rpx;
  border: 1rpx solid #DCE6EF;
  border-radius: 18rpx;
  background: #FFFFFF;
}

.health-mark {
  margin-right: 18rpx;
}

.heart-reading {
  flex: 1;
  min-width: 0;
}

.heart-number-line {
  display: flex;
  align-items: baseline;
  gap: 7rpx;
}

.heart-number {
  color: #18334F;
  font-size: 39rpx;
  font-weight: 730;
  font-variant-numeric: tabular-nums;
}

.heart-unit {
  color: #8294A5;
  font-size: 17rpx;
  font-weight: 600;
}

.heart-state {
  display: block;
  margin-top: 2rpx;
  color: #6D8296;
  font-size: 19rpx;
}

.health-bars {
  display: flex;
  align-items: flex-end;
  gap: 4rpx;
  width: 124rpx;
  height: 54rpx;
}

.health-bar {
  flex: 1;
  min-width: 4rpx;
  border-radius: 4rpx;
  background: #79AFE3;
}

.classroom-section { margin-top: 18rpx; }
.section-heading > view:first-child { display: flex; flex-direction: column; gap: 4rpx; }
.section-subtitle { color: #8090A3; font-size: 20rpx; font-weight: 400; }
.classroom-panel { overflow: hidden; margin-top: 16rpx; border: 1rpx solid #DDE6EE; border-radius: 18rpx; background: #FFFFFF; }
.classroom-row { display: flex; align-items: center; gap: 15rpx; min-height: 124rpx; padding: 14rpx 17rpx; border-bottom: 1rpx solid #E9EEF3; }
.classroom-row:last-child { border-bottom: 0; }
.classroom-cover, .classroom-video-cover { flex-shrink: 0; width: 142rpx; height: 94rpx; border-radius: 10rpx; }
.classroom-video-cover { position: relative; overflow: hidden; }
.classroom-video-cover .classroom-cover { width: 100%; height: 100%; }
.row-play { position: absolute; top: 50%; left: 50%; display: flex; align-items: center; justify-content: center; width: 38rpx; height: 38rpx; border-radius: 50%; background: rgba(169, 33, 43, .92); transform: translate(-50%, -50%); }
.classroom-copy { display: flex; flex: 1; min-width: 0; flex-direction: column; gap: 5rpx; }
.classroom-tag { font-size: 17rpx; font-weight: 680; }.tag-article { color: #2E6DD1; }.tag-video { color: #A9212B; }
.classroom-title { overflow: hidden; color: #263D55; font-size: 23rpx; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.classroom-meta { overflow: hidden; color: #8492A3; font-size: 17rpx; text-overflow: ellipsis; white-space: nowrap; }
.quiz-shortcut { display: flex; align-items: center; gap: 14rpx; margin-top: 18rpx; padding: 16rpx; border: 1rpx solid #D6E2F1; border-radius: 14rpx; background: #F1F6FC; }
.quiz-shortcut-mark { display: flex; align-items: center; justify-content: center; width: 65rpx; height: 54rpx; border-radius: 9rpx; background: #2E6DD1; color: #FFFFFF; font-size: 19rpx; font-weight: 700; }
.quiz-shortcut-copy { display: flex; flex: 1; flex-direction: column; gap: 4rpx; }
.quiz-shortcut-copy text:first-child { color: #294766; font-size: 23rpx; font-weight: 700; }
.quiz-shortcut-copy text:last-child { color: #7B8B9E; font-size: 18rpx; }

.safety-note {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  padding: 42rpx 24rpx 0;
  color: #71869A;
  font-size: 19rpx;
}

.safety-line {
  width: 34rpx;
  height: 2rpx;
  background: #B52832;
}

.bottom-space {
  height: calc(150rpx + env(safe-area-inset-bottom));
}

@media (max-width: 350px) {
  .hero { min-height: 590rpx; }
  .hero-copy { padding-top: 32rpx; }
  .hero-title { font-size: 49rpx; }
  .quick-action { padding-right: 15rpx; padding-left: 15rpx; }
}
</style>
