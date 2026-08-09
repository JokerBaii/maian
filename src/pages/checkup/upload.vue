<template>
  <view class="page apple-page motion-page-sheet">
    <view class="intro">
      <text class="intro-title">上传体检报告</text>
      <text class="intro-copy">拍摄完整、清晰的检查单，系统会自动整理结果。</text>
    </view>

    <view class="upload-card">
      <view v-if="!imagePath" class="upload-empty app-motion-control" @tap="chooseImage">
        <app-icon-tile name="camera" tone="blue" size="large" />
        <text class="upload-title">拍摄或选择检查单</text>
        <text class="upload-hint">支持 JPG、PNG，建议包含完整页眉与检查结果</text>
      </view>

      <view v-else class="image-preview">
        <image :src="imagePath" class="preview-image" mode="aspectFill" />
        <view class="preview-actions">
          <view class="preview-action" @tap="chooseImage">重新选择</view>
          <view class="preview-action preview-action-danger" @tap="clearImage">移除</view>
        </view>
      </view>
    </view>

    <view class="process-card">
      <text class="process-title">自动整理内容</text>
      <view class="process-row">
        <text class="process-index">1</text>
        <view><text class="process-name">读取检查单</text><text class="process-desc">识别机构、日期和原始文字</text></view>
      </view>
      <view class="process-row">
        <text class="process-index">2</text>
        <view><text class="process-name">整理检查结果</text><text class="process-desc">提取项目、数值、单位与参考范围</text></view>
      </view>
      <view class="process-row process-row-last">
        <text class="process-index">3</text>
        <view><text class="process-name">生成健康报告</text><text class="process-desc">归纳需要关注的项目和后续建议</text></view>
      </view>
    </view>

    <view class="privacy-note">
      <app-icon name="auth-filled" :size="15" color="#5E6B7C" />
      <text>仅在你确认授权后处理报告；结果用于健康信息整理，不能替代医生诊断。</text>
    </view>

    <view class="submit-area">
      <view
        class="submit-button app-motion-control"
        :class="{ disabled: submitting || !imagePath }"
        @tap="generateReport"
      >
        <app-icon :name="submitting ? 'spinner-cycle' : 'scan'" :size="19" color="#FFFFFF" />
        <text>{{ submitting ? '正在识别并生成…' : '识别并生成报告' }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import AppIconTile from '@/components/AppIconTile.vue'
import { uploadImage } from '@/api/files'
import { createHealthReport, recognizeHealthReport } from '@/api/reports'
import { getUserSettings, updateUserSettings } from '@/api/user'
import { userFacingError } from '@/utils/presentation'

const imagePath = ref('')
const uploadedMediaId = ref('')
const submitting = ref(false)

function chooseImage() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: result => {
      imagePath.value = result.tempFilePaths[0] || ''
      uploadedMediaId.value = ''
    }
  })
}

function clearImage() {
  imagePath.value = ''
  uploadedMediaId.value = ''
}

async function generateReport() {
  if (submitting.value) return
  if (!imagePath.value) {
    uni.showToast({ title: '请先拍摄或选择检查单', icon: 'none' })
    return
  }
  submitting.value = true
  uni.showLoading({ title: '正在识别…' })
  try {
    if (!await ensureProcessingConsent()) return
    if (!uploadedMediaId.value) {
      uploadedMediaId.value = (await uploadImage(imagePath.value, 'HEALTH_REPORT')).mediaId
    }
    const recognized = await recognizeHealthReport(uploadedMediaId.value)
    uni.showLoading({ title: '正在生成报告…' })
    const report = await createHealthReport({
      checkupDate: recognized.checkupDate,
      hospital: recognized.hospital,
      sourceMediaId: uploadedMediaId.value,
      indicators: recognized.indicators
    })
    uni.hideLoading()
    uni.redirectTo({ url: `/pages/checkup/report?id=${encodeURIComponent(report.id)}` })
  } catch (error: any) {
    uni.hideLoading()
    uni.showToast({ title: userFacingError(error, '检查单处理失败，请重新拍摄后再试'), icon: 'none' })
  } finally {
    submitting.value = false
  }
}

async function ensureProcessingConsent() {
  const settings = await getUserSettings()
  if (settings.healthDataShare) return true
  uni.hideLoading()
  const confirmed = await new Promise<boolean>(resolve => {
    uni.showModal({
      title: '授权处理体检报告',
      content: '检查单将发送至受保护的文字识别与健康信息整理服务。是否同意本次及后续处理？',
      confirmText: '同意并继续',
      cancelText: '暂不处理',
      success: result => resolve(Boolean(result.confirm)),
      fail: () => resolve(false)
    })
  })
  if (!confirmed) return false
  await updateUserSettings({
    rescuePush: settings.rescuePush,
    healthAlert: settings.healthAlert,
    scienceUpdate: settings.scienceUpdate,
    locationShare: settings.locationShare,
    healthDataShare: true,
    maxHeartRate: settings.maxHeartRate,
    minHeartRate: settings.minHeartRate
  })
  uni.showLoading({ title: '正在识别…' })
  return true
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 34rpx 28rpx calc(42rpx + env(safe-area-inset-bottom));
  background: #F2F2F7;
  color: #1C1C1E;
}

.intro { padding: 4rpx 4rpx 26rpx; }
.intro-title { display: block; font-size: 38rpx; font-weight: 750; letter-spacing: -1rpx; }
.intro-copy { display: block; margin-top: 8rpx; color: #636366; font-size: 25rpx; line-height: 1.5; }

.upload-card,
.process-card {
  overflow: hidden;
  border: 1rpx solid rgba(60, 60, 67, .12);
  border-radius: 20rpx;
  background: #FFFFFF;
}

.upload-empty {
  display: flex;
  min-height: 330rpx;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32rpx;
}
.upload-title { margin-top: 20rpx; font-size: 29rpx; font-weight: 680; }
.upload-hint { max-width: 520rpx; margin-top: 8rpx; color: #8E8E93; text-align: center; font-size: 22rpx; line-height: 1.5; }

.image-preview { position: relative; height: 430rpx; background: #E5E5EA; }
.preview-image { width: 100%; height: 100%; }
.preview-actions {
  position: absolute;
  right: 16rpx;
  bottom: 16rpx;
  display: flex;
  overflow: hidden;
  border-radius: 12rpx;
  background: rgba(28, 28, 30, .76);
  backdrop-filter: blur(18rpx);
}
.preview-action { padding: 12rpx 18rpx; color: #FFFFFF; font-size: 22rpx; }
.preview-action + .preview-action { border-left: 1rpx solid rgba(255, 255, 255, .22); }
.preview-action-danger { color: #FFB3B0; }

.process-card { margin-top: 24rpx; padding-left: 24rpx; }
.process-title { display: block; padding: 24rpx 24rpx 14rpx 0; font-size: 27rpx; font-weight: 680; }
.process-row { display: flex; min-height: 92rpx; align-items: center; gap: 18rpx; border-bottom: 1rpx solid rgba(60, 60, 67, .12); }
.process-row-last { border-bottom: 0; }
.process-index { display: flex; width: 38rpx; height: 38rpx; flex: none; align-items: center; justify-content: center; border-radius: 50%; background: #EAF2FC; color: #2E6DD1; font-size: 20rpx; font-weight: 700; }
.process-name,
.process-desc { display: block; }
.process-name { font-size: 25rpx; font-weight: 600; }
.process-desc { margin-top: 2rpx; color: #8E8E93; font-size: 21rpx; }

.privacy-note { display: flex; align-items: flex-start; gap: 10rpx; padding: 22rpx 8rpx; color: #636366; font-size: 21rpx; line-height: 1.5; }
.privacy-note text { flex: 1; }

.submit-area { margin-top: 4rpx; }
.submit-button { display: flex; min-height: 92rpx; align-items: center; justify-content: center; gap: 10rpx; border-radius: 16rpx; background: #007AFF; color: #FFFFFF; font-size: 28rpx; font-weight: 680; }
.submit-button.disabled { opacity: .36; }
</style>
