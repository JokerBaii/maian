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

    <scroll-view scroll-x class="category-scroll">
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

    <scroll-view scroll-y class="content-list" :style="{ height: listHeight + 'px' }">
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

      <view class="quiz-entry" @tap="goQuiz">
        <view class="quiz-icon-wrap">
          <app-icon class="quiz-icon-text" name="compose" :size="26" color="#1F63D5" />
        </view>
        <view class="quiz-info">
          <text class="quiz-title">急救知识自测</text>
          <text class="quiz-desc">检验你的急救知识水平</text>
        </view>
        <app-icon class="quiz-arrow" name="right" :size="18" color="#8C98AC" />
      </view>

      <view class="list-bottom">
        <text class="list-bottom-text">— 已经到底了 —</text>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { scienceArticles, scienceCategories } from '@/data/editorial'

const statusBarHeight = ref(0)
const listHeight = ref(600)
const systemInfo = uni.getSystemInfoSync()
statusBarHeight.value = systemInfo.statusBarHeight || 20
listHeight.value = systemInfo.windowHeight - statusBarHeight.value - 44 - 52 - 48

const searchText = ref('')
const activeCategory = ref('all')
const categories = scienceCategories

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

function goContribute() {
  uni.navigateTo({ url: '/pages/science/contribute' })
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F5F7FA;
}

.nav-bar {
  background: linear-gradient(135deg, #2B6FF0, #4A9BFF);
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
  background: #F5F7FA;
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
  color: #1D2129;
}

.search-placeholder {
  color: #C9CDD4;
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
  background: #2B6FF0;
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
  color: #1D2129;
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

.tag-device { background: #E8F3FF; color: #2B6FF0; }
.tag-emergency { background: #FFECE8; color: #F53F3F; }
.tag-health { background: #E8FFEA; color: #00B42A; }
.tag-exercise { background: #FFF7E8; color: #FF7D00; }

.meta-right {
  display: flex;
  gap: 12rpx;
}

.meta-stat {
  font-size: 20rpx;
  color: #86909C;
}

.quiz-entry {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #2B6FF0, #4A9BFF);
  border-radius: 20rpx;
  padding: 28rpx 24rpx;
  margin-bottom: 20rpx;
}

.quiz-icon-wrap {
  width: 72rpx;
  height: 72rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
}

.quiz-icon-text {
  font-size: 36rpx;
}

.quiz-info {
  flex: 1;
}

.quiz-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #fff;
}

.quiz-desc {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 4rpx;
}

.quiz-arrow {
  font-size: 36rpx;
  color: rgba(255, 255, 255, 0.8);
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
