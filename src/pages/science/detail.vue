<template>
  <view class="page">
    <view class="page-scroll">
    <view class="cover-area">
      <view class="cover-image-area">
        <image
          class="cover-image"
          :src="article.cover.startsWith('/uploads/') ? resolveApiUrl(article.cover) : article.cover"
          mode="aspectFill"
        />
        <view class="cover-gradient"></view>
      </view>
      <view class="cover-category">
        <view class="category-tag">
          <text class="category-text">{{ article.categoryLabel }}</text>
        </view>
      </view>
    </view>

    <view class="scroll-content">
      <view class="article-header">
        <text class="article-title">{{ article.title }}</text>
        <view class="author-row">
          <app-icon-tile name="author" tone="cyan" />
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

      <view class="article-body">
        <view
          v-for="(paragraph, idx) in contentParagraphs"
          :key="idx"
          class="paragraph-block"
        >
          <text class="paragraph-text">{{ paragraph }}</text>
        </view>
      </view>

    </view>
    </view>

    <view class="action-bar">
      <view class="action-bar-inner">
        <view class="action-item" @tap="toggleLike">
          <view class="action-icon-wrap" :class="{ 'action-icon-active': isLiked }">
            <app-icon
              class="action-icon"
              :name="isLiked ? 'heart-filled' : 'heart'"
              :size="21"
              :color="isLiked ? '#D64B4B' : '#738099'"
            />
          </view>
          <text class="action-label" :class="{ 'action-label-active': isLiked }">
            {{ isLiked ? article.likeCount + 1 : article.likeCount }}
          </text>
        </view>
        <view class="action-item" @tap="toggleCollect">
          <view class="action-icon-wrap" :class="{ 'action-icon-active': isCollected }">
            <app-icon
              class="action-icon"
              :name="isCollected ? 'star-filled' : 'star'"
              :size="21"
              :color="isCollected ? '#D18A2A' : '#738099'"
            />
          </view>
          <text class="action-label" :class="{ 'action-label-active': isCollected }">
            {{ isCollected ? article.collectCount + 1 : article.collectCount }}
          </text>
        </view>
        <view class="action-item" @tap="handleShare">
          <view class="action-icon-wrap">
            <app-icon class="action-icon" name="redo-filled" :size="21" color="#738099" />
          </view>
          <text class="action-label">分享</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { scienceArticles } from '@/data/editorial'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import { resolveApiUrl } from '@/api/http'
import {
  getScienceArticleInteraction,
  listApprovedScienceSubmissions,
  updateScienceArticleInteraction
} from '@/api/science'

const articleId = ref('S001')
const pages = getCurrentPages()
const currentPage = pages[pages.length - 1] as any
if (currentPage?.options?.id) {
  articleId.value = currentPage.options.id
}

/** 投稿类文章（用户投稿审核通过后上架，id 形如 submission-{uuid}）。 */
const submissionArticle = ref<{
  title: string
  categoryLabel: string
  cover: string
  author: string
  publishTime: string
  viewCount: number
  likeCount: number
  collectCount: number
  content: string
  summary: string
  isLiked: boolean
  isCollected: boolean
  media: { type: string; url: string; poster: string; images: string[] }
} | null>(null)

const article = computed(() => {
  if (submissionArticle.value) return submissionArticle.value
  const found = scienceArticles.find(c => c.id === articleId.value)
  return found || scienceArticles[0]
})

const isLiked = ref(article.value.isLiked)
const isCollected = ref(article.value.isCollected)
const interactionSaving = ref(false)

async function loadInteraction() {
  if (articleId.value.startsWith('submission-')) return
  try {
    const interaction = await getScienceArticleInteraction(articleId.value)
    isLiked.value = interaction.liked
    isCollected.value = interaction.collected
  } catch {
    uni.showToast({ title: '互动状态加载失败', icon: 'none' })
  }
}

async function saveInteraction(previousLiked: boolean, previousCollected: boolean) {
  interactionSaving.value = true
  try {
    const interaction = await updateScienceArticleInteraction(articleId.value, {
      liked: isLiked.value,
      collected: isCollected.value
    })
    isLiked.value = interaction.liked
    isCollected.value = interaction.collected
    return true
  } catch {
    isLiked.value = previousLiked
    isCollected.value = previousCollected
    uni.showToast({ title: '操作失败，请稍后重试', icon: 'none' })
    return false
  } finally {
    interactionSaving.value = false
  }
}

const contentParagraphs = computed(() => {
  const content = article.value.content || ''
  return content.split('\n\n').filter(p => p.trim())
})

function formatViewCount(count: number) {
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + '万'
  }
  return count.toString()
}

async function toggleLike() {
  if (interactionSaving.value) return
  const previousLiked = isLiked.value
  const previousCollected = isCollected.value
  isLiked.value = !isLiked.value
  if (await saveInteraction(previousLiked, previousCollected)) {
    uni.showToast({
      title: isLiked.value ? '已点赞' : '已取消点赞',
      icon: 'none',
      duration: 1000
    })
  }
}

async function toggleCollect() {
  if (interactionSaving.value) return
  const previousLiked = isLiked.value
  const previousCollected = isCollected.value
  isCollected.value = !isCollected.value
  if (await saveInteraction(previousLiked, previousCollected)) {
    uni.showToast({
      title: isCollected.value ? '已收藏' : '已取消收藏',
      icon: 'none',
      duration: 1000
    })
  }
}

function handleShare() {
  uni.setClipboardData({
    data: `${article.value.title}\n${article.value.summary}`,
    success: () => uni.showToast({ title: '分享内容已复制', icon: 'none' })
  })
}

function previewImage(idx: number) {
  if (article.value.media && article.value.media.images) {
    uni.previewImage({
      urls: article.value.media.images,
      current: article.value.media.images[idx]
    })
  }
}

onMounted(async () => {
  if (articleId.value.startsWith('submission-')) {
    try {
      const page = await listApprovedScienceSubmissions()
      const found = page.content.find(item => 'submission-' + item.id === articleId.value)
      if (found) {
        submissionArticle.value = {
          title: found.title,
          categoryLabel: ({ device: '设备使用', emergency: '突发急症', health: '健康管理', exercise: '运动养生' } as Record<string, string>)[found.category] || '科普投稿',
          cover: found.coverImageUrl || '',
          author: '用户投稿',
          publishTime: (found.submittedAt || '').slice(0, 10),
          summary: found.content,
          isLiked: false,
          isCollected: false,
          viewCount: 0,
          likeCount: 0,
          collectCount: 0,
          content: found.content,
          media: { type: 'image', url: '', poster: '', images: [] }
        }
      }
    } catch {
      uni.showToast({ title: '投稿加载失败', icon: 'none' })
    }
    return
  }
  loadInteraction()
})

</script>

<style lang="scss" scoped>
/* 扣掉导航栏高度，避免 body 多出可滚空间导致底部操作栏跟随滚动 */
.page {
  height: calc(100vh - var(--window-top, 0px) - var(--window-bottom, 0px));
  height: calc(100dvh - var(--window-top, 0px) - var(--window-bottom, 0px));
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #F3F7FA;
}

.page-scroll {
  flex: 1;
  height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.cover-area {
  position: relative;
  height: 500rpx;
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
.cover-gradient {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 280rpx;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0) 0%, rgba(0, 0, 0, 0.6) 100%);
  z-index: 2;
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

.scroll-content {
  position: relative;
  z-index: 2;
  margin-top: -32rpx;
  background: #FFFFFF;
  border-radius: 32rpx 32rpx 0 0;
  box-shadow: 0 -8rpx 32rpx rgba(0, 0, 0, 0.08);
}

.article-header {
  padding: 40rpx 32rpx 0;
}
.article-title {
  font-size: 40rpx;
  font-weight: 800;
  color: #20364D;
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
.author-info {
  margin-left: 16rpx;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}
.author-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #20364D;
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
  background: #2E6DD1;
  margin-right: 12rpx;
}
.gallery-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #20364D;
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

.action-bar {
  flex-shrink: 0;
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
  color: #C93D46;
}
.action-label {
  font-size: 20rpx;
  color: #86909C;
  font-weight: 500;
  transition: color 0.3s ease;
}
.action-label-active {
  color: #C93D46;
}
</style>
