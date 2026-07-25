<template>
  <view class="page">
    <!-- 封面/视频区域 -->
    <view class="cover-area">
      <!-- 视频播放器 -->
      <view v-if="article.media && article.media.type === 'video'" class="video-area">
        <video
          id="science-video"
          class="cover-video"
          :src="article.media.url"
          :poster="article.media.poster || article.cover"
          controls
          autoplay
          muted
          show-center-play-btn
          enable-progress-gesture
          object-fit="contain"
          webkit-playsinline
          playsinline
          x5-playsinline
        ></video>
        <view class="video-badge">
          <text class="video-badge-icon">▶</text>
          <text class="video-badge-text">视频教程</text>
        </view>
      </view>
      <!-- 封面图（无视频时） -->
      <view v-else class="cover-image-area">
        <image class="cover-image" :src="article.cover" mode="aspectFill" />
        <view class="cover-gradient"></view>
      </view>
      <view class="cover-nav" :style="{ paddingTop: statusBarHeight + 'px' }">
        <view class="nav-bar-content">
          <view class="nav-back" @tap="goBack">
            <text class="back-icon">&#x2190;</text>
          </view>
          <view class="nav-placeholder"></view>
        </view>
      </view>
      <view v-if="!(article.media && article.media.type === 'video')" class="cover-category">
        <view class="category-tag">
          <text class="category-text">{{ article.categoryLabel }}</text>
        </view>
      </view>
    </view>

    <scroll-view
      class="scroll-content"
      scroll-y
      :style="{ marginTop: coverHeight + 'px' }"
    >
      <!-- 文章信息 -->
      <view class="article-header">
        <text class="article-title">{{ article.title }}</text>
        <view class="author-row">
          <image class="author-avatar" :src="article.authorAvatar" mode="aspectFill" />
          <view class="author-info">
            <text class="author-name">{{ article.author }}</text>
            <view class="meta-row">
              <text class="meta-text">{{ article.publishTime }}</text>
              <text class="meta-dot">·</text>
              <text class="meta-text">{{ formatViewCount(article.viewCount) }} 阅读</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 图片画廊 -->
      <view v-if="article.media && article.media.images && article.media.images.length > 0" class="media-gallery">
        <view class="gallery-header">
          <view class="gallery-title-bar"></view>
          <text class="gallery-title">相关图示</text>
          <text class="gallery-count">{{ article.media.images.length }}张</text>
        </view>
        <scroll-view scroll-x class="gallery-scroll">
          <view class="gallery-list">
            <image
              v-for="(img, idx) in article.media.images"
              :key="idx"
              class="gallery-img"
              :src="img"
              mode="aspectFill"
              @tap="previewImage(idx)"
            />
          </view>
        </scroll-view>
      </view>

      <!-- 文章正文 -->
      <view class="article-body">
        <view
          v-for="(paragraph, idx) in contentParagraphs"
          :key="idx"
          class="paragraph-block"
        >
          <text class="paragraph-text">{{ paragraph }}</text>
        </view>
      </view>

      <!-- 底部操作栏占位 -->
      <view class="action-bar-placeholder"></view>
    </scroll-view>

    <!-- 底部操作栏 -->
    <view class="action-bar">
      <view class="action-bar-inner">
        <view class="action-item" @tap="toggleLike">
          <view class="action-icon-wrap" :class="{ 'action-icon-active': isLiked }">
            <text class="action-icon">{{ isLiked ? '&#x2764;' : '&#x2661;' }}</text>
          </view>
          <text class="action-label" :class="{ 'action-label-active': isLiked }">
            {{ isLiked ? article.likeCount + 1 : article.likeCount }}
          </text>
        </view>
        <view class="action-item" @tap="toggleCollect">
          <view class="action-icon-wrap" :class="{ 'action-icon-active': isCollected }">
            <text class="action-icon">{{ isCollected ? '&#x2605;' : '&#x2606;' }}</text>
          </view>
          <text class="action-label" :class="{ 'action-label-active': isCollected }">
            {{ isCollected ? article.collectCount + 1 : article.collectCount }}
          </text>
        </view>
        <view class="action-item" @tap="handleShare">
          <view class="action-icon-wrap">
            <text class="action-icon">&#x2197;</text>
          </view>
          <text class="action-label">分享</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { mockScienceContents } from '@/mock/data'

// 系统信息
const statusBarHeight = ref(0)
const systemInfo = uni.getSystemInfoSync()
statusBarHeight.value = systemInfo.statusBarHeight || 20

// 获取路由参数
const articleId = ref('S001')
const pages = getCurrentPages()
const currentPage = pages[pages.length - 1] as any
if (currentPage?.options?.id) {
  articleId.value = currentPage.options.id
}

// 封面高度（导航栏 + 封面图区域）
const coverHeight = computed(() => {
  return statusBarHeight.value + 44 + 320
})

// 文章数据 - 根据URL参数查找
const article = computed(() => {
  const found = mockScienceContents.find(c => c.id === articleId.value)
  return found || mockScienceContents[0]
})

// 点赞/收藏状态
const isLiked = ref(article.value.isLiked)
const isCollected = ref(article.value.isCollected)

// 内容段落拆分
const contentParagraphs = computed(() => {
  const content = article.value.content || ''
  return content.split('\n\n').filter(p => p.trim())
})

// 视频自动播放 - 使用uni-app VideoContext
let videoContext: any = null

// 页面加载后触发视频自动播放
onMounted(() => {
  if (article.value.media && article.value.media.type === 'video') {
    setTimeout(() => {
      videoContext = uni.createVideoContext('science-video')
      if (videoContext) {
        videoContext.play()
      }
    }, 800)
  }
})

// 格式化阅读数
function formatViewCount(count: number) {
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + '万'
  }
  return count.toString()
}

// 切换点赞
function toggleLike() {
  isLiked.value = !isLiked.value
  uni.showToast({
    title: isLiked.value ? '已点赞' : '已取消点赞',
    icon: 'none',
    duration: 1000
  })
}

// 切换收藏
function toggleCollect() {
  isCollected.value = !isCollected.value
  uni.showToast({
    title: isCollected.value ? '已收藏' : '已取消收藏',
    icon: 'none',
    duration: 1000
  })
}

// 分享
function handleShare() {
  uni.showToast({
    title: '分享功能开发中',
    icon: 'none',
    duration: 1000
  })
}

// 预览图片
function previewImage(idx: number) {
  if (article.value.media && article.value.media.images) {
    uni.previewImage({
      urls: article.value.media.images,
      current: article.value.media.images[idx]
    })
  }
}

// 返回
function goBack() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F5F7FA;
}

/* 封面图区域 */
.cover-area {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 500rpx;
  z-index: 10;
}
.cover-image-area {
  width: 100%;
  height: 100%;
  position: relative;
}
.cover-image {
  width: 100%;
  height: 100%;
}
.video-area {
  width: 100%;
  height: 100%;
  position: relative;
  background: #000;
}
.cover-video {
  width: 100%;
  height: 100%;
}
.video-badge {
  position: absolute;
  top: 100rpx;
  right: 24rpx;
  display: flex;
  align-items: center;
  gap: 6rpx;
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  background: rgba(0, 0, 0, 0.5);
  z-index: 20;
}
.video-badge-icon {
  font-size: 22rpx;
  color: #FFFFFF;
}
.video-badge-text {
  font-size: 22rpx;
  color: #FFFFFF;
  font-weight: 500;
}
.cover-gradient {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 280rpx;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0) 0%, rgba(0, 0, 0, 0.6) 100%);
  z-index: 2;
}
.cover-nav {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 20;
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
  background: rgba(0, 0, 0, 0.2);
  border-radius: 50%;
  backdrop-filter: blur(8px);
}
.back-icon {
  font-size: 36rpx;
  color: #FFFFFF;
  font-weight: 700;
}
.nav-placeholder {
  width: 64rpx;
}
.cover-category {
  position: absolute;
  bottom: 32rpx;
  left: 32rpx;
  z-index: 20;
}
.category-tag {
  display: inline-flex;
  padding: 8rpx 24rpx;
  border-radius: 24rpx;
  background: rgba(43, 111, 240, 0.85);
  backdrop-filter: blur(8px);
}
.category-text {
  font-size: 22rpx;
  font-weight: 600;
  color: #FFFFFF;
}

/* 滚动内容 */
.scroll-content {
  position: relative;
  z-index: 15;
  min-height: 100vh;
  background: #FFFFFF;
  border-radius: 32rpx 32rpx 0 0;
  box-shadow: 0 -8rpx 32rpx rgba(0, 0, 0, 0.08);
}

/* 文章头部 */
.article-header {
  padding: 40rpx 32rpx 0;
}
.article-title {
  font-size: 40rpx;
  font-weight: 800;
  color: #1D2129;
  line-height: 1.4;
  letter-spacing: 1rpx;
}
.author-row {
  display: flex;
  align-items: center;
  margin-top: 28rpx;
  padding-bottom: 28rpx;
  border-bottom: 1rpx solid #F2F3F5;
}
.author-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  border: 3rpx solid #E8F0FE;
  flex-shrink: 0;
}
.author-info {
  margin-left: 16rpx;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.author-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1D2129;
}
.meta-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.meta-text {
  font-size: 22rpx;
  color: #86909C;
}
.meta-dot {
  font-size: 22rpx;
  color: #C9CDD4;
}

/* 文章正文 */
.article-body {
  padding: 32rpx;
}
.paragraph-block {
  margin-bottom: 28rpx;
}
.paragraph-block:last-child {
  margin-bottom: 0;
}
.paragraph-text {
  font-size: 30rpx;
  color: #3D4A5C;
  line-height: 1.8;
  letter-spacing: 1rpx;
}

/* 图片画廊 */
.media-gallery {
  padding: 24rpx 32rpx 0;
}
.gallery-header {
  display: flex;
  align-items: center;
  margin-bottom: 16rpx;
}
.gallery-title-bar {
  width: 6rpx;
  height: 28rpx;
  border-radius: 3rpx;
  background: #2B6FF0;
  margin-right: 12rpx;
}
.gallery-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #1D2129;
}
.gallery-count {
  font-size: 22rpx;
  color: #86909C;
  margin-left: 12rpx;
}
.gallery-scroll {
  white-space: nowrap;
}
.gallery-list {
  display: flex;
  gap: 16rpx;
}
.gallery-img {
  width: 320rpx;
  height: 220rpx;
  border-radius: 16rpx;
  flex-shrink: 0;
}

/* 底部操作栏占位 */
.action-bar-placeholder {
  height: 140rpx;
}

/* 底部操作栏 */
.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: #FFFFFF;
  border-top: 1rpx solid #F2F3F5;
  padding-bottom: env(safe-area-inset-bottom);
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.04);
}
.action-bar-inner {
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 108rpx;
  padding: 0 48rpx;
}
.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
  padding: 8rpx 24rpx;
  transition: all 0.2s ease;
}
.action-item:active {
  transform: scale(0.92);
}
.action-icon-wrap {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: #F7F8FA;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}
.action-icon-active {
  background: rgba(245, 63, 63, 0.1);
}
.action-icon {
  font-size: 32rpx;
  color: #86909C;
  transition: color 0.3s ease;
}
.action-icon-active .action-icon {
  color: #F53F3F;
}
.action-label {
  font-size: 20rpx;
  color: #86909C;
  font-weight: 500;
  transition: color 0.3s ease;
}
.action-label-active {
  color: #F53F3F;
}
</style>
