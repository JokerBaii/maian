<template>
  <view v-if="lesson" class="page">
    <video
      class="video-player"
      :src="lesson.url"
      :poster="lesson.poster"
      controls
      autoplay
      enable-play-gesture
      show-fullscreen-btn
      show-play-btn
      object-fit="contain"
    />
    <view class="lesson-card">
      <text class="lesson-label">急救视频课堂</text>
      <text class="lesson-title">{{ lesson.title }}</text>
      <text class="lesson-summary">{{ lesson.summary }}</text>
      <view class="source-row">
        <view class="source-dot"></view>
        <view class="source-copy">
          <text>内容来源</text>
          <text>{{ lesson.source }}</text>
        </view>
        <view class="source-link" @tap="openSource">查看原始发布</view>
      </view>
    </view>
    <view class="learning-note">
      <text class="note-title">学习提示</text>
      <text>观看视频后可进入急救自测巩固流程；实际操作请以专业急救培训和现场调度指导为准。</text>
      <view class="quiz-button" @tap="openQuiz">完成后去自测</view>
    </view>
  </view>
  <view v-else class="empty">视频课程不存在</view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { officialFirstAidVideos } from '@/data/editorial'
import { openExternalUrl } from '@/utils/external'

const lesson = ref<(typeof officialFirstAidVideos)[number] | null>(null)

onLoad((query) => {
  lesson.value = officialFirstAidVideos.find(item => item.id === query?.id) || null
})

function openSource() {
  if (lesson.value) openExternalUrl(lesson.value.sourcePage)
}

function openQuiz() {
  uni.navigateTo({ url: '/pages/science/quiz' })
}
</script>

<style lang="scss" scoped>
.page { min-height: 100vh; padding-bottom: 50rpx; background: #F3F7FA; color: #20364D; }
.video-player { width: 100%; height: 460rpx; background: #101820; }
.lesson-card, .learning-note { margin: 20rpx 24rpx 0; padding: 26rpx; border: 1rpx solid #DDE6EE; border-radius: 18rpx; background: #FFFFFF; }
.lesson-label { display: block; color: #A9212B; font-size: 20rpx; font-weight: 700; }
.lesson-title { display: block; margin-top: 9rpx; color: #20364D; font-size: 33rpx; font-weight: 760; line-height: 1.35; }
.lesson-summary { display: block; margin-top: 13rpx; color: #6F8093; font-size: 23rpx; line-height: 1.65; }
.source-row { display: flex; align-items: center; gap: 12rpx; margin-top: 23rpx; padding-top: 20rpx; border-top: 1rpx solid #E9EEF3; }
.source-dot { width: 12rpx; height: 12rpx; border-radius: 50%; background: #24986B; }
.source-copy { display: flex; flex: 1; min-width: 0; flex-direction: column; gap: 3rpx; }.source-copy text:first-child { color: #8A98A8; font-size: 18rpx; }.source-copy text:last-child { overflow: hidden; font-size: 21rpx; text-overflow: ellipsis; white-space: nowrap; }
.source-link { color: #2E6DD1; font-size: 20rpx; font-weight: 650; }
.note-title { display: block; margin-bottom: 9rpx; font-size: 25rpx; font-weight: 700; }.learning-note > text:last-of-type { color: #718197; font-size: 21rpx; line-height: 1.6; }
.quiz-button { display: flex; align-items: center; justify-content: center; height: 76rpx; margin-top: 20rpx; border-radius: 11rpx; background: #2E6DD1; color: #FFFFFF; font-size: 24rpx; font-weight: 700; }
.empty { padding-top: 200rpx; color: #8390A1; text-align: center; }
</style>
