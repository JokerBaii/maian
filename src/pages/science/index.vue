<template>
  <view class="page">
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-content">
        <view class="nav-back" @tap="goBack">
          <app-icon class="back-icon" name="back" :size="22" color="#FFFFFF" />
        </view>
        <text class="nav-title">急救与健康科普</text>
        <view class="nav-right" @tap="goContribute">
          <app-icon class="nav-action" name="compose" :size="21" color="#FFFFFF" />
        </view>
      </view>
    </view>

    <view class="search-section">
      <view class="search-bar">
        <app-icon class="search-icon" name="search" :size="18" color="#56627A" />
        <input
          class="search-input"
          v-model="searchText"
          placeholder="搜索科普内容"
          placeholder-class="search-placeholder"
        />
      </view>
    </view>

    <view class="content-modes">
      <view
        v-for="mode in contentModes"
        :key="mode.key"
        class="mode-item"
        :class="{ 'mode-active': activeMode === mode.key }"
        @tap="activeMode = mode.key"
      >
        <app-icon :name="mode.icon" :size="17" :color="activeMode === mode.key ? '#2E6DD1' : '#718197'" />
        <text>{{ mode.label }}</text>
      </view>
    </view>

    <scroll-view v-if="activeMode === 'articles'" scroll-x class="category-scroll">
      <view class="category-list">
        <view
          v-for="cat in categories"
          :key="cat.key"
          class="category-item"
          :class="{ 'category-active': activeCategory === cat.key }"
          @tap="activeCategory = cat.key"
        >
          <text class="category-text">{{ cat.label }}</text>
        </view>
      </view>
    </scroll-view>

    <view v-if="activeMode === 'articles'" class="content-list">
      <view
        v-for="item in filteredContents"
        :key="item.id"
        class="content-card"
        @tap="goDetail(item)"
      >
        <view class="card-cover">
          <image class="card-cover-image" :src="item.cover" mode="aspectFill" />
        </view>
        <view class="card-body">
          <text class="card-title">{{ item.title }}</text>
          <text class="card-summary">{{ item.summary }}</text>
          <view class="card-meta">
            <view class="meta-left">
              <text class="meta-author">{{ item.author }}</text>
              <text class="meta-dot">·</text>
              <text class="meta-tag" :class="'tag-' + item.category">{{ item.categoryLabel }}</text>
            </view>
            <view class="meta-right">
              <view class="meta-stat">
                <app-icon name="hand-up" :size="13" color="#8994A8" />
                <text>{{ formatCount(item.likeCount) }}</text>
              </view>
              <view class="meta-stat">
                <app-icon name="eye" :size="13" color="#8994A8" />
                <text>{{ formatCount(item.viewCount) }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="list-bottom">
        <text class="list-bottom-text">— 已经到底了 —</text>
      </view>
    </view>

    <view v-else-if="activeMode === 'videos'" class="video-section">
      <view class="section-copy">
        <text class="section-title">急救视频课</text>
        <text class="section-desc">权威课程，按急救流程逐步学习</text>
      </view>
      <view v-for="video in officialFirstAidVideos" :key="video.id" class="video-card" @tap="playVideo(video.id)">
        <view class="video-poster">
          <image :src="video.poster" mode="aspectFill" />
          <view class="play-button"><app-icon name="videocam-filled" :size="20" color="#FFFFFF" /></view>
          <text class="duration-chip">{{ video.duration }}</text>
        </view>
        <view class="video-copy">
          <text class="video-title">{{ video.title }}</text>
          <text class="video-summary">{{ video.summary }}</text>
          <text class="video-source">{{ video.source }}</text>
        </view>
      </view>
    </view>

    <view v-else class="quiz-board">
      <view class="quiz-board-head">
        <view class="quiz-mark"><text>15</text><text>题</text></view>
        <view class="quiz-board-copy">
          <text class="quiz-board-title">急救知识自测</text>
          <text class="quiz-board-desc">AED、心肺复苏、创伤处置与常见急症</text>
        </view>
      </view>
      <view class="quiz-facts">
        <view><text class="fact-value">10</text><text class="fact-label">分钟</text></view>
        <view><text class="fact-value">80</text><text class="fact-label">分合格</text></view>
        <view><text class="fact-value">即时</text><text class="fact-label">答案解析</text></view>
      </view>
      <view class="quiz-start" @tap="goQuiz">
        <text>开始自测</text><app-icon name="right" :size="17" color="#FFFFFF" />
      </view>
      <text class="quiz-note">自测用于知识巩固，不能替代线下急救培训。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { scienceArticles, scienceCategories, officialFirstAidVideos } from '@/data/editorial'

const statusBarHeight = ref(0)
const systemInfo = uni.getSystemInfoSync()
statusBarHeight.value = systemInfo.statusBarHeight || 20

const searchText = ref('')
const activeCategory = ref('all')
const activeMode = ref<'articles' | 'videos' | 'quiz'>('articles')
const categories = scienceCategories
const contentModes = [
  { key: 'articles', label: '知识文章', icon: 'list' },
  { key: 'videos', label: '视频课堂', icon: 'videocam-filled' },
  { key: 'quiz', label: '急救自测', icon: 'science-update' }
] as const

const filteredContents = computed(() => {
  let list = scienceArticles
  if (activeCategory.value !== 'all') {
    list = list.filter(c => c.category === activeCategory.value)
  }
  if (searchText.value) {
    const kw = searchText.value.toLowerCase()
    list = list.filter(c =>
      c.title.toLowerCase().includes(kw) ||
      c.summary.toLowerCase().includes(kw)
    )
  }
  return list
})

function formatCount(count: number) {
  if (count >= 10000) return (count / 10000).toFixed(1) + 'w'
  if (count >= 1000) return (count / 1000).toFixed(1) + 'k'
  return count.toString()
}

function goBack() {
  uni.navigateBack()
}

function goDetail(item: any) {
  uni.navigateTo({ url: '/pages/science/detail?id=' + item.id })
}

function goQuiz() {
  uni.navigateTo({ url: '/pages/science/quiz' })
}

function playVideo(id: string) {
  uni.navigateTo({ url: `/pages/science/video?id=${id}` })
}

function goContribute() {
  uni.navigateTo({ url: '/pages/science/contribute' })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F3F7FA;
}

.nav-bar {
  background: linear-gradient(135deg, #2E6DD1, #4A9BFF);
}

.nav-bar-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 88rpx;
  padding: 0 24rpx;
}

.nav-back {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-icon {
  font-size: 36rpx;
  color: #fff;
}

.nav-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #fff;
}

.nav-right {
  width: 60rpx;
  height: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-action {
  font-size: 32rpx;
}

.search-section {
  padding: 20rpx 24rpx;
  background: #fff;
}

.search-bar {
  display: flex;
  align-items: center;
  background: #F3F7FA;
  border-radius: 36rpx;
  padding: 16rpx 24rpx;
}

.search-icon {
  font-size: 28rpx;
  margin-right: 12rpx;
}

.search-input {
  flex: 1;
  font-size: 26rpx;
  color: #20364D;
}

.search-placeholder {
  color: #C9CDD4;
}

.content-modes {
  display: flex;
  gap: 8rpx;
  padding: 0 24rpx 18rpx;
  background: #FFFFFF;
}
.mode-item {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  padding: 16rpx 8rpx;
  border: 1rpx solid transparent;
  border-radius: 12rpx;
  background: #F3F6F9;
  color: #718197;
  font-size: 23rpx;
  font-weight: 550;
}
.mode-active {
  border-color: #C8D9F2;
  background: #EAF1FD;
  color: #2E6DD1;
  font-weight: 700;
}

.category-scroll {
  width: 100%;
  box-sizing: border-box;
  white-space: nowrap;
  background: #fff;
  padding: 0 24rpx 20rpx;
}

.category-list {
  display: flex;
  gap: 16rpx;
}

.category-item {
  padding: 10rpx 28rpx;
  border-radius: 28rpx;
  background: #F2F3F5;
}

.category-active {
  background: #2E6DD1;
}

.category-text {
  font-size: 24rpx;
  color: #4E5969;
}

.category-active .category-text {
  color: #fff;
}

.content-list {
  width: 100%;
  box-sizing: border-box;
  padding: 20rpx 24rpx;
}

.video-section,
.quiz-board {
  margin: 20rpx 24rpx 0;
}
.section-copy {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 4rpx 2rpx 16rpx;
}
.section-title {
  color: #20364D;
  font-size: 29rpx;
  font-weight: 700;
}
.section-desc {
  color: #8190A2;
  font-size: 20rpx;
}
.video-card {
  display: flex;
  gap: 18rpx;
  margin-bottom: 16rpx;
  padding: 18rpx;
  border: 1rpx solid #DEE6EE;
  border-radius: 18rpx;
  background: #FFFFFF;
}
.video-poster {
  position: relative;
  flex-shrink: 0;
  width: 214rpx;
  height: 142rpx;
  overflow: hidden;
  border-radius: 12rpx;
  background: #DDE7F1;
}
.video-poster image { width: 100%; height: 100%; }
.video-poster::after { position: absolute; inset: 0; content: ''; background: linear-gradient(90deg, rgba(15, 34, 54, .18), transparent); }
.play-button { position: absolute; z-index: 2; top: 50%; left: 50%; display: flex; align-items: center; justify-content: center; width: 48rpx; height: 48rpx; border-radius: 50%; background: rgba(169, 33, 43, .92); transform: translate(-50%, -50%); }
.duration-chip { position: absolute; z-index: 2; right: 8rpx; bottom: 8rpx; padding: 3rpx 7rpx; border-radius: 6rpx; background: rgba(18, 35, 53, .76); color: #FFFFFF; font-size: 17rpx; }
.video-copy { display: flex; flex: 1; min-width: 0; flex-direction: column; }
.video-title { color: #20364D; font-size: 26rpx; font-weight: 700; line-height: 1.35; }
.video-summary { display: -webkit-box; overflow: hidden; margin-top: 7rpx; color: #718197; font-size: 20rpx; line-height: 1.45; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.video-source { overflow: hidden; margin-top: auto; color: #2E6DD1; font-size: 18rpx; text-overflow: ellipsis; white-space: nowrap; }

.quiz-board { padding: 28rpx; border: 1rpx solid #DCE5EE; border-radius: 20rpx; background: #FFFFFF; }
.quiz-board-head { display: flex; align-items: center; gap: 18rpx; }
.quiz-mark { display: flex; flex-shrink: 0; align-items: baseline; justify-content: center; width: 88rpx; height: 88rpx; border-radius: 18rpx; background: #EAF1FD; color: #2E6DD1; }
.quiz-mark text:first-child { align-self: center; font-size: 36rpx; font-weight: 800; }.quiz-mark text:last-child { align-self: center; margin-left: 2rpx; font-size: 18rpx; }
.quiz-board-copy { display: flex; flex-direction: column; gap: 7rpx; }
.quiz-board-title { color: #20364D; font-size: 31rpx; font-weight: 750; }
.quiz-board-desc { color: #718197; font-size: 21rpx; line-height: 1.45; }
.quiz-facts { display: flex; margin: 27rpx 0; padding: 20rpx 0; border-top: 1rpx solid #EDF1F5; border-bottom: 1rpx solid #EDF1F5; }
.quiz-facts > view { display: flex; flex: 1; flex-direction: column; align-items: center; gap: 4rpx; border-right: 1rpx solid #E6EBF0; }.quiz-facts > view:last-child { border-right: 0; }
.fact-value { color: #244C7C; font-size: 27rpx; font-weight: 750; }.fact-label { color: #8694A6; font-size: 18rpx; }
.quiz-start { display: flex; align-items: center; justify-content: center; gap: 8rpx; height: 82rpx; border-radius: 12rpx; background: #2E6DD1; color: #FFFFFF; font-size: 26rpx; font-weight: 700; }
.quiz-note { display: block; margin-top: 15rpx; color: #8996A6; text-align: center; font-size: 18rpx; }

.content-card {
  display: flex;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.card-cover {
  width: 160rpx;
  height: 160rpx;
  border-radius: 16rpx;
  overflow: hidden;
  margin-right: 24rpx;
  flex-shrink: 0;
  background: #EDF3FA;
}

.card-cover-image {
  width: 100%;
  height: 100%;
}

.card-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
}

.card-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #20364D;
  lines: 2;
  text-overflow: ellipsis;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-summary {
  font-size: 24rpx;
  color: #86909C;
  margin-top: 8rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12rpx;
}

.meta-left {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.meta-author {
  font-size: 22rpx;
  color: #86909C;
}

.meta-dot {
  font-size: 22rpx;
  color: #C9CDD4;
}

.meta-tag {
  font-size: 20rpx;
  padding: 2rpx 12rpx;
  border-radius: 8rpx;
}

.tag-device { background: #E8F3FF; color: #2E6DD1; }
.tag-emergency { background: #FFECE8; color: #C93D46; }
.tag-health { background: #E8FFEA; color: #23956A; }
.tag-exercise { background: #FFF7E8; color: #FF7D00; }

.meta-right {
  display: flex;
  gap: 12rpx;
}

.meta-stat {
  font-size: 20rpx;
  color: #86909C;
}

.list-bottom {
  text-align: center;
  padding: 32rpx;
}

.list-bottom-text {
  font-size: 24rpx;
  color: #C9CDD4;
}
</style>
