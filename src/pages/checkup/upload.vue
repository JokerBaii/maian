<template>
  <view class="page">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-bar-content">
        <view class="nav-back" @tap="goBack">
          <text class="back-arrow">&#x2190;</text>
        </view>
        <text class="nav-title">体检报告上传</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <scroll-view
      class="scroll-content"
      scroll-y
      :style="{ paddingTop: (statusBarHeight + 44) + 'px' }"
    >
      <!-- 顶部说明卡片 -->
      <view class="instruction-card">
        <view class="instruction-bg"></view>
        <view class="instruction-content">
          <view class="instruction-icon-wrap">
            <text class="instruction-icon">📄</text>
          </view>
          <view class="instruction-text-wrap">
            <text class="instruction-title">上传体检报告</text>
            <text class="instruction-desc">拍照或选择体检报告图片，AI将自动识别指标数据并进行智能健康分析</text>
          </view>
        </view>
        <view class="instruction-steps">
          <view class="step-item">
            <view class="step-num">1</view>
            <text class="step-text">上传报告</text>
          </view>
          <view class="step-line"></view>
          <view class="step-item">
            <view class="step-num">2</view>
            <text class="step-text">OCR识别</text>
          </view>
          <view class="step-line"></view>
          <view class="step-item">
            <view class="step-num">3</view>
            <text class="step-text">AI分析</text>
          </view>
        </view>
      </view>

      <!-- 上传方式卡片 -->
      <view class="upload-methods">
        <view class="method-card" @tap="handleCamera">
          <view class="method-icon-wrap method-camera">
            <text class="method-icon">📷</text>
          </view>
          <text class="method-title">拍照上传</text>
          <text class="method-desc">拍摄体检报告</text>
        </view>
        <view class="method-card" @tap="handleAlbum">
          <view class="method-icon-wrap method-album">
            <text class="method-icon">🖼️</text>
          </view>
          <text class="method-title">相册选择</text>
          <text class="method-desc">从相册选取</text>
        </view>
      </view>

      <!-- 图片预览区 -->
      <view v-if="hasImage" class="preview-card">
        <view class="card-header">
          <view class="card-title-wrap">
            <view class="card-title-bar"></view>
            <text class="card-title">已上传图片</text>
          </view>
          <view class="remove-btn" @tap="removeImage">
            <text class="remove-text">移除</text>
          </view>
        </view>
        <view class="preview-area">
          <view class="preview-image-wrap">
            <view class="preview-placeholder">
              <text class="placeholder-icon">📄</text>
              <text class="placeholder-text">体检报告预览</text>
              <text class="placeholder-sub">浙江大学医学院附属第一医院</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 开始识别按钮 -->
      <view v-if="hasImage && !ocrStarted" class="action-area">
        <view class="btn-primary" @tap="startOCR">
          <text class="btn-text">开始识别</text>
        </view>
      </view>

      <!-- OCR扫描动画 -->
      <view v-if="ocrProcessing" class="scan-card">
        <view class="scan-area">
          <view class="scan-placeholder">
            <text class="scan-icon">📄</text>
          </view>
          <view class="scan-line"></view>
        </view>
        <view class="scan-info">
          <text class="scan-title">正在识别中...</text>
          <text class="scan-desc">AI正在提取体检指标数据</text>
          <view class="scan-progress">
            <view class="progress-bar">
              <view class="progress-fill" :style="{ width: scanProgress + '%' }"></view>
            </view>
            <text class="progress-text">{{ scanProgress }}%</text>
          </view>
        </view>
      </view>

      <!-- OCR识别结果 -->
      <view v-if="ocrDone" class="result-card">
        <view class="card-header">
          <view class="card-title-wrap">
            <view class="card-title-bar result-bar"></view>
            <text class="card-title">识别结果</text>
          </view>
          <view class="result-badge">
            <text class="result-badge-text">共{{ report.ocrResult.items.length }}项</text>
          </view>
        </view>

        <!-- 结果表格 -->
        <view class="result-table">
          <!-- 表头 -->
          <view class="table-header">
            <text class="th th-name">指标名称</text>
            <text class="th th-value">数值</text>
            <text class="th th-ref">参考范围</text>
            <text class="th th-status">状态</text>
          </view>
          <!-- 表体 -->
          <view
            v-for="(item, idx) in report.ocrResult.items"
            :key="idx"
            class="table-row"
            :class="{ 'row-abnormal': item.status !== 'normal' }"
          >
            <text class="td td-name">{{ item.name }}</text>
            <view class="td td-value-wrap">
              <text class="td-value" :class="'value-' + item.status">{{ item.value }}</text>
              <text class="td-unit">{{ item.unit }}</text>
            </view>
            <text class="td td-ref">{{ item.refRange }}</text>
            <view class="td td-status">
              <view v-if="item.status === 'normal'" class="status-normal">
                <text class="status-check">&#x2713;</text>
              </view>
              <view v-else-if="item.status === 'low'" class="status-low">
                <text class="status-arrow">&#x2193;</text>
              </view>
              <view v-else-if="item.status === 'high'" class="status-high">
                <text class="status-arrow">&#x2191;</text>
              </view>
            </view>
          </view>
        </view>

        <!-- 异常统计 -->
        <view class="abnormal-summary">
          <view class="summary-item summary-normal">
            <text class="summary-num">{{ normalCount }}</text>
            <text class="summary-label">正常</text>
          </view>
          <view class="summary-item summary-low">
            <text class="summary-num">{{ lowCount }}</text>
            <text class="summary-label">偏低</text>
          </view>
          <view class="summary-item summary-high">
            <text class="summary-num">{{ highCount }}</text>
            <text class="summary-label">偏高</text>
          </view>
        </view>
      </view>

      <!-- AI智能分析按钮 -->
      <view v-if="ocrDone" class="action-area">
        <view class="btn-ai" @tap="goReport">
          <view class="btn-ai-icon">
            <text class="btn-ai-icon-text">&#x2728;</text>
          </view>
          <text class="btn-ai-text">AI智能分析</text>
        </view>
      </view>

      <!-- 底部安全区 -->
      <view class="bottom-safe"></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { mockCheckupReport } from '@/mock/data'

// 系统信息
const statusBarHeight = ref(0)
const systemInfo = uni.getSystemInfoSync()
statusBarHeight.value = systemInfo.statusBarHeight || 20

// 数据
const report = computed(() => mockCheckupReport)

// 状态
const hasImage = ref(false)
const ocrStarted = ref(false)
const ocrProcessing = ref(false)
const ocrDone = ref(false)
const scanProgress = ref(0)

// 统计
const normalCount = computed(() => report.value.ocrResult.items.filter(i => i.status === 'normal').length)
const lowCount = computed(() => report.value.ocrResult.items.filter(i => i.status === 'low').length)
const highCount = computed(() => report.value.ocrResult.items.filter(i => i.status === 'high').length)

// 上传操作
function handleCamera() {
  hasImage.value = true
  uni.showToast({ title: '已模拟拍照上传', icon: 'none' })
}

function handleAlbum() {
  hasImage.value = true
  uni.showToast({ title: '已模拟相册选择', icon: 'none' })
}

function removeImage() {
  hasImage.value = false
  ocrStarted.value = false
  ocrDone.value = false
  ocrProcessing.value = false
  scanProgress.value = 0
}

// OCR识别
function startOCR() {
  ocrStarted.value = true
  ocrProcessing.value = true
  scanProgress.value = 0

  const timer = setInterval(() => {
    scanProgress.value += Math.floor(Math.random() * 8) + 3
    if (scanProgress.value >= 100) {
      scanProgress.value = 100
      clearInterval(timer)
      setTimeout(() => {
        ocrProcessing.value = false
        ocrDone.value = true
      }, 400)
    }
  }, 120)
}

// 导航
function goReport() {
  uni.navigateTo({ url: '/pages/checkup/report?id=' + report.value.id })
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

/* 说明卡片 */
.instruction-card {
  position: relative;
  margin: 24rpx 24rpx 0;
  background: #FFFFFF;
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.08);
}
.instruction-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #2B6FF0 0%, #1A4FD0 50%, #0D3AAF 100%);
  opacity: 0.04;
}
.instruction-content {
  position: relative;
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 28rpx 28rpx 20rpx;
  z-index: 2;
}
.instruction-icon-wrap {
  width: 80rpx;
  height: 80rpx;
  border-radius: 20rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(43, 111, 240, 0.25);
  flex-shrink: 0;
}
.instruction-icon {
  font-size: 40rpx;
}
.instruction-text-wrap {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  flex: 1;
}
.instruction-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1D2129;
}
.instruction-desc {
  font-size: 24rpx;
  color: #86909C;
  line-height: 1.5;
}
.instruction-steps {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 40rpx 24rpx;
  gap: 0;
  z-index: 2;
}
.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}
.step-num {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  color: #FFFFFF;
  font-size: 24rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(43, 111, 240, 0.3);
}
.step-text {
  font-size: 22rpx;
  color: #4E5969;
  font-weight: 500;
}
.step-line {
  flex: 1;
  height: 2rpx;
  background: linear-gradient(90deg, #2B6FF0, #5B8DEF);
  margin: 0 16rpx;
  margin-bottom: 28rpx;
  opacity: 0.3;
}

/* 上传方式 */
.upload-methods {
  display: flex;
  gap: 20rpx;
  margin: 24rpx 24rpx 0;
}
.method-card {
  flex: 1;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 32rpx 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.06);
  transition: all 0.2s ease;
}
.method-card:active {
  transform: scale(0.97);
  box-shadow: 0 2rpx 12rpx rgba(43, 111, 240, 0.1);
}
.method-icon-wrap {
  width: 88rpx;
  height: 88rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.method-camera {
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  box-shadow: 0 6rpx 20rpx rgba(43, 111, 240, 0.3);
}
.method-album {
  background: linear-gradient(135deg, #722ED1 0%, #B37FEB 100%);
  box-shadow: 0 6rpx 20rpx rgba(114, 46, 209, 0.3);
}
.method-icon {
  font-size: 44rpx;
}
.method-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #1D2129;
}
.method-desc {
  font-size: 22rpx;
  color: #86909C;
}

/* 图片预览 */
.preview-card {
  margin: 24rpx 24rpx 0;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.06);
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}
.card-title-wrap {
  display: flex;
  align-items: center;
}
.card-title-bar {
  width: 6rpx;
  height: 28rpx;
  border-radius: 3rpx;
  background: #2B6FF0;
  margin-right: 12rpx;
}
.result-bar {
  background: #00B42A;
}
.card-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1D2129;
}
.remove-btn {
  padding: 8rpx 20rpx;
  border-radius: 16rpx;
  background: rgba(245, 63, 63, 0.08);
}
.remove-text {
  font-size: 24rpx;
  color: #F53F3F;
  font-weight: 500;
}
.preview-area {
  border-radius: 16rpx;
  overflow: hidden;
}
.preview-image-wrap {
  width: 100%;
  height: 320rpx;
  border-radius: 16rpx;
  overflow: hidden;
}
.preview-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #F2F3F5 0%, #E5E6EB 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
}
.placeholder-icon {
  font-size: 64rpx;
  opacity: 0.5;
}
.placeholder-text {
  font-size: 28rpx;
  color: #86909C;
  font-weight: 500;
}
.placeholder-sub {
  font-size: 22rpx;
  color: #C9CDD4;
}

/* 操作按钮 */
.action-area {
  margin: 32rpx 24rpx 0;
}
.btn-primary {
  height: 96rpx;
  border-radius: 48rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 32rpx rgba(43, 111, 240, 0.35);
}
.btn-primary:active {
  transform: scale(0.98);
  box-shadow: 0 4rpx 16rpx rgba(43, 111, 240, 0.3);
}
.btn-text {
  font-size: 32rpx;
  font-weight: 700;
  color: #FFFFFF;
  letter-spacing: 2rpx;
}

/* OCR扫描动画 */
.scan-card {
  margin: 24rpx 24rpx 0;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.06);
}
.scan-area {
  position: relative;
  width: 100%;
  height: 280rpx;
  border-radius: 16rpx;
  overflow: hidden;
  background: linear-gradient(135deg, #F2F3F5 0%, #E5E6EB 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
}
.scan-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
}
.scan-icon {
  font-size: 80rpx;
  opacity: 0.3;
}
.scan-line {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4rpx;
  background: linear-gradient(90deg, transparent 0%, #2B6FF0 30%, #5B8DEF 50%, #2B6FF0 70%, transparent 100%);
  box-shadow: 0 0 20rpx rgba(43, 111, 240, 0.6), 0 0 60rpx rgba(43, 111, 240, 0.3);
  animation: scanMove 2s ease-in-out infinite;
}
@keyframes scanMove {
  0% { top: 0; }
  50% { top: calc(100% - 4rpx); }
  100% { top: 0; }
}
.scan-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}
.scan-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #2B6FF0;
}
.scan-desc {
  font-size: 24rpx;
  color: #86909C;
}
.scan-progress {
  display: flex;
  align-items: center;
  gap: 16rpx;
  width: 100%;
  margin-top: 8rpx;
}
.progress-bar {
  flex: 1;
  height: 12rpx;
  border-radius: 6rpx;
  background: #F2F3F5;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: 6rpx;
  background: linear-gradient(90deg, #2B6FF0 0%, #5B8DEF 100%);
  transition: width 0.15s ease;
}
.progress-text {
  font-size: 24rpx;
  color: #2B6FF0;
  font-weight: 700;
  min-width: 60rpx;
  text-align: right;
}

/* 识别结果 */
.result-card {
  margin: 24rpx 24rpx 0;
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 24rpx rgba(43, 111, 240, 0.06);
}
.result-badge {
  padding: 6rpx 16rpx;
  border-radius: 12rpx;
  background: rgba(0, 180, 42, 0.08);
}
.result-badge-text {
  font-size: 22rpx;
  color: #00B42A;
  font-weight: 600;
}

/* 结果表格 */
.result-table {
  border-radius: 16rpx;
  overflow: hidden;
  background: #F7F8FA;
}
.table-header {
  display: flex;
  align-items: center;
  padding: 16rpx 12rpx;
  background: linear-gradient(135deg, #2B6FF0 0%, #5B8DEF 100%);
}
.th {
  font-size: 22rpx;
  color: #FFFFFF;
  font-weight: 600;
}
.th-name { width: 200rpx; padding-left: 8rpx; }
.th-value { width: 160rpx; }
.th-ref { width: 160rpx; }
.th-status { flex: 1; text-align: center; }

.table-row {
  display: flex;
  align-items: center;
  padding: 20rpx 12rpx;
  border-bottom: 1rpx solid #F2F3F5;
  transition: background 0.2s ease;
}
.table-row:last-child {
  border-bottom: none;
}
.row-abnormal {
  background: rgba(43, 111, 240, 0.02);
}
.td {
  font-size: 24rpx;
  color: #4E5969;
}
.td-name {
  width: 200rpx;
  font-weight: 500;
  color: #1D2129;
  padding-left: 8rpx;
  font-size: 22rpx;
}
.td-value-wrap {
  width: 160rpx;
  display: flex;
  align-items: baseline;
  gap: 4rpx;
}
.td-value {
  font-weight: 700;
  font-size: 26rpx;
}
.value-normal { color: #00B42A; }
.value-low { color: #2B6FF0; }
.value-high { color: #F53F3F; }
.td-unit {
  font-size: 18rpx;
  color: #C9CDD4;
}
.td-ref {
  width: 160rpx;
  font-size: 20rpx;
  color: #86909C;
}
.td-status {
  flex: 1;
  display: flex;
  justify-content: center;
}
.status-normal {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: rgba(0, 180, 42, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}
.status-check {
  font-size: 24rpx;
  color: #00B42A;
  font-weight: 700;
}
.status-low {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: rgba(43, 111, 240, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}
.status-high {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: rgba(245, 63, 63, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}
.status-arrow {
  font-size: 24rpx;
  font-weight: 800;
}
.status-low .status-arrow { color: #2B6FF0; }
.status-high .status-arrow { color: #F53F3F; }

/* 异常统计 */
.abnormal-summary {
  display: flex;
  align-items: center;
  margin-top: 20rpx;
  padding: 20rpx;
  background: #F7F8FA;
  border-radius: 16rpx;
  gap: 0;
}
.summary-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
}
.summary-num {
  font-size: 40rpx;
  font-weight: 800;
  line-height: 1;
}
.summary-normal .summary-num { color: #00B42A; }
.summary-low .summary-num { color: #2B6FF0; }
.summary-high .summary-num { color: #F53F3F; }
.summary-label {
  font-size: 22rpx;
  color: #86909C;
  font-weight: 500;
}

/* AI分析按钮 */
.btn-ai {
  height: 96rpx;
  border-radius: 48rpx;
  background: linear-gradient(135deg, #722ED1 0%, #B37FEB 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  box-shadow: 0 8rpx 32rpx rgba(114, 46, 209, 0.35);
}
.btn-ai:active {
  transform: scale(0.98);
}
.btn-ai-icon {
  width: 44rpx;
  height: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.btn-ai-icon-text {
  font-size: 32rpx;
}
.btn-ai-text {
  font-size: 32rpx;
  font-weight: 700;
  color: #FFFFFF;
  letter-spacing: 2rpx;
}

/* 底部安全区 */
.bottom-safe {
  height: 60rpx;
}
</style>
