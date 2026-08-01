<template>
  <view class="page">
    <view v-if="!showResult" class="quiz-content">
      <view class="progress-section">
        <view class="progress-info">
          <text class="progress-current">{{ currentIndex + 1 }}</text>
          <text class="progress-divider">/</text>
          <text class="progress-total">{{ quizData.length }}</text>
        </view>
        <view class="progress-bar-wrap">
          <view class="progress-bar-bg">
            <view
              class="progress-bar-fill"
              :style="{ width: ((currentIndex + 1) / quizData.length * 100) + '%' }"
            ></view>
          </view>
        </view>
      </view>

      <view class="question-card">
        <view class="question-number">
          <text class="question-number-text">第{{ currentIndex + 1 }}题</text>
        </view>
        <text class="question-text">{{ currentQuestion.question }}</text>
      </view>

      <view class="options-list">
        <view
          v-for="(option, idx) in currentQuestion.options"
          :key="idx"
          class="option-item"
          :class="{
            'option-selected': selectedIndex === idx,
            'option-correct': answered && idx === currentQuestion.answer,
            'option-wrong': answered && selectedIndex === idx && idx !== currentQuestion.answer
          }"
          @tap="selectOption(idx)"
        >
          <view class="option-prefix" :class="{
            'prefix-selected': selectedIndex === idx,
            'prefix-correct': answered && idx === currentQuestion.answer,
            'prefix-wrong': answered && selectedIndex === idx && idx !== currentQuestion.answer
          }">
            <text class="option-prefix-text">{{ optionLabels[idx] }}</text>
          </view>
          <text class="option-text" :class="{
            'option-text-correct': answered && idx === currentQuestion.answer,
            'option-text-wrong': answered && selectedIndex === idx && idx !== currentQuestion.answer
          }">{{ option }}</text>
          <view v-if="answered && idx === currentQuestion.answer" class="option-feedback feedback-correct">
            <app-icon class="feedback-icon" name="checkmarkempty" :size="15" color="#FFFFFF" />
          </view>
          <view v-if="answered && selectedIndex === idx && idx !== currentQuestion.answer" class="option-feedback feedback-wrong">
            <app-icon class="feedback-icon" name="closeempty" :size="15" color="#FFFFFF" />
          </view>
        </view>
      </view>

      <view v-if="answered" class="explanation-card">
        <view class="explanation-header">
          <view class="explanation-icon-wrap">
            <app-icon
              class="explanation-icon"
              :name="isCurrentCorrect ? 'checkmarkempty' : 'closeempty'"
              :size="17"
              color="#FFFFFF"
            />
          </view>
          <text class="explanation-title">{{ isCurrentCorrect ? '回答正确' : '回答错误' }}</text>
        </view>
        <text class="explanation-text">{{ currentQuestion.explanation }}</text>
      </view>

      <view v-if="answered" class="next-btn-wrap">
        <view class="next-btn" @tap="nextQuestion">
          <text class="next-btn-text">{{ isLastQuestion ? '查看结果' : '下一题' }}</text>
          <app-icon class="next-btn-arrow" name="right" :size="18" color="#FFFFFF" />
        </view>
      </view>
    </view>

    <view v-if="showResult" class="result-content">
      <view class="result-card">
        <view class="score-ring-wrap">
          <view class="score-ring">
            <view class="score-ring-bg"></view>
            <view class="score-ring-fill" :style="{ '--score-deg': scoreDeg + 'deg' }"></view>
            <view class="score-ring-inner">
              <text class="score-number">{{ score }}</text>
              <text class="score-unit">分</text>
            </view>
          </view>
        </view>

        <view class="result-stats">
          <view class="result-stat-item">
            <text class="result-stat-value result-stat-correct">{{ correctCount }}</text>
            <text class="result-stat-label">答对</text>
          </view>
          <view class="result-stat-divider"></view>
          <view class="result-stat-item">
            <text class="result-stat-value result-stat-wrong">{{ quizData.length - correctCount }}</text>
            <text class="result-stat-label">答错</text>
          </view>
          <view class="result-stat-divider"></view>
          <view class="result-stat-item">
            <text class="result-stat-value result-stat-total">{{ quizData.length }}</text>
            <text class="result-stat-label">总题数</text>
          </view>
        </view>

        <view class="encourage-section">
          <app-icon
            class="encourage-emoji"
            :name="encourageIcon"
            :size="46"
            color="#1F63D5"
          />
          <text class="encourage-text">{{ encourageMessage }}</text>
        </view>
      </view>

      <view class="detail-card">
        <view class="detail-header">
          <view class="detail-title-bar"></view>
          <text class="detail-title">答题详情</text>
        </view>
        <view class="detail-list">
          <view
            v-for="(item, idx) in quizData"
            :key="idx"
            class="detail-item"
          >
            <view class="detail-item-icon" :class="item.userCorrect ? 'detail-icon-correct' : 'detail-icon-wrong'">
              <app-icon
                class="detail-item-icon-text"
                :name="item.userCorrect ? 'checkmarkempty' : 'closeempty'"
                :size="14"
                color="#FFFFFF"
              />
            </view>
            <text class="detail-item-text">第{{ idx + 1 }}题</text>
          </view>
        </view>
      </view>

      <view class="result-actions">
        <view class="result-btn result-btn-retry" @tap="retryQuiz">
          <text class="result-btn-text result-btn-text-retry">再试一次</text>
        </view>
        <view class="result-btn result-btn-back" @tap="goBack">
          <text class="result-btn-text result-btn-text-back">返回</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { scienceQuizQuestions } from '@/data/editorial'

const optionLabels = ['A', 'B', 'C', 'D']

const quizData = ref(scienceQuizQuestions.map(q => ({ ...q, userCorrect: false })))

const currentIndex = ref(0)
const selectedIndex = ref(-1)
const answered = ref(false)
const showResult = ref(false)

const currentQuestion = computed(() => quizData.value[currentIndex.value])

const isLastQuestion = computed(() => currentIndex.value === quizData.value.length - 1)

const isCurrentCorrect = computed(() => selectedIndex.value === currentQuestion.value.answer)

const correctCount = computed(() => quizData.value.filter(q => q.userCorrect).length)

const score = computed(() => Math.round((correctCount.value / quizData.value.length) * 100))

const scoreDeg = computed(() => (score.value / 100) * 360)

const encourageIcon = computed(() => {
  if (score.value >= 90) return 'medal-filled'
  if (score.value >= 70) return 'hand-up-filled'
  if (score.value >= 50) return 'flag-filled'
  return 'compose'
})

const encourageMessage = computed(() => {
  if (score.value >= 90) return '太棒了！你是急救知识达人！'
  if (score.value >= 70) return '不错！继续学习可以更好！'
  if (score.value >= 50) return '加油！多学多练一定能进步！'
  return '别灰心，急救知识值得认真学习！'
})

function selectOption(idx: number) {
  if (answered.value) return
  selectedIndex.value = idx
  answered.value = true
  quizData.value[currentIndex.value].userCorrect = idx === currentQuestion.value.answer
}

function nextQuestion() {
  if (isLastQuestion.value) {
    showResult.value = true
    return
  }
  currentIndex.value++
  selectedIndex.value = -1
  answered.value = false
}

function retryQuiz() {
  currentIndex.value = 0
  selectedIndex.value = -1
  answered.value = false
  showResult.value = false
  quizData.value = scienceQuizQuestions.map(q => ({ ...q, userCorrect: false }))
}

function goBack() {
  uni.navigateBack()
}

</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F3F7FA;
}

.quiz-content {
  min-height: 100vh;
  padding: 0 32rpx;
  box-sizing: border-box;
}

.progress-section {
  padding: 32rpx 0 24rpx;
}
.progress-info {
  display: flex;
  align-items: baseline;
  gap: 4rpx;
  margin-bottom: 16rpx;
}
.progress-current {
  font-size: 40rpx;
  font-weight: 800;
  color: #2E6DD1;
}
.progress-divider {
  font-size: 28rpx;
  color: #C9CDD4;
  margin: 0 4rpx;
}
.progress-total {
  font-size: 28rpx;
  color: #86909C;
  font-weight: 600;
}
.progress-bar-wrap {
  width: 100%;
}
.progress-bar-bg {
  width: 100%;
  height: 12rpx;
  border-radius: 6rpx;
  background: #E8F0FE;
  overflow: hidden;
}
.progress-bar-fill {
  height: 100%;
  border-radius: 6rpx;
  background: linear-gradient(90deg, #2E6DD1 0%, #2E6DD1 100%);
  transition: width 0.4s ease;
  box-shadow: 0 2rpx 8rpx rgba(43, 111, 240, 0.3);
}

.question-card {
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 36rpx 32rpx;
  margin-bottom: 28rpx;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.06);
}
.question-number {
  display: inline-flex;
  padding: 6rpx 20rpx;
  border-radius: 20rpx;
  background: rgba(43, 111, 240, 0.08);
  margin-bottom: 20rpx;
}
.question-number-text {
  font-size: 22rpx;
  font-weight: 700;
  color: #2E6DD1;
}
.question-text {
  font-size: 32rpx;
  font-weight: 700;
  color: #20364D;
  line-height: 1.6;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  margin-bottom: 28rpx;
}
.option-item {
  display: flex;
  align-items: center;
  padding: 28rpx 24rpx;
  background: #FFFFFF;
  border-radius: 20rpx;
  border: 2rpx solid #E8F0FE;
  transition: all 0.3s ease;
  box-shadow: 0 2rpx 12rpx rgba(43, 111, 240, 0.04);
  position: relative;
}
.option-item:active {
  transform: scale(0.98);
}
.option-selected {
  border-color: #2E6DD1;
  background: rgba(43, 111, 240, 0.04);
  box-shadow: 0 4rpx 16rpx rgba(43, 111, 240, 0.12);
}
.option-correct {
  border-color: #23956A;
  background: rgba(0, 180, 42, 0.04);
  box-shadow: 0 4rpx 16rpx rgba(0, 180, 42, 0.12);
}
.option-wrong {
  border-color: #C93D46;
  background: rgba(245, 63, 63, 0.04);
  box-shadow: 0 4rpx 16rpx rgba(245, 63, 63, 0.12);
}
.option-prefix {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  background: #F7F8FA;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  flex-shrink: 0;
  transition: all 0.3s ease;
}
.prefix-selected {
  background: #2E6DD1;
}
.prefix-correct {
  background: #23956A;
}
.prefix-wrong {
  background: #C93D46;
}
.option-prefix-text {
  font-size: 26rpx;
  font-weight: 700;
  color: #86909C;
  transition: color 0.3s ease;
}
.prefix-selected .option-prefix-text,
.prefix-correct .option-prefix-text,
.prefix-wrong .option-prefix-text {
  color: #FFFFFF;
}
.option-text {
  flex: 1;
  font-size: 28rpx;
  color: #4E5969;
  line-height: 1.5;
  transition: color 0.3s ease;
}
.option-text-correct {
  color: #23956A;
  font-weight: 600;
}
.option-text-wrong {
  color: #C93D46;
  font-weight: 600;
}
.option-feedback {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 12rpx;
  flex-shrink: 0;
}
.feedback-correct {
  background: rgba(0, 180, 42, 0.15);
}
.feedback-wrong {
  background: rgba(245, 63, 63, 0.15);
}
.feedback-icon {
  font-size: 24rpx;
  font-weight: 800;
}
.feedback-correct .feedback-icon {
  color: #23956A;
}
.feedback-wrong .feedback-icon {
  color: #C93D46;
}

.explanation-card {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 28rpx 24rpx;
  margin-bottom: 28rpx;
  border-left: 6rpx solid #2E6DD1;
  box-shadow: 0 4rpx 16rpx rgba(43, 111, 240, 0.06);
}
.explanation-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
}
.explanation-icon-wrap {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.explanation-icon-wrap {
  background: rgba(43, 111, 240, 0.1);
}
.explanation-icon {
  font-size: 22rpx;
  font-weight: 800;
  color: #2E6DD1;
}
.explanation-title {
  font-size: 26rpx;
  font-weight: 700;
  color: #20364D;
}
.explanation-text {
  font-size: 26rpx;
  color: #4E5969;
  line-height: 1.7;
}

.next-btn-wrap {
  padding: 16rpx 0 60rpx;
}
.next-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  height: 96rpx;
  border-radius: 48rpx;
  background: linear-gradient(135deg, #2E6DD1 0%, #2E6DD1 100%);
  box-shadow: 0 8rpx 32rpx rgba(43, 111, 240, 0.3);
  transition: all 0.2s ease;
}
.next-btn:active {
  transform: scale(0.97);
  box-shadow: 0 4rpx 16rpx rgba(43, 111, 240, 0.2);
}
.next-btn-text {
  font-size: 32rpx;
  font-weight: 700;
  color: #FFFFFF;
}
.next-btn-arrow {
  font-size: 32rpx;
  color: #FFFFFF;
  font-weight: 700;
}

.result-content {
  min-height: 100vh;
  padding: 0 32rpx;
  box-sizing: border-box;
}

.result-card {
  background: #FFFFFF;
  border-radius: 32rpx;
  padding: 48rpx 32rpx;
  margin-top: 32rpx;
  box-shadow: 0 8rpx 32rpx rgba(43, 111, 240, 0.08);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.score-ring-wrap {
  margin-bottom: 40rpx;
}
.score-ring {
  width: 240rpx;
  height: 240rpx;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.score-ring-bg {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 16rpx solid #E8F0FE;
}
.score-ring-fill {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 16rpx solid transparent;
  border-top-color: #2E6DD1;
  border-right-color: #2E6DD1;
  transform: rotate(-45deg);
  clip-path: polygon(50% 0%, 100% 0%, 100% 100%, 0% 100%, 0% 50%);
}
.score-ring-inner {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: baseline;
  justify-content: center;
}
.score-number {
  font-size: 72rpx;
  font-weight: 900;
  color: #2E6DD1;
  line-height: 1;
}
.score-unit {
  font-size: 28rpx;
  color: #86909C;
  font-weight: 600;
  margin-left: 4rpx;
}

.result-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 40rpx;
  width: 100%;
  padding: 32rpx 0;
  border-top: 1rpx solid #F2F3F5;
  border-bottom: 1rpx solid #F2F3F5;
  margin-bottom: 32rpx;
}
.result-stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}
.result-stat-value {
  font-size: 44rpx;
  font-weight: 800;
  line-height: 1;
}
.result-stat-correct {
  color: #23956A;
}
.result-stat-wrong {
  color: #C93D46;
}
.result-stat-total {
  color: #2E6DD1;
}
.result-stat-label {
  font-size: 24rpx;
  color: #86909C;
  font-weight: 500;
}
.result-stat-divider {
  width: 1rpx;
  height: 48rpx;
  background: #E5E6EB;
}

.encourage-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}
.encourage-emoji {
  font-size: 56rpx;
}
.encourage-text {
  font-size: 28rpx;
  color: #4E5969;
  font-weight: 600;
  text-align: center;
}

.detail-card {
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 28rpx 24rpx;
  margin-top: 24rpx;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.06);
}
.detail-header {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
}
.detail-title-bar {
  width: 6rpx;
  height: 28rpx;
  border-radius: 3rpx;
  background: #2E6DD1;
  margin-right: 12rpx;
}
.detail-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #20364D;
}
.detail-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.detail-item {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 12rpx 20rpx;
  border-radius: 12rpx;
  background: #F7F8FA;
}
.detail-item-icon {
  width: 32rpx;
  height: 32rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.detail-icon-correct {
  background: rgba(0, 180, 42, 0.15);
}
.detail-icon-wrong {
  background: rgba(245, 63, 63, 0.15);
}
.detail-item-icon-text {
  font-size: 18rpx;
  font-weight: 800;
}
.detail-icon-correct .detail-item-icon-text {
  color: #23956A;
}
.detail-icon-wrong .detail-item-icon-text {
  color: #C93D46;
}
.detail-item-text {
  font-size: 24rpx;
  color: #4E5969;
  font-weight: 500;
}

.result-actions {
  display: flex;
  gap: 24rpx;
  padding: 40rpx 0 60rpx;
}
.result-btn {
  flex: 1;
  height: 96rpx;
  border-radius: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}
.result-btn:active {
  transform: scale(0.97);
}
.result-btn-retry {
  background: linear-gradient(135deg, #2E6DD1 0%, #2E6DD1 100%);
  box-shadow: 0 8rpx 32rpx rgba(43, 111, 240, 0.3);
}
.result-btn-back {
  background: #FFFFFF;
  border: 2rpx solid #2E6DD1;
  box-shadow: 0 4rpx 16rpx rgba(43, 111, 240, 0.08);
}
.result-btn-text {
  font-size: 30rpx;
  font-weight: 700;
}
.result-btn-text-retry {
  color: #FFFFFF;
}
.result-btn-text-back {
  color: #2E6DD1;
}
</style>
