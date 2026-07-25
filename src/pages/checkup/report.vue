<template>
  <view class="page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-content">
        <view class="nav-back" @tap="goBack">
          <text class="back-arrow">&#x2190;</text>
        </view>
        <text class="nav-title">AI健康分析报告</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <scroll-view
      class="scroll-content"
      scroll-y
      :style="{ paddingTop: (statusBarHeight + 44) + 'px' }"
    >
      <!-- AI分析加载动画 -->
      <view v-if="aiLoading" class="ai-loading-section">
        <view class="ai-loading-card">
          <view class="ai-brain-wrap">
            <view class="ai-brain">
              <text class="brain-icon">🧠</text>
            </view>
            <view class="brain-ring ring-1"></view>
            <view class="brain-ring ring-2"></view>
            <view class="brain-ring ring-3"></view>
          </view>
          <text class="ai-loading-title">AI智能体分析中...</text>
          <text class="ai-loading-desc">正在深度解读您的体检数据</text>
          <view class="ai-loading-steps">
            <view class="loading-step" :class="{ 'step-active': loadingStep >= 1 }">
              <view class="step-dot"></view>
              <text class="step-label">数据解析</text>
            </view>
            <view class="loading-step-line" :class="{ 'line-active': loadingStep >= 2 }"></view>
            <view class="loading-step" :class="{ 'step-active': loadingStep >= 2 }">
              <view class="step-dot"></view>
              <text class="step-label">风险评估</text>
            </view>
            <view class="loading-step-line" :class="{ 'line-active': loadingStep >= 3 }"></view>
            <view class="loading-step" :class="{ 'step-active': loadingStep >= 3 }">
              <view class="step-dot"></view>
              <text class="step-label">生成建议</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 分析结果内容 -->
      <view v-if="!aiLoading" class="result-content">
        <!-- 顶部风险概览卡片 -->
        <view class="risk-header-card">
          <view class="risk-header-bg"></view>
          <view class="risk-header-content">
            <view class="risk-top-row">
              <view class="risk-badge-wrap">
                <view class="risk-badge" :class="'risk-badge-' + analysis.riskLevel">
                  <view class="risk-badge-dot"></view>
                  <text class="risk-badge-text">{{ riskLevelLabel }}</text>
                </view>
              </view>
              <view class="risk-date">
                <text class="risk-date-text">{{ report.checkupDate }}</text>
              </view>
            </view>
            <text class="risk-summary">{{ analysis.summary }}</text>
            <view class="risk-stats-row">
              <view class="risk-stat">
                <text class="risk-stat-num">{{ analysis.abnormalItems.length }}</text>
                <text class="risk-stat-label">异常指标</text>
              </view>
              <view class="risk-stat-divider"></view>
              <view class="risk-stat">
                <text class="risk-stat-num">{{ report.ocrResult.items.length }}</text>
                <text class="risk-stat-label">检测项目</text>
              </view>
              <view class="risk-stat-divider"></view>
              <view class="risk-stat">
                <text class="risk-stat-num risk-stat-num-green">{{ normalCount }}</text>
                <text class="risk-stat-label">正常项目</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 异常指标分析 -->
        <view class="section-card">
          <view class="section-header">
            <view class="section-title-wrap">
              <view class="section-icon-wrap section-icon-warning">
                <text class="section-icon">&#x26A0;</text>
              </view>
              <text class="section-title">异常指标分析</text>
            </view>
            <text class="section-count">{{ analysis.abnormalItems.length }}项</text>
          </view>

          <view
            v-for="(item, idx) in analysis.abnormalItems"
            :key="idx"
            class="abnormal-card"
            :class="'abnormal-card-' + item.riskLevel"
          >
            <!-- 折叠头部 -->
            <view class="abnormal-header" @tap="toggleItem(idx)">
              <view class="abnormal-left">
                <view class="abnormal-name-row">
                  <view class="risk-level-dot" :class="'dot-' + item.riskLevel"></view>
                  <text class="abnormal-name">{{ item.name }}</text>
                </view>
                <view class="abnormal-value-row">
                  <text class="abnormal-value" :class="'value-' + item.riskLevel">{{ item.value }}</text>
                  <text class="abnormal-vs">vs</text>
                  <text class="abnormal-ref">{{ item.refRange }}</text>
                </view>
              </view>
              <view class="abnormal-right">
                <view class="risk-tag" :class="'risk-tag-' + item.riskLevel">
                  <text class="risk-tag-text">{{ riskLevelMap[item.riskLevel] }}</text>
                </view>
                <text class="expand-arrow" :class="{ 'arrow-expanded': expandedItems[idx] }">&#x25B6;</text>
              </view>
            </view>

            <!-- 展开内容 -->
            <view v-if="expandedItems[idx]" class="abnormal-body">
              <view class="analysis-block">
                <view class="analysis-label-row">
                  <view class="analysis-label-icon">
                    <text class="analysis-label-icon-text">🧠</text>
                  </view>
                  <text class="analysis-label">AI分析</text>
                </view>
                <text class="analysis-text">{{ item.analysis }}</text>
              </view>
              <view class="suggestions-block">
                <view class="suggestions-label-row">
                  <view class="suggestions-label-icon">
                    <text class="suggestions-label-icon-text">💡</text>
                  </view>
                  <text class="suggestions-label">改善建议</text>
                </view>
                <view
                  v-for="(suggestion, sIdx) in item.suggestions"
                  :key="sIdx"
                  class="suggestion-item"
                >
                  <view class="suggestion-bullet" :class="'bullet-' + item.riskLevel"></view>
                  <text class="suggestion-text">{{ suggestion }}</text>
                </view>
              </view>
            </view>
          </view>
        </view>

        <!-- 综合建议 -->
        <view class="section-card">
          <view class="section-header">
            <view class="section-title-wrap">
              <view class="section-icon-wrap section-icon-suggest">
                <text class="section-icon">📋</text>
              </view>
              <text class="section-title">综合建议</text>
            </view>
          </view>

          <view class="overall-suggestions">
            <view
              v-for="(suggestion, idx) in analysis.overallSuggestions"
              :key="idx"
              class="overall-item"
            >
              <view class="overall-num-wrap">
                <text class="overall-num">{{ idx + 1 }}</text>
              </view>
              <view class="overall-content">
                <text class="overall-text">{{ suggestion }}</text>
              </view>
            </view>
          </view>
        </view>

        <!-- AI分析说明 -->
        <view class="disclaimer-card">
          <view class="disclaimer-icon-wrap">
            <text class="disclaimer-icon">&#x2139;</text>
          </view>
          <view class="disclaimer-content">
            <text class="disclaimer-title">AI分析说明</text>
            <text class="disclaimer-text">本分析结果由AI智能体基于体检数据生成，仅供参考，不构成医疗诊断建议。如有健康问题，请及时咨询专业医生。</text>
          </view>
        </view>

        <!-- 底部操作 -->
        <view class="bottom-actions">
          <view class="btn-share" @tap="handleShare">
            <text class="btn-share-text">分享报告</text>
          </view>
          <view class="btn-archive" @tap="goArchive">
            <text class="btn-archive-text">查看健康档案</text>
          </view>
        </view>

        <!-- 底部安全区 -->
        <view class="bottom-safe"></view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { mockCheckupReport } from '@/mock/data'

// 系统信息
const statusBarHeight = ref(0)
const systemInfo = uni.getSystemInfoSync()
statusBarHeight.value = systemInfo.statusBarHeight || 20

// 数据
const report = computed(() => mockCheckupReport)
const analysis = computed(() => mockCheckupReport.aiAnalysis)
const normalCount = computed(() => report.value.ocrResult.items.filter(i => i.status === 'normal').length)

// 风险等级映射
const riskLevelLabel = computed(() => {
  const map: Record<string, string> = {
    low: '低风险',
    medium: '中风险',
    high: '高风险'
  }
  return map[analysis.value.riskLevel] || '低风险'
})

const riskLevelMap: Record<string, string> = {
  low: '低',
  medium: '中',
  high: '高'
}

// 展开状态
const expandedItems = reactive<Record<number, boolean>>({})

function toggleItem(idx: number) {
  expandedItems[idx] = !expandedItems[idx]
}

// AI加载动画
const aiLoading = ref(true)
const loadingStep = ref(0)

onMounted(() => {
  // 模拟AI分析过程
  const step1 = setTimeout(() => { loadingStep.value = 1 }, 600)
  const step2 = setTimeout(() => { loadingStep.value = 2 }, 1500)
  const step3 = setTimeout(() => { loadingStep.value = 3 }, 2200)
  const done = setTimeout(() => { aiLoading.value = false }, 2800)

  return () => {
    clearTimeout(step1)
    clearTimeout(step2)
    clearTimeout(step3)
    clearTimeout(done)
  }
})

// 操作
function handleShare() {
  uni.showToast({ title: '分享功能开发中', icon: 'none' })
}

function goArchive() {
  uni.navigateTo({ url: '/pages/checkup/archive' })
}

function goBack() {
  uni.navigateBack()
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #F0F4FA;
}

/* 导航栏 */
.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
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
}
.back-arrow {
  font-size: 40rpx;
  color: #FFFFFF;
  font-weight: 600;
}
.nav-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #FFFFFF;
}
.nav-placeholder {
  width: 64rpx;
}

/* 滚动内容 */
.scroll-content {
  min-height: 100vh;
  box-sizing: border-box;
}

/* AI加载动画 */
.ai-loading-section {
  display: flex;
  justify-content: center;
  padding-top: 120rpx;
}
.ai-loading-card {
  width: 600rpx;
  background: #FFFFFF;
  border-radius: 32rpx;
  padding: 60rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 8rpx 40rpx rgba(43, 111, 240, 0.1);
}
.ai-brain-wrap {
  position: relative;
  width: 160rpx;
  height: 160rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32rpx;
}
.ai-brain {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #2B6FF0 0%, #722ED1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3;
  box-shadow: 0 8rpx 32rpx rgba(43, 111, 240, 0.4);
}
.brain-icon {
  font-size: 52rpx;
}
.brain-ring {
  position: absolute;
  border-radius: 50%;
  border: 2rpx solid rgba(43, 111, 240, 0.2);
  animation: ringPulse 2.5s ease-out infinite;
}
.ring-1 {
  width: 120rpx;
  height: 120rpx;
  animation-delay: 0s;
}
.ring-2 {
  width: 140rpx;
  height: 140rpx;
  animation-delay: 0.5s;
}
.ring-3 {
  width: 160rpx;
  height: 160rpx;
  animation-delay: 1s;
}
@keyframes ringPulse {
  0% {
    transform: scale(0.8);
    opacity: 0.6;
    border-color: rgba(43, 111, 240, 0.4);
  }
  100% {
    transform: scale(1.4);
    opacity: 0;
    border-color: rgba(43, 111, 240, 0);
  }
}
.ai-loading-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #2B6FF0;
  margin-bottom: 8rpx;
}
.ai-loading-desc {
  font-size: 24rpx;
  color: #86909C;
  margin-bottom: 40rpx;
}
.ai-loading-steps {
  display: flex;
  align-items: center;
  gap: 0;
  width: 100%;
}
.loading-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}
.step-dot {
  width: 24rpx;
  height: 24rpx;
  border-radius: 50%;
  background: #E5E6EB;
  transition: all 0.4s ease;
}
.step-active .step-dot {
  background: #2B6FF0;
  box-shadow: 0 0 16rpx rgba(43, 111, 240, 0.5);
}
.step-label {
  font-size: 22rpx;
  color: #C9CDD4;
  font-weight: 500;
  transition: color 0.4s ease;
}
.step-active .step-label {
  color: #2B6FF0;
  font-weight: 600;
}
.loading-step-line {
  flex: 1;
  height: 2rpx;
  background: #E5E6EB;
  margin: 0 8rpx;
  margin-bottom: 32rpx;
  transition: background 0.4s ease;
}
.line-active {
  background: linear-gradient(90deg, #2B6FF0, #5B8DEF);
}

/* 风险概览卡片 */
.risk-header-card {
  position: relative;
  margin: 24rpx 24rpx 0;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 40rpx rgba(43, 111, 240, 0.12);
}
.risk-header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #2B6FF0 0%, #1A4FD0 40%, #0D3AAF 100%);
}
.risk-header-content {
  position: relative;
  padding: 32rpx;
  z-index: 2;
}
.risk-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}
.risk-badge-wrap {
  display: flex;
}
.risk-badge {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 10rpx 24rpx;
  border-radius: 24rpx;
  backdrop-filter: blur(8px);
}
.risk-badge-low {
  background: rgba(0, 180, 42, 0.25);
}
.risk-badge-medium {
  background: rgba(255, 154, 46, 0.25);
}
.risk-badge-high {
  background: rgba(245, 63, 63, 0.25);
}
.risk-badge-dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
}
.risk-badge-low .risk-badge-dot {
  background: #4DC580;
  box-shadow: 0 0 12rpx rgba(0, 180, 42, 0.6);
}
.risk-badge-medium .risk-badge-dot {
  background: #FFCF8B;
  box-shadow: 0 0 12rpx rgba(255, 154, 46, 0.6);
}
.risk-badge-high .risk-badge-dot {
  background: #FF7D7D;
  box-shadow: 0 0 12rpx rgba(245, 63, 63, 0.6);
}
.risk-badge-text {
  font-size: 26rpx;
  color: #FFFFFF;
  font-weight: 700;
}
.risk-date {
  padding: 6rpx 16rpx;
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.15);
  border: 1rpx solid rgba(255, 255, 255, 0.2);
}
.risk-date-text {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}
.risk-summary {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.95);
  line-height: 1.6;
  margin-bottom: 24rpx;
  font-weight: 500;
}
.risk-stats-row {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 16rpx;
  padding: 20rpx 0;
  backdrop-filter: blur(4px);
}
.risk-stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
}
.risk-stat-num {
  font-size: 44rpx;
  font-weight: 800;
  color: #FFFFFF;
  line-height: 1;
}
.risk-stat-num-green {
  color: #4DC580;
}
.risk-stat-label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
}
.risk-stat-divider {
  width: 1rpx;
  height: 48rpx;
  background: rgba(255, 255, 255, 0.15);
}

/* 通用section卡片 */
.section-card {
  margin: 24rpx 24rpx 0;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.06);
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}
.section-title-wrap {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.section-icon-wrap {
  width: 48rpx;
  height: 48rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.section-icon-warning {
  background: rgba(255, 154, 46, 0.1);
}
.section-icon-suggest {
  background: rgba(43, 111, 240, 0.1);
}
.section-icon {
  font-size: 28rpx;
}
.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1D2129;
}
.section-count {
  font-size: 24rpx;
  color: #86909C;
  font-weight: 500;
  padding: 4rpx 16rpx;
  border-radius: 12rpx;
  background: #F7F8FA;
}

/* 异常指标卡片 */
.abnormal-card {
  margin-bottom: 16rpx;
  border-radius: 20rpx;
  overflow: hidden;
  background: #FFFFFF;
  border: 1rpx solid #F2F3F5;
  border-left: 6rpx solid #E5E6EB;
  transition: all 0.3s ease;
}
.abnormal-card:last-child {
  margin-bottom: 0;
}
.abnormal-card-low {
  border-left-color: #00B42A;
}
.abnormal-card-medium {
  border-left-color: #FF9A2E;
}
.abnormal-card-high {
  border-left-color: #F53F3F;
}
.abnormal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 20rpx 20rpx 16rpx;
}
.abnormal-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.abnormal-name-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
}
.risk-level-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  flex-shrink: 0;
}
.dot-low {
  background: #00B42A;
  box-shadow: 0 0 8rpx rgba(0, 180, 42, 0.4);
}
.dot-medium {
  background: #FF9A2E;
  box-shadow: 0 0 8rpx rgba(255, 154, 46, 0.4);
}
.dot-high {
  background: #F53F3F;
  box-shadow: 0 0 8rpx rgba(245, 63, 63, 0.4);
}
.abnormal-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1D2129;
}
.abnormal-value-row {
  display: flex;
  align-items: baseline;
  gap: 8rpx;
  padding-left: 24rpx;
}
.abnormal-value {
  font-size: 30rpx;
  font-weight: 800;
}
.value-low { color: #2B6FF0; }
.value-medium { color: #FF9A2E; }
.value-high { color: #F53F3F; }
.abnormal-vs {
  font-size: 20rpx;
  color: #C9CDD4;
  font-weight: 500;
}
.abnormal-ref {
  font-size: 22rpx;
  color: #86909C;
}
.abnormal-right {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-shrink: 0;
}
.risk-tag {
  padding: 6rpx 16rpx;
  border-radius: 12rpx;
}
.risk-tag-low {
  background: rgba(0, 180, 42, 0.08);
}
.risk-tag-medium {
  background: rgba(255, 154, 46, 0.08);
}
.risk-tag-high {
  background: rgba(245, 63, 63, 0.08);
}
.risk-tag-text {
  font-size: 22rpx;
  font-weight: 600;
}
.risk-tag-low .risk-tag-text { color: #00B42A; }
.risk-tag-medium .risk-tag-text { color: #FF9A2E; }
.risk-tag-high .risk-tag-text { color: #F53F3F; }
.expand-arrow {
  font-size: 20rpx;
  color: #C9CDD4;
  transition: transform 0.3s ease;
}
.arrow-expanded {
  transform: rotate(90deg);
}

/* 展开内容 */
.abnormal-body {
  padding: 0 20rpx 20rpx 16rpx;
  border-top: 1rpx solid #F2F3F5;
}
.analysis-block {
  padding: 16rpx 0;
}
.analysis-label-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-bottom: 10rpx;
}
.analysis-label-icon {
  width: 36rpx;
  height: 36rpx;
  border-radius: 10rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #722ED1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}
.analysis-label-icon-text {
  font-size: 20rpx;
}
.analysis-label {
  font-size: 24rpx;
  font-weight: 600;
  color: #2B6FF0;
}
.analysis-text {
  font-size: 26rpx;
  color: #4E5969;
  line-height: 1.7;
  padding-left: 44rpx;
}
.suggestions-block {
  padding: 16rpx 0 0;
  border-top: 1rpx dashed #E5E6EB;
}
.suggestions-label-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-bottom: 12rpx;
}
.suggestions-label-icon {
  width: 36rpx;
  height: 36rpx;
  border-radius: 10rpx;
  background: rgba(255, 154, 46, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}
.suggestions-label-icon-text {
  font-size: 20rpx;
}
.suggestions-label {
  font-size: 24rpx;
  font-weight: 600;
  color: #FF9A2E;
}
.suggestion-item {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  margin-bottom: 12rpx;
  padding-left: 44rpx;
}
.suggestion-item:last-child {
  margin-bottom: 0;
}
.suggestion-bullet {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  margin-top: 10rpx;
  flex-shrink: 0;
}
.bullet-low { background: #00B42A; }
.bullet-medium { background: #FF9A2E; }
.bullet-high { background: #F53F3F; }
.suggestion-text {
  font-size: 26rpx;
  color: #4E5969;
  line-height: 1.6;
  flex: 1;
}

/* 综合建议 */
.overall-suggestions {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
.overall-item {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  padding: 20rpx;
  background: #F7F8FA;
  border-radius: 16rpx;
}
.overall-num-wrap {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4rpx 12rpx rgba(43, 111, 240, 0.2);
}
.overall-num {
  font-size: 24rpx;
  font-weight: 700;
  color: #FFFFFF;
}
.overall-content {
  flex: 1;
  display: flex;
  align-items: center;
}
.overall-text {
  font-size: 26rpx;
  color: #4E5969;
  line-height: 1.6;
  font-weight: 500;
}

/* AI分析说明 */
.disclaimer-card {
  margin: 24rpx 24rpx 0;
  background: #FFFBE8;
  border-radius: 20rpx;
  padding: 20rpx 24rpx;
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  border: 1rpx solid rgba(255, 196, 46, 0.15);
}
.disclaimer-icon-wrap {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: rgba(255, 154, 46, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.disclaimer-icon {
  font-size: 24rpx;
  color: #FF9A2E;
  font-weight: 700;
}
.disclaimer-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}
.disclaimer-title {
  font-size: 24rpx;
  font-weight: 600;
  color: #7D6600;
}
.disclaimer-text {
  font-size: 22rpx;
  color: #A69B50;
  line-height: 1.6;
}

/* 底部操作 */
.bottom-actions {
  display: flex;
  gap: 20rpx;
  margin: 32rpx 24rpx 0;
}
.btn-share {
  flex: 1;
  height: 88rpx;
  border-radius: 44rpx;
  background: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx solid #2B6FF0;
  box-shadow: 0 4rpx 16rpx rgba(43, 111, 240, 0.1);
}
.btn-share-text {
  font-size: 28rpx;
  font-weight: 600;
  color: #2B6FF0;
}
.btn-archive {
  flex: 1;
  height: 88rpx;
  border-radius: 44rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 32rpx rgba(43, 111, 240, 0.3);
}
.btn-archive-text {
  font-size: 28rpx;
  font-weight: 600;
  color: #FFFFFF;
}

/* 底部安全区 */
.bottom-safe {
  height: 60rpx;
}
</style>
